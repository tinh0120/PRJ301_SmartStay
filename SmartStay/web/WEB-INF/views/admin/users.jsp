<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="e" uri="/WEB-INF/tld/smartstay.tld" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:layout title="Users" active="admin-users">
    <!-- Boutique Breadcrumb Navigation -->
    <nav aria-label="breadcrumb" class="mb-3">
        <ol class="breadcrumb boutique-breadcrumb m-0">
            <li class="breadcrumb-item">
                <a href="${pageContext.request.contextPath}/admin/dashboard" class="boutique-breadcrumb-link">
                    <i class="bi bi-house-door me-1"></i>Dashboard
                </a>
            </li>
            <li class="breadcrumb-item active" aria-current="page">Users</li>
        </ol>
    </nav>

    <div class="d-flex justify-content-between align-items-center mb-3">
        <div>
            <h1 class="h2 mb-1">User management</h1>
            <p class="text-muted small mb-0">Manage system users, administrators, staff profiles, and guests</p>
        </div>
        <div>
            <a href="${pageContext.request.contextPath}/admin/users/edit" class="btn btn-primary btn-sm">
                + New user
            </a>
        </div>
    </div>

    <t:if test="${not empty dbError}">
        <div class="alert alert-danger" role="alert">${e:esc(dbError)}</div>
    </t:if>

    <!-- Mobile Filter Toggle Button (< 768px) -->
    <div class="d-md-none mb-3">
        <button class="btn btn-outline-secondary btn-sm w-100 d-flex justify-content-between align-items-center" type="button" data-bs-toggle="collapse" data-bs-target="#userFilterCollapse" aria-expanded="false" aria-controls="userFilterCollapse">
            <span><i class="bi bi-funnel me-1"></i> Filters &amp; Search</span>
            <i class="bi bi-chevron-down"></i>
        </button>
    </div>

    <div class="card mb-4 shadow-sm collapse d-md-block" id="userFilterCollapse">
        <div class="card-body">
            <form action="${pageContext.request.contextPath}/admin/users" method="get" class="row g-2 align-items-end">
                <input type="hidden" name="sort" value="${e:esc(pr.sort)}">
                <input type="hidden" name="dir" value="${e:esc(pr.dir)}">

                <div class="col-md-4">
                    <label for="keyword" class="form-label small fw-semibold">Search</label>
                    <input type="text" class="form-control form-control-sm" id="keyword" name="keyword"
                           placeholder="Name, email, or phone..." maxlength="100"
                           value="${e:esc(pr.keyword)}">
                </div>

                <div class="col-md-3">
                    <label for="role" class="form-label small fw-semibold">Role</label>
                    <select class="form-select form-select-sm" id="role" name="role">
                        <option value="">All roles</option>
                        <option value="OWNER" ${pr.getFilter('role') == 'OWNER' ? 'selected' : ''}>Owner</option>
                        <option value="ADMIN" ${pr.getFilter('role') == 'ADMIN' ? 'selected' : ''}>Admin</option>
                        <option value="STAFF" ${pr.getFilter('role') == 'STAFF' ? 'selected' : ''}>Staff</option>
                        <option value="GUEST" ${pr.getFilter('role') == 'GUEST' ? 'selected' : ''}>Guest</option>
                    </select>
                </div>

                <div class="col-md-2">
                    <label for="status" class="form-label small fw-semibold">Status</label>
                    <select class="form-select form-select-sm" id="status" name="status">
                        <option value="">All statuses</option>
                        <option value="ACTIVE" ${pr.getFilter('status') == 'ACTIVE' ? 'selected' : ''}>Active</option>
                        <option value="LOCKED" ${pr.getFilter('status') == 'LOCKED' ? 'selected' : ''}>Locked</option>
                        <option value="INACTIVE" ${pr.getFilter('status') == 'INACTIVE' ? 'selected' : ''}>Inactive</option>
                    </select>
                </div>

                <div class="col-md-3 d-flex gap-2">
                    <button type="submit" class="btn btn-primary btn-sm flex-fill">Filter</button>
                    <a href="${pageContext.request.contextPath}/admin/users" class="btn btn-outline-secondary btn-sm flex-fill">Clear</a>
                </div>
            </form>
        </div>
    </div>

    <div class="card shadow-sm">
        <div class="table-responsive">
            <table class="table table-hover align-middle mb-0">
                <thead class="table-light">
                    <tr>
                        <th class="ps-3">
                            <a href="${pageContext.request.contextPath}/admin/users?keyword=${e:url(pr.keyword)}&role=${e:url(pr.getFilter('role'))}&status=${e:url(pr.getFilter('status'))}&sort=name&dir=${pr.sort == 'name' and pr.dir == 'ASC' ? 'DESC' : 'ASC'}"
                               class="text-decoration-none text-dark">
                                Name
                                <t:if test="${pr.sort == 'name'}">
                                    <small>${pr.dir == 'ASC' ? '&uarr;' : '&darr;'}</small>
                                </t:if>
                            </a>
                        </th>
                        <th>
                            <a href="${pageContext.request.contextPath}/admin/users?keyword=${e:url(pr.keyword)}&role=${e:url(pr.getFilter('role'))}&status=${e:url(pr.getFilter('status'))}&sort=email&dir=${pr.sort == 'email' and pr.dir == 'ASC' ? 'DESC' : 'ASC'}"
                               class="text-decoration-none text-dark">
                                Contact
                                <t:if test="${pr.sort == 'email'}">
                                    <small>${pr.dir == 'ASC' ? '&uarr;' : '&darr;'}</small>
                                </t:if>
                            </a>
                        </th>
                        <th>
                            <a href="${pageContext.request.contextPath}/admin/users?keyword=${e:url(pr.keyword)}&role=${e:url(pr.getFilter('role'))}&status=${e:url(pr.getFilter('status'))}&sort=role&dir=${pr.sort == 'role' and pr.dir == 'ASC' ? 'DESC' : 'ASC'}"
                               class="text-decoration-none text-dark">
                                Role
                                <t:if test="${pr.sort == 'role'}">
                                    <small>${pr.dir == 'ASC' ? '&uarr;' : '&darr;'}</small>
                                </t:if>
                            </a>
                        </th>
                        <th>
                            <a href="${pageContext.request.contextPath}/admin/users?keyword=${e:url(pr.keyword)}&role=${e:url(pr.getFilter('role'))}&status=${e:url(pr.getFilter('status'))}&sort=status&dir=${pr.sort == 'status' and pr.dir == 'ASC' ? 'DESC' : 'ASC'}"
                               class="text-decoration-none text-dark">
                                Status
                                <t:if test="${pr.sort == 'status'}">
                                    <small>${pr.dir == 'ASC' ? '&uarr;' : '&darr;'}</small>
                                </t:if>
                            </a>
                        </th>
                        <th>
                            <a href="${pageContext.request.contextPath}/admin/users?keyword=${e:url(pr.keyword)}&role=${e:url(pr.getFilter('role'))}&status=${e:url(pr.getFilter('status'))}&sort=created&dir=${pr.sort == 'created' and pr.dir == 'ASC' ? 'DESC' : 'ASC'}"
                               class="text-decoration-none text-dark">
                                Created
                                <t:if test="${pr.sort == 'created'}">
                                    <small>${pr.dir == 'ASC' ? '&uarr;' : '&darr;'}</small>
                                </t:if>
                            </a>
                        </th>
                        <th class="pe-3 text-end">Actions</th>
                    </tr>
                </thead>
                <tbody>
                    <t:if test="${empty pageResult.items}">
                        <tr>
                            <td colspan="6" class="text-center py-4 text-muted">
                                No users match the current search or filters.
                            </td>
                        </tr>
                    </t:if>

                    <t:forEach items="${pageResult.items}" var="u">
                        <tr>
                            <td class="ps-3">
                                <div class="fw-semibold">${e:esc(u.fullName)}</div>
                                <t:if test="${not empty u.position}">
                                    <div class="small text-muted">${e:esc(u.position)}</div>
                                </t:if>
                            </td>
                            <td>
                                <div><a href="mailto:${e:esc(u.email)}" class="text-decoration-none">${e:esc(u.email)}</a></div>
                                <div class="small text-muted">${e:esc(u.phone)}</div>
                            </td>
                            <td>
                                <t:if test="${u.roleName == 'OWNER'}">
                                    <span class="badge bg-dark">Owner</span>
                                </t:if>
                                <t:if test="${u.roleName == 'ADMIN'}">
                                    <span class="badge bg-primary">Admin</span>
                                </t:if>
                                <t:if test="${u.roleName == 'STAFF'}">
                                    <span class="badge bg-info text-dark">Staff</span>
                                </t:if>
                                <t:if test="${u.roleName == 'GUEST'}">
                                    <span class="badge bg-secondary">Guest</span>
                                </t:if>
                            </td>
                            <td>
                                <t:if test="${u.status == 'ACTIVE'}">
                                    <span class="badge bg-success">Active</span>
                                </t:if>
                                <t:if test="${u.status == 'LOCKED'}">
                                    <span class="badge bg-danger">Locked</span>
                                </t:if>
                                <t:if test="${u.status == 'INACTIVE'}">
                                    <span class="badge bg-secondary">Inactive</span>
                                </t:if>
                            </td>
                            <td class="small text-muted">
                                ${e:datetime(u.createdAt)}
                            </td>
                            <td class="pe-3 text-end">
                                <div class="d-inline-flex gap-1 align-items-center">
                                    <t:if test="${sessionScope.roleName == 'OWNER' or u.roleName != 'OWNER'}">
                                        <a href="${pageContext.request.contextPath}/admin/users/edit?userId=${u.userId}"
                                           class="btn btn-outline-primary btn-sm py-0 px-2" title="Edit user details">
                                            Edit
                                        </a>

                                        <form action="${pageContext.request.contextPath}/admin/users/delete" method="post"
                                              class="d-inline m-0" onsubmit="return confirm('Are you sure you want to delete user ${e:esc(u.email)}?');">
                                            <input type="hidden" name="_csrf" value="${e:esc(not empty _csrf ? _csrf : sessionScope._csrf)}">
                                            <input type="hidden" name="userId" value="${u.userId}">
                                            <t:if test="${u.userId == sessionScope.authUser.userId}">
                                                <button type="submit" class="btn btn-outline-danger btn-sm py-0 px-2"
                                                        disabled title="Cannot delete your own account">
                                                    Delete
                                                </button>
                                            </t:if>
                                            <t:if test="${u.userId != sessionScope.authUser.userId}">
                                                <button type="submit" class="btn btn-outline-danger btn-sm py-0 px-2">
                                                    Delete
                                                </button>
                                            </t:if>
                                        </form>
                                    </t:if>
                                    <t:if test="${sessionScope.roleName != 'OWNER' and u.roleName == 'OWNER'}">
                                        <span class="badge bg-light text-muted small py-1">Protected</span>
                                    </t:if>
                                </div>
                            </td>
                        </tr>
                    </t:forEach>
                </tbody>
            </table>
        </div>
    </div>

    <t:pagination result="${pageResult}" baseUrl="${pageContext.request.contextPath}/admin/users"/>
</t:layout>
