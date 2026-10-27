<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="e" uri="/WEB-INF/tld/smartstay.tld" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:layout title="Users" active="admin-users">
    <%-- V3: same anatomy as the other admin lists: header, toolbar (search + role chips), one visible
         action per row, Delete in the "⋯" menu, the Owner account marked Protected. --%>
    <div class="ss-page-header">
        <div>
            <h1 class="ss-page-header__title">Users</h1>
            <p class="ss-page-header__subtitle">Staff, admins and guest accounts.</p>
        </div>
        <a href="${pageContext.request.contextPath}/admin/users/edit" class="btn btn-primary btn-sm">New user</a>
    </div>

    <t:if test="${not empty dbError}">
        <div class="alert alert-danger" role="alert">${e:esc(dbError)}</div>
    </t:if>

    <form action="${pageContext.request.contextPath}/admin/users" method="get" class="ss-toolbar">
        <input type="hidden" name="sort" value="${e:esc(pr.sort)}">
        <input type="hidden" name="dir" value="${e:esc(pr.dir)}">
        <t:if test="${not empty pr.getFilter('role')}"><input type="hidden" name="role" value="${e:esc(pr.getFilter('role'))}"></t:if>
        <div class="ss-toolbar__main">
            <div class="ss-toolbar__search">
                <i class="bi bi-search ss-toolbar__search-icon" aria-hidden="true"></i>
                <label for="keyword" class="visually-hidden">Search users</label>
                <input type="search" class="ss-toolbar__search-input" id="keyword" name="keyword" maxlength="100"
                       placeholder="Name, email or phone" value="${e:esc(pr.keyword)}">
            </div>
            <div class="ss-toolbar__chips" role="group" aria-label="Role">
                <a class="ss-chip ${empty pr.getFilter('role') ? 'ss-chip--active' : ''}"
                   href="${pageContext.request.contextPath}/admin/users?keyword=${e:url(pr.keyword)}&amp;status=${e:url(pr.getFilter('status'))}">All</a>
                <a class="ss-chip ${pr.getFilter('role') == 'OWNER' ? 'ss-chip--active' : ''}"
                   href="${pageContext.request.contextPath}/admin/users?keyword=${e:url(pr.keyword)}&amp;status=${e:url(pr.getFilter('status'))}&amp;role=OWNER">Owner</a>
                <a class="ss-chip ${pr.getFilter('role') == 'ADMIN' ? 'ss-chip--active' : ''}"
                   href="${pageContext.request.contextPath}/admin/users?keyword=${e:url(pr.keyword)}&amp;status=${e:url(pr.getFilter('status'))}&amp;role=ADMIN">Admin</a>
                <a class="ss-chip ${pr.getFilter('role') == 'STAFF' ? 'ss-chip--active' : ''}"
                   href="${pageContext.request.contextPath}/admin/users?keyword=${e:url(pr.keyword)}&amp;status=${e:url(pr.getFilter('status'))}&amp;role=STAFF">Staff</a>
                <a class="ss-chip ${pr.getFilter('role') == 'GUEST' ? 'ss-chip--active' : ''}"
                   href="${pageContext.request.contextPath}/admin/users?keyword=${e:url(pr.keyword)}&amp;status=${e:url(pr.getFilter('status'))}&amp;role=GUEST">Guest</a>
            </div>
            <div class="ss-users__status">
                <label for="status" class="visually-hidden">Status</label>
                <select class="form-select form-select-sm" id="status" name="status" onchange="this.form.submit()">
                    <option value="">Any status</option>
                    <option value="ACTIVE" ${pr.getFilter('status') == 'ACTIVE' ? 'selected' : ''}>Active</option>
                    <option value="LOCKED" ${pr.getFilter('status') == 'LOCKED' ? 'selected' : ''}>Locked</option>
                    <option value="INACTIVE" ${pr.getFilter('status') == 'INACTIVE' ? 'selected' : ''}>Inactive</option>
                </select>
            </div>
        </div>
    </form>

    <div class="ss-dev-table">
        <t:if test="${empty pageResult.items}">
            <div class="ss-empty">
                <i class="bi bi-people ss-empty__icon" aria-hidden="true"></i>
                <p class="ss-empty__title">No users found</p>
                <p class="ss-empty__desc">Nothing matches this search. Clear the filters or create a new user.</p>
            </div>
        </t:if>
        <t:if test="${not empty pageResult.items}">
            <div class="table-responsive">
                <table class="table align-middle mb-0 ss-table">
                    <thead>
                        <tr>
                            <th scope="col" class="ps-3"><t:sortLink result="${pageResult}" baseUrl="${pageContext.request.contextPath}/admin/users" column="name" label="Name"/></th>
                            <th scope="col"><t:sortLink result="${pageResult}" baseUrl="${pageContext.request.contextPath}/admin/users" column="email" label="Contact"/></th>
                            <th scope="col"><t:sortLink result="${pageResult}" baseUrl="${pageContext.request.contextPath}/admin/users" column="role" label="Role"/></th>
                            <th scope="col"><t:sortLink result="${pageResult}" baseUrl="${pageContext.request.contextPath}/admin/users" column="status" label="Status"/></th>
                            <th scope="col"><t:sortLink result="${pageResult}" baseUrl="${pageContext.request.contextPath}/admin/users" column="created" label="Created"/></th>
                            <th scope="col" class="pe-3 text-end"><span class="visually-hidden">Actions</span></th>
                        </tr>
                    </thead>
                    <tbody>
                        <t:forEach items="${pageResult.items}" var="u">
                            <tr class="${u.status == 'INACTIVE' ? 'ss-dev-row--off' : ''}">
                                <td class="ps-3">
                                    <%-- Item 2: the name opens the edit page; other actions live in the row menu --%>
                                    <div class="ss-dev__name">
                                        <t:if test="${sessionScope.roleName == 'OWNER' or u.roleName != 'OWNER'}"><a class="ss-row-link" href="${pageContext.request.contextPath}/admin/users/edit?userId=${u.userId}">${e:esc(u.fullName)}</a></t:if>
                                        <t:if test="${!(sessionScope.roleName == 'OWNER' or u.roleName != 'OWNER')}">${e:esc(u.fullName)}</t:if>
                                        <t:if test="${u.roleName == 'OWNER'}"><span class="ss-users__protected" title="The Owner account can only be changed by the Owner">Protected</span></t:if>
                                    </div>
                                    <t:if test="${not empty u.position}"><div class="ss-act__sub">${e:esc(u.position)}</div></t:if>
                                </td>
                                <td>
                                    <div>${e:esc(u.email)}</div>
                                    <div class="ss-act__sub">${e:esc(u.phone)}</div>
                                </td>
                                <td>${e:esc(e:humanStatus(u.roleName))}</td>
                                <td>
                                    <span class="ss-dev__status">
                                        <span class="ss-status-dot ${u.status == 'ACTIVE' ? 'ss-status-dot--ok' : (u.status == 'LOCKED' ? 'ss-status-dot--danger' : 'ss-status-dot--muted')}" aria-hidden="true"></span>
                                        <span class="${u.status == 'LOCKED' ? 'ss-dev__bad' : ''}">${e:esc(e:humanStatus(u.status))}</span>
                                    </span>
                                </td>
                                <td class="ss-act__sub">${e:date(u.createdAt)}</td>
                                <td class="pe-3 text-end text-nowrap">
                                    <t:if test="${sessionScope.roleName == 'OWNER' or u.roleName != 'OWNER'}">
                                            <div class="dropdown d-inline-block ss-row-menu">
                                                <button class="btn-trigger" type="button" data-bs-toggle="dropdown" aria-expanded="false"
                                                        aria-label="More actions for ${e:esc(u.fullName)}">
                                                    <i class="bi bi-three-dots" aria-hidden="true"></i>
                                                </button>
                                                <ul class="dropdown-menu dropdown-menu-end">
                                                    <li><a class="dropdown-item" href="${pageContext.request.contextPath}/admin/users/edit?userId=${u.userId}">Edit</a></li>
                                                    <t:if test="${u.userId != sessionScope.authUser.userId}">
                                                    <li><hr class="dropdown-divider"></li>
                                                    <li>
                                                        <form action="${pageContext.request.contextPath}/admin/users/delete" method="post" class="m-0"
                                                              onsubmit="return confirm('Delete the account ${e:esc(u.email)}? This cannot be undone.');">
                                                            <input type="hidden" name="_csrf" value="${e:esc(not empty _csrf ? _csrf : sessionScope._csrf)}">
                                                            <input type="hidden" name="userId" value="${u.userId}">
                                                            <button type="submit" class="dropdown-item dropdown-item--destructive">Delete user</button>
                                                        </form>
                                                    </li>
                                                    </t:if>
                                                </ul>
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

    <t:pagination result="${pageResult}" baseUrl="${pageContext.request.contextPath}/admin/users"/>
</t:layout>
