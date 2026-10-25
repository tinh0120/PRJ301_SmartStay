package servlet;

import model.User.AuthUser;
import model.Room;
import model.RoomSlot;
import service.RoomService;
import util.Flash;
import util.PageResult.PageRequest;
import util.PageResult;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;

/**
 * Administrative controller consolidating room listing, form editing, deletion, and slot schedules.
 */
@WebServlet(name = "RoomAdminServlet", urlPatterns = {"/admin/rooms", "/admin/rooms/edit", "/admin/rooms/delete", "/admin/rooms/slots"})
@MultipartConfig(
    fileSizeThreshold = 1024 * 1024,      // 1 MB
    maxFileSize = 5 * 1024 * 1024,        // 5 MB
    maxRequestSize = 25 * 1024 * 1024     // 25 MB
)
public class RoomAdminServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(RoomAdminServlet.class.getName());
    private static final String[] SORT_KEYS = {"number", "room", "floor", "capacity", "status", "price"};
    private static final String[] SLOT_CODES = {"S1", "S2", "S3", "S4"};
    private static final Set<String> ALLOWED_STATUSES = new HashSet<>(
            Arrays.asList("AVAILABLE", "OCCUPIED", "NEEDS_CLEANING", "MAINTENANCE")
    );

    private final RoomService RoomService = new RoomService();
    private final dao.RoomDao roomDao = new dao.RoomDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String path = req.getServletPath();
        if ("/admin/rooms".equals(path)) {
            listRooms(req, resp);
            return;
        }
        if ("/admin/rooms/edit".equals(path)) {
            showEditForm(req, resp);
            return;
        }
        if ("/admin/rooms/slots".equals(path)) {
            showSlots(req, resp);
            return;
        }
        resp.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String path = req.getServletPath();
        if ("/admin/rooms/edit".equals(path)) {
            saveRoom(req, resp);
            return;
        }
        if ("/admin/rooms/delete".equals(path)) {
            deleteRoom(req, resp);
            return;
        }
        if ("/admin/rooms/slots".equals(path)) {
            saveSlots(req, resp);
            return;
        }
        resp.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    private void listRooms(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        PageRequest pr = PageRequest.from(req, SORT_KEYS, "number", "ASC");
        try {
            PageResult<Room> pageResult = RoomService.findAdminPage(pr);
            req.setAttribute("pageResult", pageResult);
            req.setAttribute("pr", pr);
            try {
                req.setAttribute("roomCounts", new dao.DashboardDao().getRoomStatusCounts());
            } catch (Exception ignored) {}
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to load room list", e);
            req.setAttribute("dbError", "Failed to load rooms from database.");
        }
        req.getRequestDispatcher("/WEB-INF/views/admin/rooms.jsp").forward(req, resp);
    }

    private void showEditForm(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String roomIdStr = req.getParameter("roomId");
        Room room = null;

        if (roomIdStr != null && !roomIdStr.trim().isEmpty()) {
            try {
                long roomId = Long.parseLong(roomIdStr.trim());
                room = RoomService.getRoom(roomId);
                if (room == null) {
                    Flash.danger(req, "Room not found.");
                    resp.sendRedirect(req.getContextPath() + "/admin/rooms");
                    return;
                }
            } catch (NumberFormatException | SQLException e) {
                LOGGER.log(Level.WARNING, "Invalid room ID requested: " + roomIdStr, e);
                Flash.danger(req, "Invalid room ID.");
                resp.sendRedirect(req.getContextPath() + "/admin/rooms");
                return;
            }
        } else {
            room = new Room();
            room.setFloorNumber(1);
            room.setCapacity(2);
            room.setActive(true);
            room.setStatus("AVAILABLE");
        }

        populateFormAttributes(req, room);
        req.setAttribute("room", room);
        req.getRequestDispatcher("/WEB-INF/views/admin/room-form.jsp").forward(req, resp);
    }

    private void saveRoom(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        AuthUser authUser = (session != null) ? (AuthUser) session.getAttribute("authUser") : null;
        long userId = (authUser != null) ? authUser.getUserId() : 1L;

        String roomIdStr = req.getParameter("roomId");
        long roomId = 0;
        if (roomIdStr != null && !roomIdStr.trim().isEmpty()) {
            try {
                roomId = Long.parseLong(roomIdStr.trim());
            } catch (NumberFormatException ignored) {
            }
        }

        String roomNumber = req.getParameter("roomNumber");
        String displayName = req.getParameter("displayName");
        String floorStr = req.getParameter("floorNumber");
        String capacityStr = req.getParameter("capacity");
        String locationDesc = req.getParameter("locationDescription");
        String description = req.getParameter("description");
        String amenities = req.getParameter("amenities");
        String imageUrl = req.getParameter("imageUrl");
        String status = req.getParameter("status");
        boolean active = "on".equalsIgnoreCase(req.getParameter("active"))
                || "true".equalsIgnoreCase(req.getParameter("active"))
                || "1".equals(req.getParameter("active"));

        Map<String, String> errors = new HashMap<>();

        if (roomNumber == null || roomNumber.trim().isEmpty()) {
            errors.put("roomNumber", "Room number is required.");
        } else if (roomNumber.trim().length() > 20) {
            errors.put("roomNumber", "Room number must not exceed 20 characters.");
        }

        if (displayName == null || displayName.trim().isEmpty()) {
            displayName = util.ElFunctions.getDisplayName(roomNumber);
        } else if (displayName.trim().length() > 100) {
            errors.put("displayName", "Display name must not exceed 100 characters.");
        }

        int floorNumber = 0;
        if (floorStr == null || floorStr.trim().isEmpty()) {
            errors.put("floorNumber", "Floor number is required.");
        } else {
            try {
                floorNumber = Integer.parseInt(floorStr.trim());
                if (floorNumber < 0) {
                    errors.put("floorNumber", "Floor number must be an integer greater than or equal to 0.");
                }
            } catch (NumberFormatException e) {
                errors.put("floorNumber", "Floor number must be a valid number.");
            }
        }

        int capacity = 0;
        if (capacityStr == null || capacityStr.trim().isEmpty()) {
            errors.put("capacity", "Capacity is required.");
        } else {
            try {
                capacity = Integer.parseInt(capacityStr.trim());
                if (capacity < 1) {
                    errors.put("capacity", "Capacity must be an integer greater than or equal to 1.");
                }
            } catch (NumberFormatException e) {
                errors.put("capacity", "Capacity must be a valid number.");
            }
        }

        if (locationDesc != null && locationDesc.trim().length() > 255) {
            errors.put("locationDescription", "Location description must not exceed 255 characters.");
        }
        if (description != null && description.trim().length() > 500) {
            errors.put("description", "Description must not exceed 500 characters.");
        }
        if (amenities != null && amenities.trim().length() > 500) {
            errors.put("amenities", "Amenities must not exceed 500 characters.");
        }

        // Handle Image Upload (Servlet 3 Multipart)
        String slug = getRoomSlug(roomNumber);
        try {
            Part filePart = req.getPart("imageFile");
            if (filePart != null && filePart.getSize() > 0) {
                long size = filePart.getSize();
                if (size > 5 * 1024 * 1024) {
                    errors.put("imageFile", "Uploaded image exceeds 5MB limit.");
                } else {
                    String submittedName = filePart.getSubmittedFileName();
                    String ext = "";
                    if (submittedName != null && submittedName.lastIndexOf('.') >= 0) {
                        ext = submittedName.substring(submittedName.lastIndexOf('.')).toLowerCase();
                    }
                    if (!".jpg".equals(ext) && !".jpeg".equals(ext) && !".png".equals(ext) && !".webp".equals(ext)) {
                        errors.put("imageFile", "Only JPG, PNG, and WEBP image formats are supported.");
                    } else {
                        String realDir = req.getServletContext().getRealPath("/assets/images/rooms/" + slug);
                        if (realDir != null) {
                            File uploadDir = new File(realDir);
                            if (!uploadDir.exists()) {
                                uploadDir.mkdirs();
                            }
                            String safeFileName = System.currentTimeMillis() + "_" + submittedName.replaceAll("[^a-zA-Z0-9._-]", "_");
                            File destFile = new File(uploadDir, safeFileName);
                            try (InputStream in = filePart.getInputStream()) {
                                Files.copy(in, destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                            }
                            // Also mirror to source directory if available
                            String srcDir = "c:\\FPT\\PRJ301\\FinalProject\\PRJ301_GROUP8_SE2021_FA26\\SmartStay\\web\\assets\\images\\rooms\\" + slug;
                            File srcFolder = new File(srcDir);
                            if (srcFolder.exists()) {
                                try {
                                    Files.copy(destFile.toPath(), new File(srcFolder, safeFileName).toPath(), StandardCopyOption.REPLACE_EXISTING);
                                } catch (Exception ignored) {
                                }
                            }
                            boolean makeMain = "true".equalsIgnoreCase(req.getParameter("setAsMain")) || imageUrl == null || imageUrl.trim().isEmpty();
                            if (makeMain) {
                                imageUrl = "/assets/images/rooms/" + slug + "/" + safeFileName;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error processing uploaded image", e);
        }

        // Handle Image Deletion
        String deleteImageName = req.getParameter("deleteImageName");
        if (deleteImageName != null && !deleteImageName.trim().isEmpty()) {
            String cleanDel = new File(deleteImageName.trim()).getName();
            String realDir = req.getServletContext().getRealPath("/assets/images/rooms/" + slug);
            if (realDir != null) {
                File f = new File(realDir, cleanDel);
                if (f.exists()) f.delete();
            }
            String srcDir = "c:\\FPT\\PRJ301\\FinalProject\\PRJ301_GROUP8_SE2021_FA26\\SmartStay\\web\\assets\\images\\rooms\\" + slug;
            File sf = new File(srcDir, cleanDel);
            if (sf.exists()) sf.delete();
            if (imageUrl != null && imageUrl.contains(cleanDel)) {
                imageUrl = "/assets/images/rooms/" + slug + "/main.jpeg";
            }
        }

        // Parse Slot Prices
        Map<String, BigDecimal> slotPrices = new HashMap<>();
        for (String sc : SLOT_CODES) {
            String pStr = req.getParameter("slotPrice_" + sc);
            if (pStr != null && !pStr.trim().isEmpty()) {
                try {
                    long p = Long.parseLong(pStr.trim().replaceAll("[,.]", ""));
                    if (p <= 0) {
                        errors.put("slotPrice_" + sc, "Slot price must be greater than 0.");
                    } else {
                        slotPrices.put(sc, BigDecimal.valueOf(p));
                    }
                } catch (NumberFormatException e) {
                    errors.put("slotPrice_" + sc, "Invalid slot price.");
                }
            }
        }

        Room room = new Room();
        room.setRoomId(roomId);
        room.setRoomNumber(roomNumber != null ? roomNumber.trim() : "");
        room.setDisplayName(displayName != null ? displayName.trim() : null);
        room.setFloorNumber(floorNumber);
        room.setCapacity(capacity);
        room.setLocationDescription(locationDesc != null ? locationDesc.trim() : null);
        room.setDescription(description != null ? description.trim() : null);
        room.setAmenities(amenities != null ? amenities.trim() : null);
        room.setImageUrl(imageUrl != null ? imageUrl.trim() : null);
        room.setActive(active);

        if (status != null && ALLOWED_STATUSES.contains(status.trim().toUpperCase())) {
            room.setStatus(status.trim().toUpperCase());
        } else if (roomId > 0) {
            try {
                Room existing = RoomService.getRoom(roomId);
                if (existing != null && existing.getStatus() != null) {
                    room.setStatus(existing.getStatus());
                } else {
                    room.setStatus("AVAILABLE");
                }
            } catch (SQLException e) {
                room.setStatus("AVAILABLE");
            }
        } else {
            room.setStatus("AVAILABLE");
        }

        if (!errors.isEmpty()) {
            populateFormAttributes(req, room);
            req.setAttribute("errors", errors);
            req.setAttribute("room", room);
            req.getRequestDispatcher("/WEB-INF/views/admin/room-form.jsp").forward(req, resp);
            return;
        }

        try {
            long targetRoomId = roomId;
            if (roomId > 0) {
                RoomService.updateRoom(room, userId, req.getRemoteAddr());
                Flash.success(req, "Room " + room.getRoomNumber() + " updated successfully.");
            } else {
                targetRoomId = RoomService.createRoom(room, userId, req.getRemoteAddr());
                Flash.success(req, "Room " + room.getRoomNumber() + " created successfully.");
            }

            if (!slotPrices.isEmpty() && targetRoomId > 0) {
                roomDao.updateRoomSlotPrices(targetRoomId, slotPrices, userId);
            }

            resp.sendRedirect(req.getContextPath() + "/admin/rooms");
        } catch (IllegalArgumentException | IllegalStateException e) {
            populateFormAttributes(req, room);
            errors.put("general", e.getMessage());
            req.setAttribute("errors", errors);
            req.setAttribute("room", room);
            req.getRequestDispatcher("/WEB-INF/views/admin/room-form.jsp").forward(req, resp);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error saving room", e);
            populateFormAttributes(req, room);
            errors.put("general", "Database error occurred while saving the room.");
            req.setAttribute("errors", errors);
            req.setAttribute("room", room);
            req.getRequestDispatcher("/WEB-INF/views/admin/room-form.jsp").forward(req, resp);
        }
    }

    private void populateFormAttributes(HttpServletRequest req, Room room) {
        String slug = getRoomSlug(room.getRoomNumber());
        req.setAttribute("roomSlug", slug);

        // 1. Slot Prices
        Map<String, Long> slotPrices = new HashMap<>();
        if (room.getRoomId() > 0) {
            try {
                List<RoomSlot> slots = roomDao.findSlotsByRoomId(room.getRoomId());
                for (RoomSlot s : slots) {
                    if (s.getSlotCode() != null) {
                        slotPrices.put(s.getSlotCode().toUpperCase(), s.getListPrice());
                    }
                }
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Failed to load slots for room " + room.getRoomId(), e);
            }
        }
        if (!slotPrices.containsKey("S1")) slotPrices.put("S1", 150000L);
        if (!slotPrices.containsKey("S2")) slotPrices.put("S2", 150000L);
        if (!slotPrices.containsKey("S3")) slotPrices.put("S3", 180000L);
        if (!slotPrices.containsKey("S4")) slotPrices.put("S4", 300000L);
        req.setAttribute("slotPrices", slotPrices);

        // 2. Images & Gallery
        List<Map<String, Object>> imageList = new ArrayList<>();
        String realDir = req.getServletContext().getRealPath("/assets/images/rooms/" + slug);
        File dir = realDir != null ? new File(realDir) : null;
        if (dir != null && dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles((d, name) -> name.toLowerCase().matches(".*\\.(jpe?g|png|webp)$"));
            if (files != null) {
                Arrays.sort(files, (a, b) -> {
                    if (a.getName().startsWith("main.")) return -1;
                    if (b.getName().startsWith("main.")) return 1;
                    return a.getName().compareTo(b.getName());
                });
                for (File f : files) {
                    Map<String, Object> img = new HashMap<>();
                    String relPath = "/assets/images/rooms/" + slug + "/" + f.getName();
                    img.put("name", f.getName());
                    img.put("url", req.getContextPath() + relPath);
                    boolean isMain = (room.getImageUrl() != null && room.getImageUrl().contains(f.getName()))
                            || (room.getImageUrl() == null && f.getName().startsWith("main."));
                    img.put("isMain", isMain);
                    imageList.add(img);
                }
            }
        }

        // Fallback to gallery definitions if files not found in realDir
        if (imageList.isEmpty() && room.getRoomNumber() != null && !room.getRoomNumber().trim().isEmpty()) {
            List<util.ElFunctions.GalleryImage> gallery = util.ElFunctions.getGalleryImages(room.getRoomNumber(), room.getDisplayName());
            for (util.ElFunctions.GalleryImage gi : gallery) {
                Map<String, Object> img = new HashMap<>();
                String relUrl = gi.getUrl();
                String fName = relUrl.substring(relUrl.lastIndexOf('/') + 1);
                img.put("name", fName);
                img.put("url", req.getContextPath() + "/" + relUrl);
                img.put("isMain", gi.isMain());
                imageList.add(img);
            }
        }
        req.setAttribute("galleryImages", imageList);
    }

    public static String getRoomSlug(String roomNumber) {
        if (roomNumber == null || roomNumber.trim().isEmpty()) {
            return "room";
        }
        String num = roomNumber.trim().toUpperCase();
        switch (num) {
            case "A101": return "butter-beige";
            case "A102": return "vanilla-zen";
            case "A103": return "fresh-tale";
            case "A104": return "power-rush";
            case "A105": return "sunny-love";
            case "A106": return "warm-house";
            default: return roomNumber.trim().toLowerCase().replaceAll("[^a-z0-9]+", "-");
        }
    }

    private void deleteRoom(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        AuthUser authUser = (session != null) ? (AuthUser) session.getAttribute("authUser") : null;
        long userId = (authUser != null) ? authUser.getUserId() : 1L;

        String roomIdStr = req.getParameter("roomId");
        if (roomIdStr == null || roomIdStr.trim().isEmpty()) {
            Flash.danger(req, "Room ID is required.");
            resp.sendRedirect(req.getContextPath() + "/admin/rooms");
            return;
        }

        try {
            long roomId = Long.parseLong(roomIdStr.trim());
            RoomService.deleteRoom(roomId, userId, req.getRemoteAddr());
            Flash.success(req, "Room deleted successfully.");
        } catch (NumberFormatException e) {
            Flash.danger(req, "Invalid room ID.");
        } catch (IllegalStateException | IllegalArgumentException e) {
            Flash.danger(req, e.getMessage());
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to delete room", e);
            Flash.danger(req, "Database error occurred while deleting room.");
        }

        resp.sendRedirect(req.getContextPath() + "/admin/rooms");
    }

    private void showSlots(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String roomIdStr = req.getParameter("roomId");
        if (roomIdStr == null || roomIdStr.trim().isEmpty()) {
            Flash.danger(req, "Room ID is required.");
            resp.sendRedirect(req.getContextPath() + "/admin/rooms");
            return;
        }

        try {
            long roomId = Long.parseLong(roomIdStr.trim());
            Room room = RoomService.getRoom(roomId);
            if (room == null) {
                Flash.danger(req, "Room not found.");
                resp.sendRedirect(req.getContextPath() + "/admin/rooms");
                return;
            }

            List<RoomSlot> slots = RoomService.getRoomSlots(roomId);
            req.setAttribute("room", room);
            req.setAttribute("slots", slots);
            req.getRequestDispatcher("/WEB-INF/views/admin/room-slots.jsp").forward(req, resp);
        } catch (NumberFormatException e) {
            Flash.danger(req, "Invalid room ID.");
            resp.sendRedirect(req.getContextPath() + "/admin/rooms");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error loading room slots", e);
            Flash.danger(req, "Failed to load room time slots.");
            resp.sendRedirect(req.getContextPath() + "/admin/rooms");
        }
    }

    private void saveSlots(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        AuthUser authUser = (session != null) ? (AuthUser) session.getAttribute("authUser") : null;
        long userId = (authUser != null) ? authUser.getUserId() : 1L;

        String roomIdStr = req.getParameter("roomId");
        if (roomIdStr == null || roomIdStr.trim().isEmpty()) {
            Flash.danger(req, "Room ID is required.");
            resp.sendRedirect(req.getContextPath() + "/admin/rooms");
            return;
        }

        long roomId;
        Room room;
        try {
            roomId = Long.parseLong(roomIdStr.trim());
            room = RoomService.getRoom(roomId);
            if (room == null) {
                Flash.danger(req, "Room not found.");
                resp.sendRedirect(req.getContextPath() + "/admin/rooms");
                return;
            }
        } catch (NumberFormatException | SQLException e) {
            Flash.danger(req, "Invalid room ID.");
            resp.sendRedirect(req.getContextPath() + "/admin/rooms");
            return;
        }

        String roomStatus = req.getParameter("roomStatus");
        List<RoomSlot> slots = parseSlotsFromRequest(req, roomId);
        if (slots == null || slots.size() != 4) {
            Flash.danger(req, "Invalid slot submission format.");
            req.setAttribute("room", room);
            req.setAttribute("slots", slots != null ? slots : new ArrayList<RoomSlot>());
            req.getRequestDispatcher("/WEB-INF/views/admin/room-slots.jsp").forward(req, resp);
            return;
        }

        try {
            RoomService.updateRoomSlots(roomId, slots, roomStatus, userId, req.getRemoteAddr());
            Flash.success(req, "Room status and time slots updated successfully.");
            resp.sendRedirect(req.getContextPath() + "/admin/rooms/slots?roomId=" + roomId);
        } catch (IllegalStateException | IllegalArgumentException e) {
            req.setAttribute("errorMessage", e.getMessage());
            req.setAttribute("room", room);
            req.setAttribute("slots", slots);
            req.getRequestDispatcher("/WEB-INF/views/admin/room-slots.jsp").forward(req, resp);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to update room slots", e);
            req.setAttribute("errorMessage", "A database error occurred while updating slots.");
            req.setAttribute("room", room);
            req.setAttribute("slots", slots);
            req.getRequestDispatcher("/WEB-INF/views/admin/room-slots.jsp").forward(req, resp);
        }
    }

    private List<RoomSlot> parseSlotsFromRequest(HttpServletRequest req, long roomId) {
        String[] startArr = req.getParameterValues("startTime");
        String[] endArr = req.getParameterValues("endTime");
        String[] nameArr = req.getParameterValues("slotName");
        String[] priceArr = req.getParameterValues("price");
        String[] slotIdArr = req.getParameterValues("slotId");
        String[] codeArr = req.getParameterValues("slotCode");

        if (startArr != null && startArr.length == 4 && endArr != null && endArr.length == 4) {
            List<RoomSlot> list = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                RoomSlot s = new RoomSlot();
                s.setRoomId(roomId);
                if (slotIdArr != null && slotIdArr.length > i) {
                    try {
                        s.setRoomSlotId(Long.parseLong(slotIdArr[i].trim()));
                    } catch (NumberFormatException ignored) {}
                }

                String code = (codeArr != null && codeArr.length > i) ? codeArr[i].trim() : SLOT_CODES[i];
                s.setSlotCode(code);
                s.setSortOrder(i + 1);

                String name = (nameArr != null && nameArr.length > i) ? nameArr[i].trim() : "";
                s.setSlotName(name);

                try {
                    if (startArr[i] != null) s.setStartTime(LocalTime.parse(startArr[i].trim()));
                    if (endArr[i] != null) s.setEndTime(LocalTime.parse(endArr[i].trim()));
                } catch (DateTimeParseException ignored) {}

                try {
                    if (priceArr != null && priceArr.length > i) s.setListPrice(Long.parseLong(priceArr[i].trim()));
                } catch (NumberFormatException ignored) {}

                String actParam = req.getParameter("active_" + code);
                if (actParam == null) {
                    actParam = req.getParameter("active_" + (i + 1));
                }
                s.setActive("on".equalsIgnoreCase(actParam) || "true".equalsIgnoreCase(actParam) || "1".equals(actParam));

                String statusParam = req.getParameter("status_" + code);
                if (statusParam == null) {
                    statusParam = req.getParameter("status_" + (i + 1));
                }
                if (statusParam != null && !statusParam.trim().isEmpty()) {
                    s.setStatus(statusParam.trim().toUpperCase());
                } else {
                    s.setStatus("AVAILABLE");
                }

                if (s.getStartTime() != null && s.getEndTime() != null) {
                    s.setCrossesMidnight(s.getEndTime().isBefore(s.getStartTime()));
                }
                list.add(s);
            }
            return list;
        }

        List<RoomSlot> list = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            String code = SLOT_CODES[i];
            RoomSlot s = new RoomSlot();
            s.setRoomId(roomId);
            s.setSlotCode(code);
            s.setSortOrder(i + 1);

            String idStr = req.getParameter("slotId_" + code);
            if (idStr == null) idStr = req.getParameter("slotId_" + (i + 1));
            if (idStr == null && slotIdArr != null && slotIdArr.length > i) {
                idStr = slotIdArr[i];
            }
            try {
                if (idStr != null) s.setRoomSlotId(Long.parseLong(idStr.trim()));
            } catch (NumberFormatException ignored) {}

            String name = req.getParameter("slotName_" + code);
            if (name == null) name = req.getParameter("slotName_" + (i + 1));
            s.setSlotName(name != null ? name.trim() : "");

            String startStr = req.getParameter("startTime_" + code);
            if (startStr == null) startStr = req.getParameter("startTime_" + (i + 1));
            String endStr = req.getParameter("endTime_" + code);
            if (endStr == null) endStr = req.getParameter("endTime_" + (i + 1));

            try {
                if (startStr != null && !startStr.trim().isEmpty()) s.setStartTime(LocalTime.parse(startStr.trim()));
                if (endStr != null && !endStr.trim().isEmpty()) s.setEndTime(LocalTime.parse(endStr.trim()));
            } catch (DateTimeParseException ignored) {}

            String priceStr = req.getParameter("price_" + code);
            if (priceStr == null) priceStr = req.getParameter("price_" + (i + 1));
            try {
                if (priceStr != null && !priceStr.trim().isEmpty()) s.setListPrice(Long.parseLong(priceStr.trim()));
            } catch (NumberFormatException ignored) {}

            String actStr = req.getParameter("active_" + code);
            if (actStr == null) actStr = req.getParameter("active_" + (i + 1));
            s.setActive("on".equalsIgnoreCase(actStr) || "true".equalsIgnoreCase(actStr) || "1".equals(actStr));

            String statusStr = req.getParameter("status_" + code);
            if (statusStr == null) statusStr = req.getParameter("status_" + (i + 1));
            if (statusStr != null && !statusStr.trim().isEmpty()) {
                s.setStatus(statusStr.trim().toUpperCase());
            } else {
                s.setStatus("AVAILABLE");
            }

            if (s.getStartTime() != null && s.getEndTime() != null) {
                s.setCrossesMidnight(s.getEndTime().isBefore(s.getStartTime()));
            }
            list.add(s);
        }
        return list;
    }
}
