package com.example.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.ConversionEntity
import com.example.data.HistoryRepository
import com.example.engine.ConversionEngineManager
import com.example.engine.FormatRegistry
import com.example.engine.SampleFileItem
import com.example.model.ConversionJob
import com.example.model.ConversionOptions
import com.example.model.FileCategory
import com.example.model.FileFormat
import com.example.model.JobStatus
import com.example.model.QualityPreset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

enum class AppNavScreen {
    HOME,
    WORKSPACE,
    BATCH,
    HISTORY,
    SETTINGS
}

data class WorkspaceUiState(
    val selectedFileName: String? = null,
    val selectedFileUri: Uri? = null,
    val selectedFileSize: Long = 0L,
    val inputFormat: FileFormat? = null,
    val availableOutputs: List<FileFormat> = emptyList(),
    val targetFormat: FileFormat? = null,
    val options: ConversionOptions = ConversionOptions(),
    val currentJob: ConversionJob? = null,
    val isConverting: Boolean = false,
    val conversionProgress: Float = 0f,
    val progressStatusText: String = ""
)

data class BatchUiState(
    val queue: List<ConversionJob> = emptyList(),
    val isProcessing: Boolean = false,
    val totalProgress: Float = 0f,
    val uniformTargetFormat: FileFormat? = null,
    val zipBundleResultUri: Uri? = null
)

class ConverterViewModel(application: Application) : AndroidViewModel(application) {

    private val engineManager = ConversionEngineManager(application)
    private val historyRepo = HistoryRepository(AppDatabase.getDatabase(application).conversionDao())

    // Navigation
    private val _currentScreen = MutableStateFlow(AppNavScreen.HOME)
    val currentScreen: StateFlow<AppNavScreen> = _currentScreen.asStateFlow()

    // Workspace State
    private val _workspaceState = MutableStateFlow(WorkspaceUiState())
    val workspaceState: StateFlow<WorkspaceUiState> = _workspaceState.asStateFlow()

    // Batch State
    private val _batchState = MutableStateFlow(BatchUiState())
    val batchState: StateFlow<BatchUiState> = _batchState.asStateFlow()

    // History Flow from Room
    val historyList: StateFlow<List<ConversionEntity>> = historyRepo.allHistory
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Settings
    val isDarkMode = MutableStateFlow(true)
    val defaultPreset = MutableStateFlow(QualityPreset.BALANCED)
    val autoClearOnExit = MutableStateFlow(false)

    fun navigateTo(screen: AppNavScreen) {
        _currentScreen.value = screen
    }

    fun selectFileForWorkspace(uri: Uri, name: String, sizeBytes: Long, mimeType: String?) {
        val detected = FormatRegistry.detectFormat(name, mimeType)
        val compatible = FormatRegistry.getCompatibleOutputs(detected)
        val recommended = FormatRegistry.getRecommendedOutput(detected)
        val target = if (compatible.contains(recommended)) recommended else compatible.firstOrNull()

        _workspaceState.value = WorkspaceUiState(
            selectedFileName = name,
            selectedFileUri = uri,
            selectedFileSize = sizeBytes,
            inputFormat = detected,
            availableOutputs = compatible,
            targetFormat = target,
            options = getOptionsForPreset(defaultPreset.value)
        )
        _currentScreen.value = AppNavScreen.WORKSPACE
    }

    fun loadSampleFile(sample: SampleFileItem) {
        val uri = sample.uriProvider(getApplication())
        val detected = FormatRegistry.detectFormat(sample.name)
        val compatible = FormatRegistry.getCompatibleOutputs(detected)
        val recommended = FormatRegistry.getRecommendedOutput(detected)
        val target = if (compatible.contains(recommended)) recommended else compatible.firstOrNull()

        _workspaceState.value = WorkspaceUiState(
            selectedFileName = sample.name,
            selectedFileUri = uri,
            selectedFileSize = 48000L,
            inputFormat = detected,
            availableOutputs = compatible,
            targetFormat = target,
            options = getOptionsForPreset(defaultPreset.value)
        )
        _currentScreen.value = AppNavScreen.WORKSPACE
    }

