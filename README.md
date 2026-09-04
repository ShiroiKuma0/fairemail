<div align="center">

<img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.png" width="120" alt="白い熊 FairEmail icon" />

# 白い熊 FairEmail

**A black-and-yellow FairEmail, tuned for the Huawei Mate XT tri-fold.**

A fork of [FairEmail](https://github.com/M66B/FairEmail) with **major additions**: a fully customisable black/yellow theme with a 28-role colour picker, per-role custom fonts and weights, a dedicated 白い熊 FairEmail UI page with one-tap export/import of everything including the mail store, a headless automation export plus a signature-pinned data door that lets a backup app take this app away with its mail, a folded two-line message subject, and every Pro feature unlocked.

Installs **side-by-side** with the official FairEmail (app id `shiroikuma.fairemail`).

**📥 Latest release: [`1.2333+004`](https://github.com/ShiroiKuma0/fairemail/releases/latest)** — [all releases & APK downloads »](https://github.com/ShiroiKuma0/fairemail/releases)

</div>

---

## 🎨 Custom black-and-yellow theme with a 28-role colour picker

A new **Custom** theme — yellow text on a pure-black background with a gold unread accent — plus a data-driven colour picker on the 白い熊 FairEmail UI page that exposes **28 colour roles across 7 sections** (Backgrounds, Text, Icons, Message-list accents, Decorations and accents, Status indicators, Highlights). Every surface that FairEmail can reach through its theme is yours to recolour, and the picker walks a single table so the palette stays consistent across the app.

---

## 🔤 Per-role custom fonts and weights

Pick a font file and a forced weight **independently for eight roles** — a Default that cascades to the rest, plus the message-list sender, subject and preview, the message-view subject, sender and body, and the app top-bar title. Weight is a real CSS 100–900 forced through `Typeface.create`, and unread rows get an automatic weight boost so they stay heavier than read mail. Empty roles fall back to the Default font, so one pick can restyle everything.

---

## 📦 One page for everything: 白い熊 FairEmail UI

A dedicated, kxkb-styled settings page — reachable by **long-pressing either toolbar hamburger**, left or right — gathers the whole fork under text-wide underlined headings separated by hairline rules: the colour picker, the font picker, and a **Kōjiki-style export/import of every settable item**. Pick an export directory once and the page always greets you with your latest export; the panel exports and imports by category (accounts and identities, rules, contacts, local emails, templates, searches, notification channels, app settings, UI customization) behind round pill buttons — Cancel left, Import and Export right — and a success flow that closes the whole chain in one tap. It replaces the stock Backup tab outright.

A backup is **one ZIP**, `shiroikuma-fairemail_<timestamp>.zip`, and inside it the export is still **stock-format JSON that even carries the font binaries** — so an unzipped backup remains importable by the official FairEmail, and the importer takes the ZIP, an older bare-JSON export, or a stock upstream backup alike. Ticking **local emails** (off by default, since a mail store can run to gigabytes) folds the whole message store into that same single file: bodies, raw MIME where it was downloaded, and every attachment payload. Restores match by account UUID and folder name, so mail lands on accounts that already exist, and a message already present is skipped rather than duplicated.

---

## 🤖 Headless automation export

The app answers an **intent contract** shared by 白い熊's sister apps, so one automation task can back the whole family up in a single run. An exported receiver takes `EXPORT_STATE`, `LIST_CATEGORIES` and `CANCEL_EXPORT`, runs the export with no Activity and no interaction, streams **progress with real counts** (`メール 1234/8942`, never a percentage), and replies with the path it wrote, the true byte length, a human-readable size and the category count. The category list states **which items start ticked**, so the caller's picker — redrawn from that reply every time — proposes what this app recommends rather than its whole footprint, and an export that names no items takes exactly that set. The **Automation export** switch ships **on**, so the app is on the batch out of the box; an authorization token is an optional extra you can ask callers for rather than the gate, and one sent when it is not required is ignored rather than refused.

A long export can be **called off from where it was started**: the cancel flips a flag the export tests at entry, account, message and block boundaries, so it unwinds at the next one instead of being torn down mid-write. Every backup, headless or from the panel, is written to `<name>.part` and renamed only once the archive is closed and complete — so a cancelled or failed export leaves the backup directory **exactly as it found it**, with no short archive to be mistaken for the latest good one.

---

## 🔐 A data door that survives a wiped phone

A second, separate surface lets a backup app take this app away **with its mail** and put it back on a clean phone. It is a `ContentProvider` rather than another broadcast, for one reason: a broadcast cannot tell you who sent it, and the caller is the party that names where the archive goes. Here the framework supplies the identity and it is checked three ways — an **exact package name** and never a prefix, the **uid the kernel reports**, and a **pinned signing certificate** — because a package name absent from a device is a name anyone can take, and a freshly wiped phone is exactly such a device.

The archive never crosses the call. The caller opens its own destination and passes a **file descriptor**; this app writes bytes into it and nothing else, so the backup app can rename, encrypt and checksum a file it owns from start to finish. The work runs in a foreground service with a wakelock, because a backgrounded app writing for minutes is frozen mid-stream on EMUI and would hand back a truncated archive underneath a success reply — the one failure indistinguishable from a good backup until the day you need it. Restore is **only** here and has no intent, because an import overwrites every account and every message.

The header the door answers with says plainly what a backup holds — including that the accounts category carries every account and identity password, and for an OAuth account the live token, in the clear — so you know which before you choose rather than after.

---

## 📐 Folded two-line subject for the Mate XT

On the narrow folded-portrait state of the tri-fold, an optional mode lets the message-list subject wrap to **two lines with the received date riding the end of line two**, instead of clipping the subject to a single line. The break is measured at bind time (Android has no text float), so short subjects stay on one line and long ones reflow cleanly with the date tucked bottom-right.

---

## 🖤 Black-and-yellow, everywhere

The theme is carried all the way into the floating and chrome surfaces that usually stay grey: **dialogs and popup menus, dropdown spinners, snackbars, and push buttons** all become black with a yellow border, the navigation **drawer gets a yellow edge without dimming the content**, compose-field hints and separators are made legible and tunable, and the launcher wears a custom **black-and-yellow line-traced envelope** icon.

---

## 🧩 Side-by-side, with every Pro feature unlocked

The app id is renamed to `shiroikuma.fairemail` so it coexists with the official build, and it shows as **白い熊 FairEmail** on the home screen. Every Pro feature is unlocked unconditionally — the paid activation gate is removed at the source rather than worked around — and the purchase pitch is stripped from the Pro-features screen, leaving only the activated status and an export-settings shortcut. The fork is versioned as `1.<upstream>+<fork>`, with the fork build number zero padded to three digits (e.g. `1.2333+004`) so builds sort in build order, and the update check is taught to ignore the `+<fork>` suffix so it never falsely flags an "update" back to stock.

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
