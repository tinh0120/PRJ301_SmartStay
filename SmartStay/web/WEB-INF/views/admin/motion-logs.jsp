<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="e" uri="/WEB-INF/tld/smartstay.tld" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:layout title="Motion logs" active="motion-logs">
    <!-- Boutique Breadcrumb Navigation -->
    <nav aria-label="breadcrumb" class="mb-3">
        <ol class="breadcrumb boutique-breadcrumb m-0">
            <li class="breadcrumb-item">
                <a href="${pageContext.request.contextPath}/admin/dashboard" class="boutique-breadcrumb-link">
                    <i class="bi bi-house-door me-1"></i>Dashboard
                </a>
            </li>
            <li class="breadcrumb-item active" aria-current="page">Motion logs</li>
        </ol>
    </nav>

    <div class="d-flex justify-content-between align-items-baseline mb-3">
        <div>
            <h1 class="h2 mb-1">Motion logs</h1>
            <p class="text-muted small mb-0">PIR motion sensor trigger events and distance telemetry.</p>
        </div>
    </div>

    <t:if test="${not empty dbError}">
        <div class="alert alert-danger" role="alert">${e:esc(dbError)}</div>
    </t:if>

    <div class="card mb-4 shadow-sm">
        <div class="card-body">
            <form action="${pageContext.request.contextPath}/admin/motion-logs" method="get" class="row g-2 needs-validation" novalidate>
                <input type="hidden" name="sort" value="${e:esc(result.request.sort)}">
                <input type="hidden" name="dir" value="${e:esc(result.request.dir)}">

                <div class="col-md-3">
                    <label for="device" class="form-label small fw-semibold">Device</label>
                    <select class="form-select form-select-sm" id="device" name="device">
                        <option value="">All devices</option>
                        <t:forEach items="${devices}" var="dev">
                            <option value="${e:esc(dev)}" ${dev == result.request.getFilter('device') ? 'selected' : ''}>
                                ${e:esc(dev)}
                            </option>
                        </t:forEach>
                    </select>
                </div>

                <div class="col-md-2">
                    <label for="zone" class="form-label small fw-semibold">Zone</label>
                    <select class="form-select form-select-sm" id="zone" name="zone">
                        <option value="">All zones</option>
                        <t:forEach items="${zones}" var="z">
                            <option value="${e:esc(z)}" ${z == result.request.getFilter('zone') ? 'selected' : ''}>
                                ${e:esc(z)}
                            </option>
                        </t:forEach>
                    </select>
                </div>

                <div class="col-md-2">
                    <label for="from" class="form-label small fw-semibold">From date</label>
                    <input type="date" class="form-control form-control-sm ${errors['from'] != null ? 'is-invalid' : ''}"
                           id="from" name="from" data-datepicker value="${e:esc(result.request.getFilter('from'))}">
                    <t:if test="${not empty errors['from']}">
                        <div class="invalid-feedback d-block">${e:esc(errors['from'])}</div>
                    </t:if>
                </div>

                <div class="col-md-2">
                    <label for="to" class="form-label small fw-semibold">To date</label>
                    <input type="date" class="form-control form-control-sm ${errors['to'] != null ? 'is-invalid' : ''}"
                           id="to" name="to" data-datepicker value="${e:esc(result.request.getFilter('to'))}">
                    <t:if test="${not empty errors['to']}">
                        <div class="invalid-feedback d-block">${e:esc(errors['to'])}</div>
                    </t:if>
                </div>

                <div class="col-md-3">
                    <label for="keyword" class="form-label small fw-semibold">Event ID</label>
                    <input type="text" class="form-control form-control-sm" id="keyword" name="keyword"
                           placeholder="MOT-..." value="${e:esc(result.request.keyword)}">
                </div>

                <div class="col-12 d-flex justify-content-end gap-2 mt-3">
                    <button type="submit" class="btn btn-primary btn-sm px-3">Filter</button>
                    <a href="${pageContext.request.contextPath}/admin/motion-logs" class="btn btn-outline-secondary btn-sm px-3">Reset</a>
                </div>
            </form>
        </div>
    </div>

    <div class="card shadow-sm mb-4">
        <div class="card-body p-0">
            <div class="table-responsive" style="max-height: 720px; overflow-y: auto;">
                <table class="table table-hover table-striped align-middle mb-0">
                    <thead class="table-light sticky-top" style="position: sticky; top: 0; z-index: 10; box-shadow: 0 1px 2px rgba(0,0,0,0.06);">
                        <tr>
                            <th scope="col" class="ps-3">
                                <t:sortLink result="${result}" baseUrl="${pageContext.request.contextPath}/admin/motion-logs" column="time" label="Time"/>
                            </th>
                            <th scope="col">Device</th>
                            <th scope="col">Zone</th>
                            <th scope="col">
                                <t:sortLink result="${result}" baseUrl="${pageContext.request.contextPath}/admin/motion-logs" column="distance" label="Distance (cm)"/>
                            </th>
                            <th scope="col">Event ID</th>
                            <th scope="col" class="pe-3 text-center" style="width: 100px;">Snapshot</th>
                        </tr>
                    </thead>
                    <tbody>
                        <t:if test="${empty result.items}">
                            <tr><td colspan="6" class="text-center py-4 text-muted">No records found</td></tr>
                        </t:if>
                        <t:forEach items="${result.items}" var="row">
                            <tr>
                                <td class="ps-3 font-monospace text-nowrap">${e:datetime(row.detectedAt)}</td>
                                <td class="font-monospace fw-semibold">${e:esc(row.deviceId)}</td>
                                <td><span class="badge bg-light text-dark border font-monospace">${e:esc(row.zoneId)}</span></td>
                                <td class="font-monospace">
                                    <t:if test="${row.distanceCm != null}">
                                        ${row.distanceCm} cm
                                    </t:if>
                                    <t:if test="${row.distanceCm == null}">
                                        <span class="text-muted">&mdash;</span>
                                    </t:if>
                                </td>
                                <td class="font-monospace text-muted small">${e:esc(row.eventId)}</td>
                                <td class="pe-3 text-center">
                                    <t:if test="${not empty row.snapshotPath}">
                                        <img src="${pageContext.request.contextPath}/${e:esc(row.snapshotPath)}"
                                             alt="Motion snapshot ${e:esc(row.eventId)}"
                                             class="img-thumbnail rounded shadow-2xs motion-snapshot-thumb"
                                             style="width: 52px; height: 38px; object-fit: cover; cursor: pointer; border-color: #D9B991; transition: transform 0.15s ease;"
                                             data-snapshot-src="${pageContext.request.contextPath}/${e:esc(row.snapshotPath)}"
                                             data-event-id="${e:esc(row.eventId)}"
                                             data-device="${e:esc(row.deviceId)}"
                                             data-timestamp="${e:datetime(row.detectedAt)}"
                                             title="Click to enlarge snapshot"
                                             onerror="this.onerror=null; this.src='${pageContext.request.contextPath}/assets/images/branding/hero_img2.jpg';">
                                    </t:if>
                                    <t:if test="${empty row.snapshotPath}">
                                        <span class="text-muted small">&mdash;</span>
                                    </t:if>
                                </td>
                            </tr>
                        </t:forEach>
                    </tbody>
                </table>
            </div>
        </div>
    </div>

    <t:pagination result="${result}" baseUrl="${pageContext.request.contextPath}/admin/motion-logs"/>

    <!-- Lightbox Modal for Motion Snapshot -->
    <div id="snapshotLightbox" class="snapshot-lightbox" style="display: none;" role="dialog" aria-modal="true" aria-label="Surveillance Snapshot Preview">
        <div class="snapshot-lightbox-backdrop"></div>
        <div class="snapshot-lightbox-dialog">
            <div class="snapshot-lightbox-header d-flex justify-content-between align-items-center mb-2 px-1">
                <div>
                    <span class="badge bg-primary text-white me-2" id="lightboxEventBadge">MOT-00000</span>
                    <span class="text-white-50 small" id="lightboxMeta">Time & Device</span>
                </div>
                <button type="button" class="btn-close btn-close-white" id="closeSnapshotLightbox" aria-label="Close"></button>
            </div>
            <div class="snapshot-lightbox-content position-relative text-center">
                <img id="lightboxImg" src="" alt="Enlarged snapshot" class="rounded shadow-lg"
                     style="max-width: 90vw; max-height: 80vh; object-fit: contain;">
            </div>
        </div>
    </div>

    <style>
        .snapshot-lightbox {
            position: fixed;
            top: 0; left: 0; right: 0; bottom: 0;
            z-index: 1055;
            display: flex;
            align-items: center;
            justify-content: center;
        }
        .snapshot-lightbox-backdrop {
            position: absolute;
            top: 0; left: 0; right: 0; bottom: 0;
            background: rgba(20, 12, 8, 0.55);
            backdrop-filter: blur(6px);
            -webkit-backdrop-filter: blur(6px);
        }
        @supports not (backdrop-filter: blur(1px)) {
            .snapshot-lightbox-backdrop {
                background: rgba(20, 12, 8, 0.85);
            }
        }
        .snapshot-lightbox-dialog {
            position: relative;
            z-index: 2;
            max-width: 92vw;
            animation: lightboxFadeIn 0.18s ease-out;
        }
        @keyframes lightboxFadeIn {
            from { opacity: 0; transform: scale(0.96); }
            to { opacity: 1; transform: scale(1); }
        }
        .motion-snapshot-thumb:hover {
            transform: scale(1.08);
            border-color: #9A5B2E !important;
        }
    </style>

    <script>
        document.addEventListener('DOMContentLoaded', function() {
            var lightbox = document.getElementById('snapshotLightbox');
            var lightboxImg = document.getElementById('lightboxImg');
            var lightboxEventBadge = document.getElementById('lightboxEventBadge');
            var lightboxMeta = document.getElementById('lightboxMeta');
            var closeBtn = document.getElementById('closeSnapshotLightbox');
            var backdrop = document.querySelector('.snapshot-lightbox-backdrop');

            function openLightbox(src, eventId, device, timestamp) {
                if (!lightbox || !lightboxImg) return;
                lightboxImg.src = src;
                if (lightboxEventBadge) lightboxEventBadge.textContent = eventId;
                if (lightboxMeta) lightboxMeta.textContent = device + ' · ' + timestamp;
                lightbox.style.display = 'flex';
                document.body.style.overflow = 'hidden';
            }

            function closeLightbox() {
                if (!lightbox) return;
                lightbox.style.display = 'none';
                document.body.style.overflow = '';
                if (lightboxImg) lightboxImg.src = '';
            }

            document.querySelectorAll('.motion-snapshot-thumb').forEach(function(thumb) {
                thumb.addEventListener('click', function() {
                    var src = this.getAttribute('data-snapshot-src');
                    var eventId = this.getAttribute('data-event-id');
                    var device = this.getAttribute('data-device');
                    var timestamp = this.getAttribute('data-timestamp');
                    openLightbox(src, eventId, device, timestamp);
                });
            });

            if (closeBtn) {
                closeBtn.addEventListener('click', closeLightbox);
            }
            if (backdrop) {
                backdrop.addEventListener('click', closeLightbox);
            }
            document.addEventListener('keydown', function(e) {
                if (e.key === 'Escape' && lightbox && lightbox.style.display === 'flex') {
                    closeLightbox();
                }
            });
        });
    </script>
</t:layout>
