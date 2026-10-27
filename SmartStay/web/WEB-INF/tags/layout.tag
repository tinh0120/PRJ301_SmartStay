<%@ tag pageEncoding="UTF-8" body-content="scriptless" %>
<%@ attribute name="title" required="true" %>
<%@ attribute name="active" required="false" %>
<%@ attribute name="fullWidth" required="false" %>
<%@ taglib prefix="e" uri="/WEB-INF/tld/smartstay.tld" %>
<%@ tag import="java.util.List, util.Flash, util.ElFunctions" %>
<%
    javax.servlet.jsp.PageContext pc = (javax.servlet.jsp.PageContext) jspContext;
    javax.servlet.http.HttpServletRequest req = (javax.servlet.http.HttpServletRequest) pc.getRequest();
    javax.servlet.http.HttpSession sess = req.getSession(false);
    Object userObj = (sess != null) ? sess.getAttribute("authUser") : null;
    String userRole = (sess != null) ? (String) sess.getAttribute("roleName") : null;
    java.util.Set<?> perms = (sess != null && sess.getAttribute("permissions") instanceof java.util.Set)
            ? (java.util.Set<?>) sess.getAttribute("permissions") : null;
    int newAlertsCount = 0;
    if (userObj != null && perms != null && perms.contains("ALERT_VIEW")) {
        try {
            newAlertsCount = new dao.AlertLogDao().countNewAlerts();
        } catch (Exception ignored) {
        }
    }
    // V2: original URL (a forwarded request reports the JSP path) decides the guest / back-office area
    Object fwdUri = request.getAttribute("javax.servlet.forward.request_uri");
    String areaUri = (fwdUri != null) ? fwdUri.toString() : request.getRequestURI();
    boolean isBackOfficeArea = areaUri.startsWith(request.getContextPath() + "/admin") || areaUri.startsWith(request.getContextPath() + "/staff");
    boolean isAdminOrOwner = "ADMIN".equals(userRole) || "OWNER".equals(userRole);
    boolean isOperationsActive = "dashboard".equals(active) || "admin-bookings".equals(active)
            || "admin-payments".equals(active) || "admin-assignments".equals(active) || "admin-rooms".equals(active);
    boolean isSecurityActive = "devices".equals(active) || "access-logs".equals(active)
            || "motion-logs".equals(active) || "activity".equals(active)
            || "alerts".equals(active) || "thresholds".equals(active);
    boolean isManagementActive = "admin-users".equals(active) || "reviews".equals(active)
            || "revenue".equals(active) || "system-tables".equals(active);

    // Demo tools (Task R2-B3): banner while the virtual clock is shifted, drawer for guest/owner/admin.
    boolean demoMode = filter.DemoModeFilter.isDemoMode(pc.getServletContext());
    boolean showDemoDrawer = demoMode && userObj != null && ("GUEST".equals(userRole) || isAdminOrOwner);
    boolean demoShifted = demoMode && util.ClockService.isShifted();
    req.setAttribute("ssDemoDrawer", showDemoDrawer);
    String demoCheckoutBooking = "";
    String demoPageBooking = "";
    if (showDemoDrawer) {
        Object reqBookingCode = req.getAttribute("bookingCode");
        if ("/mock-payos/checkout".equals(req.getAttribute("javax.servlet.forward.servlet_path"))
                && reqBookingCode instanceof String) {
            demoCheckoutBooking = (String) reqBookingCode;
        }
        Object reqDetail = req.getAttribute("detail");
        if (!(reqDetail instanceof model.BookingDetail)) {
            reqDetail = req.getAttribute("bookingDetail");
        }
        if (reqDetail instanceof model.BookingDetail && ((model.BookingDetail) reqDetail).getBooking() != null) {
            demoPageBooking = ((model.BookingDetail) reqDetail).getBooking().getBookingCode();
        } else if (reqBookingCode instanceof String) {
            demoPageBooking = (String) reqBookingCode;
        }
        if (demoPageBooking == null) {
            demoPageBooking = "";
        }
    }
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="ctx" content="${pageContext.request.contextPath}">
    <title>${e:esc(title)} | Tini SmartStay</title>
    <link rel="icon" type="image/png" href="${pageContext.request.contextPath}/assets/images/branding/tini-smartstay-favicon-96.png">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/vendor/bootstrap/css/bootstrap.min.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/vendor/bootstrap-icons/bootstrap-icons.min.css">
    <%-- V4: Flatpickr (CSS + JS) is loaded by datepicker.js only on pages with a [data-datepicker] field --%>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/app.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/animations.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/modern-ui.css">
