package com.samzebrado.transparentfloatingbrowser

import android.webkit.WebView

object TransparentStyleInjector {

    fun inject(
        webView: WebView,
        colorKeys: List<String> = listOf("#000000"),
        tolerance: Int = 48,
        onResult: ((String?) -> Unit)? = null
    ) {
        val js = buildFullInjectionScript(colorKeys, tolerance)
        webView.evaluateJavascript(js) { result ->
            onResult?.invoke(result)
        }
    }

    fun applyTransparency(
        webView: WebView,
        onResult: ((String?) -> Unit)? = null
    ) {
        val js = "if (window.__tfbApplyTransparency) { window.__tfbApplyTransparency(); } else { 'no injector installed'; }"
        webView.evaluateJavascript(js) { result ->
            onResult?.invoke(result)
        }
    }

    private fun buildFullInjectionScript(
        colorKeys: List<String>,
        tolerance: Int
    ): String {
        val parsedColors = colorKeys.mapNotNull { parseColorOrNull(it) }
            .ifEmpty { listOf(Triple(0, 0, 0)) }

        val targetsJson = parsedColors.joinToString(",", "[", "]") { (r, g, b) ->
            "{ r: $r, g: $g, b: $b }"
        }

        return """
            (function() {
                var targets = $targetsJson;
                var tolerance = $tolerance;
                var debounceMs = 100;
                var debounceTimer = null;

                if (window.__tfbTransparencyDiag) {
                    window.__tfbApplyTransparency();
                    return 'already injected, reapplied';
                }

                window.__tfbTransparencyDiag = {
                    runCount: 0,
                    mutationTriggerCount: 0,
                    lastRunAt: 0,
                    scannedElements: 0,
                    changedElements: 0,
                    lastResult: null
                };

                function parseRgbOrRgba(str) {
                    var match = str.match(/rgba?\s*\(\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)\s*(?:,\s*([\d.]+))?\s*\)/);
                    if (match) {
                        return {
                            r: parseInt(match[1]),
                            g: parseInt(match[2]),
                            b: parseInt(match[3]),
                            a: match[4] ? parseFloat(match[4]) : 1.0
                        };
                    }
                    return null;
                }

                function isNearAnyTarget(color, targets, tolerance) {
                    if (!color) return false;
                    if (color.a <= 0) return false;
                    for (var i = 0; i < targets.length; i++) {
                        var t = targets[i];
                        var dr = Math.abs(color.r - t.r);
                        var dg = Math.abs(color.g - t.g);
                        var db = Math.abs(color.b - t.b);
                        if ((dr <= tolerance) && (dg <= tolerance) && (db <= tolerance)) {
                            return true;
                        }
                    }
                    return false;
                }

                function scanAndApply() {
                    var styleEl = document.getElementById('tfb-transparent-style');
                    if (!styleEl) {
                        styleEl = document.createElement('style');
                        styleEl.id = 'tfb-transparent-style';
                        document.head.appendChild(styleEl);
                    }
                    styleEl.textContent = 'html, body { background: transparent !important; }';

                    var scanned = 0;
                    var changed = 0;

                    var elements = document.querySelectorAll('*');
                    for (var i = 0; i < elements.length; i++) {
                        var el = elements[i];
                        scanned++;
                        try {
                            var style = window.getComputedStyle(el);
                            var bgColor = style.backgroundColor;
                            var parsed = parseRgbOrRgba(bgColor);
                            if (parsed && isNearAnyTarget(parsed, targets, tolerance)) {
                                el.style.setProperty('background-color', 'transparent', 'important');
                                changed++;
                            }
                        } catch (e) {}
                    }

                    window.__tfbTransparencyDiag.runCount++;
                    window.__tfbTransparencyDiag.lastRunAt = Date.now();
                    window.__tfbTransparencyDiag.scannedElements = scanned;
                    window.__tfbTransparencyDiag.changedElements = changed;
                    window.__tfbTransparencyDiag.lastResult = 'scanned ' + scanned + ', changed ' + changed;

                    return window.__tfbTransparencyDiag.lastResult;
                }

                function debouncedScan() {
                    if (debounceTimer) {
                        clearTimeout(debounceTimer);
                    }
                    debounceTimer = setTimeout(function() {
                        scanAndApply();
                    }, debounceMs);
                }

                window.__tfbApplyTransparency = function() {
                    return scanAndApply();
                };

                var observer = new MutationObserver(function(mutations) {
                    window.__tfbTransparencyDiag.mutationTriggerCount++;
                    debouncedScan();
                });

                function startObserving() {
                    observer.observe(document.documentElement, {
                        subtree: true,
                        childList: true,
                        attributes: true,
                        attributeFilter: ['class', 'style', 'hidden']
                    });
                }

                window.addEventListener('hashchange', function() {
                    debouncedScan();
                });

                window.addEventListener('popstate', function() {
                    debouncedScan();
                });

                if (document.readyState === 'loading') {
                    document.addEventListener('DOMContentLoaded', function() {
                        startObserving();
                        scanAndApply();
                    });
                } else {
                    startObserving();
                }

                return scanAndApply();
            })();
        """.trimIndent()
    }

    private fun parseColorOrNull(hex: String): Triple<Int, Int, Int>? {
        return try {
            val cleanHex = hex.removePrefix("#")
            if (!cleanHex.matches(Regex("^[0-9a-fA-F]{6}$"))) {
                return null
            }

            val r: Int
            val g: Int
            val b: Int

            if (cleanHex.length == 3) {
                r = cleanHex[0].toString().repeat(2).toInt(16)
                g = cleanHex[1].toString().repeat(2).toInt(16)
                b = cleanHex[2].toString().repeat(2).toInt(16)
            } else {
                r = cleanHex.substring(0, 2).toInt(16)
                g = cleanHex.substring(2, 4).toInt(16)
                b = cleanHex.substring(4, 6).toInt(16)
            }

            Triple(r, g, b)
        } catch (e: Exception) {
            null
        }
    }

    private fun parseColor(hex: String): Triple<Int, Int, Int> {
        return parseColorOrNull(hex) ?: Triple(0, 0, 0)
    }
}
