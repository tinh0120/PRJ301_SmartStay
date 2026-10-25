<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="e" uri="/WEB-INF/tld/smartstay.tld" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:layout title="${card.room.displayTitle}" active="rooms">
    <style>
        /* Unified Card Styling */
        .room-detail-card {
            background-color: #FFFFFF;
            border: 1px solid var(--color-sand, #E8DFD8);
            border-radius: 18px;
            box-shadow: 0 4px 14px rgba(43, 24, 16, 0.05);
        }
        .amenity-grid-2col {
            display: grid;
            grid-template-columns: repeat(2, 1fr);
            gap: 8px;
        }
        .amenity-grid-2col .amenity-item {
            display: flex;
            align-items: center;
            gap: 8px;
            padding: 7px 10px;
            background: #FAF7F0;
            border: 1px solid #E8DFD8;
            border-radius: 10px;
            font-size: 0.82rem;
            font-weight: 500;
            color: #3B2A20;
        }
        .amenity-grid-2col .amenity-icon {
            width: 18px;
            height: 18px;
            object-fit: contain;
        }
        .room-info-tabs .nav-link {
            color: #6B5646;
            font-weight: 600;
            font-size: 0.88rem;
            border: none;
            border-bottom: 2.5px solid transparent;
            padding: 10px 14px;
            background: transparent;
            transition: all 0.2s ease;
        }
        .room-info-tabs .nav-link.active {
            color: #9A5B2E;
            border-bottom: 2.5px solid #9A5B2E;
            background: transparent;
        }
        .room-info-tab-content {
            max-height: 380px;
            overflow-y: auto;
            padding-right: 4px;
        }
        .room-info-tab-content::-webkit-scrollbar {
            width: 5px;
        }
        .room-info-tab-content::-webkit-scrollbar-thumb {
            background-color: #D9B991;
            border-radius: 4px;
        }
        .first-review-fix:first-of-type {
            border-top: none !important;
            padding-top: 0 !important;
        }
        /* Dimmed Row for Expired Today */
        .matrix-row-dimmed {
            opacity: 0.52;
            background-color: #faf7f5;
        }
        .matrix-row-dimmed td {
            padding-top: 4px !important;
            padding-bottom: 4px !important;
        }
        .matrix-row-dimmed .matrix-slot-btn {
            min-height: 44px !important;
            padding: 4px !important;
        }
        /* Desktop Booking Summary Card (Right Column) */
        .desktop-booking-summary {
            position: sticky;
            top: 90px;
            background: #FFFDF9;
            border: 1.5px solid #D9B991;
            box-shadow: 0 10px 30px rgba(43, 27, 18, 0.08);
            border-radius: 20px;
            overflow: hidden;
            margin-bottom: 2rem;
        }
        .booking-card-header {
            background: linear-gradient(135deg, #3A2317 0%, #523526 100%);
            padding: 14px 16px;
            color: #FFFFFF;
        }
        .booking-card-body {
            background: #FFFDF9;
            padding: 16px;
        }
        .booking-selection-box {
            display: block !important;
            background: #FAF5EE;
            border: 1px solid #EADBCC;
            border-radius: 12px;
            padding: 9px 12px;
            white-space: normal !important;
            font-family: inherit !important;
        }
        .booking-selection-date {
            font-weight: 700;
            color: #2B1B12;
            font-size: 0.84rem;
            line-height: 1.3;
        }
        .booking-selection-time {
            font-size: 0.76rem;
            color: #6B5646;
            margin-top: 5px;
            line-height: 1.45;
            word-break: break-word;
            white-space: normal !important;
        }
        .btn-continue-gradient {
            background: linear-gradient(135deg, #9A5B2E 0%, #7E451F 100%) !important;
            border: none !important;
            color: #FFFFFF !important;
            transition: transform 0.15s ease, box-shadow 0.15s ease;
        }
        .btn-continue-gradient:hover:not(:disabled) {
            background: linear-gradient(135deg, #8A4E24 0%, #6E3A18 100%) !important;
            transform: translateY(-1px);
            box-shadow: 0 4px 12px rgba(154, 91, 46, 0.3) !important;
        }
        .btn-continue-gradient:disabled {
            opacity: 0.65 !important;
            background: #A69182 !important;
            cursor: not-allowed !important;
            box-shadow: none !important;
            transform: none !important;
        }
        /* Mobile Agoda-Style Compact Sticky Bottom Bar & Drawer (< 992px) */
        @media (max-width: 991.98px) {
            body, .room-detail-container {
                padding-bottom: calc(100px + env(safe-area-inset-bottom, 0px)) !important;
            }
            .mobile-booking-bar {
                position: fixed;
                bottom: 0;
                left: 0;
                right: 0;
                z-index: 1050;
                min-height: 74px;
                height: auto;
                padding: 10px 1rem calc(10px + env(safe-area-inset-bottom, 0px));
                background: var(--color-cocoa, #3B2A20);
                color: var(--color-milk, #FAF7F0);
                border-top-left-radius: 16px;
                border-top-right-radius: 16px;
                box-shadow: 0 -4px 20px rgba(0, 0, 0, 0.22);
                animation: mobileBarSlideUp 0.25s cubic-bezier(0.16, 1, 0.3, 1);
            }
            @keyframes mobileBarSlideUp {
                from { transform: translateY(100%); }
                to { transform: translateY(0); }
            }
            .btn-mobile-book-now {
                background: linear-gradient(135deg, #9A5B2E 0%, #7E451F 100%) !important;
                color: #FFFFFF !important;
                border: none !important;
                font-size: 0.95rem;
                box-shadow: 0 4px 12px rgba(154, 91, 46, 0.35);
                transition: transform 0.15s ease;
            }
            .btn-mobile-book-now:active {
                transform: scale(0.96);
            }
            .mobile-drawer-backdrop {
                position: fixed;
                top: 0;
                left: 0;
                width: 100vw;
                height: 100vh;
                background: rgba(20, 12, 8, 0.65);
                backdrop-filter: blur(4px);
                -webkit-backdrop-filter: blur(4px);
                z-index: 1055;
                opacity: 0;
                pointer-events: none;
                transition: opacity 0.25s ease;
            }
            .mobile-drawer-backdrop.is-open {
                opacity: 1;
                pointer-events: auto;
            }
            .mobile-booking-drawer {
                position: fixed;
                bottom: 0;
                left: 0;
                right: 0;
                z-index: 1060;
                background: #FFFDF9;
                border-top-left-radius: 24px;
                border-top-right-radius: 24px;
                box-shadow: 0 -8px 30px rgba(0, 0, 0, 0.25);
                max-height: 85vh;
                overflow-y: auto;
                transform: translateY(100%);
                transition: transform 0.3s cubic-bezier(0.16, 1, 0.3, 1);
            }
            .mobile-booking-drawer.is-open {
                transform: translateY(0);
            }
        }
        .btn-invalid-slots,
        .btn-invalid-slots:disabled,
        a.btn-invalid-slots.disabled {
            background: linear-gradient(135deg, #B53F3F 0%, #8E2828 100%) !important;
            color: #FFFFFF !important;
            opacity: 0.92 !important;
            cursor: not-allowed !important;
            pointer-events: none !important;
            box-shadow: none !important;
            transform: none !important;
        }
        /* Top-Center Floating Notification Toast */
        .matrix-toast-container {
            position: fixed;
            top: 24px;
            left: 50%;
            transform: translateX(-50%);
            z-index: 10900;
            pointer-events: none;
            display: flex;
            flex-direction: column;
            align-items: center;
            gap: 10px;
            max-width: 520px;
            width: calc(100% - 32px);
        }
        .matrix-toast {
            pointer-events: auto;
            background: linear-gradient(135deg, #2D1A10 0%, #1F120A 100%);
            color: #FFFFFF;
            border: 1.5px solid #E59846;
            border-radius: 16px;
            padding: 14px 18px;
            box-shadow: 0 16px 45px rgba(0, 0, 0, 0.45), 0 0 0 1px rgba(229, 152, 70, 0.25);
            display: flex;
            align-items: flex-start;
            gap: 14px;
            animation: toastSlideDownBounce 0.35s cubic-bezier(0.175, 0.885, 0.32, 1.275);
            transition: opacity 0.25s ease, transform 0.25s ease;
        }
        .matrix-toast.toast-hiding {
            opacity: 0;
            transform: translateY(-16px) scale(0.95);
        }
        @keyframes toastSlideDownBounce {
            0% {
                opacity: 0;
                transform: translateY(-30px) scale(0.92);
            }
            100% {
                opacity: 1;
                transform: translateY(0) scale(1);
            }
        }
        .matrix-toast-icon-wrap {
            width: 40px;
            height: 40px;
            min-width: 40px;
            border-radius: 50%;
            background: rgba(229, 152, 70, 0.2);
            border: 1.5px solid rgba(229, 152, 70, 0.5);
            display: flex;
            align-items: center;
            justify-content: center;
            color: #F5A623;
            font-size: 1.3rem;
            margin-top: 2px;
        }
        .matrix-toast-content {
            flex: 1;
        }
        .matrix-toast-title {
            font-size: 0.94rem;
            font-weight: 700;
            color: #FFFDF9;
            margin-bottom: 4px;
            display: flex;
            align-items: center;
            gap: 6px;
        }
        .matrix-toast-message {
            font-size: 0.83rem;
            line-height: 1.45;
            color: #EEDBCE;
        }
        .matrix-toast-message strong {
            color: #FFD494;
        }
        .matrix-toast-actions {
            margin-top: 10px;
            display: flex;
            gap: 8px;
            align-items: center;
            flex-wrap: wrap;
        }
        .matrix-toast-btn-dismiss {
            background: rgba(229, 152, 70, 0.25);
            border: 1px solid #E59846;
            color: #FFFDF9;
            font-size: 0.78rem;
            font-weight: 600;
            padding: 4px 14px;
            border-radius: 20px;
            cursor: pointer;
            transition: all 0.15s ease;
        }
        .matrix-toast-btn-dismiss:hover {
            background: #E59846;
            color: #2D1A10;
        }
        .matrix-toast-btn-fix {
            background: #B53F3F;
            border: 1px solid #E74C3C;
            color: #FFFFFF;
            font-size: 0.78rem;
            font-weight: 600;
            padding: 4px 14px;
            border-radius: 20px;
            cursor: pointer;
            transition: all 0.15s ease;
        }
        .matrix-toast-btn-fix:hover {
            background: #962D22;
        }
        .matrix-toast-close {
            background: none;
            border: none;
            color: #B59E8D;
            font-size: 1.35rem;
            line-height: 1;
            cursor: pointer;
            padding: 0 4px;
            transition: color 0.15s ease;
        }
        .matrix-toast-close:hover {
            color: #FFFFFF;
        }
        @keyframes slotShake {
            0%, 100% { transform: translateX(0); }
            20%, 60% { transform: translateX(-6px); }
            40%, 80% { transform: translateX(6px); }
        }
        .slot-shake-effect {
            animation: slotShake 0.4s ease !important;
        }
        /* Mobile Sticky Left Day Column */
        @media (max-width: 767.98px) {
            .matrix-th-corner,
            .matrix-day-cell {
                position: sticky;
                left: 0;
                z-index: 4;
                background-color: var(--color-sand) !important;
                box-shadow: 2px 0 6px rgba(0, 0, 0, 0.08);
            }
            .amenity-grid-2col {
                grid-template-columns: 1fr;
            }
        }
    </style>

    <!-- 1. Page Header: Boutique Breadcrumb Navigation -->
    <nav aria-label="breadcrumb" class="mb-3">
        <ol class="breadcrumb boutique-breadcrumb m-0">
            <li class="breadcrumb-item">
                <a href="${pageContext.request.contextPath}/" class="boutique-breadcrumb-link">
                    <i class="bi bi-house-door me-1"></i>Home
                </a>
            </li>
            <li class="breadcrumb-item">
                <a href="${pageContext.request.contextPath}/rooms?date=${e:esc(date)}" class="boutique-breadcrumb-link">
                    Rooms
                </a>
            </li>
            <li class="breadcrumb-item active" aria-current="page">${e:esc(card.room.displayTitle)}</li>
        </ol>
    </nav>

    <!-- Room Detail Header & Meta Pills -->
    <div class="room-detail-header mb-4">
        <h1 class="room-detail-title mb-2">${e:esc(card.room.displayTitle)}</h1>
        <div class="d-flex align-items-center gap-2 flex-wrap">
            <span class="room-meta-pill"><i class="bi bi-door-closed me-1 text-caramel"></i>${e:esc(card.room.roomNumber)}</span>
            <span class="room-meta-pill"><i class="bi bi-layers me-1 text-caramel"></i>Floor ${card.room.floorNumber}</span>
            <span class="room-meta-pill"><i class="bi bi-people me-1 text-caramel"></i>${card.room.capacity} Guests</span>
            <t:if test="${not empty card.room.locationDescription}">
                <span class="room-meta-pill"><i class="bi bi-geo me-1 text-caramel"></i>${e:esc(card.room.locationDescription)}</span>
            </t:if>
            <span class="room-meta-pill badge-smart-stay"><i class="bi bi-shield-check me-1"></i>Smart Lock 24/7</span>
            <t:if test="${!card.bookable}">
                <span class="badge bg-danger rounded-pill px-3 py-2">MAINTENANCE</span>
            </t:if>
        </div>
    </div>

    <!-- 2. Room Information Section, 2 Columns (~58% / 42%) -->
    <div class="row g-4 align-items-start mb-4">
        <!-- Left column: large image with arrows and thumbnail gallery below (~58%) -->
        <div class="col-lg-7">
            <div class="room-detail-card overflow-hidden">
                <div class="card-body p-3">
                    <div class="gallery-container">
                        <div class="gallery-main mb-2 position-relative">
                            <img id="galleryMainImg"
                                 src="${pageContext.request.contextPath}/${card.room.resolvedImageUrl}"
                                 alt="${e:esc(card.room.displayTitle)} main image"
                                 class="gallery-main-img"
                                 onerror="this.onerror=null;this.src='${pageContext.request.contextPath}/assets/images/branding/hero_img2.jpg';">
                            <button type="button" class="gallery-zoom-badge" id="galleryZoomBtn" aria-label="Enlarge photo" title="Click to enlarge photo">
                                <i class="bi bi-arrows-fullscreen"></i>
                            </button>
                            <button type="button" class="gallery-arrow-btn gallery-arrow-prev" id="galleryPrev" aria-label="Previous image">
                                <i class="bi bi-chevron-left" aria-hidden="true"></i>
                            </button>
                            <button type="button" class="gallery-arrow-btn gallery-arrow-next" id="galleryNext" aria-label="Next image">
                                <i class="bi bi-chevron-right" aria-hidden="true"></i>
                            </button>
                        </div>
                        <div class="gallery-thumbs" role="region" aria-label="Room photo gallery">
                            <t:forEach items="${e:gallery(card.room.roomNumber, card.room.displayTitle)}" var="img">
                                <button type="button"
                                        class="gallery-thumb-btn ${img.main ? 'active' : ''}"
                                        data-img-src="${pageContext.request.contextPath}/${img.url}"
                                        data-img-alt="${e:esc(img.alt)}"
                                        aria-label="${e:esc(img.alt)}"
                                        aria-pressed="${img.main ? 'true' : 'false'}">
                                    <img src="${pageContext.request.contextPath}/${img.url}"
                                         alt="${e:esc(img.alt)}"
                                         class="gallery-thumb-img"
                                         onerror="this.onerror=null; this.src='${pageContext.request.contextPath}/assets/images/branding/hero_img2.jpg';">
                                </button>
                            </t:forEach>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <!-- Right column: unified tabbed card (~42%) (Task 5: Zero-scroll tabbed views) -->
        <div class="col-lg-5">
            <div class="room-detail-card overflow-hidden">
                <!-- Nav Tabs -->
                <div class="px-3 pt-2 bg-light border-bottom">
                    <ul class="nav nav-tabs border-0 room-info-tabs" id="roomDetailTabs" role="tablist">
                        <li class="nav-item" role="presentation">
                            <button class="nav-link active" id="tab-overview-btn" data-bs-toggle="tab" data-bs-target="#tab-overview" type="button" role="tab" aria-controls="tab-overview" aria-selected="true">
                                <i class="bi bi-info-circle me-1"></i>Overview &amp; Amenities
                            </button>
                        </li>
                        <li class="nav-item" role="presentation">
                            <button class="nav-link" id="tab-reviews-btn" data-bs-toggle="tab" data-bs-target="#tab-reviews" type="button" role="tab" aria-controls="tab-reviews" aria-selected="false">
                                <i class="bi bi-star-fill text-warning me-1"></i>Reviews (${reviewCount})
                            </button>
                        </li>
                        <li class="nav-item" role="presentation">
                            <button class="nav-link" id="tab-rules-btn" data-bs-toggle="tab" data-bs-target="#tab-rules" type="button" role="tab" aria-controls="tab-rules" aria-selected="false">
                                <i class="bi bi-shield-check me-1"></i>House Rules
                            </button>
                        </li>
                    </ul>
                </div>

                <div class="card-body p-3 p-md-4 room-info-tab-content">
                    <div class="tab-content" id="roomDetailTabsContent">
                        <!-- Tab 1: Overview & Amenities -->
                        <div class="tab-pane fade show active" id="tab-overview" role="tabpanel" aria-labelledby="tab-overview-btn">
                            <t:if test="${not empty card.room.description}">
                                <p class="text-secondary small mb-3">${e:esc(card.room.description)}</p>
                            </t:if>
                            <t:if test="${not empty card.room.amenities}">
                                <h3 class="h6 fw-bold mb-2 text-espresso">Available Amenities</h3>
                                <div class="amenity-grid-2col" role="list">
                                    <t:forEach items="${e:amenities(card.room.amenities)}" var="am">
                                        <div class="amenity-item" role="listitem">
                                            <t:if test="${am.hasIcon}">
                                                <img src="${pageContext.request.contextPath}/assets/images/amenities/${am.iconFile}" alt="" class="amenity-icon">
                                            </t:if>
                                            <span>${e:esc(am.label)}</span>
                                        </div>
                                    </t:forEach>
                                </div>
                            </t:if>
                        </div>

                        <!-- Tab 2: Guest Reviews -->
                        <div class="tab-pane fade" id="tab-reviews" role="tabpanel" aria-labelledby="tab-reviews-btn">
                            <div class="d-flex justify-content-between align-items-center mb-3">
                                <h3 class="h6 fw-bold mb-0 text-espresso">Guest Reviews</h3>
                                <t:if test="${reviewCount > 0}">
                                    <span class="badge rounded-pill bg-sand text-cocoa border border-latte">
                                        <span class="text-warning">&#9733;</span> ${avgRating} / 5 (${reviewCount})
                                    </span>
                                </t:if>
                            </div>

                            <t:if test="${reviewCount == 0}">
                                <div class="text-center py-4 text-muted small">
                                    <i class="bi bi-chat-heart fs-3 d-block mb-1 text-sand"></i>
                                    No reviews yet for this room. Be the first to share your experience!
                                </div>
                            </t:if>

                            <t:if test="${reviewCount > 0}">
                                <!-- Top 2 Reviews -->
                                <t:forEach items="${topReviews}" var="rev">
                                    <div class="border-top pt-3 pb-2 first-review-fix">
                                        <div class="d-flex justify-content-between align-items-center mb-1">
                                            <strong class="small text-espresso">${e:esc(rev.guestName)}</strong>
                                            <span class="text-muted small">${e:date(rev.createdAt)}</span>
                                        </div>
                                        <div class="text-warning small mb-1">${rev.starsHtml}</div>
                                        <t:if test="${not empty rev.comment}">
                                            <p class="small text-secondary mb-0">${e:esc(rev.comment)}</p>
                                        </t:if>
                                    </div>
                                </t:forEach>

                                <!-- Extra Reviews (Hidden until toggled) -->
                                <t:if test="${reviewCount > 2}">
                                    <div id="extraReviews" class="d-none">
                                        <t:forEach items="${extraReviews}" var="rev">
                                            <div class="border-top pt-3 pb-2">
                                                <div class="d-flex justify-content-between align-items-center mb-1">
                                                    <strong class="small text-espresso">${e:esc(rev.guestName)}</strong>
                                                    <span class="text-muted small">${e:date(rev.createdAt)}</span>
                                                </div>
                                                <div class="text-warning small mb-1">${rev.starsHtml}</div>
                                                <t:if test="${not empty rev.comment}">
                                                    <p class="small text-secondary mb-0">${e:esc(rev.comment)}</p>
                                                </t:if>
                                            </div>
                                        </t:forEach>
                                    </div>
                                    <div class="text-center mt-2 pt-2 border-top">
                                        <button type="button" id="btnToggleReviews" data-count="${reviewCount}" class="btn btn-link btn-sm text-decoration-none fw-semibold p-0" style="color: var(--color-caramel, #8C532B);">
                                            View all (${reviewCount}) &rarr;
                                        </button>
                                    </div>
                                </t:if>
                            </t:if>
                        </div>

                        <!-- Tab 3: House Rules -->
                        <div class="tab-pane fade" id="tab-rules" role="tabpanel" aria-labelledby="tab-rules-btn">
                            <h3 class="h6 fw-bold mb-3 text-espresso">Homestay Rules &amp; Regulations</h3>
                            <ul class="list-unstyled mb-0 d-flex flex-column gap-2 small text-espresso">
                                <li class="d-flex align-items-start gap-2">
                                    <i class="bi bi-x-circle-fill text-danger fs-6 mt-1"></i>
                                    <div><strong>No Smoking:</strong> Strictly no smoking or vaping inside the room or premises.</div>
                                </li>
                                <li class="d-flex align-items-start gap-2">
                                    <i class="bi bi-x-circle-fill text-danger fs-6 mt-1"></i>
                                    <div><strong>No Pets:</strong> Pets are strictly prohibited to ensure hygiene and allergen safety.</div>
                                </li>
                                <li class="d-flex align-items-start gap-2">
                                    <i class="bi bi-volume-mute-fill text-warning fs-6 mt-1"></i>
                                    <div><strong>Quiet Hours:</strong> Please keep noise to a minimum between 22:00 and 07:00.</div>
                                </li>
                                <li class="d-flex align-items-start gap-2">
                                    <i class="bi bi-key-fill text-caramel fs-6 mt-1"></i>
                                    <div><strong>Smart Lock PIN:</strong> Door PIN is automatically active during booked slots or when Early Check-in is approved.</div>
                                </li>
                            </ul>
                        </div>
                    </div>
                </div>

                <!-- Quick jump CTA to calendar -->
                <div class="card-footer bg-light p-3 border-top d-flex align-items-center justify-content-between">
                    <span class="small text-muted"><i class="bi bi-lightning-charge text-warning me-1"></i>Instant Booking</span>
                    <a href="#matrix-calendar" class="btn btn-continue-gradient btn-sm rounded-pill px-3 fw-semibold">
                        Select time slots below &darr;
                    </a>
                </div>
            </div>
        </div>
    </div>

    <!-- 3. Booking Section (Desktop 2-Columns: Left Calendar 68%, Right Summary 32%) -->
    <div class="row g-4 align-items-start mb-5" id="booking-section-row">
        <!-- Left Column: Booking Calendar (~68% on Desktop) -->
        <div class="col-12 col-lg-8">
            <div class="matrix-calendar-card" id="matrix-calendar"
                 data-room-id="${card.room.roomId}"
                 data-guest="${loggedInGuest ? 'true' : 'false'}"
                 data-rate-two="${rates.twoSlots}"
                 data-rate-three="${rates.threeSlots}"
                 data-rate-four="${rates.fourSlots}">

                <!-- Header Row with Title & Status Legend -->
                <div class="d-flex justify-content-between align-items-center flex-wrap gap-3 mb-3">
                    <div>
                        <h2 class="h4 fw-bold mb-0 text-espresso">Booking Calendar</h2>
                        <p class="text-muted small mb-0">Select your preferred time slots for the next 7 days</p>
                    </div>
                    <!-- Status Legend -->
                    <div class="matrix-legend">
                        <span class="matrix-legend-item">
                            <span class="legend-box legend-box-available"></span> Available
                        </span>
                        <span class="matrix-legend-item">
                            <img src="${pageContext.request.contextPath}/assets/images/branding/tini-smartstay-favicon-round.png"
                                 alt="" aria-hidden="true"
                                 class="legend-favicon-img" />
                            Booked / Expired
                        </span>
                        <span class="matrix-legend-item">
                            <span class="legend-box legend-box-selected"></span> Selected
                        </span>
                    </div>
                </div>

                <!-- Requirement 4: Discount Rule Info Banner -->
                <div class="matrix-discount-info-banner d-flex align-items-center gap-2 p-2 px-3 rounded-pill mb-3 shadow-2xs">
                    <i class="bi bi-tag-fill text-caramel"></i>
                    <span class="small text-cocoa">
                        <strong>Consecutive discount:</strong> 2 slots: ${rates.twoSlots}% off &middot; 3 slots: ${rates.threeSlots}% off &middot; Full day (4 slots): ${rates.fourSlots}% off
                    </span>
                </div>

                <!-- 7-Day Block Navigation Bar -->
                <div class="card border-0 rounded-4 p-2 px-3 mb-3 d-flex flex-row align-items-center justify-content-between flex-wrap gap-2 shadow-sm"
                     style="background: #FFFDF9; border: 1.5px solid #D9B991 !important;">
                    <div class="d-flex align-items-center gap-2">
                        <!-- Prev 7 Days Button -->
                        <a href="${pageContext.request.contextPath}/rooms/detail?id=${card.room.roomId}&date=${date}&weekStart=${prevWeekStart}"
                           class="btn btn-sm btn-outline-secondary rounded-pill px-3 py-1 fw-semibold ${!canGoPrev ? 'disabled opacity-50' : ''}"
                           title="Previous 7 Days">
                            <i class="bi bi-chevron-left me-1"></i> &lt; Prev 7 Days
                        </a>

                        <!-- Current 7-Day Display -->
                        <div class="px-2 py-1 bg-white rounded-pill border fw-bold text-espresso small shadow-2xs d-flex align-items-center gap-1">
                            <i class="bi bi-calendar-week text-caramel"></i>
                            <span>Week: ${startDateFormatted} &ndash; ${endDateFormatted}</span>
                        </div>

                        <!-- Next 7 Days Button -->
                        <a href="${pageContext.request.contextPath}/rooms/detail?id=${card.room.roomId}&date=${date}&weekStart=${nextWeekStart}"
                           class="btn btn-sm btn-outline-secondary rounded-pill px-3 py-1 fw-semibold ${!canGoNext ? 'disabled opacity-50' : ''}"
                           title="Next 7 Days">
                            Next 7 Days &gt; <i class="bi bi-chevron-right ms-1"></i>
                        </a>
                    </div>

                    <!-- Date Picker Jump (Weekly Block Support) -->
                    <div class="d-flex align-items-center gap-2">
                        <span class="text-muted small fw-semibold text-nowrap"><i class="bi bi-calendar3 text-caramel me-1"></i>Jump to week:</span>
                        <div class="position-relative" style="max-width: 145px;">
                            <input type="text" id="detailJumpWeekInput" class="form-control form-control-sm rounded-pill agoda-input fw-bold text-center"
                                   value="${weekStart}" readonly style="cursor: pointer; background: #FFFDF9; padding-right: 28px;"
                                   data-toggle="weekly-calendar" data-target-input="#detailJumpWeekInput"
                                   data-onselect="window.location.href='${pageContext.request.contextPath}/rooms/detail?id=${card.room.roomId}&date=' + weekStartStr + '&weekStart=' + weekStartStr;">
                            <i class="bi bi-calendar-week position-absolute end-0 top-50 translate-middle-y me-2 text-caramel pe-none" style="font-size: 0.82rem;"></i>
                        </div>
                    </div>
                </div>

                <!-- Consecutive Selection Notice Alert Banner (Above calendar table) -->
                <div id="matrix-error" class="alert alert-danger small py-2 d-none mb-3" role="alert"></div>

                <!-- 1. Desktop Matrix Grid Table (>= 992px) -->
                <div class="matrix-table-wrap d-none d-lg-block">
                    <table class="matrix-table">
                        <thead>
                            <tr>
                                <th class="matrix-th-corner">DATE &amp; TIME</th>
                                <t:forEach items="${card.slots}" var="sa">
                                    <th class="matrix-th-slot">
                                        <div class="matrix-slot-header-time">
                                            ${sa.slot.timeRange}
                                            <t:if test="${sa.slot.slotCode == 'S4'}">
                                                <i class="bi bi-moon-stars ms-1 text-caramel"></i>
                                            </t:if>
                                            <t:if test="${sa.slot.slotCode != 'S4'}">
                                                <i class="bi bi-brightness-high ms-1 text-warning"></i>
                                            </t:if>
                                        </div>
                                        <div class="matrix-slot-header-name">
                                            ${e:esc(sa.slot.slotCode)} &middot; ${e:esc(sa.slot.slotName)}
                                            <t:if test="${sa.slot.slotCode == 'S4'}">
                                                <span class="text-caramel fw-semibold">(Overnight)</span>
                                            </t:if>
                                        </div>
                                    </th>
                                </t:forEach>
                            </tr>
                        </thead>
                        <tbody>
                            <t:forEach items="${weekSchedule}" var="daySchedule">
                                <tr class="${(daySchedule.dayOfWeekName == 'Today') && daySchedule.allSlotsPast ? 'matrix-row-dimmed' : ''}">
                                    <!-- Day Header Column -->
                                    <td class="matrix-day-cell">
                                        <div class="matrix-day-name">${daySchedule.dayOfWeekName}</div>
                                        <div class="matrix-day-date">${daySchedule.dateFormatted}</div>
                                    </td>

                                    <!-- 4 Slot Cells for this Day -->
                                    <t:forEach items="${daySchedule.slots}" var="sa">
                                        <td>
                                            <t:if test="${sa.selectable}">
                                                <!-- Available Slot: Clickable -->
                                                <button type="button"
                                                        class="matrix-slot-btn matrix-slot-available"
                                                        data-date="${daySchedule.date}"
                                                        data-date-formatted="${daySchedule.dayOfWeekName}, ${daySchedule.dateFormatted}"
                                                        data-code="${e:esc(sa.slot.slotCode)}"
                                                        data-name="${e:esc(sa.slot.slotName)}"
                                                        data-time="${sa.slot.timeRange}"
                                                        data-sort="${sa.slot.sortOrder}"
                                                        data-price="${sa.slot.listPrice}"
                                                        data-price-formatted="${e:money(sa.slot.listPrice)}"
                                                        title="Click to select this time slot">
                                                    <span class="matrix-slot-price">${e:money(sa.slot.listPrice)}</span>
                                                </button>
                                            </t:if>

                                            <t:if test="${!sa.selectable}">
                                                <!-- Booked / Past Slot -->
                                                <div class="matrix-slot-btn matrix-slot-red" title="${e:esc(sa.label)}">
                                                    <t:if test="${sa.state == 'STARTED'}">
                                                        <i class="bi bi-clock-history slot-past-icon"></i>
                                                        <span class="slot-red-badge">Expired</span>
                                                    </t:if>
                                                    <t:if test="${sa.state != 'STARTED'}">
                                                        <img src="${pageContext.request.contextPath}/assets/images/branding/tini-smartstay-favicon-round.png"
                                                             alt="Booked"
                                                             class="slot-booked-favicon-large" />
                                                        <span class="slot-red-badge">Booked</span>
                                                    </t:if>
                                                </div>
                                            </t:if>
                                        </td>
                                    </t:forEach>
                                </tr>
                            </t:forEach>
                        </tbody>
                    </table>
                </div>

                <!-- 2. Mobile 7-Day Slot Picker (< 992px) -->
                <div class="mobile-slot-picker d-lg-none mb-3">
                    <!-- Day Strip Pills (Horizontally Scrollable) -->
                    <div class="mobile-day-strip mb-3">
                        <t:forEach items="${weekSchedule}" var="daySchedule">
                            <button type="button"
                                    class="mobile-day-pill rounded-4 p-2 flex-shrink-0 ${daySchedule.dayOfWeekName == 'Today' ? 'is-today' : ''} ${loop.first ? 'active' : ''}"
                                    data-target-date="${daySchedule.date}">
                                <div class="mobile-pill-day">${daySchedule.dayOfWeekName}</div>
                                <div class="mobile-pill-date">${daySchedule.dateFormatted.substring(0, 5)}</div>
                                <t:if test="${daySchedule.dayOfWeekName == 'Today'}">
                                    <span class="badge badge-today-dot">Today</span>
                                </t:if>
                            </button>
                        </t:forEach>
                    </div>

                    <!-- 2x2 Slots Grid for each day -->
                    <div class="mobile-days-container">
                        <t:forEach items="${weekSchedule}" var="daySchedule">
                            <div class="mobile-day-grid ${loop.first ? '' : 'd-none'}" id="mobile-day-${daySchedule.date}">
                                <div class="d-flex align-items-center justify-content-between mb-2">
                                    <span class="small fw-bold text-espresso">
                                        <i class="bi bi-calendar-check me-1 text-caramel"></i>${daySchedule.dayOfWeekName}, ${daySchedule.dateFormatted}
                                    </span>
                                    <span class="small text-muted">4 time slots</span>
                                </div>
                                <div class="mobile-slots-2x2">
                                    <t:forEach items="${daySchedule.slots}" var="sa">
                                        <t:if test="${sa.selectable}">
                                            <button type="button"
                                                    class="mobile-slot-card matrix-slot-available"
                                                    data-date="${daySchedule.date}"
                                                    data-date-formatted="${daySchedule.dayOfWeekName}, ${daySchedule.dateFormatted.substring(0, 5)}"
                                                    data-code="${e:esc(sa.slot.slotCode)}"
                                                    data-name="${e:esc(sa.slot.slotName)}"
                                                    data-time="${sa.slot.timeRange}"
                                                    data-sort="${sa.slot.sortOrder}"
                                                    data-price="${sa.slot.listPrice}"
                                                    data-price-formatted="${e:money(sa.slot.listPrice)}">
                                                <div class="card-slot-header d-flex justify-content-between align-items-center w-100">
                                                    <span class="card-slot-code">${e:esc(sa.slot.slotCode)}</span>
                                                    <span class="card-slot-badge-status badge-status-avail">Available</span>
                                                </div>
                                                <div class="card-slot-name">${e:esc(sa.slot.slotName)}</div>
                                                <div class="card-slot-time"><i class="bi bi-clock me-1"></i>${sa.slot.timeRange}</div>
                                                <div class="card-slot-price">${e:money(sa.slot.listPrice)}</div>
                                            </button>
                                        </t:if>
                                        <t:if test="${!sa.selectable}">
                                            <div class="mobile-slot-card mobile-slot-disabled"
                                                 data-date="${daySchedule.date}"
                                                 data-code="${e:esc(sa.slot.slotCode)}">
                                                <div class="card-slot-header d-flex justify-content-between align-items-center w-100">
                                                    <span class="card-slot-code text-muted">${e:esc(sa.slot.slotCode)}</span>
                                                    <span class="card-slot-badge-status badge-status-disabled">
                                                        <t:if test="${sa.state == 'STARTED'}">Expired</t:if>
                                                        <t:if test="${sa.state != 'STARTED'}">${e:esc(sa.label)}</t:if>
                                                    </span>
                                                </div>
                                                <div class="card-slot-name text-muted">${e:esc(sa.slot.slotName)}</div>
                                                <div class="card-slot-time text-muted"><i class="bi bi-clock me-1"></i>${sa.slot.timeRange}</div>
                                                <div class="card-slot-price text-muted text-decoration-line-through">${e:money(sa.slot.listPrice)}</div>
                                            </div>
                                        </t:if>
                                    </t:forEach>
                                </div>
                            </div>
                        </t:forEach>
                    </div>
                </div>
            </div>
        </div>

        <!-- Right Column: Dedicated Desktop Booking Summary Card (~32% on Desktop, Hidden on Mobile/Tablet) -->
        <div class="col-12 col-lg-4 d-none d-lg-block">
            <div class="desktop-booking-summary sticky-top" style="top: 90px; z-index: 90;">
                <div class="booking-card-header">
                    <div class="d-flex align-items-center justify-content-between">
                        <div class="d-flex align-items-center gap-2">
                            <i class="bi bi-receipt-cutoff text-warning fs-5"></i>
                            <span class="fw-bold text-white small">Booking Summary</span>
                        </div>
                        <button type="button" class="btn btn-outline-light btn-sm rounded-pill px-2 py-0 btn-discount-popover" style="font-size: 0.72rem;"
                                data-bs-toggle="popover" data-bs-placement="left" data-bs-html="true"
                                data-bs-title="<div class='fw-bold text-espresso py-1'>Multi-Slot Discounts</div>"
                                data-bs-content="<div class='small text-cocoa'><div class='d-flex justify-content-between py-1 border-bottom'><span>2 consecutive slots:</span><strong class='text-danger ms-3'>${rates.twoSlots}% OFF</strong></div><div class='d-flex justify-content-between py-1 border-bottom'><span>3 consecutive slots:</span><strong class='text-danger ms-3'>${rates.threeSlots}% OFF</strong></div><div class='d-flex justify-content-between pt-1'><span>Full 4 slots (24h):</span><strong class='text-danger ms-3'>${rates.fourSlots}% OFF</strong></div></div>">
                            <i class="bi bi-tag-fill text-warning me-1"></i>Discounts
                        </button>
                    </div>
                </div>

                <div class="booking-card-body">
                    <!-- State 1: When no slot is selected -->
                    <div id="sticky-empty-state" class="text-center py-4">
                        <i class="bi bi-calendar2-check fs-1 d-block mb-2 text-caramel"></i>
                        <div class="fw-bold text-espresso">No Time Slot Selected</div>
                        <div class="small mt-1 text-muted" style="font-size: 0.8rem;">Click on available time slots in the calendar on the left to start booking</div>
                    </div>

                    <!-- State 2: When 1 or more slots are selected -->
                    <div id="sticky-active-state" class="d-none">
                        <!-- Selected Date & Slots -->
                        <div class="booking-selection-box mb-3">
                            <div class="d-flex align-items-center justify-content-between flex-wrap gap-1">
                                <span class="booking-selection-date" id="sticky-date-slots">--</span>
                                <span class="badge rounded-pill px-2 py-1 small fw-bold text-white bg-caramel" id="sticky-slot-badges" style="font-size: 0.73rem;">--</span>
                            </div>
                            <div class="booking-selection-time" id="sticky-time-ranges">--</div>
                        </div>

                        <!-- Pricing breakdown -->
                        <div class="mb-3">
                            <div class="d-flex justify-content-between align-items-baseline mb-1">
                                <span class="small text-muted" style="font-size: 0.78rem;">Original Price:</span>
                                <span id="sticky-subtotal" class="small text-decoration-line-through d-none" style="color: #8C7A6B;">0 VND</span>
                            </div>
                            <div class="d-flex justify-content-between align-items-center mb-1">
                                <span class="small text-muted" style="font-size: 0.78rem;">Consecutive Discount:</span>
                                <span id="sticky-discount-badge" class="badge bg-danger rounded-pill px-2 py-1 small d-none">-0%</span>
                            </div>
                            <div class="d-flex justify-content-between align-items-baseline pt-2 border-top border-latte">
                                <span class="fw-bold text-espresso small">Total Amount:</span>
                                <span id="sticky-total" class="fs-4 fw-bold font-monospace text-caramel">0 VND</span>
                            </div>
                        </div>

                        <!-- Dynamic upsell suggestion -->
                        <div id="sticky-suggestion" class="fw-semibold mb-3 small text-caramel" style="font-size: 0.75rem;"></div>

                        <!-- Terms Checkbox & Payment Button -->
                        <t:if test="${loggedInGuest}">
                            <div class="form-check mb-3">
                                <input class="form-check-input" type="checkbox" id="termsConsent">
                                <label class="form-check-label small text-cocoa" for="termsConsent" style="font-size: 0.75rem; line-height: 1.35;">
                                    I agree to the <a href="${pageContext.request.contextPath}/terms" target="_blank" class="fw-semibold text-decoration-underline text-caramel">Terms &amp; Conditions</a> and <a href="javascript:void(0)" data-bs-toggle="modal" data-bs-target="#houseRulesModal" class="fw-semibold text-decoration-underline text-caramel">House Rules</a>
                                </label>
                            </div>
                            <button type="button" id="btn-continue" class="btn btn-continue-gradient fw-bold w-100 py-2 rounded-pill shadow-sm d-flex align-items-center justify-content-center gap-2" disabled>
                                <span>Continue to Payment</span> <i class="bi bi-arrow-right"></i>
                            </button>
                        </t:if>
                        <t:if test="${!loggedInGuest}">
                            <a id="btn-login-to-book" href="${pageContext.request.contextPath}/login?next=${e:url(currentDetailUrl)}" class="btn btn-continue-gradient fw-bold w-100 py-2 rounded-pill shadow-sm text-center d-flex align-items-center justify-content-center gap-2">
                                <span>Login to Book</span> <i class="bi bi-arrow-right"></i>
                            </a>
                        </t:if>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <!-- Hidden Master Booking Form for Submission -->
    <form id="booking-form" method="post" action="${pageContext.request.contextPath}/guest/bookings" class="d-none">
        <input type="hidden" name="_csrf" value="${e:esc(_csrf)}">
        <input type="hidden" name="roomId" value="${card.room.roomId}">
        <input type="hidden" name="slotDate" id="bookingSlotDate" value="">
        <div id="bookingSlotCodes"></div>
    </form>

    <!-- 4. Mobile Agoda-Style Compact Sticky Bottom Bar (< 992px) -->
    <div id="mobile-booking-bar" class="mobile-booking-bar d-lg-none d-none">
        <div class="mobile-bar-inner d-flex align-items-center justify-content-between h-100">
            <!-- Left Info: Slots, Discount, Price, Details Toggle (never clipped, wraps gracefully) -->
            <div class="mobile-bar-left">
                <div class="d-flex align-items-center gap-1 flex-wrap">
                    <span class="badge rounded-pill text-white bg-caramel" id="mobile-bar-slots-count" style="font-size: 0.72rem;">1 slot</span>
                    <span class="badge rounded-pill bg-danger text-white d-none" id="mobile-bar-discount" style="font-size: 0.72rem;">-10%</span>
                    <button type="button" class="btn btn-link text-decoration-none p-0 ms-1 mobile-bar-toggle-btn" id="mobileBarToggleBtn">
                        <span class="small fw-semibold text-latte" style="font-size: 0.75rem;" id="mobileBarToggleText">&#9650; Details</span>
                    </button>
                </div>
                <div class="mobile-bar-price fw-bold text-white fs-5 font-monospace lh-1 mt-1" id="mobile-bar-price">
                    0 VND
                </div>
            </div>
            <!-- Right Action: Thumb-friendly Book Now Button -->
            <div class="mobile-bar-right flex-shrink-0 ms-2">
                <button type="button" class="btn btn-mobile-book-now fw-bold px-3 py-2 rounded-pill shadow-sm" id="mobileBarBookBtn">
                    Book Now &rarr;
                </button>
            </div>
        </div>
    </div>

    <!-- Mobile Bottom Sheet Drawer Backdrop -->
    <div id="mobile-drawer-backdrop" class="mobile-drawer-backdrop d-lg-none"></div>

    <!-- Mobile Bottom Sheet Drawer (< 992px) -->
    <div id="mobile-booking-drawer" class="mobile-booking-drawer d-lg-none">
        <div class="mobile-drawer-header d-flex align-items-center justify-content-between p-3 border-bottom bg-cocoa text-milk">
            <div class="d-flex align-items-center gap-2">
                <i class="bi bi-receipt-cutoff text-warning fs-5"></i>
                <span class="fw-bold">Booking Details</span>
            </div>
            <button type="button" class="btn-close btn-close-white" id="mobileDrawerCloseBtn" aria-label="Close"></button>
        </div>
        <div class="mobile-drawer-body p-3 bg-milk">
            <!-- Selection Box -->
            <div class="booking-selection-box mb-3">
                <div class="d-flex align-items-center justify-content-between flex-wrap gap-1 mb-2">
                    <span class="booking-selection-date" id="mobile-drawer-date-slots">--</span>
                    <span class="badge rounded-pill px-2 py-1 small fw-bold text-white bg-caramel" id="mobile-drawer-slot-badges" style="font-size: 0.73rem;">--</span>
                </div>
                <!-- Individual Slot Chips (e.g. Sat 24/10 · S1) -->
                <div id="mobile-drawer-chips" class="d-flex flex-wrap gap-1 mb-2"></div>
                <div class="booking-selection-time" id="mobile-drawer-time-ranges">--</div>
            </div>

            <!-- Price Breakdown -->
            <div class="mb-3">
                <div class="d-flex justify-content-between align-items-baseline mb-1">
                    <span class="small text-muted" style="font-size: 0.78rem;">Original Price:</span>
                    <span id="mobile-drawer-subtotal" class="small text-decoration-line-through d-none" style="color: #8C7A6B;">0 VND</span>
                </div>
                <div class="d-flex justify-content-between align-items-center mb-1">
                    <span class="small text-muted" style="font-size: 0.78rem;">Consecutive Discount:</span>
                    <div class="text-end">
                        <span id="mobile-drawer-discount-badge" class="badge bg-danger rounded-pill px-2 py-1 small d-none">-0%</span>
                        <span id="mobile-drawer-discount-rule" class="small text-danger fw-semibold ms-1 d-none"></span>
                    </div>
                </div>
                <div class="d-flex justify-content-between align-items-baseline pt-2 border-top border-latte">
                    <span class="fw-bold text-espresso small">Total Amount:</span>
                    <span id="mobile-drawer-total" class="fs-4 fw-bold font-monospace text-caramel">0 VND</span>
                </div>
            </div>

            <div id="mobile-drawer-suggestion" class="fw-semibold mb-3 small text-caramel" style="font-size: 0.75rem;"></div>

            <t:if test="${loggedInGuest}">
                <div class="form-check mb-3">
                    <input class="form-check-input" type="checkbox" id="mobileTermsConsent">
                    <label class="form-check-label small text-cocoa" for="mobileTermsConsent" style="font-size: 0.75rem; line-height: 1.35;">
                        I agree to the <a href="${pageContext.request.contextPath}/terms" target="_blank" class="fw-semibold text-decoration-underline text-caramel">Terms &amp; Conditions</a> and <a href="javascript:void(0)" data-bs-toggle="modal" data-bs-target="#houseRulesModal" class="fw-semibold text-decoration-underline text-caramel">House Rules</a>
                    </label>
                </div>
                <button type="button" id="btn-mobile-continue" class="btn btn-continue-gradient fw-bold w-100 py-3 rounded-pill shadow-sm d-flex align-items-center justify-content-center gap-2" disabled>
                    <span>Continue to Payment</span> <i class="bi bi-arrow-right"></i>
                </button>
            </t:if>
            <t:if test="${!loggedInGuest}">
                <a href="${pageContext.request.contextPath}/login?next=${e:url(currentDetailUrl)}" class="btn btn-continue-gradient fw-bold w-100 py-3 rounded-pill shadow-sm text-center d-flex align-items-center justify-content-center gap-2">
                    <span>Login to Book</span> <i class="bi bi-arrow-right"></i>
                </a>
            </t:if>
        </div>
    </div>

    <!-- Floating Top-Center Notification Toast Container -->
    <div id="matrix-toast-container" class="matrix-toast-container"></div>

    <!-- House Rules Quick View Modal -->
    <div class="modal fade" id="houseRulesModal" tabindex="-1" aria-labelledby="houseRulesModalLabel" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered">
            <div class="modal-content" style="border-radius: 20px; border: 1.5px solid #EADBCC; overflow: hidden;">
                <div class="modal-header py-3" style="background: linear-gradient(135deg, #3A2317 0%, #523526 100%); color: #FFF;">
                    <div class="d-flex align-items-center gap-2">
                        <i class="bi bi-shield-check text-warning fs-5"></i>
                        <h5 class="modal-title fw-bold fs-6 mb-0" id="houseRulesModalLabel">SmartStay House Rules &amp; Regulations</h5>
                    </div>
                    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <div class="modal-body p-4" style="background-color: #FAF5EE;">
                    <ul class="list-unstyled mb-0 d-flex flex-column gap-3 small text-espresso">
                        <li class="d-flex align-items-start gap-2">
                            <i class="bi bi-x-circle-fill text-danger fs-6 mt-1"></i>
                            <div><strong>No Smoking:</strong> Strictly no smoking (including e-cigarettes / vapes) inside the room or premises.</div>
                        </li>
                        <li class="d-flex align-items-start gap-2">
                            <i class="bi bi-x-circle-fill text-danger fs-6 mt-1"></i>
                            <div><strong>No Pets Allowed:</strong> To ensure hygiene and allergen safety for future guests, pets are strictly not permitted.</div>
                        </li>
                        <li class="d-flex align-items-start gap-2">
                            <i class="bi bi-volume-mute-fill text-warning fs-6 mt-1"></i>
                            <div><strong>Quiet Hours:</strong> Please keep noise to a minimum; no loud music or parties between 22:00 and 07:00.</div>
                        </li>
                        <li class="d-flex align-items-start gap-2">
                            <i class="bi bi-key-fill text-caramel fs-6 mt-1"></i>
                            <div><strong>Smart Lock &amp; Check-out:</strong> Door PIN is active only during your booked slots. Please check out on time to avoid extra fees.</div>
                        </li>
                    </ul>
                </div>
                <div class="modal-footer py-2 px-4 border-0" style="background-color: #FAF5EE;">
                    <button type="button" class="btn btn-outline-secondary btn-sm rounded-pill px-3" data-bs-dismiss="modal">Close</button>
                    <a href="${pageContext.request.contextPath}/terms" target="_blank" class="btn btn-continue-gradient btn-sm rounded-pill px-3 text-white">View Full Terms &amp; Policies</a>
                </div>
            </div>
        </div>
    </div>

    <!-- Room Photo Lightbox Modal Dialog (Translucent Cream/Caramel Theme) -->
    <div id="roomPhotoLightbox" class="room-lightbox-modal" role="dialog" aria-modal="true" aria-label="Room photo viewer" tabindex="-1">
        <div class="lb-backdrop"></div>
        <div class="lb-dialog" tabindex="-1">
            <div class="lb-header">
                <div class="lb-counter">Photo 1 of 5</div>
                <button type="button" class="lb-close-btn" aria-label="Close photo viewer">&times;</button>
            </div>
            <div class="lb-stage">
                <button type="button" class="lb-nav-btn lb-prev" aria-label="Previous photo">
                    <i class="bi bi-chevron-left" aria-hidden="true"></i>
                </button>
                <div class="lb-img-wrap">
                    <img src="${pageContext.request.contextPath}/${card.room.resolvedImageUrl}" alt="${e:esc(card.room.displayTitle)} full view" class="lb-main-img" onerror="this.onerror=null; this.src='${pageContext.request.contextPath}/assets/images/branding/hero_img2.jpg';">
                </div>
                <button type="button" class="lb-nav-btn lb-next" aria-label="Next photo">
                    <i class="bi bi-chevron-right" aria-hidden="true"></i>
                </button>
            </div>
            <div class="lb-thumbs-wrap">
                <div class="lb-thumbs" role="region" aria-label="Enlarged photo thumbnails"></div>
            </div>
        </div>
    </div>

    <!-- Interactive Scripts -->
    <script src="${pageContext.request.contextPath}/assets/js/gallery.js"></script>
    <script src="${pageContext.request.contextPath}/assets/js/slot-matrix.js"></script>
</t:layout>
