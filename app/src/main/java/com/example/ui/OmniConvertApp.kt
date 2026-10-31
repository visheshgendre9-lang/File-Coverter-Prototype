package com.example.ui

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DynamicFeed
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.BrightCyan
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.OmniConvertTheme
import com.example.viewmodel.AppNavScreen
import com.example.viewmodel.ConverterViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OmniConvertApp(viewModel: ConverterViewModel) {
    val context = LocalContext.current
    val currentScreen by viewModel.currentScreen.collectAsState()
    val batchState by viewModel.batchState.collectAsState()
    val isDark by viewModel.isDarkMode.collectAsState()

    // Single File Picker Launcher
    val singleFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileMeta = queryFileMetadata(context, uri)
            val mimeType = context.contentResolver.getType(uri)
            viewModel.selectFileForWorkspace(
                uri = uri,
                name = fileMeta.first,
                sizeBytes = fileMeta.second,
                mimeType = mimeType
            )
        }
    }

    // Multiple Files Picker Launcher
    val multipleFilesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            val list = uris.map { uri ->
                val meta = queryFileMetadata(context, uri)
                Triple(uri, meta.first, meta.second)
            }
            viewModel.addBatchFiles(list)
        }
    }

    // Back handling for sub-screens
    BackHandler(enabled = currentScreen != AppNavScreen.HOME) {
        viewModel.navigateTo(AppNavScreen.HOME)
    }

    OmniConvertTheme(darkTheme = isDark) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = when (currentScreen) {
                                AppNavScreen.HOME -> "OmniConvert"
                                AppNavScreen.WORKSPACE -> "Workspace"
                                AppNavScreen.BATCH -> "Batch Queue"
                                AppNavScreen.HISTORY -> "History"
                                AppNavScreen.SETTINGS -> "Settings"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        if (currentScreen != AppNavScreen.HOME) {
                            IconButton(onClick = { viewModel.navigateTo(AppNavScreen.HOME) }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    modifier = Modifier.testTag("bottom_nav_bar")
                ) {
                    NavigationBarItem(
                        selected = currentScreen == AppNavScreen.HOME,
                        onClick = { viewModel.navigateTo(AppNavScreen.HOME) },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Home") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ElectricBlue,
                            indicatorColor = ElectricBlue.copy(alpha = 0.15f)
                        )
                    )

                    NavigationBarItem(
                        selected = currentScreen == AppNavScreen.WORKSPACE,
                        onClick = { viewModel.navigateTo(AppNavScreen.WORKSPACE) },
                        icon = { Icon(Icons.Default.Transform, contentDescription = "Workspace") },
                        label = { Text("Workspace") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ElectricBlue,
                            indicatorColor = ElectricBlue.copy(alpha = 0.15f)
                        )
                    )

                    NavigationBarItem(
                        selected = currentScreen == AppNavScreen.BATCH,
                        onClick = { viewModel.navigateTo(AppNavScreen.BATCH) },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (batchState.queue.isNotEmpty()) {
                                        Badge(containerColor = BrightCyan) {
                                            Text("${batchState.queue.size}")
                                        }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.DynamicFeed, contentDescription = "Batch")
                            }
                        },
                        label = { Text("Batch") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ElectricBlue,
                            indicatorColor = ElectricBlue.copy(alpha = 0.15f)
                        )
                    )

                    NavigationBarItem(
                        selected = currentScreen == AppNavScreen.HISTORY,
                        onClick = { viewModel.navigateTo(AppNavScreen.HISTORY) },
                        icon = { Icon(Icons.Default.History, contentDescription = "History") },
                        label = { Text("History") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ElectricBlue,
                            indicatorColor = ElectricBlue.copy(alpha = 0.15f)
                        )
                    )

                    NavigationBarItem(
                        selected = currentScreen == AppNavScreen.SETTINGS,
                        onClick = { viewModel.navigateTo(AppNavScreen.SETTINGS) },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                        label = { Text("Settings") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ElectricBlue,
                            indicatorColor = ElectricBlue.copy(alpha = 0.15f)
                        )
                    )
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (currentScreen) {
                    AppNavScreen.HOME -> HomeScreen(
                        viewModel = viewModel,
                        onPickSingleFile = { singleFileLauncher.launch("*/*") },
                        onPickMultipleFiles = { multipleFilesLauncher.launch("*/*") }
                    )
                    AppNavScreen.WORKSPACE -> WorkspaceScreen(
                        viewModel = viewModel,
                        onPickNewFile = { singleFileLauncher.launch("*/*") }
                    )
                    AppNavScreen.BATCH -> BatchScreen(
                        viewModel = viewModel,
                        onAddFiles = { multipleFilesLauncher.launch("*/*") }
                    )
                    AppNavScreen.HISTORY -> HistoryScreen(
                        viewModel = viewModel
                    )
                    AppNavScreen.SETTINGS -> SettingsScreen(
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

private fun queryFileMetadata(context: Context, uri: Uri): Pair<String, Long> {
    var name = "unknown_file"
    var size = 0L

    try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                if (nameIndex != -1) name = cursor.getString(nameIndex) ?: name
                if (sizeIndex != -1) size = cursor.getLong(sizeIndex)
            }
        }
    } catch (e: Exception) {
        // Fallback
        name = uri.lastPathSegment ?: "unknown_file"
    }

    return Pair(name, size)
}
