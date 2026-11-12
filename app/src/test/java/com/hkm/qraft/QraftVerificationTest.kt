package com.hkm.qraft

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hkm.qraft.data.PresetCatalog
import com.hkm.qraft.model.*
import com.hkm.qraft.qr.QrEngine
import com.hkm.qraft.qr.QrExporter
import com.hkm.qraft.qr.QrPayloadEncoder
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

        // Verify Speech Bubble + Warm Poster + 300 DPI PDF generation
        listOf(
            OuterFrameStyle.SPEECH_BUBBLE,
            OuterFrameStyle.WARM_POSTER,
            OuterFrameStyle.DOUBLE_PILL
        ).forEach { frameStyle ->
            val bmp = QrEngine.renderFramedExportBitmap(
                context = context,
                data = testPayload,
                design = QrDesignSpec(
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
                    ecc = QrEccSetting.HIGH
                ),
                targetWidthPx = 640
            )
            assertEquals(640, bmp.width)
            assertTrue(bmp.height > 640)
            bmp.recycle()
        }

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
    fun mainActivityLaunchesCleanly() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).create().start().resume()
        val activity = controller.get()
        assertNotNull(activity)
        controller.pause().stop().destroy()
    }
}
