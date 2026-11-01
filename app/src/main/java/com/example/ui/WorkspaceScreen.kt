package com.example.ui

import android.content.Intent
import android.text.format.Formatter
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NoiseAware
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.FormatRegistry
import com.example.model.FileCategory
import com.example.model.FileFormat
import com.example.model.JobStatus
import com.example.model.QualityPreset
import com.example.model.UpscaleResolution
import com.example.ui.theme.BrightCyan
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ErrorRose
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.WarningAmber
import com.example.viewmodel.ConverterViewModel

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceScreen(
    viewModel: ConverterViewModel,
    onPickNewFile: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.workspaceState.collectAsState()
    val context = LocalContext.current
    var showAdvancedSettings by remember { mutableStateOf(false) }

    if (state.selectedFileUri == null) {
        EmptyWorkspaceState(onPickFile = onPickNewFile, modifier = modifier)
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Source File Summary Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("source_file_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(ElectricBlue.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = ElectricBlue,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = state.selectedFileName ?: "Selected File",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = Formatter.formatFileSize(context, state.selectedFileSize),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(text = "•", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Slate700)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = state.inputFormat?.extension?.uppercase() ?: "UNKNOWN",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = BrightCyan
                                )
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = onPickNewFile,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Change", style = MaterialTheme.typography.labelMedium)
                    }
                }

                // If input is audio, show player for original audio
                val inputAudioUri = state.selectedFileUri
                if (state.inputFormat?.category == FileCategory.AUDIO && inputAudioUri != null) {
                    AudioPlayerCard(
                        audioUri = inputAudioUri,
                        title = "Original: ${state.selectedFileName}",
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 14.dp)
                    )
                }

                // If input is DOCX, offer direct editing
                if (state.inputFormat?.extension?.equals("docx", ignoreCase = true) == true && state.selectedFileUri != null) {
                    Button(
                        onClick = {
                            viewModel.openDocxForEditing(state.selectedFileUri!!, state.selectedFileName ?: "document.docx")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 14.dp)
                            .testTag("open_input_docx_editor_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                    ) {
                        Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open & Edit in DOCX Editor", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Voice Enhancement & Background Denoise Studio (when audio or video is loaded)
        if (state.inputFormat?.category == FileCategory.AUDIO || state.inputFormat?.category == FileCategory.VIDEO) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("voice_enhancer_card")
                        .border(1.dp, BrightCyan.copy(alpha = 0.4f), RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(BrightCyan.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Mic, contentDescription = null, tint = BrightCyan, modifier = Modifier.size(20.dp))
                            }
                            Column {
                                Text(
                                    text = "Voice Clarity & Noise Removal",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "DSP spectral filtering for speech isolation & hiss elimination",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Remove Background Noise Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Remove Background Noise", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                Text("Attenuates fan, AC hum, traffic, and room ambience", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = state.options.removeBackgroundNoise,
                                onCheckedChange = { chk -> viewModel.updateOptions { it.copy(removeBackgroundNoise = chk) } },
                                colors = SwitchDefaults.colors(checkedThumbColor = BrightCyan, checkedTrackColor = ElectricBlue.copy(alpha = 0.4f)),
                                modifier = Modifier.testTag("remove_noise_switch")
                            )
                        }

                        // Enhance Voice Clarity Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Enhance Voice Clarity & Crispness", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                Text("Excites speech presence (3kHz) for clear vocal consonants", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = state.options.enhanceVoiceClarity,
                                onCheckedChange = { chk -> viewModel.updateOptions { it.copy(enhanceVoiceClarity = chk) } },
                                colors = SwitchDefaults.colors(checkedThumbColor = BrightCyan, checkedTrackColor = ElectricBlue.copy(alpha = 0.4f)),
                                modifier = Modifier.testTag("enhance_voice_switch")
                            )
                        }

                        // Sliders if enabled
                        AnimatedVisibility(visible = state.options.removeBackgroundNoise || state.options.enhanceVoiceClarity) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                if (state.options.removeBackgroundNoise) {
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Denoise Strength", style = MaterialTheme.typography.labelMedium)
                                            Text("${(state.options.voiceDenoiseIntensity * 100).toInt()}%", fontWeight = FontWeight.Bold, color = BrightCyan)
                                        }
                                        Slider(
                                            value = state.options.voiceDenoiseIntensity,
                                            onValueChange = { v -> viewModel.updateOptions { it.copy(voiceDenoiseIntensity = v) } },
                                            valueRange = 0.1f..1.0f,
                                            colors = SliderDefaults.colors(thumbColor = BrightCyan, activeTrackColor = ElectricBlue)
                                        )
                                    }
                                }

                                if (state.options.enhanceVoiceClarity) {
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Vocal Presence Boost", style = MaterialTheme.typography.labelMedium)
                                            Text("${(state.options.voiceClarityBoost * 100).toInt()}%", fontWeight = FontWeight.Bold, color = BrightCyan)
                                        }
                                        Slider(
                                            value = state.options.voiceClarityBoost,
                                            onValueChange = { b -> viewModel.updateOptions { it.copy(voiceClarityBoost = b) } },
                                            valueRange = 0.1f..1.0f,
                                            colors = SliderDefaults.colors(thumbColor = BrightCyan, activeTrackColor = ElectricBlue)
                                        )
                                    }
                                }

                                // Leveler Switch
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Automatic Speech Leveler", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                    Switch(
                                        checked = state.options.voiceLeveler,
                                        onCheckedChange = { l -> viewModel.updateOptions { it.copy(voiceLeveler = l) } }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Target Format Selector
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Select Output Format",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${state.availableOutputs.size} compatible",
                        style = MaterialTheme.typography.labelSmall,
                        color = BrightCyan
                    )
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val recommended = state.inputFormat?.let { FormatRegistry.getRecommendedOutput(it) }

                    state.availableOutputs.forEach { format ->
                        val isSelected = state.targetFormat == format
                        val isRecommended = format == recommended

                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setTargetFormat(format) },
                            label = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = format.extension.uppercase(),
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                    if (isRecommended) {
                                        Text(
                                            text = "★",
                                            color = WarningAmber,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricBlue,
                                selectedLabelColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("format_chip_${format.extension}")
                        )
                    }
                }
            }
        }

        // Quality Configuration Panel
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quality_panel_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = BrightCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Quality Configuration",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = if (showAdvancedSettings) "Hide Custom" else "Fine Tune",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = ElectricBlue,
                            modifier = Modifier.clickable { showAdvancedSettings = !showAdvancedSettings }
                        )
                    }

                    // Presets
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        QualityPreset.values().forEach { preset ->
                            val isSelected = state.options.preset == preset
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) ElectricBlue else MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .clickable { viewModel.setQualityPreset(preset) }
                                    .padding(vertical = 2.dp)
                            ) {
                                Text(
                                    text = preset.displayName,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                    ),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    val isImageConversion = state.inputFormat?.category == FileCategory.IMAGE ||
                            state.targetFormat?.category == FileCategory.IMAGE ||
                            (state.inputFormat?.extension == "pdf" && state.targetFormat?.extension in listOf("jpg", "jpeg", "png", "webp", "bmp"))

                    Text(
                        text = state.options.preset.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Image Quality & 8K Upscaling Section (Always shown for images, or when Fine Tune / Custom is open)
                    if (isImageConversion || showAdvancedSettings || state.options.preset == QualityPreset.CUSTOM || state.options.preset == QualityPreset.ULTRA_8K) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            // Upscale / Resolution Selector
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = BrightCyan,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            "Output Resolution & Upscale",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }

                                    val resLabel = when {
                                        state.options.targetLongSidePx >= 7680 -> "8K (7680px)"
                                        state.options.targetLongSidePx >= 3840 -> "4K (3840px)"
                                        state.options.targetLongSidePx >= 2560 -> "2K (2560px)"
                                        state.options.targetLongSidePx >= 1920 -> "FHD (1920px)"
                                        state.options.scalePercent != 100 -> "${state.options.scalePercent}% Scale"
                                        else -> "Original (1x)"
                                    }
                                    Text(
                                        text = resLabel,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (state.options.targetLongSidePx >= 7680) WarningAmber else BrightCyan
                                    )
                                }

                                // Quick Resolution Chips
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    UpscaleResolution.values().forEach { res ->
                                        val isCurrent = (res.targetPx == 0 && state.options.targetLongSidePx == 0 && state.options.scalePercent == 100) ||
                                                (res.targetPx > 0 && state.options.targetLongSidePx == res.targetPx)
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isCurrent) {
                                                if (res == UpscaleResolution.UHD_8K) WarningAmber else ElectricBlue
                                            } else MaterialTheme.colorScheme.surface,
                                            modifier = Modifier
                                                .clickable {
                                                    viewModel.updateOptions {
                                                        it.copy(
                                                            targetLongSidePx = res.targetPx,
                                                            scalePercent = 100,
                                                            upscaleSharpen = res.targetPx >= 1920,
                                                            preset = when (res) {
                                                                UpscaleResolution.UHD_8K -> QualityPreset.ULTRA_8K
                                                                UpscaleResolution.UHD_4K -> QualityPreset.MAXIMUM
                                                                UpscaleResolution.ORIGINAL -> QualityPreset.BALANCED
                                                                else -> QualityPreset.CUSTOM
                                                            }
                                                        )
                                                    }
                                                }
                                                .testTag("res_chip_${res.shortTag}")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                if (res == UpscaleResolution.UHD_8K) {
                                                    Icon(
                                                        imageVector = Icons.Default.AutoAwesome,
                                                        contentDescription = null,
                                                        tint = if (isCurrent) Color.Black else WarningAmber,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                }
                                                Text(
                                                    text = res.displayName,
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (isCurrent) {
                                                            if (res == UpscaleResolution.UHD_8K) Color.Black else Color.White
                                                        } else MaterialTheme.colorScheme.onSurface
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }

                                // 8K / 4K Active Super-Resolution Callout
                                if (state.options.targetLongSidePx >= 7680 || state.options.preset == QualityPreset.ULTRA_8K) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = WarningAmber.copy(alpha = 0.15f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, WarningAmber.copy(alpha = 0.4f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AutoAwesome,
                                                contentDescription = null,
                                                tint = WarningAmber,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = "8K Super-Resolution Active: Image will be upscaled up to 7680px with detail preservation.",
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }

                                // Detail & Edge Clarity Switch
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "Edge & Texture Clarity Boost",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                                        )
                                        Text(
                                            "Unsharp mask prevents blurriness during 4K/8K upscaling",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Switch(
                                        checked = state.options.upscaleSharpen,
                                        onCheckedChange = { checked ->
                                            viewModel.updateOptions { it.copy(upscaleSharpen = checked) }
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = ElectricBlue,
                                            checkedTrackColor = BrightCyan.copy(alpha = 0.35f)
                                        )
                                    )
                                }
                            }

                            // Image Quality Level
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.HighQuality,
                                            contentDescription = null,
                                            tint = BrightCyan,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text("Image Quality", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                    }
                                    val qualityTag = when {
                                        state.options.qualityPercent >= 98 -> "100% (Lossless / Master)"
                                        state.options.qualityPercent >= 90 -> "${state.options.qualityPercent}% (Ultra)"
                                        state.options.qualityPercent >= 80 -> "${state.options.qualityPercent}% (High)"
                                        else -> "${state.options.qualityPercent}% (Compact)"
                                    }
                                    Text(qualityTag, fontWeight = FontWeight.Bold, color = BrightCyan, style = MaterialTheme.typography.labelSmall)
                                }

                                // Quick Quality Chips
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    listOf(100 to "Master", 95 to "Ultra", 85 to "High", 70 to "Normal").forEach { (q, label) ->
                                        val isSel = state.options.qualityPercent == q
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isSel) ElectricBlue else MaterialTheme.colorScheme.surface,
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable {
                                                    viewModel.updateOptions { it.copy(qualityPercent = q, preset = QualityPreset.CUSTOM) }
                                                }
                                        ) {
                                            Text(
                                                text = "$q% $label",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontSize = 10.sp,
                                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface
                                                ),
                                                modifier = Modifier.padding(vertical = 5.dp),
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                            )
                                        }
                                    }
                                }

                                Slider(
                                    value = state.options.qualityPercent.toFloat(),
                                    onValueChange = { q -> viewModel.updateOptions { it.copy(qualityPercent = q.toInt(), preset = QualityPreset.CUSTOM) } },
                                    valueRange = 10f..100f,
                                    colors = SliderDefaults.colors(thumbColor = BrightCyan, activeTrackColor = ElectricBlue)
                                )
                            }

                            // Custom Scale Slider (shown if Custom Resolution selected)
                            if (state.options.targetLongSidePx == 0) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Custom Scale Factor", style = MaterialTheme.typography.labelMedium)
                                        Text("${state.options.scalePercent}%", fontWeight = FontWeight.Bold, color = BrightCyan)
                                    }
                                    Slider(
                                        value = state.options.scalePercent.toFloat(),
                                        onValueChange = { s -> viewModel.updateOptions { it.copy(scalePercent = s.toInt(), targetLongSidePx = 0, preset = QualityPreset.CUSTOM) } },
                                        valueRange = 25f..400f,
                                        steps = 14,
                                        colors = SliderDefaults.colors(thumbColor = BrightCyan, activeTrackColor = ElectricBlue)
                                    )
                                }
                            }

                            // Audio Bitrate
                            if (state.targetFormat?.category == FileCategory.AUDIO) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("Audio Bitrate", style = MaterialTheme.typography.labelMedium)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        listOf(64, 128, 192, 256, 320).forEach { rate ->
                                            val isSel = state.options.audioBitrateKbps == rate
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (isSel) ElectricBlue else MaterialTheme.colorScheme.surface,
                                                modifier = Modifier.clickable {
                                                    viewModel.updateOptions { it.copy(audioBitrateKbps = rate, preset = QualityPreset.CUSTOM) }
                                                }
                                            ) {
                                                Text(
                                                    text = "${rate}k",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface
                                                    ),
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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
        }

        // Live Progress or Convert Action Button
        item {
            if (state.isConverting) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = state.progressStatusText,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${(state.conversionProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = BrightCyan
                            )
                        }

                        LinearProgressIndicator(
                            progress = { state.conversionProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = ElectricBlue,
                            trackColor = Slate800
                        )
                    }
                }
            } else {
                Button(
                    onClick = { viewModel.startSingleConversion() },
                    enabled = state.targetFormat != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("start_conversion_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Convert to ${state.targetFormat?.extension?.uppercase() ?: ""}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                    }
                }
            }
        }

        // Converted Result Card
        item {
            val job = state.currentJob
            if (job != null && job.status == JobStatus.COMPLETED) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("result_card")
                        .border(1.dp, SuccessEmerald.copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = SuccessEmerald,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Conversion Succeeded",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Size comparison
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Original", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(Formatter.formatFileSize(context, job.originalSizeBytes), fontWeight = FontWeight.Bold)
                            }
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = BrightCyan)
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Converted", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(Formatter.formatFileSize(context, job.resultSizeBytes), fontWeight = FontWeight.Bold, color = SuccessEmerald)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Speed", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${job.durationMs} ms", fontWeight = FontWeight.SemiBold)
                            }
                        }

                        // Playback preview if audio output
                        if ((job.targetFormat.category == FileCategory.AUDIO || job.inputFormat.category == FileCategory.AUDIO) && job.resultUri != null) {
                            AudioPlayerCard(
                                audioUri = job.resultUri!!,
                                title = "Play Enhanced Audio (${job.targetFormat.extension.uppercase()})"
                            )
                        }

                        // Share / Save Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    job.resultUri?.let { uri ->
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = job.targetFormat.mimeType
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "Share Converted File"))
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("share_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Share", fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    job.resultUri?.let { uri ->
                                        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(uri, job.targetFormat.mimeType)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        try {
                                            context.startActivity(viewIntent)
                                        } catch (e: Exception) {
                                            // Fallback share if no viewer
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = job.targetFormat.mimeType
                                                putExtra(Intent.EXTRA_STREAM, uri)
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, "Open With"))
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("open_file_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Open", fontWeight = FontWeight.Bold)
                            }
                        }

                        // Direct DOCX Editor launch button if converted file is a DOCX
                        if (job.targetFormat.extension.equals("docx", ignoreCase = true) && job.resultUri != null) {
                            Button(
                                onClick = {
                                    viewModel.openDocxForEditing(
                                        job.resultUri!!,
                                        "${job.originalName.substringBeforeLast('.')}.docx"
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("edit_docx_result_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SuccessEmerald)
                            ) {
                                Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Open in DOCX Editor", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else if (job != null && job.status == JobStatus.FAILED) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.Error, contentDescription = null, tint = ErrorRose)
                        Column {
                            Text("Conversion Failed", fontWeight = FontWeight.Bold, color = ErrorRose)
                            Text(job.errorMessage ?: "Unknown error", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyWorkspaceState(onPickFile: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(ElectricBlue.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = ElectricBlue,
                    modifier = Modifier.size(40.dp)
                )
            }

            Text(
                text = "No File Loaded",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "Pick a file from storage or choose a test sample on the Home screen.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Button(
                onClick = onPickFile,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                modifier = Modifier.height(48.dp)
            ) {
                Text("Select File to Convert", fontWeight = FontWeight.Bold)
            }
        }
    }
}
