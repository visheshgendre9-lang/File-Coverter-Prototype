package com.example.ui

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.FormatAlignRight
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.DocxEditorEngine
import com.example.ui.theme.BrightCyan
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ErrorRose
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.WarningAmber
import com.example.viewmodel.AppNavScreen
import com.example.viewmodel.ConverterViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocxEditorScreen(
    viewModel: ConverterViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.docxEditorState.collectAsState()
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Editor, 1: Formatted Preview
    var showExportMenu by remember { mutableStateOf(false) }

    BackHandler {
        viewModel.navigateTo(AppNavScreen.HOME)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = state.documentTitle,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (state.isModified) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(WarningAmber)
                                )
                            }
                        }
                        Text(
                            text = if (state.isModified) "Unsaved changes" else "Saved to DOCX",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (state.isModified) WarningAmber else SuccessEmerald
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo(AppNavScreen.HOME) },
                        modifier = Modifier.testTag("docx_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Export Menu button
                    Box {
                        IconButton(
                            onClick = { showExportMenu = true },
                            modifier = Modifier.testTag("docx_export_button")
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = "Export Document", tint = BrightCyan)
                        }

                        DropdownMenu(
                            expanded = showExportMenu,
                            onDismissRequest = { showExportMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Export to PDF") },
                                leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = ErrorRose) },
                                onClick = {
                                    showExportMenu = false
                                    viewModel.exportDocxTo("pdf")
                                },
                                modifier = Modifier.testTag("export_pdf_option")
                            )
                            DropdownMenuItem(
                                text = { Text("Export to HTML") },
                                leadingIcon = { Icon(Icons.Default.Description, contentDescription = null, tint = ElectricBlue) },
                                onClick = {
                                    showExportMenu = false
                                    viewModel.exportDocxTo("html")
                                },
                                modifier = Modifier.testTag("export_html_option")
                            )
                            DropdownMenuItem(
                                text = { Text("Export to Plain Text (.txt)") },
                                leadingIcon = { Icon(Icons.Default.TextFields, contentDescription = null, tint = BrightCyan) },
                                onClick = {
                                    showExportMenu = false
                                    viewModel.exportDocxTo("txt")
                                },
                                modifier = Modifier.testTag("export_txt_option")
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Share Document") },
                                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = SuccessEmerald) },
                                onClick = {
                                    showExportMenu = false
                                    val uri = state.savedFileUri ?: state.uri
                                    if (uri != null) {
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "Share DOCX Document"))
                                    } else {
                                        viewModel.saveDocxChanges()
                                    }
                                },
                                modifier = Modifier.testTag("share_docx_option")
                            )
                        }
                    }

                    // Save Button
                    Button(
                        onClick = { viewModel.saveDocxChanges() },
                        enabled = !state.isSaving,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("docx_save_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricBlue
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        if (state.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                        } else {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Status Banner if any
            AnimatedVisibility(
                visible = state.statusMessage != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                state.statusMessage?.let { msg ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = if (msg.contains("fail", ignoreCase = true) || msg.contains("error", ignoreCase = true)) {
                            ErrorRose.copy(alpha = 0.2f)
                        } else {
                            SuccessEmerald.copy(alpha = 0.2f)
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = if (msg.contains("fail", ignoreCase = true)) Icons.Default.Close else Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = if (msg.contains("fail", ignoreCase = true)) ErrorRose else SuccessEmerald,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = msg,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            IconButton(
                                onClick = { viewModel.dismissEditorMessage() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Dismiss", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            // Stats and Metadata Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "${state.wordCount} words",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text("•", style = MaterialTheme.typography.labelSmall, color = Slate700)
                    Text(
                        text = "${state.charCount} chars",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text("•", style = MaterialTheme.typography.labelSmall, color = Slate700)
                    Text(
                        text = "${state.paragraphs.size} paragraphs",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (state.isImageBasedDocx) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(ElectricBlue.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "IMAGE-DOCUMENT",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = BrightCyan
                            )
                        )
                    }
                }
            }

            // Formatting Action Toolbar
            DocxFormattingToolbar(
                state = state,
                onToggleBold = { viewModel.setEditorStyle(bold = !state.isBold) },
                onToggleItalic = { viewModel.setEditorStyle(italic = !state.isItalic) },
                onToggleUnderline = { viewModel.setEditorStyle(underline = !state.isUnderline) },
                onSelectAlignment = { align -> viewModel.setEditorStyle(alignment = align) },
                onInsertBullet = { viewModel.insertBulletPrefix() },
                onInsertHeading = { level -> viewModel.insertHeadingPrefix(level) }
            )

            // Tabs: Edit Text vs Formatted Preview
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = ElectricBlue,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = ElectricBlue
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Edit Document", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Formatted Preview", fontWeight = FontWeight.Bold) }
                )
            }

            // Content Area
            if (state.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(color = ElectricBlue)
                        Text(
                            text = "Reading & Parsing DOCX Document...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                when (selectedTab) {
                    0 -> {
                        // Editable Paper Sheet Canvas
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background)
                                .padding(16.dp)
                        ) {
                            Card(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .border(1.dp, Slate800, RoundedCornerShape(16.dp)),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            ) {
                                OutlinedTextField(
                                    value = state.textContent,
                                    onValueChange = { newText -> viewModel.updateEditorText(newText) },
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp)
                                        .testTag("docx_text_editor_field"),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color.Transparent,
                                        unfocusedBorderColor = Color.Transparent,
                                        cursorColor = ElectricBlue
                                    ),
                                    placeholder = {
                                        Text(
                                            "Start typing your document content here...\n\n" +
                                                    "Tip: Use # for Heading 1, ## for Heading 2, or • for bullet lists.",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    },
                                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                                        fontSize = 15.sp,
                                        lineHeight = 22.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        }
                    }
                    1 -> {
                        // Formatted Document Preview
                        FormattedDocxPreview(
                            paragraphs = state.paragraphs,
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background)
                                .padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DocxFormattingToolbar(
    state: com.example.viewmodel.DocxEditorUiState,
    onToggleBold: () -> Unit,
    onToggleItalic: () -> Unit,
    onToggleUnderline: () -> Unit,
    onSelectAlignment: (String) -> Unit,
    onInsertBullet: () -> Unit,
    onInsertHeading: (Int) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Heading 1
            ToolbarButton(
                label = "H1",
                isActive = false,
                onClick = { onInsertHeading(1) },
                testTag = "format_h1_button"
            )

            // Heading 2
            ToolbarButton(
                label = "H2",
                isActive = false,
                onClick = { onInsertHeading(2) },
                testTag = "format_h2_button"
            )

            // Bullet List
            ToolbarIconButton(
                icon = Icons.AutoMirrored.Filled.FormatListBulleted,
                contentDescription = "Bullet List",
                isActive = false,
                onClick = onInsertBullet,
                testTag = "format_bullet_button"
            )

            Box(
                modifier = Modifier
                    .height(24.dp)
                    .width(1.dp)
                    .background(Slate700)
            )

            // Bold
            ToolbarIconButton(
                icon = Icons.Default.FormatBold,
                contentDescription = "Bold",
                isActive = state.isBold,
                onClick = onToggleBold,
                testTag = "format_bold_button"
            )

            // Italic
            ToolbarIconButton(
                icon = Icons.Default.FormatItalic,
                contentDescription = "Italic",
                isActive = state.isItalic,
                onClick = onToggleItalic,
                testTag = "format_italic_button"
            )

            // Underline
            ToolbarIconButton(
                icon = Icons.Default.FormatUnderlined,
                contentDescription = "Underline",
                isActive = state.isUnderline,
                onClick = onToggleUnderline,
                testTag = "format_underline_button"
            )

            Box(
                modifier = Modifier
                    .height(24.dp)
                    .width(1.dp)
                    .background(Slate700)
            )

            // Alignment Left
            ToolbarIconButton(
                icon = Icons.AutoMirrored.Filled.FormatAlignLeft,
                contentDescription = "Align Left",
                isActive = state.alignment == "left",
                onClick = { onSelectAlignment("left") },
                testTag = "format_align_left_button"
            )

            // Alignment Center
            ToolbarIconButton(
                icon = Icons.Default.FormatAlignCenter,
                contentDescription = "Align Center",
                isActive = state.alignment == "center",
                onClick = { onSelectAlignment("center") },
                testTag = "format_align_center_button"
            )

            // Alignment Right
            ToolbarIconButton(
                icon = Icons.AutoMirrored.Filled.FormatAlignRight,
                contentDescription = "Align Right",
                isActive = state.alignment == "right",
                onClick = { onSelectAlignment("right") },
                testTag = "format_align_right_button"
            )
        }
    }
}

@Composable
private fun ToolbarIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    isActive: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isActive) ElectricBlue.copy(alpha = 0.25f) else Color.Transparent)
            .border(
                1.dp,
                if (isActive) ElectricBlue else Slate700.copy(alpha = 0.4f),
                RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isActive) ElectricBlue else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun ToolbarButton(
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Box(
        modifier = Modifier
            .height(38.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isActive) ElectricBlue.copy(alpha = 0.25f) else Color.Transparent)
            .border(
                1.dp,
                if (isActive) ElectricBlue else Slate700.copy(alpha = 0.4f),
                RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = if (isActive) ElectricBlue else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun FormattedDocxPreview(
    paragraphs: List<DocxEditorEngine.DocxParagraph>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxSize()
            .border(1.dp, Slate800, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (paragraphs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Document is empty.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            for (p in paragraphs) {
                val align = when (p.alignment) {
                    "center" -> TextAlign.Center
                    "right" -> TextAlign.Right
                    else -> TextAlign.Start
                }

                when (p.style) {
                    "Heading1" -> {
                        Text(
                            text = p.fullText,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            textAlign = align,
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        )
                    }
                    "Heading2" -> {
                        Text(
                            text = p.fullText,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = ElectricBlue
                            ),
                            textAlign = align,
                            modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                        )
                    }
                    "ListBullet" -> {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(start = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "•",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = BrightCyan
                                )
                            )
                            Text(
                                text = p.fullText,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = if (p.runs.any { it.isBold }) FontWeight.Bold else FontWeight.Normal,
                                    fontStyle = if (p.runs.any { it.isItalic }) FontStyle.Italic else FontStyle.Normal,
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                textAlign = align,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    else -> {
                        Text(
                            text = p.fullText.ifEmpty { " " },
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = if (p.runs.any { it.isBold }) FontWeight.Bold else FontWeight.Normal,
                                fontStyle = if (p.runs.any { it.isItalic }) FontStyle.Italic else FontStyle.Normal,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            textAlign = align,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
