package com.example.ui

import android.content.Intent
import android.text.format.Formatter
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DynamicFeed
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.core.content.FileProvider
import com.example.engine.ConversionEngineManager
import com.example.engine.FormatRegistry
import com.example.model.ConversionJob
import com.example.model.FileFormat
import com.example.model.JobStatus
import com.example.ui.theme.BrightCyan
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ErrorRose
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.SuccessEmerald
import com.example.viewmodel.ConverterViewModel

@Composable
fun BatchScreen(
    viewModel: ConverterViewModel,
    onAddFiles: () -> Unit,
    modifier: Modifier = Modifier
) {
    val batchState by viewModel.batchState.collectAsState()
    val context = LocalContext.current

    if (batchState.queue.isEmpty()) {
        EmptyBatchState(onAddFiles = onAddFiles, modifier = modifier)
        return
    }

    val completedCount = batchState.queue.count { it.status == JobStatus.COMPLETED }
    val allCompleted = completedCount == batchState.queue.size && batchState.queue.isNotEmpty()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Queue Header Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("batch_header_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Batch Processing Queue",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${batchState.queue.size} files in queue • $completedCount completed",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(
                            onClick = onAddFiles,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(ElectricBlue.copy(alpha = 0.15f))
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Files", tint = ElectricBlue)
                        }
                    }

                    if (batchState.isProcessing) {
                        LinearProgressIndicator(
                            progress = { batchState.totalProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = BrightCyan,
                            trackColor = Slate800
                        )
                    }
                }
            }
        }

        // Action Buttons Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { viewModel.startBatchConversion() },
                    enabled = !batchState.isProcessing && !allCompleted,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("convert_all_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                ) {
                    if (batchState.isProcessing) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Converting Queue...")
                    } else {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Convert All", fontWeight = FontWeight.Bold)
                    }
                }

                if (completedCount > 0) {
                    Button(
                        onClick = {
                            val manager = ConversionEngineManager(context)
                            val zipFile = manager.bundleJobsIntoZip(batchState.queue.filter { it.status == JobStatus.COMPLETED })
                            if (zipFile != null) {
                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", zipFile)
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/zip"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Save or Share Batch ZIP"))
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("download_zip_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessEmerald)
                    ) {
                        Icon(Icons.Default.FolderZip, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export ZIP", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Queue Items
        items(batchState.queue, key = { it.id }) { job ->
            BatchItemCard(
                job = job,
                onRemove = { viewModel.removeBatchItem(job.id) },
                onTargetChanged = { newTarget -> viewModel.updateBatchItemTarget(job.id, newTarget) }
            )
        }
    }
}

@Composable
fun BatchItemCard(
    job: ConversionJob,
    onRemove: () -> Unit,
    onTargetChanged: (FileFormat) -> Unit
) {
    val context = LocalContext.current
    var formatMenuExpanded by remember { mutableStateOf(false) }
    val compatibleFormats = remember(job.inputFormat) {
        FormatRegistry.getCompatibleOutputs(job.inputFormat)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("batch_item_${job.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = job.originalName,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Text(
                        text = "${Formatter.formatFileSize(context, job.originalSizeBytes)} • ${job.inputFormat.extension.uppercase()} to ${job.targetFormat.extension.uppercase()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Format selector dropdown
                Box {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.clickable { formatMenuExpanded = true }
                    ) {
                        Text(
                            text = "To: ${job.targetFormat.extension.uppercase()}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = BrightCyan),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = formatMenuExpanded,
                        onDismissRequest = { formatMenuExpanded = false }
                    ) {
                        compatibleFormats.forEach { fmt ->
                            DropdownMenuItem(
                                text = { Text(fmt.displayName) },
                                onClick = {
                                    onTargetChanged(fmt)
                                    formatMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                // Status or remove
                when (job.status) {
                    JobStatus.COMPLETED -> {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Done", tint = SuccessEmerald, modifier = Modifier.size(22.dp))
                    }
                    JobStatus.FAILED -> {
                        Text("Failed", color = ErrorRose, style = MaterialTheme.typography.labelSmall)
                    }
                    JobStatus.PROCESSING -> {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = BrightCyan)
                    }
                    else -> {
                        IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Remove", tint = Slate700)
                        }
                    }
                }
            }

            if (job.status == JobStatus.PROCESSING) {
                LinearProgressIndicator(
                    progress = { job.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = BrightCyan,
                    trackColor = Slate800
                )
            }
        }
    }
}

@Composable
fun EmptyBatchState(onAddFiles: () -> Unit, modifier: Modifier = Modifier) {
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
                    .background(BrightCyan.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DynamicFeed,
                    contentDescription = null,
                    tint = BrightCyan,
                    modifier = Modifier.size(40.dp)
                )
            }

            Text(
                text = "Batch Queue Empty",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "Select multiple files to convert them simultaneously and download everything in one ZIP archive.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Button(
                onClick = onAddFiles,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                modifier = Modifier.height(48.dp)
            ) {
                Text("Select Multiple Files", fontWeight = FontWeight.Bold)
            }
        }
    }
}
