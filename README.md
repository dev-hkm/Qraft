# Qraft — Custom QR Code Creator

Native Android (Jetpack Compose) QR Code Studio powered by [QRose](https://github.com/alexzhirkevich/qrose) and [ZXing](https://github.com/zxing/zxing).

## Features

- **12 QR Payload Types**: Website URL, Wi-Fi Pass (WPA/WEP/Open + Hidden SSID), vCard 3.0 Contact, Plain Text, Email Draft, Phone Call, SMS Message, Geo Location, Calendar Event (VEVENT), Social Profile, Payment & Crypto (PayPal, BTC, ETH, SOL, UPI), and App / Deep Links.
- **Full Visual Customization**:
  - **12 Data Pixel Shapes**: Square, Rounded, Dots, Liquid (neighbor-connected), Diamond, Star, Vertical Bars, Horizontal Bars, Plus, Heart, Hexagon, Soft Shield + density & corner radius sliders.
  - **8 Eye Frame Shapes & 8 Eye Ball Shapes**: Sharp, Rounded, Ring, Leaf A/B, Drop, Chamfer, Squircle, Diamond, Star, Cross + 4th Alignment Eye toggle + Square/Circle/Hexagon QR silhouettes + optional light-module grid dots.
  - **Infinite Colors & Gradients**: Interactive HSB color spectrum + Hex input + Solid, Linear (8 directions + 0–360° custom angle), Radial, and Spectrum Sweep gradients, independent colors for Pixels, Eye Frames, Eye Balls, Light Dots, and Canvas Background (Solid, Gradient, Transparent alpha, or Custom Photo with adjustable scrim).
  - **Center Logo Studio**: Gallery photo picker, 24 built-in vector icons, emojis, or 1–4 char monogram badges with 5 clip masks, cutout padding modes (`Natural`, `Accurate`, `Overlay`), backing halo, and border ring.
  - **9 Outer Sticker & Poster Frames**: Apple Card, Scan Banner, Top Tag, Callout, Gradient Ring, Polaroid, Twin Pill, Warm Poster with editable callout labels, colors, corner radius, and border stroke.
- **Live Scannability & Contrast Verification**: Background ZXing test-decode with automatic contrast & ECC diagnostics and 1-tap Auto-Fix.
- **Multi-Format Export & Batch Tools**: Lossless PNG (transparent alpha supported), JPEG, WebP, standalone Vector SVG, Print-ready PDF, Fullscreen max-brightness Scan View, and Batch QR Generator.
- **Apple-Grade Light & Dark Modes + Spring Motion**: Warm cream/espresso light palette and deep obsidian/caramel dark palette with tactile spring interactions.
