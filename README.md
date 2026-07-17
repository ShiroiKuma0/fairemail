<div align="center">

<img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.png" width="120" alt="白い熊 FairEmail icon" />

# 白い熊 FairEmail

**A black-and-yellow FairEmail, tuned for the Huawei Mate XT tri-fold.**

A fork of [FairEmail](https://github.com/M66B/FairEmail) with **major additions**: a fully customisable black/yellow theme with a 28-role colour picker, per-role custom fonts and weights, a folded two-line message subject, and every Pro feature unlocked.

Installs **side-by-side** with the official FairEmail (app id `shiroikuma.fairemail`).

**📥 Latest release: [`1.2325+4`](https://github.com/ShiroiKuma0/fairemail/releases/latest)** — [all releases & APK downloads »](https://github.com/ShiroiKuma0/fairemail/releases)

</div>

---

## 🎨 Custom black-and-yellow theme with a 28-role colour picker

A new **Custom** theme — yellow text on a pure-black background with a gold unread accent — plus a data-driven colour picker in Display settings that exposes **28 colour roles across 7 sections** (Backgrounds, Text, Icons, Message-list accents, Decorations and accents, Status indicators, Highlights). Every surface that FairEmail can reach through its theme is yours to recolour, and the picker walks a single table so the palette stays consistent across the app.

---

## 🔤 Per-role custom fonts and weights

Pick a font file and a forced weight **independently for eight roles** — a Default that cascades to the rest, plus the message-list sender, subject and preview, the message-view subject, sender and body, and the app top-bar title. Weight is a real CSS 100–900 forced through `Typeface.create`, and unread rows get an automatic weight boost so they stay heavier than read mail. Empty roles fall back to the Default font, so one pick can restyle everything.

---

## 📐 Folded two-line subject for the Mate XT

On the narrow folded-portrait state of the tri-fold, an optional mode lets the message-list subject wrap to **two lines with the received date riding the end of line two**, instead of clipping the subject to a single line. The break is measured at bind time (Android has no text float), so short subjects stay on one line and long ones reflow cleanly with the date tucked bottom-right.

---

## 🖤 Black-and-yellow, everywhere

The theme is carried all the way into the floating and chrome surfaces that usually stay grey: **dialogs and popup menus, dropdown spinners, snackbars, and push buttons** all become black with a yellow border, the navigation **drawer gets a yellow edge without dimming the content**, compose-field hints and separators are made legible and tunable, and the launcher wears a custom **black-and-yellow line-traced envelope** icon.

---

## 🧩 Side-by-side, with every Pro feature unlocked

The app id is renamed to `shiroikuma.fairemail` so it coexists with the official build, and it shows as **白い熊 FairEmail** on the home screen. Every Pro feature is unlocked unconditionally — the paid activation gate is removed at the source rather than worked around — and the purchase pitch is stripped from the Pro-features screen, leaving only the activated status and an export-settings shortcut. The fork is versioned as `1.<upstream>+<fork>` (e.g. `1.2325+4`) and the update check is taught to ignore the `+<fork>` suffix so it never falsely flags an "update" back to stock.

---

## Built on FairEmail

A fork of [FairEmail](https://github.com/M66B/FairEmail) by M66B (app id `shiroikuma.fairemail`, so it coexists with the official build). FairEmail is a fully featured, privacy-oriented, open-source email client; all of its mail handling, security and privacy work is upstream's. This fork only layers personal theming, typography and tri-fold ergonomics on top. The code remains under the **GPLv3**.

## Building

```bash
git clone https://github.com/ShiroiKuma0/fairemail.git
cd fairemail
git checkout custom

export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export ANDROID_HOME=$HOME/android-sdk ANDROID_SDK_ROOT=$HOME/android-sdk
./gradlew :app:assembleGithubRelease
```

The signed release build needs a `keystore.properties` at the repo root pointing at your own keystore; the unsigned APK lands in `app/build/outputs/apk/github/release/`.
