package com.example

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.example.clipboard.ClipboardItem
import com.example.ui.theme.MyApplicationTheme

enum class MainNavTab(val title: String) {
    SETUP("Setup"),
    THEMES("Themes"),
    CLIPBOARD("Clipboard"),
    SETTINGS("Settings"),
    GUIDES("Guides")
}

class MainActivity : ComponentActivity() {

    private lateinit var themeManager: ThemeManager
    private lateinit var clipboardHelper: ClipboardManagerHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        themeManager = ThemeManager(this)
        clipboardHelper = ClipboardManagerHelper(this)

        setContent {
            MyApplicationTheme {
                UVKeyboardMainScreen(
                    themeManager = themeManager,
                    clipboardHelper = clipboardHelper
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UVKeyboardMainScreen(
    themeManager: ThemeManager,
    clipboardHelper: ClipboardManagerHelper
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(MainNavTab.SETUP) }

    // IME Status tracking
    var isImeEnabled by remember { mutableStateOf(checkIsImeEnabled(context)) }
    var isImeSelected by remember { mutableStateOf(checkIsImeSelected(context)) }

    // Lifecycle observer to re-check IME status when user returns from system settings
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isImeEnabled = checkIsImeEnabled(context)
                isImeSelected = checkIsImeSelected(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFF0F172A),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF6366F1), Color(0xFF38BDF8))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "UV",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "UV Keyboard",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                text = "Bengali & English Custom IME",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1E293B)
                ),
                actions = {
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isImeSelected) Color(0xFF065F46) else Color(0xFF7F1D1D)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isImeSelected) "Active" else "Inactive",
                            color = if (isImeSelected) Color(0xFFA7F3D0) else Color(0xFFFECACA),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF1E293B),
                contentColor = Color.White
            ) {
                MainNavTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = {
                            when (tab) {
                                MainNavTab.SETUP -> Icon(Icons.Default.Keyboard, contentDescription = "Setup")
                                MainNavTab.THEMES -> Icon(Icons.Default.Palette, contentDescription = "Themes")
                                MainNavTab.CLIPBOARD -> Icon(Icons.Default.ContentCopy, contentDescription = "Clipboard")
                                MainNavTab.SETTINGS -> Icon(Icons.Default.Settings, contentDescription = "Settings")
                                MainNavTab.GUIDES -> Icon(Icons.Default.Help, contentDescription = "Guides")
                            }
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF38BDF8),
                            selectedTextColor = Color(0xFF38BDF8),
                            unselectedIconColor = Color(0xFF94A3B8),
                            unselectedTextColor = Color(0xFF94A3B8),
                            indicatorColor = Color(0xFF334155)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                MainNavTab.SETUP -> SetupTabContent(
                    isImeEnabled = isImeEnabled,
                    isImeSelected = isImeSelected,
                    onRefresh = {
                        isImeEnabled = checkIsImeEnabled(context)
                        isImeSelected = checkIsImeSelected(context)
                    }
                )
                MainNavTab.THEMES -> ThemesTabContent(themeManager = themeManager)
                MainNavTab.CLIPBOARD -> ClipboardTabContent(clipboardHelper = clipboardHelper)
                MainNavTab.SETTINGS -> SettingsTabContent(themeManager = themeManager)
                MainNavTab.GUIDES -> GuidesTabContent()
            }
        }
    }
}

