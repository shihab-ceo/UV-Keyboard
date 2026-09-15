package com.example

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.keyboard.ui.BackgroundImageViewer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * ImageCropActivity allows users to pan, pinch-to-zoom, crop to keyboard aspect ratio,
 * and adjust the darkness overlay opacity before applying their custom photo as the keyboard theme.
 */
class ImageCropActivity : ComponentActivity() {

    companion object {
        const val EXTRA_IMAGE_URI = "extra_image_uri"
    }

    private lateinit var themeManager: ThemeManager
    private var viewerRef: BackgroundImageViewer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        themeManager = ThemeManager(this)

        val uriString = intent.getStringExtra(EXTRA_IMAGE_URI) ?: intent.dataString
        if (uriString.isNullOrEmpty()) {
            Toast.makeText(this, "No image provided for cropping", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val imageUri = Uri.parse(uriString)

        setContent {
            ImageCropScreen(
                imageUri = imageUri,
                initialOpacity = themeManager.bgImageOpacity,
                onApply = { opacity ->
                    val viewer = viewerRef
                    val cropped = viewer?.getCroppedBitmap()
                    if (cropped != null) {
                        val savedUri = themeManager.saveCustomCroppedBackground(this, cropped, opacity)
                        if (savedUri != null) {
                            Toast.makeText(this, "Keyboard background applied!", Toast.LENGTH_SHORT).show()
                            setResult(Activity.RESULT_OK)
                            finish()
                        } else {
                            Toast.makeText(this, "Failed to save cropped image", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this, "Error cropping image", Toast.LENGTH_SHORT).show()
                    }
                },
                onCancel = {
                    setResult(Activity.RESULT_CANCELED)
                    finish()
                },
                onViewerReady = { viewer ->
                    viewerRef = viewer
                }
            )
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun ImageCropScreen(
        imageUri: Uri,
        initialOpacity: Float,
        onApply: (Float) -> Unit,
        onCancel: () -> Unit,
        onViewerReady: (BackgroundImageViewer) -> Unit
    ) {
        var overlayOpacity by remember { mutableFloatStateOf(initialOpacity) }
        var loadedBitmap by remember { mutableStateOf<Bitmap?>(null) }
        var isLoading by remember { mutableStateOf(true) }

        // Safely load bitmap with memory-safe downsampling
        LaunchedEffect(imageUri) {
            withContext(Dispatchers.IO) {
                try {
                    val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    contentResolver.openInputStream(imageUri)?.use { stream ->
                        BitmapFactory.decodeStream(stream, null, boundsOptions)
                    }

                    // Downscale large camera photos to ~1920px max dimension
                    val maxDim = maxOf(boundsOptions.outWidth, boundsOptions.outHeight)
                    var sampleSize = 1
                    while (maxDim / sampleSize > 1920) {
                        sampleSize *= 2
                    }

                    val decodeOptions = BitmapFactory.Options().apply {
                        inSampleSize = sampleSize
                        inPreferredConfig = Bitmap.Config.RGB_565
                    }

                    val bitmap = contentResolver.openInputStream(imageUri)?.use { stream ->
                        BitmapFactory.decodeStream(stream, null, decodeOptions)
                    }
                    withContext(Dispatchers.Main) {
                        loadedBitmap = bitmap
                        isLoading = false
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        isLoading = false
                        Toast.makeText(this@ImageCropActivity, "Failed to load image: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Crop Keyboard Background",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onCancel,
                            modifier = Modifier.testTag("crop_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF0F172A)
                    )
                )
            },
            containerColor = Color(0xFF0F172A)
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Interactive Viewport Area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color(0xFF0088CC))
                    } else if (loadedBitmap != null) {
                        AndroidView(
                            factory = { context ->
                                BackgroundImageViewer(context).apply {
                                    setImageBitmap(loadedBitmap)
                                    this.overlayOpacity = overlayOpacity
                                    onViewerReady(this)
                                }
                            },
                            update = { viewer ->
                                viewer.overlayOpacity = overlayOpacity
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("background_image_cropper_view")
                        )
                    } else {
                        Text(
                            text = "Could not load selected photo.",
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                // Control Sheet / Adjustment Panel
                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Gesture Instruction
                        Text(
                            text = "💡 Pinch to zoom & drag to align inside the keyboard frame",
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp
                        )

                        // Darkness / Opacity Slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Darkness Overlay (Legibility)",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "${(overlayOpacity * 100).toInt()}%",
                                color = Color(0xFF38BDF8),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        Slider(
                            value = overlayOpacity,
                            onValueChange = { overlayOpacity = it },
                            valueRange = 0f..0.90f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF0088CC),
                                activeTrackColor = Color(0xFF0088CC),
                                inactiveTrackColor = Color(0xFF334155)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("crop_opacity_slider")
                        )

                        // Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = onCancel,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("crop_cancel_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFF94A3B8)
                                )
                            ) {
                                Text("Cancel", fontWeight = FontWeight.SemiBold)
                            }

                            Button(
                                onClick = { onApply(overlayOpacity) },
                                modifier = Modifier
                                    .weight(1.5f)
                                    .height(48.dp)
                                    .testTag("crop_apply_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF0088CC)
                                ),
                                enabled = !isLoading && loadedBitmap != null
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Apply to Keyboard",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