</head>
<body class="d-flex flex-column min-vh-100 <%= isBackOfficeArea ? "ss-area-admin" : "ss-area-guest" %>">
    <a class="ss-skip-link" href="#main-content">Skip to main content</a>
    <nav class="navbar navbar-expand-lg navbar-dark bg-primary shadow-sm sticky-top main-navbar">
        <div class="container">
            <a class="navbar-brand brand-block" href="${pageContext.request.contextPath}/">
                <span class="brand-mark">
                    <img src="${pageContext.request.contextPath}/assets/images/branding/tini-smartstay-logo-sm.png" width="198" height="132"
                         alt="" aria-hidden="true">
                </span>
                <span class="brand-wordmark">
                    <span class="brand-wordmark-top">Tini</span>
                    <span class="brand-wordmark-bottom">SmartStay</span>
                </span>
                <span class="visually-hidden">Tini SmartStay</span>
            </a>
            <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navbarMain"
                    aria-controls="navbarMain" aria-expanded="false" aria-label="Toggle navigation">
                <span class="navbar-toggler-icon"></span>
            </button>
            <div class="collapse navbar-collapse" id="navbarMain">
                <ul class="navbar-nav me-auto mb-2 mb-lg-0">
                    <li class="nav-item">
                        <a class="nav-link ${active == 'home' ? 'active' : ''}"
                           ${active == 'home' ? 'aria-current="page"' : ''}
                           href="${pageContext.request.contextPath}/">Home</a>
                    </li>
                    <% if (!isAdminOrOwner) { %>
                        <li class="nav-item">
                            <a class="nav-link ${active == 'rooms' ? 'active' : ''}"
                               ${active == 'rooms' ? 'aria-current="page"' : ''}
                               href="${pageContext.request.contextPath}/rooms">Rooms</a>
                        </li>
                    <% } %>
                    <li class="nav-item">
                        <a class="nav-link ${active == 'contact' ? 'active' : ''}"
                           ${active == 'contact' ? 'aria-current="page"' : ''}
                           href="${pageContext.request.contextPath}/contact">Contact</a>
                    </li>
                    <% if ("GUEST".equals(userRole)) { %>
                        <li class="nav-item">
                            <a class="nav-link ${active == 'bookings' ? 'active' : ''}"
                               ${active == 'bookings' ? 'aria-current="page"' : ''}
                               href="${pageContext.request.contextPath}/guest/bookings">My Bookings</a>
                        </li>
                    <% } else if ("STAFF".equals(userRole)) { %>
                        <li class="nav-item">
                            <a class="nav-link ${active == 'assignments' ? 'active' : ''}"
                               ${active == 'assignments' ? 'aria-current="page"' : ''}
                               href="${pageContext.request.contextPath}/staff/assignments">My Assignments</a>
                        </li>
                    <% } else if (isAdminOrOwner) { %>
                        <!-- Operations Dropdown -->
                        <li class="nav-item dropdown">
                            <a class="nav-link dropdown-toggle <%= isOperationsActive ? "active" : "" %>"
                               href="#" id="navOperations" role="button" data-bs-toggle="dropdown"
                               aria-expanded="false" <%= isOperationsActive ? "aria-current=\"page\"" : "" %>>
                                Operations
                            </a>
                            <ul class="dropdown-menu" aria-labelledby="navOperations">
                                <li>
                                    <a class="dropdown-item ${active == 'dashboard' ? 'active' : ''}"
                                       href="${pageContext.request.contextPath}/admin/dashboard">Dashboard</a>
                                </li>
                                <% if (perms != null && (perms.contains("BOOKING_MANAGE") || perms.contains("BOOKING_VIEW_ALL"))) { %>
                                    <li>
                                        <a class="dropdown-item ${active == 'admin-bookings' ? 'active' : ''}"
                                           href="${pageContext.request.contextPath}/admin/bookings">Bookings</a>
                                    </li>
                                <% } %>
                                <% if (perms != null && perms.contains("ASSIGNMENT_MANAGE")) { %>
                                    <li>
                                        <a class="dropdown-item ${active == 'admin-assignments' ? 'active' : ''}"
                                           href="${pageContext.request.contextPath}/admin/assignments">Assignments</a>
                                    </li>
                                <% } %>
                                <% if (perms != null && perms.contains("ROOM_MANAGE")) { %>
                                    <li>
                                        <a class="dropdown-item ${active == 'admin-rooms' ? 'active' : ''}"
                                           href="${pageContext.request.contextPath}/admin/rooms">Rooms</a>
                                    </li>
                                <% } %>
                            </ul>
                        </li>

                        <!-- Security & Devices Dropdown -->
                        <li class="nav-item dropdown">
                            <a class="nav-link dropdown-toggle <%= isSecurityActive ? "active" : "" %>"
                               href="#" id="navSecurity" role="button" data-bs-toggle="dropdown"
                               aria-expanded="false" <%= isSecurityActive ? "aria-current=\"page\"" : "" %>>
                                Security &amp; Devices
                                <span class="badge rounded-pill bg-danger ms-1" id="navAlertBadge" style="<%= newAlertsCount > 0 ? "" : "display:none;" %>"><%= newAlertsCount %></span>
                            </a>
                            <ul class="dropdown-menu" aria-labelledby="navSecurity">
                                <% if (perms != null && perms.contains("DEVICE_MANAGE")) { %>
                                    <li>
                                        <a class="dropdown-item ${active == 'devices' ? 'active' : ''}"
                                           href="${pageContext.request.contextPath}/admin/devices">Devices</a>
                                    </li>
                                <% } %>
                                <% if (perms != null && (perms.contains("LOG_ACCESS_VIEW") || perms.contains("LOG_MOTION_VIEW"))) { %>
                                    <%-- R9: Access + Motion logs merged into one page with tabs --%>
                                    <li>
                                        <a class="dropdown-item ${active == 'activity' ? 'active' : ''}"
                                           href="${pageContext.request.contextPath}/admin/activity">Activity logs</a>
                                    </li>
                                <% } %>
                                <% if (perms != null && perms.contains("ALERT_VIEW")) { %>
                                    <li>
                                        <a class="dropdown-item ${active == 'alerts' ? 'active' : ''}"
                                           href="${pageContext.request.contextPath}/admin/alerts">
                                            Alerts
                                            <span class="badge rounded-pill bg-danger ms-1" data-alert-count style="<%= newAlertsCount > 0 ? "" : "display:none;" %>"><%= newAlertsCount %></span>
                                        </a>
                                    </li>
                                <% } %>
                            </ul>
                        </li>

                        <!-- Management Dropdown -->
                        <li class="nav-item dropdown">
                            <a class="nav-link dropdown-toggle <%= isManagementActive ? "active" : "" %>"
                               href="#" id="navManagement" role="button" data-bs-toggle="dropdown"
                               aria-expanded="false" <%= isManagementActive ? "aria-current=\"page\"" : "" %>>
                                Management
                            </a>
                            <ul class="dropdown-menu" aria-labelledby="navManagement">
                                <% if (perms != null && perms.contains("USER_MANAGE")) { %>
                                    <li>
                                        <a class="dropdown-item ${active == 'admin-users' ? 'active' : ''}"
                                           href="${pageContext.request.contextPath}/admin/users">Users</a>
                                    </li>
                                <% } %>
                                <% if (perms != null && perms.contains("REVIEW_MODERATE")) { %>
                                    <li>
                                        <a class="dropdown-item ${active == 'reviews' ? 'active' : ''}"
                                           href="${pageContext.request.contextPath}/admin/reviews">Reviews</a>
                                    </li>
                                <% } %>
                                <% if (perms != null && perms.contains("REPORT_REVENUE")) { %>
                                    <li>
                                        <a class="dropdown-item ${active == 'revenue' ? 'active' : ''}"
                                           href="${pageContext.request.contextPath}/admin/reports/revenue">Revenue</a>
                                    </li>
                                <% } %>
                            </ul>
                        </li>
                    <% } %>
                </ul>
                <%
                    if (userObj == null) {
                %>
                    <ul class="navbar-nav ms-auto mb-2 mb-lg-0 align-items-lg-center gap-2">
                        <li class="nav-item">
                            <a class="btn btn-nav-auth ${active == 'login' ? 'active' : ''}"
                               href="${pageContext.request.contextPath}/login">Login</a>
                        </li>
                        <li class="nav-item">
                            <a class="btn btn-nav-auth ${active == 'register' ? 'active' : ''}"
                               href="${pageContext.request.contextPath}/register">Register</a>
                        </li>
                    </ul>
                <%
                    } else {
                %>
                    <ul class="navbar-nav ms-auto mb-2 mb-lg-0 align-items-lg-center gap-2">
                        <li class="nav-item">
                            <div class="nav-user-pill">
                                <span class="nav-user-avatar">
                                    <i class="bi bi-person-fill"></i>
                                </span>
                                <div class="nav-user-details">
                                    <span class="nav-user-greeting">Signed in as</span>
                                    <span class="nav-user-name">${e:esc(sessionScope.authUser.fullName)}</span>
                                </div>
                            </div>
                        </li>
                        <li class="nav-item">
                            <form action="${pageContext.request.contextPath}/logout" method="post" class="d-inline m-0">
                                <input type="hidden" name="_csrf" value="${e:esc(not empty _csrf ? _csrf : sessionScope._csrf)}">
                                <button type="submit" class="btn btn-nav-logout" title="Sign out of your account">
                                    <i class="bi bi-box-arrow-right"></i>
                                    <span>Logout</span>
                                </button>
                            </form>
                        </li>
                    </ul>
                <%
                    }
                %>
            </div>
        </div>
    </nav>

    <% if (demoShifted) { %>
    <div class="ss-demo-banner" id="ssDemoBanner" role="status" data-offset-ms="<%= util.ClockService.offsetMillis() %>">
        <div class="container ss-demo-banner__inner">
            <i class="bi bi-clock-history ss-demo-banner__icon" aria-hidden="true"></i>
            <span>Simulated time
                <span class="ss-demo-banner__time" id="ssDemoBannerTime"><%= util.ClockService.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) %></span></span>
            <% if (showDemoDrawer) { %>
                <span aria-hidden="true">&middot;</span>
                <button type="button" class="ss-demo-banner__reset" data-ss-demo-reset>Reset</button>
            <% } %>
        </div>
    </div>
    <% } %>

    <%
        List<Flash.Message> flashMessages = Flash.consume(req);
        if (flashMessages != null && !flashMessages.isEmpty()) {
    %>
    <div class="container mt-3 flash-container">
        <%
                for (Flash.Message flashMsg : flashMessages) {
                    jspContext.setAttribute("currentFlash", flashMsg);
        %>
            <div class="alert alert-${e:esc(currentFlash.type)} flash-alert alert-dismissible fade show d-flex align-items-center py-2 px-3 shadow-sm rounded-3 mb-2 ${active == 'admin-rooms' ? 'ss-toast ss-toast--'.concat(currentFlash.type == 'danger' ? 'error' : e:esc(currentFlash.type)) : ''}" role="alert">
                <% if ("success".equalsIgnoreCase(flashMsg.getType())) { %>
                    <i class="bi bi-check-circle-fill fs-5 text-success me-2 flex-shrink-0"></i>
                <% } else if ("danger".equalsIgnoreCase(flashMsg.getType()) || "error".equalsIgnoreCase(flashMsg.getType())) { %>
                    <i class="bi bi-x-circle-fill fs-5 text-danger me-2 flex-shrink-0"></i>
                <% } else if ("warning".equalsIgnoreCase(flashMsg.getType())) { %>
                    <i class="bi bi-exclamation-triangle-fill fs-5 text-warning me-2 flex-shrink-0"></i>
                <% } else { %>
                    <i class="bi bi-info-circle-fill fs-5 text-primary me-2 flex-shrink-0"></i>
                <% } %>
                <div class="fw-semibold small flex-grow-1">${e:esc(currentFlash.text)}</div>
                <button type="button" class="btn-close ms-2" data-bs-dismiss="alert" aria-label="Close" style="padding: 0.85rem;"></button>
            </div>
        <%
                }
        %>
    </div>
    <%
        }
    %>

    <main id="main-content" tabindex="-1" class="${fullWidth eq 'true' ? 'flex-grow-1' : ('login' eq active || 'register' eq active) ? 'container py-4 py-lg-5 flex-grow-1 d-flex flex-column justify-content-center' : 'container py-4 flex-grow-1'}">
        <jsp:doBody/>
    </main>

    <%
        boolean isAuthPage = "login".equals(active) || "register".equals(active);
        // Item 2: the back office gets the one-line footer too (the guest footer added ~390px to every admin page)
        if (isAuthPage || isBackOfficeArea) {
    %>
    <footer class="site-footer site-footer-mini mt-auto py-3">
        <div class="container d-flex flex-column flex-sm-row align-items-center justify-content-between gap-2 text-center text-sm-start">
            <div class="d-flex align-items-center gap-2">
                <span class="brand-mark brand-mark-sm">
                    <img src="${pageContext.request.contextPath}/assets/images/branding/tini-smartstay-logo-sm.png" width="198" height="132"
                         alt="" aria-hidden="true">
                </span>
                <span class="small footer-mini-copy">
                    &copy; 2026 <strong>Tini SmartStay</strong> &ndash; PRJ301 Group 8.
                </span>
            </div>
            <div class="d-flex align-items-center gap-3 small">
                <a href="${pageContext.request.contextPath}/rooms" class="footer-mini-link">Our Rooms</a>
                <span class="footer-mini-dot">&bull;</span>
                <a href="${pageContext.request.contextPath}/contact" class="footer-mini-link">Contact</a>
            </div>
        </div>
    </footer>
    <%
        } else {
    %>
    <footer class="site-footer mt-auto">
        <div class="container py-5">
            <div class="row g-4 justify-content-between align-items-start">
                <!-- Col 1: Brand & Quote & Social -->
                <div class="col-12 col-md-5 col-lg-4">
                    <a class="footer-brand d-inline-flex align-items-center gap-2 mb-3 text-decoration-none" href="${pageContext.request.contextPath}/">
                        <span class="brand-mark">
                            <img src="${pageContext.request.contextPath}/assets/images/branding/tini-smartstay-logo-sm.png" width="198" height="132"
                                 alt="" aria-hidden="true">
                        </span>
                        <span class="brand-wordmark">
                            <span class="brand-wordmark-top">Tini</span>
                            <span class="brand-wordmark-bottom">SmartStay</span>
                        </span>
                    </a>
                    <p class="footer-tagline mb-3">
                        Sweet spaces to stay, from a night to a season. Comfortable, connected, and made to feel like home
                    </p>
                    <div class="footer-social-links d-flex gap-2">
                        <a href="https://www.instagram.com/" target="_blank" rel="noopener noreferrer" class="footer-social-btn" aria-label="Instagram">
                            <i class="bi bi-instagram"></i>
                        </a>
                        <a href="https://www.facebook.com/nguyen.trung.tinh.158117/" target="_blank" rel="noopener noreferrer" class="footer-social-btn" aria-label="Facebook">
                            <i class="bi bi-facebook"></i>
                        </a>
                    </div>
                </div>

                <!-- Col 2: Our Rooms -->
                <div class="col-6 col-md-3 col-lg-2">
                    <h6 class="footer-heading">Our rooms</h6>
                    <ul class="footer-links-list list-unstyled mb-0">
                        <li><a href="${pageContext.request.contextPath}/rooms/detail?id=1">Butter Beige</a></li>
                        <li><a href="${pageContext.request.contextPath}/rooms/detail?id=2">Vanilla Zen</a></li>
                        <li><a href="${pageContext.request.contextPath}/rooms/detail?id=3">Fresh Tale</a></li>
                        <li><a href="${pageContext.request.contextPath}/rooms/detail?id=4">Power Rush</a></li>
                        <li><a href="${pageContext.request.contextPath}/rooms/detail?id=5">Sunny Love</a></li>
                        <li><a href="${pageContext.request.contextPath}/rooms/detail?id=6">Warm House</a></li>
                    </ul>
                </div>

                <!-- Col 3: Quick Links -->
                <div class="col-6 col-md-4 col-lg-2">
                    <h6 class="footer-heading">Quick links</h6>
                    <ul class="footer-links-list list-unstyled mb-0">
                        <li><a href="${pageContext.request.contextPath}/contact">Contact Us</a></li>
                        <li><a href="${pageContext.request.contextPath}/rooms">Book Direct</a></li>
                        <li><a href="javascript:void(0)" data-bs-toggle="modal" data-bs-target="#houseRulesModal">House Rules</a></li>
                        <li><a href="${pageContext.request.contextPath}/terms">Privacy Policy &amp; Terms</a></li>
                    </ul>
                </div>

                <!-- Col 4: Contact Information -->
                <div class="col-12 col-md-12 col-lg-4">
                    <h6 class="footer-heading">Contact</h6>
                    <ul class="footer-contact-list list-unstyled mb-0">
                        <li class="d-flex align-items-start gap-2 mb-3">
                            <i class="bi bi-geo-alt footer-contact-icon flex-shrink-0 mt-1" aria-hidden="true"></i>
                            <a href="https://maps.google.com/?q=8+QL30,+Cao+L%C3%A3nh,+%C4%90%E1%BB%93ng+Th%C3%A1p,+Vi%E1%BB%87t+Nam" target="_blank" rel="noopener noreferrer" class="footer-contact-text">
                                8 QL30, Cao Lanh, Dong Thap, Vietnam
                            </a>
                        </li>
                        <li class="d-flex align-items-center gap-2 mb-3">
                            <i class="bi bi-telephone footer-contact-icon flex-shrink-0" aria-hidden="true"></i>
                            <a href="tel:0344493658" class="footer-contact-text">
                                (+84) 0344 493 658
                            </a>
                        </li>
                        <li class="d-flex align-items-center gap-2">
                            <i class="bi bi-envelope footer-contact-icon flex-shrink-0" aria-hidden="true"></i>
                            <a href="mailto:kimthu0120657@gmail.com" class="footer-contact-text">
                                kimthu0120657@gmail.com
                            </a>
                        </li>
                    </ul>
                </div>
            </div>
        </div>

        <div class="footer-bottom py-3 border-top text-center">
            <div class="container small">
                &copy; 2026 Tini SmartStay &ndash; PRJ301 Group 8.
            </div>
        </div>
    </footer>

    <!-- House Rules Modal -->
    <div class="modal fade" id="houseRulesModal" tabindex="-1" aria-labelledby="houseRulesModalLabel" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered modal-lg">
            <div class="modal-content border-0 shadow-lg" style="border-radius: 1rem; overflow: hidden;">
                <div class="modal-header text-white" style="background: linear-gradient(135deg, #2b1810 0%, #442a1d 100%); border-bottom: none; padding: 1.25rem 1.75rem;">
                    <div class="d-flex align-items-center gap-2">
                        <i class="bi bi-shield-check fs-4 text-warning"></i>
                        <div>
                            <h5 class="modal-title fw-bold mb-0 text-white" id="houseRulesModalLabel">SmartStay House Rules</h5>
                            <small class="text-white-50">Essential guidelines for a comfortable and secure stay</small>
                        </div>
                    </div>
                    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <div class="modal-body p-4" style="background-color: #fcfbfa;">
                    <div class="row g-3">
                        <!-- Rule 1: Check-in & Smart Lock -->
                        <div class="col-md-6">
                            <div class="p-3 bg-white rounded-3 border h-100 shadow-sm">
                                <div class="d-flex align-items-center gap-2 mb-2">
                                    <i class="bi bi-key-fill fs-5" style="color: #c88a35;"></i>
                                    <h6 class="fw-bold mb-0 text-dark">Check-in &amp; Smart Lock</h6>
                                </div>
                                <p class="small text-muted mb-0">
                                    Self check-in using your unique 6-digit PIN (available in <strong>My Bookings</strong> upon payment). The PIN is active strictly during your booked time slots (S1, S2, S3, S4, or Overnight) and expires automatically.
                                </p>
                            </div>
                        </div>

                        <!-- Rule 2: No Pets Allowed -->
                        <div class="col-md-6">
                            <div class="p-3 bg-white rounded-3 border h-100 shadow-sm">
                                <div class="d-flex align-items-center gap-2 mb-2">
                                    <i class="bi bi-slash-circle-fill fs-5 text-danger"></i>
                                    <h6 class="fw-bold mb-0 text-dark">No Pets Allowed</h6>
                                </div>
                                <p class="small text-muted mb-0">
                                    Pets and animals are strictly prohibited on the property to maintain cleanliness, prevent allergies, and ensure sanitary standards for all guests.
                                </p>
                            </div>
                        </div>

                        <!-- Rule 3: Non-Smoking -->
                        <div class="col-md-6">
                            <div class="p-3 bg-white rounded-3 border h-100 shadow-sm">
                                <div class="d-flex align-items-center gap-2 mb-2">
                                    <i class="bi bi-fire fs-5 text-danger"></i>
                                    <h6 class="fw-bold mb-0 text-dark">Strictly Non-Smoking</h6>
                                </div>
                                <p class="small text-muted mb-0">
                                    All rooms are equipped with automated smoke sensors. Smoking or vaping inside the rooms is strictly prohibited. A deep-cleaning sanitization fee applies to violations.
                                </p>
                            </div>
                        </div>

                        <!-- Rule 4: Quiet Hours -->
                        <div class="col-md-6">
                            <div class="p-3 bg-white rounded-3 border h-100 shadow-sm">
                                <div class="d-flex align-items-center gap-2 mb-2">
                                    <i class="bi bi-moon-stars-fill fs-5" style="color: #6c5ce7;"></i>
                                    <h6 class="fw-bold mb-0 text-dark">Quiet Hours (22:00 &ndash; 06:00)</h6>
                                </div>
                                <p class="small text-muted mb-0">
                                    Please keep noise to a respectful minimum after 22:00. Loud speakers, parties, and noisy gatherings are not allowed to protect neighborhood tranquility.
                                </p>
                            </div>
                        </div>

                        <!-- Rule 5: Room Capacity & IoT Care -->
                        <div class="col-12">
                            <div class="p-3 bg-white rounded-3 border shadow-sm">
                                <div class="d-flex align-items-center gap-2 mb-2">
                                    <i class="bi bi-cpu fs-5" style="color: #0984e3;"></i>
                                    <h6 class="fw-bold mb-0 text-dark">Room Capacity &amp; Smart Equipment</h6>
                                </div>
                                <p class="small text-muted mb-0">
                                    Occupancy must not exceed the maximum registered room capacity. Please take good care of smart IoT devices (door lock, environmental sensors, AC, touch switches) and turn off lights/appliances when stepping out.
                                </p>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="modal-footer bg-light px-4 py-3 border-top d-flex justify-content-between align-items-center">
                    <a href="${pageContext.request.contextPath}/terms" class="btn btn-link text-decoration-none p-0 small fw-semibold" style="color: #8c532b;">
                        Read the full terms and cancellation policy
                    </a>
                    <button type="button" class="btn btn-dark btn-sm px-4 fw-medium" data-bs-dismiss="modal">
                        I Understand
                    </button>
                </div>
            </div>
        </div>
    </div>
    <%
        }
    %>

    <script src="${pageContext.request.contextPath}/assets/vendor/bootstrap/js/bootstrap.bundle.min.js"></script>
    <script src="${pageContext.request.contextPath}/assets/js/datepicker.js"></script>
    <script src="${pageContext.request.contextPath}/assets/js/app.js"></script>
    <script src="${pageContext.request.contextPath}/assets/js/validate.js"></script>
    <% if (showDemoDrawer) { %>
    <!-- Demo tools (Task R2-B3): closed by default, rendered only when demo.mode is on -->
    <div class="ss-demo-spacer" aria-hidden="true"></div>
    <button type="button" class="ss-demo-tab" id="ssDemoTab" aria-controls="ssDemoDrawer" aria-expanded="false"
            aria-label="Open demo tools">DEMO</button>
    <aside class="ss-demo-drawer" id="ssDemoDrawer" role="dialog" aria-modal="false" aria-labelledby="ssDemoTitle"
           data-ctx="${pageContext.request.contextPath}"
           data-csrf="${e:esc(not empty _csrf ? _csrf : sessionScope._csrf)}"
           data-page-booking="<%= ElFunctions.esc(demoPageBooking) %>"
           data-checkout-booking="<%= ElFunctions.esc(demoCheckoutBooking) %>">
        <div class="ss-demo-drawer__header">
            <h2 class="ss-demo-drawer__title" id="ssDemoTitle" tabindex="-1">Demo tools</h2>
            <span class="ss-demo-drawer__badge">DEV</span>
            <button type="button" class="ss-demo-drawer__close" data-ss-demo-close aria-label="Close demo tools">
                <i class="bi bi-x-lg" aria-hidden="true"></i>
            </button>
        </div>
        <div class="ss-demo-drawer__body">
            <section class="ss-demo-drawer__section" aria-labelledby="ssDemoClockHeading">
                <h3 class="ss-demo-drawer__heading" id="ssDemoClockHeading">Simulated time</h3>
                <div class="ss-demo-clock" id="ssDemoClock">
                    <span class="ss-demo-clock__now" id="ssDemoNow">&nbsp;</span>
                    <span class="ss-demo-clock__meta" id="ssDemoOffset">Real time</span>
                </div>
                <button type="button" class="ss-demo-btn ss-demo-btn--quiet" data-ss-demo-reset>
                    <i class="bi bi-arrow-counterclockwise" aria-hidden="true"></i>Reset clock
                </button>
                <p class="ss-demo-drawer__hint">Resets the clock only, data changes stay.</p>
            </section>

            <% if (!demoCheckoutBooking.isEmpty()) { %>
            <section class="ss-demo-drawer__section" aria-labelledby="ssDemoPayHeading">
                <h3 class="ss-demo-drawer__heading" id="ssDemoPayHeading">Payment</h3>
                <p class="ss-demo-drawer__hint">Sends a sandbox bank result for booking <%= ElFunctions.esc(demoCheckoutBooking) %>.</p>
                <button type="button" class="ss-demo-btn ss-demo-btn--success" data-ss-demo-pay="SUCCESS">
                    <i class="bi bi-check2-circle" aria-hidden="true"></i>Simulate successful transfer
                </button>
                <button type="button" class="ss-demo-btn ss-demo-btn--danger" data-ss-demo-pay="FAILED">
                    <i class="bi bi-x-circle" aria-hidden="true"></i>Simulate failure
                </button>
            </section>
            <% } %>

            <section class="ss-demo-drawer__section" aria-labelledby="ssDemoTravelHeading">
                <h3 class="ss-demo-drawer__heading" id="ssDemoTravelHeading">Time travel</h3>
                <label class="ss-demo-drawer__hint" for="ssDemoBooking">Booking</label>
                <select class="form-select ss-demo-drawer__select" id="ssDemoBooking"></select>
                <p class="ss-demo-drawer__hint" id="ssDemoBookingHint"></p>
                <p class="ss-demo-drawer__hint" id="ssDemoStepsNote"></p>
                <ol class="ss-demo-steps" id="ssDemoSteps" aria-label="Stay timeline"></ol>
                <div class="ss-demo-confirm" id="ssDemoConfirm" hidden></div>
            </section>

            <p class="ss-demo-status" id="ssDemoStatus" role="status" aria-live="polite"></p>
        </div>
    </aside>
    <% } %>

    <script src="${pageContext.request.contextPath}/assets/js/realtime.js"></script>
    <script src="${pageContext.request.contextPath}/assets/js/weekly-calendar.js"></script>
    <% if (showDemoDrawer) { %>
    <script src="${pageContext.request.contextPath}/assets/js/demo.js"></script>
    <% } %>
</body>
</html>