    fun openVoiceEnhancerDemo() {
        val samples = com.example.engine.SampleAssetsProvider.getSampleFiles()
        val voiceSample = samples.firstOrNull { it.name.contains("voice") } ?: samples.first()
        val uri = voiceSample.uriProvider(getApplication())
        val detected = FormatRegistry.detectFormat(voiceSample.name)
        val compatible = FormatRegistry.getCompatibleOutputs(detected)

        _workspaceState.value = WorkspaceUiState(
            selectedFileName = voiceSample.name,
            selectedFileUri = uri,
            selectedFileSize = 176000L,
            inputFormat = detected,
            availableOutputs = compatible,
            targetFormat = FormatRegistry.MP3,
            options = getOptionsForPreset(defaultPreset.value).copy(
                enhanceVoiceClarity = true,
                removeBackgroundNoise = true,
                voiceDenoiseIntensity = 0.8f,
                voiceClarityBoost = 0.75f,
                voiceLeveler = true
            )
        )
        _currentScreen.value = AppNavScreen.WORKSPACE
    }

    fun setTargetFormat(format: FileFormat) {
        _workspaceState.value = _workspaceState.value.copy(targetFormat = format)
    }

    fun setQualityPreset(preset: QualityPreset) {
        val currentOpts = _workspaceState.value.options
        val newOpts = getOptionsForPreset(preset, currentOpts)
        _workspaceState.value = _workspaceState.value.copy(options = newOpts)
    }

    fun updateOptions(transform: (ConversionOptions) -> ConversionOptions) {
        val current = _workspaceState.value.options
        _workspaceState.value = _workspaceState.value.copy(options = transform(current))
    }

    fun startSingleConversion() {
        val state = _workspaceState.value
        val input = state.inputFormat ?: return
        val target = state.targetFormat ?: return
        val uri = state.selectedFileUri ?: return

        val job = ConversionJob(
            id = UUID.randomUUID().toString(),
            originalName = state.selectedFileName ?: "input_file",
            originalUri = uri,
            originalSizeBytes = state.selectedFileSize,
            inputFormat = input,
            targetFormat = target,
            options = state.options
        )

        _workspaceState.value = state.copy(
            isConverting = true,
            conversionProgress = 0.05f,
            progressStatusText = "Initializing conversion...",
            currentJob = job
        )

        viewModelScope.launch {
            val resultJob = engineManager.processJob(job) { prog, statusText ->
                _workspaceState.value = _workspaceState.value.copy(
                    conversionProgress = prog,
                    progressStatusText = statusText
                )
            }

            _workspaceState.value = _workspaceState.value.copy(
                isConverting = false,
                currentJob = resultJob,
                conversionProgress = 1.0f,
                progressStatusText = if (resultJob.status == JobStatus.COMPLETED) "Completed" else "Failed"
            )

            // Persist into Room
            if (resultJob.status == JobStatus.COMPLETED) {
                historyRepo.recordConversion(
                    ConversionEntity(
                        id = resultJob.id,
                        fileName = resultJob.originalName,
                        inputFormat = resultJob.inputFormat.extension.uppercase(),
                        targetFormat = resultJob.targetFormat.extension.uppercase(),
                        originalSizeBytes = resultJob.originalSizeBytes,
                        resultSizeBytes = resultJob.resultSizeBytes,
                        durationMs = resultJob.durationMs,
                        resultPath = resultJob.resultUri?.toString(),
                        status = "Completed"
                    )
                )
            }
        }
    }

    // --- Batch Processing ---

    fun addBatchFiles(items: List<Triple<Uri, String, Long>>) {
        val currentList = _batchState.value.queue.toMutableList()
        for (item in items) {
            val format = FormatRegistry.detectFormat(item.second)
            val compatible = FormatRegistry.getCompatibleOutputs(format)
            val defaultTarget = FormatRegistry.getRecommendedOutput(format)
            val job = ConversionJob(
                id = UUID.randomUUID().toString(),
                originalName = item.second,
                originalUri = item.first,
                originalSizeBytes = item.third,
                inputFormat = format,
                targetFormat = if (compatible.contains(defaultTarget)) defaultTarget else (compatible.firstOrNull() ?: format),
                options = getOptionsForPreset(defaultPreset.value)
            )
            currentList.add(job)
        }
        _batchState.value = _batchState.value.copy(queue = currentList)
        _currentScreen.value = AppNavScreen.BATCH
    }

