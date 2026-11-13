package com.hkm.qraft

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hkm.qraft.data.PresetCatalog
import com.hkm.qraft.data.QraftBackupManager
import com.hkm.qraft.model.*
import androidx.compose.ui.test.performClick
import com.hkm.qraft.qr.BarcodeEngine
import com.hkm.qraft.qr.BatchSequenceExpander
import com.hkm.qraft.qr.PaletteExtractor
import com.hkm.qraft.qr.PrintSheetEngine
import com.hkm.qraft.qr.PrintSheetItem
import com.hkm.qraft.qr.PrintSheetLayout
import com.hkm.qraft.qr.QrCodeImporter
import com.hkm.qraft.qr.QrEngine
import com.hkm.qraft.qr.QrExporter
import com.hkm.qraft.qr.QrPayloadEncoder
import com.hkm.qraft.qr.QrPdfWriter
import com.hkm.qraft.ui.screens.HomeScreen
import com.hkm.qraft.ui.screens.LibraryScreen
import com.hkm.qraft.ui.screens.ScannerScreen
import com.hkm.qraft.ui.screens.StudioScreen
import com.hkm.qraft.ui.theme.QraftTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.FileOutputStream

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class QraftVerificationTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun payloadEncoderFormatsAllTypesProperly() {
        assertEquals("https://qraft.studio", QrPayloadEncoder.encode(QrContentType.URL, QrPayloadFields(url = "qraft.studio")))
        assertEquals(
            "WIFI:T:WPA;S:Cafe\\;5G;P:pass\\:123;H:true;;",
            QrPayloadEncoder.encode(
                QrContentType.WIFI,
                QrPayloadFields(wifiSsid = "Cafe;5G", wifiPassword = "pass:123", wifiSecurity = WifiSecurity.WPA, wifiHidden = true)
            )
        )
        val vcard = QrPayloadEncoder.encode(
            QrContentType.VCARD,
            QrPayloadFields(firstName = "Alex", lastName = "Nguyen", phone = "+84901", email = "a@b.c")
        )
        assertTrue(vcard.contains("BEGIN:VCARD") && vcard.contains("FN:Alex Nguyen") && vcard.contains("END:VCARD"))
        assertTrue(QrPayloadEncoder.encode(QrContentType.GEO, QrPayloadFields(latitude = "10.7", longitude = "106.7")).startsWith("geo:10.7,106.7"))
        assertEquals("https://github.com/alex", QrPayloadEncoder.encode(QrContentType.SOCIAL, QrPayloadFields(socialPlatform = SocialPlatform.GITHUB, socialHandle = "@alex")))
        assertEquals("https://paypal.me/qraft/10", QrPayloadEncoder.encode(QrContentType.PAYMENT, QrPayloadFields(paymentMethod = PaymentMethod.PAYPAL, paymentTarget = "qraft", paymentAmount = "10")))
    }

    @Test
    fun svgExporterProducesValidSvgDocument() {
        val baseDesign = PresetCatalog.builtInPresets.first().design.copy(
            outerFrame = OuterFrameConfig(style = OuterFrameStyle.DOUBLE_PILL, label = "SCAN ME"),
            exportSizePx = 8192
        )
        val svg = QrEngine.exportSvg("https://qraft.studio", baseDesign)
        assertTrue(svg.startsWith("<?xml"))
        assertTrue(svg.contains("<svg"))
        assertTrue(svg.contains("""width="8192""""))
        assertTrue(svg.contains("""fill-rule="evenodd""""))
        assertTrue(svg.contains("</svg>"))
        assertTrue(svg.contains("SCAN ME"))

        QrPixelStyle.entries.forEach { style ->
            val styledSvg = QrEngine.exportSvg(
                "https://qraft.studio/$style",
                baseDesign.copy(pixelStyle = style, lightPixelStyle = QrLightPixelStyle.DOT)
            )
            assertTrue("SVG should render pixel style $style", styledSvg.contains("<path d="))
        }
    }

    @Test
    fun qrEngineRendersAndDecodesAllPresetsAt4KAndCustomFrames() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val testPayload = "https://qraft.studio/verify"

        // Verify ALL built-in presets decode cleanly with ZXing
        val failedPresets = mutableListOf<String>()
        PresetCatalog.builtInPresets.forEach { preset ->
            val report = QrEngine.verifyScannability(context, testPayload, preset.design)
            if (!report.isScannable || report.decodedPreview != testPayload) {
                failedPresets += "${preset.name} (contrast=${"%.1f".format(report.contrastRatio)}, warning=${report.warningMessage})"
            }
        }
        assertTrue("Non-scannable presets: $failedPresets", failedPresets.isEmpty())

        // Verify true 8K UHD (8192px) export works cleanly without OOM
        val uhd8k = QrEngine.renderFramedExportBitmap(
            context = context,
            data = testPayload,
            design = PresetCatalog.builtInPresets.first().design.copy(
                outerFrame = OuterFrameConfig(
                    style = OuterFrameStyle.NONE
                )
            ),
            targetWidthPx = 8192
        )
        assertEquals(8192, uhd8k.width)
        assertEquals(8192, uhd8k.height)
        uhd8k.recycle()

        // Verify ALL 16 frame styles render & export SVG cleanly
        OuterFrameStyle.entries.forEach { frameStyle ->
            val frameDesign = QrDesignSpec(
                pixelStyle = QrPixelStyle.OCTAGON,
                eyeFrameStyle = QrEyeFrameStyle.DOUBLE_RING,
                eyeBallStyle = QrEyeBallStyle.HEXAGON,
                logo = LogoStyleConfig(
                    sourceType = LogoSourceType.MONOGRAM,
                    monogramText = "QR",
                    sizeFraction = 0.20f,
                    rotationDeg = 12f,
                    borderWidthFraction = 0.08f
                ),
                outerFrame = OuterFrameConfig(
                    style = frameStyle,
                    label = "SCAN NOW",
                    subtitle = "QRAFT STUDIO"
                ),
                ecc = QrEccSetting.HIGH,
                matrixShadowEnabled = true
            )
            val bmp = QrEngine.renderFramedExportBitmap(
                context = context,
                data = testPayload,
                design = frameDesign,
                targetWidthPx = 640
            )
            assertEquals(640, bmp.width)
            if (frameStyle == OuterFrameStyle.NONE) {
                assertEquals(640, bmp.height)
            } else {
                assertTrue("Frame $frameStyle should extend height", bmp.height >= 640)
            }
            bmp.recycle()
            val svgDoc = QrEngine.exportSvg(testPayload, frameDesign)
            assertTrue(svgDoc.contains("</svg>"))
        }

        assertEquals(30, PresetCatalog.builtInPresets.size)
        assertEquals(16, QrPixelStyle.entries.size)
        assertEquals(11, QrEyeFrameStyle.entries.size)
        assertEquals(10, QrEyeBallStyle.entries.size)
        assertEquals(16, OuterFrameStyle.entries.size)

        val pdfBytes = QrExporter.renderPdfBytes(
            context = context,
            title = "8K Test Print",
            data = testPayload,
            design = PresetCatalog.builtInPresets.first().design
        )
        val header = String(pdfBytes.take(5).toByteArray())
        assertEquals("%PDF-", header)
    }

    @Test
    fun barcodeEngineGeneratesVerifiesAndExportsAllFormats() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals(10, BarcodeFormatType.entries.size)

        // GS1 auto check digit checks
        val ean13Full = BarcodeEngine.normalizeAndValidate(BarcodeFormatType.EAN_13, "400638133393")
        assertTrue(ean13Full.isValid)
        assertEquals("4006381333931", ean13Full.normalizedData)
        val upcAFull = BarcodeEngine.normalizeAndValidate(BarcodeFormatType.UPC_A, "03600029145")
        assertTrue(upcAFull.isValid)
        assertEquals("036000291452", upcAFull.normalizedData)

        BarcodeFormatType.entries.forEach { format ->
            val spec = BarcodeDesignSpec(
                format = format,
                data = format.defaultSample,
                exportWidthPx = 2048,
                showText = true,
                roundedBars = !format.is2D,
                framed = true
            )
            val verification = BarcodeEngine.verifyBarcodeScannable(spec)
            assertTrue("Barcode format $format failed verification: ${verification.warningMessage}", verification.isScannable)

            val bmp = BarcodeEngine.renderBarcodeBitmap(spec, targetWidthPx = 900)
            assertEquals(900, bmp.width)
            bmp.recycle()

            val svg = BarcodeEngine.exportBarcodeSvg(spec)
            assertTrue("SVG invalid for $format", svg.contains("<svg") && svg.contains("</svg>"))
        }

        val barcodePdf = QrExporter.renderBarcodePdfBytes(
            context = context,
            title = "Retail SKU",
            spec = BarcodeDesignSpec(format = BarcodeFormatType.CODE_128, data = "QRAFT-PRO-8K")
        )
        assertEquals("%PDF-", String(barcodePdf.take(5).toByteArray()))
    }

    @Test
    fun qrCodeImporterDecodesAndParsesStructuredPayloads() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val wifiFields = QrPayloadFields(
            wifiSsid = "Studio;5G",
            wifiPassword = "key:secret!",
            wifiSecurity = WifiSecurity.WPA,
            wifiHidden = true
        )
        val wifiEncoded = QrPayloadEncoder.encode(QrContentType.WIFI, wifiFields)
        val wifiBitmap = QrEngine.renderQrCoreBitmap(
            context = context,
            data = wifiEncoded,
            design = PresetCatalog.builtInPresets.first().design,
            sizePx = 512
        )
        val decodedWifi = QrCodeImporter.decodeFromBitmap(wifiBitmap)
        wifiBitmap.recycle()
        assertNotNull(decodedWifi)
        assertEquals(QrContentType.WIFI, decodedWifi!!.type)
        assertEquals("Studio;5G", decodedWifi.fields.wifiSsid)
        assertEquals("key:secret!", decodedWifi.fields.wifiPassword)
        assertTrue(decodedWifi.fields.wifiHidden)

        val (geoType, geoFields) = QrCodeImporter.parsePayload("geo:10.7769,106.7009?q=10.7769,106.7009(Ho%20Chi%20Minh)")
        assertEquals(QrContentType.GEO, geoType)
        assertEquals("10.7769", geoFields.latitude)
        assertEquals("106.7009", geoFields.longitude)
        assertEquals("Ho Chi Minh", geoFields.geoLabel)

        val vcardRaw = QrPayloadEncoder.encode(
            QrContentType.VCARD,
            QrPayloadFields(firstName = "Minh", lastName = "Khanh", organization = "Qraft", phone = "+8490123")
        )
        val (vcType, vcFields) = QrCodeImporter.parsePayload(vcardRaw)
        assertEquals(QrContentType.VCARD, vcType)
        assertEquals("Minh", vcFields.firstName)
        assertEquals("Khanh", vcFields.lastName)
        assertEquals("Qraft", vcFields.organization)
        assertEquals("+8490123", vcFields.phone)
    }

    @Test
    fun paletteExtractorEnforcesScannableContrast() {
        val brandBmp = Bitmap.createBitmap(120, 120, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(brandBmp)
        canvas.drawColor(Color.rgb(255, 210, 140)) // warm light peach
        val accentPaint = Paint().apply { color = Color.rgb(255, 130, 50) } // bright tangerine
        canvas.drawCircle(60f, 60f, 38f, accentPaint)

        val extracted = PaletteExtractor.extractFromBitmap(brandBmp)
        brandBmp.recycle()

        assertTrue(
            "Expected >= 4.5 contrast ratio, got ${extracted.contrastOnCanvas}",
            extracted.contrastOnCanvas >= 4.5
        )
        assertEquals(
            extracted.contrastOnCanvas,
            QrEngine.contrastRatio(extracted.primaryDarkArgb, extracted.lightCanvasArgb),
            0.05
        )
    }

    @Test
    fun viewModelUndoRedoRestoresDraftHistoryCleanly() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = QraftViewModel(app)
        vm.startNewQr(QrContentType.URL)
        assertFalse(vm.canUndo.value)
        assertFalse(vm.canRedo.value)

        val initialPixel = vm.draft.value.design.pixelStyle
        vm.updateDraftDesign(vm.draft.value.design.copy(pixelStyle = QrPixelStyle.HEXAGON))
        assertTrue(vm.canUndo.value)
        assertEquals(QrPixelStyle.HEXAGON, vm.draft.value.design.pixelStyle)

        vm.updateDraftDesign(vm.draft.value.design.copy(pixelStyle = QrPixelStyle.STAR))
        assertEquals(QrPixelStyle.STAR, vm.draft.value.design.pixelStyle)

        vm.undoDesignEdit()
        assertEquals(QrPixelStyle.HEXAGON, vm.draft.value.design.pixelStyle)
        assertTrue(vm.canRedo.value)

        vm.undoDesignEdit()
        assertEquals(initialPixel, vm.draft.value.design.pixelStyle)
        assertFalse(vm.canUndo.value)

        vm.redoDesignEdit()
        assertEquals(QrPixelStyle.HEXAGON, vm.draft.value.design.pixelStyle)
    }

    @Test
    fun printSheetEngineComposes300DpiA4AndMultiPagePdf() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sequence = BatchSequenceExpander.expandSequence(
            prefix = "https://qraft.studio/t-",
            suffix = "",
            start = 1,
            end = 14,
            zeroPad = 2
        )
        assertEquals(14, sequence.size)
        assertEquals("https://qraft.studio/t-01", sequence.first())
        assertEquals("https://qraft.studio/t-14", sequence.last())

        val csvLines = BatchSequenceExpander.parseImportedLines(
            "url,note\n\"https://a.example\",Table 1\nhttps://b.example,Table 2\n"
        )
        assertEquals(listOf("https://a.example", "https://b.example"), csvLines)

        val defaultDesign = PresetCatalog.builtInPresets.first().design
        val sheetItems = sequence.mapIndexed { idx, url ->
            PrintSheetItem(title = "Table ${idx + 1}", payload = url, design = defaultDesign)
        }
        val page1 = PrintSheetEngine.renderPrintSheetPageBitmap(
            context = context,
            items = sheetItems,
            layout = PrintSheetLayout.STICKERS_3X4,
            pageIndex = 0,
            totalPages = 2,
            sheetTitle = "Qraft Batch Labels"
        )
        assertEquals(QrPdfWriter.A4_WIDTH_PX, page1.width)
        assertEquals(QrPdfWriter.A4_HEIGHT_PX, page1.height)
        page1.recycle()

        val pdfBytes = PrintSheetEngine.renderPrintSheetPdfBytes(
            context = context,
            items = sheetItems,
            layout = PrintSheetLayout.STICKERS_3X4,
            sheetTitle = "Qraft MultiPage Print"
        )
        val asciiHeader = String(pdfBytes.take(400).toByteArray(), Charsets.ISO_8859_1)
        assertTrue(asciiHeader.startsWith("%PDF-"))
        assertTrue(asciiHeader.contains("/Count 2"))
    }

    @Test
    fun backupManagerRoundtripsItemsAndPresetsJson() {
        val starter = PresetCatalog.starterItems(now = 1700000000000L)
        val customPreset = SavedStylePreset(
            id = "custom_brand_v4",
            name = "Brand Slate",
            subtitle = "Custom test preset",
            design = PresetCatalog.builtInPresets.last().design,
            isBuiltIn = false
        )
        val json = QraftBackupManager.serialize(
            items = starter,
            customPresets = listOf(customPreset),
            appVersion = "4.0.0"
        )
        assertTrue(json.contains("custom_brand_v4"))

        val restored = QraftBackupManager.deserialize(json)
        assertNotNull(restored)
        assertEquals(starter.size, restored!!.items.size)
        assertEquals(starter.first().id, restored.items.first().id)
        assertEquals(1, restored.customPresets.size)
        assertEquals("Brand Slate", restored.customPresets.first().name)
        assertNull(QraftBackupManager.deserialize("not valid json"))
    }

    @Test
    @Config(qualifiers = "w360dp-h780dp-xhdpi", fontScale = 1.15f)
    fun composeScreensRenderCleanlyInCompactLightAndDarkModes() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val qaDir = (System.getProperty("qraft.qa.dir") ?: System.getenv("QRAFT_QA_DIR"))
            ?.takeIf { it.isNotBlank() }?.let { File(it).apply { mkdirs() } }
        fun savePng(name: String, bmp: Bitmap) {
            qaDir ?: return
            FileOutputStream(File(qaDir, name)).use { fos -> bmp.compress(Bitmap.CompressFormat.PNG, 95, fos) }
        }

        val starterItems = PresetCatalog.starterItems()
        val qrDraft = StudioDraftState(
            editingId = null,
            codeKind = CodeKind.QR,
            title = "Studio Preview v5",
            type = QrContentType.URL,
            payload = QrPayloadFields(url = "https://qraft.studio"),
            design = PresetCatalog.builtInPresets[0].design,
            isFavorite = true
        )
        val barcodeDraft = StudioDraftState(
            editingId = null,
            codeKind = CodeKind.BARCODE,
            title = "Retail SKU",
            barcode = BarcodeDesignSpec(
                format = BarcodeFormatType.EAN_13,
                data = "4006381333931",
                exportWidthPx = 8192
            )
        )

        val darkModeState = androidx.compose.runtime.mutableStateOf(false)
        // 0 = Home, 1 = Studio QR, 2 = Studio Barcode, 3 = Library, 4 = Scanner
        val screenState = androidx.compose.runtime.mutableIntStateOf(0)

        composeRule.setContent {
            val isDark = darkModeState.value
            val scr = screenState.intValue
            QraftTheme(themeMode = if (isDark) ThemeMode.DARK else ThemeMode.LIGHT) {
                when (scr) {
                    0 -> HomeScreen(
                        creatorName = "Budi",
                        themeMode = if (isDark) ThemeMode.DARK else ThemeMode.LIGHT,
                        items = starterItems,
                        selectedCategory = null,
                        onSelectCategory = {},
                        onCycleTheme = {},
                        onOpenSettings = {},
                        onOpenStudioNew = {},
                        onOpenStudioNewBarcode = {},
                        onOpenScanner = {},
                        onOpenExisting = {},
                        onQuickFullscreen = {},
                        onToggleFavorite = {},
                        onOpenLibrary = {},
                        onOpenBatch = {},
                        onOpenCreatePicker = {},
                        onImportQr = {}
                    )
                    1, 2 -> StudioScreen(
                        draft = if (scr == 2) barcodeDraft else qrDraft,
                        customPresets = emptyList(),
                        scannability = ScannabilityReport(
                            isScannable = true,
                            decodedPreview = if (scr == 2) "4006381333931" else "https://qraft.studio",
                            contrastRatio = 19.2,
                            matrixSize = 25,
                            effectiveEcc = "M"
                        ),
                        isVerifying = false,
                        canUndo = true,
                        canRedo = false,
                        onBack = {},
                        onTitleChange = {},
                        onCodeKindChange = {},
                        onTypeChange = {},
                        onFieldsChange = {},
                        onDesignChange = {},
                        onBarcodeChange = {},
                        onPickLogoPhoto = {},
                        onPickBackgroundPhoto = {},
                        onToggleFavorite = {},
                        onApplyPreset = {},
                        onSaveCustomPreset = {},
                        onDeleteCustomPreset = {},
                        onRandomizeStyle = {},
                        onAutoFixScannability = {},
                        onSaveToLibrary = {},
                        onOpenExport = {},
                        onOpenFullscreen = {}
                    )
                    3 -> LibraryScreen(
                        items = starterItems,
                        onBack = {},
                        onOpenExisting = {},
                        onDuplicate = {},
                        onToggleFavorite = {},
                        onDelete = {},
                        onQuickFullscreen = {},
                        onOpenBatch = {},
                        onCreateNew = {},
                        onImportQr = {},
                        onOpenScanner = {}
                    )
                    else -> ScannerScreen(
                        onBack = {},
                        onOpenInStudio = { _, _ -> },
                        onSaveToLibrary = { _, _ -> },
                        onCopyResult = {}
                    )
                }
            }
        }

        composeRule.onNodeWithText("Budi", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Remix").assertIsDisplayed()
        composeRule.onNodeWithText("Batch").assertIsDisplayed()
        savePng("qa_home_light.png", composeRule.onRoot().captureToImage().asAndroidBitmap())

        darkModeState.value = true
        composeRule.waitForIdle()
        savePng("qa_home_dark.png", composeRule.onRoot().captureToImage().asAndroidBitmap())

        screenState.intValue = 1
        darkModeState.value = false
        composeRule.waitForIdle()
        composeRule.onAllNodesWithText("Studio Preview v5").onFirst().assertIsDisplayed()
        savePng("qa_studio_light.png", composeRule.onRoot().captureToImage().asAndroidBitmap())

        // Switch to Shapes tab to verify borderless shape selectors visually!
        composeRule.onNodeWithText("Shapes").performClick()
        composeRule.waitForIdle()
        savePng("qa_studio_shapes_light.png", composeRule.onRoot().captureToImage().asAndroidBitmap())

        darkModeState.value = true
        composeRule.waitForIdle()
        savePng("qa_studio_dark.png", composeRule.onRoot().captureToImage().asAndroidBitmap())

        screenState.intValue = 2
        darkModeState.value = false
        composeRule.waitForIdle()
        composeRule.onAllNodesWithText("Retail SKU").onFirst().assertIsDisplayed()
        savePng("qa_studio_barcode_light.png", composeRule.onRoot().captureToImage().asAndroidBitmap())

        screenState.intValue = 3
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Library").assertIsDisplayed()
        savePng("qa_library_light.png", composeRule.onRoot().captureToImage().asAndroidBitmap())

        screenState.intValue = 4
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Scanner").assertIsDisplayed()
        savePng("qa_scanner_light.png", composeRule.onRoot().captureToImage().asAndroidBitmap())

        if (qaDir != null) {
            val bare = QrEngine.renderFramedExportBitmap(
                context = context,
                data = "https://qraft.studio/v5",
                design = PresetCatalog.builtInPresets[0].design,
                targetWidthPx = 900
            )
            savePng("qa_frame_none.png", bare)
            bare.recycle()

            val stamp = QrEngine.renderFramedExportBitmap(
                context = context,
                data = "https://qraft.studio/v5",
                design = PresetCatalog.builtInPresets.first { it.design.outerFrame.style == OuterFrameStyle.CIRCULAR_STAMP }.design,
                targetWidthPx = 900
            )
            savePng("qa_frame_stamp.png", stamp)
            stamp.recycle()

            val bcBmp = BarcodeEngine.renderBarcodeBitmap(
                spec = barcodeDraft.barcode,
                targetWidthPx = 900
            )
            savePng("qa_barcode_preview.png", bcBmp)
            bcBmp.recycle()
        }
    }

    @Test
    fun mainActivityLaunchesCleanly() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).create().start().resume()
        val activity = controller.get()
        assertNotNull(activity)
        controller.pause().stop().destroy()
    }
}
