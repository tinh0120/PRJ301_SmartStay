<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="e" uri="/WEB-INF/tld/smartstay.tld" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:layout title="${room.roomId > 0 ? 'Edit Room' : 'New Room'}" active="admin-rooms">
    <%-- R8: Details tab only. Prices, room status and slot status live on the Slots & Pricing tab
         (room-slots.jsp); field names, CSRF and the image manager hooks are unchanged. --%>
    <div class="ss-page-header ss-rm-form__header">
        <div>
            <a href="${pageContext.request.contextPath}/admin/rooms" class="ss-rm-form__back">
                <i class="bi bi-arrow-left" aria-hidden="true"></i>Rooms
            </a>
            <h1 class="ss-page-header__title">
                <t:if test="${room.roomId > 0}">${e:esc(room.displayTitle)} <span class="ss-rm-form__code">${e:esc(room.roomNumber)}</span></t:if>
                <t:if test="${room.roomId <= 0}">New room</t:if>
            </h1>
        </div>
    </div>

    <t:roomTabs roomId="${room.roomId}" active="details"/>

    <t:if test="${not empty errors['general']}">
        <div class="ss-toast ss-toast--error" role="alert">${e:esc(errors['general'])}</div>
    </t:if>

    <form action="${pageContext.request.contextPath}/admin/rooms/edit?_csrf=${e:url(not empty _csrf ? _csrf : sessionScope._csrf)}"
          method="post" enctype="multipart/form-data" novalidate id="roomForm" class="ss-rm-form">
        <input type="hidden" name="_csrf" value="${e:esc(not empty _csrf ? _csrf : sessionScope._csrf)}">
        <input type="hidden" name="roomId" value="${room.roomId}">

        <!-- 1. Basic info -->
        <section class="ss-rm-section" aria-labelledby="secBasic">
            <h2 class="ss-rm-section__title" id="secBasic">Basic info</h2>
            <div class="row g-3">
                <div class="col-md-8">
                    <label for="displayName" class="form-label fw-semibold">Name <span class="text-danger">*</span></label>
                    <input type="text" class="form-control ${not empty errors['displayName'] ? 'is-invalid' : ''}"
                           id="displayName" name="displayName" maxlength="100" required
                           placeholder="e.g. Butter Beige" value="${e:esc(room.displayName)}">
                    <t:if test="${not empty errors['displayName']}">
                        <div class="invalid-feedback d-block">${e:esc(errors['displayName'])}</div>
                    </t:if>
                </div>
                <div class="col-md-4">
                    <label for="roomNumber" class="form-label fw-semibold">Code <span class="text-danger">*</span></label>
                    <input type="text" class="form-control ${not empty errors['roomNumber'] ? 'is-invalid' : ''}"
                           id="roomNumber" name="roomNumber" maxlength="20" required
                           placeholder="e.g. A101" value="${e:esc(room.roomNumber)}">
                    <t:if test="${not empty errors['roomNumber']}">
                        <div class="invalid-feedback d-block">${e:esc(errors['roomNumber'])}</div>
                    </t:if>
                </div>
                <div class="col-6 col-md-3">
                    <label for="floorNumber" class="form-label fw-semibold">Floor <span class="text-danger">*</span></label>
                    <input type="number" class="form-control ${not empty errors['floorNumber'] ? 'is-invalid' : ''}"
                           id="floorNumber" name="floorNumber" min="0" required value="${room.floorNumber}">
                    <t:if test="${not empty errors['floorNumber']}">
                        <div class="invalid-feedback d-block">${e:esc(errors['floorNumber'])}</div>
                    </t:if>
                </div>
                <div class="col-6 col-md-3">
                    <label for="capacity" class="form-label fw-semibold">Guests <span class="text-danger">*</span></label>
                    <input type="number" class="form-control ${not empty errors['capacity'] ? 'is-invalid' : ''}"
                           id="capacity" name="capacity" min="1" required value="${room.capacity}">
                    <t:if test="${not empty errors['capacity']}">
                        <div class="invalid-feedback d-block">${e:esc(errors['capacity'])}</div>
                    </t:if>
                </div>
                <div class="col-md-6">
                    <label for="locationDescription" class="form-label fw-semibold">Location</label>
                    <input type="text" class="form-control ${not empty errors['locationDescription'] ? 'is-invalid' : ''}"
                           id="locationDescription" name="locationDescription" maxlength="255"
                           placeholder="e.g. 1st floor, lake-facing wing" value="${e:esc(room.locationDescription)}">
                    <t:if test="${not empty errors['locationDescription']}">
                        <div class="invalid-feedback d-block">${e:esc(errors['locationDescription'])}</div>
                    </t:if>
                </div>
                <div class="col-12">
                    <label for="description" class="form-label fw-semibold">Description</label>
                    <textarea class="form-control ${not empty errors['description'] ? 'is-invalid' : ''}"
                              id="description" name="description" rows="3" maxlength="500"
                              placeholder="What makes this room pleasant to stay in?">${e:esc(room.description)}</textarea>
                    <t:if test="${not empty errors['description']}">
                        <div class="invalid-feedback d-block">${e:esc(errors['description'])}</div>
                    </t:if>
                </div>
                <div class="col-12">
                    <label class="form-label fw-semibold" for="amenityInput">Amenities</label>
                    <input type="hidden" id="amenities" name="amenities" value="${e:esc(room.amenities)}">
                    <div class="ss-rm-chips" id="amenityChipContainer">
                        <div id="amenityChips" class="ss-rm-chips__list"></div>
                        <input type="text" id="amenityInput" class="ss-rm-chips__input" placeholder="Type an amenity and press Enter">
                    </div>
                    <div class="form-text">Press Enter or comma to add, Backspace to remove the last one.</div>
                    <t:if test="${not empty errors['amenities']}">
                        <div class="invalid-feedback d-block">${e:esc(errors['amenities'])}</div>
                    </t:if>
                </div>
            </div>
        </section>

        <!-- 2. Photos -->
        <section class="ss-rm-section" aria-labelledby="secPhotos">
            <h2 class="ss-rm-section__title" id="secPhotos">Photos</h2>
            <input type="hidden" name="imageUrl" id="mainImageUrlInput" value="${e:esc(room.imageUrl)}">
            <input type="hidden" name="deleteImageName" id="deleteImageNameInput" value="">

            <t:if test="${empty galleryImages}">
                <p class="ss-rm-section__hint">No photos yet. Upload the first one below; it becomes the main photo.</p>
            </t:if>
            <div class="ss-rm-photos" id="galleryThumbsGrid">
                <t:forEach items="${galleryImages}" var="img">
                    <div class="ss-rm-photo image-card ${img.isMain ? 'ss-rm-photo--main' : ''}" data-img-name="${e:esc(img.name)}" data-img-url="${e:esc(img.url)}">
                        <div class="ss-rm-photo__frame card">
                            <img src="${img.url}" class="ss-rm-photo__img" width="768" height="576" alt="${e:esc(img.name)}"
                                 onerror="this.onerror=null; this.src='${pageContext.request.contextPath}/assets/images/branding/hero_img2.jpg';">
                            <t:if test="${img.isMain}">
                                <span class="ss-rm-photo__badge main-badge">Main</span>
                            </t:if>
                        </div>
                        <div class="ss-rm-photo__actions">
                            <button type="button" class="ss-rm-photo__btn btn-set-main" ${img.isMain ? 'aria-pressed="true"' : ''}>
                                ${img.isMain ? 'Main photo' : 'Set as main'}
                            </button>
                            <button type="button" class="ss-rm-photo__btn ss-rm-photo__btn--danger btn-delete-image" aria-label="Delete ${e:esc(img.name)}">
                                <i class="bi bi-trash" aria-hidden="true"></i>
                            </button>
                        </div>
                    </div>
                </t:forEach>
            </div>

            <div class="ss-rm-upload">
                <label for="imageFile" class="form-label fw-semibold">Upload a photo</label>
                <input type="file" class="form-control ${not empty errors['imageFile'] ? 'is-invalid' : ''}"
                       id="imageFile" name="imageFile" accept=".jpg,.jpeg,.png,.webp">
                <div class="form-text">JPG, PNG or WEBP, up to 5 MB. Saved when you press Update room.</div>
                <t:if test="${not empty errors['imageFile']}">
                    <div class="invalid-feedback d-block">${e:esc(errors['imageFile'])}</div>
                </t:if>
                <div class="form-check mt-2">
                    <input class="form-check-input" type="checkbox" id="setAsMain" name="setAsMain" value="true">
                    <label class="form-check-label" for="setAsMain">Use as main photo</label>
                </div>
            </div>
        </section>

        <!-- 3. Visibility -->
        <section class="ss-rm-section" aria-labelledby="secVisibility">
            <h2 class="ss-rm-section__title" id="secVisibility">Visibility</h2>
            <div class="form-check form-switch ss-rm-switch">
                <input class="form-check-input" type="checkbox" role="switch" id="active" name="active" ${room.active ? 'checked' : ''}>
                <label class="form-check-label fw-semibold" for="active">Show this room to guests</label>
            </div>
            <p class="ss-rm-section__hint">When off, the room disappears from the guest search and cannot be booked. Existing bookings stay as they are.</p>
        </section>

        <!-- Sticky action bar -->
        <div class="ss-rm-actionbar">
            <button type="submit" class="btn btn-primary px-4" id="roomSaveBtn">${room.roomId > 0 ? 'Update room' : 'Create room'}</button>
            <a href="${pageContext.request.contextPath}/admin/rooms" class="btn btn-outline-soft px-4">Cancel</a>
        </div>
    </form>

    <script>
        document.addEventListener('DOMContentLoaded', function() {
            // 1. Amenities chips (hidden input #amenities keeps the "a, b, c" value the servlet expects)
            (function() {
                var hiddenInput = document.getElementById('amenities');
                var chipContainer = document.getElementById('amenityChips');
                var textInput = document.getElementById('amenityInput');
                var outerBox = document.getElementById('amenityChipContainer');
                if (!hiddenInput || !chipContainer || !textInput) return;

                if (outerBox) {
                    outerBox.addEventListener('click', function(e) {
                        if (e.target !== textInput && !e.target.closest('button')) {
                            textInput.focus();
                        }
                    });
                }

                var amenitiesList = [];
                if (hiddenInput.value && hiddenInput.value.trim().length > 0) {
                    hiddenInput.value.split(',').forEach(function(item) {
                        var trimmed = item.trim();
                        if (trimmed.length > 0 && amenitiesList.indexOf(trimmed) === -1) {
                            amenitiesList.push(trimmed);
                        }
                    });
                }

                function renderChips() {
                    chipContainer.innerHTML = '';
                    amenitiesList.forEach(function(item, idx) {
                        var chip = document.createElement('span');
                        chip.className = 'ss-chip ss-rm-chips__chip';
                        var text = document.createElement('span');
                        text.textContent = item;
                        var remove = document.createElement('button');
                        remove.type = 'button';
                        remove.className = 'ss-rm-chips__remove';
                        remove.setAttribute('aria-label', 'Remove ' + item);
                        remove.innerHTML = '<i class="bi bi-x" aria-hidden="true"></i>';
                        remove.addEventListener('click', function(e) {
                            e.preventDefault();
                            e.stopPropagation();
                            amenitiesList.splice(idx, 1);
                            renderChips();
                        });
                        chip.appendChild(text);
                        chip.appendChild(remove);
                        chipContainer.appendChild(chip);
                    });
                    hiddenInput.value = amenitiesList.join(', ');
                }

                function addAmenity(val) {
                    var clean = val.replace(/,/g, '').trim();
                    if (clean.length > 0 && amenitiesList.indexOf(clean) === -1) {
                        amenitiesList.push(clean);
                        renderChips();
                    }
                    textInput.value = '';
                }

                textInput.addEventListener('keydown', function(e) {
                    if (e.key === 'Enter' || e.key === ',') {
                        e.preventDefault();
                        addAmenity(textInput.value);
                    } else if (e.key === 'Backspace' && textInput.value === '' && amenitiesList.length > 0) {
                        amenitiesList.pop();
                        renderChips();
                    }
                });
                textInput.addEventListener('blur', function() {
                    if (textInput.value.trim().length > 0) {
                        addAmenity(textInput.value);
                    }
                });
                renderChips();
            })();

            // 2. Photos: set main / mark for delete (applied when the form is saved)
            (function() {
                var mainInput = document.getElementById('mainImageUrlInput');
                var deleteInput = document.getElementById('deleteImageNameInput');
                var slug = '${e:esc(roomSlug)}';

                document.querySelectorAll('.btn-set-main').forEach(function(btn) {
                    btn.addEventListener('click', function(e) {
                        e.preventDefault();
                        var card = this.closest('.image-card');
                        if (!card) return;
                        if (mainInput) {
                            mainInput.value = '/assets/images/rooms/' + slug + '/' + card.getAttribute('data-img-name');
                        }
                        document.querySelectorAll('.image-card').forEach(function(c) {
                            c.classList.remove('ss-rm-photo--main');
                            var b = c.querySelector('.main-badge');
                            if (b) b.remove();
                            var setBtn = c.querySelector('.btn-set-main');
                            if (setBtn) {
                                setBtn.textContent = 'Set as main';
                                setBtn.removeAttribute('aria-pressed');
                            }
                        });
                        card.classList.add('ss-rm-photo--main');
                        var badge = document.createElement('span');
                        badge.className = 'ss-rm-photo__badge main-badge';
                        badge.textContent = 'Main';
                        card.querySelector('.card').appendChild(badge);
                        this.textContent = 'Main photo';
                        this.setAttribute('aria-pressed', 'true');
                    });
                });

                document.querySelectorAll('.btn-delete-image').forEach(function(btn) {
                    btn.addEventListener('click', function(e) {
                        e.preventDefault();
                        var card = this.closest('.image-card');
                        if (!card) return;
                        var imgName = card.getAttribute('data-img-name');
                        if (confirm('Delete photo "' + imgName + '"? It is removed when you save the room.')) {
                            if (deleteInput) {
                                deleteInput.value = imgName;
                            }
                            card.classList.add('ss-rm-photo--deleted');
                            var note = document.createElement('span');
                            note.className = 'ss-rm-photo__deleted';
                            note.textContent = 'Will be deleted';
                            card.querySelector('.card').appendChild(note);
                        }
                    });
                });
            })();

            // 3. Loading state on save
            var form = document.getElementById('roomForm');
            var saveBtn = document.getElementById('roomSaveBtn');
            if (form && saveBtn) {
                form.addEventListener('submit', function() {
                    saveBtn.disabled = true;
                    saveBtn.textContent = 'Saving...';
                });
            }
        });
    </script>
</t:layout>
