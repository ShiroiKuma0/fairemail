---
name: upstream-new-version
description: Check M66B/FairEmail upstream for a new release and bring the shiroikuma.fairemail fork up to it. Invoked as /upstream-new-version. Fetches upstream tags, compares the latest 1.NNNN release tag against the custom branch base, and if newer rebases the custom patch stack onto it, reconciles the recurring conflicts, resets the fork build number, builds and verifies the APK, and deploys to the device, stopping at the Push gate for on-device verification. Sits on top of the fairemail-fork skill for project identity, the build and deploy pipeline, and conventions. Use when the user asks whether there is a new upstream FairEmail version, to update or rebase onto the latest upstream, or types /upstream-new-version.
---

# upstream-new-version

One-call runbook for "there is a new upstream FairEmail; rebuild our fork on it."
It automates the exact flow documented in the **fairemail-fork** skill (the
**Upstream rebase procedure** and **Build + deploy pipeline** sections). Read
`fairemail-fork` first — it auto-loads on any task in this repo and is the
authoritative reference for project identity, the feature inventory, the
coverage ceilings, and the process lessons. This skill is the ordered driver;
`fairemail-fork` is the detail it leans on.

## What "rebase master, apply our custom changes" means here

The fork is a stack of feature commits on the `custom` branch, replayed onto an
upstream release tag. Bringing our changes onto a new upstream is literally
`git rebase <newtag>` on `custom`: that replays every fork commit on top of the
new tag, which is exactly "apply all our changes from custom". The local
`master` branch is only a mirror of `origin/master`; it carries no fork work and
does not need to move for a build. (It can be fast-forwarded at the end as a
cosmetic nicety; see Step 8.4.)

## Preflight

- Working tree clean: `git status --short` is empty. If not, stop and ask.
- On `custom`: `git rev-parse --abbrev-ref HEAD` is `custom` (else `git checkout custom`).
- Build env (NOT set in non-interactive shells, so export every run):
  ```bash
  export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
  export ANDROID_HOME=$HOME/android-sdk ANDROID_SDK_ROOT=$HOME/android-sdk
  ```

## Step 1 — Detect a new version

```bash
git fetch upstream --tags
base=$(git tag --merged custom --sort=-v:refname | grep -E '^1\.[0-9]+$' | head -1)
new=$(git tag -l '1.*' --sort=-v:refname | grep -E '^1\.[0-9]+$' | head -1)
echo "base=$base  new=$new"
```
- `^1\.[0-9]+$` keeps only bare release tags (`1.2318`) and skips betas/rcs like
  `1.2010b`, `1.2007c`.
- `base` = the highest release tag that is already an ancestor of `custom` (our
  current base). `new` = the highest release tag upstream now publishes.
- **If `new == base`**: there is no new version. Report "already on the latest
  upstream (`$base`)" and STOP — do nothing else.
- **If `new` is greater than `base`**: proceed. Scope the jump first so you know
  what to expect:
  ```bash
  git rev-list --count "$base".."$new"                       # upstream commit count
  comm -12 <(git diff --name-only "$base"..custom | sort) \
           <(git diff --name-only "$base" "$new" | sort)     # OUR files upstream also touched
  ```
  The second command is the tell: only files in both sets can conflict.
  Historically that is just `app/build.gradle` (the version bump).

## Step 2 — Safety branch, then rebase

```bash
git branch "custom-pre-${new}-rebase"      # backstop
git rebase "$new"
```
Resolve conflicts (Step 3), then `git rebase --continue` until "Successfully
rebased". If it goes sideways: `git rebase --abort` restores the pre-rebase
state; the safety branch is the deeper net.

## Step 3 — Reconcile conflicts (resolve at the root, never blindly)

Recurring spots, from the fairemail-fork rebase procedure:

- **`app/build.gradle`** — almost always the only conflict.
  - **Keep ours**: `applicationId "shiroikuma.fairemail"`, the `getForkBuild`
    block and its comment, and the `versionCode`/`versionName` lines that fold
    the fork build into the upstream code.
  - **Take upstream**: the `getVersionCode` value (the new `return NNNN`), and
    any compileSdk/minSdk/targetSdk/NDK/Java/Gradle/AGP bumps.
  - Net result: `getVersionCode` returns the new bare upstream number;
    `getForkBuild` stays our literal (reset in Step 4).
- **`app/src/main/res/layout/fragment_options_display.xml`** and
  **`AdapterMessage.java`** — only if upstream reshuffled the display options or
  the message-row bind code. Re-anchor our additions (the `swSenderItalic`
  switch near `spSenderEllipsize`, the two-line-subject bind logic, the
  `CustomFont.apply(...)` calls, the colour-override reads) to wherever upstream
  moved the neighbouring views. The fairemail-fork **Feature inventory** lists
  what each hook attaches to and why.

Invariants that MUST survive any reconciliation — never let a merge revert them:
- `applicationId "shiroikuma.fairemail"`; the Java `namespace` stays `eu.faircode.email`.
- The Pro-salt pin in `app/src/github/java/eu/faircode/email/ActivityBilling.java`:
  `return Helper.sha256("eu.faircode.email" + getChallenge(context));` (comment at the site explains why).
- The github `app_name` = `白い熊 FairEmail` in `app/src/github/res/values/strings.xml`.
- The `ActivityView` update-check `+<fork>` suffix strip (`version.indexOf('+')`),
  so `1.NNNN+F` parses and does not falsely report an upstream update.

