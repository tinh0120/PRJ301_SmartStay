<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="e" uri="/WEB-INF/tld/smartstay.tld" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:layout title="${room.roomId > 0 ? 'Edit Room' : 'New Room'}" active="admin-rooms">
    <nav aria-label="breadcrumb" class="mb-3">
        <ol class="breadcrumb boutique-breadcrumb m-0">
            <li class="breadcrumb-item">
                <a href="${pageContext.request.contextPath}/admin/dashboard" class="boutique-breadcrumb-link">
                    <i class="bi bi-house-door me-1"></i>Dashboard
                </a>
            </li>
            <li class="breadcrumb-item">
                <a href="${pageContext.request.contextPath}/admin/rooms" class="boutique-breadcrumb-link">
                    Rooms
                </a>
            </li>
            <li class="breadcrumb-item active" aria-current="page">${room.roomId > 0 ? 'Edit Room' : 'New Room'}</li>
        </ol>
    </nav>

    <div class="row justify-content-center">
        <div class="col-lg-9">
            <div class="card shadow-sm border-0">
                <div class="card-header bg-white py-3 d-flex justify-content-between align-items-center border-bottom">
                    <div>
                        <h1 class="h4 mb-1 fw-bold text-espresso">${room.roomId > 0 ? 'Edit Room' : 'Create New Room'}</h1>
                        <t:if test="${room.roomId > 0}">
                            <p class="text-muted small mb-0">Update room details. Slot pricing is configured below.</p>
                        </t:if>
                        <t:if test="${room.roomId <= 0}">
                            <p class="text-muted small mb-0">Fill in the room details below. New rooms will automatically have 4 default time slots created.</p>
                        </t:if>
                    </div>
                    <t:if test="${room.roomId > 0 and sessionScope.permissions.contains('ROOM_SLOT_MANAGE')}">
                        <a href="${pageContext.request.contextPath}/admin/rooms/slots?roomId=${room.roomId}" class="btn btn-outline-primary btn-sm">
                            Manage Slots &amp; Pricing &rarr;
                        </a>
                    </t:if>
                </div>
                <div class="card-body p-4">
                    <t:if test="${not empty errors['general']}">
                        <div class="alert alert-danger" role="alert">${e:esc(errors['general'])}</div>
                    </t:if>

                    <form action="${pageContext.request.contextPath}/admin/rooms/edit?_csrf=${e:url(not empty _csrf ? _csrf : sessionScope._csrf)}" method="post" enctype="multipart/form-data" novalidate id="roomForm">
                        <input type="hidden" name="_csrf" value="${e:esc(not empty _csrf ? _csrf : sessionScope._csrf)}">
                        <input type="hidden" name="roomId" value="${room.roomId}">

                        <div class="row g-3 mb-3">
                            <div class="col-md-6">
                                <label for="displayName" class="form-label fw-semibold">Public Display Name <span class="text-danger">*</span></label>
                                <input type="text" class="form-control ${not empty errors['displayName'] ? 'is-invalid' : ''}"
                                       id="displayName" name="displayName" maxlength="100" required
                                       placeholder="e.g. Butter Beige" value="${e:esc(room.displayName)}">
                                <t:if test="${not empty errors['displayName']}">
                                    <div class="invalid-feedback d-block">${e:esc(errors['displayName'])}</div>
                                </t:if>
                            </div>

                            <div class="col-md-6">
                                <label for="roomNumber" class="form-label fw-semibold">Room Reference / Code <span class="text-danger">*</span></label>
                                <input type="text" class="form-control ${not empty errors['roomNumber'] ? 'is-invalid' : ''}"
                                       id="roomNumber" name="roomNumber" maxlength="20" required
                                       placeholder="e.g. A101, B202" value="${e:esc(room.roomNumber)}">
                                <t:if test="${not empty errors['roomNumber']}">
                                    <div class="invalid-feedback d-block">${e:esc(errors['roomNumber'])}</div>
                                </t:if>
                            </div>
                        </div>

                        <div class="row g-3 mb-3">
                            <div class="col-md-4">
                                <label for="floorNumber" class="form-label fw-semibold">Floor Number <span class="text-danger">*</span></label>
                                <input type="number" class="form-control ${not empty errors['floorNumber'] ? 'is-invalid' : ''}"
                                       id="floorNumber" name="floorNumber" min="0" required
                                       value="${room.floorNumber}">
                                <t:if test="${not empty errors['floorNumber']}">
                                    <div class="invalid-feedback d-block">${e:esc(errors['floorNumber'])}</div>
                                </t:if>
                            </div>

                            <div class="col-md-4">
                                <label for="capacity" class="form-label fw-semibold">Capacity (guests) <span class="text-danger">*</span></label>
                                <input type="number" class="form-control ${not empty errors['capacity'] ? 'is-invalid' : ''}"
                                       id="capacity" name="capacity" min="1" required
                                       value="${room.capacity}">
                                <t:if test="${not empty errors['capacity']}">
                                    <div class="invalid-feedback d-block">${e:esc(errors['capacity'])}</div>
                                </t:if>
                            </div>

                            <div class="col-md-4">
                                <label for="status" class="form-label fw-semibold">Operational Status <span class="text-danger">*</span></label>
                                <select class="form-select ${not empty errors['status'] ? 'is-invalid' : ''}" id="status" name="status" required>
                                    <option value="AVAILABLE" ${room.status == 'AVAILABLE' ? 'selected' : ''}>Available</option>
                                    <option value="MAINTENANCE" ${room.status == 'MAINTENANCE' ? 'selected' : ''}>Maintenance</option>
                                    <option value="NEEDS_CLEANING" ${room.status == 'NEEDS_CLEANING' ? 'selected' : ''}>Needs cleaning</option>
                                    <t:if test="${room.status == 'OCCUPIED'}">
                                        <option value="OCCUPIED" selected>Occupied</option>
                                    </t:if>
                                </select>
                                <t:if test="${not empty errors['status']}">
                                    <div class="invalid-feedback d-block">${e:esc(errors['status'])}</div>
                                </t:if>
                            </div>
                        </div>

                        <div class="mb-3">
                            <label for="locationDescription" class="form-label fw-semibold">Location Description</label>
                            <input type="text" class="form-control ${not empty errors['locationDescription'] ? 'is-invalid' : ''}"
                                   id="locationDescription" name="locationDescription" maxlength="255"
                                   placeholder="e.g. 1st Floor, Lake-facing wing" value="${e:esc(room.locationDescription)}">
                            <t:if test="${not empty errors['locationDescription']}">
                                <div class="invalid-feedback d-block">${e:esc(errors['locationDescription'])}</div>
                            </t:if>
                        </div>

                        <div class="mb-3">
                            <label for="description" class="form-label fw-semibold">Room Description</label>
                            <textarea class="form-control ${not empty errors['description'] ? 'is-invalid' : ''}"
                                      id="description" name="description" rows="3" maxlength="500"
                                      placeholder="Brief overview of room features and ambiance...">${e:esc(room.description)}</textarea>
                            <t:if test="${not empty errors['description']}">
                                <div class="invalid-feedback d-block">${e:esc(errors['description'])}</div>
                            </t:if>
                        </div>

                        <!-- Amenities: Tag-Chip Input -->
                        <div class="mb-3">
                            <label class="form-label fw-semibold">Amenities</label>
                            <input type="hidden" id="amenities" name="amenities" value="${e:esc(room.amenities)}">
                            <div class="tag-chip-container border rounded p-2 bg-white d-flex flex-wrap gap-2 align-items-center" id="amenityChipContainer" style="min-height: 46px; cursor: text;">
                                <div id="amenityChips" class="d-flex flex-wrap gap-2 align-items-center"></div>
                                <input type="text" id="amenityInput" class="border-0 shadow-none flex-grow-1" style="outline: none; min-width: 160px; font-size: 0.88rem;" placeholder="Type amenity and press Enter...">
                            </div>
                            <div class="form-text small text-muted">Type an amenity (e.g. "King Bed", "Projector", "Netflix") and press <kbd>Enter</kbd> or <kbd>,</kbd> to add. Click &times; to remove.</div>
                            <t:if test="${not empty errors['amenities']}">
                                <div class="invalid-feedback d-block">${e:esc(errors['amenities'])}</div>
                            </t:if>
                        </div>

                        <!-- Time Slot Pricing Section (S1..S4) -->
                        <div class="card shadow-sm border-0 mb-4 bg-light">
                            <div class="card-header bg-white py-2 fw-bold text-espresso d-flex justify-content-between align-items-center border-bottom">
                                <span><i class="bi bi-tag-fill me-2 text-primary"></i>Time Slot Pricing (S1 &ndash; S4)</span>
                                <span class="small text-muted fw-normal">Configured per 4 standard slots</span>
                            </div>
                            <div class="card-body p-3">
                                <div class="row g-3">
                                    <div class="col-md-3 col-sm-6">
                                        <div class="card h-100 border p-2 bg-white shadow-2xs">
                                            <div class="d-flex justify-content-between align-items-center mb-1">
                                                <span class="badge bg-espresso text-cream">S1 Morning</span>
                                                <small class="text-muted">06:00 &ndash; 12:00</small>
                                            </div>
                                            <label for="slotPrice_S1" class="form-label small fw-semibold mb-1">Price (VND)</label>
                                            <input type="number" class="form-control form-control-sm font-monospace ${not empty errors['slotPrice_S1'] ? 'is-invalid' : ''}"
                                                   id="slotPrice_S1" name="slotPrice_S1" min="10000" step="5000" required
                                                   value="${not empty slotPrices['S1'] ? slotPrices['S1'] : '150000'}">
                                            <t:if test="${not empty errors['slotPrice_S1']}">
                                                <div class="invalid-feedback d-block">${e:esc(errors['slotPrice_S1'])}</div>
                                            </t:if>
                                        </div>
                                    </div>
                                    <div class="col-md-3 col-sm-6">
                                        <div class="card h-100 border p-2 bg-white shadow-2xs">
                                            <div class="d-flex justify-content-between align-items-center mb-1">
                                                <span class="badge bg-espresso text-cream">S2 Afternoon</span>
                                                <small class="text-muted">12:00 &ndash; 18:00</small>
                                            </div>
                                            <label for="slotPrice_S2" class="form-label small fw-semibold mb-1">Price (VND)</label>
                                            <input type="number" class="form-control form-control-sm font-monospace ${not empty errors['slotPrice_S2'] ? 'is-invalid' : ''}"
                                                   id="slotPrice_S2" name="slotPrice_S2" min="10000" step="5000" required
                                                   value="${not empty slotPrices['S2'] ? slotPrices['S2'] : '150000'}">
                                            <t:if test="${not empty errors['slotPrice_S2']}">
                                                <div class="invalid-feedback d-block">${e:esc(errors['slotPrice_S2'])}</div>
                                            </t:if>
                                        </div>
                                    </div>
                                    <div class="col-md-3 col-sm-6">
                                        <div class="card h-100 border p-2 bg-white shadow-2xs">
                                            <div class="d-flex justify-content-between align-items-center mb-1">
                                                <span class="badge bg-espresso text-cream">S3 Evening</span>
                                                <small class="text-muted">18:00 &ndash; 23:00</small>
                                            </div>
                                            <label for="slotPrice_S3" class="form-label small fw-semibold mb-1">Price (VND)</label>
                                            <input type="number" class="form-control form-control-sm font-monospace ${not empty errors['slotPrice_S3'] ? 'is-invalid' : ''}"
                                                   id="slotPrice_S3" name="slotPrice_S3" min="10000" step="5000" required
                                                   value="${not empty slotPrices['S3'] ? slotPrices['S3'] : '180000'}">
                                            <t:if test="${not empty errors['slotPrice_S3']}">
                                                <div class="invalid-feedback d-block">${e:esc(errors['slotPrice_S3'])}</div>
                                            </t:if>
                                        </div>
                                    </div>
                                    <div class="col-md-3 col-sm-6">
                                        <div class="card h-100 border p-2 bg-white shadow-2xs">
                                            <div class="d-flex justify-content-between align-items-center mb-1">
                                                <span class="badge bg-espresso text-cream">S4 Overnight</span>
                                                <small class="text-muted">23:00 &ndash; 06:00</small>
                                            </div>
                                            <label for="slotPrice_S4" class="form-label small fw-semibold mb-1">Price (VND)</label>
                                            <input type="number" class="form-control form-control-sm font-monospace ${not empty errors['slotPrice_S4'] ? 'is-invalid' : ''}"
                                                   id="slotPrice_S4" name="slotPrice_S4" min="10000" step="5000" required
                                                   value="${not empty slotPrices['S4'] ? slotPrices['S4'] : '300000'}">
                                            <t:if test="${not empty errors['slotPrice_S4']}">
                                                <div class="invalid-feedback d-block">${e:esc(errors['slotPrice_S4'])}</div>
                                            </t:if>
                                        </div>
                                    </div>
                                </div>
                                <div class="form-text small text-muted mt-2">Saved slot pricing applies immediately to all future bookings for this room.</div>
                            </div>
                        </div>

                        <!-- Image Manager: Gallery Thumbnails + Upload + Set Main + Delete -->
                        <div class="card shadow-sm border-0 mb-4 bg-light">
                            <div class="card-header bg-white py-2 fw-bold text-espresso d-flex justify-content-between align-items-center border-bottom">
                                <span><i class="bi bi-images me-2 text-primary"></i>Room Image Manager</span>
                                <span class="badge bg-light text-muted border fw-normal">Directory: assets/images/rooms/${e:esc(roomSlug)}/</span>
                            </div>
                            <div class="card-body p-3">
                                <input type="hidden" name="imageUrl" id="mainImageUrlInput" value="${e:esc(room.imageUrl)}">
                                <input type="hidden" name="deleteImageName" id="deleteImageNameInput" value="">

                                <label class="form-label small fw-semibold mb-2">Current Gallery Images</label>
                                <div class="row g-2 mb-3" id="galleryThumbsGrid">
                                    <t:if test="${empty galleryImages}">
                                        <div class="col-12 text-muted small py-2">
                                            No images uploaded yet. Upload a photo below to initialize the room gallery.
                                        </div>
                                    </t:if>
                                    <t:forEach items="${galleryImages}" var="img">
                                        <div class="col-6 col-sm-4 col-md-3 col-lg-2 image-card" data-img-name="${e:esc(img.name)}" data-img-url="${e:esc(img.url)}">
                                            <div class="card h-100 position-relative border ${img.isMain ? 'border-primary shadow-sm' : ''}" style="overflow: hidden;">
                                                <div class="ratio ratio-4x3 bg-light">
                                                    <img src="${img.url}" class="card-img-top object-fit-cover" alt="${e:esc(img.name)}"
                                                         onerror="this.src='${pageContext.request.contextPath}/assets/images/rooms/butter-beige/main.jpeg'">
                                                </div>
                                                <t:if test="${img.isMain}">
                                                    <span class="badge bg-primary position-absolute top-0 start-0 m-1 main-badge" style="font-size: 0.65rem;">Main</span>
                                                </t:if>
                                                <div class="card-body p-1 d-flex flex-column gap-1 bg-white">
                                                    <div class="small text-truncate text-muted text-center" style="font-size: 0.72rem;" title="${e:esc(img.name)}">${e:esc(img.name)}</div>
                                                    <div class="d-flex gap-1 justify-content-center">
                                                        <button type="button" class="btn btn-xs ${img.isMain ? 'btn-primary' : 'btn-outline-primary'} py-0 px-1 w-100 btn-set-main" style="font-size: 0.65rem;">
                                                            ${img.isMain ? 'Main Photo' : 'Set Main'}
                                                        </button>
                                                        <button type="button" class="btn btn-xs btn-outline-danger py-0 px-1 btn-delete-image" style="font-size: 0.65rem;" title="Delete image">
                                                            <i class="bi bi-trash"></i>
                                                        </button>
                                                    </div>
                                                </div>
                                            </div>
                                        </div>
                                    </t:forEach>
                                </div>

                                <!-- Upload new image -->
                                <div class="card bg-white border p-3">
                                    <label for="imageFile" class="form-label small fw-semibold mb-1">Upload New Room Image</label>
                                    <div class="row g-2 align-items-center">
                                        <div class="col-sm-8">
                                            <input type="file" class="form-control form-control-sm ${not empty errors['imageFile'] ? 'is-invalid' : ''}"
                                                   id="imageFile" name="imageFile" accept=".jpg,.jpeg,.png,.webp">
                                            <div class="form-text small">Max 5MB. Supported formats: JPG, PNG, WEBP.</div>
                                            <t:if test="${not empty errors['imageFile']}">
                                                <div class="invalid-feedback d-block">${e:esc(errors['imageFile'])}</div>
                                            </t:if>
                                        </div>
                                        <div class="col-sm-4">
                                            <div class="form-check">
                                                <input class="form-check-input" type="checkbox" id="setAsMain" name="setAsMain" value="true">
                                                <label class="form-check-label small" for="setAsMain">Set as main photo</label>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>

                        <!-- Active Toggle Switch with Theme Styling -->
                        <div class="mb-4">
                            <div class="form-check form-switch theme-switch">
                                <input class="form-check-input" type="checkbox" role="switch" id="active" name="active"
                                       ${room.active ? 'checked' : ''}>
                                <label class="form-check-label fw-semibold ms-1" for="active">Active (available for bookings)</label>
                            </div>
                            <div class="form-text small text-muted">Inactive rooms are hidden from the guest search catalog and cannot be booked.</div>
                        </div>

                        <div class="d-flex gap-2 align-items-center pt-2 border-top">
                            <button type="submit" class="btn btn-primary px-4">
                                ${room.roomId > 0 ? 'Update Room' : 'Create Room'}
                            </button>
                            <a href="${pageContext.request.contextPath}/admin/rooms" class="btn btn-outline-secondary px-4">
                                Cancel
                            </a>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    </div>

    <!-- Scripts for Amenities Tag-Chip & Image Manager -->
    <script>
        document.addEventListener('DOMContentLoaded', function() {
            // 1. Amenities Tag-Chip Input
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

                function escapeText(text) {
                    var div = document.createElement('div');
                    div.textContent = text;
                    return div.innerHTML;
                }

                function renderChips() {
                    chipContainer.innerHTML = '';
                    amenitiesList.forEach(function(item, idx) {
                        var badge = document.createElement('span');
                        badge.className = 'badge bg-cream text-espresso border d-inline-flex align-items-center gap-1 py-1 px-2 shadow-2xs';
                        badge.style.borderColor = '#D9B991';
                        badge.style.fontSize = '0.82rem';
                        badge.innerHTML = '<span>' + escapeText(item) + '</span><button type="button" class="btn-close btn-close-sm ms-1" style="font-size: 0.55rem;" aria-label="Remove"></button>';
                        badge.querySelector('button').addEventListener('click', function(e) {
                            e.preventDefault();
                            e.stopPropagation();
                            amenitiesList.splice(idx, 1);
                            syncAndRender();
                        });
                        chipContainer.appendChild(badge);
                    });
                    hiddenInput.value = amenitiesList.join(', ');
                }

                function syncAndRender() {
                    hiddenInput.value = amenitiesList.join(', ');
                    renderChips();
                }

                function addAmenity(val) {
                    var clean = val.replace(/,/g, '').trim();
                    if (clean.length > 0 && amenitiesList.indexOf(clean) === -1) {
                        amenitiesList.push(clean);
                        syncAndRender();
                    }
                    textInput.value = '';
                }

                textInput.addEventListener('keydown', function(e) {
                    if (e.key === 'Enter' || e.key === ',') {
                        e.preventDefault();
                        addAmenity(textInput.value);
                    } else if (e.key === 'Backspace' && textInput.value === '' && amenitiesList.length > 0) {
                        amenitiesList.pop();
                        syncAndRender();
                    }
                });

                textInput.addEventListener('blur', function() {
                    if (textInput.value.trim().length > 0) {
                        addAmenity(textInput.value);
                    }
                });

                renderChips();
            })();

            // 2. Image Manager Actions
            (function() {
                var mainInput = document.getElementById('mainImageUrlInput');
                var deleteInput = document.getElementById('deleteImageNameInput');
                var slug = '${e:esc(roomSlug)}';

                // Set Main Image
                document.querySelectorAll('.btn-set-main').forEach(function(btn) {
                    btn.addEventListener('click', function(e) {
                        e.preventDefault();
                        var card = this.closest('.image-card');
                        if (!card) return;
                        var imgName = card.getAttribute('data-img-name');
                        if (mainInput) {
                            mainInput.value = '/assets/images/rooms/' + slug + '/' + imgName;
                        }
                        // Update UI
                        document.querySelectorAll('.image-card').forEach(function(c) {
                            c.querySelector('.card').classList.remove('border-primary', 'shadow-sm');
                            var b = c.querySelector('.main-badge');
                            if (b) b.remove();
                            var setBtn = c.querySelector('.btn-set-main');
                            if (setBtn) {
                                setBtn.className = 'btn btn-xs btn-outline-primary py-0 px-1 w-100 btn-set-main';
                                setBtn.textContent = 'Set Main';
                            }
                        });

                        card.querySelector('.card').classList.add('border-primary', 'shadow-sm');
                        var badge = document.createElement('span');
                        badge.className = 'badge bg-primary position-absolute top-0 start-0 m-1 main-badge';
                        badge.style.fontSize = '0.65rem';
                        badge.textContent = 'Main';
                        card.querySelector('.card').appendChild(badge);

                        this.className = 'btn btn-xs btn-primary py-0 px-1 w-100 btn-set-main';
                        this.textContent = 'Main Photo';
                    });
                });

                // Delete Image
                document.querySelectorAll('.btn-delete-image').forEach(function(btn) {
                    btn.addEventListener('click', function(e) {
                        e.preventDefault();
                        var card = this.closest('.image-card');
                        if (!card) return;
                        var imgName = card.getAttribute('data-img-name');
                        if (confirm('Delete photo "' + imgName + '"? Changes take effect when submitting.')) {
                            if (deleteInput) {
                                deleteInput.value = imgName;
                            }
                            card.style.opacity = '0.3';
                            card.style.pointerEvents = 'none';
                            var deletedNotice = document.createElement('div');
                            deletedNotice.className = 'badge bg-danger position-absolute top-50 start-50 translate-middle';
                            deletedNotice.textContent = 'Marked for delete';
                            card.querySelector('.card').appendChild(deletedNotice);
                        }
                    });
                });
            })();
        });
    </script>
</t:layout>
