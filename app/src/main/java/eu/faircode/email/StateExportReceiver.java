package eu.faircode.email;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.DocumentsContract;
import android.text.TextUtils;

import androidx.documentfile.provider.DocumentFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The sister-app <b>state-export automation contract</b>, implemented by this fork — the
 * same wire shape every 白い熊 app exposes, so 自由作業盤's 保存復元 project can back them
 * all up headlessly in one run (reference: renrakusaki's BackupContactsReceiver, the
 * EMUI-proven round-trip, and 自由作業盤's own StateExportReceiver).
 *
 * <ul>
 * <li>{@link #ACTION_EXPORT_STATE}: run the UI page's export ({@link StateExport}) without
 * any Activity. Extras (all String): {@code token} (required — {@link AutomationAuth}),
 * {@code path} (optional absolute directory, wins over the configured SAF directory),
 * {@code items} (optional comma list of category ids; absent/empty = the default set, which
 * is the categories flagged on in {@link StateExport#CAT_DEFAULTS}, not everything),
 * {@code progress_action} (optional), plus the reply trio {@code reply_action} /
 * {@code reply_package} / {@code reply_id}.</li>
 * <li>{@link #ACTION_LIST_CATEGORIES}: token-gated category enumeration for the caller's
 * item picker, one {@code id<TAB>label<TAB>parent<TAB>on|off} line each. This app's
 * categories are flat, so the parent field is always empty.</li>
 * </ul>
 *
 * <p>Reply: a FRESH broadcast to {@code reply_package} with action {@code reply_action},
 * extras {@code reply_id} (echoed verbatim) + {@code result} =
 * {@code OK:<path>|<bytes>|<human size>|<n> categories} for an export, {@code OK:} plus
 * {@code id<TAB>label} lines for the category list, or {@code ERROR:<short reason>}.
 * Exactly one terminal reply, single-fire guarded.
 *
 * <p><b>Do not "improve" the reply channel.</b> No ResultReceiver, no PendingIntent, no
 * Messenger: EMUI does not reliably carry a live Binder into another app's manifest
 * receiver and may drop the broadcast outright. Do not rely on the ordered-broadcast
 * result either — EMUI severs that channel between third-party apps. The plain reply
 * broadcast with {@link Intent#FLAG_INCLUDE_STOPPED_PACKAGES} is the only working path
 * (verified on the Mate XT, 2026-07-23); without the flag a stopped caller never hears us.
 */
public class StateExportReceiver extends BroadcastReceiver {
    static final String ACTION_EXPORT_STATE = BuildConfig.APPLICATION_ID + ".action.EXPORT_STATE";
    static final String ACTION_LIST_CATEGORIES = BuildConfig.APPLICATION_ID + ".action.LIST_CATEGORIES";

    // Contract extras — deliberately bare names, shared verbatim by every sister app
    private static final String EXTRA_TOKEN = "token";
    private static final String EXTRA_PATH = "path";
    private static final String EXTRA_ITEMS = "items";
    private static final String EXTRA_PROGRESS_ACTION = "progress_action";
    private static final String EXTRA_REPLY_ACTION = "reply_action";
    private static final String EXTRA_REPLY_PACKAGE = "reply_package";
    private static final String EXTRA_REPLY_ID = "reply_id";
    private static final String EXTRA_RESULT = "result";
    private static final String EXTRA_PROGRESS_APP = "app";
    private static final String EXTRA_PROGRESS_TEXT = "text";
    private static final String EXTRA_PROGRESS_CURRENT = "current";
    private static final String EXTRA_PROGRESS_TOTAL = "total";
    private static final String EXTRA_PROGRESS_UNIT = "unit";

    private static final long PROGRESS_INTERVAL = 500L; // ms, at most one broadcast per

    @Override
    public void onReceive(Context context, Intent intent) {
        final Context app = context.getApplicationContext();
        String action = intent.getAction();
        if (action == null)
            return;

        String token = intent.getStringExtra(EXTRA_TOKEN);
        final String replyAction = trimmed(intent.getStringExtra(EXTRA_REPLY_ACTION));
        final String replyPackage = trimmed(intent.getStringExtra(EXTRA_REPLY_PACKAGE));
        final String replyId = trimmed(intent.getStringExtra(EXTRA_REPLY_ID));
        final String progressAction = trimmed(intent.getStringExtra(EXTRA_PROGRESS_ACTION));
        final String pathOverride = trimmed(intent.getStringExtra(EXTRA_PATH));
        String items = trimmed(intent.getStringExtra(EXTRA_ITEMS));

        final Replier replier = new Replier(app, replyAction, replyPackage, replyId);

        EntityLog.log(app, "State export request action=" + action + " id=" + replyId +
                " from=" + replyPackage);

        // Gate first, and report "disabled" and "bad token" distinctly (they debug differently)
        if (!AutomationAuth.enabled(app)) {
            replier.reply("ERROR:automation disabled");
            return;
        }
        if (!AutomationAuth.isTokenValid(app, token)) {
            replier.reply("ERROR:bad token");
            return;
        }

        if (ACTION_LIST_CATEGORIES.equals(action)) {
            // id TAB label TAB parent TAB on|off - the parent field stays empty because this
            // app's categories are flat, and the fourth one is how the caller's picker learns
            // which of them start ticked (it is drawn fresh from this reply every time).
            StringBuilder sb = new StringBuilder("OK:");
            for (int i = 0; i < StateExport.CAT_IDS.length; i++) {
                if (i > 0)
                    sb.append('\n');
                sb.append(StateExport.CAT_IDS[i]).append('\t')
                        .append(app.getString(StateExport.CAT_LABELS[i])).append('\t')
                        .append('\t')
                        .append(StateExport.CAT_DEFAULTS[i] ? "on" : "off");
            }
            replier.reply(sb.toString());
            return;
        }

        if (!ACTION_EXPORT_STATE.equals(action)) {
            replier.reply("ERROR:unknown action: " + action);
            return;
        }

        // No items named means "your default set" - what this app recommends backing up,
        // never its whole footprint, which is why the heavy mail store is flagged off.
        final List<String> cats;
        if (TextUtils.isEmpty(items))
            cats = StateExport.defaultCats();
        else {
            cats = new ArrayList<>();
            for (String id : items.split(",")) {
                id = id.trim();
                if (id.isEmpty())
                    continue;
                if (!StateExport.isKnownCat(id)) {
                    replier.reply("ERROR:unknown category in items: " + items);
                    return;
                }
                if (!cats.contains(id))
                    cats.add(id);
            }
            if (cats.isEmpty())
                cats.addAll(StateExport.defaultCats());
        }

        final ProgressEmitter progress = new ProgressEmitter(
                app, progressAction, replyPackage, replyId, appLabel(app));

        // The export walks the database and writes a ZIP — hold the broadcast open and
        // finish from a background thread.
        final PendingResult pending = goAsync();
        Helper.getParallelExecutor().submit(new Runnable() {
            @Override
            public void run() {
                try {
                    String name = StateExport.exportFileName();
                    long bytes;
                    String shownPath;

                    boolean allFiles = (Build.VERSION.SDK_INT < Build.VERSION_CODES.R ||
                            Environment.isExternalStorageManager());

                    if (!TextUtils.isEmpty(pathOverride) && allFiles) {
                        // Absolute-directory override (MANAGE_EXTERNAL_STORAGE) — the normal
                        // automation route, so every sister app lands in one directory.
                        File dir = new File(pathOverride);
                        if (!dir.exists() && !dir.mkdirs())
                            throw new IOException("cannot create " + pathOverride);
                        if (!dir.isDirectory())
                            throw new IOException("not a directory: " + pathOverride);
                        File file = new File(dir, name);
                        try (OutputStream out = new FileOutputStream(file)) {
                            StateExport.export(app, cats, out, progress);
                        }
                        bytes = file.length();
                        shownPath = file.getAbsolutePath();
                    } else {
                        // No usable path override: fall back to the directory configured on
                        // the UI page, which is the only place we may write without All files
                        // access.
                        DocumentFile dir = StateExport.exportDir(app);
                        if (dir == null)
                            throw new IllegalStateException(
                                    TextUtils.isEmpty(pathOverride) ? "no-directory" : "no-storage-access");
                        DocumentFile file = dir.createFile(StateExport.EXPORT_MIME, name);
                        if (file == null)
                            throw new IOException("cannot create " + name + " in the export directory");
                        try (OutputStream out = app.getContentResolver().openOutputStream(file.getUri())) {
                            if (out == null)
                                throw new IOException("cannot open " + name + " for writing");
                            StateExport.export(app, cats, out, progress);
                        }
                        bytes = file.length();
                        shownPath = dirPath(dir) + "/" + (file.getName() == null ? name : file.getName());
                    }

                    progress.finish(cats.size());

                    String result = "OK:" + shownPath + "|" + bytes + "|" +
                            humanSize(bytes) + "|" + cats.size() + " categories";
                    EntityLog.log(app, "State export " + result);
                    replier.reply(result);
                } catch (Throwable ex) {
                    Log.w(ex);
                    String message = ex.getMessage();
                    if (TextUtils.isEmpty(message))
                        message = ex.getClass().getSimpleName();
                    EntityLog.log(app, "State export ERROR:" + message);
                    replier.reply("ERROR:" + message);
                } finally {
                    pending.finish();
                }
            }
        });
    }

    private static String trimmed(String value) {
        return (value == null ? "" : value.trim());
    }

    private static String appLabel(Context context) {
        try {
            return context.getPackageManager()
                    .getApplicationLabel(context.getApplicationInfo()).toString();
        } catch (Throwable ex) {
            return BuildConfig.APPLICATION_ID;
        }
    }

    /**
     * A readable filesystem path for a SAF tree, so the reply names a place 白い熊 can
     * find rather than a content URI. Falls back to the directory name.
     */
    private static String dirPath(DocumentFile dir) {
        try {
            String docId = DocumentsContract.getTreeDocumentId(dir.getUri());
            if (docId != null) {
                int colon = docId.indexOf(':');
                if (colon > 0) {
                    String volume = docId.substring(0, colon);
                    String relative = docId.substring(colon + 1);
                    if ("primary".equals(volume))
                        return "/storage/emulated/0/" + relative;
                    return "/storage/" + volume + "/" + relative;
                }
            }
        } catch (Throwable ex) {
            Log.w(ex);
        }
        String name = dir.getName();
        return (name == null ? dir.getUri().toString() : name);
    }

    static String humanSize(long bytes) {
        if (bytes >= (1L << 30))
            return String.format(Locale.ROOT, "%.2f GB", bytes / (double) (1L << 30));
        if (bytes >= (1L << 20))
            return String.format(Locale.ROOT, "%.1f MB", bytes / (double) (1L << 20));
        if (bytes >= (1L << 10))
            return String.format(Locale.ROOT, "%.1f KB", bytes / (double) (1L << 10));
        return bytes + " B";
    }

    /** Exactly one terminal reply per request, whichever path gets there first. */
    private static class Replier {
        private final Context context;
        private final String action;
        private final String pkg;
        private final String id;
        private final AtomicBoolean fired = new AtomicBoolean(false);

        Replier(Context context, String action, String pkg, String id) {
            this.context = context;
            this.action = action;
            this.pkg = pkg;
            this.id = id;
        }

        void reply(String result) {
            if (TextUtils.isEmpty(action) || TextUtils.isEmpty(pkg))
                return;
            if (!fired.compareAndSet(false, true))
                return;
            Intent intent = new Intent(action);
            intent.setPackage(pkg);
            intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
            intent.putExtra(EXTRA_REPLY_ID, id);
            intent.putExtra(EXTRA_RESULT, result);
            context.sendBroadcast(intent);
        }
    }

    /**
     * Progress broadcasts with real counts — never a percentage. Throttled to at most one
     * every {@link #PROGRESS_INTERVAL} ms, with a forced final one at completion.
     */
    private static class ProgressEmitter implements StateExport.Progress {
        private final Context context;
        private final String action;
        private final String pkg;
        private final String id;
        private final String label;
        private long last = 0;
        private String lastUnit = "";
        private long lastTotal = 0;

        ProgressEmitter(Context context, String action, String pkg, String id, String label) {
            this.context = context;
            this.action = action;
            this.pkg = pkg;
            this.id = id;
            this.label = label;
        }

        @Override
        public void report(long current, long total, String unit, String text) {
            lastUnit = unit;
            lastTotal = total;
            send(current, total, unit, text, false);
        }

        /** The mandatory final broadcast: the export is done, counts are complete. */
        void finish(int categories) {
            String unit = (TextUtils.isEmpty(lastUnit) ? "区分" : lastUnit);
            long total = (lastTotal > 0 ? lastTotal : categories);
            send(total, total, unit, unit + " " + total + "/" + total, true);
        }

        private void send(long current, long total, String unit, String text, boolean force) {
            if (TextUtils.isEmpty(action) || TextUtils.isEmpty(pkg))
                return;
            long now = SystemClock.elapsedRealtime();
            if (!force && now - last < PROGRESS_INTERVAL)
                return;
            last = now;

            Intent intent = new Intent(action);
            intent.setPackage(pkg);
            intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
            intent.putExtra(EXTRA_REPLY_ID, id);
            intent.putExtra(EXTRA_PROGRESS_APP, label);
            intent.putExtra(EXTRA_PROGRESS_TEXT, text);
            intent.putExtra(EXTRA_PROGRESS_CURRENT, current);
            intent.putExtra(EXTRA_PROGRESS_TOTAL, total);
            intent.putExtra(EXTRA_PROGRESS_UNIT, unit);
            context.sendBroadcast(intent);
        }
    }
}
