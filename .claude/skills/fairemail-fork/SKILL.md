---
name: fairemail-fork
description: Build, modify, and maintain the user's personal fork of FairEmail (M66B/FairEmail), an Android email client, customised and sideloaded as shiroikuma.fairemail on a Huawei Mate XT. Use this skill ANY time the user wants to modify FairEmail behaviour or appearance, add a feature, rebase onto a newer upstream tag, build/deploy the APK, or troubleshoot the fork. Trigger words include FairEmail, faircode, shiroikuma.fairemail, "my email app", "the fork", "the email client", custom theme/colours, custom font, the message list, or any work in this repository. Covers project identity, the build+deploy pipeline, commit conventions, the upstream rebase procedure, the full feature inventory by commit, architecture/coverage-ceiling notes, and hard-won process lessons.
---

# FairEmail fork (shiroikuma.fairemail)

This is the personal fork of **M66B/FairEmail** (open-source Android email client), customised and sideloaded side-by-side with any official build. Work proceeds as a stack of feature commits on a `custom` branch, rebased onto upstream release tags periodically. This skill is the authoritative record of how the fork is built and maintained.

## Project identity

| Item | Value |
|---|---|
| Upstream | `M66B/FairEmail` (git remote `upstream`) |
| Fork | `ShiroiKuma0/fairemail`, SSH `git@github.com:ShiroiKuma0/fairemail.git` (remote `origin`) |
| Working branch | `custom` (rebased onto an upstream tag) |
| Current base tag | `1.2316` |
| `namespace` | `eu.faircode.email` (unchanged from upstream; Java package stays this) |
| `applicationId` | `shiroikuma.fairemail` (debug variant adds `.debug`) |
| Display name | `FairEmail Custom` |
| Keystore | `~/.android-keystores/fairemail-custom.jks`, alias `fairemail`. Password is NOT in this repo — keep it in `~/.gradle/gradle.properties` or an env var. |
| Build flavor / type | `github` / `release` → task `:app:assembleGithubRelease` |
| Built APK path | `app/build/outputs/apk/github/release/FairEmail-v<tag>a-github-release.apk` |
| Deployed APK names | `~/tmp/shiroikuma-fairemail_<tag>_<datetime>_arm64-v8a.apk` and pushed to `/sdcard/tmp/` on device |
| Toolchain (tag 1.2316) | compileSdk=37, minSdk=23, targetSdk=37, NDK `27.3.13750724` (r27d), AGP/Gradle 9.x, Java toolchain 17, kotlin-android plugin REMOVED. Host build JDK is OpenJDK 21 (`JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64`). |
| Device | Huawei Mate XT tri-fold. Folded-portrait ≈ 1008×2127 px (~366 dp wide); every other fold/orientation state is ≥ ~745 dp. |

## Commit stack on origin/custom

Newest first (short hashes). For the authoritative current list run `git log --oneline <upstream-tag>..origin/custom`.

```
aa80647  Folded message list: optional two-line subject with trailing date
a978594  Custom fonts: expand to independent per-role selection across eight roles
2be8fe0  Custom font picker: defer pref save so the activity recreates cleanly
dc827b2  Add custom font and weight selection for message text
43cb1f5  Custom theme colours: route tvBody link colour through the override
55c057c  Enable Gradle configuration cache
c4abb89  Custom theme colours: expand to 28 roles across 7 sections
90850d0  Pin Pro activation salt to original package id
e01671a  Custom theme colours: refactor picker UI to be data-driven
13878ea  Custom theme: hook XML-resolved colours via Activity-only Resources wrap
90ee296  Custom theme: hook code-resolved colours to user prefs
748c96e  Custom theme: scaffold the customizable colour picker UI
eb2e4dd  Custom theme: unread accent yellow, decouple sender colour, extend font sizes
b08a51e  Add Custom theme: yellow-on-black with gold unread accent
395217d  Customize github flavor for sideloaded shiroikuma.fairemail build
```

