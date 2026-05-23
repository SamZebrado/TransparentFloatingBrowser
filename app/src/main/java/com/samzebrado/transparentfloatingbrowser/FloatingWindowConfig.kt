package com.samzebrado.transparentfloatingbrowser

import org.json.JSONObject

data class FloatingWindowConfig(
    val id: Int,
    var url: String,
    var transparentColors: List<String>,
    var tolerance: Int,
    var x: Int,
    var y: Int,
    var width: Int,
    var height: Int,
    var isVisible: Boolean = true,
    var viewModeAlpha: Int = 100
) {
    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("id", id)
        json.put("url", url)
        json.put("transparentColors", transparentColors.joinToString(","))
        json.put("tolerance", tolerance)
        json.put("x", x)
        json.put("y", y)
        json.put("width", width)
        json.put("height", height)
        json.put("isVisible", isVisible)
        json.put("viewModeAlpha", viewModeAlpha)
        return json
    }

    companion object {
        fun fromJson(json: JSONObject): FloatingWindowConfig {
            val colorsString = json.optString("transparentColors", AppPrefs.DEFAULT_TRANSPARENT_COLORS)
            val colors = colorsString.split(",")
                .map { it.trim() }
                .filter { it.matches(Regex("^#?[0-9a-fA-F]{6}$")) }
                .map { if (it.startsWith("#")) it.uppercase() else "#${it.uppercase()}" }
                .distinct()
                .ifEmpty { listOf(AppPrefs.DEFAULT_TRANSPARENT_COLORS) }

            val rawId = json.optInt("id", 0)
            val rawWidth = json.optInt("width", 600)
            val rawHeight = json.optInt("height", 800)
            val rawTolerance = json.optInt("tolerance", 48)
            val rawAlpha = json.optInt("viewModeAlpha", 100)

            return FloatingWindowConfig(
                id = if (rawId > 0) rawId else 1,
                url = json.optString("url", ""),
                transparentColors = colors,
                tolerance = rawTolerance.coerceIn(0, 255),
                x = json.optInt("x", 100),
                y = json.optInt("y", 100),
                width = rawWidth.coerceAtLeast(80),
                height = rawHeight.coerceAtLeast(80),
                isVisible = json.optBoolean("isVisible", true),
                viewModeAlpha = rawAlpha.coerceIn(
                    AppPrefs.MIN_VIEW_MODE_ALPHA_PERCENT,
                    AppPrefs.MAX_VIEW_MODE_ALPHA_PERCENT
                )
            )
        }

        fun createDefault(id: Int, defaultUrl: String, density: Float): FloatingWindowConfig {
            val defaultWidth = (OVERLAY_WIDTH_DP * density).toInt()
            val defaultHeight = (OVERLAY_HEIGHT_DP * density).toInt()
            val defaultX = (100 * density).toInt()
            val defaultY = (100 * density).toInt()

            return FloatingWindowConfig(
                id = id,
                url = defaultUrl,
                transparentColors = listOf(AppPrefs.DEFAULT_TRANSPARENT_COLORS),
                tolerance = 48,
                x = defaultX + (id * 50),
                y = defaultY + (id * 50),
                width = defaultWidth,
                height = defaultHeight,
                isVisible = true,
                viewModeAlpha = 100
            )
        }

        private const val OVERLAY_WIDTH_DP = 300
        private const val OVERLAY_HEIGHT_DP = 400
    }
}
