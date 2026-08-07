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
| Current base tag | `1.2328` |
| `namespace` | `eu.faircode.email` (unchanged from upstream; Java package stays this) |
| `applicationId` | `shiroikuma.fairemail` (debug variant adds `.debug`) |
| Display name | `白い熊 FairEmail` (github flavor `app_name` in `app/src/github/res/values/strings.xml`) |
| Versioning | `versionName` = `1.<upstream>+<fork>`, fork number zero padded to three digits (e.g. `1.2328+002`); `versionCode` = `<upstream> * 10000 + <fork>` (e.g. `23280002`, unpadded arithmetic). The `getForkBuild` literal in `app/build.gradle` is the fork build number — reset to **1** on every upstream rebase, **+1** on every subsequent local build. `getVersionCode()` keeps returning the bare upstream code (it feeds archivesName/changelog/signature paths; do not repurpose it). |
| Keystore | `~/.android-keystores/fairemail-custom.jks`, alias `fairemail`. Password is NOT in this repo — keep it in `~/.gradle/gradle.properties` or an env var. |
| Build flavor / type | `github` / `release` → task `:app:assembleGithubRelease` |
| Built APK path | `app/build/outputs/apk/github/release/FairEmail-v<tag>a-github-release.apk` |
| Deployed APK names | `shiroikuma-fairemail_<versionName>_arm64-v8a.apk` (e.g. `shiroikuma-fairemail_1.2328+002_arm64-v8a.apk`), copied to `~/tmp/` AND pushed to `/sdcard/tmp/`. No datetime — matches the user's other sideloaded apps (denwa, futokxkb, simplex): `shiroikuma-<app>_<upstream>+<fork>_arm64-v8a.apk`, fork number zero padded to three digits so the shared directories sort in build order. |
| Toolchain (tag 1.2328) | compileSdk=37, minSdk=23, targetSdk=37, NDK `27.3.13750724` (r27d), AGP/Gradle 9.x, Java toolchain 21 (upstream bumped 17→21 at 1.2317), kotlin-android plugin REMOVED. Host build JDK is OpenJDK 21 (`JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64`). SDK lives at `$HOME/android-sdk`; export `ANDROID_HOME`/`ANDROID_SDK_ROOT` for builds (set in the user's `.bashrc` but NOT in non-interactive shells, and no `local.properties` is committed). |
| Device | Huawei Mate XT tri-fold. Folded-portrait ≈ 1008×2127 px (~366 dp wide); every other fold/orientation state is ≥ ~745 dp. |

## Commit stack on origin/custom

Newest first (short hashes). For the authoritative current list run `git log --oneline <upstream-tag>..origin/custom`.

```
2c793f8fc5  Publish 1.2328+002: refresh changelog and README for the 1.2328 rebase
e0b2f78c21  Bump the fork build number to 2
45bc8636c9  Zero pad the fork build number in the version name
713b6aef0f  Refresh fork skill and CLAUDE docs for the 1.2327 rebase
b1988058a1  Publish 1.2327+1: refresh changelog and README for the 1.2327 rebase
681f27b878  Reset the fork build number for the 1.2327 rebase
46dd36bb4c  Publish 1.2326+8: refresh changelog and README for the cancel work
73c57dac64  Bump the fork build number to 8
75d6a9cb2b  Stop a running export, leaving no partial file behind
c4ad45f4b2  Say which backup categories start ticked
8292cad984  Publish 1.2326+7: refresh changelog and README for the backup work
46f2fd3c08  Bump the fork build number to 7
b1d826d373  Answer the sister app state export automation contract
b75be0d9f1  Add the automation token gate and its Export Import rows
9a5da02af5  Export the local mail store as an opt-in backup category
ed85942617  Write backups as one ZIP named by the sister-app convention
0525504b48  Publish 1.2326+5: refresh changelog and README for the UI page
ec772d93c6  Bump the fork build number to 5
641283b3d2  Custom theme: black toolbar overflow menu with a yellow border
936c4f76c8  Open the UI page by long-pressing the toolbar hamburger buttons
82ffa5ddbe  Add the 白い熊 FairEmail UI page with export/import replacing Backup
2d92c9f67e  Publish 1.2326+1: refresh changelog and README for the 1.2326 rebase
9192bb881c  Publish 1.2325+4: refresh README and changelog for the pro unlock
9018edbbdc  Hide the purchase section on the pro features screen
4940542081  Unlock all pro features unconditionally
35bf1b5a1c  skills: document the merged changelog workflow
e1929b051e  Add a merged fork changelog to CHANGELOG.md
5e7ecd89c5  Add a fork README for the GitHub release
795df5d7fa  Custom theme: black dropdown spinners with a yellow border
f2c58c4a92  skills: auto-deliver builds via /after-build (drop the transfer prompt)
ad9a8dab44  Make compose field hints and separators legible and tunable
f8e59182a6  docs: no attribution trailer in commits
e56ba26c51  Refresh fork skill and CLAUDE docs for the 1.2326 rebase
464330f93b  Custom launcher icon: black-yellow line-traced envelope
c9eaf995f0  Skill: never delete old APKs on the device when deploying
6f706d6abf  Custom theme: yellow drawer border without dimming the content
b86dcc501c  Custom theme: black push buttons with a yellow border
56415976a8  Custom theme: black snackbars with a yellow border and yellow text
783bc8f687  Custom theme: black dialogs and popup menus with a yellow border
e2e8859b3b  Add upstream-new-version skill to drive the upstream rebase and build
d684915005  kxkb: document fork versioning and label, refresh skill for 1.2318
a561d39b84  Rename the sideloaded app label to 白い熊 FairEmail
876ee05657  Version the fork as the upstream version plus a local build number
c08d3fc004  kxkb: add agent config (CLAUDE.md + .claude/skills/)
40ffea5030  Folded message list: optional two-line subject with trailing date
4a66abd393  Custom fonts: expand to independent per-role selection across eight roles
e8e8b9259d  Custom font picker: defer pref save so the activity recreates cleanly
31b33e77a7  Add custom font and weight selection for message text
09c0b1046b  Custom theme colours: route tvBody link colour through the override
d9f165e738  Custom theme colours: expand to 28 roles across 7 sections
6470bf4a12  Custom theme colours: refactor picker UI to be data-driven
3cc205ab5c  Custom theme: hook XML-resolved colours via Activity-only Resources wrap
bafa4b1240  Custom theme: hook code-resolved colours to user prefs
7f80ca93a7  Custom theme: scaffold the customizable colour picker UI
d4fbda7757  Custom theme: unread accent yellow, decouple sender colour, extend font size range
af12a755da  Custom theme polish: swap subject/sender highlight target, never bold sender, add sender_italic toggle, accent yellow
3c34b08925  Add Custom theme: yellow-on-black with gold unread accent
996c100170  Customize github flavor for sideloaded shiroikuma.fairemail build
```

## Build + deploy pipeline

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export ANDROID_HOME=$HOME/android-sdk ANDROID_SDK_ROOT=$HOME/android-sdk   # not set in non-interactive shells
./gradlew --stop          # clear stale daemon state when gradle.properties / SDK changed
./gradlew clean           # REQUIRED when res/, strings, new files, or SDK changed
./gradlew :app:assembleGithubRelease
```

Copy to `~/tmp/`, then deliver via `/after-build` — never auto-install:

```bash
build_apk=app/build/outputs/apk/github/release/FairEmail-v1.2328a-github-release.apk   # archivesName uses the bare upstream code
version=1.2328+002                                          # versionName: 1.<upstream>+<fork>, fork padded to 3 digits (bump <fork> each local build)
apk_name="shiroikuma-fairemail_${version}_arm64-v8a.apk"
cp "$build_apk" ~/tmp/$apk_name
```
Then invoke the global `/after-build` skill: it runs `/adb-check` UNSANDBOXED, then `/adb-push` to `/sdcard/tmp/` if the phone is connected, else `/scp` to skhw, and announces the filename — never prompt "is the phone connected?", `/adb-check` answers it. (Old `/sdcard/tmp/shiroikuma-fairemail_*.apk` are never wiped — prior builds stay in place.)

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

Periodically rebase `custom` onto a newer upstream tag (last done 1.2327 → 1.2328):

1. `git fetch upstream --tags`.
2. Create a safety branch: `git branch custom-pre-<newtag>-rebase`.
3. `git rebase <newtag>` and resolve conflicts. Recurring spots: `app/build.gradle` (**keep** `applicationId "shiroikuma.fairemail"` and the `getForkBuild` versioning lines; **take upstream** `getVersionCode`/SDK/NDK/Java/Gradle bumps); `CHANGELOG.md` (and its build-copied twin `app/src/main/assets/CHANGELOG.md`) when upstream prepends a new version block — **keep our fork section above the `---` divider, take upstream's new `### 1.NNNN` block below it**; the fork section sits above `## Changelog` precisely so this stays a clean three-way merge most rebases (see the Merged changelog note in the feature inventory); plus `fragment_options_display.xml` and `AdapterMessage.java` when upstream reshuffles the display options or message-row bind code (1.2317 dropped the `tvSenderEllipsizeRemark`/`tvSubjectEllipsizeRemark` hints and flattened the subject single-line block — re-anchor our `swSenderItalic` switch to `spSenderEllipsize`). The 1.2322 → 1.2324 rebase (25 upstream commits, skipping the 1.2323 tag) had a single real conflict, the `getVersionCode` 2322→2324 bump in `app/build.gradle`. Upstream 1.2324 added `org.gradle.configuration-cache=true` to `gradle.properties` itself, the exact line our `Enable Gradle configuration cache` commit introduced, so that fork commit was auto-skipped as an already-applied cherry-pick. `ActivityView.java`, `ActivityBase.java`, `AdapterMessage.java`, `ApplicationEx.java`, and `Helper.java` were all in the overlap set but auto-merged cleanly (verify the fork hooks survived after such an auto-merge: the `CustomFont.apply` calls, the two-line-subject logic, the `swSenderItalic` anchor, the `migrateLegacyWeightIfNeeded` call, the colour-override reads, and the `ActivityView` update-check `+<fork>` suffix strip). The 1.2327 → 1.2328 rebase (16 upstream commits, all housekeeping: Crowdin sync, S/MIME roots, PSL, Brave debounce list, the Thundermail provider, a Gemini model-name fix, a VPN list button, an NPE guard, a display-cutout inset and default medium spacing) again had exactly one conflict, the `getVersionCode` 2327→2328 bump. `ActivityBase.java`, `ApplicationEx.java`, `FragmentOptionsDisplay.java`, `strings.xml` and both changelogs were in the overlap set and auto-merged cleanly. Cheapest proof the stack replayed intact: `git diff <oldtag>..<safety-branch>` and `git diff <newtag>..custom` should have an identical file list and line count — if they match, no fork hunk was dropped.
4. **Reset the fork build number**: set `getForkBuild` in `app/build.gradle` back to `1` so the first build on the new tag is `1.<newtag>+001`; bump it +1 on every subsequent local build. If the rebase replayed prior `Bump the fork build number` commits, drop them (they recorded local builds on the old tag) so the reset is clean — the versioning commit already sets `getForkBuild` to `1`.
5. Build + verify, then `git push --force-with-lease origin custom`.
6. Delete the safety branch once confirmed.

## Feature inventory (what each layer does)

### Custom theme colours (commits b08a51e … c4abb89, e01671a, 43cb1f5)
User-customisable colour picker in Display settings, **28 roles across 7 sections** (Backgrounds, Text, Icons, Message list accents, Decorations and accents, Status indicators, Highlights). Data-driven from `CustomThemeColors.ENTRIES`; `FragmentOptionsDisplay.populateCustomColorPicker()` walks the table. Adding a role is a 4-place edit: `colors_custom.xml` resource, an `ENTRIES` row, 2 strings, and `AppThemeCustom`/`actionBarStyleCustom` routing.

**Coverage ceiling (important):** colours are resolved through a `ColorOverrideResources extends Resources` wrapper installed per-Activity. It catches `getColor` and drawable-XML paths but NOT layout-XML `?attr/...` backgrounds nor inflation-time `linkTextColor`/widget-tint reads, because `TypedArray.getDrawable`/`getColorStateList` call package-private `Resources.loadDrawable`/`loadColorStateList` directly. The systemic fix would need a `LayoutInflater.Factory2`; deferred. The **targeted workaround pattern** for any widget whose colour/font is read at inflation: reapply it programmatically at bind time using an override-aware value (e.g. `tvBody.setLinkTextColor(...)` in commit 43cb1f5, body links).

**Gotcha:** `setTextAppearance(R.style.TextAppearance_AppCompat_*)` does NOT compile (symbols not in the project R class) — use `setTextSize` + `setTypeface` instead when building picker UI programmatically.

### Pro activation salt fix (commit 90850d0) — SUPERSEDED
Historical: the github flavor `ActivityBilling.getResponse()` computes `sha256(BuildConfig.APPLICATION_ID + sha256(ANDROID_ID))`, and the renamed `applicationId` broke activation (M66B's server signed against `eu.faircode.email`), so the salt was pinned to the literal `"eu.faircode.email"`.

**This pin is gone.** The later `Unlock all pro features unconditionally` commit made `isPro()` return `true` outright, which removed the reason for the pin, and `getResponse()` was returned to the upstream `BuildConfig.APPLICATION_ID.replace(".debug", "")` line. The challenge/response code is still present but gates nothing. So grepping for `sha256("eu.faircode.email"` correctly returns no hits on a healthy tree — that is not a lost fork change, and it must not be "restored" during a rebase. The invariant to protect here is the unconditional `isPro()`.

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

### Fork versioning, APK naming, and app label (`app/build.gradle`, github `strings.xml`, `ActivityView`)
- **Versioning:** `getForkBuild` in `app/build.gradle` carries the fork build number. `versionCode = getVersionCode() * 10000 + getForkBuild()`; `versionName = "1." + getVersionCode() + "+" + String.format("%03d", getForkBuild())`. So upstream `2328` build `2` → versionName `1.2328+002`, versionCode `23280002`. Reset `getForkBuild` to 1 on each upstream rebase, +1 each subsequent local build. The zero padding is confined to the versionName string (the literal stays a bare int, so the versionCode arithmetic is untouched); it exists so `~/tmp/` and `/sdcard/tmp/`, which every sideloaded sister app shares, sort in build order instead of putting `+10` before `+2`. The scheme stays monotonic because the upstream code only increases. `getVersionCode()` is deliberately left returning the bare upstream code so archivesName, the `CHANGELOG.md` rename, the fdroid signature dirs, and `build_uuid` keep their upstream-keyed values.
- **APK name on deploy:** `shiroikuma-fairemail_<versionName>_arm64-v8a.apk`. The built artifact under `app/build/.../FairEmail-v1.<upstream>a-github-release.apk` is unchanged (archivesName uses the bare code), so only the copied/pushed filename carries the `+<fork>`.
- **App label:** github flavor `app_name` = `白い熊 FairEmail` in `app/src/github/res/values/strings.xml` (was `FairEmail Custom`). Only the github flavor is renamed; `app/src/main` keeps `FairEmail`.
- **Update-check robustness (`ActivityView`):** the github update checker compares `Double.parseDouble(info.tag_name)` against `Double.parseDouble(BuildConfig.VERSION_NAME)`. `1.2328+002` is not a parseable double, so the comparison now strips the `+<fork>` suffix before parsing (the strip cuts at the `+`, so it is agnostic to the padding width); otherwise every check would log an exception and falsely report an update to M66B's upstream build.

### Merged changelog (`CHANGELOG.md`, in-app + GitHub)
The fork keeps a **single merged changelog**: a fork section at the very top of `CHANGELOG.md` (a `# 白い熊 FairEmail — fork changes` heading with one `### 1.NNNN+F` block per fork release, newest first), then a `---` divider, then upstream's verbatim `## Changelog`. Editing **only the root `CHANGELOG.md`** is enough — the Gradle `copyMarkdown` task (a `preBuild` dependency) copies it verbatim into `app/src/main/assets/CHANGELOG.md`, so the in-app Changelog screen shows the fork section too; commit both files in sync (a build re-copies). The `copyChangelog` task also derives `metadata/en-US/changelogs/<code>.txt` from it (markdown stripped) — that one is build output, leave it to regenerate.

- **Placement is deliberate:** the fork section sits **above** upstream's `## Changelog`, and upstream only ever inserts new `### 1.NNNN` blocks at the top of the *version list* (well below our section). So most rebases three-way-merge cleanly; when they do conflict, keep our block above the `---`, take upstream's new block below it.
- **Release notes source:** `/publish-version` should take the GitHub release notes from the **fork section** of `CHANGELOG.md` (the relevant `### 1.NNNN+F` block, or the whole fork section for a first release) — it is the single source, so do not hand-maintain a separate notes file.

## Architecture / coverage-ceiling summary

- **Inflation-time reads are unreachable** by the `Resources` wrapper (colours) and by simple constructor setup (fonts): `?attr` layout backgrounds, `linkTextColor`, widget tints, and general-UI typefaces. The clean systemic fix for all of them is a `LayoutInflater.Factory2`; until then use the **bind-time reapply** workaround per widget.
- **`subject_top` swaps the view IDs**: the adapter's `tvSubject` field always holds the subject text but points to `R.id.tvFrom` (when `subject_top`) or `R.id.tvSubject`. The `tvFrom` field holds the sender. Date is `tvTime`, default-aligned to `R.id.tvFrom`. Any message-row layout work must account for this swap.

## Open / deferred

- **General-UI fonts** (menus, settings, dialogs, AlertDialogs). Needs `LayoutInflater.Factory2`. Would also knock out the colour inflation-time coverage ceiling in one go (the two problems share the same fix).
- **Subject-split flash on fast scroll** in folded two-line mode. Move from `tvSubject.post(...)` to `OnPreDrawListener` to render the split before the first paint instead of one frame after.

## Lessons (process — these all bit us in real sessions)

1. **Verify a push actually moved HEAD before building the next feature.** A "Push" that did not complete left HEAD unmoved while the working tree held the changes; later steps got built on a base that never landed and a `git checkout --` then reverted half of it into a non-compiling frankenstein. After any push: `git fetch origin && git log --oneline -1` and confirm the hash/subject.
2. **Verify every newly-referenced type is imported.** When editing Java, an added reference to `TextView` etc. without the matching `import` fails the build. Quick check: for each symbol the patch uses, `grep -c "import .*\.<Symbol>;" <file>`. Run a build before claiming success.
3. **A build can silently ship the previous APK.** Incremental Gradle no-ops, `cp` copies the old artifact under a new filename. ALWAYS verify APK mtime is current AND run the `resources.arsc` integrity probe before deploying. **Never delete old APKs on the device** (per 白い熊) — leave every prior `/sdcard/tmp/shiroikuma-fairemail_*.apk` in place; the version in the filename keeps builds apart, and the mtime + integrity check already guards against shipping a stale build.
4. **SAF / ActivityResult callbacks fire inside `super.onResume()`** before `ActivityBase` sets `visible=true`. Saving a pref there triggers `onSharedPreferenceChanged` synchronously, which calls `finish()` and skips the relaunch because `visible` is still false — the app appears to vanish. Defer any pref save from such a callback with `Handler(Looper.getMainLooper()).post(...)` (see `FragmentOptionsDisplay.onFontPicked`). In-process dialogs (e.g. the colour picker) are NOT affected.
5. **`./gradlew clean` whenever res/strings/new-files/SDK changed**, and `./gradlew --stop` after touching `gradle.properties`.
6. **A rebase or branch-switch in this repo cannot delete the harness-mounted `.claude/` files.** The session bind-mounts `.claude/settings*.json` and the `.claude/skills` tree read-only (the skill files it is executing from). Several fork commits add files there, so `git rebase <newtag>` checks out the tag (which lacks `.claude/`), then cannot remove or re-create them — `git rebase --abort` / `git reset --hard` choke the same way, stranding a detached HEAD. **Validated fix: mark them skip-worktree before any rebase/reset** so git leaves the mounted copies alone: `git update-index --skip-worktree .claude/settings.json .claude/skills/fairemail-fork/SKILL.md .claude/skills/upstream-new-version/SKILL.md`. The rebase then sails straight to the real conflicts (just `app/build.gradle`). Run the git ops with the sandbox disabled (`dangerouslyDisableSandbox`). To unstick a main repo already stranded in a detached HEAD: `git rebase --quit` then `git checkout -f <branch>` (the mounted files already match, so the forced overwrite is a no-op). A worktree outside the mounts (`git worktree add -b rebase-<newtag> ~/tmp/fe-rebase-<newtag> custom`) also works but is heavier; skip-worktree is the lighter validated path. **Caveat:** to commit edits to those doc files (e.g. this skill refresh after a rebase), the skip-worktree bit must be OFF for them — `git reset --hard` and the rebase usually clear it; if not, `git update-index --no-skip-worktree <paths>` first.

---

**Commit convention — no Claude attribution.** Never add a `Co-Authored-By: Claude …` / "Generated with Claude" trailer to commit messages or PR bodies; end the message at the last line of the body. This overrides the harness default. (Global rule: `~/.claude/CLAUDE.md`.)
