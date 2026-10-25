<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="e" uri="/WEB-INF/tld/smartstay.tld" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:layout title="Payments" active="admin-payments">
    <!-- Boutique Breadcrumb Navigation -->
    <nav aria-label="breadcrumb" class="mb-3">
        <ol class="breadcrumb boutique-breadcrumb m-0">
            <li class="breadcrumb-item">
                <a href="${pageContext.request.contextPath}/admin/dashboard" class="boutique-breadcrumb-link">
                    <i class="bi bi-house-door me-1"></i>Dashboard
                </a>
            </li>
            <li class="breadcrumb-item active" aria-current="page">Payments</li>
        </ol>
    </nav>

    <div class="d-flex justify-content-between align-items-center mb-3">
        <div>
            <h1 class="h3 fw-bold mb-1">Payment transactions</h1>
            <p class="text-muted small mb-0">Track all gateway transactions, orders, and resolve disputed payments.</p>
        </div>
    </div>

    <t:if test="${not empty dbError}">
        <div class="alert alert-danger py-2" role="alert">${e:esc(dbError)}</div>
    </t:if>

    <!-- Filter Card -->
    <div class="card shadow-sm border-0 mb-4 bg-light">
        <div class="card-body p-3">
            <form action="${pageContext.request.contextPath}/admin/payments" method="get" class="row g-2 align-items-end">
                <input type="hidden" name="sort" value="${e:esc(pr.sort)}">
                <input type="hidden" name="dir" value="${e:esc(pr.dir)}">

                <div class="col-md-5">
                    <label for="keyword" class="form-label small fw-semibold">Search</label>
                    <input type="text" class="form-control form-control-sm" id="keyword" name="keyword"
                           placeholder="Order code, booking code, guest name or email..." maxlength="100"
                           value="${e:esc(pr.keyword)}">
                </div>

                <div class="col-md-3">
                    <label for="status" class="form-label small fw-semibold">Status</label>
                    <select class="form-select form-select-sm" id="status" name="status">
                        <option value="">All statuses</option>
                        <option value="REVIEW_REQUIRED" ${pr.getFilter('status') == 'REVIEW_REQUIRED' ? 'selected' : ''}>REVIEW_REQUIRED</option>
                        <option value="PAID" ${pr.getFilter('status') == 'PAID' ? 'selected' : ''}>PAID</option>
                        <option value="PENDING" ${pr.getFilter('status') == 'PENDING' ? 'selected' : ''}>PENDING</option>
                        <option value="REFUND_PENDING" ${pr.getFilter('status') == 'REFUND_PENDING' ? 'selected' : ''}>REFUND_PENDING</option>
                        <option value="REFUNDED" ${pr.getFilter('status') == 'REFUNDED' ? 'selected' : ''}>REFUNDED</option>
                        <option value="FAILED" ${pr.getFilter('status') == 'FAILED' ? 'selected' : ''}>FAILED</option>
                        <option value="EXPIRED" ${pr.getFilter('status') == 'EXPIRED' ? 'selected' : ''}>EXPIRED</option>
                    </select>
                </div>

                <div class="col-md-4 d-flex gap-2">
                    <button type="submit" class="btn btn-primary btn-sm flex-fill">Filter</button>
                    <a href="${pageContext.request.contextPath}/admin/payments" class="btn btn-outline-secondary btn-sm flex-fill">Clear</a>
                </div>
            </form>
        </div>
    </div>

    <!-- Summary -->
    <div class="d-flex justify-content-between align-items-center mb-2">
        <span class="text-muted small">${e:esc(pageResult.summary)}</span>
    </div>

    <!-- Table -->
    <div class="card shadow-sm border-0 mb-3">
        <div class="table-responsive">
            <table class="table table-sm table-hover align-middle mb-0">
                <thead class="table-light">
                    <tr>
                        <th class="ps-3">
                            <t:sortLink column="order" label="Order Code" result="${pageResult}" baseUrl="${pageContext.request.contextPath}/admin/payments"/>
                        </th>
                        <th>Booking</th>
                        <th>Guest</th>
                        <th>
                            <t:sortLink column="amount" label="Amount" result="${pageResult}" baseUrl="${pageContext.request.contextPath}/admin/payments"/>
                        </th>
                        <th>
                            <t:sortLink column="status" label="Status" result="${pageResult}" baseUrl="${pageContext.request.contextPath}/admin/payments"/>
                        </th>
                        <th>
                            <t:sortLink column="created" label="Created" result="${pageResult}" baseUrl="${pageContext.request.contextPath}/admin/payments"/>
                        </th>
                        <th>
                            <t:sortLink column="paid_at" label="Paid At" result="${pageResult}" baseUrl="${pageContext.request.contextPath}/admin/payments"/>
                        </th>
                        <th class="pe-3 text-end">Actions</th>
                    </tr>
                </thead>
                <tbody>
                    <t:if test="${empty pageResult.items}">
                        <tr>
                            <td colspan="8" class="text-center py-4 text-muted">
                                No payment records match the current filters.
                            </td>
                        </tr>
                    </t:if>

                    <t:forEach items="${pageResult.items}" var="p">
                        <tr>
                            <td class="ps-3 font-monospace fw-semibold">
                                ${p.orderCode}
                            </td>
                            <td>
                                <a href="${pageContext.request.contextPath}/admin/bookings/detail?id=${p.bookingId}" class="fw-semibold text-decoration-none">
                                    ${e:esc(p.bookingCode)}
                                </a>
                            </td>
                            <td>
                                <div class="fw-semibold">${e:esc(p.guestName)}</div>
                                <div class="small text-muted">${e:esc(p.guestEmail)}</div>
                            </td>
                            <td class="fw-semibold">${e:money(p.amount)}</td>
                            <td>
                                <t:if test="${p.status == 'PAID'}">
                                    <span class="badge bg-success">PAID</span>
                                </t:if>
                                <t:if test="${p.status == 'PENDING'}">
                                    <span class="badge bg-warning text-dark">PENDING</span>
                                </t:if>
                                <t:if test="${p.status == 'REVIEW_REQUIRED'}">
                                    <span class="badge bg-danger">REVIEW_REQUIRED</span>
                                </t:if>
                                <t:if test="${p.status == 'REFUND_PENDING'}">
                                    <span class="badge bg-info text-dark">REFUND_PENDING</span>
                                </t:if>
                                <t:if test="${p.status == 'REFUNDED'}">
                                    <span class="badge bg-secondary">REFUNDED</span>
                                </t:if>
                                <t:if test="${p.status == 'FAILED'}">
                                    <span class="badge bg-danger">FAILED</span>
                                </t:if>
                                <t:if test="${p.status == 'EXPIRED'}">
                                    <span class="badge bg-secondary">EXPIRED</span>
                                </t:if>
                            </td>
                            <td class="small text-muted">${e:datetime(p.createdAt)}</td>
                            <td class="small text-muted">
                                <div>${e:datetime(p.paidAt)}</div>
                                <t:if test="${not empty p.resolutionNote}">
                                    <div class="text-secondary small fst-italic">${e:esc(p.resolutionNote)}</div>
                                </t:if>
                            </td>
                            <td class="pe-3 text-end">
                                <t:if test="${p.status == 'REVIEW_REQUIRED' and sessionScope.permissions.contains('PAYMENT_RESOLVE')}">
                                    <button type="button" class="btn btn-outline-danger btn-sm py-0 px-2"
                                            data-bs-toggle="modal" data-bs-target="#resolveModal_${p.paymentId}">
                                        Resolve
                                    </button>

                                    <!-- Modal for this payment -->
                                    <div class="modal fade text-start" id="resolveModal_${p.paymentId}" tabindex="-1" aria-hidden="true">
                                        <div class="modal-dialog">
                                            <div class="modal-content">
                                                <form action="${pageContext.request.contextPath}/admin/payments/resolve" method="post">
                                                    <input type="hidden" name="_csrf" value="${e:esc(not empty _csrf ? _csrf : sessionScope._csrf)}">
                                                    <input type="hidden" name="paymentId" value="${p.paymentId}">
                                                    <div class="modal-header">
                                                        <h5 class="modal-title">Resolve Payment #${p.paymentId}</h5>
                                                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                                                    </div>
                                                    <div class="modal-body">
                                                        <div class="mb-2">
                                                            <span class="text-muted small d-block">Order &amp; Booking</span>
                                                            <strong>${p.orderCode}</strong> (Booking ${e:esc(p.bookingCode)}) &ndash; ${e:money(p.amount)}
                                                        </div>
                                                        <div class="mb-3">
                                                            <label class="form-label fw-semibold">Action <span class="text-danger">*</span></label>
                                                            <div class="form-check">
                                                                <input class="form-check-input" type="radio" name="action" id="act_refund_${p.paymentId}" value="refund_pending" checked>
                                                                <label class="form-check-label" for="act_refund_${p.paymentId}">
                                                                    <strong>Mark refund pending</strong> (status &rarr; REFUND_PENDING)
                                                                </label>
                                                            </div>
                                                            <div class="form-check">
                                                                <input class="form-check-input" type="radio" name="action" id="act_dismiss_${p.paymentId}" value="dismiss">
                                                                <label class="form-check-label" for="act_dismiss_${p.paymentId}">
                                                                    <strong>Dismiss with note</strong> (status unchanged, logs resolution note)
                                                                </label>
                                                            </div>
                                                        </div>
                                                        <div class="mb-3">
                                                            <label for="note_${p.paymentId}" class="form-label fw-semibold">Resolution Note <span class="text-danger">*</span></label>
                                                            <textarea class="form-control form-control-sm" id="note_${p.paymentId}" name="note" rows="3" maxlength="500" required
                                                                      placeholder="Explain why this disputed payment is marked for refund or dismissed..."></textarea>
                                                            <div class="form-text">Max 500 characters. Audit log will record this note.</div>
                                                        </div>
                                                    </div>
                                                    <div class="modal-footer">
                                                        <button type="button" class="btn btn-outline-secondary btn-sm" data-bs-dismiss="modal">Close</button>
                                                        <button type="submit" class="btn btn-primary btn-sm">Confirm Resolution</button>
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
    </div>

    <t:pagination result="${pageResult}" baseUrl="${pageContext.request.contextPath}/admin/payments"/>
</t:layout>
