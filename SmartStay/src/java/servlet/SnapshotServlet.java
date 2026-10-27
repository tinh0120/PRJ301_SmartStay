package servlet;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Set;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import util.SnapshotStore;

/**
 * GET /assets/images/snapshots/{name}.jpg - camera photos from util.SnapshotStore (outside the build folder).
 * Only signed-in users with SNAPSHOT_VIEW may open them (they used to be plain public static files).
 */
@WebServlet(name = "SnapshotServlet", urlPatterns = {"/assets/images/snapshots/*"})
public class SnapshotServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        Object perms = (session != null) ? session.getAttribute("permissions") : null;
        if (!(perms instanceof Set) || !((Set<?>) perms).contains("SNAPSHOT_VIEW")) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        String info = req.getPathInfo();
        File f = SnapshotStore.find(getServletContext(), SnapshotStore.URL_PREFIX + (info == null ? "" : info.replaceFirst("^/", "")));
        if (f == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        resp.setContentType("image/jpeg");
        resp.setHeader("Cache-Control", "private, max-age=86400");
        resp.setContentLengthLong(f.length());
        try (OutputStream out = resp.getOutputStream()) {
            Files.copy(f.toPath(), out);
        }
    }
}
