package com.example

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color as AndroidColor
import org.json.JSONArray
import org.json.JSONObject

/**
 * Background Type enumeration for Custom Keyboard Themes.
 */
enum class BackgroundType {
    SOLID,
    GRADIENT,
    IMAGE
}

/**
 * Data model for a User-Created Custom Keyboard Theme.
 */
data class CustomThemeModel(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String = "My Custom Theme",
    val backgroundType: BackgroundType = BackgroundType.SOLID,
    val backgroundColor: Int = AndroidColor.parseColor("#0F172A"),
    val gradientStartColor: Int = AndroidColor.parseColor("#4C1D95"),
    val gradientEndColor: Int = AndroidColor.parseColor("#0F172A"),
    val galleryImageUri: String? = null,
    val imageOverlayOpacity: Float = 0.45f,
    val keyBackgroundColor: Int = AndroidColor.parseColor("#1E293B"),
    val keyBackgroundOpacity: Float = 0.90f,
    val keyCornerRadius: Float = 14f,
    val keyTextColor: Int = AndroidColor.parseColor("#FFFFFF"),
    val accentColor: Int = AndroidColor.parseColor("#38BDF8"),
    val keyPressRippleColor: Int = AndroidColor.parseColor("#334155"),
    val isDark: Boolean = true
) {
    /**
     * Converts this custom theme model into a standard runtime KeyboardTheme.
     */
    fun toKeyboardTheme(): KeyboardTheme {
        val calculatedDark = if (backgroundType == BackgroundType.GRADIENT) {
            val lum = (AndroidColor.red(gradientEndColor) * 0.299 +
                    AndroidColor.green(gradientEndColor) * 0.587 +
                    AndroidColor.blue(gradientEndColor) * 0.114) / 255.0
            lum < 0.5
        } else {
            val lum = (AndroidColor.red(backgroundColor) * 0.299 +
                    AndroidColor.green(backgroundColor) * 0.587 +
                    AndroidColor.blue(backgroundColor) * 0.114) / 255.0
            lum < 0.5
        }

        return KeyboardTheme(
            id = "custom_$id",
            name = name,
            backgroundColor = backgroundColor,
            keyColor = keyBackgroundColor,
            keyPressedColor = keyPressRippleColor,
            actionKeyColor = accentColor,
            textColor = keyTextColor,
            accentColor = accentColor,
            sublabelColor = if (calculatedDark) AndroidColor.parseColor("#94A3B8") else AndroidColor.parseColor("#64748B"),
            isGradient = (backgroundType == BackgroundType.GRADIENT),
            gradientStartColor = gradientStartColor,
            gradientEndColor = gradientEndColor,
            isDark = calculatedDark,
            keyCornerRadius = keyCornerRadius,
            keyBackgroundOpacity = keyBackgroundOpacity,
            keyPressRippleColor = keyPressRippleColor
        )
    }

    fun toJsonObject(): JSONObject {
        val json = JSONObject()
        json.put("id", id)
        json.put("name", name)
        json.put("backgroundType", backgroundType.name)
        json.put("backgroundColor", backgroundColor)
        json.put("gradientStartColor", gradientStartColor)
        json.put("gradientEndColor", gradientEndColor)
        json.put("galleryImageUri", galleryImageUri ?: "")
        json.put("imageOverlayOpacity", imageOverlayOpacity.toDouble())
        json.put("keyBackgroundColor", keyBackgroundColor)
        json.put("keyBackgroundOpacity", keyBackgroundOpacity.toDouble())
        json.put("keyCornerRadius", keyCornerRadius.toDouble())
        json.put("keyTextColor", keyTextColor)
        json.put("accentColor", accentColor)
        json.put("keyPressRippleColor", keyPressRippleColor)
        json.put("isDark", isDark)
        return json
    }

    companion object {
        fun fromJsonObject(json: JSONObject): CustomThemeModel {
            val bgTypeStr = json.optString("backgroundType", BackgroundType.SOLID.name)
            val bgType = try {
                BackgroundType.valueOf(bgTypeStr)
            } catch (_: Exception) {
                BackgroundType.SOLID
            }

            return CustomThemeModel(
                id = json.optString("id", java.util.UUID.randomUUID().toString()),
                name = json.optString("name", "Custom Theme"),
                backgroundType = bgType,
                backgroundColor = json.optInt("backgroundColor", AndroidColor.parseColor("#0F172A")),
                gradientStartColor = json.optInt("gradientStartColor", AndroidColor.parseColor("#4C1D95")),
                gradientEndColor = json.optInt("gradientEndColor", AndroidColor.parseColor("#0F172A")),
                galleryImageUri = json.optString("galleryImageUri").takeIf { it.isNotEmpty() },
                imageOverlayOpacity = json.optDouble("imageOverlayOpacity", 0.45).toFloat(),
                keyBackgroundColor = json.optInt("keyBackgroundColor", AndroidColor.parseColor("#1E293B")),
                keyBackgroundOpacity = json.optDouble("keyBackgroundOpacity", 0.90).toFloat(),
                keyCornerRadius = json.optDouble("keyCornerRadius", 14.0).toFloat(),
                keyTextColor = json.optInt("keyTextColor", AndroidColor.parseColor("#FFFFFF")),
                accentColor = json.optInt("accentColor", AndroidColor.parseColor("#38BDF8")),
                keyPressRippleColor = json.optInt("keyPressRippleColor", AndroidColor.parseColor("#334155")),
                isDark = json.optBoolean("isDark", true)
            )
        }
    }
}

/**
 * Local JSON Repository for saving, retrieving, and deleting custom themes.
 * Stores user-created themes in SharedPreferences as a structured JSON array.
 */
class CustomThemeRepository(private val context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        const val PREFS_NAME = "uv_custom_themes_prefs"
        const val KEY_CUSTOM_THEMES = "saved_custom_themes_json"
    }

    /**
     * Retrieves all saved custom themes sorted from newest to oldest.
     */
    fun getAllCustomThemes(): List<CustomThemeModel> {
        val jsonStr = prefs.getString(KEY_CUSTOM_THEMES, null) ?: return emptyList()
        val list = mutableListOf<CustomThemeModel>()
        try {
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(CustomThemeModel.fromJsonObject(obj))
            }
        } catch (_: Exception) {}
        return list
    }

    /**
     * Saves a new or updated custom theme into persistent storage.
     */
    fun saveCustomTheme(theme: CustomThemeModel) {
        val current = getAllCustomThemes().toMutableList()
        val existingIndex = current.indexOfFirst { it.id == theme.id }
        if (existingIndex >= 0) {
            current[existingIndex] = theme
        } else {
            current.add(0, theme)
        }
        persistList(current)
    }

    /**
     * Deletes a custom theme by its ID.
     */
    fun deleteCustomTheme(themeId: String) {
        val current = getAllCustomThemes().filterNot { it.id == themeId }
        persistList(current)
    }

    /**
     * Gets a single custom theme by its ID.
     */
    fun getCustomTheme(id: String): CustomThemeModel? {
        return getAllCustomThemes().find { it.id == id }
    }

    private fun persistList(themes: List<CustomThemeModel>) {
        val jsonArray = JSONArray()
        themes.forEach { jsonArray.put(it.toJsonObject()) }
        prefs.edit().putString(KEY_CUSTOM_THEMES, jsonArray.toString()).apply()
    }
}
