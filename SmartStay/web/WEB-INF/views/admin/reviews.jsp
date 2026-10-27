<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="e" uri="/WEB-INF/tld/smartstay.tld" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:layout title="Reviews" active="reviews">
    <%-- R11: one page scroll. Per-room cards filter the feed; the feed is a list of cards with a "⋯" menu. --%>
    <div class="ss-page-header">
        <div>
            <h1 class="ss-page-header__title">Reviews</h1>
            <p class="ss-page-header__subtitle">What guests say after their stay. Hidden reviews stay here but are not shown on the site.</p>
        </div>
    </div>

    <!-- Per-room averages (click to filter) -->
    <t:if test="${not empty roomRatingSummaries}">
        <div class="ss-rv-rooms" role="list" aria-label="Average rating per room">
            <t:forEach items="${roomRatingSummaries}" var="rs">
                <a role="listitem" class="ss-rv-room ${selectedRoomId == rs.roomId ? 'ss-rv-room--active' : ''}"
                   ${selectedRoomId == rs.roomId ? 'aria-current="true"' : ''}
                   href="${pageContext.request.contextPath}/admin/reviews?${roomBaseQs}${selectedRoomId == rs.roomId ? '' : '&roomId='.concat(rs.roomId)}"
                   title="${selectedRoomId == rs.roomId ? 'Show all rooms' : 'Show reviews for this room only'}">
                    <span class="ss-rv-room__head">
                        <span class="ss-rv-room__name">Room ${e:esc(rs.roomNumber)}</span>
                        <span class="ss-rv-room__count">${rs.totalReviews} review${rs.totalReviews == 1 ? '' : 's'}</span>
                    </span>
                    <span class="ss-rv-room__sub">${e:esc(rs.displayName)}</span>
                    <span class="ss-rv-room__avg">
                        <span class="ss-rv-room__num">${rs.totalReviews > 0 ? rs.avgRating : '-'}</span>
                        <span class="ss-rv-room__bar" aria-hidden="true"><span style="width: ${rs.avgPercent}%;"></span></span>
                    </span>
                    <span class="visually-hidden">Average ${rs.avgRating} out of 5</span>
                </a>
            </t:forEach>
        </div>
    </t:if>

    <nav class="ss-tabs ss-bk-tabs" aria-label="Review visibility">
        <a class="ss-tabs__link ${tab == 'all' ? 'ss-tabs__link--active' : ''}" ${tab == 'all' ? 'aria-current="page"' : ''}
           href="${pageContext.request.contextPath}/admin/reviews?${tabBaseQs}">
            All <span class="ss-bk-tabs__count">${empty tabCounts ? 0 : tabCounts['ALL']}</span>
        </a>
        <a class="ss-tabs__link ${tab == 'visible' ? 'ss-tabs__link--active' : ''}" ${tab == 'visible' ? 'aria-current="page"' : ''}
           href="${pageContext.request.contextPath}/admin/reviews?${tabBaseQs}&amp;tab=visible">
            Visible <span class="ss-bk-tabs__count">${empty tabCounts ? 0 : tabCounts['VISIBLE']}</span>
        </a>
        <a class="ss-tabs__link ${tab == 'hidden' ? 'ss-tabs__link--active' : ''}" ${tab == 'hidden' ? 'aria-current="page"' : ''}
           href="${pageContext.request.contextPath}/admin/reviews?${tabBaseQs}&amp;tab=hidden">
            Hidden <span class="ss-bk-tabs__count">${empty tabCounts ? 0 : tabCounts['HIDDEN']}</span>
        </a>
        <a class="ss-tabs__link ${tab == 'attention' ? 'ss-tabs__link--active' : ''}" ${tab == 'attention' ? 'aria-current="page"' : ''}
           href="${pageContext.request.contextPath}/admin/reviews?${tabBaseQs}&amp;tab=attention">
            Needs attention <span class="ss-bk-tabs__count">${empty tabCounts ? 0 : tabCounts['ATTENTION']}</span>
        </a>
    </nav>

    <!-- Toolbar -->
    <form action="${pageContext.request.contextPath}/admin/reviews" method="get" class="ss-toolbar">
        <t:if test="${tab != 'all'}"><input type="hidden" name="tab" value="${e:esc(tab)}"></t:if>
        <div class="ss-toolbar__main">
            <div class="ss-toolbar__search">
                <i class="bi bi-search ss-toolbar__search-icon" aria-hidden="true"></i>
                <label for="keyword" class="visually-hidden">Search reviews</label>
                <input type="search" class="ss-toolbar__search-input" id="keyword" name="keyword" maxlength="100"
                       placeholder="Comment, guest name or email" value="${e:esc(result.request.keyword)}">
            </div>
            <div class="ss-rv-toolbar__room">
                <label for="roomId" class="visually-hidden">Room</label>
                <select class="form-select form-select-sm" id="roomId" name="roomId" onchange="this.form.submit()">
                    <option value="">All rooms</option>
                    <t:forEach items="${rooms}" var="rm">
                        <option value="${rm.roomId}" ${selectedRoomId == rm.roomId ? 'selected' : ''}>Room ${e:esc(rm.roomNumber)}</option>
                    </t:forEach>
                </select>
            </div>
            <button type="submit" class="btn btn-primary btn-sm px-3">Search</button>
        </div>
    </form>

    <!-- Feed -->
    <div class="ss-rv-feed">
        <t:if test="${empty result.items}">
            <div class="ss-empty">
                <i class="bi bi-chat-square-heart ss-empty__icon" aria-hidden="true"></i>
                <p class="ss-empty__title">${tab == 'attention' ? 'No low ratings' : (tab == 'hidden' ? 'No hidden reviews' : 'No reviews found')}</p>
                <p class="ss-empty__desc">${empty result.request.keyword && empty selectedRoomId ? 'Reviews appear here after guests check out.' : 'Nothing matches this search or room.'}</p>
            </div>
        </t:if>

        <t:forEach items="${result.items}" var="rv">
            <article class="ss-rv ${rv.status == 'HIDDEN' ? 'ss-rv--hidden' : ''}" aria-label="Review by ${e:esc(rv.guestName)}">
                <div class="ss-rv__avatar" aria-hidden="true">${empty rv.guestName ? '?' : e:esc(rv.guestName.substring(0, 1).toUpperCase())}</div>
                <div class="ss-rv__body">
                    <header class="ss-rv__head">
                        <span class="ss-rv__name" title="${e:esc(rv.guestEmail)}">${e:esc(rv.guestName)}</span>
                        <span class="ss-rv__stars" role="img" aria-label="${rv.rating} out of 5 stars">
                            <i class="bi ${rv.rating >= 1 ? 'bi-star-fill' : 'bi-star'}" aria-hidden="true"></i><i class="bi ${rv.rating >= 2 ? 'bi-star-fill' : 'bi-star'}" aria-hidden="true"></i><i class="bi ${rv.rating >= 3 ? 'bi-star-fill' : 'bi-star'}" aria-hidden="true"></i><i class="bi ${rv.rating >= 4 ? 'bi-star-fill' : 'bi-star'}" aria-hidden="true"></i><i class="bi ${rv.rating >= 5 ? 'bi-star-fill' : 'bi-star'}" aria-hidden="true"></i>
                        </span>
                        <span class="ss-chip ss-rv__room">Room ${e:esc(rv.roomNumber)}</span>
                        <t:if test="${rv.status == 'HIDDEN'}">
                            <span class="ss-badge ss-badge--neutral" title="${empty rv.moderatorName ? '' : 'Hidden by '.concat(e:esc(rv.moderatorName))}">Hidden</span>
                        </t:if>
                        <div class="dropdown ss-row-menu ss-rv__menu">
                            <button class="btn-trigger" type="button" data-bs-toggle="dropdown" aria-expanded="false"
                                    aria-label="More actions for the review by ${e:esc(rv.guestName)}">
                                <i class="bi bi-three-dots" aria-hidden="true"></i>
                            </button>
                            <ul class="dropdown-menu dropdown-menu-end">
                                <t:if test="${rv.status == 'VISIBLE'}">
                                    <li><button type="button" class="dropdown-item" data-rv-hide="${rv.reviewId}">Hide review&hellip;</button></li>
                                </t:if>
                                <t:if test="${rv.status == 'HIDDEN'}">
                                    <li>
                                        <form method="post" action="${pageContext.request.contextPath}/admin/reviews/action">
                                            <input type="hidden" name="_csrf" value="${e:esc(not empty _csrf ? _csrf : sessionScope._csrf)}">
                                            <input type="hidden" name="action" value="show">
                                            <input type="hidden" name="reviewId" value="${rv.reviewId}">
                                            <input type="hidden" name="redirectQuery" value="${e:esc(returnQs)}">
                                            <button type="submit" class="dropdown-item">Show on the site</button>
                                        </form>
                                    </li>
                                </t:if>
                                <t:if test="${rv.bookingId > 0}">
                                    <li><a class="dropdown-item" href="${pageContext.request.contextPath}/admin/bookings/detail?id=${rv.bookingId}">Open booking</a></li>
                                </t:if>
                            </ul>
                        </div>
                    </header>
                    <t:if test="${not empty rv.comment}"><p class="ss-rv__comment">${e:esc(rv.comment)}</p></t:if>
                    <t:if test="${empty rv.comment}"><p class="ss-rv__comment ss-rv__comment--none">No comment, rating only.</p></t:if>
                    <footer class="ss-rv__meta">
                        <time>${e:date(rv.createdAt)}</time>
                        <t:if test="${rv.bookingId > 0}">
                            <span aria-hidden="true">&middot;</span>
                            <a href="${pageContext.request.contextPath}/admin/bookings/detail?id=${rv.bookingId}">${empty rv.bookingCode ? 'Booking' : e:esc(rv.bookingCode)}</a>
                        </t:if>
                    </footer>

                    <t:if test="${rv.status == 'VISIBLE'}">
                        <form method="post" action="${pageContext.request.contextPath}/admin/reviews/action" class="ss-rv__hide"
                              id="rvHide-${rv.reviewId}" hidden>
                            <input type="hidden" name="_csrf" value="${e:esc(not empty _csrf ? _csrf : sessionScope._csrf)}">
                            <input type="hidden" name="action" value="hide">
                            <input type="hidden" name="reviewId" value="${rv.reviewId}">
                            <input type="hidden" name="redirectQuery" value="${e:esc(returnQs)}">
                            <label for="rvReason-${rv.reviewId}" class="ss-rv__hide-label">Why hide it? (optional)</label>
                            <input type="text" class="form-control form-control-sm" id="rvReason-${rv.reviewId}" name="reason" maxlength="200"
                                   placeholder="e.g. personal details, offensive words">
                            <div class="ss-rv__hide-actions">
                                <button type="button" class="btn btn-outline-soft btn-sm" data-rv-cancel>Cancel</button>
                                <button type="submit" class="btn btn-primary btn-sm">Hide review</button>
                            </div>
                        </form>
                    </t:if>
                </div>
            </article>
        </t:forEach>
    </div>

    <t:pagination result="${result}" baseUrl="${pageContext.request.contextPath}/admin/reviews"/>

    <script>
        // R11: "Hide review…" opens the small reason form under the card; Esc / Cancel closes it.
        (function () {
            document.addEventListener('click', function (e) {
                var open = e.target.closest('[data-rv-hide]');
                if (open) {
                    var form = document.getElementById('rvHide-' + open.getAttribute('data-rv-hide'));
                    if (form) {
                        form.hidden = false;
                        var input = form.querySelector('input[name="reason"]');
                        if (input) { input.focus(); }
                    }
                    return;
                }
                var cancel = e.target.closest('[data-rv-cancel]');
                if (cancel) {
                    var f = cancel.closest('form');
                    f.hidden = true;
                    var trigger = f.closest('.ss-rv').querySelector('.ss-rv__menu .btn-trigger');
                    if (trigger) { trigger.focus(); }
                }
            });
            document.addEventListener('keydown', function (e) {
                if (e.key === 'Escape') {
                    var f = e.target.closest && e.target.closest('.ss-rv__hide');
                    if (f) {
                        f.hidden = true;
                    }
                }
            });
        })();
    </script>
</t:layout>
