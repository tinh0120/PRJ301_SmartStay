package service;

import dao.RoomDao;
import model.Booking.Quote;
import model.Room;
import model.Room.RoomCard;
import model.RoomSlot;
import model.RoomSlot.SlotAvailability;
import util.AppConstants;
import util.DBUtil;
import util.PageResult.PageRequest;
import util.PageResult;
import util.ElFunctions;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Unified Room service coordinating both public catalog/availability/quotation
 * and administrative room CRUD, lifecycle status, and slot scheduling.
 */
public class RoomService {

    private static final Logger LOGGER = Logger.getLogger(RoomService.class.getName());

    private final RoomDao roomDao;
    private final PricingService pricingService;

    public RoomService() {
        this.roomDao = new RoomDao();
        this.pricingService = new PricingService();
    }

    public RoomService(RoomDao roomDao, PricingService pricingService) {
        this.roomDao = roomDao;
        this.pricingService = pricingService;
    }

    // =========================================================================
    // PUBLIC CATALOG & AVAILABILITY METHODS
    // =========================================================================

    public PageResult<RoomCard> searchRooms(PageRequest pr, LocalDate date) throws SQLException {
        return searchRooms(pr, date, null);
    }

    public PageResult<RoomCard> searchRooms(PageRequest pr, LocalDate date, List<String> selectedSlotCodes) throws SQLException {
        PageResult<Room> roomPage = roomDao.findPage(pr, date);
        if (roomPage.isEmpty()) {
            return new PageResult<>(Collections.<RoomCard>emptyList(), 0, pr);
        }

        List<Long> roomIds = new ArrayList<>();
        for (Room r : roomPage.getItems()) {
            roomIds.add(r.getRoomId());
        }

        Map<Long, List<RoomSlot>> slotMap = roomDao.findByRoomIds(roomIds);
        Set<Long> heldIds = roomDao.findHeldSlotIds(roomIds, date);
        LocalDateTime now = LocalDateTime.now();

        List<RoomCard> cards = new ArrayList<>();
        for (Room r : roomPage.getItems()) {
            List<RoomSlot> slots = slotMap.get(r.getRoomId());
            if (slots == null) {
                slots = Collections.emptyList();
            }
            List<SlotAvailability> availList = availabilityFor(r, slots, heldIds, date, now);
            long minPrice = Long.MAX_VALUE;
            long overnightPrice = 0L;
            for (RoomSlot s : slots) {
                if (s.isActive()) {
                    if (s.getListPrice() < minPrice) {
                        minPrice = s.getListPrice();
                    }
                    if ("S4".equalsIgnoreCase(s.getSlotCode()) || "Overnight".equalsIgnoreCase(s.getSlotName())) {
                        overnightPrice = s.getListPrice();
                    }
                }
            }
            if (minPrice == Long.MAX_VALUE) {
                minPrice = 0L;
            }
            if (overnightPrice > 0) {
                r.setOvernightPrice(overnightPrice);
            }
            boolean bookable = r.isActive() && !"MAINTENANCE".equalsIgnoreCase(r.getStatus());
            cards.add(new RoomCard(r, availList, minPrice, bookable));
        }

        if (selectedSlotCodes != null && !selectedSlotCodes.isEmpty()) {
            List<RoomCard> filteredCards = new ArrayList<>();
            for (RoomCard card : cards) {
                if (card.isBookable() && hasAllSlotsAvailable(card, selectedSlotCodes)) {
                    filteredCards.add(card);
                }
            }
            return new PageResult<>(filteredCards, filteredCards.size(), pr);
        }

        return new PageResult<>(cards, roomPage.getTotal(), pr);
    }

