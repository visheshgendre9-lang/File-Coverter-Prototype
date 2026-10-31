package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.engine.FormatRegistry
import com.example.model.FileCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("OmniConvert", appName)
    }

    @Test
    fun `test format detection and compatibility`() {
        val pngFormat = FormatRegistry.detectFormat("photo.png")
        assertEquals(FormatRegistry.PNG, pngFormat)
        assertEquals(FileCategory.IMAGE, pngFormat.category)

        val compatibleOutputs = FormatRegistry.getCompatibleOutputs(pngFormat)
        assertTrue(compatibleOutputs.contains(FormatRegistry.WEBP))
        assertTrue(compatibleOutputs.contains(FormatRegistry.JPG))

        val recommended = FormatRegistry.getRecommendedOutput(pngFormat)
        assertNotNull(recommended)
    }

    @Test
    fun `test voice enhancer engine processing`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val samples = com.example.engine.SampleAssetsProvider.getSampleFiles()
        val voiceSample = samples.first { it.name.contains("voice") }
        val uri = voiceSample.uriProvider(context)
        assertNotNull(uri)

        val outputDir = context.cacheDir
        val config = com.example.engine.VoiceEnhanceConfig(
            denoiseIntensity = 0.8f,
            clarityBoost = 0.7f,
            removeLowRumble = true,
            removeHighHiss = true,
            normalizeSpeech = true
        )
        val enhancedFile = com.example.engine.VoiceEnhancerEngine.enhanceVoice(
            context = context,
            inputUri = uri,
            config = config,
            outputDir = outputDir,
            onProgress = { _, _ -> }
        )
        assertTrue(enhancedFile.exists())
        assertTrue(enhancedFile.length() > 0)
    }

    @Test
    fun `test markdown document conversion`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val samples = com.example.engine.SampleAssetsProvider.getSampleFiles()
        val mdSample = samples.first { it.name.endsWith(".md") }
        val uri = mdSample.uriProvider(context)
        assertNotNull(uri)

        val outputDir = context.cacheDir
        val convertedFile = com.example.engine.DocumentEngine.convertDocument(
            context = context,
            inputUri = uri,
            inputFormat = FormatRegistry.MD,
            targetFormat = FormatRegistry.HTML,
            options = com.example.model.ConversionOptions(),
            outputDir = outputDir,
            onProgress = {}
        )
        assertTrue(convertedFile.exists())
        val htmlContent = convertedFile.readText()
        assertTrue(htmlContent.contains("<!DOCTYPE html>"))
        assertTrue(htmlContent.contains("OmniConvert"))
    }

    @Test
    fun `test 8k image upscaling and conversion`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val samples = com.example.engine.SampleAssetsProvider.getSampleFiles()
        val imageSample = samples.first { it.name.endsWith(".png") }
        val uri = imageSample.uriProvider(context)
        assertNotNull(uri)

        val outputDir = context.cacheDir
        val options = com.example.model.ConversionOptions(
            preset = com.example.model.QualityPreset.ULTRA_8K,
            targetLongSidePx = 7680,
            qualityPercent = 100,
            upscaleSharpen = true
        )

        val resultFile = com.example.engine.ImageEngine.convertImage(
            context = context,
            inputUri = uri,
            targetFormat = FormatRegistry.JPG,
            options = options,
            outputDir = outputDir,
            onProgress = {}
        )

        assertTrue(resultFile.exists())
        assertTrue(resultFile.length() > 0)
    }
}
