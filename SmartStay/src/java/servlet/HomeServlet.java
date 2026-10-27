package servlet;

import dao.RoomDao;
import model.Room;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Public landing controller for the SmartStay homepage and contact information.
 */
@WebServlet(name = "HomeServlet", urlPatterns = {"/home", "", "/contact", "/terms", "/policy", "/house-rules"})
public class HomeServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(HomeServlet.class.getName());
    private static final String HOMESTAY_NAME = "SmartStay Lakeside";

    private final RoomDao roomDao = new RoomDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String path = req.getServletPath();
        if ("/contact".equals(path)) {
            req.getRequestDispatcher("/WEB-INF/views/publicsite/contact.jsp").forward(req, resp);
            return;
        }
        if ("/terms".equals(path) || "/policy".equals(path) || "/house-rules".equals(path)) {
            req.getRequestDispatcher("/WEB-INF/views/publicsite/terms.jsp").forward(req, resp);
            return;
        }

        req.setAttribute("homestayName", HOMESTAY_NAME);

        java.time.LocalDate today = util.ClockService.now().toLocalDate();
        java.time.LocalDate maxDate = today.plusDays(util.AppConstants.MAX_ADVANCE_BOOKING_DAYS);
        req.setAttribute("today", today);
        req.setAttribute("maxDate", maxDate);

        List<Room> showcaseRooms = null;
        try {
            showcaseRooms = roomDao.findAllShowcase();
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to load showcase rooms from database", e);
        }

        if (showcaseRooms == null || showcaseRooms.isEmpty()) {
            showcaseRooms = createFallbackRooms();
        }
        req.setAttribute("showcaseRooms", showcaseRooms);

        req.getRequestDispatcher("/WEB-INF/views/publicsite/home.jsp").forward(req, resp);
    }

    private List<Room> createFallbackRooms() {
        List<Room> list = new java.util.ArrayList<>();
        list.add(createRoom(1L, "A101", "Butter Beige", 1, 2, 259000L, 359000L, "assets/images/rooms/butter-beige/main.jpeg"));
        list.add(createRoom(2L, "A102", "Vanilla Zen", 1, 2, 299000L, 399000L, "assets/images/rooms/vanilla-zen/main.jpeg"));
        list.add(createRoom(3L, "A103", "Fresh Tale", 1, 3, 199000L, 299000L, "assets/images/rooms/fresh-tale/main.jpeg"));
        list.add(createRoom(4L, "A104", "Power Rush", 2, 2, 259000L, 359000L, "assets/images/rooms/power-rush/main.jpeg"));
        list.add(createRoom(5L, "A105", "Sunny Love", 2, 2, 249000L, 349000L, "assets/images/rooms/sunny-love/main.jpeg"));
        list.add(createRoom(6L, "A106", "Warm House", 2, 2, 249000L, 349000L, "assets/images/rooms/warm-house/main.png"));
        return list;
    }

    private Room createRoom(Long id, String number, String name, int floor, int cap, Long p3h, Long pOvernight, String img) {
        Room r = new Room();
        r.setRoomId(id);
        r.setRoomNumber(number);
        r.setDisplayName(name);
        r.setFloorNumber(floor);
        r.setCapacity(cap);
        r.setMinPrice(p3h);
        r.setOvernightPrice(pOvernight);
        r.setImageUrl(img);
        r.setStatus("AVAILABLE");
        r.setActive(true);
        return r;
    }
}
