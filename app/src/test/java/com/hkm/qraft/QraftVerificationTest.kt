package com.hkm.qraft

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hkm.qraft.data.PresetCatalog
import com.hkm.qraft.model.*
import com.hkm.qraft.qr.QrEngine
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
        val svg = QrEngine.exportSvg("https://qraft.studio", PresetCatalog.builtInPresets.first().design)
        assertTrue(svg.startsWith("<?xml"))
        assertTrue(svg.contains("<svg"))
        assertTrue(svg.contains("</svg>"))
        assertTrue(svg.contains("SCAN ME"))
    }

    @Test
    fun qrEngineRendersAndDecodesPresetsAndCustomLogos() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val testPayload = "https://qraft.studio/verify"

        // Verify first 4 built-in presets decode back identically with ZXing!
        PresetCatalog.builtInPresets.take(4).forEach { preset ->
            val report = QrEngine.verifyScannability(context, testPayload, preset.design)
            assertTrue("Preset ${preset.name} should be scannable", report.isScannable)
            assertEquals(testPayload, report.decodedPreview)
        }

        // Verify embedded monogram logo + custom pixel shape + outer frame renders cleanly
        val customDesign = QrDesignSpec(
            pixelStyle = QrPixelStyle.DIAMOND,
            eyeFrameStyle = QrEyeFrameStyle.CHAMFER,
            eyeBallStyle = QrEyeBallStyle.STAR,
            logo = LogoStyleConfig(
                sourceType = LogoSourceType.MONOGRAM,
                monogramText = "QR",
                sizeFraction = 0.20f
            ),
            outerFrame = OuterFrameConfig(style = OuterFrameStyle.BOTTOM_SCAN_BANNER, label = "TEST"),
            ecc = QrEccSetting.HIGH
        )
        val framed = QrEngine.renderFramedExportBitmap(context, testPayload, customDesign, targetWidthPx = 512)
        assertTrue(framed.width == 512 && framed.height > 512)
        val report = QrEngine.verifyScannability(context, testPayload, customDesign)
        assertTrue("Custom diamond/monogram QR should decode", report.isScannable)
    }

    @Test
    fun mainActivityLaunchesCleanly() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).create().start().resume()
        val activity = controller.get()
        assertNotNull(activity)
        controller.pause().stop().destroy()
    }
}
