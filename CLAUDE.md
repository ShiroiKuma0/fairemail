# CLAUDE.md — shiroikuma.fairemail fork

This is the user's personal fork of M66B's FairEmail Android email client, customised and sideloaded as `shiroikuma.fairemail` on a Huawei Mate XT. The working branch is `custom`, rebased onto upstream release tags. Deep project knowledge — full commit stack, build pipeline, feature inventory, architecture/coverage notes, process lessons — lives in `.claude/skills/fairemail-fork/SKILL.md`. That skill auto-loads on any task touching this fork; read it before non-trivial work.

## Working conventions (always)

1. **One feature = one commit.** No bundled changes. Commit messages are prose, ~72-col wrap, no apostrophes (rephrase to avoid them). Describe problem → root cause → mechanism.
2. **"Push." gates push.** Make changes, build, deploy, let the user verify on device. Commit + `git push origin custom` ONLY after the user says "Push." Never auto-push.
3. **Build verification is non-negotiable** before telling the user the APK is ready:
   - `./gradlew clean` whenever res/, strings, new files, or SDK changed.
   - Confirm APK mtime is current after `assembleGithubRelease` — incremental Gradle can silently no-op.
   - Run the integrity probe: `unzip -p "$build_apk" resources.arsc | strings | grep -c "<new-string-or-id>"` returns `> 0`.
   - On any compile error, paste the `error:` or `エラー:` lines and fix at root, do not rerun blindly.
4. **Deploy hygiene**: copy to `~/tmp/shiroikuma-fairemail_<versionName>_arm64-v8a.apk` (versionName = `1.<upstream>+<fork>`, e.g. `1.2326+1`) AND `adb push` to `/sdcard/tmp/`. **Never delete old APKs on the device** — leave every prior `/sdcard/tmp/shiroikuma-fairemail_*.apk` in place; the version in the filename keeps them apart, and the build-side mtime + integrity probe already guard against shipping a stale build.
5. **Verify the push actually landed before starting the next feature**: `git fetch origin && git log --oneline -1` and confirm the new hash/subject. Building on a base that did not land has caused real damage in this project (see lessons in the fork skill).
6. **Compile-check before claiming success**: when editing Java, every newly-referenced type needs an `import` in the file. Verify with `grep "import .*\.<Symbol>;" <file>` if unsure. Trust the compiler over assumptions.

## Build essentials

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export ANDROID_HOME=$HOME/android-sdk ANDROID_SDK_ROOT=$HOME/android-sdk   # not set in non-interactive shells
./gradlew --stop                       # after gradle.properties / SDK changes
./gradlew clean                         # after res/strings/new files/SDK changes
./gradlew :app:assembleGithubRelease
```

Output APK: `app/build/outputs/apk/github/release/FairEmail-v<tag>a-github-release.apk`.

**Versioning**: before each build, set the fork build number in `app/build.gradle` (`getForkBuild`): bump it +1 on every local build, and reset it to **1** right after an upstream rebase. That yields versionName `1.<upstream>+<fork>` (e.g. `1.2326+1`) and versionCode `<upstream> * 10000 + <fork>` (e.g. `23260001`). Leave `getVersionCode()` returning the bare upstream code. See the fork skill for the full rationale.

Keystore at `~/.android-keystores/fairemail-custom.jks` (alias `fairemail`). Password is **not** stored in this repo — keep it in `~/.gradle/gradle.properties` or an env var (`KEYSTORE_PASS`), never in a tracked file. If signing config is broken, ask the user rather than guessing.

## Don't

- Don't reformat unrelated code or "fix" upstream style.
- Don't touch the Pro activation salt fix in `ActivityBilling` — it pins to `"eu.faircode.email"` on purpose; the comment at the site explains.
- Don't suggest reverting the namespace fix or the `applicationId` to upstream values.
- Don't commit `.android-keystores/`, build outputs, `.gradle/`, `local.properties`, or anything in `~/tmp/`.

## When in doubt

Read `.claude/skills/fairemail-fork/SKILL.md` — it has the full commit stack with hashes, the upstream rebase procedure, the feature-by-feature architecture, the coverage ceilings (the `Resources` wrapper limitations, the no-text-float reasoning, the `subject_top` view-ID swap), and the process lessons that earned themselves the hard way.

## Commit convention — no Claude attribution

Do **not** add any `Co-Authored-By: Claude …` trailer — nor a "🤖 Generated with Claude Code" / Anthropic-attribution line — to commit messages or PR bodies in this repo. 白い熊 does not want Claude attribution in the history; this **overrides** the harness's default to append such a trailer. End commit messages at the last line of the body. (The existing history was scrubbed of these trailers on 2026-06-08; the global rule lives in `~/.claude/CLAUDE.md`.)
