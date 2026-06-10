# 🎤 Podium Mic — மேடைப் பேச்சு App

## அம்சங்கள் (Features)

| # | அம்சம் | விளக்கம் |
|---|--------|----------|
| 1 | **Keep Screen On** | App திறந்திருக்கும் வரை திரை அணையாது (FLAG_KEEP_SCREEN_ON) |
| 2 | **Notes View** | கருப்பு background + வெள்ளை எழுத்து (மேடை வெளிச்சத்தில் தெளிவாக தெரியும்) |
| 3 | **Edit Mode** | பேச்சு குறிப்புகளை எழுதி save செய்யலாம் (app மூடினாலும் நினைவில் இருக்கும்) |
| 4 | **A+ / A-** | எழுத்து அளவு 14–72sp |
| 5 | **Auto Scroll** | Teleprompter போல மெதுவாக தானாக scroll ஆகும் |
| 6 | **Mic → Bluetooth** | Phone mic-ல் பேசுவது live-ஆக Bluetooth speaker/amplifier-ல் ஒலிக்கும் |
| 7 | **Gain Slider** | குரல் volume boost 1.0x – 4.0x |
| 8 | **Echo/Noise filter** | Device support இருந்தால் AcousticEchoCanceler + NoiseSuppressor |

## Build செய்வது எப்படி

1. **Android Studio** (Hedgehog அல்லது புதியது) install செய்யுங்கள்
2. `File → Open` → இந்த `PodiumMic` folder-ஐ திறக்கவும்
3. Gradle sync முடியும் வரை காத்திருங்கள்
4. Phone-ஐ USB-ல் இணைத்து (USB Debugging ON) → **Run ▶**
5. அல்லது `Build → Build APK(s)` → APK-ஐ phone-க்கு copy செய்து install

## பயன்படுத்தும் முறை

1. Phone Settings → Bluetooth → உங்கள் **amplifier/speaker-ஐ pair + connect** செய்யுங்கள்
2. App-ஐ திறங்கள் → Edit அழுத்தி பேச்சு குறிப்புகளை type செய்யுங்கள் → Done
3. **🎤 Mic ON** அழுத்துங்கள் (முதல் முறை microphone permission கேட்கும் → Allow)
4. இப்போது நீங்கள் பேசுவது Bluetooth amplifier-ல் கேட்கும்
5. Gain slider-ஐ நகர்த்தி volume சரிசெய்யுங்கள்

## ⚠️ முக்கிய குறிப்புகள்

- **Bluetooth latency**: A2DP-ல் ~100–300ms delay இயல்பு. பெரிய மண்டபத்தில் இது பிரச்சனை இல்லை, ஆனால் phone-க்கு அருகில் நின்றால் சிறு echo போல உணரலாம். Low-latency (aptX LL / LE Audio) speaker என்றால் delay மிகக் குறைவு.
- **Feedback (கீச் சத்தம்)**: Phone mic-ஐ speaker-க்கு நேராக காட்டாதீர்கள். Gain-ஐ அதிகமாக வைத்தால் feedback வரலாம் — 2.0x-ல் தொடங்குங்கள்.
- Bluetooth connect ஆகவில்லை என்றால் ஒலி phone speaker-ல் வரும் (உடனே echo/feedback வரும்) — app warning காட்டும்.
- Mic ON-ல் இருக்கும்போது notification-ல் "Mic LIVE" காட்டும்; screen lock ஆனாலும் service ஓடும்.

## கோப்பு அமைப்பு

```
PodiumMic/
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
└── app/
    ├── build.gradle.kts
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/com/seyad/podiummic/
        │   ├── MainActivity.kt        (Notes UI + Keep Screen On)
        │   └── MicStreamService.kt    (Mic → Bluetooth audio loop)
        └── res/ (layout, strings, theme, icon)
```
