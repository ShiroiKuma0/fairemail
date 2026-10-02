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
| Current base tag | `1.2338` |
| `namespace` | `eu.faircode.email` (unchanged from upstream; Java package stays this) |
| `applicationId` | `shiroikuma.fairemail` (debug variant adds `.debug`) |
| Display name | `白い熊 FairEmail` (github flavor `app_name` in `app/src/github/res/values/strings.xml`) |
| Versioning | `versionName` = `1.<upstream>+<fork>`, fork number zero padded to three digits (e.g. `1.2338+001`); `versionCode` = `<upstream> * 10000 + <fork>` (e.g. `23380001`, unpadded arithmetic). The `getForkBuild` literal in `app/build.gradle` is the fork build number — reset to **1** on every upstream rebase, **+1** on every subsequent local build. `getVersionCode()` keeps returning the bare upstream code (it feeds archivesName/changelog/signature paths; do not repurpose it). |
| Keystore | `~/.android-keystores/fairemail-custom.jks`, alias `fairemail`. Password is NOT in this repo — keep it in `~/.gradle/gradle.properties` or an env var. |
| Build flavor / type | `github` / `release` → task `:app:assembleGithubRelease` |
| Built APK path | `app/build/outputs/apk/github/release/FairEmail-v<tag>a-github-release.apk` |
| Deployed APK names | `shiroikuma-fairemail_<versionName>_arm64-v8a.apk` (e.g. `shiroikuma-fairemail_1.2338+001_arm64-v8a.apk`), copied to `~/tmp/` AND pushed to `/sdcard/tmp/`. No datetime — matches the user's other sideloaded apps (denwa, futokxkb, simplex): `shiroikuma-<app>_<upstream>+<fork>_arm64-v8a.apk`, fork number zero padded to three digits so the shared directories sort in build order. |
| Toolchain (tag 1.2338) | compileSdk=37, minSdk=23, targetSdk=37, NDK `27.3.13750724` (r27d), AGP 9.4.1 / Gradle 9.7.1, Java toolchain 21 (upstream bumped 17→21 at 1.2317), kotlin-android plugin REMOVED. Host build JDK is OpenJDK 21 (`JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64`). SDK lives at `$HOME/android-sdk`; export `ANDROID_HOME`/`ANDROID_SDK_ROOT` for builds (set in the user's `.bashrc` but NOT in non-interactive shells, and no `local.properties` is committed). |
| Device | Huawei Mate XT tri-fold. Folded-portrait ≈ 1008×2127 px (~366 dp wide); every other fold/orientation state is ≥ ~745 dp. |

## Commit stack on origin/custom

Newest first (short hashes). For the authoritative current list run `git log --oneline <upstream-tag>..origin/custom`.

```
ef44426347  Reset the fork build number for the 1.2338 rebase
d7160087ef  Publish 1.2337+004: refresh changelog and README for the folder fetch
964cd40745  Bump the fork build number to 4
9b0af4f2ac  Fetch an entire folder with progress and a stop button
738c41f4eb  Publish 1.2337+001: refresh changelog and README for the 1.2337 rebase
1cf75fcee1  Refresh fork skill and CLAUDE docs for the 1.2337 rebase
3185e19343  Reset the fork build number for the 1.2337 rebase
824f5264f4  Publish 1.2333+010: refresh changelog and README for the language box and the link fix
b58c7a22a2  Bump the fork build number to 10
71808d5cb1  Apply a per host link decision only inside its own exemption
8b4abe86e1  Lead the miscellaneous settings tab with a loud language box
f5f8052ef7  Publish 1.2333+008: refresh changelog and README for the progress fixes
a376d69855  Bump the fork build number to 8
4f2d542363  Report progress through the restore, not only through the spool
7c6b7f6bd0  Send the import byte count in the fields the caller reads
03fd49e7c6  Read a stream to its real end, not to its first empty read
adef442754  Publish 1.2333+006: refresh changelog and README for the backup fixes
9bccc6076d  Bump the fork build number to 6
0b13237dce  Split email attachments from message bodies as their own category
7a39d7f3c0  Answer the caller even when the automation worker cannot start
b4f4ac9259  Restore every category the archive carries, not the export defaults
fe6d57b2ed  Report the whole category catalogue at the automation data door
e862c8c633  Drop the ask list from the project Claude settings
2f7c1db0c3  Publish 1.2333+004: refresh changelog and README for the automation data door
8c94f21bd4  Bump the fork build number to 4
875e79a0ae  Add the automation data door for backup and restore
29afff81a9  Send automation progress from one place for both doors
9485f5b287  Make the automation token optional and ship the switch on
a16d1b2bdf  Declare both automation callers in the manifest queries element
5294092996  Publish 1.2333+001: refresh changelog and README for the 1.2333 rebase
61559d373d  Refresh fork skill and CLAUDE docs for the 1.2333 rebase
bd1f80f1a3  Publish 1.2331+001: refresh changelog and README for the 1.2331 rebase
d4fe15ab3a  Publish 1.2330+001: refresh changelog and README for the 1.2330 rebase
ea40069ae5  Publish 1.2329+001: refresh changelog and README for the 1.2329 rebase
5740248bc1  Reset the fork build number for the 1.2329 rebase
6446700c04  Publish 1.2328+002: refresh changelog and README for the 1.2328 rebase
4953ab15b5  Bump the fork build number to 2
51d14429e5  Zero pad the fork build number in the version name
de6bfd82be  Refresh fork skill and CLAUDE docs for the 1.2327 rebase
9e43797589  Publish 1.2327+1: refresh changelog and README for the 1.2327 rebase
ffc067c95e  Reset the fork build number for the 1.2327 rebase
21f005213c  Publish 1.2326+8: refresh changelog and README for the cancel work
eb34258817  Bump the fork build number to 8
7ee87664a0  Stop a running export, leaving no partial file behind
166197d5ff  Say which backup categories start ticked
515bb5a38a  Publish 1.2326+7: refresh changelog and README for the backup work
485b4dea1e  Bump the fork build number to 7
f7c0a2fb50  Answer the sister app state export automation contract
83f0578f29  Add the automation token gate and its Export Import rows
1beaea1bf2  Export the local mail store as an opt-in backup category
39a6bdac86  Write backups as one ZIP named by the sister-app convention
5662518d70  Publish 1.2326+5: refresh changelog and README for the UI page
abb01fbeb7  Bump the fork build number to 5
acdeef1ade  Custom theme: black toolbar overflow menu with a yellow border
44ba7bcfba  Open the UI page by long-pressing the toolbar hamburger buttons
578fb7747d  Add the 白い熊 FairEmail UI page with export/import replacing Backup
07d9ec19ce  Publish 1.2326+1: refresh changelog and README for the 1.2326 rebase
d1fdefaf50  Publish 1.2325+4: refresh README and changelog for the pro unlock
39d7b55f32  Hide the purchase section on the pro features screen
023ef9ed38  Unlock all pro features unconditionally
63a3b016aa  skills: document the merged changelog workflow
b6848ccb4a  Add a merged fork changelog to CHANGELOG.md
03281c619b  Add a fork README for the GitHub release
dd8f1adce0  Custom theme: black dropdown spinners with a yellow border
0d470eda3b  skills: auto-deliver builds via /after-build (drop the transfer prompt)
402213ec72  Make compose field hints and separators legible and tunable
1376ce58ec  docs: no attribution trailer in commits
0d561ee4c5  Refresh fork skill and CLAUDE docs for the 1.2326 rebase
c5d4361fb3  Custom launcher icon: black-yellow line-traced envelope
fa44c86369  Skill: never delete old APKs on the device when deploying
1d66c1bd21  Custom theme: yellow drawer border without dimming the content
89e4f42c2a  Custom theme: black push buttons with a yellow border
eca75137ef  Custom theme: black snackbars with a yellow border and yellow text
9bd7042865  Custom theme: black dialogs and popup menus with a yellow border
1fa8237afc  Add upstream-new-version skill to drive the upstream rebase and build
04d8b6d8d3  kxkb: document fork versioning and label, refresh skill for 1.2318
228c4629bf  Rename the sideloaded app label to 白い熊 FairEmail
1f837cf713  Version the fork as the upstream version plus a local build number
c585b69d01  kxkb: add agent config (CLAUDE.md + .claude/skills/)
331d0085d7  Folded message list: optional two-line subject with trailing date
c28222550d  Custom fonts: expand to independent per-role selection across eight roles
0b0742d302  Custom font picker: defer pref save so the activity recreates cleanly
4f2c925c94  Add custom font and weight selection for message text
f4731fd09e  Custom theme colours: route tvBody link colour through the override
6f37cb6b04  Custom theme colours: expand to 28 roles across 7 sections
336428377d  Custom theme colours: refactor picker UI to be data-driven
b7b47a32bb  Custom theme: hook XML-resolved colours via Activity-only Resources wrap
50867840ab  Custom theme: hook code-resolved colours to user prefs
f7a80c2c49  Custom theme: scaffold the customizable colour picker UI
86e7df41f3  Custom theme: unread accent yellow, decouple sender colour, extend font size range
dda439ece2  Custom theme polish: swap subject/sender highlight target, never bold sender, add sender_italic toggle, accent yellow
92910dcd25  Add Custom theme: yellow-on-black with gold unread accent
37aa88391f  Customize github flavor for sideloaded shiroikuma.fairemail build
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
build_apk=app/build/outputs/apk/github/release/FairEmail-v1.2338a-github-release.apk   # archivesName uses the bare upstream code
version=1.2338+001                                          # versionName: 1.<upstream>+<fork>, fork padded to 3 digits (bump <fork> each local build)
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

Periodically rebase `custom` onto a newer upstream tag (last done 1.2337 → 1.2338):

1. `git fetch upstream --tags`.
2. Create a safety branch: `git branch custom-pre-<newtag>-rebase`.
3. `git rebase <newtag>` and resolve conflicts. Recurring spots: `app/build.gradle` (**keep** `applicationId "shiroikuma.fairemail"` and the `getForkBuild` versioning lines; **take upstream** `getVersionCode`/SDK/NDK/Java/Gradle bumps); `CHANGELOG.md` (and its build-copied twin `app/src/main/assets/CHANGELOG.md`) when upstream prepends a new version block — **keep our fork section above the `---` divider, take upstream's new `### 1.NNNN` block below it**; the fork section sits above `## Changelog` precisely so this stays a clean three-way merge most rebases (see the Merged changelog note in the feature inventory); plus `fragment_options_display.xml` and `AdapterMessage.java` when upstream reshuffles the display options or message-row bind code (1.2317 dropped the `tvSenderEllipsizeRemark`/`tvSubjectEllipsizeRemark` hints and flattened the subject single-line block — re-anchor our `swSenderItalic` switch to `spSenderEllipsize`). The 1.2322 → 1.2324 rebase (25 upstream commits, skipping the 1.2323 tag) had a single real conflict, the `getVersionCode` 2322→2324 bump in `app/build.gradle`. Upstream 1.2324 added `org.gradle.configuration-cache=true` to `gradle.properties` itself, the exact line our `Enable Gradle configuration cache` commit introduced, so that fork commit was auto-skipped as an already-applied cherry-pick. `ActivityView.java`, `ActivityBase.java`, `AdapterMessage.java`, `ApplicationEx.java`, and `Helper.java` were all in the overlap set but auto-merged cleanly (verify the fork hooks survived after such an auto-merge: the `CustomFont.apply` calls, the two-line-subject logic, the `swSenderItalic` anchor, the `migrateLegacyWeightIfNeeded` call, the colour-override reads, and the `ActivityView` update-check `+<fork>` suffix strip). The 1.2327 → 1.2328 rebase (16 upstream commits, all housekeeping: Crowdin sync, S/MIME roots, PSL, Brave debounce list, the Thundermail provider, a Gemini model-name fix, a VPN list button, an NPE guard, a display-cutout inset and default medium spacing) again had exactly one conflict, the `getVersionCode` 2327→2328 bump. `ActivityBase.java`, `ApplicationEx.java`, `FragmentOptionsDisplay.java`, `strings.xml` and both changelogs were in the overlap set and auto-merged cleanly. The 1.2328 → 1.2329 rebase (10 upstream commits: CSS colour schemes and `prefers-color-scheme: dark` now ignored, cached CSS media-list matching, a settings button on the notification-permission dialog, a contact-picker cursor guard, plus PSL, Brave debounce list, AndroidX and Crowdin) had the same single `getVersionCode` conflict. `FragmentCompose.java` was in the overlap set for the first time — upstream touched the contact-picker cursor read and the `processStyles` call site, both far from our compose hint and separator colour hooks — and it auto-merged cleanly, as did both changelogs, which for once needed no manual reconciliation. The 1.2329 → 1.2330 rebase (15 upstream commits: the message language added as an expression condition in rules, rule regular expressions validated before they are saved, and library bumps for Jsoup 1.23.1, EvalEx 3.7.0, BouncyCastle 1.85, ez-vcard 0.12.2, MiniDNS 1.1.1, JsonPath 3.0.0 and core library desugaring 2.1.5) had the same single `getVersionCode` conflict, this time in the original versioning commit rather than the tip, because that is the commit that first touches the line: take upstream `2330`, keep the `getForkBuild` block, and the later zero padding commit replays over it untouched. Both changelogs auto-merged for the second rebase running. No toolchain movement at all this round — compileSdk 37, NDK r27d and Gradle 9.6.1 all stand — so `clean` plus the resource probe was the whole verification. The 1.2330 → 1.2331 rebase (12 upstream commits: support for PGPony, the submitter decoded from `@mozmail.com` masked addresses, suspicious addresses shown in the personal field with the earlier "Show email address if name contains email address" behaviour reverted, relaxed qmail and nullmailer checks, debug code removed, plus PSL, FAQ and Crowdin) again had the single `getVersionCode` conflict in the original versioning commit, resolved the same way: take upstream `2331`, keep the `getForkBuild` block. `AdapterMessage.java`, `FragmentCompose.java`, `Helper.java`, `strings.xml` and both changelogs were in the overlap set and auto-merged cleanly, the third rebase running with no changelog handwork. No toolchain movement again — compileSdk 37, NDK r27d and Gradle 9.x all stand. The 1.2331 → 1.2332 rebase (8 upstream commits: a Jsoup crash in the reformatted message view prevented on Android 6 by reading the stream into memory and parsing the string instead of streaming it through the ICU charset decoder, which threw `IllegalArgumentException: Bad position` on API 23; Rackspace added to the provider list; the Android Gradle plugin moved to 9.3.2; plus FAQ and Crowdin) had the same single `getVersionCode` conflict in the original versioning commit, resolved the same way: take upstream `2332`, keep the `getForkBuild` block. Only `app/build.gradle` and both changelogs were in the overlap set this round, the smallest overlap yet, and the changelogs auto-merged for the fourth rebase running. No toolchain movement beyond that AGP point release — compileSdk 37, NDK r27d and Java 21 all stand. The 1.2332 → 1.2333 rebase (44 upstream commits, the largest jump since 1.2324: S/MIME gains auth-enveloped-data support along with fixed class cast exceptions and better recipient info handling, and known root certificates are no longer stored; rule expression conditions gain `startswith`, `endswith` and `jpath` operators, and a failing rule now names itself in the exception; the AI layer was reworked, with Gemini migrated to the OpenAI compatible API, a model selector, a max tokens option, error reporting, and notifications delayed while a summary generates; experimental Gadgetbridge support; a GMX check with a time limit and tag; the MiniDNS customisation dropped in favour of the stock library; spacing disabled for the tabular view; a dismissible LAN snackbar; plus FAQ and Crowdin) had the same single `getVersionCode` conflict in the original versioning commit, resolved the same way: take upstream `2333`, keep the `getForkBuild` block. `ApplicationEx.java`, `strings.xml` and both changelogs were in the overlap set and auto-merged cleanly, the fifth rebase running with no changelog handwork. Gradle moved 9.6.1 → 9.7.1, the first toolchain movement in five rebases that needs more than a clean build: run `./gradlew --stop` before building so the new distribution is fetched against no stale daemon. compileSdk 37, NDK r27d and Java 21 all stand. The 1.2333 → 1.2337 rebase (50 upstream commits across four tags: demo accounts added to comply with Play Store policies at 1.2337; bug fixes in new features at 1.2336; the FAQ base URI moved from `https://m66b.github.io/FairEmail/` to `https://github.com/M66B/FairEmail/blob/master/FAQ.md` at 1.2335, because Microsoft incorrectly blocks the Pages host; and at 1.2334 drag and drop on address bubbles, `via`, `submitter`, `cc`, `bcc` and `replyto` added to rule expression conditions, plus the Public Suffix List and Crowdin) was the first rebase in six to conflict anywhere but `app/build.gradle`. That one conflicted as always, in the original versioning commit: take upstream `2337`, keep the `getForkBuild` block. **`README.md` conflicted for the first time**, because upstream reworded eighteen lines of its own README while our fork replaces the file wholesale — there is nothing of upstream to preserve there, so resolve it by taking the fork README verbatim (`git show <fork-README-commit>:README.md > README.md`). Both changelogs auto-merged for the sixth rebase running. `ApplicationEx.java` and `Helper.java` were in the overlap set and auto-merged cleanly: upstream gated its Gemini to OpenAI migration on no OpenAI key being set already and stripped the `models/` prefix from the model name, then dropped the `SHORT`/`MEDIUM` restriction from `getTimeInstance`/`getDateTimeInstance` and gave the `LONG` time style seconds and a zone — all far from our extended zoom ladder, the `resolveColor` override hook, the themed snackbar and the `migrateLegacyWeightIfNeeded` call. Because the stack replayed ending on `Bump the fork build number to 10`, this rebase also needed its own **Reset the fork build number** commit, which the 1.2330 through 1.2333 rebases did not: the 1.2329 reset commit had left the literal at 1 and nothing after it moved it until the 1.2333 feature work did. No toolchain movement whatsoever — compileSdk 37, NDK r27d, Gradle 9.7.1 and Java 21 all stand — so `clean` plus the probes was the whole verification. Note also that `git fetch upstream --tags` over the old HTTPS remote URL now fails with `HTTP 401 www-authenticate: Basic realm="GitHub"`, sandboxed or not, because GitHub refuses the anonymous HTTPS fetch; the `upstream` remote was switched to `git@github.com:M66B/FairEmail.git` on 2026-09-03, which fixes it for good. The 1.2337 → 1.2338 rebase (32 upstream commits: a beige switch added to the theme selector, single-account direct device search, an option to hide the username for accounts, links allowed in summaries, Cloud sync deprecated, more notifications, MTE enabled for debug builds, rules marked as a Pro feature, pro enabled for demo accounts, a FAB foreground fix for yellow backgrounds, sanitized host and domain confirmation, plus PSL, AndroidX, FAQ and Crowdin) was the first rebase to conflict inside a fork Java hunk: **`FragmentDialogTheme.java`** conflicted in the original `Add Custom theme` commit, because upstream added a `bandw` (black and white) flag next to our `custom` flag in `eval()` and a new `swBeige` switch whose enable line sat next to our `swBlack` line. Resolve by keeping both flags and adding `!custom` to the new `swBeige.setEnabled(...)` as well, so the Custom theme greys the beige switch out exactly as it greys out black, reverse and the light/dark options (the `beige` preference itself predates the switch and only tints light card backgrounds, so nothing at runtime needed reconciling). `app/build.gradle` conflicted as always in the original versioning commit: take upstream `2338`, keep the `getForkBuild` block. `AdapterMessage.java`, `Core.java`, `FragmentMessages.java`, `FragmentOptionsBackup.java`, `NotificationHelper.java`, the three `dialog_theme.xml` layouts, `strings.xml`, `styles.xml` and both changelogs were in the overlap set — the largest overlap yet — and all auto-merged cleanly, the seventh rebase running with no changelog handwork. The replay ended on `Bump the fork build number to 4`, so a **Reset the fork build number** commit was needed again. Toolchain: AGP moved 9.3.2 → 9.4.1 in the root `build.gradle` and AndroidX fragment 1.9.0 → 1.9.1; compileSdk 37, NDK r27d, Gradle 9.7.1 and Java 21 all stand, and `clean` plus the probes was enough (no `--stop` needed, `gradle.properties` was untouched). The identical-file-list proof below held with a one-line difference, which is exactly the `!custom` added to the beige line. Cheapest proof the stack replayed intact: `git diff <oldtag>..<safety-branch>` and `git diff <newtag>..custom` should have an identical file list and line count — if they match, no fork hunk was dropped.
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
- **Versioning:** `getForkBuild` in `app/build.gradle` carries the fork build number. `versionCode = getVersionCode() * 10000 + getForkBuild()`; `versionName = "1." + getVersionCode() + "+" + String.format("%03d", getForkBuild())`. So upstream `2338` build `1` → versionName `1.2338+001`, versionCode `23380001`. Reset `getForkBuild` to 1 on each upstream rebase, +1 each subsequent local build. The zero padding is confined to the versionName string (the literal stays a bare int, so the versionCode arithmetic is untouched); it exists so `~/tmp/` and `/sdcard/tmp/`, which every sideloaded sister app shares, sort in build order instead of putting `+10` before `+2`. The scheme stays monotonic because the upstream code only increases. `getVersionCode()` is deliberately left returning the bare upstream code so archivesName, the `CHANGELOG.md` rename, the fdroid signature dirs, and `build_uuid` keep their upstream-keyed values.
- **APK name on deploy:** `shiroikuma-fairemail_<versionName>_arm64-v8a.apk`. The built artifact under `app/build/.../FairEmail-v1.<upstream>a-github-release.apk` is unchanged (archivesName uses the bare code), so only the copied/pushed filename carries the `+<fork>`.
- **App label:** github flavor `app_name` = `白い熊 FairEmail` in `app/src/github/res/values/strings.xml` (was `FairEmail Custom`). Only the github flavor is renamed; `app/src/main` keeps `FairEmail`.
- **Update-check robustness (`ActivityView`):** the github update checker compares `Double.parseDouble(info.tag_name)` against `Double.parseDouble(BuildConfig.VERSION_NAME)`. `1.2338+001` is not a parseable double, so the comparison now strips the `+<fork>` suffix before parsing (the strip cuts at the `+`, so it is agnostic to the padding width); otherwise every check would log an exception and falsely report an update to M66B's upstream build.

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
