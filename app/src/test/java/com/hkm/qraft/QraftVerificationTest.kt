package com.hkm.qraft

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hkm.qraft.data.PresetCatalog
import com.hkm.qraft.data.QraftBackupManager
import com.hkm.qraft.model.*
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
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class QraftVerificationTest {

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
        val defaultDesign = PresetCatalog.builtInPresets.first().design
        assertEquals(4096, defaultDesign.exportSizePx)
        val svg = QrEngine.exportSvg("https://qraft.studio", defaultDesign)
        assertTrue(svg.startsWith("<?xml"))
        assertTrue(svg.contains("<svg"))
        assertTrue(svg.contains("""width="4096""""))
        assertTrue(svg.contains("""fill-rule="evenodd""""))
        assertTrue(svg.contains("</svg>"))
        assertTrue(svg.contains("SCAN ME"))

        QrPixelStyle.entries.forEach { style ->
            val styledSvg = QrEngine.exportSvg(
                "https://qraft.studio/$style",
                defaultDesign.copy(pixelStyle = style, lightPixelStyle = QrLightPixelStyle.DOT)
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

        // Verify true 4K UHD (4096px) export works cleanly without OOM
        val uhd4k = QrEngine.renderFramedExportBitmap(
            context = context,
            data = testPayload,
            design = PresetCatalog.builtInPresets.first().design.copy(
                outerFrame = OuterFrameConfig(
                    style = OuterFrameStyle.DOUBLE_PILL,
                    label = "SCAN ME",
                    subtitle = "4K ULTRA HD"
                )
            ),
            targetWidthPx = 4096
        )
        assertEquals(4096, uhd4k.width)
        assertTrue(uhd4k.height > 4096)
        uhd4k.recycle()

        // Verify Speech Bubble + Warm Poster + Double Pill + Glass Card + Viewfinder + 300 DPI PDF generation
        listOf(
            OuterFrameStyle.SPEECH_BUBBLE,
            OuterFrameStyle.WARM_POSTER,
            OuterFrameStyle.DOUBLE_PILL,
            OuterFrameStyle.GLASS_CARD,
            OuterFrameStyle.CORNER_BRACKET
        ).forEach { frameStyle ->
            val frameDesign = QrDesignSpec(
                pixelStyle = QrPixelStyle.DIAMOND,
                eyeFrameStyle = QrEyeFrameStyle.CHAMFER,
                eyeBallStyle = QrEyeBallStyle.STAR,
                logo = LogoStyleConfig(
                    sourceType = LogoSourceType.MONOGRAM,
                    monogramText = "QR",
                    sizeFraction = 0.20f
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
            assertTrue(bmp.height > 640)
            bmp.recycle()
            val svgDoc = QrEngine.exportSvg(testPayload, frameDesign)
            assertTrue(svgDoc.contains("</svg>"))
        }

        assertEquals(14, PresetCatalog.builtInPresets.size)

        val pdfBytes = QrExporter.renderPdfBytes(
            context = context,
            title = "4K Test Print",
            data = testPayload,
            design = PresetCatalog.builtInPresets.first().design
        )
        val header = String(pdfBytes.take(5).toByteArray())
        assertEquals("%PDF-", header)
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
    fun mainActivityLaunchesCleanly() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).create().start().resume()
        val activity = controller.get()
        assertNotNull(activity)
        controller.pause().stop().destroy()
    }
}
