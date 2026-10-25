package util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

/**
 * Encapsulates paginated results, count metrics, and pagination navigation state.
 *
 * @param <T> the item type
 */
public class PageResult<T> {

    private final List<T> items;
    private final int totalItems;
    private final int page;
    private final int size;
    private final int totalPages;
    private final PageRequest request;

    public PageResult(List<T> items, int totalItems, PageRequest request) {
        this.items = (items != null) ? Collections.unmodifiableList(items) : Collections.<T>emptyList();
        this.totalItems = Math.max(0, totalItems);
        this.request = request;
        this.page = (request != null) ? request.getPage() : 1;
        this.size = (request != null) ? request.getSize() : 10;
        this.totalPages = Math.max(1, (int) Math.ceil((double) this.totalItems / this.size));
    }

    public List<T> getItems() {
        return items;
    }

    public int getTotalItems() {
        return totalItems;
    }

    public int getTotal() {
        return totalItems;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public int getPageSize() {
        return size;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public PageRequest getRequest() {
        return request;
    }

    public boolean isEmpty() {
        return items.isEmpty() || totalItems == 0;
    }

    public int getFromIndex() {
        if (isEmpty()) {
            return 0;
        }
        return (page - 1) * size + 1;
    }

    public int getToIndex() {
        if (isEmpty()) {
            return 0;
        }
        return Math.min(totalItems, page * size);
    }

    public boolean isHasPrevious() {
        return page > 1;
    }

    public boolean hasPrevious() {
        return page > 1;
    }

    public boolean isHasNext() {
        return page < totalPages;
    }

    public boolean hasNext() {
        return page < totalPages;
    }

    /**
     * Returns a human-readable summary such as "Showing 1-10 of 57",
     * or "No records found" when the result set is empty.
     */
    public String getSummary() {
        if (isEmpty()) {
            return "No records found";
        }
        return "Showing " + getFromIndex() + "-" + getToIndex() + " of " + totalItems;
    }

    // =========================================================================
    // INNER CLASS: PageRequest (merged into PageResult)
    // =========================================================================

    /**
     * Encapsulates pagination, sorting, search keyword, and custom filter parameters.
     * <p>
     * Security note: {@link #getSort()} returns a validated whitelist key (such as "name" or "created"),
     * never an SQL expression or column fragment. Each DAO must map whitelist keys to internal
     * SQL column identifiers using a static mapping table, preventing SQL injection into ORDER BY clauses.
     */
    public static class PageRequest {

        private int page = 1;
        private int size = 10;
        private String sort;
        private String dir;
        private String keyword = "";
        private final Map<String, String> filters = new LinkedHashMap<>();

        public PageRequest() {
        }

        public static PageRequest from(HttpServletRequest req, String[] sortKeys, String defaultSortKey, String defaultDir) {
            Set<String> set = (sortKeys != null) ? new HashSet<>(Arrays.asList(sortKeys)) : Collections.<String>emptySet();
            return from(req, set, defaultSortKey, defaultDir);
        }

        /**
         * Builds a PageRequest from an HttpServletRequest with whitelist sort verification.
         */
        public static PageRequest from(HttpServletRequest req, Set<String> sortKeys, String defaultSortKey, String defaultDir) {
            PageRequest pr = new PageRequest();

            // 1. page
            String pageStr = req.getParameter("page");
            if (pageStr != null && !pageStr.trim().isEmpty()) {
                try {
                    int p = Integer.parseInt(pageStr.trim());
                    pr.page = Math.max(1, p);
                } catch (NumberFormatException ignored) {
                    pr.page = 1;
                }
            } else {
                pr.page = 1;
            }

            // 2. size
            String sizeStr = req.getParameter("size");
            if (sizeStr != null && !sizeStr.trim().isEmpty()) {
                try {
                    int s = Integer.parseInt(sizeStr.trim());
                    if (s >= 5 && s <= 100) {
                        pr.size = s;
                    } else {
                        pr.size = 10;
                    }
                } catch (NumberFormatException ignored) {
                    pr.size = 10;
                }
            } else {
                pr.size = 10;
            }

            // 3. sort
            String sortStr = req.getParameter("sort");
            if (sortStr != null && sortKeys != null && sortKeys.contains(sortStr.trim())) {
                pr.sort = sortStr.trim();
            } else {
                pr.sort = defaultSortKey;
            }

            // 4. dir
            String dirStr = req.getParameter("dir");
            if (dirStr != null && ("ASC".equalsIgnoreCase(dirStr.trim()) || "DESC".equalsIgnoreCase(dirStr.trim()))) {
                pr.dir = dirStr.trim().toUpperCase();
            } else {
                pr.dir = (defaultDir != null && "DESC".equalsIgnoreCase(defaultDir.trim())) ? "DESC" : "ASC";
            }

            // 5. keyword
            String kw = req.getParameter("keyword");
            if (kw != null) {
                String trimmedKw = kw.trim();
                if (trimmedKw.length() > 100) {
                    trimmedKw = trimmedKw.substring(0, 100);
                }
                pr.keyword = trimmedKw;
            } else {
                pr.keyword = "";
            }

            // 6. filters: all other parameters excluding page, size, sort, dir, keyword, _csrf, action
            Set<String> reserved = new HashSet<>(Arrays.asList("page", "size", "sort", "dir", "keyword", "_csrf", "action"));
            Enumeration<String> paramNames = req.getParameterNames();
            while (paramNames.hasMoreElements()) {
                String paramName = paramNames.nextElement();
                if (!reserved.contains(paramName)) {
                    String val = req.getParameter(paramName);
                    if (val != null) {
                        String trimmedVal = val.trim();
                        if (!trimmedVal.isEmpty()) {
                            pr.filters.put(paramName, trimmedVal);
                        }
                    }
                }
            }

            return pr;
        }

        public int getPage() {
            return page;
        }

        public void setPage(int page) {
            this.page = Math.max(1, page);
        }

        public void clampPage(int totalItems) {
            if (totalItems <= 0) {
                this.page = 1;
                return;
            }
            int maxPage = (int) Math.ceil((double) totalItems / this.size);
            if (this.page > maxPage) {
                this.page = Math.max(1, maxPage);
            }
        }

        public int getSize() {
            return size;
        }

        public void setSize(int size) {
            if (size >= 5 && size <= 100) {
                this.size = size;
            } else {
                this.size = 10;
            }
        }

        public int getOffset() {
            return (page - 1) * size;
        }

        public String getSort() {
            return sort;
        }

        public void setSort(String sort) {
            this.sort = sort;
        }

        public String getDir() {
            return dir;
        }

        public void setDir(String dir) {
            this.dir = dir;
        }

        public String getKeyword() {
            return keyword;
        }

        public void setKeyword(String keyword) {
            this.keyword = (keyword != null) ? keyword.trim() : "";
        }

        public String getFilter(String name) {
            String val = filters.get(name);
            return (val != null) ? val : "";
        }

        public Map<String, String> getFilters() {
            return Collections.unmodifiableMap(filters);
        }

        public void setFilter(String name, String value) {
            if (name != null) {
                if (value != null && !value.trim().isEmpty()) {
                    filters.put(name, value.trim());
                } else {
                    filters.remove(name);
                }
            }
        }

        /**
         * Builds a query string skipping excluded keys and empty values.
         */
        public String toQueryString(String... excludeKeys) {
            Set<String> excludes = new HashSet<>();
            if (excludeKeys != null) {
                excludes.addAll(Arrays.asList(excludeKeys));
            }

            Map<String, String> params = new LinkedHashMap<>();
            if (!excludes.contains("page")) {
                params.put("page", String.valueOf(page));
            }
            if (!excludes.contains("size")) {
                params.put("size", String.valueOf(size));
            }
            if (!excludes.contains("sort") && sort != null && !sort.isEmpty()) {
                params.put("sort", sort);
            }
            if (!excludes.contains("dir") && dir != null && !dir.isEmpty()) {
                params.put("dir", dir);
            }
            if (!excludes.contains("keyword") && keyword != null && !keyword.isEmpty()) {
                params.put("keyword", keyword);
            }

            for (Map.Entry<String, String> entry : filters.entrySet()) {
                if (!excludes.contains(entry.getKey()) && entry.getValue() != null && !entry.getValue().isEmpty()) {
                    params.put(entry.getKey(), entry.getValue());
                }
            }

            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (!first) {
                    sb.append("&");
                }
                first = false;
                try {
                    sb.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8.name()))
                      .append("=")
                      .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8.name()));
                } catch (UnsupportedEncodingException ignored) {
                }
            }
            return sb.toString();
        }
    }
}