    private boolean hasAllSlotsAvailable(RoomCard card, List<String> requiredCodes) {
        if (card.getSlots() == null || card.getSlots().isEmpty()) {
            return false;
        }
        for (String reqCode : requiredCodes) {
            if (reqCode == null || reqCode.trim().isEmpty()) continue;
            String normalizedReq = reqCode.trim().toUpperCase();
            boolean foundAndAvail = false;
            for (SlotAvailability sa : card.getSlots()) {
                if (sa.getSlot() != null && normalizedReq.equalsIgnoreCase(sa.getSlot().getSlotCode())) {
                    if (sa.isSelectable()) {
                        foundAndAvail = true;
                    }
                    break;
                }
            }
            if (!foundAndAvail) {
                return false;
            }
        }
        return true;
    }

    public RoomCard getRoomCard(long roomId, LocalDate date) throws SQLException {
        Room room = roomDao.findById(roomId);
        if (room == null || !room.isActive()) {
            return null;
        }

        List<Long> idList = Collections.singletonList(roomId);
        Map<Long, List<RoomSlot>> slotMap = roomDao.findByRoomIds(idList);
        Set<Long> heldIds = roomDao.findHeldSlotIds(idList, date);
        LocalDateTime now = LocalDateTime.now();

        List<RoomSlot> slots = slotMap.get(roomId);
        if (slots == null) {
            slots = Collections.emptyList();
        }
        List<SlotAvailability> availList = availabilityFor(room, slots, heldIds, date, now);
        long minPrice = Long.MAX_VALUE;
        long overnightPrice = 0L;
        for (RoomSlot s : slots) {
            if (s.isActive()) {
                if (s.getListPrice() < minPrice) {
                    minPrice = s.getListPrice();
                }
                if ("S4".equalsIgnoreCase(s.getSlotCode()) || "Overnight".equalsIgnoreCase(s.getSlotName())) {
                    overnightPrice = s.getListPrice();
                }
            }
        }
        if (minPrice == Long.MAX_VALUE) {
            minPrice = 0L;
        }
        if (overnightPrice > 0) {
            room.setOvernightPrice(overnightPrice);
        }
        boolean bookable = room.isActive() && !"MAINTENANCE".equalsIgnoreCase(room.getStatus());
        return new RoomCard(room, availList, minPrice, bookable);
    }

    public Map<LocalDate, Set<Long>> findHeldSlotIdsInRange(long roomId, LocalDate startDate, LocalDate endDate) throws SQLException {
        return roomDao.findHeldSlotIdsInRange(roomId, startDate, endDate);
    }

    public List<SlotAvailability> availabilityFor(Room room, List<RoomSlot> slots, Set<Long> heldIds,
                                                  LocalDate date, LocalDateTime now) {
        if (slots == null) {
            return Collections.emptyList();
        }

        boolean bookable = (room != null && room.isActive() && !"MAINTENANCE".equalsIgnoreCase(room.getStatus()));
        LocalDate today = now.toLocalDate();
        boolean inRange = withinAdvanceLimit(date, today);

        List<SlotAvailability> list = new ArrayList<>();
        for (RoomSlot slot : slots) {
            LocalDateTime startAt = (date != null && slot.getStartTime() != null)
                    ? date.atTime(slot.getStartTime()) : null;
            LocalDateTime endAt = null;
            if (date != null && slot.getEndTime() != null) {
                endAt = date.atTime(slot.getEndTime());
                if (slot.isCrossesMidnight()) {
                    endAt = endAt.plusDays(1);
                }
            }

            String state;
            String label;

            if (!slot.isActive()) {
                state = SlotAvailability.STATE_NOT_OFFERED;
                label = "Not offered";
            } else if ("MAINTENANCE".equalsIgnoreCase(slot.getStatus())) {
                state = "MAINTENANCE";
                label = "Maintenance";
            } else if ("NEEDS_CLEANING".equalsIgnoreCase(slot.getStatus())) {
                state = "NEEDS_CLEANING";
                label = "Cleaning";
            } else if (!inRange) {
                state = SlotAvailability.STATE_OUT_OF_RANGE;
                label = "Not open yet";
            } else if (heldIds != null && heldIds.contains(slot.getRoomSlotId())) {
                state = SlotAvailability.STATE_HELD;
                label = "Held";
            } else if (startAt != null && !startAt.isAfter(now)) {
                state = SlotAvailability.STATE_STARTED;
                label = "Started";
            } else {
                state = SlotAvailability.STATE_AVAILABLE;
                label = "Available";
            }

            boolean selectable = SlotAvailability.STATE_AVAILABLE.equals(state) && bookable;
            list.add(new SlotAvailability(slot, startAt, endAt, state, selectable, label));
        }
        return list;
    }

