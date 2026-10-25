/**
 * SmartStay Chart Loader Utility (ES5)
 */
(function () {
    'use strict';

    var chartInstances = {};

    function getCssVar(name, fallback) {
        try {
            var val = getComputedStyle(document.documentElement).getPropertyValue(name);
            if (val && val.trim()) {
                return val.trim();
            }
        } catch (e) {
        }
        return fallback;
    }

    var chartPalette = {
        barPrimary: getCssVar('--chart-bar-primary', '#9A5B2E'),
        barSecondary: getCssVar('--chart-bar-secondary', '#D9B991'),
        linePrimary: getCssVar('--chart-line-primary', '#2B1B12'),
        lineSecondary: getCssVar('--chart-line-secondary', '#9A5B2E'),
        grid: getCssVar('--chart-grid', 'rgba(74, 51, 38, 0.1)'),
        get: function (name, fallback) {
            return getCssVar(name, fallback);
        }
    };

    function applyThemePalette(config) {
        if (!config || !config.data || !config.data.datasets) {
            return;
        }
        var palette = (window.SmartStayCharts && window.SmartStayCharts.palette) ? window.SmartStayCharts.palette : chartPalette;
        config.data.datasets.forEach(function (ds) {
            if (config.type === 'bar') {
                if (!ds.backgroundColor || ds.backgroundColor === '#1f6f8b' || ds.backgroundColor === '#0d6efd') {
                    ds.backgroundColor = palette.barPrimary;
                }
            } else if (config.type === 'line') {
                if (!ds.borderColor || ds.borderColor === '#0d6efd') {
                    ds.borderColor = palette.linePrimary;
                }
                if (!ds.backgroundColor) {
                    ds.backgroundColor = palette.lineSecondary;
                }
            }
        });
    }

    function formatDateTime(isoString) {
        try {
            var d = new Date(isoString);
            if (isNaN(d.getTime())) {
                return isoString;
            }
            var yyyy = d.getFullYear();
            var mm = ('0' + (d.getMonth() + 1)).slice(-2);
            var dd = ('0' + d.getDate()).slice(-2);
            var hh = ('0' + d.getHours()).slice(-2);
            var mi = ('0' + d.getMinutes()).slice(-2);
            return yyyy + '-' + mm + '-' + dd + ' ' + hh + ':' + mi;
        } catch (e) {
            return isoString;
        }
    }

    function renderChart(canvas, config) {
        applyThemePalette(config);
        var canvasId = canvas.id;
        if (chartInstances[canvasId]) {
            try {
                chartInstances[canvasId].destroy();
            } catch (ignored) {
            }
            delete chartInstances[canvasId];
        }
        var ctx = canvas.getContext('2d');
        chartInstances[canvasId] = new Chart(ctx, config);
    }

    window.SmartStayCharts = {
        palette: chartPalette,
        load: function (options) {
            if (!options || !options.canvasId) {
                return;
            }

            var canvas = document.getElementById(options.canvasId);
            var statusEl = options.statusId ? document.getElementById(options.statusId) : null;

            if (typeof Chart === 'undefined') {
                if (statusEl) {
                    statusEl.innerHTML = '<div class="alert alert-warning py-1 px-2 small">Chart library is not available.</div>';
                }
                return;
            }

            if (!canvas) {
                return;
            }

            function tryDrawFromCache(fallbackMsg) {
                var cachedObj = null;
                try {
                    var cachedRaw = localStorage.getItem(options.cacheKey);
                    if (cachedRaw) {
                        cachedObj = JSON.parse(cachedRaw);
                    }
                } catch (e) {
                    cachedObj = null;
                }

                if (cachedObj && cachedObj.data && typeof options.build === 'function') {
                    var config = options.build(cachedObj.data);
                    renderChart(canvas, config);
                    if (statusEl) {
                        var dateStr = formatDateTime(cachedObj.savedAt);
                        statusEl.innerHTML = '<div class="alert alert-warning py-1 px-2 small">Showing cached data saved at ' + dateStr + '.</div>';
                    }
                } else if (statusEl) {
                    statusEl.innerHTML = '<div class="alert alert-danger py-1 px-2 small">' + (fallbackMsg || 'Could not load chart data and no cached copy is available.') + '</div>';
                }
            }

            var xhr = new XMLHttpRequest();
            xhr.open('GET', options.url, true);
            xhr.setRequestHeader('Accept', 'application/json');
            xhr.timeout = 10000; // 10 seconds

            xhr.onload = function () {
                if (xhr.status === 200) {
                    try {
                        var data = JSON.parse(xhr.responseText);
                        try {
                            var cachePayload = {
                                savedAt: new Date().toISOString(),
                                data: data
                            };
                            localStorage.setItem(options.cacheKey, JSON.stringify(cachePayload));
                        } catch (e) {
                            // localStorage may fail in private mode or quota exceeded
                        }

                        if (typeof options.build === 'function') {
                            var config = options.build(data);
                            renderChart(canvas, config);
                        }
                        if (statusEl) {
                            statusEl.innerHTML = '';
                        }
                    } catch (err) {
                        tryDrawFromCache('Could not parse chart data.');
                    }
                } else {
                    tryDrawFromCache();
                }
            };

            xhr.onerror = function () {
                tryDrawFromCache();
            };

            xhr.ontimeout = function () {
                tryDrawFromCache();
            };

            xhr.send();
        }
    };
})();
