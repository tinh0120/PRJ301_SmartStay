<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="e" uri="/WEB-INF/tld/smartstay.tld" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:layout title="Contact" active="contact" fullWidth="true">
    <!-- Contact Header Banner (Dark Mocha Theme matching mockup Image 1) -->
    <div class="contact-banner">
        <div class="container py-5">
            <div class="contact-eyebrow-wrap">
                <span class="contact-eyebrow">GET IN TOUCH</span>
            </div>
            <h1 class="contact-title" id="contact-heading">
                Contact <span class="contact-title-accent">Us</span>
            </h1>
            <p class="contact-lead mb-0">
                Book rooms, ask about long-stay, or just say hello &mdash; we reply within 1 hour.
            </p>
        </div>
    </div>

    <!-- Contact Main Body (Map on Top Row, Information & Location Below) -->
    <div class="contact-body py-5">
        <div class="container">
            <!-- Boutique Breadcrumb Navigation -->
            <nav aria-label="breadcrumb" class="mb-4">
                <ol class="breadcrumb boutique-breadcrumb m-0">
                    <li class="breadcrumb-item">
                        <a href="${pageContext.request.contextPath}/" class="boutique-breadcrumb-link">
                            <i class="bi bi-house-door me-1"></i>Home
                        </a>
                    </li>
                    <li class="breadcrumb-item active" aria-current="page">Contact</li>
                </ol>
            </nav>

            <!-- Static Address & Location Card (Offline Friendly) -->
            <div class="row mb-4">
                <div class="col-12">
                    <div class="contact-info-card p-4 p-md-5">
                        <div class="row align-items-center g-4">
                            <div class="col-lg-7">
                                <div class="d-flex align-items-center gap-2 mb-2">
                                    <span class="badge" style="background: rgba(166,124,82,0.15); color: #784E2D; font-weight: 600; padding: 6px 12px; border-radius: 999px;">
                                        <i class="bi bi-geo-alt-fill me-1"></i> BOUTIQUE LOCATION
                                    </span>
                                    <span class="badge" style="background: rgba(46,125,50,0.12); color: #2E7D32; font-weight: 600; padding: 6px 12px; border-radius: 999px;">
                                        <i class="bi bi-clock-fill me-1"></i> Open 24/7
                                    </span>
                                </div>
                                <h2 class="h3 fw-bold mb-2" style="font-family: var(--font-serif); color: #2D241E;">SmartStay Boutique Hotel</h2>
                                <p class="text-muted mb-3 fs-5">
                                    8 QL30, Ward My Phu, Cao Lanh City, Dong Thap Province, Vietnam
                                </p>
                                <div class="row g-3 text-secondary small mb-4">
                                    <div class="col-sm-6 d-flex align-items-center gap-2">
                                        <i class="bi bi-signpost-2 text-primary"></i>
                                        <span>Direct access from National Route 30</span>
                                    </div>
                                    <div class="col-sm-6 d-flex align-items-center gap-2">
                                        <i class="bi bi-p-square text-primary"></i>
                                        <span>Complimentary secure parking</span>
                                    </div>
                                    <div class="col-sm-6 d-flex align-items-center gap-2">
                                        <i class="bi bi-telephone text-primary"></i>
                                        <span>Front Desk: (+84) 0344 493 658</span>
                                    </div>
                                    <div class="col-sm-6 d-flex align-items-center gap-2">
                                        <i class="bi bi-envelope text-primary"></i>
                                        <span>kimthu0120657@gmail.com</span>
                                    </div>
                                </div>
                                <a href="https://maps.google.com/?q=8+QL30,+Cao+L%C3%A3nh,+%C4%90%E1%BB%93ng+Th%C3%A1p,+Vi%E1%BB%87t+Nam" 
                                   target="_blank" 
                                   rel="noopener noreferrer" 
                                   class="contact-map-btn d-inline-flex align-items-center gap-2">
                                    <i class="bi bi-map"></i> Open in Google Maps <span class="ms-1">&rarr;</span>
                                </a>
                            </div>
                            <div class="col-lg-5">
                                <div class="rounded-4 overflow-hidden shadow-sm" style="height: 240px; position: relative;">
                                    <img src="${pageContext.request.contextPath}/assets/images/branding/hero_img2.jpg" 
                                         alt="SmartStay Boutique Hotel Location"
                                         class="w-100 h-100" 
                                         style="object-fit: cover;"
                                         onerror="this.onerror=null; this.src='${pageContext.request.contextPath}/assets/images/branding/hero_img2.jpg';">
                                    <div class="position-absolute bottom-0 start-0 end-0 p-3" style="background: linear-gradient(transparent, rgba(0,0,0,0.7)); color: white;">
                                        <div class="fw-semibold">Cao Lanh Oasis</div>
                                        <div class="small text-white-50">Warm Luxury Hospitality</div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <!-- Row 3: Quick Contact Info Pushed Down Below -->
            <div class="row">
                <div class="col-12">
                    <div class="contact-info-card p-4">
                        <h3 class="contact-quick-eyebrow mb-3 h6">QUICK CONTACT</h3>
                        <div class="row g-3">
                            <!-- Call Card -->
                            <div class="col-12 col-sm-6 col-lg-3">
                                <a href="tel:0344493658" class="quick-contact-pill h-100 text-decoration-none">
                                    <div class="quick-icon-wrap" aria-hidden="true">
                                        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                            <path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z"></path>
                                        </svg>
                                    </div>
                                    <div class="quick-content">
                                        <span class="quick-label">Call</span>
                                        <span class="quick-value">0344 493 658</span>
                                    </div>
                                </a>
                            </div>

                            <!-- Zalo Card -->
                            <div class="col-12 col-sm-6 col-lg-3">
                                <a href="https://zalo.me/0344493658" target="_blank" rel="noopener noreferrer" class="quick-contact-pill h-100 text-decoration-none">
                                    <div class="quick-icon-wrap" aria-hidden="true">
                                        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                            <path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z"></path>
                                        </svg>
                                    </div>
                                    <div class="quick-content">
                                        <span class="quick-label">Zalo</span>
                                        <span class="quick-value">0344 493 658</span>
                                    </div>
                                </a>
                            </div>

                            <!-- Facebook Card -->
                            <div class="col-12 col-sm-6 col-lg-3">
                                <a href="https://www.facebook.com/nguyen.trung.tinh.158117/" target="_blank" rel="noopener noreferrer" class="quick-contact-pill h-100 text-decoration-none">
                                    <div class="quick-icon-wrap" aria-hidden="true">
                                        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                            <path d="M18 2h-3a5 5 0 0 0-5 5v3H7v4h3v8h4v-8h3l1-4h-4V7a1 1 0 0 1 1-1h3z"></path>
                                        </svg>
                                    </div>
                                    <div class="quick-content">
                                        <span class="quick-label">Facebook</span>
                                        <span class="quick-value">Trung Tinh</span>
                                    </div>
                                </a>
                            </div>

                            <!-- Email Card -->
                            <div class="col-12 col-sm-6 col-lg-3">
                                <a href="mailto:kimthu0120657@gmail.com" class="quick-contact-pill h-100 text-decoration-none">
                                    <div class="quick-icon-wrap" aria-hidden="true">
                                        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                            <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"></path>
                                            <polyline points="22,6 12,13 2,6"></polyline>
                                        </svg>
                                    </div>
                                    <div class="quick-content">
                                        <span class="quick-label">Email</span>
                                        <span class="quick-value text-break">kimthu0120657@gmail.com</span>
                                    </div>
                                </a>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</t:layout>