## Build + deploy pipeline

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
./gradlew --stop          # clear stale daemon state when gradle.properties / SDK changed
./gradlew clean           # REQUIRED when res/, strings, new files, or SDK changed
./gradlew :app:assembleGithubRelease
```

Deploy to two targets, never auto-install:

```bash
build_apk=app/build/outputs/apk/github/release/FairEmail-v1.2316a-github-release.apk
apk_name="shiroikuma-fairemail_1.2316_$(date '+%Y-%m-%d_%H-%M-%S')_arm64-v8a.apk"
cp "$build_apk" ~/tmp/$apk_name
adb shell rm -f '/sdcard/tmp/shiroikuma-fairemail_*.apk'    # wipe stale APKs on device
adb push "$build_apk" /sdcard/tmp/$apk_name
```

### Build verification is MANDATORY

Before telling the user the APK is ready:

1. Confirm the APK **mtime is current** (`ls -lh "$build_apk"`). Incremental Gradle can no-op and `cp` then ships a STALE APK under a fresh filename — mtime betrays it (stays at the prior build time).
2. **Integrity probe** against the compiled resources for a string/id the change introduced:
   ```bash
   unzip -p "$build_apk" resources.arsc | strings | grep -c "<new-string-or-id>"
   ```
   `> 0` means the new resources were actually packaged. `javac` errors print in the host locale (Japanese: `エラー:`), so grep build output for `error:|エラー:|FAILED`.

## Workflow conventions

- **One feature = one commit.** No bundled changes.
- **"Push." gates push.** Make the change, build, deploy, let the user verify. Commit + `git push origin custom` ONLY after the user types "Push." Never auto-push.
- **Commit messages**: prose, ~72-column wrap, NO apostrophes (rephrase: "does not" not "doesn't"). Describe the problem, the root cause, and the mechanism. No conventional-commit prefixes; the FairEmail/M66B style is prose.
- **Never auto-stage with `git add -A` if untracked artifacts may be present.** Stage by path or verify the staged diff with `git diff --cached --stat` and grep for `\.(apk|aab|so)` before committing.

## Upstream rebase procedure

Periodically rebase `custom` onto a newer upstream tag (last done 1.2315 → 1.2316):

1. `git fetch upstream --tags`.
2. Create a safety branch: `git branch custom-pre-<newtag>-rebase`.
3. `git rebase <newtag>` and resolve conflicts. The recurring conflict is `app/build.gradle` `defaultConfig`: **keep** `applicationId "shiroikuma.fairemail"`, **take upstream** SDK/NDK/Java/Gradle bumps.
4. Build + verify, then `git push --force-with-lease origin custom`.
5. Delete the safety branch once confirmed.

## Feature inventory (what each layer does)

### Custom theme colours (commits b08a51e … c4abb89, e01671a, 43cb1f5)
User-customisable colour picker in Display settings, **28 roles across 7 sections** (Backgrounds, Text, Icons, Message list accents, Decorations and accents, Status indicators, Highlights). Data-driven from `CustomThemeColors.ENTRIES`; `FragmentOptionsDisplay.populateCustomColorPicker()` walks the table. Adding a role is a 4-place edit: `colors_custom.xml` resource, an `ENTRIES` row, 2 strings, and `AppThemeCustom`/`actionBarStyleCustom` routing.

**Coverage ceiling (important):** colours are resolved through a `ColorOverrideResources extends Resources` wrapper installed per-Activity. It catches `getColor` and drawable-XML paths but NOT layout-XML `?attr/...` backgrounds nor inflation-time `linkTextColor`/widget-tint reads, because `TypedArray.getDrawable`/`getColorStateList` call package-private `Resources.loadDrawable`/`loadColorStateList` directly. The systemic fix would need a `LayoutInflater.Factory2`; deferred. The **targeted workaround pattern** for any widget whose colour/font is read at inflation: reapply it programmatically at bind time using an override-aware value (e.g. `tvBody.setLinkTextColor(...)` in commit 43cb1f5, body links).

**Gotcha:** `setTextAppearance(R.style.TextAppearance_AppCompat_*)` does NOT compile (symbols not in the project R class) — use `setTextSize` + `setTypeface` instead when building picker UI programmatically.

### Pro activation salt fix (commit 90850d0)
The github flavor `ActivityBilling.getResponse()` computes `sha256(BuildConfig.APPLICATION_ID + sha256(ANDROID_ID))`. The renamed `applicationId` broke activation (M66B's server signed against `eu.faircode.email`). Fix: pin the salt to the literal `"eu.faircode.email"`. The challenge stays per-device/per-signing-key (non-transferable). Do not revert on rebase — there is a warning comment at the site.

### Gradle configuration cache (commit 55c057c)
`org.gradle.configuration-cache=true` in `gradle.properties`. Read at daemon startup, so run `./gradlew --stop` after changing it. If a future task is incompatible, soften with `org.gradle.configuration-cache.problems=warn`.

### Custom font + weight — single, then per-role (commits dc827b2, 2be8fe0, a978594)
`CustomFont.java` is the core. **Role-based** across **8 roles in 4 sections**: Default (the fallback/cascade source) plus Message list (sender, subject, preview), Message view when reading (subject, sender, body), and App chrome (top bar title). General-UI beyond the top bar needs a `LayoutInflater.Factory2` hook and is deferred.

- **Font cascade:** a role with an empty font pref falls back to the Default role's font; if Default is also empty, no override.
- **Weight is independent per role, no cascade:** 0 = the typeface's natural weight, slider 1–9 = forced CSS weight 100–900 via `Typeface.create(tf, weight, italic)` (API 28+). Bold from the unread state maps to a +300 weight boost (capped 900) so unread rows stay heavier.
- **Pref keys:** Default keeps the legacy bare names (`custom_font_path`, `custom_font_name`, `custom_font_weight`) so existing picks survive; other roles use `_<role>` suffixes. Per-role file slots under `filesDir/custom_fonts/<role>.ttf` (Default = `picked.ttf`).
- **Picker UI:** built programmatically in `FragmentOptionsDisplay.populateCustomFontPicker()` from `CustomFont.ENTRIES`, one shared `ActivityResultLauncher` dispatched by a `pendingFontRole`.
- **Migration:** `CustomFont.migrateLegacyWeightIfNeeded()` (called in `ApplicationEx.onCreate`, idempotent via `custom_font_migrated_v2`) copies the old single global weight to each non-Default role so "bold everywhere" survives the refactor.
- **Apply convention:** `CustomFont.apply(ctx, view, role)` is called at the **bind site, AFTER** the existing `setTypeface` calls, so it preserves the italic/bold style flags those set. Bind sites in `AdapterMessage`: `tvFrom`→LIST_SENDER, `tvSubject`→LIST_SUBJECT, `tvPreview`→LIST_PREVIEW, `tvFromEx`→VIEW_SENDER, `tvSubjectEx`→VIEW_SUBJECT, `tvBody`→VIEW_BODY. `ActivityBase.onResume()` walks the `R.id.toolbar` TextView children for TOP_BAR (after `visible=true`).

### Folded two-line subject (commit aa80647)
Display toggle `subject_lines_narrow` (off by default, in Display options next to "Show subject above sender", in the reset list). When ON and the screen is narrow, the compact message-list subject uses two lines with the date at the end of line 2, instead of clipping to one line in folded-portrait.

- **Narrow detection:** `configuration.screenWidthDp < 500` (constant `FOLDED_WIDTH_DP`). Isolates Mate XT folded-portrait (~366 dp) from all wider states. `ActivityView` has no `configChanges`, so it RECREATES on fold → holders rebuilt → width re-evaluated; no recycle-time reset needed.
- **Why measurement is required:** a single TextView is a rectangle — it cannot render line 1 at full width while line 2 stops short for a bottom-right date (Android has no text float). So the break is computed at bind time. Constructor: subject laid out full width (`endToStart = R.id.ibFlagged`), maxLines=2; date (`tvTime`) and size (`tvSize`) anchored to the subject's bottom via `anchorToSubjectBottom()` (bottom_toBottom = subject, top constraints UNSET) so they ride to the end of the last subject line.
- **The split** (`AdapterMessage.applyTwoLineSubject()`, in `tvSubject.post(...)` so width/Layout are valid): short/medium subject that fits on one line stays one line (truncated only to clear the date); a long subject keeps line 1 exactly as the full-width Layout wrapped it (`layout.getLineEnd(0)`) and re-flows the remainder onto line 2, ellipsized to `width − dateWidth − gap`. Idempotent and recycle-safe: the runnable acts only while the view still shows the original un-split text (`TextUtils.equals`).
- **Known minor:** the split lands one frame after bind, so a fast scroll may show the pre-split layout for a frame. Move to `OnPreDrawListener` if it ever bothers the user.

## Architecture / coverage-ceiling summary

- **Inflation-time reads are unreachable** by the `Resources` wrapper (colours) and by simple constructor setup (fonts): `?attr` layout backgrounds, `linkTextColor`, widget tints, and general-UI typefaces. The clean systemic fix for all of them is a `LayoutInflater.Factory2`; until then use the **bind-time reapply** workaround per widget.
- **`subject_top` swaps the view IDs**: the adapter's `tvSubject` field always holds the subject text but points to `R.id.tvFrom` (when `subject_top`) or `R.id.tvSubject`. The `tvFrom` field holds the sender. Date is `tvTime`, default-aligned to `R.id.tvFrom`. Any message-row layout work must account for this swap.

## Open / deferred

- **General-UI fonts** (menus, settings, dialogs, AlertDialogs). Needs `LayoutInflater.Factory2`. Would also knock out the colour inflation-time coverage ceiling in one go (the two problems share the same fix).
- **Subject-split flash on fast scroll** in folded two-line mode. Move from `tvSubject.post(...)` to `OnPreDrawListener` to render the split before the first paint instead of one frame after.

## Lessons (process — these all bit us in real sessions)

1. **Verify a push actually moved HEAD before building the next feature.** A "Push" that did not complete left HEAD unmoved while the working tree held the changes; later steps got built on a base that never landed and a `git checkout --` then reverted half of it into a non-compiling frankenstein. After any push: `git fetch origin && git log --oneline -1` and confirm the hash/subject.
2. **Verify every newly-referenced type is imported.** When editing Java, an added reference to `TextView` etc. without the matching `import` fails the build. Quick check: for each symbol the patch uses, `grep -c "import .*\.<Symbol>;" <file>`. Run a build before claiming success.
3. **A build can silently ship the previous APK.** Incremental Gradle no-ops, `cp` copies the old artifact under a new filename. ALWAYS verify APK mtime is current AND run the `resources.arsc` integrity probe before deploying. Wipe `/sdcard/tmp/shiroikuma-fairemail_*.apk` before pushing so the user cannot tap a stale one.
4. **SAF / ActivityResult callbacks fire inside `super.onResume()`** before `ActivityBase` sets `visible=true`. Saving a pref there triggers `onSharedPreferenceChanged` synchronously, which calls `finish()` and skips the relaunch because `visible` is still false — the app appears to vanish. Defer any pref save from such a callback with `Handler(Looper.getMainLooper()).post(...)` (see `FragmentOptionsDisplay.onFontPicked`). In-process dialogs (e.g. the colour picker) are NOT affected.
5. **`./gradlew clean` whenever res/strings/new-files/SDK changed**, and `./gradlew --stop` after touching `gradle.properties`.