    fun removeBatchItem(jobId: String) {
        val updated = _batchState.value.queue.filter { it.id != jobId }
        _batchState.value = _batchState.value.copy(queue = updated)
    }

    fun updateBatchItemTarget(jobId: String, target: FileFormat) {
        val updated = _batchState.value.queue.map {
            if (it.id == jobId) it.copy(targetFormat = target) else it
        }
        _batchState.value = _batchState.value.copy(queue = updated)
    }

    fun applyUniformFormatToBatch(format: FileFormat) {
        val updated = _batchState.value.queue.map { job ->
            val compatible = FormatRegistry.getCompatibleOutputs(job.inputFormat)
            if (compatible.contains(format)) {
                job.copy(targetFormat = format)
            } else {
                job
            }
        }
        _batchState.value = _batchState.value.copy(queue = updated, uniformTargetFormat = format)
    }

    fun startBatchConversion() {
        val queue = _batchState.value.queue
        if (queue.isEmpty() || _batchState.value.isProcessing) return

        _batchState.value = _batchState.value.copy(isProcessing = true, totalProgress = 0f)

        viewModelScope.launch {
            var completedCount = 0
            val total = queue.size

            for (job in queue) {
                if (job.status == JobStatus.COMPLETED) {
                    completedCount++
                    continue
                }

                engineManager.processJob(job) { p, text ->
                    val overallProg = (completedCount.toFloat() + p) / total.toFloat()
                    _batchState.value = _batchState.value.copy(
                        totalProgress = overallProg
                    )
                }
                completedCount++
                _batchState.value = _batchState.value.copy(
                    totalProgress = completedCount.toFloat() / total.toFloat()
                )

                if (job.status == JobStatus.COMPLETED) {
                    historyRepo.recordConversion(
                        ConversionEntity(
                            id = job.id,
                            fileName = job.originalName,
                            inputFormat = job.inputFormat.extension.uppercase(),
                            targetFormat = job.targetFormat.extension.uppercase(),
                            originalSizeBytes = job.originalSizeBytes,
                            resultSizeBytes = job.resultSizeBytes,
                            durationMs = job.durationMs,
                            resultPath = job.resultUri?.toString(),
                            status = "Completed"
                        )
                    )
                }
            }

            _batchState.value = _batchState.value.copy(isProcessing = false)
        }
    }

    fun deleteHistoryRecord(id: String) {
        viewModelScope.launch {
            historyRepo.deleteRecord(id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            historyRepo.clearHistory()
        }
    }

    fun clearAppCache() {
        engineManager.clearCache()
    }

    private fun getOptionsForPreset(
        preset: QualityPreset,
        base: ConversionOptions = ConversionOptions()
    ): ConversionOptions {
        return when (preset) {
            QualityPreset.MAXIMUM -> base.copy(
                preset = preset,
                qualityPercent = 100,
                scalePercent = 100,
                pdfRenderDpi = 300,
                targetLongSidePx = 3840, // 4K
                compressionLevel = 1,
                audioBitrateKbps = 320
            )
            QualityPreset.HIGH -> base.copy(
                preset = preset,
                qualityPercent = 92,
                scalePercent = 100,
                pdfRenderDpi = 220,
                targetLongSidePx = 0,
                compressionLevel = 4,
                audioBitrateKbps = 256
            )
            QualityPreset.BALANCED -> base.copy(
                preset = preset,
                qualityPercent = 85,
                scalePercent = 100,
                pdfRenderDpi = 150,
                targetLongSidePx = 0,
                compressionLevel = 6,
                audioBitrateKbps = 192
            )
            QualityPreset.SMALLER -> base.copy(
                preset = preset,
                qualityPercent = 65,
                scalePercent = 75,
                pdfRenderDpi = 100,
                targetLongSidePx = 0,
                compressionLevel = 9,
                audioBitrateKbps = 128
            )
            QualityPreset.CUSTOM -> base.copy(preset = preset)
        }
    }
}
