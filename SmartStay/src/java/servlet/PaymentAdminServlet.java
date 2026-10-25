package servlet;

import model.User.AuthUser;
import model.Payment;
import service.ServiceException;
import service.PaymentService;
import util.Flash;
import util.PageResult.PageRequest;
import util.PageResult;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Administrative controller consolidating payment transaction listing and dispute/review resolution.
 */
@WebServlet(name = "PaymentAdminServlet", urlPatterns = {"/admin/payments", "/admin/payments/resolve"})
public class PaymentAdminServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(PaymentAdminServlet.class.getName());
    private static final String[] SORT_KEYS = {"created", "amount", "status", "paid_at", "order"};

    private final PaymentService paymentService = new PaymentService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String path = req.getServletPath();
        if ("/admin/payments".equals(path)) {
            listPayments(req, resp);
            return;
        }
        if ("/admin/payments/resolve".equals(path)) {
            resp.sendRedirect(req.getContextPath() + "/admin/payments");
            return;
        }
        resp.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String path = req.getServletPath();
        if ("/admin/payments/resolve".equals(path)) {
            resolvePayment(req, resp);
            return;
        }
        resp.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    private void listPayments(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        PageRequest pr = PageRequest.from(req, SORT_KEYS, "created", "DESC");

        String q = req.getParameter("q");
        if (q != null && !q.trim().isEmpty() && pr.getKeyword().isEmpty()) {
            pr.setKeyword(q.trim());
        }

        try {
            PageResult<Payment> pageResult = paymentService.findPage(pr);
            req.setAttribute("pageResult", pageResult);
            req.setAttribute("pr", pr);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to load payment transactions", e);
            req.setAttribute("dbError", "Failed to load payments from database.");
        }

        req.getRequestDispatcher("/WEB-INF/views/admin/payments.jsp").forward(req, resp);
    }

    private void resolvePayment(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        AuthUser authUser = (session != null) ? (AuthUser) session.getAttribute("authUser") : null;
        long adminUserId = (authUser != null) ? authUser.getUserId() : 1L;

        String paymentIdStr = req.getParameter("paymentId");
        long paymentId = -1;
        if (paymentIdStr != null && !paymentIdStr.trim().isEmpty()) {
            try {
                paymentId = Long.parseLong(paymentIdStr.trim());
            } catch (NumberFormatException ignored) {
            }
        }

        if (paymentId <= 0) {
            Flash.error(req, "Invalid payment ID.");
            resp.sendRedirect(req.getContextPath() + "/admin/payments");
            return;
        }

        String action = req.getParameter("action");
        if (action == null || action.trim().isEmpty()) {
            Flash.error(req, "Action parameter is required (refund_pending or dismiss).");
            resp.sendRedirect(req.getContextPath() + "/admin/payments");
            return;
        }

        String note = req.getParameter("note");
        if (note == null || note.trim().isEmpty()) {
            Flash.error(req, "Resolution note is required.");
            resp.sendRedirect(req.getContextPath() + "/admin/payments");
            return;
        }

        String returnUrl = req.getParameter("returnUrl");
        if (returnUrl == null || returnUrl.trim().isEmpty() || !returnUrl.startsWith("/")) {
            returnUrl = req.getContextPath() + "/admin/payments";
        }

        try {
            paymentService.resolvePayment(adminUserId, paymentId, action.trim(), note.trim(), req.getRemoteAddr());
            Flash.success(req, "Payment #" + paymentId + " successfully resolved.");
            resp.sendRedirect(returnUrl);
        } catch (ServiceException pe) {
            Flash.error(req, pe.getMessage());
            resp.sendRedirect(returnUrl);
        } catch (SQLException se) {
            LOGGER.log(Level.SEVERE, "Database error resolving payment " + paymentId, se);
            Flash.error(req, "Failed to resolve payment due to a database error.");
            resp.sendRedirect(returnUrl);
        }
    }
}
