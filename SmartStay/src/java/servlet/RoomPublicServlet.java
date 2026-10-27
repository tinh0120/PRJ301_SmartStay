package servlet;

import dao.ReviewDao;
import model.Review;
import model.Room;
import model.Room.RoomCard;
import model.RoomSlot;
import model.RoomSlot.SlotAvailability;
import service.PricingService;
import service.RoomService;
import util.AppConstants;
import util.PageResult.PageRequest;
import util.PageResult;
import util.RoomFilter;
import util.Validator;
import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Public controller for browsing available rooms and inspecting room details.
 */
@WebServlet(name = "RoomPublicServlet", urlPatterns = {"/rooms", "/rooms/detail"})
public class RoomPublicServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(RoomPublicServlet.class.getName());
    private static final String[] SORT_KEYS = {"room", "floor", "capacity", "price"};

    private final RoomService catalogService = new RoomService();
    private final ReviewDao reviewDao = new ReviewDao();
    private final PricingService pricingService = new PricingService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String path = req.getServletPath();
        if ("/rooms/detail".equals(path)) {
            handleDetail(req, resp);
            return;
        }
        handleList(req, resp);
    }

    // =========================================================================
    // ROOM LIST & SEARCH
    // =========================================================================

    private void handleList(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        LocalDate today = util.ClockService.now().toLocalDate();
        int maxDays = AppConstants.MAX_ADVANCE_BOOKING_DAYS;
        LocalDate maxDate = today.plusDays(maxDays);
        String ctx = req.getContextPath();

        String dateParam = req.getParameter("date");
        LocalDate date = RoomService.parseDateOrToday(dateParam);

        if (!RoomService.withinAdvanceLimit(date, today)) {
            req.setAttribute("dateNotice", "Bookings are open from today up to " + maxDays + " days ahead.");
        }

        // Slots: repeated "slots" values, "S1,S2" or legacy "slot"; day order; consecutive only (R4)
        List<String> requestedSlots = RoomFilter.parseSlots(concat(req.getParameterValues("slots"), req.getParameter("slot")));
        List<String> selectedSlotCodes = requestedSlots;
        if (!RoomFilter.isConsecutive(requestedSlots)) {
            selectedSlotCodes = RoomFilter.firstConsecutiveRun(requestedSlots);
            req.setAttribute("slotNotice", "Time slots must be next to each other. Showing "
                    + RoomFilter.slotLabels(selectedSlotCodes) + ".");
        }

        Integer minCapacity = RoomFilter.parseIntFilter(req.getParameter("minCapacity"), 1, 20);
        Integer floor = RoomFilter.parseIntFilter(req.getParameter("floor"), 0, 50);
        String sort = RoomFilter.normalizeSort(req.getParameter("sort"), req.getParameter("dir"));
        boolean availableOnly = "1".equals(req.getParameter("available")) || "true".equalsIgnoreCase(req.getParameter("available"));

        // Weekly 7-day strip navigation
        String weekStartParam = req.getParameter("weekStart");
        LocalDate weekStart = RoomService.parseDateOrToday(weekStartParam != null && !weekStartParam.isEmpty() ? weekStartParam : dateParam);
        if (weekStart.isBefore(today)) weekStart = today;
        if (weekStart.isAfter(maxDate)) weekStart = maxDate;
        if (date.isBefore(weekStart) || date.isAfter(weekStart.plusDays(6))) {
            weekStart = date.isBefore(today) ? today : date;
        }

        RoomFilter.State state = new RoomFilter.State(date.toString(), weekStart.toString(), selectedSlotCodes,
                minCapacity, floor, sort, availableOnly);
        String roomsPath = ctx + "/rooms";

        List<WeekDayItem> weekDays = new ArrayList<>(7);
        DateTimeFormatter dayFmt = DateTimeFormatter.ofPattern("dd/MM");
        for (int i = 0; i < 7; i++) {
            LocalDate d = weekStart.plusDays(i);
            String dayName;
            if (d.isEqual(today)) {
                dayName = "Today";
            } else {
                switch (d.getDayOfWeek().getValue()) {
                    case 1: dayName = "Mon"; break;
                    case 2: dayName = "Tue"; break;
                    case 3: dayName = "Wed"; break;
                    case 4: dayName = "Thu"; break;
                    case 5: dayName = "Fri"; break;
                    case 6: dayName = "Sat"; break;
                    case 7: dayName = "Sun"; break;
                    default: dayName = "Day " + d.getDayOfMonth(); break;
                }
            }
            WeekDayItem item = new WeekDayItem(d, dayName, d.format(dayFmt), d.isEqual(date));
            item.setUrl(state.withDate(d.toString(), weekStart.toString()).url(roomsPath));
            weekDays.add(item);
        }
        LocalDate prevWeekStart = weekStart.minusDays(7);
        if (prevWeekStart.isBefore(today)) prevWeekStart = today;
        LocalDate nextWeekStart = weekStart.plusDays(7);
        boolean canGoPrev = weekStart.isAfter(today);
        boolean canGoNext = nextWeekStart.isBefore(maxDate) || nextWeekStart.isEqual(maxDate);

        // Load every active room once; capacity/floor filters and sorting run in memory (6 rooms).
        PageRequest pr = new PageRequest();
        pr.setSize(100);
        pr.setSort("room");
        pr.setDir("ASC");

        List<RoomListItem> items = new ArrayList<>();
        java.util.TreeSet<Integer> capacityOptions = new java.util.TreeSet<>();
        java.util.TreeSet<Integer> floorOptions = new java.util.TreeSet<>();
        int totalRoomCount = 0;
        int availableRoomCount = 0;
        PricingService.DiscountRates rates = PricingService.DiscountRates.fromConfig();
        try {
            List<RoomCard> allCards = catalogService.searchRooms(pr, date).getItems();
            for (RoomCard c : allCards) {
                capacityOptions.add(c.getRoom().getCapacity());
                floorOptions.add(c.getRoom().getFloorNumber());
            }
            for (RoomCard c : allCards) {
                if (minCapacity != null && c.getRoom().getCapacity() < minCapacity) continue;
                if (floor != null && c.getRoom().getFloorNumber() != floor) continue;
                RoomListItem item = buildListItem(c, selectedSlotCodes, date, rates);
                item.setDetailUrl(ctx + "/rooms/detail?id=" + c.getRoom().getRoomId() + "&date=" + date
                        + (selectedSlotCodes.isEmpty() ? "" : "&slots=" + state.getSlotsCsv()));
                items.add(item);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to load room catalog from database", e);
            req.setAttribute("dbError", "Rooms could not be loaded. Please try again later.");
        }

        sortItems(items, sort);
        totalRoomCount = items.size();
        for (RoomListItem it : items) {
            if (it.isAvailable()) availableRoomCount++;
        }
        List<RoomListItem> shown = new ArrayList<>();
        for (RoomListItem it : items) {
            if (!availableOnly || it.isAvailable()) shown.add(it);
        }

        // Status line + active filter chips (each removable) + clear all
        DateTimeFormatter longFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String statusLine = availableRoomCount + " of " + totalRoomCount + " rooms available · " + date.format(longFmt)
                + (selectedSlotCodes.isEmpty() ? "" : " · " + RoomFilter.slotLabels(selectedSlotCodes));
        List<FilterChip> chips = new ArrayList<>();
        if (!date.isEqual(today)) {
            chips.add(new FilterChip(date.format(longFmt), "Remove date filter",
                    state.withDate(today.toString(), today.toString()).url(roomsPath)));
        }
        if (!selectedSlotCodes.isEmpty()) {
            chips.add(new FilterChip(RoomFilter.slotLabels(selectedSlotCodes).replace(", ", " + "), "Remove time slots",
                    state.withSlots(null).url(roomsPath)));
        }
        if (minCapacity != null) {
            chips.add(new FilterChip(minCapacity + "+ guests", "Remove capacity filter", state.withMinCapacity(null).url(roomsPath)));
        }
        if (floor != null) {
            chips.add(new FilterChip("Floor " + floor, "Remove floor filter", state.withFloor(null).url(roomsPath)));
        }
        if (availableOnly) {
            chips.add(new FilterChip("Available only", "Show unavailable rooms too", state.withAvailableOnly(false).url(roomsPath)));
        }

        req.setAttribute("rooms", shown);
        req.setAttribute("totalRoomCount", totalRoomCount);
        req.setAttribute("availableRoomCount", availableRoomCount);
        req.setAttribute("statusLine", statusLine);
        req.setAttribute("chips", chips);
        req.setAttribute("clearAllUrl", roomsPath);
        req.setAttribute("capacityOptions", new ArrayList<>(capacityOptions));
        req.setAttribute("floorOptions", new ArrayList<>(floorOptions));
        req.setAttribute("filterState", state);
        req.setAttribute("selectedSlots", selectedSlotCodes);
        req.setAttribute("selectedSlotsStr", state.getSlotsCsv());
        req.setAttribute("weekStart", weekStart);
        req.setAttribute("weekDays", weekDays);
        req.setAttribute("prevWeekUrl", state.withDate(prevWeekStart.toString(), prevWeekStart.toString()).url(roomsPath));
        req.setAttribute("nextWeekUrl", state.withDate(nextWeekStart.toString(), nextWeekStart.toString()).url(roomsPath));
        req.setAttribute("canGoPrev", canGoPrev);
        req.setAttribute("canGoNext", canGoNext);
        req.setAttribute("date", date);
        req.setAttribute("today", today);
        req.setAttribute("maxDate", maxDate);
        req.setAttribute("rates", rates);
        req.setAttribute("currentCap", minCapacity == null ? "" : String.valueOf(minCapacity));
        req.setAttribute("currentFloor", floor == null ? "" : String.valueOf(floor));
        req.setAttribute("currentSort", sort);
        req.setAttribute("activeSortLabel", RoomFilter.sortLabel(sort));
        req.setAttribute("availableOnly", availableOnly);

        req.getRequestDispatcher("/WEB-INF/views/publicsite/rooms.jsp").forward(req, resp);
    }

    private static String[] concat(String[] values, String extra) {
        List<String> all = new ArrayList<>();
        if (values != null) {
            Collections.addAll(all, values);
        }
        if (extra != null) {
            all.add(extra);
        }
        return all.toArray(new String[0]);
    }

    /**
     * Availability, reason and price of one room card for the current selection.
     * The price comes from PricingService.quote, the same code as the booking summary.
     */
    private RoomListItem buildListItem(RoomCard card, List<String> selected, LocalDate date,
                                       PricingService.DiscountRates rates) {
        RoomListItem item = new RoomListItem(card);
        Map<String, SlotAvailability> byCode = new HashMap<>();
        List<RoomSlot> roomSlots = new ArrayList<>();
        if (card.getSlots() != null) {
            for (SlotAvailability sa : card.getSlots()) {
                if (sa.getSlot() != null && sa.getSlot().getSlotCode() != null) {
                    byCode.put(sa.getSlot().getSlotCode().toUpperCase(), sa);
                    roomSlots.add(sa.getSlot());
                }
            }
        }

        if (selected.isEmpty()) {
            boolean any = false;
            for (SlotAvailability sa : byCode.values()) {
                if (sa.isSelectable()) { any = true; break; }
            }
            item.setAvailable(card.isBookable() && any);
            if (!card.isBookable()) {
                item.setReason("Under maintenance");
            } else if (!any) {
                item.setReason("No free slots on " + date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            }
            return item;
        }

        List<String> missing = new ArrayList<>();
        for (String code : selected) {
            SlotAvailability sa = byCode.get(code);
            if (sa == null || !sa.isSelectable()) {
                missing.add(code);
            }
        }
        item.setAvailable(card.isBookable() && missing.isEmpty());
        if (!card.isBookable()) {
            item.setReason("Under maintenance");
        } else if (!missing.isEmpty()) {
            item.setReason("Unavailable for " + RoomFilter.slotLabels(missing));
        }

        // Real times of this room for the selected slots, e.g. "09:30 → 19:30" or "16:30 → 09:00 (+1 day)"
        SlotAvailability first = byCode.get(selected.get(0));
        SlotAvailability last = byCode.get(selected.get(selected.size() - 1));
        if (first != null && last != null && first.getStartAt() != null && last.getEndAt() != null) {
            DateTimeFormatter hm = DateTimeFormatter.ofPattern("HH:mm");
            String t = first.getStartAt().format(hm) + " → " + last.getEndAt().format(hm);
            if (last.getEndAt().toLocalDate().isAfter(first.getStartAt().toLocalDate())) {
                t += " (+1 day)";
            }
            item.setTimeText(t);
        }

        try {
            model.Booking.Quote q = pricingService.quote(roomSlots, selected, rates);
            item.setQuoteTotal(q.getTotal());
            item.setDiscountPercent(q.getDiscountPercent());
            item.setSlotCount(selected.size());
        } catch (Exception e) {
            // Slot not offered by this room: no combined price, the reason already explains it.
            item.setQuoteTotal(-1);
        }
        return item;
    }

    /** Sort by the chosen key, then move unavailable rooms to the end (stable). */
    private static void sortItems(List<RoomListItem> items, final String sort) {
        java.util.Comparator<RoomListItem> byRoom = new java.util.Comparator<RoomListItem>() {
            @Override
            public int compare(RoomListItem a, RoomListItem b) {
                return String.valueOf(a.getCard().getRoom().getRoomNumber())
                        .compareToIgnoreCase(String.valueOf(b.getCard().getRoom().getRoomNumber()));
            }
        };
        java.util.Comparator<RoomListItem> cmp;
        if (RoomFilter.SORT_PRICE.equals(sort) || RoomFilter.SORT_PRICE_DESC.equals(sort)) {
            final int sign = RoomFilter.SORT_PRICE.equals(sort) ? 1 : -1;
            cmp = new java.util.Comparator<RoomListItem>() {
                @Override
                public int compare(RoomListItem a, RoomListItem b) {
                    return sign * Long.compare(a.getSortPrice(), b.getSortPrice());
                }
            };
        } else if (RoomFilter.SORT_CAPACITY.equals(sort)) {
            cmp = new java.util.Comparator<RoomListItem>() {
                @Override
                public int compare(RoomListItem a, RoomListItem b) {
                    return Integer.compare(b.getCard().getRoom().getCapacity(), a.getCard().getRoom().getCapacity());
                }
            };
        } else {
            cmp = byRoom;
        }
        Collections.sort(items, cmp.thenComparing(byRoom));
        Collections.sort(items, new java.util.Comparator<RoomListItem>() {
            @Override
            public int compare(RoomListItem a, RoomListItem b) {
                return Boolean.compare(!a.isAvailable(), !b.isAvailable());
            }
        });
    }

    // =========================================================================
    // ROOM DETAIL
    // =========================================================================

    private void handleDetail(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String idStr = req.getParameter("id");
        long roomId;
        if (idStr == null || idStr.trim().isEmpty()) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Room ID is missing.");
            return;
        }
        try {
            roomId = Long.parseLong(idStr.trim());
            if (roomId <= 0) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Invalid Room ID.");
                return;
            }
        } catch (NumberFormatException e) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Invalid Room ID format.");
            return;
        }

        LocalDate today = util.ClockService.now().toLocalDate();
        int maxDays = AppConstants.MAX_ADVANCE_BOOKING_DAYS;
        LocalDate maxDate = today.plusDays(maxDays);

        String dateParam = req.getParameter("date");
        LocalDate date = RoomService.parseDateOrToday(dateParam);

        String weekStartParam = req.getParameter("weekStart");
        LocalDate weekStart = RoomService.parseDateOrToday(weekStartParam != null && !weekStartParam.isEmpty() ? weekStartParam : dateParam);
        if (weekStart.isBefore(today)) weekStart = today;
        if (weekStart.isAfter(maxDate)) weekStart = maxDate;

        LocalDate startDate = weekStart;
        LocalDate endDate = startDate.plusDays(6);
        if (endDate.isAfter(maxDate)) endDate = maxDate;

        LocalDate prevWeekStart = startDate.minusDays(7);
        if (prevWeekStart.isBefore(today)) prevWeekStart = today;
        LocalDate nextWeekStart = startDate.plusDays(7);
        boolean canGoPrev = startDate.isAfter(today);
        boolean canGoNext = nextWeekStart.isBefore(maxDate) || nextWeekStart.isEqual(maxDate);

        RoomCard card;
        List<Review> reviews;
        double[] ratingStats;

        try {
            card = catalogService.getRoomCard(roomId, date);
            if (card == null || card.getRoom() == null || !card.getRoom().isActive()) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Room not found or inactive.");
                return;
            }
            reviews = reviewDao.findVisibleByRoom(roomId, 10);
            ratingStats = reviewDao.averageRating(roomId);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to load room details for ID " + roomId, e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to load room details.");
            return;
        }

        List<DaySchedule> weekSchedule = new ArrayList<>(7);
        java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
        try {
            Map<LocalDate, Set<Long>> heldByDate = catalogService.findHeldSlotIdsInRange(roomId, startDate, endDate);

            Room room = card.getRoom();
            List<RoomSlot> baseSlots = new ArrayList<>();
            if (card.getSlots() != null) {
                for (SlotAvailability sa : card.getSlots()) {
                    if (sa.getSlot() != null) {
                        baseSlots.add(sa.getSlot());
                    }
                }
            }
            LocalDateTime now = util.ClockService.now();

            for (int i = 0; i < 7; i++) {
                LocalDate d = startDate.plusDays(i);
                String label;
                if (d.isEqual(today)) {
                    label = "Today";
                } else {
                    int dow = d.getDayOfWeek().getValue();
                    switch (dow) {
                        case 1: label = "Mon"; break;
                        case 2: label = "Tue"; break;
                        case 3: label = "Wed"; break;
                        case 4: label = "Thu"; break;
                        case 5: label = "Fri"; break;
                        case 6: label = "Sat"; break;
                        case 7: label = "Sun"; break;
                        default: label = "Day " + dow; break;
                    }
                }
                Set<Long> dayHeld = heldByDate.get(d);
                if (dayHeld == null) {
                    dayHeld = Collections.emptySet();
                }
                List<SlotAvailability> dayAvail = catalogService.availabilityFor(room, baseSlots, dayHeld, d, now);
                RoomCard dayCard = new RoomCard(room, dayAvail, card.getMinPrice(), card.isBookable());
                weekSchedule.add(new DaySchedule(d, label, d.format(dtf), dayCard));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to load week schedule for room " + roomId, e);
        }

        HttpSession session = req.getSession(false);
        boolean loggedInGuest = (session != null && "GUEST".equalsIgnoreCase((String) session.getAttribute("roleName")));

        List<Review> safeReviews = reviews != null ? reviews : Collections.<Review>emptyList();
        List<Review> topReviews = new ArrayList<>();
        List<Review> extraReviews = new ArrayList<>();
        for (int i = 0; i < safeReviews.size(); i++) {
            if (i < 2) {
                topReviews.add(safeReviews.get(i));
            } else {
                extraReviews.add(safeReviews.get(i));
            }
        }

        req.setAttribute("card", card);
        // Rooms page "View Room & Availability" passes date + slots; the slot picker preselects them (R4)
        req.setAttribute("preselectDate", date.toString());
        req.setAttribute("preselectSlots", String.join(",",
                RoomFilter.firstConsecutiveRun(RoomFilter.parseSlots(req.getParameterValues("slots")))));
        // 48-hour cancellation line in the booking summary (R3): virtual "now" + window for cancel-policy.js
        req.setAttribute("clockNowIso", util.ClockService.now().withNano(0).toString());
        req.setAttribute("cancelFreeHours", AppConstants.CANCEL_FREE_HOURS);
        req.setAttribute("weekSchedule", weekSchedule);
        req.setAttribute("reviews", safeReviews);
        req.setAttribute("topReviews", topReviews);
        req.setAttribute("extraReviews", extraReviews);
        req.setAttribute("avgRating", ratingStats[0]);
        req.setAttribute("reviewCount", (int) ratingStats[1]);
        req.setAttribute("rates", PricingService.DiscountRates.fromConfig());
        req.setAttribute("date", date);
        req.setAttribute("today", today);
        req.setAttribute("maxDate", maxDate);
        req.setAttribute("weekStart", startDate);
        req.setAttribute("prevWeekStart", prevWeekStart);
        req.setAttribute("nextWeekStart", nextWeekStart);
        req.setAttribute("canGoPrev", canGoPrev);
        req.setAttribute("canGoNext", canGoNext);
        req.setAttribute("startDateFormatted", startDate.format(dtf));
        req.setAttribute("endDateFormatted", startDate.plusDays(6).format(dtf));
        // R5: one week control "‹ 25 – 31 Oct ›"
        req.setAttribute("weekRangeLabel", RoomFilter.weekRangeLabel(startDate, startDate.plusDays(6)));
        req.setAttribute("loggedInGuest", loggedInGuest);
        req.setAttribute("currentDetailUrl", "/rooms/detail?id=" + roomId + "&date=" + date + "&weekStart=" + startDate);

        req.getRequestDispatcher("/WEB-INF/views/publicsite/room-detail.jsp").forward(req, resp);
    }

    public static class DaySchedule implements Serializable {
        private static final long serialVersionUID = 1L;
        private final LocalDate date;
        private final String dayOfWeekName;
        private final String dateFormatted;
        private final RoomCard roomCard;

        public DaySchedule(LocalDate date, String dayOfWeekName, String dateFormatted, RoomCard roomCard) {
            this.date = date;
            this.dayOfWeekName = dayOfWeekName;
            this.dateFormatted = dateFormatted;
            this.roomCard = roomCard;
        }

        public LocalDate getDate() {
            return date;
        }

        public String getDayOfWeekName() {
            return dayOfWeekName;
        }

        public String getDateFormatted() {
            return dateFormatted;
        }

        public RoomCard getRoomCard() {
            return roomCard;
        }

        public List<SlotAvailability> getSlots() {
            return roomCard != null ? roomCard.getSlots() : Collections.<SlotAvailability>emptyList();
        }

        public boolean isAllSlotsPast() {
            List<SlotAvailability> slots = getSlots();
            if (slots == null || slots.isEmpty()) return false;
            for (SlotAvailability sa : slots) {
                if (sa.isSelectable()) {
                    return false;
                }
            }
            return true;
        }

        public boolean getAllSlotsPast() {
            return isAllSlotsPast();
        }
    }

    public static class WeekDayItem implements Serializable {
        private static final long serialVersionUID = 1L;
        private final LocalDate date;
        private final String dayName;
        private final String shortDate;
        private final boolean active;

        public WeekDayItem(LocalDate date, String dayName, String shortDate, boolean active) {
            this.date = date;
            this.dayName = dayName;
            this.shortDate = shortDate;
            this.active = active;
        }

        public LocalDate getDate() {
            return date;
        }

        public String getDayName() {
            return dayName;
        }

        public String getShortDate() {
            return shortDate;
        }

        public boolean isActive() {
            return active;
        }

        /** Link that keeps every other filter (R4). */
        private String url;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }

    /** One room on the Rooms page for the current date/slot selection (R4). */
    public static class RoomListItem implements Serializable {
        private static final long serialVersionUID = 1L;
        private final RoomCard card;
        private boolean available;
        private String reason;
        private String timeText;
        private long quoteTotal = -1;
        private int discountPercent;
        private int slotCount;
        private String detailUrl;

        public RoomListItem(RoomCard card) {
            this.card = card;
        }

        public RoomCard getCard() { return card; }
        public boolean isAvailable() { return available; }
        public void setAvailable(boolean available) { this.available = available; }
        /** Plain-words reason when unavailable, e.g. "Unavailable for Morning, Afternoon". */
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        /** This room's real times for the selected slots, e.g. "09:30 → 19:30". */
        public String getTimeText() { return timeText; }
        public void setTimeText(String timeText) { this.timeText = timeText; }
        /** Total for the selected slots from PricingService.quote, or -1 when no slots are selected. */
        public long getQuoteTotal() { return quoteTotal; }
        public void setQuoteTotal(long quoteTotal) { this.quoteTotal = quoteTotal; }
        public boolean isHasQuote() { return quoteTotal >= 0; }
        public int getDiscountPercent() { return discountPercent; }
        public void setDiscountPercent(int discountPercent) { this.discountPercent = discountPercent; }
        public int getSlotCount() { return slotCount; }
        public void setSlotCount(int slotCount) { this.slotCount = slotCount; }
        public String getDetailUrl() { return detailUrl; }
        public void setDetailUrl(String detailUrl) { this.detailUrl = detailUrl; }

        /** Price used for sorting: the selection total when known, else the "from" price. */
        public long getSortPrice() {
            return quoteTotal >= 0 ? quoteTotal : card.getMinPrice();
        }
    }

    /** Removable active-filter chip (R4). */
    public static class FilterChip implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String label;
        private final String removeLabel;
        private final String removeUrl;

        public FilterChip(String label, String removeLabel, String removeUrl) {
            this.label = label;
            this.removeLabel = removeLabel;
            this.removeUrl = removeUrl;
        }

        public String getLabel() { return label; }
        public String getRemoveLabel() { return removeLabel; }
        public String getRemoveUrl() { return removeUrl; }
    }
}
