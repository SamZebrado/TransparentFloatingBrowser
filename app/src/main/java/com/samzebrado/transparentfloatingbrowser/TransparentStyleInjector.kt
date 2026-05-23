package com.samzebrado.transparentfloatingbrowser

import android.webkit.WebView

object TransparentStyleInjector {

    fun inject(
        webView: WebView,
        colorKey: String = "#000000",
        tolerance: Int = 48,
        onResult: ((String?) -> Unit)? = null
    ) {
        val js = buildInjectionScript(colorKey, tolerance)
        webView.evaluateJavascript(js) { result ->
            onResult?.invoke(result)
        }
    }

    private fun buildInjectionScript(
        colorKey: String,
        tolerance: Int
    ): String {
        val (targetR, targetG, targetB) = parseColor(colorKey)

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
                
                // 3. 检查是否接近目标颜色
                function isNearTarget(color, targetR, targetG, targetB, tolerance) {
                    if (!color) return false;
                    if (color.a <= 0) return false;
                    
                    var dr = Math.abs(color.r - targetR);
                    var dg = Math.abs(color.g - targetG);
                    var db = Math.abs(color.b - targetB);
                    
                    return (dr <= tolerance) && (dg <= tolerance) && (db <= tolerance);
                }
                
                // 4. 遍历所有元素
                var targetR = $targetR;
                var targetG = $targetG;
                var targetB = $targetB;
                var tolerance = $tolerance;
                
                var elements = document.querySelectorAll('*');
                for (var i = 0; i < elements.length; i++) {
                    var el = elements[i];
                    try {
                        var style = window.getComputedStyle(el);
                        var bgColor = style.backgroundColor;
                        
                        var parsed = parseRgbOrRgba(bgColor);
                        if (parsed && isNearTarget(parsed, targetR, targetG, targetB, tolerance)) {
                            el.style.setProperty('background-color', 'transparent', 'important');
                            injectedCount++;
                        }
                    } catch (e) {
                        // 忽略错误
                    }
                }
                
                return 'transparent applied: ' + injectedCount + ' elements';
            })();
        """.trimIndent()
    }

    private fun parseColor(hex: String): Triple<Int, Int, Int> {
        val cleanHex = hex.removePrefix("#")
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

        return Triple(r, g, b)
    }
}
