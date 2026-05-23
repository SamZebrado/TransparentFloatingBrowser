package com.samzebrado.transparentfloatingbrowser

import android.webkit.WebView

object TransparentStyleInjector {

    fun inject(
        webView: WebView,
        colorKeys: List<String> = listOf("#000000"),
        tolerance: Int = 48,
        onResult: ((String?) -> Unit)? = null
    ) {
        val js = buildInjectionScript(colorKeys, tolerance)
        webView.evaluateJavascript(js) { result ->
            onResult?.invoke(result)
        }
    }

    private fun buildInjectionScript(
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
                var injectedCount = 0;

                // 1. 创建或更新样式标签
                var styleEl = document.getElementById('tfb-transparent-style');
                if (!styleEl) {
                    styleEl = document.createElement('style');
                    styleEl.id = 'tfb-transparent-style';
                    document.head.appendChild(styleEl);
                }
                styleEl.textContent = 'html, body { background: transparent !important; }';

                // 2. 解析 computed style 颜色
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

                // 3. 检查是否接近任意目标颜色
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

                // 4. 遍历所有元素
                var targets = $targetsJson;
                var tolerance = $tolerance;

                var elements = document.querySelectorAll('*');
                for (var i = 0; i < elements.length; i++) {
                    var el = elements[i];
                    try {
                        var style = window.getComputedStyle(el);
                        var bgColor = style.backgroundColor;

                        var parsed = parseRgbOrRgba(bgColor);
                        if (parsed && isNearAnyTarget(parsed, targets, tolerance)) {
                            el.style.setProperty('background-color', 'transparent', 'important');
                            injectedCount++;
                        }
                    } catch (e) {
                        // 忽略错误
                    }
                }

                return 'transparent applied: ' + injectedCount + ' elements for ' + targets.length + ' colors';
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