// -------------------------------------------------------------
// 1. SETUP TAB
// -------------------------------------------------------------
@Composable
fun SetupTabContent(
    isImeEnabled: Boolean,
    isImeSelected: Boolean,
    onRefresh: () -> Unit
) {
    val context = LocalContext.current
    var testText by remember { mutableStateOf("") }

    // Audio Permission Launcher
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudioPermission = granted
        if (granted) {
            Toast.makeText(context, "Voice input permission granted!", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "🚀 কীবোর্ড সক্রিয়করণ (Setup Wizard)",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = onRefresh) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh Status",
                            tint = Color(0xFF38BDF8)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Follow these two quick steps to start typing with UV Keyboard anywhere on your device.",
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp
                )
            }
        }

        // Step 1: Enable Keyboard
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isImeEnabled) Color(0xFF132E27) else Color(0xFF1E293B)
            ),
            shape = RoundedCornerShape(14.dp),
            border = if (isImeEnabled) {
                androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981))
            } else null
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isImeEnabled) Color(0xFF10B981) else Color(0xFF334155)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isImeEnabled) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                    } else {
                        Text("1", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ধাপ ১: UV Keyboard সক্ষম করুন",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = if (isImeEnabled) "সক্ষম করা হয়েছে (Enabled)" else "Settings থেকে কীবোর্ডটি চালু করুন",
                        color = if (isImeEnabled) Color(0xFF34D399) else Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                }
                Button(
                    onClick = {
                        val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isImeEnabled) Color(0xFF047857) else Color(0xFF2563EB)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(if (isImeEnabled) "Manage" else "Enable")
                }
            }
        }

        // Step 2: Select Keyboard
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isImeSelected) Color(0xFF132E27) else Color(0xFF1E293B)
            ),
            shape = RoundedCornerShape(14.dp),
            border = if (isImeSelected) {
                androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981))
            } else null
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isImeSelected) Color(0xFF10B981) else Color(0xFF334155)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isImeSelected) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                    } else {
                        Text("2", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ধাপ ২: UV Keyboard নির্বাচন করুন",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = if (isImeSelected) "বর্তমানে সক্রিয় কীবোর্ড (Active)" else "কীবোর্ড স্যুইচ করুন",
                        color = if (isImeSelected) Color(0xFF34D399) else Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                }
                Button(
                    onClick = {
                        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.showInputMethodPicker()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isImeSelected) Color(0xFF047857) else Color(0xFF2563EB)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(if (isImeSelected) "Switch" else "Select")
                }
            }
        }

        // Voice Input Permission Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (hasAudioPermission) Color(0xFF059669) else Color(0xFF475569)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Mic, contentDescription = "Mic", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ভয়েস টাইপিং পারমিশন (Voice-to-Text)",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = if (hasAudioPermission) "Permission Granted" else "Required for microphone dictation",
                        color = if (hasAudioPermission) Color(0xFF34D399) else Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                }
                if (!hasAudioPermission) {
                    OutlinedButton(
                        onClick = {
                            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Grant", color = Color(0xFF38BDF8))
                    }
                }
            }
        }

        // Live Interactive Typing Test Area
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "✍️ লাইভ টাইপিং টেস্ট বক্স",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    if (testText.isNotEmpty()) {
                        IconButton(onClick = { testText = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color(0xFF94A3B8))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = testText,
                    onValueChange = { testText = it },
                    placeholder = {
                        Text("এখানে টাইপ করে জাতীয়/Avro/বিজয় ও স্পীচ পরীক্ষা করুন...", color = Color(0xFF64748B))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .testTag("test_typing_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF38BDF8),
                        unfocusedBorderColor = Color(0xFF475569),
                        cursorColor = Color(0xFF38BDF8)
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val presets = listOf("বাংলাদেশ", "জাতীয়", "ধন্যবাদ", "Avro test")
                    presets.forEach { word ->
                        FilterChip(
                            selected = false,
                            onClick = { testText = (testText + " " + word).trim() },
                            label = { Text(word, color = Color(0xFFCBD5E1), fontSize = 12.sp) }
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 2. THEMES TAB
// -------------------------------------------------------------
@Composable
fun ThemesTabContent(themeManager: ThemeManager) {
    val context = LocalContext.current
    val customThemeRepo = remember { CustomThemeRepository(context) }
    var customThemesList by remember { mutableStateOf(customThemeRepo.getAllCustomThemes()) }
    var activeCustomThemeId by remember { mutableStateOf(themeManager.activeCustomThemeId) }
    var selectedThemeId by remember { mutableStateOf(themeManager.selectedThemeId) }
    var useCustomColor by remember { mutableStateOf(themeManager.useCustomColor) }
    var customColorInt by remember { mutableIntStateOf(themeManager.customBgColor) }
    var bgImageUriStr by remember { mutableStateOf(themeManager.bgImageUriString) }
    var bgOpacity by remember { mutableFloatStateOf(themeManager.bgImageOpacity) }

    // Custom Theme Builder Dialog State
    var showBuilderDialog by remember { mutableStateOf(false) }
    var themeToEdit by remember { mutableStateOf<CustomThemeModel?>(null) }

    // Preset Filter Category
    var selectedPresetCategory by remember { mutableStateOf("All") }

    // Cropping Activity Launcher
    val cropLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            bgImageUriStr = themeManager.bgImageUriString
            bgOpacity = themeManager.bgImageOpacity
            Toast.makeText(context, "Keyboard background updated!", Toast.LENGTH_SHORT).show()
        }
    }

    // Gallery Image Picker Launcher -> forwards to ImageCropActivity
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
            val cropIntent = Intent(context, ImageCropActivity::class.java).apply {
                putExtra(ImageCropActivity.EXTRA_IMAGE_URI, uri.toString())
                data = uri
            }
            cropLauncher.launch(cropIntent)
        }
    }

    val filteredPresetThemes = remember(selectedPresetCategory) {
        when (selectedPresetCategory) {
            "Classic" -> ThemeManager.PRESET_THEMES.filter {
                it.id in listOf("material_light", "amoled_black", "midnight_blue", "dark_night", "classic_white")
            }
            "Gradient" -> ThemeManager.PRESET_THEMES.filter {
                it.isGradient || it.id in listOf("sunset_gradient", "deep_space_purple", "ocean_gradient", "forest_emerald")
            }
            "Neon" -> ThemeManager.PRESET_THEMES.filter {
                it.id in listOf("high_contrast_dark", "neon_purple", "pastel_mint", "slate", "aqua_sky", "greenery")
            }
            else -> ThemeManager.PRESET_THEMES
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "🎨 কীবোর্ড থিম ও থিম বিল্ডার",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = "বিল্ট-ইন প্রিসেট থিম বেছে নিন অথবা সম্পূর্ণ কাস্টম থিম তৈরি করুন।",
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        context.startActivity(android.content.Intent(context, ThemeSelectionActivity::class.java))
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8))
                ) {
                    Icon(Icons.Default.Palette, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("থিম স্ক্রিন", fontSize = 12.sp)
                }
                Button(
                    onClick = {
                        themeToEdit = null
                        showBuilderDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("নতুন থিম", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Section 1: Built-in Preset Themes
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "১. প্রিসেট থিমস (Built-in Preset Themes)",
                    color = Color(0xFF38BDF8),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Category Filter Chips
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    val categories = listOf(
                        "All" to "সকল থিম",
                        "Classic" to "ক্লাসিক",
                        "Gradient" to "গ্রেডিয়েন্ট",
                        "Neon" to "মিনিমাল ও নিয়ন"
                    )
                    categories.forEach { (catKey, catLabel) ->
                        FilterChip(
                            selected = selectedPresetCategory == catKey,
                            onClick = { selectedPresetCategory = catKey },
                            label = { Text(catLabel, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF38BDF8),
                                selectedLabelColor = Color(0xFF0F172A),
                                containerColor = Color(0xFF0F172A),
                                labelColor = Color(0xFF94A3B8)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(filteredPresetThemes) { theme ->
                        val isSelected = !useCustomColor && activeCustomThemeId == null && selectedThemeId == theme.id
                        ThemeCard(
                            theme = theme,
                            isSelected = isSelected,
                            onClick = {
                                themeManager.applyPresetTheme(theme.id)
                                selectedThemeId = theme.id
                                activeCustomThemeId = null
                                useCustomColor = false
                                bgImageUriStr = null
                                Toast.makeText(context, "${theme.name} প্রয়োগ করা হয়েছে", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }

        // Section 2: Saved Custom Themes & Builder
        SavedCustomThemesSection(
            themeManager = themeManager,
            onOpenCreateDialog = {
                themeToEdit = null
                showBuilderDialog = true
            },
            onEditTheme = { customTheme ->
                themeToEdit = customTheme
                showBuilderDialog = true
            }
        )

        // Section 3: Custom Solid Color
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ColorLens, contentDescription = null, tint = Color(0xFF38BDF8))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "৩. দ্রুত সলিড কালার (Quick Solid Color)",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                val paletteColors = listOf(
                    0xFF0F172A.toInt(), 0xFF1E1B4B.toInt(), 0xFF064E3B.toInt(),
                    0xFF831843.toInt(), 0xFF701A75.toInt(), 0xFF1C1917.toInt(),
                    0xFF083344.toInt(), 0xFF312E81.toInt(), 0xFF1E293B.toInt()
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 6.dp)
                ) {
                    items(paletteColors) { colorValue ->
                        val isPicked = useCustomColor && customColorInt == colorValue
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color(colorValue))
                                .border(
                                    width = if (isPicked) 3.dp else 1.dp,
                                    color = if (isPicked) Color.White else Color(0xFF475569),
                                    shape = CircleShape
                                )
                                .clickable {
                                    useCustomColor = true
                                    themeManager.useCustomColor = true
                                    themeManager.activeCustomThemeId = null
                                    activeCustomThemeId = null
                                    customColorInt = colorValue
                                    themeManager.customBgColor = colorValue
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isPicked) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section 4: Custom Gallery Image Background
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Image, contentDescription = null, tint = Color(0xFF38BDF8))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "৪. গ্যালারি ব্যাকগ্রাউন্ড ইমেজ (Gallery Image)",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Pick any image from your device. It will be center-cropped beneath the keys with adjustable opacity overlay.",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            galleryLauncher.launch(arrayOf("image/*"))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("গ্যালারি থেকে ছবি বাছুন")
                    }

                    if (bgImageUriStr != null) {
                        Button(
                            onClick = {
                                val cropIntent = Intent(context, ImageCropActivity::class.java).apply {
                                    putExtra(ImageCropActivity.EXTRA_IMAGE_URI, bgImageUriStr)
                                    data = Uri.parse(bgImageUriStr)
                                }
                                cropLauncher.launch(cropIntent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0088CC)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("ক্রপ / অ্যাডজাস্ট")
                        }

                        OutlinedButton(
                            onClick = {
                                themeManager.clearCustomBackground(context)
                                bgImageUriStr = null
                                Toast.makeText(context, "Image removed", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Remove", color = Color(0xFFEF4444))
                        }
                    }
                }

                if (bgImageUriStr != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Overlay Opacity / Darkening: ${(bgOpacity * 100).toInt()}%",
                        color = Color(0xFFCBD5E1),
                        fontSize = 13.sp
                    )
                    Slider(
                        value = bgOpacity,
                        onValueChange = {
                            bgOpacity = it
                            themeManager.bgImageOpacity = it
                        },
                        valueRange = 0.1f..0.9f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF38BDF8),
                            activeTrackColor = Color(0xFF38BDF8),
                            inactiveTrackColor = Color(0xFF334155)
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    // Image Preview Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        AsyncImage(
                            model = bgImageUriStr,
                            contentDescription = "Background Preview",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = bgOpacity))
                        )
                        Text(
                            text = "কীবোর্ড প্রিভিউ (Preview)",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }
        }
    }

    // Builder Dialog
    if (showBuilderDialog) {
        CustomThemeBuilderDialog(
            initialTheme = themeToEdit,
            onDismiss = {
                showBuilderDialog = false
                themeToEdit = null
            },
            onSaveAndApply = { newOrUpdatedTheme ->
                val repo = CustomThemeRepository(context)
                repo.saveCustomTheme(newOrUpdatedTheme)
                themeManager.applyCustomTheme(newOrUpdatedTheme)
                activeCustomThemeId = newOrUpdatedTheme.id
                selectedThemeId = "custom_${newOrUpdatedTheme.id}"
                useCustomColor = false
                bgImageUriStr = themeManager.bgImageUriString
                bgOpacity = themeManager.bgImageOpacity
                showBuilderDialog = false
                themeToEdit = null
                Toast.makeText(context, "${newOrUpdatedTheme.name} সংরক্ষিত ও প্রয়োগ করা হয়েছে!", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun ThemeCard(
    theme: KeyboardTheme,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(136.dp)
            .height(164.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (theme.isGradient) Color(theme.gradientStartColor) else Color(theme.backgroundColor)
        ),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF38BDF8))
        } else {
            androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (theme.isGradient) {
                        Modifier.background(
                            Brush.linearGradient(
                                listOf(Color(theme.gradientStartColor), Color(theme.gradientEndColor))
                            )
                        )
                    } else Modifier
                )
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
                        text = theme.name,
                        color = Color(theme.textColor),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (isSelected) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Selected",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Mini keyboard preview keys
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        listOf("ক", "খ", "গ").forEach { letter ->
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(RoundedCornerShape(theme.keyCornerRadius.coerceIn(2f, 8f).dp))
                                    .background(Color(theme.keyColor).copy(alpha = theme.keyBackgroundOpacity)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(letter, color = Color(theme.textColor), fontSize = 10.sp)
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        listOf("া", "ি", "ী").forEach { letter ->
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(RoundedCornerShape(theme.keyCornerRadius.coerceIn(2f, 8f).dp))
                                    .background(Color(theme.keyColor).copy(alpha = theme.keyBackgroundOpacity)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(letter, color = Color(theme.textColor), fontSize = 10.sp)
                            }
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(18.dp)
                            .clip(RoundedCornerShape(theme.keyCornerRadius.coerceIn(2f, 6f).dp))
                            .background(Color(theme.actionKeyColor)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("UV", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 3. CLIPBOARD TAB (Room DB 3-Day & Pinning)
// -------------------------------------------------------------
@Composable
fun ClipboardTabContent(clipboardHelper: ClipboardManagerHelper) {
    val context = LocalContext.current
    val clips by clipboardHelper.getAllClips().collectAsState(initial = emptyList())
    var searchQuery by remember { mutableStateOf("") }

    val filteredClips = clips.filter {
        searchQuery.isEmpty() || it.text.contains(searchQuery, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Info Banner: 3-Day Auto-Expiry & Pinning logic
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PushPin, contentDescription = null, tint = Color(0xFF38BDF8))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "স্মার্ট ক্লিপবোর্ড ম্যানেজার (Smart Clipboard)",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "• সমস্ত কপি করা টেক্সট ৩ দিন (৭২ ঘণ্টা) সংরক্ষণ হয়।\n" +
                            "• পিন (📌) করা ক্লিপ কখনো স্বয়ংক্রিয়ভাবে মুছে যায় না।\n" +
                            "• কীবোর্ডের candidate বার থেকেও দ্রুত এক ট্যাপে পেস্ট করা যায়।",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }

        // Search and Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("ক্লিপ খুঁজুন...", color = Color(0xFF64748B), fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color(0xFF94A3B8)) },
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF38BDF8),
                    unfocusedBorderColor = Color(0xFF334155)
                )
            )

            Button(
                onClick = {
                    clipboardHelper.clearUnpinnedClips()
                    Toast.makeText(context, "Unpinned clips cleared", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Clear All", fontSize = 12.sp)
            }
        }

        // List of Clips
        if (filteredClips.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (searchQuery.isNotEmpty()) "কোন ক্লিপ খুঁজে পাওয়া যায়নি" else "ক্লিপবোর্ড খালি। যেকোনো টেক্সট কপি করলে এখানে জমা হবে।",
                    color = Color(0xFF64748B),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredClips, key = { it.id }) { clip ->
                    ClipboardItemRow(
                        clip = clip,
                        onPinToggle = { clipboardHelper.togglePin(clip) },
                        onCopy = {
                            clipboardHelper.copyToClipboard(clip.text)
                            Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        onDelete = { clipboardHelper.deleteClip(clip.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun ClipboardItemRow(
    clip: ClipboardItem,
    onPinToggle: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit
) {
    val threeDaysMs = 72L * 60 * 60 * 1000L
    val remainingHours = ((clip.timestamp + threeDaysMs - System.currentTimeMillis()) / (60 * 60 * 1000)).coerceAtLeast(0)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(12.dp),
        border = if (clip.isPinned) {
            androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8))
        } else null
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (clip.isPinned) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF0369A1))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("PINNED (স্থায়ী)", color = Color(0xFFE0F2FE), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Text(
                        text = "মেয়াদ: ${remainingHours}h বাকি",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onPinToggle, modifier = Modifier.size(32.dp)) {
                    Icon(
                        if (clip.isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                        contentDescription = "Pin",
                        tint = if (clip.isPinned) Color(0xFF38BDF8) else Color(0xFF94A3B8)
                    )
                }
                IconButton(onClick = onCopy, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color(0xFF34D399))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444))
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = clip.text,
                color = Color.White,
                fontSize = 13.sp,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// -------------------------------------------------------------
// 4. SETTINGS TAB
// -------------------------------------------------------------
@Composable
fun SettingsTabContent(themeManager: ThemeManager) {
    var heightScale by remember { mutableFloatStateOf(themeManager.keyboardHeightScale) }
    var fontScale by remember { mutableFloatStateOf(themeManager.fontSizeScale) }
    var haptic by remember { mutableStateOf(themeManager.hapticFeedbackEnabled) }
    var sound by remember { mutableStateOf(themeManager.keySoundEnabled) }
    var predictions by remember { mutableStateOf(themeManager.wordPredictionsEnabled) }
    var autocorrect by remember { mutableStateOf(themeManager.autocorrectEnabled) }
    var personalLearning by remember { mutableStateOf(themeManager.personalLearningEnabled) }
    var aggressiveness by remember { mutableStateOf(themeManager.autocorrectAggressiveness) }
    var defaultLayout by remember { mutableStateOf(themeManager.defaultStartupLayout) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "⚙️ সেটিংস ও আকার নিয়ন্ত্রণ (Settings)",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp
        )

        // Sizing & Dimensions
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "আকার ও ডাইমেনশন (Dimensions)",
                    color = Color(0xFF38BDF8),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Keyboard Height
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "কীবোর্ড উচ্চতা (Height Scale):",
                        color = Color.White,
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${(heightScale * 100).toInt()}%",
                        color = Color(0xFF38BDF8),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
                Slider(
                    value = heightScale,
                    onValueChange = {
                        heightScale = it
                        themeManager.keyboardHeightScale = it
                    },
                    valueRange = 0.8f..2.5f,
                    modifier = Modifier.fillMaxWidth().testTag("keyboard_height_slider"),
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF38BDF8),
                        activeTrackColor = Color(0xFF38BDF8),
                        inactiveTrackColor = Color(0xFF334155)
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Font Size Scale
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "ফন্ট স্কেলিং (Font Size Scale):",
                        color = Color.White,
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${(fontScale * 100).toInt()}%",
                        color = Color(0xFF38BDF8),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
                Slider(
                    value = fontScale,
                    onValueChange = {
                        fontScale = it
                        themeManager.fontSizeScale = it
                    },
                    valueRange = 0.8f..2.5f,
                    modifier = Modifier.fillMaxWidth().testTag("key_font_size_slider"),
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF38BDF8),
                        activeTrackColor = Color(0xFF38BDF8),
                        inactiveTrackColor = Color(0xFF334155)
                    )
                )
            }
        }

        // Feedback Toggles
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "ইনপুট ও ফিডব্যাক (Input & Feedback)",
                    color = Color(0xFF38BDF8),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Haptic Feedback
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("হ্যাপটিক ফিডব্যাক (Vibration on keypress)", color = Color.White, fontSize = 14.sp)
                        Text("কী-বোর্ড স্পর্শে হালকা কম্পন", color = Color(0xFF94A3B8), fontSize = 11.sp)
                    }
                    Switch(
                        checked = haptic,
                        onCheckedChange = {
                            haptic = it
                            themeManager.hapticFeedbackEnabled = it
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF2563EB))
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Key Sound
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("কী সাউন্ড ইফেক্টস (Key Sound Effects)", color = Color.White, fontSize = 14.sp)
                        Text("কী টাইপ করার সময় ক্লিক শব্দ", color = Color(0xFF94A3B8), fontSize = 11.sp)
                    }
                    Switch(
                        checked = sound,
                        onCheckedChange = {
                            sound = it
                            themeManager.keySoundEnabled = it
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF2563EB))
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Smart Word Suggestions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("স্মার্ট সাজেশন (Smart Suggestions)", color = Color.White, fontSize = 14.sp)
                        Text("ক্যান্ডিডেট বারে প্রাসঙ্গিক শব্দ ও প্রেডিকশন প্রদর্শন", color = Color(0xFF94A3B8), fontSize = 11.sp)
                    }
                    Switch(
                        checked = predictions,
                        onCheckedChange = {
                            predictions = it
                            themeManager.wordPredictionsEnabled = it
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF2563EB))
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Smart Autocorrect
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("স্মার্ট অটো-কারেক্ট (Smart Autocorrect)", color = Color.White, fontSize = 14.sp)
                        Text("স্পেসবারে ভুল বানান স্বয়ংক্রিয়ভাবে সঠিক করা", color = Color(0xFF94A3B8), fontSize = 11.sp)
                    }
                    Switch(
                        checked = autocorrect,
                        onCheckedChange = {
                            autocorrect = it
                            themeManager.autocorrectEnabled = it
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF2563EB))
                    )
                }

                if (autocorrect) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "অটো-কারেক্ট সংবেদনশীলতা (Aggressiveness)",
                        color = Color(0xFF38BDF8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "Conservative" to "সংযত",
                            "Balanced" to "ভারসাম্যপূর্ণ",
                            "Aggressive" to "আক্রমণাত্মক"
                        ).forEach { (mode, label) ->
                            val isSelected = aggressiveness == mode
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color(0xFF2563EB) else Color(0xFF334155))
                                    .clickable {
                                        aggressiveness = mode
                                        themeManager.autocorrectAggressiveness = mode
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = mode,
                                        color = if (isSelected) Color.White else Color(0xFFCBD5E1),
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                    Text(
                                        text = label,
                                        color = if (isSelected) Color(0xFFE0F2FE) else Color(0xFF94A3B8),
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Personal Learning
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("ব্যক্তিগত শব্দ শিক্ষা (Personal Learning)", color = Color.White, fontSize = 14.sp)
                        Text("আপনার ঘন ঘন টাইপ করা শব্দ অফলাইনে মনে রাখা", color = Color(0xFF94A3B8), fontSize = 11.sp)
                    }
                    Switch(
                        checked = personalLearning,
                        onCheckedChange = {
                            personalLearning = it
                            themeManager.personalLearningEnabled = it
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF2563EB))
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Clear Learned Words
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF334155))
                        .clickable {
                            coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                try {
                                    val db = com.example.clipboard.AppDatabase.getInstance(context)
                                    db.userWordDao().clearAll()
                                } catch (_: Exception) {}
                            }
                            Toast.makeText(context, "ব্যক্তিগত শেখা শব্দ মুছে ফেলা হয়েছে", Toast.LENGTH_SHORT).show()
                        }
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Clear dictionary",
                        tint = Color(0xFFF87171),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("শেখা শব্দ রিসেট (Clear Learned Words)", color = Color(0xFFF87171), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("অফলাইনে সংরক্ষিত ব্যক্তিগত ডিকশনারি মুছে ফেলবে", color = Color(0xFF94A3B8), fontSize = 10.sp)
                    }
                }
            }
        }

        // Enabled Layouts for Spacebar Swipe
        var enabledLayoutsSet by remember { mutableStateOf(themeManager.enabledLayouts) }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "🌐 স্পেসবার সোয়াইপ লেআউট নির্বাচন (Spacebar Layouts)",
                    color = Color(0xFF38BDF8),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "যে লেআউটগুলো সক্রিয় রাখবেন, স্পেসবারে ডানে/বামে সোয়াইপ করলে কেবল সেগুলোর মধ্যেই স্যুইচ হবে।",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                val availableLayouts = listOf(
                    ThemeManager.LAYOUT_JATIYA to "জাতীয় (Jatiya BDS 1738)",
                    ThemeManager.LAYOUT_AVRO to "Avro Phonetic (অফলাইন ফোনেটিক)",
                    ThemeManager.LAYOUT_BIJOY to "বিজয় (Bijoy Classic)",
                    ThemeManager.LAYOUT_PRABHAT to "প্রভাত (Prabhat Layout)",
                    ThemeManager.LAYOUT_ENGLISH to "English (US QWERTY)"
                )

                availableLayouts.forEach { (modeKey, title) ->
                    val isChecked = enabledLayoutsSet.contains(modeKey)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                themeManager.setLayoutEnabled(modeKey, !isChecked)
                                enabledLayoutsSet = themeManager.enabledLayouts
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { checked ->
                                themeManager.setLayoutEnabled(modeKey, checked)
                                enabledLayoutsSet = themeManager.enabledLayouts
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = Color(0xFF38BDF8),
                                checkmarkColor = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = title, color = Color.White, fontSize = 13.sp)
                    }
                }
            }
        }

        // Default Startup Layout
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "ডিফল্ট স্টার্টআপ লেআউট (Default Startup Layout)",
                    color = Color(0xFF38BDF8),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                val layouts = listOf(
                    ThemeManager.LAYOUT_JATIYA to "জাতীয় লেআউট (Jatiya BDS 1738 - Default)",
                    ThemeManager.LAYOUT_AVRO to "Avro Phonetic (অফলাইন ফোনেটিক সিস্টেম)",
                    ThemeManager.LAYOUT_BIJOY to "বিজয় লেআউট (Bijoy Classic)",
                    ThemeManager.LAYOUT_PRABHAT to "প্রভাত লেআউট (Prabhat Layout)",
                    ThemeManager.LAYOUT_ENGLISH to "English (US QWERTY)"
                )

                layouts.forEach { (modeKey, title) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                defaultLayout = modeKey
                                themeManager.defaultStartupLayout = modeKey
                            }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = defaultLayout == modeKey,
                            onClick = {
                                defaultLayout = modeKey
                                themeManager.defaultStartupLayout = modeKey
                            },
                            colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF38BDF8))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = title, color = Color.White, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 5. GUIDES TAB
// -------------------------------------------------------------
@Composable
fun GuidesTabContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "📖 টাইপিং নির্দেশিকা ও গাইড (User Guides)",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp
        )

        // Jatiya BDS 1738
        GuideCard(
            title = "জাতীয় লেআউট (BDS 1738)",
            content = "• বাংলাদেশ সরকারের জাতীয় মানসম্মত কীবোর্ড লেআউট।\n" +
                    "• উপরের প্রিভিউ লেবেল দেখে Shift চেপে বা দীর্ঘ চাপ দিয়ে যুক্তবর্ণ/স্বরবর্ণ পাওয়া যায়।\n" +
                    "• স্পেসবারে বামে বা ডানে সোয়াইপ করে তাৎক্ষণিক অন্য লেআউটে স্যুইচ করা যায়।"
        )

        // Avro Phonetic
        GuideCard(
            title = "Avro ফোনেটিক টাইপিং রুলস (Phonetic Shortcuts)",
            content = "• ami = আমি, tumi = তুমি, bangla = বাংলা\n" +
                    "• shonar = সোনার, dhaka = ঢাকা, desh = দেশ\n" +
                    "• kkh = ক্ষ, gn = জ্ঞ, ng = ঙ, sh = শ, Sh = ষ\n" +
                    "• টাইপ করার সাথে সাথেই ক্যান্ডিডেট বারে শব্দ সাজেশন চলে আসে। স্পেস চাপলেই স্বয়ংক্রিয়ভাবে প্রথম সাজেশনটি কমিট হয়।"
        )

        // Bijoy Classic
        GuideCard(
            title = "বিজয় লেআউট (Bijoy Classic)",
            content = "• ঐতিহ্যবাহী আনসি/ইউনিকোড বিজয় কীবোর্ড ম্যাপিং।\n" +
                    "• Q, W, E, R... ও Shift সমন্বয়ে পরিচিত পদ্ধতিতে দ্রুত টাইপ করুন।"
        )

        // Prabhat Layout
        GuideCard(
            title = "প্রভাত লেআউট (Prabhat Layout)",
            content = "• জনপ্রিয় মুক্ত ও উন্মুক্ত সোর্স বাংলা টাইপিং লেআউট।\n" +
                    "• সহজে ও স্বাচ্ছন্দ্যে বাংলা বর্ণমালা টাইপ করার সুশৃঙ্খল বিন্যাস।"
        )

        // Smart Clipboard
        GuideCard(
            title = "স্মার্ট ক্লিপবোর্ড ও ৩ দিনের অটো-ক্লিনআপ",
            content = "• যেকোনো টেক্সট কপি করলে ব্যাকগ্রাউন্ডে তা সংরক্ষিত হয়।\n" +
                    "• ৭২ ঘণ্টা পর অপ্রয়োজনীয় ক্লিপ স্বয়ংক্রিয়ভাবে মুছে যায়।\n" +
                    "• যে ক্লিপগুলো আপনি 'পিন (📌)' করবেন, সেগুলো কখনোই ডিলিট হবে না।"
        )
    }
}

@Composable
fun GuideCard(title: String, content: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                color = Color(0xFF38BDF8),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = content,
                color = Color(0xFFCBD5E1),
                fontSize = 13.sp,
                lineHeight = 20.sp
            )
        }
    }
}

// -------------------------------------------------------------
// Helper Methods & Greeting Composable for Screenshot Tests
// -------------------------------------------------------------
fun checkIsImeEnabled(context: Context): Boolean {
    return try {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        val enabledList = imm?.enabledInputMethodList ?: emptyList()
        enabledList.any { it.packageName == context.packageName }
    } catch (_: Exception) {
        false
    }
}

fun checkIsImeSelected(context: Context): Boolean {
    return try {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            imm?.currentInputMethodInfo?.packageName == context.packageName
        } else {
            val currentIme = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.DEFAULT_INPUT_METHOD
            )
            currentIme?.contains(context.packageName) == true
        }
    } catch (_: Exception) {
        false
    }
}

/**
 * Retained for backward-compatibility with screenshot tests
 */
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color(0xFF0F172A)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Welcome $name!",
                color = Color(0xFF38BDF8),
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
            Text(
                text = "UV Keyboard Ready",
                color = Color(0xFF94A3B8),
                fontSize = 14.sp
            )
        }
    }
}
