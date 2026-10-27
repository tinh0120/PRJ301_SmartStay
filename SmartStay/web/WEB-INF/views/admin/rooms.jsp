<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="e" uri="/WEB-INF/tld/smartstay.tld" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:layout title="Rooms" active="admin-rooms">
    <%-- R8: card grid, one status badge per card, S1–S4 dots follow the room status, Edit + ⋯ menu. --%>
    <div class="ss-page-header">
        <div>
            <h1 class="ss-page-header__title">Rooms</h1>
            <p class="ss-page-header__subtitle">Details, photos and visibility. Prices and slots are on each room's Slots &amp; Pricing tab.</p>
        </div>
        <a href="${pageContext.request.contextPath}/admin/rooms/edit" class="btn btn-primary btn-sm rounded-pill px-3">
            <i class="bi bi-plus-lg me-1" aria-hidden="true"></i>New room
        </a>
    </div>

    <t:if test="${not empty dbError}">
        <div class="alert alert-danger" role="alert">${e:esc(dbError)}</div>
    </t:if>

    <!-- Toolbar: search + status chips -->
    <form action="${pageContext.request.contextPath}/admin/rooms" method="get" class="ss-toolbar">
        <input type="hidden" name="status" value="${e:esc(activeChip)}">
        <div class="ss-toolbar__main">
            <div class="ss-toolbar__search">
                <i class="bi bi-search ss-toolbar__search-icon" aria-hidden="true"></i>
                <label for="keyword" class="visually-hidden">Search rooms</label>
                <input type="search" class="ss-toolbar__search-input" id="keyword" name="keyword"
                       placeholder="Room code, name or description" maxlength="100" value="${e:esc(pr.keyword)}">
            </div>
            <div class="ss-toolbar__chips" role="group" aria-label="Room status">
                <a class="ss-chip ${empty activeChip ? 'ss-chip--active' : ''}" ${empty activeChip ? 'aria-current="true"' : ''}
                   href="${pageContext.request.contextPath}/admin/rooms?${chipBaseQs}">All</a>
                <a class="ss-chip ${activeChip == 'AVAILABLE' ? 'ss-chip--active' : ''}" ${activeChip == 'AVAILABLE' ? 'aria-current="true"' : ''}
                   href="${pageContext.request.contextPath}/admin/rooms?${chipBaseQs}&amp;status=AVAILABLE">Available</a>
                <a class="ss-chip ${activeChip == 'NEEDS_CLEANING' ? 'ss-chip--active' : ''}" ${activeChip == 'NEEDS_CLEANING' ? 'aria-current="true"' : ''}
                   href="${pageContext.request.contextPath}/admin/rooms?${chipBaseQs}&amp;status=NEEDS_CLEANING">Needs cleaning</a>
                <a class="ss-chip ${activeChip == 'MAINTENANCE' ? 'ss-chip--active' : ''}" ${activeChip == 'MAINTENANCE' ? 'aria-current="true"' : ''}
                   href="${pageContext.request.contextPath}/admin/rooms?${chipBaseQs}&amp;status=MAINTENANCE">Maintenance</a>
                <a class="ss-chip ${activeChip == 'INACTIVE' ? 'ss-chip--active' : ''}" ${activeChip == 'INACTIVE' ? 'aria-current="true"' : ''}
                   href="${pageContext.request.contextPath}/admin/rooms?${chipBaseQs}&amp;status=INACTIVE">Inactive</a>
            </div>
        </div>
    </form>

    <t:if test="${empty pageResult.items && empty dbError}">
        <div class="ss-empty ss-rm-empty">
            <i class="bi bi-door-closed ss-empty__icon" aria-hidden="true"></i>
            <p class="ss-empty__title">No rooms match</p>
            <p class="ss-empty__desc">Choose another status or clear the search.</p>
            <a href="${pageContext.request.contextPath}/admin/rooms" class="btn btn-outline-soft btn-sm rounded-pill px-3">Show all rooms</a>
        </div>
    </t:if>

    <div class="ss-rm-grid">
        <t:forEach items="${pageResult.items}" var="r">
            <article class="ss-rm-card ${r.active ? '' : 'ss-rm-card--inactive'}">
                <div class="ss-rm-card__media">
                    <img src="${pageContext.request.contextPath}/${r.resolvedImageUrl}" alt="${e:esc(r.displayTitle)}"
                         class="ss-rm-card__img" width="768" height="576" loading="lazy"
                         onerror="this.onerror=null; this.src='${pageContext.request.contextPath}/assets/images/branding/hero_img2.jpg';">
                </div>
                <div class="ss-rm-card__body">
                    <div class="ss-rm-card__head">
                        <div>
                            <h2 class="ss-rm-card__title">${e:esc(r.displayTitle)}</h2>
                            <p class="ss-rm-card__code">${e:esc(r.roomNumber)}</p>
                        </div>
                        <t:if test="${!r.active}"><span class="ss-badge ss-badge--neutral">Inactive</span></t:if>
                        <t:if test="${r.active && r.status == 'AVAILABLE'}"><span class="ss-badge ss-badge--success">Available</span></t:if>
                        <t:if test="${r.active && r.status == 'OCCUPIED'}"><span class="ss-badge ss-badge--neutral">Occupied</span></t:if>
                        <t:if test="${r.active && r.status == 'NEEDS_CLEANING'}"><span class="ss-badge ss-badge--warning">Needs cleaning</span></t:if>
                        <t:if test="${r.active && r.status == 'MAINTENANCE'}"><span class="ss-badge ss-badge--danger">Maintenance</span></t:if>
                    </div>
                    <p class="ss-rm-card__meta">Floor ${r.floorNumber} &middot; ${r.capacity} ${r.capacity == 1 ? 'guest' : 'guests'}</p>
                    <p class="ss-rm-card__price">
                        <t:if test="${not empty r.minPrice and r.minPrice > 0}">From ${e:money(r.minPrice)}</t:if>
                        <t:if test="${empty r.minPrice or r.minPrice <= 0}">No price set</t:if>
                    </p>
                    <t:if test="${not empty r.slots}">
                        <ul class="ss-rm-card__slots" aria-label="Today's slots">
                            <t:forEach items="${r.slots}" var="s">
                                <%-- V4: slot code + a shape per state, so the colour is not the only cue --%>
                                <li class="ss-rm-card__slot" title="${e:esc(s.slotName)}: ${e:esc(s.todayLabel)}">
                                    <t:if test="${s.todayStatus == 'AVAILABLE'}"><span class="ss-status-dot ss-status-dot--ok" aria-hidden="true"></span></t:if>
                                    <t:if test="${s.todayStatus == 'BOOKED'}"><i class="bi bi-person-fill ss-rm-card__slot-icon" aria-hidden="true"></i></t:if>
                                    <t:if test="${s.todayStatus == 'NEEDS_CLEANING'}"><i class="bi bi-stars ss-rm-card__slot-icon ss-rm-card__slot-icon--warn" aria-hidden="true"></i></t:if>
                                    <t:if test="${s.todayStatus == 'MAINTENANCE'}"><i class="bi bi-tools ss-rm-card__slot-icon ss-rm-card__slot-icon--danger" aria-hidden="true"></i></t:if>
                                    <t:if test="${s.todayStatus == 'INACTIVE'}"><i class="bi bi-dash-lg ss-rm-card__slot-icon" aria-hidden="true"></i></t:if>
                                    <span class="ss-rm-card__slot-code" aria-hidden="true">${e:esc(s.slotCode)}</span>
                                    <span class="visually-hidden">${e:esc(s.slotName)}: ${e:esc(s.todayLabel)}</span>
                                </li>
                            </t:forEach>
                        </ul>
                    </t:if>
                    <div class="ss-rm-card__actions">
                        <a href="${pageContext.request.contextPath}/admin/rooms/edit?roomId=${r.roomId}" class="btn btn-outline-soft btn-sm px-3">Edit</a>
                        <div class="dropdown ss-row-menu">
                            <button class="btn-trigger" type="button" data-bs-toggle="dropdown" aria-expanded="false"
                                    aria-label="More actions for ${e:esc(r.displayTitle)}">
                                <i class="bi bi-three-dots" aria-hidden="true"></i>
                            </button>
                            <ul class="dropdown-menu dropdown-menu-end">
                                <t:if test="${sessionScope.permissions.contains('ROOM_SLOT_MANAGE')}">
                                    <li><a class="dropdown-item" href="${pageContext.request.contextPath}/admin/rooms/slots?roomId=${r.roomId}">Manage slots &amp; prices</a></li>
                                </t:if>
                                <li>
                                    <form action="${pageContext.request.contextPath}/admin/rooms/active" method="post" class="m-0">
                                        <input type="hidden" name="_csrf" value="${e:esc(not empty _csrf ? _csrf : sessionScope._csrf)}">
                                        <input type="hidden" name="roomId" value="${r.roomId}">
                                        <input type="hidden" name="active" value="${r.active ? 'false' : 'true'}">
                                        <button type="submit" class="dropdown-item">${r.active ? 'Deactivate (hide from guests)' : 'Activate (show to guests)'}</button>
                                    </form>
                                </li>
                                <li><hr class="dropdown-divider"></li>
                                <li>
                                    <form action="${pageContext.request.contextPath}/admin/rooms/delete" method="post" class="m-0"
                                          onsubmit="return confirm('Delete room ${e:esc(r.roomNumber)}? This cannot be undone.');">
                                        <input type="hidden" name="_csrf" value="${e:esc(not empty _csrf ? _csrf : sessionScope._csrf)}">
                                        <input type="hidden" name="roomId" value="${r.roomId}">
                                        <button type="submit" class="dropdown-item dropdown-item--destructive">Delete room</button>
                                    </form>
                                </li>
                            </ul>
                        </div>
                    </div>
                </div>
            </article>
        </t:forEach>
    </div>

    <t:pagination result="${pageResult}" baseUrl="${pageContext.request.contextPath}/admin/rooms"/>
</t:layout>
