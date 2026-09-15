package com.example

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage

/**
 * Dialog component for building and customizing personalized keyboard themes.
 */
@Composable
fun CustomThemeBuilderDialog(
    initialTheme: CustomThemeModel? = null,
    onDismiss: () -> Unit,
    onSaveAndApply: (CustomThemeModel) -> Unit
) {
    val context = LocalContext.current

    var themeName by remember { mutableStateOf(initialTheme?.name ?: "আমার থিম") }
    var backgroundType by remember { mutableStateOf(initialTheme?.backgroundType ?: BackgroundType.SOLID) }
    var backgroundColor by remember { mutableIntStateOf(initialTheme?.backgroundColor ?: AndroidColor.parseColor("#0F172A")) }
    var gradientStartColor by remember { mutableIntStateOf(initialTheme?.gradientStartColor ?: AndroidColor.parseColor("#4C1D95")) }
    var gradientEndColor by remember { mutableIntStateOf(initialTheme?.gradientEndColor ?: AndroidColor.parseColor("#0F172A")) }
    var galleryImageUri by remember { mutableStateOf(initialTheme?.galleryImageUri) }
    var imageOverlayOpacity by remember { mutableFloatStateOf(initialTheme?.imageOverlayOpacity ?: 0.45f) }

    var keyBackgroundColor by remember { mutableIntStateOf(initialTheme?.keyBackgroundColor ?: AndroidColor.parseColor("#1E293B")) }
    var keyBackgroundOpacity by remember { mutableFloatStateOf(initialTheme?.keyBackgroundOpacity ?: 0.90f) }
    var keyCornerRadius by remember { mutableFloatStateOf(initialTheme?.keyCornerRadius ?: 14f) }

    var keyTextColor by remember { mutableIntStateOf(initialTheme?.keyTextColor ?: AndroidColor.parseColor("#FFFFFF")) }
    var accentColor by remember { mutableIntStateOf(initialTheme?.accentColor ?: AndroidColor.parseColor("#38BDF8")) }

    var selectedTabIndex by remember { mutableIntStateOf(0) }

    // Gallery Picker launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}
            galleryImageUri = uri.toString()
            backgroundType = BackgroundType.IMAGE
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxSize(0.92f),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF0F172A),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (initialTheme != null) "✏️ থিম এডিট করুন" else "🎨 কাস্টম থিম বিল্ডার",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Design your own custom keyboard look",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // LIVE INTERACTIVE KEYBOARD PREVIEW
                LiveKeyboardPreviewCard(
                    backgroundType = backgroundType,
                    backgroundColor = backgroundColor,
                    gradientStartColor = gradientStartColor,
                    gradientEndColor = gradientEndColor,
                    galleryImageUri = galleryImageUri,
                    imageOverlayOpacity = imageOverlayOpacity,
                    keyBackgroundColor = keyBackgroundColor,
                    keyBackgroundOpacity = keyBackgroundOpacity,
                    keyCornerRadius = keyCornerRadius,
                    keyTextColor = keyTextColor,
                    accentColor = accentColor
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Tab Selector: Background | Keys | Text & Colors
                val tabs = listOf("ব্যাকগ্রাউন্ড", "বোতাম", "কালার ও টেক্সট")
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = Color(0xFF1E293B),
                    contentColor = Color(0xFF38BDF8),
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            color = Color(0xFF38BDF8)
                        )
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = {
                                Text(
                                    text = title,
                                    fontSize = 13.sp,
                                    fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTabIndex == index) Color(0xFF38BDF8) else Color(0xFF94A3B8)
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Scrollable Controls Area
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    when (selectedTabIndex) {
                        0 -> {
                            // TAB 1: BACKGROUND CUSTOMIZATION
                            Text(
                                text = "ব্যাকগ্রাউন্ড টাইপ (Background Type):",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                BackgroundTypeButton(
                                    title = "সলিড কালার",
                                    selected = backgroundType == BackgroundType.SOLID,
                                    modifier = Modifier.weight(1f)
                                ) { backgroundType = BackgroundType.SOLID }

                                BackgroundTypeButton(
                                    title = "গ্রেডিয়েন্ট",
                                    selected = backgroundType == BackgroundType.GRADIENT,
                                    modifier = Modifier.weight(1f)
                                ) { backgroundType = BackgroundType.GRADIENT }

                                BackgroundTypeButton(
                                    title = "গ্যালারি ছবি",
                                    selected = backgroundType == BackgroundType.IMAGE,
                                    modifier = Modifier.weight(1f)
                                ) { backgroundType = BackgroundType.IMAGE }
                            }

                            when (backgroundType) {
                                BackgroundType.SOLID -> {
                                    Text(
                                        text = "ব্যাকগ্রাউন্ড কালার নির্বাচন করুন:",
                                        color = Color(0xFFCBD5E1),
                                        fontSize = 12.sp
                                    )
                                    val solidColors = listOf(
                                        0xFF0F172A.toInt(), 0xFF1E1B4B.toInt(), 0xFF022C22.toInt(),
                                        0xFF000000.toInt(), 0xFF312E81.toInt(), 0xFF1C1917.toInt(),
                                        0xFF831843.toInt(), 0xFF064E3B.toInt(), 0xFF082F49.toInt(),
                                        0xFFF8F9FA.toInt(), 0xFFE6F4EA.toInt(), 0xFFFFFBEB.toInt()
                                    )
                                    ColorPalettePicker(
                                        colors = solidColors,
                                        selectedColor = backgroundColor,
                                        onColorSelected = { backgroundColor = it }
                                    )
                                }
                                BackgroundType.GRADIENT -> {
                                    Text(
                                        text = "গ্রেডিয়েন্ট শুরুর কালার (Start Color):",
                                        color = Color(0xFFCBD5E1),
                                        fontSize = 12.sp
                                    )
                                    val startColors = listOf(
                                        0xFFEC4899.toInt(), 0xFF4C1D95.toInt(), 0xFF0284C7.toInt(),
                                        0xFF059669.toInt(), 0xFFDC2626.toInt(), 0xFF7C3AED.toInt(),
                                        0xFFD97706.toInt(), 0xFF0D9488.toInt()
                                    )
                                    ColorPalettePicker(
                                        colors = startColors,
                                        selectedColor = gradientStartColor,
                                        onColorSelected = { gradientStartColor = it }
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "গ্রেডিয়েন্ট শেষের কালার (End Color):",
                                        color = Color(0xFFCBD5E1),
                                        fontSize = 12.sp
                                    )
                                    val endColors = listOf(
                                        0xFF0F172A.toInt(), 0xFF1E1B4B.toInt(), 0xFF022C22.toInt(),
                                        0xFF000000.toInt(), 0xFF1E293B.toInt(), 0xFF041F38.toInt(),
                                        0xFFEA580C.toInt(), 0xFF4A044E.toInt()
                                    )
                                    ColorPalettePicker(
                                        colors = endColors,
                                        selectedColor = gradientEndColor,
                                        onColorSelected = { gradientEndColor = it }
                                    )
                                }
                                BackgroundType.IMAGE -> {
                                    Button(
                                        onClick = { galleryLauncher.launch(arrayOf("image/*")) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("গ্যালারি থেকে ছবি সিলেক্ট করুন")
                                    }

                                    if (galleryImageUri != null) {
                                        Text(
                                            text = "ছবির উপর ডার্ক ওভারলে (Overlay Opacity): ${(imageOverlayOpacity * 100).toInt()}%",
                                            color = Color(0xFFCBD5E1),
                                            fontSize = 12.sp
                                        )
                                        Slider(
                                            value = imageOverlayOpacity,
                                            onValueChange = { imageOverlayOpacity = it },
                                            valueRange = 0.05f..0.90f,
                                            colors = SliderDefaults.colors(
                                                thumbColor = Color(0xFF38BDF8),
                                                activeTrackColor = Color(0xFF38BDF8),
                                                inactiveTrackColor = Color(0xFF334155)
                                            )
                                        )
                                    }
                                }
                            }
                        }
                        1 -> {
                            // TAB 2: KEY SHAPE, OPACITY & CORNERS
                            Text(
                                text = "কী-বোর্ড বোতামের কোণা (Corner Radius): ${keyCornerRadius.toInt()} dp",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Slider(
                                value = keyCornerRadius,
                                onValueChange = { keyCornerRadius = it },
                                valueRange = 0f..24f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF38BDF8),
                                    activeTrackColor = Color(0xFF38BDF8),
                                    inactiveTrackColor = Color(0xFF334155)
                                )
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("তীক্ষ্ণ (Sharp 0dp)", color = Color(0xFF64748B), fontSize = 11.sp)
                                Text("স্ট্যান্ডার্ড (14dp)", color = Color(0xFF64748B), fontSize = 11.sp)
                                Text("গোলাকার (Pill 24dp)", color = Color(0xFF64748B), fontSize = 11.sp)
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "বোতামের স্বচ্ছতা (Key Opacity): ${(keyBackgroundOpacity * 100).toInt()}%",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Slider(
                                value = keyBackgroundOpacity,
                                onValueChange = { keyBackgroundOpacity = it },
                                valueRange = 0.10f..1.0f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF38BDF8),
                                    activeTrackColor = Color(0xFF38BDF8),
                                    inactiveTrackColor = Color(0xFF334155)
                                )
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "বোতামের কালার (Key Color):",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            val keyColors = listOf(
                                0xFF1E293B.toInt(), 0xFF121212.toInt(), 0xFF27272A.toInt(),
                                0xFF1E1035.toInt(), 0xFF0E4B75.toInt(), 0xFF064E3B.toInt(),
                                0xFF3D105A.toInt(), 0xFFFFFFFF.toInt(), 0xFFF1F5F9.toInt()
                            )
                            ColorPalettePicker(
                                colors = keyColors,
                                selectedColor = keyBackgroundColor,
                                onColorSelected = { keyBackgroundColor = it }
                            )
                        }
                        2 -> {
                            // TAB 3: TEXT COLOR & ACCENTS
                            Text(
                                text = "অক্ষরের কালার (Key Text Color):",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            val textColors = listOf(
                                0xFFFFFFFF.toInt(), 0xFFF8FAFC.toInt(), 0xFFFACC15.toInt(),
                                0xFF38BDF8.toInt(), 0xFF34D399.toInt(), 0xFFF472B6.toInt(),
                                0xFF0F172A.toInt(), 0xFF1E293B.toInt()
                            )
                            ColorPalettePicker(
                                colors = textColors,
                                selectedColor = keyTextColor,
                                onColorSelected = { keyTextColor = it }
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "প্রাইমারি অ্যাকসেন্ট কালার (Enter / Shift):",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            val accentColors = listOf(
                                0xFF38BDF8.toInt(), 0xFF2563EB.toInt(), 0xFF10B981.toInt(),
                                0xFFEC4899.toInt(), 0xFFF97316.toInt(), 0xFFA855F7.toInt(),
                                0xFFFACC15.toInt(), 0xFF06B6D4.toInt(), 0xFFEF4444.toInt()
                            )
                            ColorPalettePicker(
                                colors = accentColors,
                                selectedColor = accentColor,
                                onColorSelected = { accentColor = it }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Theme Name Input
                    OutlinedTextField(
                        value = themeName,
                        onValueChange = { themeName = it },
                        label = { Text("থিমের নাম (Theme Name)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF475569),
                            focusedLabelColor = Color(0xFF38BDF8),
                            unfocusedLabelColor = Color(0xFF94A3B8)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("বাতিল", color = Color(0xFF94A3B8))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = {
                            val finalName = if (themeName.isBlank()) "আমার থিম" else themeName.trim()
                            val createdTheme = CustomThemeModel(
                                id = initialTheme?.id ?: java.util.UUID.randomUUID().toString(),
                                name = finalName,
                                backgroundType = backgroundType,
                                backgroundColor = backgroundColor,
                                gradientStartColor = gradientStartColor,
                                gradientEndColor = gradientEndColor,
                                galleryImageUri = galleryImageUri,
                                imageOverlayOpacity = imageOverlayOpacity,
                                keyBackgroundColor = keyBackgroundColor,
                                keyBackgroundOpacity = keyBackgroundOpacity,
                                keyCornerRadius = keyCornerRadius,
                                keyTextColor = keyTextColor,
                                accentColor = accentColor,
                                keyPressRippleColor = AndroidColor.parseColor("#334155"),
                                isDark = true
                            )
                            onSaveAndApply(createdTheme)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("সংরক্ষণ ও প্রয়োগ করুন")
                    }
                }
            }
        }
    }
}

/**
 * Visual live keyboard preview card showing simulated keys with the current settings.
 */
@Composable
fun LiveKeyboardPreviewCard(
    backgroundType: BackgroundType,
    backgroundColor: Int,
    gradientStartColor: Int,
    gradientEndColor: Int,
    galleryImageUri: String?,
    imageOverlayOpacity: Float,
    keyBackgroundColor: Int,
    keyBackgroundOpacity: Float,
    keyCornerRadius: Float,
    keyTextColor: Int,
    accentColor: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background rendering
            when (backgroundType) {
                BackgroundType.SOLID -> {
                    Box(modifier = Modifier.fillMaxSize().background(Color(backgroundColor)))
                }
                BackgroundType.GRADIENT -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(gradientStartColor), Color(gradientEndColor))
                                )
                            )
                    )
                }
                BackgroundType.IMAGE -> {
                    if (!galleryImageUri.isNullOrEmpty()) {
                        AsyncImage(
                            model = galleryImageUri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0F172A)))
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = imageOverlayOpacity))
                    )
                }
            }

            // Keyboard Preview Keys Layout
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Row 1: Characters
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    listOf("ক", "খ", "গ", "ঘ", "ঙ", "চ", "ছ").forEach { char ->
                        PreviewKey(
                            label = char,
                            keyBgColor = Color(keyBackgroundColor).copy(alpha = keyBackgroundOpacity),
                            textColor = Color(keyTextColor),
                            cornerRadius = keyCornerRadius,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Row 2: Vowel Signs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    listOf("া", "ি", "ী", "ু", "ূ", "ৃ", "ে").forEach { char ->
                        PreviewKey(
                            label = char,
                            keyBgColor = Color(keyBackgroundColor).copy(alpha = keyBackgroundOpacity),
                            textColor = Color(keyTextColor),
                            cornerRadius = keyCornerRadius,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Row 3: Bottom action row (Shift, Space, Enter)
                val shiftIconColor = if (Color(accentColor).luminance() < 0.55f) Color.White else Color(0xFF0F172A)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    PreviewKey(
                        label = "⬆",
                        keyBgColor = Color(accentColor),
                        textColor = shiftIconColor,
                        cornerRadius = keyCornerRadius,
                        modifier = Modifier.width(36.dp)
                    )

                    PreviewKey(
                        label = "স্পেসবার (Space)",
                        keyBgColor = Color(keyBackgroundColor).copy(alpha = keyBackgroundOpacity),
                        textColor = Color(keyTextColor),
                        cornerRadius = keyCornerRadius,
                        modifier = Modifier.weight(1f)
                    )

                    PreviewKey(
                        label = "↵",
                        keyBgColor = Color(accentColor),
                        textColor = Color.White,
                        cornerRadius = keyCornerRadius,
                        modifier = Modifier.width(36.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PreviewKey(
    label: String,
    keyBgColor: Color,
    textColor: Color,
    cornerRadius: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(28.dp)
            .clip(RoundedCornerShape(cornerRadius.dp))
            .background(keyBgColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
fun BackgroundTypeButton(
    title: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) Color(0xFF0284C7) else Color(0xFF1E293B))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = if (selected) Color.White else Color(0xFF94A3B8),
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun ColorPalettePicker(
    colors: List<Int>,
    selectedColor: Int,
    onColorSelected: (Int) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        items(colors) { colorInt ->
            val isSelected = colorInt == selectedColor
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(colorInt))
                    .border(
                        width = if (isSelected) 3.dp else 1.dp,
                        color = if (isSelected) Color.White else Color(0xFF475569),
                        shape = CircleShape
                    )
                    .clickable { onColorSelected(colorInt) },
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = if (Color(colorInt).luminance() > 0.5f) Color.Black else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

fun Color.luminance(): Float {
    return (0.299f * red + 0.587f * green + 0.114f * blue)
}

/**
 * Saved Custom Themes Section Composable to be embedded into the Themes tab in MainActivity.
 */
@Composable
fun SavedCustomThemesSection(
    themeManager: ThemeManager,
    onOpenCreateDialog: () -> Unit,
    onEditTheme: (CustomThemeModel) -> Unit
) {
    val context = LocalContext.current
    val repo = remember { CustomThemeRepository(context) }
    var customThemes by remember { mutableStateOf(repo.getAllCustomThemes()) }
    var activeCustomId by remember { mutableStateOf(themeManager.activeCustomThemeId) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Palette, contentDescription = null, tint = Color(0xFF38BDF8))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "২. আমার তৈরি কাস্টম থিমস (Saved Custom Themes)",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }

            Button(
                onClick = onOpenCreateDialog,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("নতুন থিম", fontSize = 12.sp)
            }
        }

        if (customThemes.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "এখনও কোনো কাস্টম থিম তৈরি করা হয়নি।",
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onOpenCreateDialog,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("একটি নতুন থিম তৈরি করুন")
                    }
                }
            }
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(customThemes) { customTheme ->
                    val isSelected = activeCustomId == customTheme.id
                    Card(
                        modifier = Modifier
                            .width(140.dp)
                            .height(160.dp)
                            .clickable {
                                themeManager.applyCustomTheme(customTheme)
                                activeCustomId = customTheme.id
                                Toast.makeText(context, "${customTheme.name} প্রয়োগ করা হয়েছে", Toast.LENGTH_SHORT).show()
                            },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = when (customTheme.backgroundType) {
                                BackgroundType.SOLID -> Color(customTheme.backgroundColor)
                                BackgroundType.GRADIENT -> Color(customTheme.gradientStartColor)
                                BackgroundType.IMAGE -> Color(0xFF0F172A)
                            }
                        ),
                        border = if (isSelected) {
                            androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF38BDF8))
                        } else {
                            androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
                        }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(10.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = customTheme.name,
                                    color = Color(customTheme.keyTextColor),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = "Active",
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            // Mini preview
                            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                listOf("ক", "খ", "গ").forEach { letter ->
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(RoundedCornerShape(customTheme.keyCornerRadius.coerceIn(2f, 8f).dp))
                                            .background(Color(customTheme.keyBackgroundColor).copy(alpha = customTheme.keyBackgroundOpacity)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(letter, color = Color(customTheme.keyTextColor), fontSize = 10.sp)
                                    }
                                }
                            }

                            // Action buttons: Edit & Delete
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                IconButton(
                                    onClick = { onEditTheme(customTheme) },
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Edit",
                                        tint = Color(0xFF94A3B8),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        repo.deleteCustomTheme(customTheme.id)
                                        customThemes = repo.getAllCustomThemes()
                                        if (activeCustomId == customTheme.id) {
                                            themeManager.applyPresetTheme("dark_night")
                                            activeCustomId = null
                                        }
                                        Toast.makeText(context, "থিম ডিলিট করা হয়েছে", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
