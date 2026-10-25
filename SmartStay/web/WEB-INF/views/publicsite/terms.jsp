<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="e" uri="/WEB-INF/tld/smartstay.tld" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:layout title="Privacy Policy & Terms" active="terms" fullWidth="true">
    <!-- Header Banner -->
    <div class="contact-banner">
        <div class="container py-5">
            <div class="contact-eyebrow-wrap">
                <span class="contact-eyebrow">LEGAL &amp; POLICIES</span>
            </div>
            <h1 class="contact-title" id="terms-heading">
                Privacy Policy &amp; <span class="contact-title-accent">Terms</span>
            </h1>
            <p class="contact-lead mb-0">
                Transparent, secure, and dedicated to elevating your smart stay experience at Tini SmartStay.
            </p>
        </div>
    </div>

    <!-- Main Body -->
    <div class="contact-body py-5" style="background-color: #fbf9f6;">
        <div class="container">
            <!-- Boutique Breadcrumb Navigation -->
            <nav aria-label="breadcrumb" class="mb-4">
                <ol class="breadcrumb boutique-breadcrumb m-0">
                    <li class="breadcrumb-item">
                        <a href="${pageContext.request.contextPath}/" class="boutique-breadcrumb-link">
                            <i class="bi bi-house-door me-1"></i>Home
                        </a>
                    </li>
                    <li class="breadcrumb-item active" aria-current="page">Privacy Policy &amp; Terms</li>
                </ol>
            </nav>

            <div class="row g-4">
                <!-- Navigation Sidebar (Sticky) -->
                <div class="col-lg-3">
                    <div class="card border-0 shadow-sm sticky-top" style="top: 90px; border-radius: 0.75rem; overflow: hidden;">
                        <div class="card-header bg-white border-bottom py-3">
                            <h6 class="fw-bold mb-0 text-dark">
                                <i class="bi bi-list-check me-2" style="color: #c88a35;"></i>Table of Contents
                            </h6>
                        </div>
                        <div class="list-group list-group-flush small">
                            <a href="#section-general" class="list-group-item list-group-item-action py-2 d-flex align-items-center gap-2">
                                <i class="bi bi-file-text text-muted"></i> 1. General Terms
                            </a>
                            <a href="#section-booking" class="list-group-item list-group-item-action py-2 d-flex align-items-center gap-2">
                                <i class="bi bi-calendar2-check text-muted"></i> 2. Booking &amp; Smart PIN
                            </a>
                            <a href="#section-cancellation" class="list-group-item list-group-item-action py-2 d-flex align-items-center gap-2 text-danger fw-medium">
                                <i class="bi bi-arrow-counterclockwise text-danger"></i> 3. Cancellation &amp; Refund
                            </a>
                            <a href="#section-rules" class="list-group-item list-group-item-action py-2 d-flex align-items-center gap-2">
                                <i class="bi bi-shield-check text-muted"></i> 4. House Rules
                            </a>
                            <a href="#section-privacy" class="list-group-item list-group-item-action py-2 d-flex align-items-center gap-2">
                                <i class="bi bi-lock text-muted"></i> 5. Privacy Policy
                            </a>
                            <a href="#section-support" class="list-group-item list-group-item-action py-2 d-flex align-items-center gap-2">
                                <i class="bi bi-headset text-muted"></i> 6. Support &amp; Inquiries
                            </a>
                        </div>
                    </div>
                </div>

                <!-- Content Area -->
                <div class="col-lg-9">
                    <!-- 1. General Terms -->
                    <section id="section-general" class="card border-0 shadow-sm p-4 mb-4" style="border-radius: 0.75rem;">
                        <div class="d-flex align-items-center gap-2 mb-3 pb-2 border-bottom">
                            <span class="badge bg-dark rounded-pill px-2 py-1">01</span>
                            <h4 class="fw-bold mb-0 text-dark">General Terms of Service</h4>
                        </div>
                        <p class="text-secondary small leading-relaxed">
                            Welcome to the <strong>Tini SmartStay</strong> smart hospitality ecosystem. By accessing our website, creating an account, and booking suites through our platform, you agree to comply with and be bound by the terms and conditions outlined below.
                        </p>
                        <ul class="text-secondary small mb-0 ps-3">
                            <li class="mb-2">Guests are responsible for providing accurate personal information (Full Name, Phone Number, Email) upon registration and reservation.</li>
                            <li class="mb-2">Each registered guest account holds full responsibility for maintaining the confidentiality of their credentials and smart door PIN codes provided for stay sessions.</li>
                            <li>Tini SmartStay reserves the right to refuse service upon detection of fraudulent activities, intentional sabotage of IoT infrastructure, or violations of applicable regulations.</li>
                        </ul>
                    </section>

                    <!-- 2. Booking & Smart PIN -->
                    <section id="section-booking" class="card border-0 shadow-sm p-4 mb-4" style="border-radius: 0.75rem;">
                        <div class="d-flex align-items-center gap-2 mb-3 pb-2 border-bottom">
                            <span class="badge bg-dark rounded-pill px-2 py-1">02</span>
                            <h4 class="fw-bold mb-0 text-dark">Booking Regulations &amp; Smart Lock PIN</h4>
                        </div>
                        <p class="text-secondary small">
                            SmartStay delivers an autonomous, contactless, and private experience through our automated Smart Lock keyless system:
                        </p>
                        <div class="row g-3 mb-3">
                            <div class="col-md-6">
                                <div class="p-3 bg-light rounded-3 h-100 border">
                                    <h6 class="fw-bold text-dark mb-1"><i class="bi bi-clock-history me-1 text-primary"></i> Flexible Time Slots</h6>
                                    <p class="small text-muted mb-0">
                                        Guests can reserve by time slots: <strong>S1</strong> (06:00 - 12:00), <strong>S2</strong> (12:00 - 18:00), <strong>S3</strong> (18:00 - 22:00), or <strong>S4 / Overnight</strong> (22:00 - 06:00 next day).
                                    </p>
                                </div>
                            </div>
                            <div class="col-md-6">
                                <div class="p-3 bg-light rounded-3 h-100 border">
                                    <h6 class="fw-bold text-dark mb-1"><i class="bi bi-hourglass-split me-1 text-warning"></i> 15-Minute Reservation Hold</h6>
                                    <p class="small text-muted mb-0">
                                        Upon creating a booking, the room is placed in <strong>HELD</strong> status for 15 minutes. If payment is not completed before timeout, the system automatically releases the hold.
                                    </p>
                                </div>
                            </div>
                        </div>
                        <div class="p-3 rounded-3 border" style="background-color: #fff9f0;">
                            <h6 class="fw-bold text-dark mb-1"><i class="bi bi-key-fill me-1" style="color: #c88a35;"></i> Automated PIN Activation:</h6>
                            <p class="small text-muted mb-0">
                                Once payment is successfully confirmed, a unique 6-digit PIN code appears in the guest's <strong>My Bookings</strong> dashboard. The PIN code is valid strictly from the start time to the end time of the booked slots and automatically deactivates upon checkout.
                            </p>
                        </div>
                    </section>

                    <!-- 3. Cancellation & Refund Policy -->
                    <section id="section-cancellation" class="card border-0 shadow-sm p-4 mb-4" style="border-radius: 0.75rem;">
                        <div class="d-flex align-items-center gap-2 mb-3 pb-2 border-bottom">
                            <span class="badge bg-danger rounded-pill px-2 py-1">03</span>
                            <h4 class="fw-bold mb-0 text-dark">Cancellation &amp; Refund Policy</h4>
                        </div>
                        <p class="text-secondary small mb-3">
                            To protect guest rights while allowing operational readiness for incoming guests, SmartStay enforces a transparent cancellation policy:
                        </p>
                        <div class="table-responsive mb-3">
                            <table class="table table-bordered table-sm align-middle small mb-0">
                                <thead class="table-light">
                                    <tr>
                                        <th style="width: 45%;">Cancellation Request Time</th>
                                        <th style="width: 30%;">Refund Rate</th>
                                        <th style="width: 25%;">Status</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <tr>
                                        <td class="fw-semibold">
                                            <i class="bi bi-check-circle-fill text-success me-1"></i> More than 24 hours prior to check-in time
                                        </td>
                                        <td class="text-success fw-bold">100% Refund</td>
                                        <td><span class="badge bg-success-subtle text-success border border-success-subtle">Free Cancellation</span></td>
                                    </tr>
                                    <tr>
                                        <td class="fw-semibold">
                                            <i class="bi bi-x-circle-fill text-danger me-1"></i> Within 24 hours prior to check-in time
                                        </td>
                                        <td class="text-danger fw-bold">Non-refundable (0%)</td>
                                        <td><span class="badge bg-danger-subtle text-danger border border-danger-subtle">Non-refundable</span></td>
                                    </tr>
                                </tbody>
                            </table>
                        </div>
                        <p class="small text-muted mb-0">
                            <strong>* Refund Processing Method:</strong> Refund amounts will be audited and transferred to your bank account within <strong>1 to 3 business days</strong> following confirmed cancellation.
                        </p>
                    </section>

                    <!-- 4. House Rules -->
                    <section id="section-rules" class="card border-0 shadow-sm p-4 mb-4" style="border-radius: 0.75rem;">
                        <div class="d-flex align-items-center gap-2 mb-3 pb-2 border-bottom">
                            <span class="badge bg-dark rounded-pill px-2 py-1">04</span>
                            <h4 class="fw-bold mb-0 text-dark">House Rules</h4>
                        </div>
                        <div class="row g-3">
                            <div class="col-md-6">
                                <div class="d-flex align-items-start gap-2 p-2">
                                    <i class="bi bi-slash-circle-fill text-danger fs-5 mt-1"></i>
                                    <div>
                                        <strong class="d-block text-dark small">No Pets Allowed:</strong>
                                        <span class="text-muted small">Animals, dogs, and cats are strictly prohibited in all rooms to maintain hygiene and prevent allergic reactions for subsequent guests.</span>
                                    </div>
                                </div>
                            </div>
                            <div class="col-md-6">
                                <div class="d-flex align-items-start gap-2 p-2">
                                    <i class="bi bi-fire text-danger fs-5 mt-1"></i>
                                    <div>
                                        <strong class="d-block text-dark small">Strictly Non-Smoking:</strong>
                                        <span class="text-muted small">All suites are equipped with automated smoke detectors. Smoking or vaping indoors is strictly prohibited. A 1,000,000 VND deep-cleaning fee applies upon violation.</span>
                                    </div>
                                </div>
                            </div>
                            <div class="col-md-6">
                                <div class="d-flex align-items-start gap-2 p-2">
                                    <i class="bi bi-moon-stars-fill text-primary fs-5 mt-1"></i>
                                    <div>
                                        <strong class="d-block text-dark small">Quiet Hours (22:00 - 06:00):</strong>
                                        <span class="text-muted small">Please respect shared community peace. Loud speakers, excessive noise, and party gatherings are prohibited after 22:00.</span>
                                    </div>
                                </div>
                            </div>
                            <div class="col-md-6">
                                <div class="d-flex align-items-start gap-2 p-2">
                                    <i class="bi bi-cpu-fill text-success fs-5 mt-1"></i>
                                    <div>
                                        <strong class="d-block text-dark small">IoT Equipment &amp; Capacity:</strong>
                                        <span class="text-muted small">Accommodate only the registered number of guests. Protect smart locks, sensors, and room amenities. Turn off electrical appliances when leaving.</span>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </section>

                    <!-- 5. Privacy Policy -->
                    <section id="section-privacy" class="card border-0 shadow-sm p-4 mb-4" style="border-radius: 0.75rem;">
                        <div class="d-flex align-items-center gap-2 mb-3 pb-2 border-bottom">
                            <span class="badge bg-dark rounded-pill px-2 py-1">05</span>
                            <h4 class="fw-bold mb-0 text-dark">Privacy Policy</h4>
                        </div>
                        <p class="text-secondary small leading-relaxed">
                            Tini SmartStay respects and is committed to fully safeguarding your personal information:
                        </p>
                        <ul class="text-secondary small mb-3 ps-3">
                            <li class="mb-2"><strong>Data Collected:</strong> Full name, phone number, email address, reservation history, and door access logs recorded for safety and security.</li>
                            <li class="mb-2"><strong>Purpose of Use:</strong> Generating room access PIN codes, confirming payment transactions, providing emergency support, and enhancing service quality.</li>
                            <li class="mb-2"><strong>Payment Transaction Security:</strong> Our system records only reference reconciliation codes from banking and payment partners; no bank account credentials or card details are ever stored.</li>
                            <li><strong>No Third-Party Sharing:</strong> We do not sell, rent, or disclose guest information to any third-party commercial entities without your explicit consent.</li>
                        </ul>
                    </section>

                    <!-- 6. Support & Inquiries -->
                    <section id="section-support" class="card border-0 shadow-sm p-4" style="border-radius: 0.75rem; background: linear-gradient(135deg, #2b1810 0%, #3e2417 100%); color: #fff;">
                        <div class="d-flex align-items-center justify-content-between flex-wrap gap-3">
                            <div>
                                <h5 class="fw-bold mb-1 text-white"><i class="bi bi-headset me-2 text-warning"></i>Need further assistance?</h5>
                                <p class="small text-white-50 mb-0">The Tini SmartStay team is always on standby to assist you within 1 hour.</p>
                            </div>
                            <div class="d-flex gap-2">
                                <a href="tel:0344493658" class="btn btn-warning btn-sm fw-semibold px-3">
                                    <i class="bi bi-telephone me-1"></i> 0344 493 658
                                </a>
                                <a href="${pageContext.request.contextPath}/contact" class="btn btn-outline-light btn-sm fw-medium px-3">
                                    Contact Us Directly &rarr;
                                </a>
                            </div>
                        </div>
                    </section>
                </div>
            </div>
        </div>
    </div>
</t:layout>