    public Quote quoteFor(long roomId, LocalDate date, Collection<String> codes, LocalDateTime now)
            throws SQLException, ServiceException {

        Room room = roomDao.findById(roomId);
        if (room == null || !room.isActive()) {
            throw new ServiceException(ServiceException.ROOM_NOT_FOUND, "This room is not available.");
        }

        if ("MAINTENANCE".equalsIgnoreCase(room.getStatus())) {
            throw new ServiceException(ServiceException.ROOM_NOT_BOOKABLE,
                    "This room is under maintenance and cannot be booked.");
        }

        LocalDate today = now.toLocalDate();
        int maxDays = AppConstants.MAX_ADVANCE_BOOKING_DAYS;
        if (!withinAdvanceLimit(date, today)) {
            throw new ServiceException(ServiceException.DATE_OUT_OF_RANGE,
                    "Bookings are open from today up to " + maxDays + " days ahead.");
        }

        Map<Long, List<RoomSlot>> slotMap = roomDao.findByRoomIds(Collections.singletonList(roomId));
        List<RoomSlot> slots = slotMap.get(roomId);
        if (slots == null) {
            slots = Collections.emptyList();
        }

        PricingService.DiscountRates rates = PricingService.DiscountRates.fromConfig();
        Quote quote = pricingService.quote(slots, codes, rates);

        Set<Long> heldIds = roomDao.findHeldSlotIds(Collections.singletonList(roomId), date);
        List<SlotAvailability> availabilities = availabilityFor(room, slots, heldIds, date, now);

        Map<String, SlotAvailability> availMap = new HashMap<>();
        for (SlotAvailability sa : availabilities) {
            if (sa.getSlot() != null && sa.getSlot().getSlotCode() != null) {
                availMap.put(sa.getSlot().getSlotCode().toUpperCase(), sa);
            }
        }

        for (RoomSlot selected : quote.getSlots()) {
            SlotAvailability sa = availMap.get(selected.getSlotCode().toUpperCase());
            if (sa == null || !sa.isSelectable()) {
                throw new ServiceException(ServiceException.SLOT_UNAVAILABLE,
                        "One or more selected slots are no longer available.");
            }
        }

        return quote;
    }

    public static LocalDate parseDateOrToday(String s) {
        if (s == null || s.trim().isEmpty()) {
            return LocalDate.now();
        }
        try {
            return LocalDate.parse(s.trim());
        } catch (DateTimeParseException e) {
            return LocalDate.now();
        }
    }

    public static boolean withinAdvanceLimit(LocalDate date, LocalDate today) {
        if (date == null || today == null) {
            return false;
        }
        int maxDays = AppConstants.MAX_ADVANCE_BOOKING_DAYS;
        return !date.isBefore(today) && !date.isAfter(today.plusDays(maxDays));
    }

    // =========================================================================
    // ADMIN ROOM MANAGEMENT METHODS
    // =========================================================================

    public PageResult<Room> findAdminPage(PageRequest pr) throws SQLException {
        PageResult<Room> result = roomDao.findAdminPage(pr);
        if (result != null && result.getItems() != null && !result.getItems().isEmpty()) {
            List<Long> roomIds = new ArrayList<>();
            for (Room r : result.getItems()) {
                roomIds.add(r.getRoomId());
            }
            Map<Long, List<RoomSlot>> slotMap = roomDao.findAllByRoomIds(roomIds);
            Set<Long> heldIds = roomDao.findHeldSlotIds(roomIds, LocalDate.now());

            for (Room r : result.getItems()) {
                List<RoomSlot> slots = slotMap.get(r.getRoomId());
                if (slots != null) {
                    for (RoomSlot s : slots) {
                        s.setTodayStatus(s.getEffectiveStatus(heldIds));
                    }
                    r.setSlots(slots);
                } else {
                    r.setSlots(new ArrayList<RoomSlot>());
                }
            }
        }
        return result;
    }

