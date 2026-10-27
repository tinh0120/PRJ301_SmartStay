<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="e" uri="/WEB-INF/tld/smartstay.tld" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:layout title="Home" active="home" fullWidth="true">
    <%-- V2: the hero opens with what is unique here: rooms booked by the slot, opened with your own PIN. --%>
    <section class="hero-full ss-home-hero" aria-labelledby="hero-title">
        <div class="container hero-inner">
            <div class="hero-content">
                <h1 class="hero-title ss-serif" id="hero-title">
                    Book a room for the hours you need. Open the door with your own PIN.
                </h1>
                <p class="hero-lead">
                    Pick a morning, afternoon, evening or overnight slot in Cao L&atilde;nh, pay by bank transfer,
                    and get a 6-digit door code right away. No front desk, no keys.
                </p>
                <div class="hero-actions">
                    <a href="${pageContext.request.contextPath}/rooms" class="btn btn-primary btn-lg px-4 shadow-sm">Browse rooms</a>
                    <a href="#daily-slots" class="btn btn-hero-ghost btn-lg px-4">See the time slots</a>
                </div>
                <ul class="hero-points">
                    <li>Door PIN right after payment</li>
                    <li>Free cancellation up to 48 hours before</li>
                    <li>Up to 50% off a full day</li>
                </ul>
            </div>
        </div>
    </section>

    <!-- Agoda-style Sticky / Floating Search Card on Home -->
    <div class="container position-relative" style="margin-top: -45px; z-index: 20;">
        <div class="card border-0 shadow-lg rounded-4 p-3 p-md-4 agoda-search-box ss-home-search">
            <form action="${pageContext.request.contextPath}/rooms" method="get" class="search-form-grid search-grid-home">
                <input type="hidden" id="homeHiddenWeekStart" name="weekStart" value="${today}">

                <!-- 1. Date Picker (Weekly Block Support) -->
                <div class="search-field">
                    <label for="homeAgodaDateInput" class="search-label">
                        <i class="bi bi-calendar-event me-1 text-caramel"></i>Check-in date
                    </label>
                    <div class="position-relative search-control">
                        <input type="text" id="homeAgodaDateInput" name="date" class="form-control rounded-pill px-3 py-2 fw-semibold agoda-input"
                               value="${today}" readonly style="cursor: pointer;" required
                               data-toggle="weekly-calendar" data-target-input="#homeAgodaDateInput" data-target-weekstart="#homeHiddenWeekStart">
                        <i class="bi bi-calendar3 position-absolute end-0 top-50 translate-middle-y me-3 text-caramel pe-none"></i>
                    </div>
                </div>

                <!-- 2. Slot Multi-Selector (4 equal buttons in 1 row) -->
                <div class="search-field">
                    <span class="search-label" id="homeSlotsLabel">
                        <i class="bi bi-clock-history me-1 text-caramel"></i>Time slots
                    </span>
                    <div class="slot-picker-grid search-control" role="group" aria-labelledby="homeSlotsLabel" data-slot-group>
                        <label class="slot-pill-check">
                            <input type="checkbox" name="slots" value="S1">
                            <span class="slot-pill-btn"><span class="slot-time-label">Morning</span></span>
                        </label>
                        <label class="slot-pill-check">
                            <input type="checkbox" name="slots" value="S2">
                            <span class="slot-pill-btn"><span class="slot-time-label">Afternoon</span></span>
                        </label>
                        <label class="slot-pill-check">
                            <input type="checkbox" name="slots" value="S3">
                            <span class="slot-pill-btn"><span class="slot-time-label">Evening</span></span>
                        </label>
                        <label class="slot-pill-check">
                            <input type="checkbox" name="slots" value="S4">
                            <span class="slot-pill-btn">
                                <i class="bi bi-moon-stars-fill slot-moon-icon" aria-hidden="true"></i>
                                <span class="slot-time-label">Overnight</span>
                            </span>
                        </label>
                    </div>
                </div>

                <!-- 3. Capacity -->
                <div class="search-field">
                    <label for="homeCapacitySelect" class="search-label">
                        <i class="bi bi-people me-1 text-caramel"></i>Guests
                    </label>
                    <select id="homeCapacitySelect" name="minCapacity" class="form-select rounded-pill px-3 py-2 fw-semibold agoda-input search-control">
                        <option value="">Any size</option>
                        <option value="2">2+ guests</option>
                        <option value="3">3+ guests</option>
                    </select>
                </div>

                <!-- 4. Submit Button -->
                <div class="search-field">
                    <button type="submit" class="btn btn-primary rounded-pill w-100 py-2 fw-bold d-flex align-items-center justify-content-center gap-2 shadow-sm agoda-search-btn search-control">
                        <i class="bi bi-search"></i>
                        <span>Search rooms</span>
                    </button>
                </div>
            </form>
        </div>
    </div>

    <div class="container py-4">
        <%-- Deliberate XSS check test point --%>
        <t:if test="${not empty param.q}">
            <div class="alert alert-info py-2"><p class="text-muted mb-0">Search preview: <strong>${e:esc(param.q)}</strong></p></div>
        </t:if>

        <section id="daily-slots" class="daily-slots-section section-anchor">
            <div class="row g-4 mt-1">
                <div class="col-12 text-center mb-2">
                    <h2 class="slots-title ss-serif mb-2">Four slots a day</h2>
                    <p class="slots-note text-muted mb-2">
                        Exact times vary by room; each room page shows its own schedule.
                    </p>
                    <div class="slots-discount-pill d-inline-flex align-items-center flex-wrap justify-content-center gap-2 px-3 py-1 rounded-pill">
                        <span class="fw-semibold">Book slots back to back and save:</span>
                        <span>2 slots 10% off, 3 slots 20% off, the full day 50% off</span>
                    </div>
                </div>
                <div class="col-12 col-sm-6 col-lg-3">
                    <div class="slot-overview-card h-100">
                        <div class="card-body p-4 d-flex flex-column">
                            <span class="slot-badge mb-3">Slot 1</span>
                            <h3 class="slot-name ss-serif mb-2">Morning</h3>
                            <p class="slot-desc mb-0">Great for morning meetings, quiet work sessions, or quick relaxation.</p>
                        </div>
                    </div>
                </div>
                <div class="col-12 col-sm-6 col-lg-3">
                    <div class="slot-overview-card h-100">
                        <div class="card-body p-4 d-flex flex-column">
                            <span class="slot-badge mb-3">Slot 2</span>
                            <h3 class="slot-name ss-serif mb-2">Afternoon</h3>
                            <p class="slot-desc mb-0">Ideal for afternoon focus, creative study, or mid-day rest.</p>
                        </div>
                    </div>
                </div>
                <div class="col-12 col-sm-6 col-lg-3">
                    <div class="slot-overview-card h-100">
                        <div class="card-body p-4 d-flex flex-column">
                            <span class="slot-badge mb-3">Slot 3</span>
                            <h3 class="slot-name ss-serif mb-2">Evening</h3>
                            <p class="slot-desc mb-0">Perfect for casual dining, evening wind-down, and relaxation.</p>
                        </div>
                    </div>
                </div>
                <div class="col-12 col-sm-6 col-lg-3">
                    <div class="slot-overview-card h-100">
                        <div class="card-body p-4 d-flex flex-column">
                            <span class="slot-badge mb-3">Slot 4</span>
                            <h3 class="slot-name ss-serif mb-2">Overnight</h3>
                            <p class="slot-desc mb-0">Comfortable night sleep with automated access PIN and quiet atmosphere.</p>
                        </div>
                    </div>
                </div>
            </div>
        </section>
    </div>

    <!-- About Tini SmartStay Section -->
    <section id="about" class="about-section section-anchor" aria-labelledby="about-heading">
        <div class="container py-5">
            <!-- Top Row: Brand Story & Quote -->
            <div class="row align-items-center g-4 g-lg-5 mb-5">
                <div class="col-lg-7">
                    <h2 class="about-title ss-serif" id="about-heading">
                        A quiet homestay that runs itself
                    </h2>
                    <p class="about-lead ss-measure">
                        Six themed rooms at 8 QL30, Cao L&atilde;nh, &Dstrok;&ocirc;ng Th&aacute;p. You check in with your own
                        door code, and the rooms stay calm and away from the street noise.
                    </p>
                </div>
                <div class="col-lg-5">
                    <div class="about-quote-box">
                        <blockquote class="about-quote-text ss-serif">
                            &ldquo;Every room has its own mood, from warm butter tones to fresh and breezy. Guests don&rsquo;t just sleep here &mdash; they experience something.&rdquo;
                        </blockquote>
                        <div class="about-quote-author">
                            Tini SmartStay, Cao L&atilde;nh
                        </div>
                    </div>
                </div>
            </div>

            <!-- Bottom Row: 3 Feature Cards -->
            <div class="row g-4">
                <!-- Card 01 -->
                <div class="col-12 col-md-4">
                    <div class="about-card h-100">
                        <div class="about-card-top">
                            <div class="about-card-icon" aria-hidden="true">
                                <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                    <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"></path>
                                    <circle cx="12" cy="10" r="3"></circle>
                                </svg>
                            </div>
                        </div>
                        <h3 class="about-card-title ss-serif">Easy to reach in Cao L&atilde;nh</h3>
                        <p class="about-card-desc">
                            Nestled at 8 QL30, Cao L&atilde;nh, &Dstrok;&ograve;ng Th&aacute;p, conveniently accessible yet tucked away in a quiet, peaceful sanctuary.
                        </p>
                    </div>
                </div>

                <!-- Card 02 -->
                <div class="col-12 col-md-4">
                    <div class="about-card h-100">
                        <div class="about-card-top">
                            <div class="about-card-icon" aria-hidden="true">
                                <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                    <path d="M21 2l-2 2m-1.5 1.5L14 9l2 2 3-3-2-2 4-4z"/>
                                    <circle cx="7.5" cy="16.5" r="4.5"/>
                                </svg>
                            </div>
                        </div>
                        <h3 class="about-card-title ss-serif">Self check-in, day or night</h3>
                        <p class="about-card-desc">
                            Smart lock system lets you arrive on your own schedule &mdash; no waiting, no front desk, total freedom.
                        </p>
                    </div>
                </div>

                <!-- Card 03 -->
                <div class="col-12 col-md-4">
                    <div class="about-card h-100">
                        <div class="about-card-top">
                            <div class="about-card-icon" aria-hidden="true">
                                <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                    <circle cx="13.5" cy="6.5" r=".5" fill="currentColor"></circle>
                                    <circle cx="17.5" cy="10.5" r=".5" fill="currentColor"></circle>
                                    <circle cx="8.5" cy="7.5" r=".5" fill="currentColor"></circle>
                                    <circle cx="6.5" cy="12.5" r=".5" fill="currentColor"></circle>
                                    <path d="M12 2C6.5 2 2 6.5 2 12s4.5 10 10 10c.9 0 1.6-.7 1.6-1.6 0-.4-.2-.8-.4-1.1-.3-.4-.4-.8-.4-1.3 0-1.1.9-2 2-2h2.3c3.8 0 6.9-3.1 6.9-6.9 0-5-4.4-9.1-10-9.1z"></path>
                                </svg>
                            </div>
                        </div>
                        <h3 class="about-card-title ss-serif">A different mood in every room</h3>
                        <p class="about-card-desc">
                            Every room has its own mood &mdash; from warm butter tones and vanilla calm to fresh and breezy suites. Stay differently each time.
                        </p>
                    </div>
                </div>
            </div>
        </div>
    </section>

    <!-- 6 Sweet Spaces Showcase Section (Matching Uploaded Mockup) -->
    <section id="sweet-spaces" class="showcase-section section-anchor" aria-labelledby="showcase-heading">
        <div class="container">
            <!-- Header Row -->
            <div class="d-flex justify-content-between align-items-end flex-wrap gap-3 mb-3 mb-lg-4">
                <div>
                    <h2 class="showcase-title ss-serif mb-0" id="showcase-heading">
                        Six rooms, six moods
                    </h2>
                </div>
                <div>
                    <a href="${pageContext.request.contextPath}/rooms" class="btn btn-showcase-view-all">
                        View all rooms
                    </a>
                </div>
            </div>

            <!-- Netflix-style 1-Row Slider (Peeking 4th Card + Arrow Navigation) -->
            <div class="showcase-slider-wrapper position-relative">
                <!-- Floating Left Paddle -->
                <button type="button" class="slider-nav-btn slider-nav-prev" id="showcaseSliderPrev" aria-label="Previous rooms">
                    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round">
                        <polyline points="15 18 9 12 15 6"></polyline>
                    </svg>
                </button>

                <!-- Slider Track (Horizontal, Peeking Card) -->
                <div class="showcase-slider-track" id="showcaseSliderTrack" tabindex="0" role="region" aria-label="Room carousel">
                    <t:forEach items="${showcaseRooms}" var="room">
                        <div class="showcase-slide">
                            <a href="${pageContext.request.contextPath}/rooms/detail?id=${room.roomId}" class="showcase-card-link text-decoration-none">
                                <article class="showcase-card h-100">
                                    <div class="showcase-card-img-wrap">
                                        <img src="${pageContext.request.contextPath}/${room.resolvedImageUrl}"
                                             alt="${e:esc(room.displayTitle)} main photo"
                                             class="showcase-card-img" width="768" height="576"
                                             loading="lazy"
                                             onerror="this.onerror=null; this.src='${pageContext.request.contextPath}/assets/images/branding/hero_img2.jpg';">
                                        <%-- V4: the tag repeated the room name shown right below; removed --%>
                                        <span class="showcase-card-watermark">${e:esc(room.roomNumber)}</span>
                                    </div>
                                    <div class="showcase-card-body">
                                        <h3 class="showcase-card-title ss-serif">${e:esc(room.displayTitle)}</h3>
                                        <p class="showcase-card-caption">${e:esc(room.caption)}</p>
                                        <ul class="showcase-card-meta ss-facts">
                                            <li><i class="bi bi-building" aria-hidden="true"></i>Floor ${room.floorNumber}</li>
                                            <li><i class="bi bi-people" aria-hidden="true"></i>Up to ${room.capacity} guests</li>
                                        </ul>
                                        <div class="showcase-card-footer">
                                            <div class="showcase-card-pricing">
                                                <div class="pricing-rate-3h">
                                                    <span class="rate-value">${e:money(room.minPrice)}</span>
                                                    <span class="rate-unit">/ slot</span>
                                                </div>
                                                <div class="pricing-rate-overnight">
                                                    <span class="rate-value-sub">${e:money(room.overnightPrice)}</span>
                                                    <span class="rate-unit-sub">/ overnight</span>
                                                </div>
                                            </div>
                                            <div class="showcase-card-btn" aria-hidden="true">
                                                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                                                    <line x1="5" y1="12" x2="19" y2="12"></line>
                                                    <polyline points="12 5 19 12 12 19"></polyline>
                                                </svg>
                                            </div>
                                        </div>
                                    </div>
                                </article>
                            </a>
                        </div>
                    </t:forEach>
                </div>

                <!-- Floating Right Paddle -->
                <button type="button" class="slider-nav-btn slider-nav-next" id="showcaseSliderNext" aria-label="Next rooms">
                    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round">
                        <polyline points="9 18 15 12 9 6"></polyline>
                    </svg>
                </button>
            </div>

            <!-- Slider Pagination Dots -->
            <div class="showcase-slider-dots d-flex justify-content-center align-items-center gap-2 mt-4" id="showcaseSliderDots" role="tablist" aria-label="Room slides pagination"></div>
        </div>
    </section>

    <!-- Netflix Slider Interaction Script with Dots & Touch Support -->
    <script>
        (function() {
            var track = document.getElementById('showcaseSliderTrack');
            var btnPrev = document.getElementById('showcaseSliderPrev');
            var btnNext = document.getElementById('showcaseSliderNext');
            var dotsContainer = document.getElementById('showcaseSliderDots');
            if (!track) return;

            var slides = track.querySelectorAll('.showcase-slide');
            var dots = [];

            function getStep() {
                var firstSlide = track.querySelector('.showcase-slide');
                if (firstSlide) {
                    var gap = 22;
                    try {
                        var style = window.getComputedStyle(track);
                        gap = parseFloat(style.gap) || 22;
                    } catch (e) {}
                    return firstSlide.offsetWidth + gap;
                }
                return 340;
            }

            // Build pagination dots
            if (dotsContainer && slides.length > 0) {
                dotsContainer.innerHTML = '';
                slides.forEach(function(_, idx) {
                    var dot = document.createElement('button');
                    dot.type = 'button';
                    dot.className = 'slider-dot' + (idx === 0 ? ' active' : '');
                    dot.setAttribute('role', 'tab');
                    dot.setAttribute('aria-label', 'Go to room ' + (idx + 1));
                    dot.addEventListener('click', function(e) {
                        e.preventDefault();
                        var step = getStep();
                        track.scrollTo({ left: idx * step, behavior: 'smooth' });
                    });
                    dotsContainer.appendChild(dot);
                    dots.push(dot);
                });
            }

            function slide(direction) {
                var step = getStep() * direction;
                track.scrollBy({ left: step, behavior: 'smooth' });
            }

            if (btnPrev) btnPrev.addEventListener('click', function(e) { e.preventDefault(); slide(-1); });
            if (btnNext) btnNext.addEventListener('click', function(e) { e.preventDefault(); slide(1); });

            function updateUI() {
                var atStart = track.scrollLeft <= 4;
                var atEnd = Math.ceil(track.scrollLeft + track.clientWidth) >= track.scrollWidth - 6;
                if (btnPrev) {
                    btnPrev.disabled = atStart;
                    btnPrev.classList.toggle('disabled', atStart);
                }
                if (btnNext) {
                    btnNext.disabled = atEnd;
                    btnNext.classList.toggle('disabled', atEnd);
                }

                // Update active dot based on scroll position
                if (dots.length > 0) {
                    var step = getStep();
                    var activeIndex = Math.min(dots.length - 1, Math.max(0, Math.round(track.scrollLeft / step)));
                    dots.forEach(function(dot, idx) {
                        var isActive = (idx === activeIndex);
                        dot.classList.toggle('active', isActive);
                        dot.setAttribute('aria-selected', isActive ? 'true' : 'false');
                    });
                }
            }

            // Mouse drag scroll support for desktop
            var isDown = false;
            var startX;
            var scrollLeftStart;

            track.addEventListener('mousedown', function(e) {
                if (e.target.closest('a') || e.target.closest('button')) return;
                isDown = true;
                track.classList.add('is-dragging');
                startX = e.pageX - track.offsetLeft;
                scrollLeftStart = track.scrollLeft;
            });

            window.addEventListener('mouseup', function() {
                if (!isDown) return;
                isDown = false;
                track.classList.remove('is-dragging');
                updateUI();
            });

            track.addEventListener('mousemove', function(e) {
                if (!isDown) return;
                e.preventDefault();
                var x = e.pageX - track.offsetLeft;
                var walk = (x - startX) * 1.5;
                track.scrollLeft = scrollLeftStart - walk;
            });

            track.addEventListener('scroll', updateUI, { passive: true });
            window.addEventListener('resize', updateUI);
            setTimeout(updateUI, 120);
        })();
    </script>
    <script src="${pageContext.request.contextPath}/assets/js/room-search.js"></script>
</t:layout>
