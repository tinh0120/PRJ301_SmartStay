/**
 * Tini SmartStay - Room Detail Interactive Gallery & Photo Lightbox Modal
 * Smooth thumbnail switching, next/prev carousel, and click-to-enlarge full-screen lightbox.
 * Translucent backdrop, accessible focus trap, touch swipe, and zero layout shift.
 */
(function() {
    'use strict';

    function initGallery() {
        var mainImg = document.getElementById('galleryMainImg');
        var thumbBtns = Array.prototype.slice.call(document.querySelectorAll('.gallery-thumb-btn'));
        var prevBtn = document.getElementById('galleryPrev');
        var nextBtn = document.getElementById('galleryNext');
        var zoomBtn = document.getElementById('galleryZoomBtn');

        if (!mainImg || thumbBtns.length === 0) {
            return;
        }

        var photos = [];
        var currentIndex = 0;
        var lastFocusedElement = null;

        thumbBtns.forEach(function(btn, idx) {
            var src = btn.getAttribute('data-img-src') || (btn.querySelector('img') ? btn.querySelector('img').src : '');
            var alt = btn.getAttribute('data-img-alt') || 'Room photo';
            photos.push({ src: src, alt: alt, btn: btn });

            if (btn.classList.contains('active')) {
                currentIndex = idx;
            }
        });

        function setPhoto(index) {
            if (index < 0) {
                index = thumbBtns.length - 1;
            } else if (index >= thumbBtns.length) {
                index = 0;
            }
            currentIndex = index;

            var activeBtn = thumbBtns[currentIndex];
            var newSrc = activeBtn.getAttribute('data-img-src');
            var newAlt = activeBtn.getAttribute('data-img-alt');

            if (newSrc && mainImg) {
                mainImg.src = newSrc;
                if (newAlt) {
                    mainImg.alt = newAlt;
                }
            }

            thumbBtns.forEach(function(b, idx) {
                var isActive = (idx === currentIndex);
                b.classList.toggle('active', isActive);
                b.setAttribute('aria-pressed', isActive ? 'true' : 'false');
            });

            // Update lightbox if open
            updateLightbox(currentIndex);
        }

        if (prevBtn) {
            prevBtn.addEventListener('click', function(e) {
                e.preventDefault();
                setPhoto(currentIndex - 1);
            });
        }

        if (nextBtn) {
            nextBtn.addEventListener('click', function(e) {
                e.preventDefault();
                setPhoto(currentIndex + 1);
            });
        }

        thumbBtns.forEach(function(btn, idx) {
            btn.addEventListener('click', function(e) {
                e.preventDefault();
                setPhoto(idx);
            });
        });

        // -------------------------------------------------------------
        // Photo Lightbox Modal
        // -------------------------------------------------------------
        var lightbox = document.getElementById('roomPhotoLightbox');
        var lbImg = null;
        var lbCounter = null;
        var lbThumbsContainer = null;
        var lbStage = null;
        var lbCloseBtn = null;
        var lbPrevBtn = null;
        var lbNextBtn = null;
        var lbBackdrop = null;
        var isLightboxInitialized = false;

        function setupLightboxDOM() {
            if (isLightboxInitialized && lightbox) {
                return;
            }

            if (!lightbox) {
                lightbox = document.getElementById('roomPhotoLightbox');
            }

            if (!lightbox) {
                lightbox = document.createElement('div');
                lightbox.id = 'roomPhotoLightbox';
                lightbox.className = 'room-lightbox-modal';
                lightbox.setAttribute('role', 'dialog');
                lightbox.setAttribute('aria-modal', 'true');
                lightbox.setAttribute('aria-label', 'Room photo viewer');
                lightbox.setAttribute('tabindex', '-1');

                var html =
                    '<div class="lb-backdrop"></div>' +
                    '<div class="lb-dialog" tabindex="-1">' +
                        '<div class="lb-header">' +
                            '<div class="lb-counter"></div>' +
                            '<button type="button" class="lb-close-btn" aria-label="Close photo viewer">&times;</button>' +
                        '</div>' +
                        '<div class="lb-stage">' +
                            '<button type="button" class="lb-nav-btn lb-prev" aria-label="Previous photo">' +
                                '<i class="bi bi-chevron-left" aria-hidden="true"></i>' +
                            '</button>' +
                            '<div class="lb-img-wrap">' +
                                '<img src="" alt="" class="lb-main-img">' +
                            '</div>' +
                            '<button type="button" class="lb-nav-btn lb-next" aria-label="Next photo">' +
                                '<i class="bi bi-chevron-right" aria-hidden="true"></i>' +
                            '</button>' +
                        '</div>' +
                        '<div class="lb-thumbs-wrap">' +
                            '<div class="lb-thumbs" role="region" aria-label="Enlarged photo thumbnails"></div>' +
                        '</div>' +
                    '</div>';

                lightbox.innerHTML = html;
                document.body.appendChild(lightbox);
            }

            lbBackdrop = lightbox.querySelector('.lb-backdrop');
            lbImg = lightbox.querySelector('.lb-main-img');
            lbCounter = lightbox.querySelector('.lb-counter');
            lbThumbsContainer = lightbox.querySelector('.lb-thumbs');
            lbStage = lightbox.querySelector('.lb-stage');
            lbCloseBtn = lightbox.querySelector('.lb-close-btn');
            lbPrevBtn = lightbox.querySelector('.lb-prev');
            lbNextBtn = lightbox.querySelector('.lb-next');

            // Populate thumbnails if empty
            if (lbThumbsContainer && lbThumbsContainer.children.length === 0) {
                photos.forEach(function(p, i) {
                    var tb = document.createElement('button');
                    tb.type = 'button';
                    tb.className = 'lb-thumb-item' + (i === currentIndex ? ' active' : '');
                    tb.setAttribute('aria-label', 'View photo ' + (i + 1) + ' of ' + photos.length);
                    tb.innerHTML = '<img src="' + p.src + '" alt="' + p.alt + '">';
                    tb.addEventListener('click', function(e) {
                        e.stopPropagation();
                        setPhoto(i);
                    });
                    lbThumbsContainer.appendChild(tb);
                });
            }

            // Close on click outside the image or on close button
            lightbox.addEventListener('click', function(e) {
                if (e.target.closest('.lb-main-img') ||
                    e.target.closest('.lb-nav-btn') ||
                    e.target.closest('.lb-thumb-item') ||
                    e.target.closest('.lb-counter')) {
                    return;
                }
                closeLightbox();
            });

            // Close on close button click explicitly
            if (lbCloseBtn) {
                lbCloseBtn.addEventListener('click', function(e) {
                    e.preventDefault();
                    e.stopPropagation();
                    closeLightbox();
                });
            }

            // Prev / Next button click
            if (lbPrevBtn) {
                lbPrevBtn.addEventListener('click', function(e) {
                    e.preventDefault();
                    e.stopPropagation();
                    setPhoto(currentIndex - 1);
                });
            }

            if (lbNextBtn) {
                lbNextBtn.addEventListener('click', function(e) {
                    e.preventDefault();
                    e.stopPropagation();
                    setPhoto(currentIndex + 1);
                });
            }

            // Touch swipe support for mobile (on both stage and image)
            var touchStartX = 0;
            var touchStartY = 0;
            var touchEndX = 0;
            var touchEndY = 0;

            var touchTargets = [lbStage, lbImg].filter(function(el) { return !!el; });
            touchTargets.forEach(function(targetEl) {
                targetEl.addEventListener('touchstart', function(e) {
                    if (e.changedTouches && e.changedTouches.length > 0) {
                        touchStartX = e.changedTouches[0].clientX;
                        touchStartY = e.changedTouches[0].clientY;
                        touchEndX = touchStartX;
                        touchEndY = touchStartY;
                    }
                }, { passive: true });

                targetEl.addEventListener('touchmove', function(e) {
                    if (e.changedTouches && e.changedTouches.length > 0) {
                        touchEndX = e.changedTouches[0].clientX;
                        touchEndY = e.changedTouches[0].clientY;
                    }
                }, { passive: true });

                targetEl.addEventListener('touchend', function(e) {
                    if (e.changedTouches && e.changedTouches.length > 0) {
                        touchEndX = e.changedTouches[0].clientX;
                        touchEndY = e.changedTouches[0].clientY;
                        handleSwipe();
                    }
                }, { passive: true });
            });

            function handleSwipe() {
                var diffX = touchEndX - touchStartX;
                var diffY = touchEndY - touchStartY;
                if (Math.abs(diffX) > Math.abs(diffY) && Math.abs(diffX) > 40) {
                    if (diffX < 0) {
                        // Swipe left -> next
                        setPhoto(currentIndex + 1);
                    } else {
                        // Swipe right -> prev
                        setPhoto(currentIndex - 1);
                    }
                }
            }

            // Focus trap inside dialog
            lightbox.addEventListener('keydown', function(e) {
                if (e.key === 'Tab') {
                    var focusableSelectors = 'button:not([disabled]), [tabindex]:not([tabindex="-1"])';
                    var focusables = Array.prototype.slice.call(lightbox.querySelectorAll(focusableSelectors));
                    if (focusables.length === 0) return;

                    var firstFocusable = focusables[0];
                    var lastFocusable = focusables[focusables.length - 1];

                    if (e.shiftKey) {
                        if (document.activeElement === firstFocusable || !lightbox.contains(document.activeElement)) {
                            e.preventDefault();
                            lastFocusable.focus();
                        }
                    } else {
                        if (document.activeElement === lastFocusable || !lightbox.contains(document.activeElement)) {
                            e.preventDefault();
                            firstFocusable.focus();
                        }
                    }
                }
            });

            isLightboxInitialized = true;
            updateLightbox(currentIndex);
        }

        function updateLightbox(index) {
            if (!lightbox) return;

            if (index === undefined || index === null) {
                index = currentIndex;
            }
            if (index < 0) index = 0;
            if (index >= photos.length) index = photos.length - 1;

            if (lbImg && photos[index]) {
                lbImg.src = photos[index].src;
                lbImg.alt = photos[index].alt;
            }

            if (lbCounter && photos.length > 0) {
                lbCounter.textContent = 'Photo ' + (index + 1) + ' of ' + photos.length;
            }

            if (lbThumbsContainer) {
                var items = lbThumbsContainer.querySelectorAll('.lb-thumb-item');
                items.forEach(function(item, i) {
                    item.classList.toggle('active', i === index);
                });
                if (items[index]) {
                    items[index].scrollIntoView({ behavior: 'smooth', block: 'nearest', inline: 'center' });
                }
            }
        }

        function openLightbox(triggerEl) {
            lastFocusedElement = triggerEl || document.activeElement;
            setupLightboxDOM();

            // Lock body scroll with padding compensation to avoid layout shift
            var scrollbarWidth = window.innerWidth - document.documentElement.clientWidth;
            if (scrollbarWidth > 0) {
                document.body.style.paddingRight = scrollbarWidth + 'px';
            }
            document.body.style.overflow = 'hidden';

            lightbox.classList.add('is-open');
            updateLightbox(currentIndex);

            // Set initial focus to close button
            if (lbCloseBtn) {
                setTimeout(function() {
                    lbCloseBtn.focus();
                }, 50);
            }
        }

        function closeLightbox() {
            if (lightbox && lightbox.classList.contains('is-open')) {
                lightbox.classList.remove('is-open');

                // Restore body scroll and padding compensation
                document.body.style.overflow = '';
                document.body.style.paddingRight = '';

                // Return focus to the thumbnail that opened it or active thumbnail
                var targetToFocus = lastFocusedElement;
                if (!targetToFocus || !document.body.contains(targetToFocus)) {
                    targetToFocus = thumbBtns[currentIndex] || mainImg;
                }
                if (targetToFocus && typeof targetToFocus.focus === 'function') {
                    try {
                        targetToFocus.focus();
                    } catch (err) {}
                }
            }
        }

        // Eagerly setup lightbox if container exists in DOM
        if (document.getElementById('roomPhotoLightbox')) {
            setupLightboxDOM();
        }

        // Open on main image click
        mainImg.style.cursor = 'zoom-in';
        mainImg.title = 'Click to enlarge photo';
        mainImg.addEventListener('click', function(e) {
            e.preventDefault();
            openLightbox(thumbBtns[currentIndex] || mainImg);
        });

        // Open on zoom badge click if present
        if (zoomBtn) {
            zoomBtn.addEventListener('click', function(e) {
                e.preventDefault();
                openLightbox(zoomBtn);
            });
        }

        // Global keyboard navigation
        document.addEventListener('keydown', function(e) {
            if (lightbox && lightbox.classList.contains('is-open')) {
                if (e.key === 'Escape') {
                    e.preventDefault();
                    closeLightbox();
                } else if (e.key === 'ArrowLeft') {
                    e.preventDefault();
                    setPhoto(currentIndex - 1);
                } else if (e.key === 'ArrowRight') {
                    e.preventDefault();
                    setPhoto(currentIndex + 1);
                }
            }
        });
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initGallery);
    } else {
        initGallery();
    }
})();
