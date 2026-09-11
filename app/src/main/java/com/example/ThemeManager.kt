package com.example

import android.content.Context
import android.content.SharedPreferences
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
    val isDark: Boolean = true
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

        const val KEY_KEYBOARD_HEIGHT_SCALE = "keyboard_height_scale"
        const val KEY_FONT_SIZE_SCALE = "font_size_scale"
        const val KEY_HAPTIC_FEEDBACK = "haptic_feedback"
        const val KEY_KEY_SOUND = "key_sound"
        const val KEY_PREDICTIONS = "word_predictions"
        const val KEY_DEFAULT_LAYOUT = "default_layout"
        const val KEY_ENABLED_LAYOUTS = "enabled_layouts"

        // Layout constants
        const val LAYOUT_JATIYA = "JATIYA"
        const val LAYOUT_AVRO = "AVRO"
        const val LAYOUT_BIJOY = "BIJOY"
        const val LAYOUT_PRABHAT = "PRABHAT"
        const val LAYOUT_ENGLISH = "ENGLISH"

        val PRESET_THEMES = listOf(
            KeyboardTheme(
                id = "classic_white",
                name = "Classic White",
                backgroundColor = Color.parseColor("#F1F5F9"),
                keyColor = Color.parseColor("#FFFFFF"),
                keyPressedColor = Color.parseColor("#E2E8F0"),
                actionKeyColor = Color.parseColor("#E2E8F0"),
                textColor = Color.parseColor("#0F172A"),
                accentColor = Color.parseColor("#0284C7"),
                sublabelColor = Color.parseColor("#64748B"),
                isDark = false
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
                isDark = true
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
                isDark = true
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
                isDark = true
            ),
            KeyboardTheme(
                id = "light_ocean",
                name = "Light Ocean",
                backgroundColor = Color.parseColor("#F0F9FF"),
                keyColor = Color.parseColor("#FFFFFF"),
                keyPressedColor = Color.parseColor("#E0F2FE"),
                actionKeyColor = Color.parseColor("#0284C7"),
                textColor = Color.parseColor("#0F172A"),
                accentColor = Color.parseColor("#0284C7"),
                sublabelColor = Color.parseColor("#64748B"),
                isDark = false
            ),
            KeyboardTheme(
                id = "amoled_black",
                name = "AMOLED Black",
                backgroundColor = Color.parseColor("#000000"),
                keyColor = Color.parseColor("#18181B"),
                keyPressedColor = Color.parseColor("#27272A"),
                actionKeyColor = Color.parseColor("#7C3AED"),
                textColor = Color.parseColor("#FAFAFA"),
                accentColor = Color.parseColor("#A855F7"),
                sublabelColor = Color.parseColor("#A1A1AA"),
                isDark = true
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
                isDark = true
            ),
            KeyboardTheme(
                id = "gradient",
                name = "Gradient",
                backgroundColor = Color.parseColor("#2E1065"),
                keyColor = Color.parseColor("#4C1D95"),
                keyPressedColor = Color.parseColor("#5B21B6"),
                actionKeyColor = Color.parseColor("#EC4899"),
                textColor = Color.parseColor("#FDF2F8"),
                accentColor = Color.parseColor("#F472B6"),
                sublabelColor = Color.parseColor("#FBCFE8"),
                isGradient = true,
                gradientStartColor = Color.parseColor("#4C1D95"),
                gradientEndColor = Color.parseColor("#0F172A"),
                isDark = true
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
                isGradient = false
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
