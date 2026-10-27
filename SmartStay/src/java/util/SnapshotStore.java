package util;

import java.io.File;
import java.util.regex.Pattern;
import javax.servlet.ServletContext;

/**
 * Camera snapshots live OUTSIDE the deployed web folder so "Clean and Build" never deletes them.
 * Folder: context-param "snapshot.dir" in web.xml (default C:/SmartStay/snapshots).
 *
 * The database keeps the same relative path as before ("assets/images/snapshots/cam_x.jpg");
 * servlet.SnapshotServlet serves that URL from this folder, falling back to an old file that
 * still sits inside the web app.
 */
public final class SnapshotStore {

    public static final String URL_PREFIX = "assets/images/snapshots/";
    public static final String DEFAULT_DIR = "C:/SmartStay/snapshots";
    private static final Pattern SAFE_NAME = Pattern.compile("^[A-Za-z0-9_.-]{1,100}\\.(jpg|jpeg)$");

    private SnapshotStore() {
    }

    /** The storage folder (created on first use). */
    public static File dir(ServletContext ctx) {
        String configured = ctx == null ? null : ctx.getInitParameter("snapshot.dir");
        File d = new File(configured == null || configured.trim().isEmpty() ? DEFAULT_DIR : configured.trim());
        if (!d.isDirectory()) {
            d.mkdirs();
        }
        return d;
    }

    /**
     * File name from a stored path such as "assets/images/snapshots/cam_1.jpg" (or "/..."), or null when
     * the path is outside the snapshot URL or the name is not a plain .jpg name (no "..", no sub folders).
     */
    public static String fileName(String path) {
        if (path == null) {
            return null;
        }
        String clean = path.trim().replace('\\', '/');
        while (clean.startsWith("/")) {
            clean = clean.substring(1);
        }
        if (!clean.startsWith(URL_PREFIX)) {
            return null;
        }
        String name = clean.substring(URL_PREFIX.length());
        return SAFE_NAME.matcher(name).matches() && !name.contains("..") ? name : null;
    }

    /** Existing file for a stored path: the snapshot folder first, then the legacy copy inside the web app. */
    public static File find(ServletContext ctx, String path) {
        String name = fileName(path);
        if (name == null) {
            return null;
        }
        File f = new File(dir(ctx), name);
        if (f.isFile()) {
            return f;
        }
        String legacy = ctx == null ? null : ctx.getRealPath("/" + URL_PREFIX + name);
        if (legacy != null && new File(legacy).isFile()) {
            return new File(legacy);
        }
        return null;
    }

    public static boolean exists(ServletContext ctx, String path) {
        return find(ctx, path) != null;
    }
}
