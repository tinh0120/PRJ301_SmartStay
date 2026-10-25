<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="e" uri="/WEB-INF/tld/smartstay.tld" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:layout title="Reviews" active="reviews">
    <!-- Boutique Breadcrumb Navigation -->
    <nav aria-label="breadcrumb" class="mb-3">
        <ol class="breadcrumb boutique-breadcrumb m-0">
            <li class="breadcrumb-item">
                <a href="${pageContext.request.contextPath}/admin/dashboard" class="boutique-breadcrumb-link">
                    <i class="bi bi-house-door me-1"></i>Dashboard
                </a>
            </li>
            <li class="breadcrumb-item active" aria-current="page">Reviews</li>
        </ol>
    </nav>

    <div class="d-flex justify-content-between align-items-center mb-3">
        <div>
            <h1 class="h3 fw-bold mb-1">Customer Reviews Moderation</h1>
            <p class="text-muted small mb-0">Inspect customer feedback, filter by rating and status, and moderate public visibility.</p>
        </div>
    </div>

    <!-- Average Rating Per Room (Summary Cards) -->
    <t:if test="${not empty roomRatingSummaries}">
        <div class="row g-2 mb-4">
            <t:forEach items="${roomRatingSummaries}" var="rs">
                <div class="col-6 col-md-4 col-lg-2">
                    <div class="card h-100 shadow-2xs border bg-white p-2">
                        <div class="d-flex justify-content-between align-items-center mb-1">
                            <span class="badge bg-light text-dark border">Room ${e:esc(rs.roomNumber)}</span>
                            <span class="text-warning small fw-bold"><i class="bi bi-star-fill text-warning me-1"></i>${rs.avgRating}</span>
                        </div>
                        <div class="small fw-semibold text-truncate text-espresso" title="${e:esc(rs.displayName)}">${e:esc(rs.displayName)}</div>
                        <div class="text-muted small" style="font-size: 0.72rem;">${rs.totalReviews} review${rs.totalReviews == 1 ? '' : 's'}</div>
                    </div>
                </div>
            </t:forEach>
        </div>
    </t:if>

    <!-- Mobile Filter Toggle Button (< 768px) -->
    <div class="d-md-none mb-3">
        <button class="btn btn-outline-secondary btn-sm w-100 d-flex justify-content-between align-items-center" type="button" data-bs-toggle="collapse" data-bs-target="#reviewFilterCollapse" aria-expanded="false" aria-controls="reviewFilterCollapse">
            <span><i class="bi bi-funnel me-1"></i> Filters &amp; Search</span>
            <i class="bi bi-chevron-down"></i>
        </button>
    </div>

    <!-- Filter Card -->
    <div class="card shadow-sm border-0 mb-4 bg-light collapse d-md-block" id="reviewFilterCollapse">
        <div class="card-body p-3">
            <form action="${pageContext.request.contextPath}/admin/reviews" method="get" class="row g-2 align-items-end">
                <input type="hidden" name="sort" value="${e:esc(result.request.sort)}">
                <input type="hidden" name="dir" value="${e:esc(result.request.dir)}">

                <div class="col-md-2">
                    <label for="rating" class="form-label small fw-semibold">Rating</label>
                    <select class="form-select form-select-sm" id="rating" name="rating">
                        <option value="">All Ratings</option>
                        <option value="5" ${result.request.getFilter('rating') == '5' ? 'selected' : ''}>5 Stars (&#9733;&#9733;&#9733;&#9733;&#9733;)</option>
                        <option value="4" ${result.request.getFilter('rating') == '4' ? 'selected' : ''}>4 Stars (&#9733;&#9733;&#9733;&#9733;)</option>
                        <option value="3" ${result.request.getFilter('rating') == '3' ? 'selected' : ''}>3 Stars (&#9733;&#9733;&#9733;)</option>
                        <option value="2" ${result.request.getFilter('rating') == '2' ? 'selected' : ''}>2 Stars (&#9733;&#9733;)</option>
                        <option value="1" ${result.request.getFilter('rating') == '1' ? 'selected' : ''}>1 Star (&#9733;)</option>
                    </select>
                </div>

                <div class="col-md-2">
                    <label for="status" class="form-label small fw-semibold">Status</label>
                    <select class="form-select form-select-sm" id="status" name="status">
                        <option value="">All Statuses</option>
                        <option value="VISIBLE" ${result.request.getFilter('status') == 'VISIBLE' ? 'selected' : ''}>VISIBLE</option>
                        <option value="HIDDEN" ${result.request.getFilter('status') == 'HIDDEN' ? 'selected' : ''}>HIDDEN</option>
                    </select>
                </div>

                <div class="col-md-2">
                    <label for="roomId" class="form-label small fw-semibold">Room</label>
                    <select class="form-select form-select-sm" id="roomId" name="roomId">
                        <option value="">All Rooms</option>
                        <t:forEach items="${rooms}" var="rm">
                            <option value="${rm.roomId}" ${result.request.getFilter('roomId') == rm.roomId ? 'selected' : ''}>
                                Room ${e:esc(rm.roomNumber)}
                            </option>
                        </t:forEach>
                    </select>
                </div>

                <div class="col-md-4">
                    <label for="keyword" class="form-label small fw-semibold">Keyword</label>
                    <input type="text" class="form-control form-control-sm" id="keyword" name="keyword"
                           placeholder="Search in comment or guest name..."
                           value="${e:esc(result.request.keyword)}">
                </div>

                <div class="col-md-2 d-flex gap-2">
                    <button type="submit" class="btn btn-primary btn-sm flex-fill">Filter</button>
                    <a href="${pageContext.request.contextPath}/admin/reviews" class="btn btn-outline-secondary btn-sm flex-fill">Reset</a>
                </div>
            </form>
        </div>
    </div>

    <!-- Summary & Results Table -->
    <div class="d-flex justify-content-between align-items-center mb-2">
        <span class="text-muted small">${e:esc(result.summary)}</span>
    </div>

    <div class="card shadow-sm border-0 mb-3">
        <div class="table-responsive" style="max-height: 720px; overflow-y: auto;">
            <table class="table table-sm table-hover table-striped align-middle mb-0">
                <thead class="table-light sticky-top" style="position: sticky; top: 0; z-index: 10; box-shadow: 0 1px 2px rgba(0,0,0,0.06);">
                    <tr>
                        <th scope="col" style="width: 120px;">
                            <t:sortLink column="room" label="Room" result="${result}" baseUrl="${pageContext.request.contextPath}/admin/reviews"/>
                        </th>
                        <th scope="col" style="width: 110px;">Booking</th>
                        <th scope="col" style="width: 170px;">Guest</th>
                        <th scope="col" style="width: 130px;">
                            <t:sortLink column="rating" label="Rating" result="${result}" baseUrl="${pageContext.request.contextPath}/admin/reviews"/>
                        </th>
                        <th scope="col">Comment</th>
                        <th scope="col" style="width: 110px;">
                            <t:sortLink column="status" label="Status" result="${result}" baseUrl="${pageContext.request.contextPath}/admin/reviews"/>
                        </th>
                        <th scope="col" style="width: 150px;">
                            <t:sortLink column="created" label="Date" result="${result}" baseUrl="${pageContext.request.contextPath}/admin/reviews"/>
                        </th>
                        <th scope="col" style="width: 100px;" class="text-center">Action</th>
                    </tr>
                </thead>
                <tbody>
                    <t:forEach items="${result.items}" var="item">
                        <tr>
                            <td>
                                <div class="fw-semibold small">${e:esc(item.roomDisplayName != null ? item.roomDisplayName : item.roomTitle)}</div>
                                <span class="badge bg-light text-dark border">Room ${e:esc(item.roomNumber)}</span>
                            </td>
                            <td>
                                <t:if test="${item.bookingId > 0}">
                                    <a href="${pageContext.request.contextPath}/admin/bookings/detail?id=${item.bookingId}" class="badge bg-light text-espresso border text-decoration-none font-monospace" title="View booking details">
                                        <i class="bi bi-box-arrow-up-right me-1" style="font-size: 0.7rem;"></i><t:if test="${not empty item.bookingCode}">${e:esc(item.bookingCode)}</t:if><t:if test="${empty item.bookingCode}">BK#${item.bookingId}</t:if>
                                    </a>
                                </t:if>
                                <t:if test="${empty item.bookingId || item.bookingId <= 0}">
                                    <span class="text-muted small">—</span>
                                </t:if>
                            </td>
                            <td>
                                <div class="small fw-semibold text-truncate" style="max-width: 160px;">${e:esc(item.guestName)}</div>
                                <div class="text-muted text-truncate" style="font-size: 0.75rem; max-width: 160px;">${e:esc(item.guestEmail)}</div>
                            </td>
                            <td>
                                <span class="text-warning fs-6" title="${item.rating} of 5 stars">${item.starsHtml}</span>
                                <span class="small text-muted fw-semibold ms-1">(${item.rating}/5)</span>
                            </td>
                            <td>
                                <div class="small" style="max-width: 360px;">
                                    ${e:esc(item.comment)}
                                </div>
                            </td>
                            <td>
                                <span class="badge-status status-${item.status}">${item.status}</span>
                                <t:if test="${not empty item.moderatorName}">
                                    <div class="text-muted" style="font-size: 0.7rem;">by ${e:esc(item.moderatorName)}</div>
                                </t:if>
                            </td>
                            <td class="small text-nowrap">${e:datetime(item.createdAt)}</td>
                            <td class="text-center text-nowrap">
                                <t:if test="${item.status == 'VISIBLE'}">
                                    <button type="button" class="btn btn-outline-danger btn-sm px-2 py-1"
                                            data-bs-toggle="modal" data-bs-target="#hideReviewModal"
                                            data-review-id="${item.reviewId}"
                                            data-review-author="${e:esc(item.guestName)}"
                                            title="Hide this review with optional moderation reason">
                                        <i class="bi bi-eye-slash me-1"></i>Hide
                                    </button>
                                </t:if>
                                <t:if test="${item.status == 'HIDDEN'}">
                                    <form method="post" action="${pageContext.request.contextPath}/admin/reviews/action" class="d-inline">
                                        <input type="hidden" name="_csrf" value="${e:esc(not empty _csrf ? _csrf : sessionScope._csrf)}">
                                        <input type="hidden" name="action" value="show">
                                        <input type="hidden" name="reviewId" value="${item.reviewId}">
                                        <input type="hidden" name="redirectQuery" value="${e:esc(result.request.toQueryString())}">
                                        <button type="submit" class="btn btn-outline-success btn-sm px-2 py-1"
                                                title="Make this review visible publicly">
                                            <i class="bi bi-eye me-1"></i>Show
                                        </button>
                                    </form>
                                </t:if>
                            </td>
                        </tr>
                    </t:forEach>
                    <t:if test="${empty result.items}">
                        <tr><td colspan="8" class="text-center py-4 text-muted">No reviews found.</td></tr>
                    </t:if>
                </tbody>
            </table>
        </div>
    </div>

    <!-- Hide Review Modal with Optional Reason -->
    <div class="modal fade" id="hideReviewModal" tabindex="-1" aria-labelledby="hideReviewModalLabel" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered">
            <div class="modal-content border-0 shadow">
                <form method="post" action="${pageContext.request.contextPath}/admin/reviews/action">
                    <input type="hidden" name="_csrf" value="${e:esc(not empty _csrf ? _csrf : sessionScope._csrf)}">
                    <input type="hidden" name="action" value="hide">
                    <input type="hidden" name="reviewId" id="hideModalReviewId" value="">
                    <input type="hidden" name="redirectQuery" value="${e:esc(result.request.toQueryString())}">

                    <div class="modal-header border-bottom">
                        <h5 class="modal-title fw-semibold text-espresso" id="hideReviewModalLabel">
                            <i class="bi bi-eye-slash text-danger me-2"></i>Hide Customer Review
                        </h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                    </div>
                    <div class="modal-body p-4">
                        <p class="small text-muted mb-3">
                            You are about to hide a review by <strong id="hideModalAuthor" class="text-espresso">Guest</strong>. Hidden reviews are immediately removed from the public website.
                        </p>
                        <div class="mb-2">
                            <label for="hideReasonInput" class="form-label small fw-semibold text-espresso">
                                Moderation Reason <span class="text-muted fw-normal">(optional)</span>
                            </label>
                            <input type="text" class="form-control form-control-sm" id="hideReasonInput" name="reason"
                                   placeholder="e.g. Inappropriate language, spam, promotional link, off-topic...">
                            <div class="form-text" style="font-size: 0.75rem;">
                                This reason will be logged in the system audit trail for reference.
                            </div>
                        </div>
                    </div>
                    <div class="modal-footer bg-light border-top py-2">
                        <button type="button" class="btn btn-outline-secondary btn-sm" data-bs-dismiss="modal">Cancel</button>
                        <button type="submit" class="btn btn-danger btn-sm px-3">
                            <i class="bi bi-eye-slash me-1"></i>Confirm Hide
                        </button>
                    </div>
                </form>
            </div>
        </div>
    </div>

    <script>
        document.addEventListener('DOMContentLoaded', function () {
            var hideModal = document.getElementById('hideReviewModal');
            if (hideModal) {
                hideModal.addEventListener('show.bs.modal', function (event) {
                    var button = event.relatedTarget;
                    if (button) {
                        var reviewId = button.getAttribute('data-review-id');
                        var author = button.getAttribute('data-review-author');
                        var idInput = hideModal.querySelector('#hideModalReviewId');
                        var authorEl = hideModal.querySelector('#hideModalAuthor');
                        var reasonInput = hideModal.querySelector('#hideReasonInput');
                        if (idInput) idInput.value = reviewId || '';
                        if (authorEl) authorEl.textContent = author || ('Review #' + reviewId);
                        if (reasonInput) reasonInput.value = '';
                    }
                });
            }
        });
    </script>

    <!-- Pagination -->
    <t:pagination result="${result}" baseUrl="${pageContext.request.contextPath}/admin/reviews"/>
</t:layout>
