<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="e" uri="/WEB-INF/tld/smartstay.tld" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:layout title="Payments" active="admin-payments">
    <%-- V3: normal payments are quiet (dot + text); only "Needs review", refunds and failures get colour.
         One visible action per row: Resolve, only where a review is needed. --%>
    <div class="ss-page-header">
        <div>
            <h1 class="ss-page-header__title">Payments</h1>
            <p class="ss-page-header__subtitle">Every payment link and bank transfer, newest first.</p>
        </div>
    </div>

    <t:if test="${not empty dbError}">
        <div class="alert alert-danger py-2" role="alert">${e:esc(dbError)}</div>
    </t:if>

    <form action="${pageContext.request.contextPath}/admin/payments" method="get" class="ss-toolbar">
        <input type="hidden" name="sort" value="${e:esc(pr.sort)}">
        <input type="hidden" name="dir" value="${e:esc(pr.dir)}">
        <t:if test="${not empty pr.getFilter('status')}"><input type="hidden" name="status" value="${e:esc(pr.getFilter('status'))}"></t:if>
        <div class="ss-toolbar__main">
            <div class="ss-toolbar__search">
                <i class="bi bi-search ss-toolbar__search-icon" aria-hidden="true"></i>
                <label for="keyword" class="visually-hidden">Search payments</label>
                <input type="search" class="ss-toolbar__search-input" id="keyword" name="keyword" maxlength="100"
                       placeholder="Order code, booking code, guest name or email" value="${e:esc(pr.keyword)}">
            </div>
            <div class="ss-toolbar__chips" role="group" aria-label="Payment status">
                <a class="ss-chip ${empty pr.getFilter('status') ? 'ss-chip--active' : ''}"
                   href="${pageContext.request.contextPath}/admin/payments?keyword=${e:url(pr.keyword)}">All</a>
                <a class="ss-chip ${pr.getFilter('status') == 'REVIEW_REQUIRED' ? 'ss-chip--active' : ''}"
                   href="${pageContext.request.contextPath}/admin/payments?keyword=${e:url(pr.keyword)}&amp;status=REVIEW_REQUIRED">Needs review</a>
                <a class="ss-chip ${pr.getFilter('status') == 'REFUND_PENDING' ? 'ss-chip--active' : ''}"
                   href="${pageContext.request.contextPath}/admin/payments?keyword=${e:url(pr.keyword)}&amp;status=REFUND_PENDING">Refund pending</a>
                <a class="ss-chip ${pr.getFilter('status') == 'PAID' ? 'ss-chip--active' : ''}"
                   href="${pageContext.request.contextPath}/admin/payments?keyword=${e:url(pr.keyword)}&amp;status=PAID">Paid</a>
                <a class="ss-chip ${pr.getFilter('status') == 'FAILED' ? 'ss-chip--active' : ''}"
                   href="${pageContext.request.contextPath}/admin/payments?keyword=${e:url(pr.keyword)}&amp;status=FAILED">Failed</a>
                <%-- Item 3: five chips; the rarer states stay reachable from the status menu --%>
                <div class="dropdown d-inline-block">
                    <button class="ss-chip ${pr.getFilter('status') == 'PENDING' || pr.getFilter('status') == 'REFUNDED' || pr.getFilter('status') == 'EXPIRED' ? 'ss-chip--active' : ''}"
                            type="button" data-bs-toggle="dropdown" aria-expanded="false">
                        ${pr.getFilter('status') == 'PENDING' ? 'Waiting' : (pr.getFilter('status') == 'REFUNDED' ? 'Refunded' : (pr.getFilter('status') == 'EXPIRED' ? 'Expired' : 'More'))}
                        <i class="bi bi-chevron-down" aria-hidden="true"></i>
                    </button>
                    <ul class="dropdown-menu">
                        <li><a class="dropdown-item" href="${pageContext.request.contextPath}/admin/payments?keyword=${e:url(pr.keyword)}&amp;status=PENDING">Waiting</a></li>
                        <li><a class="dropdown-item" href="${pageContext.request.contextPath}/admin/payments?keyword=${e:url(pr.keyword)}&amp;status=REFUNDED">Refunded</a></li>
                        <li><a class="dropdown-item" href="${pageContext.request.contextPath}/admin/payments?keyword=${e:url(pr.keyword)}&amp;status=EXPIRED">Expired</a></li>
                    </ul>
                </div>
            </div>
        </div>
    </form>

    <div class="ss-dev-table">
        <t:if test="${empty pageResult.items}">
            <div class="ss-empty">
                <i class="bi bi-receipt ss-empty__icon" aria-hidden="true"></i>
                <p class="ss-empty__title">No payments found</p>
                <p class="ss-empty__desc">Nothing matches this search or status. Choose "All" to see every payment.</p>
            </div>
        </t:if>
        <t:if test="${not empty pageResult.items}">
            <div class="table-responsive">
                <table class="table align-middle mb-0 ss-table ss-pay-table">
                    <thead>
                        <tr>
                            <th scope="col" class="ps-3"><t:sortLink column="order" label="Booking" result="${pageResult}" baseUrl="${pageContext.request.contextPath}/admin/payments"/></th>
                            <th scope="col">Guest</th>
                            <th scope="col" class="text-end"><t:sortLink column="amount" label="Amount" result="${pageResult}" baseUrl="${pageContext.request.contextPath}/admin/payments"/></th>
                            <th scope="col"><t:sortLink column="status" label="Status" result="${pageResult}" baseUrl="${pageContext.request.contextPath}/admin/payments"/></th>
                            <th scope="col"><t:sortLink column="created" label="Created" result="${pageResult}" baseUrl="${pageContext.request.contextPath}/admin/payments"/></th>
                            <th scope="col"><t:sortLink column="paid_at" label="Paid" result="${pageResult}" baseUrl="${pageContext.request.contextPath}/admin/payments"/></th>
                            <th scope="col" class="pe-3 text-end"><span class="visually-hidden">Actions</span></th>
                        </tr>
                    </thead>
                    <tbody>
                        <t:forEach items="${pageResult.items}" var="p">
                            <tr>
                                <%-- Item 3: the booking code leads; the 12-digit gateway order code is the small line --%>
                                <td class="ps-3">
                                    <div class="ss-dev__name"><a class="ss-row-link" href="${pageContext.request.contextPath}/admin/bookings/detail?id=${p.bookingId}">${e:esc(p.bookingCode)}</a></div>
                                    <div class="ss-act__sub ss-num">Order ${p.orderCode}</div>
                                </td>
                                <td title="${e:esc(p.guestEmail)}">${e:esc(p.guestName)}</td>
                                <td class="text-end ss-num text-nowrap">${e:money(p.amount)}</td>
                                <td>
                                    <t:if test="${p.status == 'REVIEW_REQUIRED' || p.status == 'FAILED'}">
                                        <span class="ss-badge ss-badge--danger">${p.status == 'REVIEW_REQUIRED' ? 'Needs review' : 'Failed'}</span>
                                    </t:if>
                                    <t:if test="${p.status == 'REFUND_PENDING'}">
                                        <span class="ss-badge ss-badge--warning">Refund pending</span>
                                    </t:if>
                                    <t:if test="${p.status != 'REVIEW_REQUIRED' && p.status != 'FAILED' && p.status != 'REFUND_PENDING'}">
                                        <span class="ss-dev__status">
                                            <span class="ss-status-dot ${p.status == 'PAID' ? 'ss-status-dot--ok' : 'ss-status-dot--muted'}" aria-hidden="true"></span>
                                            ${p.status == 'PENDING' ? 'Waiting' : e:esc(e:humanStatus(p.status))}
                                        </span>
                                    </t:if>
                                </td>
                                <td class="ss-act__sub text-nowrap">${e:datetime(p.createdAt)}</td>
                                <td class="ss-act__sub">
                                    <div class="text-nowrap">${empty p.paidAt ? '-' : e:datetime(p.paidAt)}</div>
                                    <t:if test="${not empty p.resolutionNote}">
                                        <div class="ss-pay__note text-truncate" title="${e:esc(p.resolutionNote)}">${e:esc(p.resolutionNote)}</div>
                                    </t:if>
                                </td>
                                <td class="pe-3 text-end">
                                    <t:if test="${p.status == 'REVIEW_REQUIRED' and sessionScope.permissions.contains('PAYMENT_RESOLVE')}">
                                        <button type="button" class="btn btn-primary btn-sm"
                                                data-bs-toggle="modal" data-bs-target="#resolveModal_${p.paymentId}">Resolve</button>

                                        <div class="modal fade text-start" id="resolveModal_${p.paymentId}" tabindex="-1" aria-labelledby="resolveTitle_${p.paymentId}" aria-hidden="true">
                                            <div class="modal-dialog">
                                                <div class="modal-content">
                                                    <form action="${pageContext.request.contextPath}/admin/payments/resolve" method="post">
                                                        <input type="hidden" name="_csrf" value="${e:esc(not empty _csrf ? _csrf : sessionScope._csrf)}">
                                                        <input type="hidden" name="paymentId" value="${p.paymentId}">
                                                        <div class="modal-header">
                                                            <h2 class="modal-title h5" id="resolveTitle_${p.paymentId}">Resolve payment ${p.orderCode}</h2>
                                                            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                                                        </div>
                                                        <div class="modal-body">
                                                            <p class="mb-3">Booking ${e:esc(p.bookingCode)} &middot; ${e:money(p.amount)}</p>
                                                            <fieldset class="mb-3">
                                                                <legend class="form-label fw-semibold fs-6">What should happen?</legend>
                                                                <div class="form-check">
                                                                    <input class="form-check-input" type="radio" name="action" id="act_refund_${p.paymentId}" value="refund_pending" checked>
                                                                    <label class="form-check-label" for="act_refund_${p.paymentId}">Refund the guest (marks the payment "Refund pending")</label>
                                                                </div>
                                                                <div class="form-check">
                                                                    <input class="form-check-input" type="radio" name="action" id="act_dismiss_${p.paymentId}" value="dismiss">
                                                                    <label class="form-check-label" for="act_dismiss_${p.paymentId}">Keep the payment and close the review with a note</label>
                                                                </div>
                                                            </fieldset>
                                                            <div>
                                                                <label for="note_${p.paymentId}" class="form-label fw-semibold">Note</label>
                                                                <textarea class="form-control form-control-sm" id="note_${p.paymentId}" name="note" rows="3" maxlength="500" required
                                                                          placeholder="Why is it refunded or kept? This goes to the audit log."></textarea>
                                                            </div>
                                                        </div>
                                                        <div class="modal-footer">
                                                            <button type="button" class="btn btn-outline-soft btn-sm" data-bs-dismiss="modal">Cancel</button>
                                                            <button type="submit" class="btn btn-primary btn-sm">Save decision</button>
                                                        </div>
                                                    </form>
                                                </div>
                                            </div>
                                        </div>
                                    </t:if>
                                </td>
                            </tr>
                        </t:forEach>
                    </tbody>
                </table>
            </div>
        </t:if>
    </div>

    <t:pagination result="${pageResult}" baseUrl="${pageContext.request.contextPath}/admin/payments"/>
</t:layout>
