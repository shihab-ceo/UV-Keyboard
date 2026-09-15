package com.example

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri

data class KeyboardTheme(
    val id: String,
    val name: String,
    val backgroundColor: Int,
    val keyColor: Int,
    val keyPressedColor: Int,
    val actionKeyColor: Int,
    val textColor: Int,
    val accentColor: Int,
    val sublabelColor: Int,
    val isGradient: Boolean = false,
    val gradientStartColor: Int = 0,
    val gradientEndColor: Int = 0,
    val isDark: Boolean = true,
    val keyCornerRadius: Float = 14f,
    val keyBackgroundOpacity: Float = 1.0f,
    val keyPressRippleColor: Int = keyPressedColor
)

class ThemeManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "uv_keyboard_theme_prefs"

        const val KEY_THEME_ID = "theme_id"
        const val KEY_CUSTOM_BG_COLOR = "custom_bg_color"
        const val KEY_USE_CUSTOM_COLOR = "use_custom_color"
        const val KEY_BG_IMAGE_URI = "bg_image_uri"
        const val KEY_BG_IMAGE_OPACITY = "bg_image_opacity"
        const val KEY_ACTIVE_CUSTOM_THEME_ID = "active_custom_theme_id"

        const val KEY_KEYBOARD_HEIGHT_SCALE = "keyboard_height_scale"
        const val KEY_FONT_SIZE_SCALE = "font_size_scale"
        const val KEY_HAPTIC_FEEDBACK = "haptic_feedback"
        const val KEY_KEY_SOUND = "key_sound"
        const val KEY_PREDICTIONS = "word_predictions"
        const val KEY_AUTOCORRECT = "autocorrect_enabled"
        const val KEY_PERSONAL_LEARNING = "personal_learning_enabled"
        const val KEY_AUTOCORRECT_AGGRESSIVENESS = "autocorrect_aggressiveness"
        const val KEY_DEFAULT_LAYOUT = "default_layout"
        const val KEY_ENABLED_LAYOUTS = "enabled_layouts"

        // Layout constants
        const val LAYOUT_JATIYA = "JATIYA"
        const val LAYOUT_AVRO = "AVRO"
        const val LAYOUT_BIJOY = "BIJOY"
        const val LAYOUT_PRABHAT = "PRABHAT"
        const val LAYOUT_ENGLISH = "ENGLISH"

        val PRESET_THEMES = listOf(
            // --- 1. CLASSIC THEMES ---
            KeyboardTheme(
                id = "material_light",
                name = "Material Light",
                backgroundColor = Color.parseColor("#F8F9FA"),
                keyColor = Color.parseColor("#FFFFFF"),
                keyPressedColor = Color.parseColor("#E8EAED"),
                actionKeyColor = Color.parseColor("#1A73E8"),
                textColor = Color.parseColor("#202124"),
                accentColor = Color.parseColor("#1A73E8"),
                sublabelColor = Color.parseColor("#5F6368"),
                isDark = false,
                keyCornerRadius = 12f,
                keyBackgroundOpacity = 1.0f,
                keyPressRippleColor = Color.parseColor("#E8EAED")
            ),
            KeyboardTheme(
                id = "amoled_black",
                name = "Dark AMOLED",
                backgroundColor = Color.parseColor("#000000"),
                keyColor = Color.parseColor("#121212"),
                keyPressedColor = Color.parseColor("#242424"),
                actionKeyColor = Color.parseColor("#7C3AED"),
                textColor = Color.parseColor("#FFFFFF"),
                accentColor = Color.parseColor("#A855F7"),
                sublabelColor = Color.parseColor("#9CA3AF"),
                isDark = true,
                keyCornerRadius = 14f,
                keyBackgroundOpacity = 1.0f,
                keyPressRippleColor = Color.parseColor("#27272A")
            ),
            KeyboardTheme(
                id = "midnight_blue",
                name = "Midnight Blue",
                backgroundColor = Color.parseColor("#0A1128"),
                keyColor = Color.parseColor("#1C2541"),
                keyPressedColor = Color.parseColor("#2E3C66"),
                actionKeyColor = Color.parseColor("#0077B6"),
                textColor = Color.parseColor("#F0F4F8"),
                accentColor = Color.parseColor("#48CAE4"),
                sublabelColor = Color.parseColor("#829AB1"),
                isDark = true,
                keyCornerRadius = 14f,
                keyBackgroundOpacity = 0.95f,
                keyPressRippleColor = Color.parseColor("#2E3C66")
            ),
            KeyboardTheme(
                id = "dark_night",
                name = "Dark Night",
                backgroundColor = Color.parseColor("#0F172A"),
                keyColor = Color.parseColor("#1E293B"),
                keyPressedColor = Color.parseColor("#334155"),
                actionKeyColor = Color.parseColor("#2563EB"),
                textColor = Color.parseColor("#F8FAFC"),
                accentColor = Color.parseColor("#38BDF8"),
                sublabelColor = Color.parseColor("#94A3B8"),
                isDark = true,
                keyCornerRadius = 14f,
                keyBackgroundOpacity = 1.0f,
                keyPressRippleColor = Color.parseColor("#334155")
            ),

            // --- 2. GRADIENT THEMES ---
            KeyboardTheme(
                id = "sunset_gradient",
                name = "Sunset Pink/Orange",
                backgroundColor = Color.parseColor("#2A0845"),
                keyColor = Color.parseColor("#3D105A"),
                keyPressedColor = Color.parseColor("#581880"),
                actionKeyColor = Color.parseColor("#F97316"),
                textColor = Color.parseColor("#FFF7ED"),
                accentColor = Color.parseColor("#FB923C"),
                sublabelColor = Color.parseColor("#FDBA74"),
                isGradient = true,
                gradientStartColor = Color.parseColor("#EC4899"),
                gradientEndColor = Color.parseColor("#EA580C"),
                isDark = true,
                keyCornerRadius = 16f,
                keyBackgroundOpacity = 0.85f,
                keyPressRippleColor = Color.parseColor("#581880")
            ),
            KeyboardTheme(
                id = "deep_space_purple",
                name = "Deep Space Purple",
                backgroundColor = Color.parseColor("#090514"),
                keyColor = Color.parseColor("#1E1035"),
                keyPressedColor = Color.parseColor("#321B59"),
                actionKeyColor = Color.parseColor("#9333EA"),
                textColor = Color.parseColor("#FAF5FF"),
                accentColor = Color.parseColor("#C084FC"),
                sublabelColor = Color.parseColor("#D8B4FE"),
                isGradient = true,
                gradientStartColor = Color.parseColor("#581C87"),
                gradientEndColor = Color.parseColor("#0F0A1C"),
                isDark = true,
                keyCornerRadius = 14f,
                keyBackgroundOpacity = 0.88f,
                keyPressRippleColor = Color.parseColor("#321B59")
            ),
            KeyboardTheme(
                id = "ocean_gradient",
                name = "Ocean Cyan/Blue",
                backgroundColor = Color.parseColor("#082F49"),
                keyColor = Color.parseColor("#0E4B75"),
                keyPressedColor = Color.parseColor("#156094"),
                actionKeyColor = Color.parseColor("#0284C7"),
                textColor = Color.parseColor("#F0F9FF"),
                accentColor = Color.parseColor("#38BDF8"),
                sublabelColor = Color.parseColor("#7DD3FC"),
                isGradient = true,
                gradientStartColor = Color.parseColor("#0284C7"),
                gradientEndColor = Color.parseColor("#041F38"),
                isDark = true,
                keyCornerRadius = 14f,
                keyBackgroundOpacity = 0.85f,
                keyPressRippleColor = Color.parseColor("#156094")
            ),
            KeyboardTheme(
                id = "forest_emerald",
                name = "Forest Emerald",
                backgroundColor = Color.parseColor("#022C22"),
                keyColor = Color.parseColor("#064E3B"),
                keyPressedColor = Color.parseColor("#065F46"),
                actionKeyColor = Color.parseColor("#059669"),
                textColor = Color.parseColor("#ECFDF5"),
                accentColor = Color.parseColor("#34D399"),
                sublabelColor = Color.parseColor("#A7F3D0"),
                isGradient = true,
                gradientStartColor = Color.parseColor("#047857"),
                gradientEndColor = Color.parseColor("#02241C"),
                isDark = true,
                keyCornerRadius = 14f,
                keyBackgroundOpacity = 0.88f,
                keyPressRippleColor = Color.parseColor("#065F46")
            ),

            // --- 3. MINIMAL & NEON THEMES ---
            KeyboardTheme(
                id = "high_contrast_dark",
                name = "High Contrast Dark",
                backgroundColor = Color.parseColor("#000000"),
                keyColor = Color.parseColor("#262626"),
                keyPressedColor = Color.parseColor("#404040"),
                actionKeyColor = Color.parseColor("#CA8A04"),
                textColor = Color.parseColor("#FFFFFF"),
                accentColor = Color.parseColor("#FACC15"),
                sublabelColor = Color.parseColor("#FDE047"),
                isDark = true,
                keyCornerRadius = 8f,
                keyBackgroundOpacity = 1.0f,
                keyPressRippleColor = Color.parseColor("#404040")
            ),
            KeyboardTheme(
                id = "neon_purple",
                name = "Neon Purple",
                backgroundColor = Color.parseColor("#0B0813"),
                keyColor = Color.parseColor("#19122B"),
                keyPressedColor = Color.parseColor("#291C47"),
                actionKeyColor = Color.parseColor("#C026D3"),
                textColor = Color.parseColor("#FFFFFF"),
                accentColor = Color.parseColor("#E879F9"),
                sublabelColor = Color.parseColor("#22D3EE"),
                isDark = true,
                keyCornerRadius = 18f,
                keyBackgroundOpacity = 0.90f,
                keyPressRippleColor = Color.parseColor("#291C47")
            ),
            KeyboardTheme(
                id = "pastel_mint",
                name = "Pastel Mint",
                backgroundColor = Color.parseColor("#E6F4EA"),
                keyColor = Color.parseColor("#FFFFFF"),
                keyPressedColor = Color.parseColor("#CEEAD6"),
                actionKeyColor = Color.parseColor("#0D9488"),
                textColor = Color.parseColor("#134E4A"),
                accentColor = Color.parseColor("#0D9488"),
                sublabelColor = Color.parseColor("#047857"),
                isDark = false,
                keyCornerRadius = 16f,
                keyBackgroundOpacity = 1.0f,
                keyPressRippleColor = Color.parseColor("#CEEAD6")
            ),

            // --- 4. COMPATIBILITY ALIASES & ADDITIONAL FAVORITES ---
            KeyboardTheme(
                id = "classic_white",
                name = "Classic White",
                backgroundColor = Color.parseColor("#F1F5F9"),
                keyColor = Color.parseColor("#FFFFFF"),
                keyPressedColor = Color.parseColor("#E2E8F0"),
                actionKeyColor = Color.parseColor("#0284C7"),
                textColor = Color.parseColor("#0F172A"),
                accentColor = Color.parseColor("#0284C7"),
                sublabelColor = Color.parseColor("#64748B"),
                isDark = false,
                keyCornerRadius = 12f
            ),
            KeyboardTheme(
                id = "greenery",
                name = "Greenery",
                backgroundColor = Color.parseColor("#064E3B"),
                keyColor = Color.parseColor("#065F46"),
                keyPressedColor = Color.parseColor("#047857"),
                actionKeyColor = Color.parseColor("#10B981"),
                textColor = Color.parseColor("#ECFDF5"),
                accentColor = Color.parseColor("#34D399"),
                sublabelColor = Color.parseColor("#A7F3D0"),
                isDark = true,
                keyCornerRadius = 14f
            ),
            KeyboardTheme(
                id = "aqua_sky",
                name = "Aqua Sky",
                backgroundColor = Color.parseColor("#083344"),
                keyColor = Color.parseColor("#0E7490"),
                keyPressedColor = Color.parseColor("#155E75"),
                actionKeyColor = Color.parseColor("#06B6D4"),
                textColor = Color.parseColor("#E0F2FE"),
                accentColor = Color.parseColor("#38BDF8"),
                sublabelColor = Color.parseColor("#7DD3FC"),
                isDark = true,
                keyCornerRadius = 14f
            ),
            KeyboardTheme(
                id = "slate",
                name = "Slate",
                backgroundColor = Color.parseColor("#334155"),
                keyColor = Color.parseColor("#475569"),
                keyPressedColor = Color.parseColor("#64748B"),
                actionKeyColor = Color.parseColor("#0EA5E9"),
                textColor = Color.parseColor("#F1F5F9"),
                accentColor = Color.parseColor("#38BDF8"),
                sublabelColor = Color.parseColor("#CBD5E1"),
                isDark = true,
                keyCornerRadius = 14f
            )
        )
    }

    var selectedThemeId: String
        get() = prefs.getString(KEY_THEME_ID, "dark_night") ?: "dark_night"
        set(value) = prefs.edit().putString(KEY_THEME_ID, value).apply()

    var useCustomColor: Boolean
        get() = prefs.getBoolean(KEY_USE_CUSTOM_COLOR, false)
        set(value) = prefs.edit().putBoolean(KEY_USE_CUSTOM_COLOR, value).apply()

    var customBgColor: Int
        get() = prefs.getInt(KEY_CUSTOM_BG_COLOR, Color.parseColor("#1E1B4B"))
        set(value) = prefs.edit().putInt(KEY_CUSTOM_BG_COLOR, value).apply()

    var bgImageUriString: String?
        get() = prefs.getString(KEY_BG_IMAGE_URI, null)
        set(value) = prefs.edit().putString(KEY_BG_IMAGE_URI, value).apply()

    var bgImageOpacity: Float
        get() = prefs.getFloat(KEY_BG_IMAGE_OPACITY, 0.45f)
        set(value) = prefs.edit().putFloat(KEY_BG_IMAGE_OPACITY, value).apply()

    var activeCustomThemeId: String?
        get() = prefs.getString(KEY_ACTIVE_CUSTOM_THEME_ID, null)
        set(value) = prefs.edit().putString(KEY_ACTIVE_CUSTOM_THEME_ID, value).apply()

    val customThemeRepository: CustomThemeRepository = CustomThemeRepository(context)

    fun getSavedCustomThemes(): List<CustomThemeModel> {
        return customThemeRepository.getAllCustomThemes()
    }

    fun saveCustomTheme(customTheme: CustomThemeModel) {
        customThemeRepository.saveCustomTheme(customTheme)
    }

    fun deleteCustomTheme(themeId: String) {
        customThemeRepository.deleteCustomTheme(themeId)
        if (activeCustomThemeId == themeId) {
            applyPresetTheme("dark_night")
        }
    }

    fun applyCustomTheme(customTheme: CustomThemeModel) {
        activeCustomThemeId = customTheme.id
        useCustomColor = false
        selectedThemeId = "custom_${customTheme.id}"
        if (customTheme.backgroundType == BackgroundType.IMAGE && !customTheme.galleryImageUri.isNullOrEmpty()) {
            bgImageUriString = customTheme.galleryImageUri
            bgImageOpacity = customTheme.imageOverlayOpacity
        } else {
            bgImageUriString = null
        }
    }

    fun applyPresetTheme(presetThemeId: String) {
        activeCustomThemeId = null
        useCustomColor = false
        selectedThemeId = presetThemeId
        bgImageUriString = null
    }

    /**
     * Saves a user-cropped background image to private internal storage, updating the URI and opacity.
     * Storing internally prevents permission revocation across app restarts or OS updates.
     */
    fun saveCustomCroppedBackground(context: Context, bitmap: Bitmap, opacity: Float): Uri? {
        return try {
            val file = java.io.File(context.filesDir, "keyboard_custom_bg.jpg")
            java.io.FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            val uri = Uri.fromFile(file)
            bgImageUriString = uri.toString()
            bgImageOpacity = opacity.coerceIn(0f, 0.95f)
            uri
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Clears any active custom background image and restores default theme rendering.
     */
    fun clearCustomBackground(context: Context) {
        bgImageUriString = null
        try {
            val file = java.io.File(context.filesDir, "keyboard_custom_bg.jpg")
            if (file.exists()) {
                file.delete()
            }
        } catch (_: Exception) {}
    }

    var keyboardHeightScale: Float
        get() = prefs.getFloat(KEY_KEYBOARD_HEIGHT_SCALE, 1.30f)
        set(value) = prefs.edit().putFloat(KEY_KEYBOARD_HEIGHT_SCALE, value).apply()

    var fontSizeScale: Float
        get() = prefs.getFloat(KEY_FONT_SIZE_SCALE, 1.40f)
        set(value) = prefs.edit().putFloat(KEY_FONT_SIZE_SCALE, value).apply()

    var hapticFeedbackEnabled: Boolean
        get() = prefs.getBoolean(KEY_HAPTIC_FEEDBACK, true)
        set(value) = prefs.edit().putBoolean(KEY_HAPTIC_FEEDBACK, value).apply()

    var keySoundEnabled: Boolean
        get() = prefs.getBoolean(KEY_KEY_SOUND, false)
        set(value) = prefs.edit().putBoolean(KEY_KEY_SOUND, value).apply()

    var wordPredictionsEnabled: Boolean
        get() = prefs.getBoolean(KEY_PREDICTIONS, true)
        set(value) = prefs.edit().putBoolean(KEY_PREDICTIONS, value).apply()

    var autocorrectEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTOCORRECT, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTOCORRECT, value).apply()

    var personalLearningEnabled: Boolean
        get() = prefs.getBoolean(KEY_PERSONAL_LEARNING, true)
        set(value) = prefs.edit().putBoolean(KEY_PERSONAL_LEARNING, value).apply()

    var autocorrectAggressiveness: String
        get() = prefs.getString(KEY_AUTOCORRECT_AGGRESSIVENESS, "Balanced") ?: "Balanced"
        set(value) = prefs.edit().putString(KEY_AUTOCORRECT_AGGRESSIVENESS, value).apply()

    var defaultStartupLayout: String
        get() = prefs.getString(KEY_DEFAULT_LAYOUT, LAYOUT_JATIYA) ?: LAYOUT_JATIYA
        set(value) = prefs.edit().putString(KEY_DEFAULT_LAYOUT, value).apply()

    var enabledLayouts: Set<String>
        get() {
            val saved = prefs.getStringSet(KEY_ENABLED_LAYOUTS, null)
            return if (!saved.isNullOrEmpty()) saved else setOf(
                LAYOUT_JATIYA,
                LAYOUT_AVRO,
                LAYOUT_BIJOY,
                LAYOUT_PRABHAT,
                LAYOUT_ENGLISH
            )
        }
        set(value) = prefs.edit().putStringSet(KEY_ENABLED_LAYOUTS, value).apply()

    fun isLayoutEnabled(layout: String): Boolean = enabledLayouts.contains(layout)

    fun setLayoutEnabled(layout: String, enabled: Boolean) {
        val current = enabledLayouts.toMutableSet()
        if (enabled) {
            current.add(layout)
        } else {
            // Keep at least one layout active to prevent empty state
            if (current.size > 1) {
                current.remove(layout)
            }
        }
        enabledLayouts = current
    }

    fun getOrderedEnabledLayouts(): List<String> {
        val allOrdered = listOf(
            LAYOUT_JATIYA,
            LAYOUT_AVRO,
            LAYOUT_BIJOY,
            LAYOUT_PRABHAT,
            LAYOUT_ENGLISH
        )
        val enabled = enabledLayouts
        val list = allOrdered.filter { enabled.contains(it) }
        return if (list.isNotEmpty()) list else listOf(LAYOUT_JATIYA, LAYOUT_ENGLISH)
    }

    fun getCurrentTheme(): KeyboardTheme {
        val customId = activeCustomThemeId
        if (!customId.isNullOrEmpty()) {
            val customTheme = CustomThemeRepository(context).getCustomTheme(customId)
            if (customTheme != null) {
                return customTheme.toKeyboardTheme()
            }
        }

        val baseTheme = PRESET_THEMES.find { it.id == selectedThemeId } ?: PRESET_THEMES[0]
        if (useCustomColor) {
            val bg = customBgColor
            // determine brightness
            val luminance = (0.299 * Color.red(bg) + 0.587 * Color.green(bg) + 0.114 * Color.blue(bg)) / 255.0
            val isDark = luminance < 0.5
            val keyColor = if (isDark) {
                blendColors(bg, Color.WHITE, 0.15f)
            } else {
                blendColors(bg, Color.BLACK, 0.08f)
            }
            val keyPressedColor = if (isDark) {
                blendColors(bg, Color.WHITE, 0.30f)
            } else {
                blendColors(bg, Color.BLACK, 0.20f)
            }
            val textColor = if (isDark) Color.WHITE else Color.parseColor("#0F172A")
            val sublabelColor = if (isDark) Color.parseColor("#CBD5E1") else Color.parseColor("#475569")
            val accentColor = if (isDark) Color.parseColor("#38BDF8") else Color.parseColor("#0284C7")

            return baseTheme.copy(
                name = "Custom Color",
                backgroundColor = bg,
                keyColor = keyColor,
                keyPressedColor = keyPressedColor,
                textColor = textColor,
                sublabelColor = sublabelColor,
                accentColor = accentColor,
                isDark = isDark,
                isGradient = false,
                keyCornerRadius = 14f,
                keyBackgroundOpacity = 1.0f,
                keyPressRippleColor = keyPressedColor
            )
        }
        return baseTheme
    }

    private fun blendColors(color1: Int, color2: Int, ratio: Float): Int {
        val inverseRatio = 1f - ratio
        val a = (Color.alpha(color1) * inverseRatio + Color.alpha(color2) * ratio).toInt()
        val r = (Color.red(color1) * inverseRatio + Color.red(color2) * ratio).toInt()
        val g = (Color.green(color1) * inverseRatio + Color.green(color2) * ratio).toInt()
        val b = (Color.blue(color1) * inverseRatio + Color.blue(color2) * ratio).toInt()
        return Color.argb(a, r, g, b)
    }
}
