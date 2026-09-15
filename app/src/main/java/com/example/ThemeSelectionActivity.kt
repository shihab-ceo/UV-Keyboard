package com.example

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MyApplicationTheme

/**
 * Dedicated Theme Selection Activity allowing users to:
 * 1. Browse, customize, and apply built-in Preset Themes.
 * 2. Access "My Custom Themes" to securely list, apply, edit, and delete user-created themes.
 * 3. Create personalized themes with the live Custom Theme Builder.
 */
class ThemeSelectionActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                ThemeSelectionScreen(
                    onBackPressed = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSelectionScreen(
    onBackPressed: () -> Unit
) {
    val context = LocalContext.current
    val themeManager = remember { ThemeManager(context) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: My Custom Themes, 1: Preset Themes

    var savedThemes by remember { mutableStateOf(themeManager.getSavedCustomThemes()) }
    var activeCustomId by remember { mutableStateOf(themeManager.activeCustomThemeId) }
    var activePresetId by remember { mutableStateOf(themeManager.selectedThemeId) }

    var showBuilderDialog by remember { mutableStateOf(false) }
    var themeToEdit by remember { mutableStateOf<CustomThemeModel?>(null) }
    var themeToDelete by remember { mutableStateOf<CustomThemeModel?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "থিমসমূহ ও কাস্টমাইজেশন",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        themeToEdit = null
                        showBuilderDialog = true
                    }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Create Custom Theme",
                            tint = Color(0xFF38BDF8)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F172A)
                )
            )
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = {
                        themeToEdit = null
                        showBuilderDialog = true
                    },
                    containerColor = Color(0xFF2563EB),
                    contentColor = Color.White
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "New Theme")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("নতুন থিম", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        },
        containerColor = Color(0xFF0B1120)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Category Tabs: "আমার থিমসমূহ" & "প্রিসেট থিমসমূহ"
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF0F172A),
                contentColor = Color(0xFF38BDF8),
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = Color(0xFF38BDF8)
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Palette, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("আমার থিমসমূহ (${savedThemes.size})", fontWeight = FontWeight.SemiBold)
                        }
                    },
                    selectedContentColor = Color(0xFF38BDF8),
                    unselectedContentColor = Color(0xFF94A3B8)
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ColorLens, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("প্রিসেট থিমসমূহ", fontWeight = FontWeight.SemiBold)
                        }
                    },
                    selectedContentColor = Color(0xFF38BDF8),
                    unselectedContentColor = Color(0xFF94A3B8)
                )
            }

            // Tab Content
            if (selectedTab == 0) {
                // Section: My Custom Themes ("আমার থিমসমূহ")
                MyCustomThemesContent(
                    customThemes = savedThemes,
                    activeCustomId = activeCustomId,
                    onSelectTheme = { customTheme ->
                        themeManager.applyCustomTheme(customTheme)
                        activeCustomId = customTheme.id
                        activePresetId = "custom_${customTheme.id}"
                        Toast.makeText(context, "${customTheme.name} প্রয়োগ করা হয়েছে", Toast.LENGTH_SHORT).show()
                    },
                    onEditTheme = { customTheme ->
                        themeToEdit = customTheme
                        showBuilderDialog = true
                    },
                    onDeleteTheme = { customTheme ->
                        themeToDelete = customTheme
                    },
                    onCreateNew = {
                        themeToEdit = null
                        showBuilderDialog = true
                    }
                )
            } else {
                // Section: Preset Themes
                PresetThemesContent(
                    activePresetId = activePresetId,
                    onSelectPreset = { preset ->
                        themeManager.applyPresetTheme(preset.id)
                        activeCustomId = null
                        activePresetId = preset.id
                        Toast.makeText(context, "${preset.name} প্রয়োগ করা হয়েছে", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }

    // Delete Confirmation Dialog
    themeToDelete?.let { customTheme ->
        AlertDialog(
            onDismissRequest = { themeToDelete = null },
            title = { Text("থিম মুছে ফেলুন", fontWeight = FontWeight.Bold) },
            text = { Text("আপনি কি নিশ্চিত যে '${customTheme.name}' থিমটি স্থায়ীভাবে মুছে ফেলতে চান?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        themeManager.deleteCustomTheme(customTheme.id)
                        savedThemes = themeManager.getSavedCustomThemes()
                        if (activeCustomId == customTheme.id) {
                            activeCustomId = null
                            activePresetId = themeManager.selectedThemeId
                        }
                        themeToDelete = null
                        Toast.makeText(context, "${customTheme.name} মুছে ফেলা হয়েছে", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("মুছে ফেলুন", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { themeToDelete = null }) {
                    Text("বাতিল")
                }
            },
            containerColor = Color(0xFF1E293B),
            titleContentColor = Color.White,
            textContentColor = Color(0xFFCBD5E1)
        )
    }

    // Theme Builder Dialog
    if (showBuilderDialog) {
        CustomThemeBuilderDialog(
            initialTheme = themeToEdit,
            onDismiss = {
                showBuilderDialog = false
                themeToEdit = null
            },
            onSaveAndApply = { newTheme ->
                themeManager.saveCustomTheme(newTheme)
                themeManager.applyCustomTheme(newTheme)
                savedThemes = themeManager.getSavedCustomThemes()
                activeCustomId = newTheme.id
                activePresetId = "custom_${newTheme.id}"
                showBuilderDialog = false
                themeToEdit = null
                Toast.makeText(context, "${newTheme.name} সংরক্ষিত ও প্রয়োগ করা হয়েছে!", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

/**
 * Renders the "My Custom Themes" section.
 */
@Composable
private fun MyCustomThemesContent(
    customThemes: List<CustomThemeModel>,
    activeCustomId: String?,
    onSelectTheme: (CustomThemeModel) -> Unit,
    onEditTheme: (CustomThemeModel) -> Unit,
    onDeleteTheme: (CustomThemeModel) -> Unit,
    onCreateNew: () -> Unit
) {
    if (customThemes.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "এখনও কোনো কাস্টম থিম সংরক্ষিত নেই",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "পছন্দসই ব্যাকগ্রাউন্ড, রঙের প্যালেট এবং কী শৈলী দিয়ে আপনার নিজস্ব কিবোর্ড তৈরি করুন।",
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onCreateNew,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("একটি নতুন থিম তৈরি করুন")
                    }
                }
            }
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            items(customThemes, key = { it.id }) { theme ->
                val isSelected = activeCustomId == theme.id
                CustomThemeCard(
                    theme = theme,
                    isSelected = isSelected,
                    onSelect = { onSelectTheme(theme) },
                    onEdit = { onEditTheme(theme) },
                    onDelete = { onDeleteTheme(theme) }
                )
            }
        }
    }
}

/**
 * Visual card displaying a saved custom theme with live mini-keyboard preview and management buttons.
 */
@Composable
fun CustomThemeCard(
    theme: CustomThemeModel,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val bgModifier = when (theme.backgroundType) {
        BackgroundType.SOLID -> Modifier.background(Color(theme.backgroundColor))
        BackgroundType.GRADIENT -> Modifier.background(
            Brush.linearGradient(
                colors = listOf(Color(theme.gradientStartColor), Color(theme.gradientEndColor))
            )
        )
        BackgroundType.IMAGE -> Modifier.background(Color(0xFF0F172A))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        shape = RoundedCornerShape(14.dp),
        border = if (isSelected) {
            BorderStroke(2.dp, Color(0xFF38BDF8))
        } else {
            BorderStroke(1.dp, Color(0xFF334155))
        },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(bgModifier)
                .padding(12.dp)
        ) {
            Column {
                // Header: Theme Name + Active Checkmark
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = theme.name,
                        color = Color(theme.keyTextColor),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF38BDF8)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Active",
                                tint = Color.Black,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Mini-Keyboard Mockup Preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.25f))
                        .padding(6.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        // Row 1
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            listOf("ক", "খ", "গ", "ঘ").forEach { char ->
                                MiniMockKey(
                                    label = char,
                                    bgColor = Color(theme.keyBackgroundColor).copy(alpha = theme.keyBackgroundOpacity),
                                    textColor = Color(theme.keyTextColor),
                                    cornerRadius = theme.keyCornerRadius
                                )
                            }
                        }
                        // Row 2
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            MiniMockKey(
                                label = "⇧",
                                bgColor = Color(theme.accentColor).copy(alpha = 0.85f),
                                textColor = Color.White,
                                cornerRadius = theme.keyCornerRadius,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            MiniMockKey(
                                label = "Space",
                                bgColor = Color(theme.keyBackgroundColor).copy(alpha = theme.keyBackgroundOpacity),
                                textColor = Color(theme.keyTextColor),
                                cornerRadius = theme.keyCornerRadius,
                                modifier = Modifier.weight(2f)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            MiniMockKey(
                                label = "↵",
                                bgColor = Color(theme.accentColor),
                                textColor = Color.White,
                                cornerRadius = theme.keyCornerRadius,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Action Buttons: Apply indicator / Edit / Trash (Delete)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isSelected) {
                        Text(
                            text = "সক্রিয়",
                            color = Color(0xFF38BDF8),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    } else {
                        Text(
                            text = "প্রয়োগ করতে ট্যাপ করুন",
                            color = Color(theme.keyTextColor).copy(alpha = 0.7f),
                            fontSize = 10.sp
                        )
                    }

                    Row {
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Theme",
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Theme",
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

@Composable
private fun MiniMockKey(
    label: String,
    bgColor: Color,
    textColor: Color,
    cornerRadius: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(20.dp)
            .clip(RoundedCornerShape(cornerRadius.coerceIn(2f, 8f).dp))
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Renders the Preset Themes tab categorized into Classic, Gradient, and Minimal/Neon.
 */
@Composable
private fun PresetThemesContent(
    activePresetId: String,
    onSelectPreset: (KeyboardTheme) -> Unit
) {
    val presets = ThemeManager.PRESET_THEMES

    val classicThemes = presets.filter { !it.isGradient && it.id in listOf("material_light", "amoled_black", "midnight_blue", "dark_night", "classic_white") }
    val gradientThemes = presets.filter { it.isGradient }
    val minimalThemes = presets.filter { it.id in listOf("high_contrast", "neon_purple", "pastel_mint", "slate", "aqua_sky", "greenery") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "ক্লাসিক থিম (Classic)",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(classicThemes) { theme ->
                    PresetThemeCard(
                        theme = theme,
                        isSelected = activePresetId == theme.id,
                        onClick = { onSelectPreset(theme) }
                    )
                }
            }
        }

        item {
            Text(
                text = "গ্রেডিয়েন্ট থিম (Gradients)",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(gradientThemes) { theme ->
                    PresetThemeCard(
                        theme = theme,
                        isSelected = activePresetId == theme.id,
                        onClick = { onSelectPreset(theme) }
                    )
                }
            }
        }

        item {
            Text(
                text = "মিনিমাল ও নিয়ন থিম (Minimal & Neon)",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(minimalThemes) { theme ->
                    PresetThemeCard(
                        theme = theme,
                        isSelected = activePresetId == theme.id,
                        onClick = { onSelectPreset(theme) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PresetThemeCard(
    theme: KeyboardTheme,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgModifier = if (theme.isGradient) {
        Modifier.background(
            Brush.linearGradient(
                colors = listOf(Color(theme.gradientStartColor), Color(theme.gradientEndColor))
            )
        )
    } else {
        Modifier.background(Color(theme.backgroundColor))
    }

    Card(
        modifier = Modifier
            .width(130.dp)
            .height(130.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        border = if (isSelected) BorderStroke(2.dp, Color(0xFF38BDF8)) else BorderStroke(1.dp, Color(0xFF334155))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(bgModifier)
                .padding(10.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = theme.name,
                        color = Color(theme.textColor),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Active",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Mini key row
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("অ", "আ", "ক").forEach { char ->
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(RoundedCornerShape(theme.keyCornerRadius.coerceIn(2f, 6f).dp))
                                .background(Color(theme.keyColor).copy(alpha = theme.keyBackgroundOpacity)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(char, color = Color(theme.textColor), fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}