After resolving, confirm no markers remain:
```bash
git grep -nE '^(<<<<<<<|=======|>>>>>>>)' -- '*.gradle' '*.java' '*.xml' || echo "clean"
```

## Step 4 — Reset the fork build number

Per fork versioning, the first build on a new tag is `1.<new>+1`. In
`app/build.gradle` set `getForkBuild` back to **1**, and confirm `getVersionCode`
now returns the new bare number (the merge in Step 3 should already have taken it):
```bash
grep -nE 'getVersionCode = |getForkBuild = ' app/build.gradle
# expect:  return <new-number>   and   return 1
```

## Step 5 — Build + verify (MANDATORY)

A rebase changes res/strings, so `clean` is required (and `--stop` first only if
`gradle.properties` changed in the merge).
```bash
new_num=${new#1.}
./gradlew clean :app:assembleGithubRelease 2>&1 | tee /tmp/fe-build-${new}.log
build_apk=app/build/outputs/apk/github/release/FairEmail-v${new}a-github-release.apk
```
Verify before claiming success — incremental Gradle can silently ship a stale APK:
```bash
grep -cE 'error:|エラー:|FAILED' /tmp/fe-build-${new}.log     # must be 0; paste any error lines and fix at root
ls -lh "$build_apk"; date                                     # mtime must be current, not the prior build
aapt2=$(ls "$HOME"/android-sdk/build-tools/*/aapt2 | sort -V | tail -1)
"$aapt2" dump badging "$build_apk" | grep -E 'package: name|versionName|application-label'
# expect: name='shiroikuma.fairemail'  versionName='${new}+1'  application-label:'白い熊 FairEmail'
unzip -p "$build_apk" resources.arsc | strings | grep -c 'subject_lines_narrow'   # >0 = fork resources packaged
```
Note: `白い熊` greps to 0 from `strings` on resources.arsc (UTF-16 encoding) — that
is a false negative; the `aapt2` application-label line is authoritative, trust it.

## Step 6 — Deploy (never auto-install)

```bash
version="${new}+1"                               # bump the +N for each subsequent local build
apk_name="shiroikuma-fairemail_${version}_arm64-v8a.apk"
mkdir -p ~/tmp; rm -f ~/tmp/shiroikuma-fairemail_*.apk
cp "$build_apk" ~/tmp/"$apk_name"
```
Then invoke the global `/after-build` skill: it runs `/adb-check` UNSANDBOXED,
then `/adb-push` to `/sdcard/tmp/` if the phone is connected, else `/scp` to
skhw, and announces the filename — never prompt "is the phone connected?".
(Old `/sdcard/tmp/shiroikuma-fairemail_*.apk` are never wiped — prior builds
stay in place.)

## Step 7 — Device verification, then the Push gate

Report to the user: the new version (`$base` to `$new`), versionName/code, what
upstream changed, and any non-trivial reconciliation you did. Ask them to install
and verify on the Mate XT — custom theme, fonts, the folded two-line subject.
**Do NOT push. Wait for the user to type "Push."** (fork convention).

## Step 8 — After "Push."

1. Discard build-generated noise the assemble may have stamped (the Gradle
   changelog task rewrites the release date to today — not a fork change):
   ```bash
   git checkout -- app/src/main/assets/CHANGELOG.md metadata/en-US/changelogs/${new_num}.txt
   ```
2. Refresh the docs to the new tag and fold them into the top skill-doc commit
   (amend keeps the curated stack a constant size across rebases):
   - `fairemail-fork/SKILL.md`: `Current base tag` to `$new`; the version
     examples (`1.<num>+1`, `<num>0001`); the `Toolchain (tag ...)` header;
     the build-pipeline APK path; the rebase line `last done <base> → <new>`;
     and regenerate the commit-stack listing from `git log --oneline ${new}..custom`.
   - `CLAUDE.md`: the `1.<num>+1` / `<num>0001` version examples.
   - Stage ONLY the doc files by path, then `git commit --amend -F <msgfile>`
     (subject "...refresh skill for $new"). Never `git add -A` (build artifacts
     may be untracked).
3. Push and verify it actually landed (a no-op push has caused real damage here):
   ```bash
   git push --force-with-lease origin custom
   git fetch origin
   [ "$(git rev-parse custom)" = "$(git rev-parse origin/custom)" ] && echo "landed"
   git merge-base --is-ancestor "$new" origin/custom && echo "based on $new"
   ```
4. Delete the safety branch: `git branch -D "custom-pre-${new}-rebase"`.
5. (Optional, only if the user wants `master` synced) `git branch -f master "$new"`
   — purely cosmetic; the fork builds from `custom`.

## Lessons baked in (full list in fairemail-fork)

- **Verify the push landed** before treating the rebase as done (Step 8.3).
- **A build can silently ship the previous APK** — always check mtime + the
  integrity probe (Step 5). **Never delete old APKs on the device** (per 白い熊);
  leave every prior `/sdcard/tmp/shiroikuma-fairemail_*.apk` in place.
- **`clean` after res/strings/new-files/SDK changes; `--stop` after `gradle.properties`.**
- **Every newly referenced Java type needs its import** if reconciliation adds
  references: `grep -c "import .*\.<Symbol>;" <file>`. Trust the compiler.
- **One feature = one commit; prose messages, ~72 col, no apostrophes; "Push." gates push.**

---

**Commit convention — no Claude attribution.** Never add a `Co-Authored-By: Claude …` / "Generated with Claude" trailer to commit messages or PR bodies; end the message at the last line of the body. This overrides the harness default. (Global rule: `~/.claude/CLAUDE.md`.)