    public Room getRoom(long roomId) throws SQLException {
        return roomDao.findById(roomId);
    }

    /**
     * Creates a new room and automatically generates its 4 default time slots in a single transaction.
     */
    public long createRoom(Room room, long userId, String ip) throws SQLException {
        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                if (roomDao.existsRoomNumber(conn, room.getRoomNumber(), null)) {
                    throw new IllegalArgumentException("This room number already exists.");
                }

                if (room.getDisplayName() == null || room.getDisplayName().trim().isEmpty()) {
                    room.setDisplayName(ElFunctions.getDisplayName(room.getRoomNumber()));
                }

                long roomId = roomDao.insert(conn, room);

                // Auto-create 4 default slots:
                // S1: 09:00 - 12:00 (150,000 VND)
                // S2: 13:00 - 16:00 (150,000 VND)
                // S3: 16:30 - 19:30 (180,000 VND)
                // S4: 20:00 - 09:00 (+1 day) (300,000 VND)
                createDefaultSlots(conn, roomId, userId);

                LOGGER.info("Created room " + room.getRoomNumber() + " with default slots");

                conn.commit();
                return roomId;
            } catch (Exception e) {
                conn.rollback();
                if (e instanceof IllegalArgumentException) {
                    throw (IllegalArgumentException) e;
                }
                if (e instanceof SQLException) {
                    SQLException se = (SQLException) e;
                    if (se.getErrorCode() == 2601 || se.getErrorCode() == 2627) {
                        throw new IllegalArgumentException("This room number already exists.");
                    }
                    throw se;
                }
                throw new SQLException("Failed to create room", e);
            }
        }
    }

    /**
     * Updates room metadata.
     */
    public void updateRoom(Room room, long userId, String ip) throws SQLException {
        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                if (roomDao.existsRoomNumber(conn, room.getRoomNumber(), room.getRoomId())) {
                    throw new IllegalArgumentException("This room number already exists.");
                }

                if ("AVAILABLE".equalsIgnoreCase(room.getStatus())) {
                    if (roomDao.hasActiveCheckin(conn, room.getRoomId())) {
                        throw new IllegalStateException("Room has an active check-in and cannot be set to Available.");
                    }
                }

                roomDao.update(conn, room);

                LOGGER.info("Updated room " + room.getRoomNumber());

                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                if (e instanceof IllegalArgumentException) {
                    throw (IllegalArgumentException) e;
                }
                if (e instanceof SQLException) {
                    SQLException se = (SQLException) e;
                    if (se.getErrorCode() == 2601 || se.getErrorCode() == 2627) {
                        throw new IllegalArgumentException("This room number already exists.");
                    }
                    throw se;
                }
                throw new SQLException("Failed to update room", e);
            }
        }
    }

    /**
     * Changes room status with guard against setting to AVAILABLE while an active CHECKED_IN booking exists.
     */
    public void changeRoomStatus(long roomId, String newStatus, long userId, String ip) throws SQLException {
        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Room room = roomDao.findById(roomId);
                if (room == null) {
                    throw new IllegalArgumentException("Room not found.");
                }

                if ("AVAILABLE".equalsIgnoreCase(newStatus)) {
                    if (roomDao.hasActiveCheckin(conn, roomId)) {
                        throw new IllegalStateException("Room has an active check-in and cannot be set to Available.");
                    }
                }

                String oldStatus = room.getStatus();
                roomDao.updateStatus(conn, roomId, newStatus);

                LOGGER.info("Room status changed for room " + roomId + ": " + oldStatus + " -> " + newStatus);

                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                if (e instanceof IllegalStateException || e instanceof IllegalArgumentException) {
                    throw e;
                }
                throw new SQLException("Failed to change room status", e);
            }
        }
    }

    /**
     * Hard-deletes a room and its slots if no bookings reference it.
     */
    public void deleteRoom(long roomId, long userId, String ip) throws SQLException {
        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Room room = roomDao.findById(roomId);
                if (room == null) {
                    throw new IllegalArgumentException("Room not found.");
                }

                roomDao.delete(conn, roomId);

                LOGGER.info("Deleted room " + room.getRoomNumber());

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                if (e.getErrorCode() == 547) { // Foreign Key reference
                    throw new IllegalStateException("This room has booking history and cannot be deleted. Deactivate it instead (is_active = 0).");
                }
                throw e;
            } catch (Exception e) {
                conn.rollback();
                throw new SQLException("Failed to delete room", e);
            }
        }
    }

    /**
     * Loads the 4 slot definitions for a given room.
     */
    public List<RoomSlot> getRoomSlots(long roomId) throws SQLException {
        String sql = "SELECT room_slot_id, room_id, slot_code, slot_name, start_time, end_time, "
                + "crosses_midnight, list_price, sort_order, is_active, status "
                + "FROM ROOM_TIME_SLOT WHERE room_id = ? ORDER BY sort_order ASC";

        List<RoomSlot> list = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    RoomSlot s = new RoomSlot();
                    s.setRoomSlotId(rs.getLong("room_slot_id"));
                    s.setRoomId(rs.getLong("room_id"));
                    s.setSlotCode(rs.getString("slot_code"));
                    s.setSlotName(rs.getString("slot_name"));
                    String st = rs.getString("start_time");
                    if (st != null && !st.trim().isEmpty()) {
                        try {
                            s.setStartTime(LocalTime.parse(st.trim()));
                        } catch (Exception ex) {
                            Time t = rs.getTime("start_time");
                            if (t != null) s.setStartTime(t.toLocalTime());
                        }
                    }
                    String et = rs.getString("end_time");
                    if (et != null && !et.trim().isEmpty()) {
                        try {
                            s.setEndTime(LocalTime.parse(et.trim()));
                        } catch (Exception ex) {
                            Time t = rs.getTime("end_time");
                            if (t != null) s.setEndTime(t.toLocalTime());
                        }
                    }
                    s.setCrossesMidnight(rs.getBoolean("crosses_midnight"));
                    BigDecimal p = rs.getBigDecimal("list_price");
                    s.setListPrice(p != null ? p.longValue() : 0L);
                    s.setSortOrder(rs.getInt("sort_order"));
                    s.setActive(rs.getBoolean("is_active"));
                    try {
                        String stt = rs.getString("status");
                        s.setStatus(stt != null ? stt : "AVAILABLE");
                    } catch (Exception ignored) {
                        s.setStatus("AVAILABLE");
                    }
                    list.add(s);
                }
            }
        }
        return list;
    }

    /**
     * Updates all 4 time slots of a room in one transaction.
     */
    public void updateRoomSlots(long roomId, List<RoomSlot> updatedSlots, long userId, String ip) throws SQLException {
        updateRoomSlots(roomId, updatedSlots, null, userId, ip);
    }

    public void updateRoomSlots(long roomId, List<RoomSlot> updatedSlots, String roomStatus, long userId, String ip) throws SQLException {
        if (updatedSlots == null || updatedSlots.size() != 4) {
            throw new IllegalArgumentException("Must provide exactly 4 time slots (S1 to S4).");
        }

        // 1. Validate individual slot inputs and derive crosses_midnight
        for (RoomSlot s : updatedSlots) {
            if (s.getListPrice() <= 0) {
                throw new IllegalArgumentException("Price must be greater than 0.");
            }
            if (s.getSlotName() == null || s.getSlotName().trim().isEmpty()) {
                throw new IllegalArgumentException("Slot name is required.");
            }
            if (s.getSlotName().trim().length() > 50) {
                throw new IllegalArgumentException("Slot name must not exceed 50 characters.");
            }
            if (s.getStartTime() == null || s.getEndTime() == null) {
                throw new IllegalArgumentException("Start time and end time are required.");
            }
            if (s.getStartTime().equals(s.getEndTime())) {
                throw new IllegalArgumentException("Start time and end time cannot be identical.");
            }

            boolean crosses = s.getEndTime().isBefore(s.getStartTime());
            s.setCrossesMidnight(crosses);
        }

        // 2. Validate non-overlapping intervals across all slots
        validateNoOverlap(updatedSlots);

        // 3. Under transaction: check W25 hold guard and update rows
        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Room room = roomDao.findById(roomId);
                if (room == null) {
                    throw new IllegalArgumentException("Room not found.");
                }

                List<RoomSlot> existingSlots = getRoomSlots(roomId);

                for (RoomSlot newSlot : updatedSlots) {
                    RoomSlot oldSlot = findMatchingSlot(existingSlots, newSlot.getRoomSlotId(), newSlot.getSlotCode());
                    if (oldSlot == null) {
                        throw new IllegalArgumentException("Unknown slot ID " + newSlot.getRoomSlotId());
                    }

                    // Guard W25: check if HELD booking slots exist in future
                    boolean timeOrActiveChanged = !newSlot.getStartTime().equals(oldSlot.getStartTime())
                            || !newSlot.getEndTime().equals(oldSlot.getEndTime())
                            || newSlot.isActive() != oldSlot.isActive();

                    if (timeOrActiveChanged) {
                        String holdCheckSql = "SELECT COUNT(*) FROM BOOKING_SLOT "
                                + "WHERE room_slot_id = ? AND hold_status = 'HELD' AND end_at > SYSDATETIME()";
                        try (PreparedStatement ps = conn.prepareStatement(holdCheckSql)) {
                            ps.setLong(1, oldSlot.getRoomSlotId());
                            try (ResultSet rs = ps.executeQuery()) {
                                if (rs.next() && rs.getInt(1) > 0) {
                                    throw new IllegalStateException("This slot has active holds and its time cannot be changed until they are released.");
                                }
                            }
                        }
                    }

                    // Update slot
                    String updateSql = "UPDATE ROOM_TIME_SLOT SET slot_name = ?, start_time = ?, end_time = ?, "
                            + "crosses_midnight = ?, list_price = ?, is_active = ?, status = ?, updated_by = ?, updated_at = SYSDATETIME() "
                            + "WHERE room_slot_id = ?";
                    try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                        ps.setString(1, newSlot.getSlotName().trim());
                        ps.setTime(2, Time.valueOf(newSlot.getStartTime()));
                        ps.setTime(3, Time.valueOf(newSlot.getEndTime()));
                        ps.setBoolean(4, newSlot.isCrossesMidnight());
                        ps.setBigDecimal(5, BigDecimal.valueOf(newSlot.getListPrice()));
                        ps.setBoolean(6, newSlot.isActive());
                        ps.setString(7, (newSlot.getStatus() != null && !newSlot.getStatus().trim().isEmpty()) ? newSlot.getStatus() : "AVAILABLE");
                        ps.setLong(8, userId);
                        ps.setLong(9, oldSlot.getRoomSlotId());
                        ps.executeUpdate();
                    }
                }

                // Update overall room status if specified
                if (roomStatus != null && !roomStatus.trim().isEmpty() && Room.ALLOWED_STATUSES.contains(roomStatus.trim().toUpperCase())) {
                    String updateRoomSql = "UPDATE ROOM SET status = ?, updated_at = SYSDATETIME() WHERE room_id = ?";
                    try (PreparedStatement ps = conn.prepareStatement(updateRoomSql)) {
                        ps.setString(1, roomStatus.trim().toUpperCase());
                        ps.setLong(2, roomId);
                        ps.executeUpdate();
                    }
                }

                LOGGER.info("Updated schedule and prices for room " + room.getRoomNumber());

                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                if (e instanceof IllegalStateException || e instanceof IllegalArgumentException) {
                    throw e;
                }
                throw new SQLException("Failed to update room time slots", e);
            }
        }
    }

    private void createDefaultSlots(Connection conn, long roomId, long userId) throws SQLException {
        String sql = "INSERT INTO ROOM_TIME_SLOT (room_id, slot_code, slot_name, start_time, end_time, "
                + "crosses_midnight, list_price, sort_order, is_active, updated_by, updated_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 1, ?, SYSDATETIME())";

        Object[][] defaults = new Object[][]{
            {"S1", "Morning", Time.valueOf(LocalTime.of(9, 0)), Time.valueOf(LocalTime.of(12, 0)), false, BigDecimal.valueOf(150000), 1},
            {"S2", "Afternoon", Time.valueOf(LocalTime.of(13, 0)), Time.valueOf(LocalTime.of(16, 0)), false, BigDecimal.valueOf(150000), 2},
            {"S3", "Evening", Time.valueOf(LocalTime.of(16, 30)), Time.valueOf(LocalTime.of(19, 30)), false, BigDecimal.valueOf(180000), 3},
            {"S4", "Overnight", Time.valueOf(LocalTime.of(20, 0)), Time.valueOf(LocalTime.of(9, 0)), true, BigDecimal.valueOf(300000), 4}
        };

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Object[] def : defaults) {
                ps.setLong(1, roomId);
                ps.setString(2, (String) def[0]);
                ps.setString(3, (String) def[1]);
                ps.setTime(4, (Time) def[2]);
                ps.setTime(5, (Time) def[3]);
                ps.setBoolean(6, (Boolean) def[4]);
                ps.setBigDecimal(7, (BigDecimal) def[5]);
                ps.setInt(8, (Integer) def[6]);
                ps.setLong(9, userId);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private static class MinuteInterval {
        final int start;
        final int end;
        final String slotCode;

        MinuteInterval(int start, int end, String slotCode) {
            this.start = start;
            this.end = end;
            this.slotCode = slotCode;
        }
    }

    private void validateNoOverlap(List<RoomSlot> slots) {
        List<MinuteInterval> intervals = new ArrayList<>();

        for (RoomSlot s : slots) {
            int startMin = s.getStartTime().getHour() * 60 + s.getStartTime().getMinute();
            int endMin = s.getEndTime().getHour() * 60 + s.getEndTime().getMinute();

            if (s.isCrossesMidnight()) {
                // Split into [startMin, 1440) and [0, endMin)
                intervals.add(new MinuteInterval(startMin, 1440, s.getSlotCode()));
                intervals.add(new MinuteInterval(0, endMin, s.getSlotCode()));
            } else {
                intervals.add(new MinuteInterval(startMin, endMin, s.getSlotCode()));
            }
        }

        // Compare all pairs of intervals belonging to DIFFERENT slots
        for (int i = 0; i < intervals.size(); i++) {
            for (int j = i + 1; j < intervals.size(); j++) {
                MinuteInterval a = intervals.get(i);
                MinuteInterval b = intervals.get(j);

                if (a.slotCode.equals(b.slotCode)) {
                    continue; // Same slot split parts
                }

                int overlapStart = Math.max(a.start, b.start);
                int overlapEnd = Math.min(a.end, b.end);

                if (overlapStart < overlapEnd) {
                    throw new IllegalArgumentException("Time slots must not overlap, including overnight ranges.");
                }
            }
        }
    }

    private RoomSlot findMatchingSlot(List<RoomSlot> list, long slotId, String slotCode) {
        for (RoomSlot s : list) {
            if (s.getRoomSlotId() == slotId) {
                return s;
            }
        }
        for (RoomSlot s : list) {
            if (s.getSlotCode().equalsIgnoreCase(slotCode)) {
                return s;
            }
        }
        return null;
    }
}
