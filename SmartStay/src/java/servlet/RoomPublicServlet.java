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
        LocalDate today = LocalDate.now();
        int maxDays = AppConstants.MAX_ADVANCE_BOOKING_DAYS;
        LocalDate maxDate = today.plusDays(maxDays);

        String dateParam = req.getParameter("date");
        LocalDate date = RoomService.parseDateOrToday(dateParam);

        if (!RoomService.withinAdvanceLimit(date, today)) {
            req.setAttribute("dateNotice", "Bookings are open from today up to " + maxDays + " days ahead.");
        }

        // Parse slots filter: supports multiple slots parameter values or comma-separated string
        List<String> selectedSlotCodes = new ArrayList<>();
        String[] slotArr = req.getParameterValues("slots");
        if (slotArr != null) {
            for (String s : slotArr) {
                if (s != null) {
                    for (String part : s.split(",")) {
                        String clean = part.trim().toUpperCase();
                        if (!clean.isEmpty() && !selectedSlotCodes.contains(clean)) {
                            selectedSlotCodes.add(clean);
                        }
                    }
                }
            }
        }
        String singleSlot = req.getParameter("slot");
        if (singleSlot != null && !singleSlot.trim().isEmpty()) {
            String clean = singleSlot.trim().toUpperCase();
            if (!selectedSlotCodes.contains(clean)) {
                selectedSlotCodes.add(clean);
            }
        }

        // Weekly 7-day strip navigation
        String weekStartParam = req.getParameter("weekStart");
        LocalDate weekStart = RoomService.parseDateOrToday(weekStartParam != null && !weekStartParam.isEmpty() ? weekStartParam : dateParam);
        if (weekStart.isBefore(today)) weekStart = today;
        if (weekStart.isAfter(maxDate)) weekStart = maxDate;

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
            weekDays.add(new WeekDayItem(d, dayName, d.format(dayFmt), d.isEqual(date)));
        }
        LocalDate prevWeekStart = weekStart.minusDays(7);
        if (prevWeekStart.isBefore(today)) prevWeekStart = today;
        LocalDate nextWeekStart = weekStart.plusDays(7);
        boolean canGoPrev = weekStart.isAfter(today);
        boolean canGoNext = nextWeekStart.isBefore(maxDate) || nextWeekStart.isEqual(maxDate);

        PageRequest pr = PageRequest.from(req, SORT_KEYS, "room", "ASC");
        Map<String, String> errors = new HashMap<>();

        String capStr = pr.getFilter("minCapacity");
        if (!capStr.isEmpty()) {
            Integer cap = Validator.parseIntOrNull(capStr);
            if (cap == null || cap < 1 || cap > 20) {
                errors.put("minCapacity", "Capacity must be between 1 and 20.");
                pr.setFilter("minCapacity", null);
            }
        }

        String floorStr = pr.getFilter("floor");
        if (!floorStr.isEmpty()) {
            Integer floor = Validator.parseIntOrNull(floorStr);
            if (floor == null || floor < 0 || floor > 50) {
                errors.put("floor", "Floor must be between 0 and 50.");
                pr.setFilter("floor", null);
            }
        }

        String minPriceStr = pr.getFilter("minPrice");
        String maxPriceStr = pr.getFilter("maxPrice");
        Long minPrice = null;
        Long maxPrice = null;

        if (!minPriceStr.isEmpty()) {
            try {
                long mp = Long.parseLong(minPriceStr.trim());
                if (mp < 0 || mp > 100000000L) {
                    errors.put("minPrice", "Price must be between 0 and 100,000,000 VND.");
                    pr.setFilter("minPrice", null);
                } else {
                    minPrice = mp;
                }
            } catch (NumberFormatException e) {
                errors.put("minPrice", "Invalid minimum price format.");
                pr.setFilter("minPrice", null);
            }
        }

        if (!maxPriceStr.isEmpty()) {
            try {
                long mp = Long.parseLong(maxPriceStr.trim());
                if (mp < 0 || mp > 100000000L) {
                    errors.put("maxPrice", "Price must be between 0 and 100,000,000 VND.");
                    pr.setFilter("maxPrice", null);
                } else {
                    maxPrice = mp;
                }
            } catch (NumberFormatException e) {
                errors.put("maxPrice", "Invalid maximum price format.");
                pr.setFilter("maxPrice", null);
            }
        }

        if (minPrice != null && maxPrice != null && minPrice > maxPrice) {
            errors.put("minPrice", "Minimum price cannot exceed maximum price.");
            pr.setFilter("minPrice", null);
            pr.setFilter("maxPrice", null);
        }

        PageResult<RoomCard> result;
        int totalRoomCount = 0;
        int availableRoomCount = 0;
        try {
            PageResult<RoomCard> allCards = catalogService.searchRooms(pr, date);
            totalRoomCount = allCards.getItems().size();

            if (selectedSlotCodes != null && !selectedSlotCodes.isEmpty()) {
                result = catalogService.searchRooms(pr, date, selectedSlotCodes);
                availableRoomCount = result.getItems().size();
            } else {
                result = allCards;
                int countAvail = 0;
                for (RoomCard c : allCards.getItems()) {
                    if (c.isBookable()) {
                        boolean any = false;
                        if (c.getSlots() != null) {
                            for (SlotAvailability sa : c.getSlots()) {
                                if (sa.isSelectable()) { any = true; break; }
                            }
                        }
                        if (any) countAvail++;
                    }
                }
                availableRoomCount = countAvail;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to load room catalog from database", e);
            result = new PageResult<>(Collections.<RoomCard>emptyList(), 0, pr);
            req.setAttribute("dbError", "Rooms could not be loaded. Please try again later.");
        }

        req.setAttribute("result", result);
        req.setAttribute("totalRoomCount", totalRoomCount);
        req.setAttribute("availableRoomCount", availableRoomCount);
        req.setAttribute("selectedSlots", selectedSlotCodes);
        req.setAttribute("selectedSlotsStr", String.join(",", selectedSlotCodes));
        req.setAttribute("weekStart", weekStart);
        req.setAttribute("weekDays", weekDays);
        req.setAttribute("prevWeekStart", prevWeekStart);
        req.setAttribute("nextWeekStart", nextWeekStart);
        req.setAttribute("canGoPrev", canGoPrev);
        req.setAttribute("canGoNext", canGoNext);
        req.setAttribute("date", date);
        req.setAttribute("today", today);
        req.setAttribute("maxDate", maxDate);
        req.setAttribute("errors", errors);
        req.setAttribute("rates", PricingService.DiscountRates.fromConfig());
        String curCap = pr.getFilter("minCapacity");
        String curFloor = pr.getFilter("floor");
        String curSort = (pr.getSort() != null) ? pr.getSort() : "room";
        String curDir = (pr.getDir() != null) ? pr.getDir() : "ASC";
        req.setAttribute("currentCap", curCap);
        req.setAttribute("currentFloor", curFloor);
        req.setAttribute("currentSort", curSort);
        req.setAttribute("currentDir", curDir);
        req.setAttribute("activeSortLabel", util.ElFunctions.sortLabel(curSort, curDir));

        req.getRequestDispatcher("/WEB-INF/views/publicsite/rooms.jsp").forward(req, resp);
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

        LocalDate today = LocalDate.now();
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
            LocalDateTime now = LocalDateTime.now();

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
    }
}
