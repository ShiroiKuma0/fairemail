package eu.faircode.email;

import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.os.PowerManager;
import android.text.TextUtils;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Where a data export or import started at {@link AutomationProvider} actually runs.
 *
 * <p><b>Why a foreground service and not the provider call.</b> The call returns in
 * milliseconds; this runs for minutes. A binder call holds the caller — 応用管理 is drawing a
 * list, and a multi-minute synchronous call would freeze its UI, report no progress and refuse
 * cancellation. And <b>a backgrounded app writing for minutes is frozen mid-stream on this
 * phone</b>, which yields a truncated archive underneath a success reply: the worst possible
 * failure, because it is indistinguishable from a good backup until the day it is restored.
 * This app holds the largest export in the family, so that is not a theoretical risk here.
 *
 * <p><b>The descriptor</b> was already duplicated by {@link AutomationProvider} before it got
 * here, because the original belongs to the binder transaction and is closed the moment
 * {@code call()} returns. This service owns the copy and closes it on every path out —
 * leaking one would hold the caller's file open indefinitely, and the caller cannot checksum
 * or encrypt a file that is still open.
 *
 * <p><b>The ordering in {@link #onStartCommand} is a recipe, not a set of independent rules.</b>
 * Two requirements pull in opposite directions: the descriptor wants an owner before anything
 * that can throw, and {@code startForeground} has to happen before anything that can return —
 * once a caller has invoked {@code startForegroundService} the platform kills the process with
 * {@code ForegroundServiceDidNotStartInTimeException} whatever this service then decides, so a
 * caller retrying with a stale job id would <em>crash the app it is backing up</em>. Only one
 * order satisfies both: <b>read the extras defensively → go foreground inside a try → drain the
 * handover → then the early returns</b>, each of which stops the foreground behind it.
 *
 * <p><b>The wakelock</b> is held across the work because EMUI dozes the CPU with the screen
 * off and a foreground service alone does not stop it; whether the system honours the lock at
 * all depends on this app being exempt from battery optimisation, which FairEmail already asks
 * for on its own account.
 */
public class AutomationDataService extends ServiceBase {
    private static final String EXTRA_JOB = "job";
    private static final String EXTRA_IMPORTING = "importing";

    /**
     * Same series as {@link NotificationHelper}'s foreground ids (synchronize 100 … TTS 600),
     * kept here because this is the fork's own service and nothing upstream needs to know it.
     */
    private static final int NOTIFICATION_AUTOMATION = 700;

    private static final int SPOOL_BUFFER = 65536;

    /**
     * The descriptor's way across, because an Intent is the wrong vehicle for one: a
     * {@link ParcelFileDescriptor} in an extra is duplicated by the system on delivery and the
     * copy's lifetime stops being ours to reason about. Handing it through a map keyed by the
     * job id keeps exactly one open descriptor with exactly one owner.
     */
    private static final ConcurrentHashMap<String, ParcelFileDescriptor> handover =
            new ConcurrentHashMap<>();

    /** The job this instance is running, so a system timeout knows what to unwind. */
    private volatile String running;

    /**
     * Start the service, or throw so the provider can close the descriptor it duplicated and
     * drop the job. This is a background start: on API 31+ it can be refused outright unless
     * this app is exempt from battery optimisation, and a stranded descriptor in a map nothing
     * will ever read is the caller's own file held open forever.
     */
    static void start(Context context, String jobId, ParcelFileDescriptor fd,
                      boolean importing, @Nullable Bundle extras) {
        handover.put(jobId, fd);
        Intent intent = new Intent(context, AutomationDataService.class);
        intent.putExtra(EXTRA_JOB, jobId);
        intent.putExtra(EXTRA_IMPORTING, importing);
        if (extras != null) {
            intent.putExtra(AutomationProvider.KEY_ITEMS,
                    extras.getString(AutomationProvider.KEY_ITEMS));
            intent.putExtra(AutomationProvider.KEY_REPLY_ACTION,
                    extras.getString(AutomationProvider.KEY_REPLY_ACTION));
            intent.putExtra(AutomationProvider.KEY_REPLY_PACKAGE,
                    extras.getString(AutomationProvider.KEY_REPLY_PACKAGE));
            intent.putExtra(AutomationProvider.KEY_PROGRESS_ACTION,
                    extras.getString(AutomationProvider.KEY_PROGRESS_ACTION));
        }
        try {
            context.startForegroundService(intent);
        } catch (Throwable ex) {
            handover.remove(jobId);
            throw ex;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        // Defensively, and BEFORE anything that can return: the notification wants importing
        // out of the very intent whose job id might be missing, and reading the id first is the
        // natural way to write it - which is exactly how the crash on a null intent is written.
        final boolean importing = (intent != null && intent.getBooleanExtra(EXTRA_IMPORTING, false));
        boolean foreground;
        try {
            goForeground(importing);
            foreground = true;
        } catch (Throwable ex) {
            // Guarded because the start may itself have been refused, and a throw here would be
            // the very crash this ordering exists to avoid.
            Log.w(ex);
            foreground = false;
        }

        final String jobId = (intent == null ? null : intent.getStringExtra(EXTRA_JOB));
        final ParcelFileDescriptor fd = (jobId == null ? null : handover.remove(jobId));
        if (fd == null) {
            // A stale or already-claimed job id stops SILENTLY. The instinct is to answer
            // "ERROR:unknown job"; that id's request has already had its one terminal reply,
            // and a second one breaks the single-reply rule the whole contract rests on.
            return stop(startId);
        }

        final String items = intent.getStringExtra(AutomationProvider.KEY_ITEMS);
        final String replyAction = intent.getStringExtra(AutomationProvider.KEY_REPLY_ACTION);
        final String replyPackage = intent.getStringExtra(AutomationProvider.KEY_REPLY_PACKAGE);
        final String progressAction = intent.getStringExtra(AutomationProvider.KEY_PROGRESS_ACTION);

        final Context context = getApplicationContext();
        final Replier replier = new Replier(context, replyAction, replyPackage, jobId);
        final AutomationProgress progress = new AutomationProgress(
                context, progressAction, replyPackage, jobId, appLabel(context));
        final int id = startId;

        // One flag rather than one guard per failure: the window is between draining the map
        // and the worker taking ownership, and going foreground is only one way to leave it.
        boolean handedOff = false;
        try {
            if (!foreground) {
                // The descriptor has left the map by now, so nothing else would ever close it -
                // and the caller is holding an OK for work that cannot run. Answer rather than
                // die quietly: this shows up only on a phone without the battery-optimisation
                // exemption, which is precisely the clean-phone case.
                replier.reply("ERROR:cannot go foreground");
                return stop(startId);
            }

            running = jobId;
            EntityLog.log(context, "Automation data " + (importing ? "import" : "export") +
                    " job=" + jobId + " items=" + items);

            Helper.getParallelExecutor().submit(new Runnable() {
                @Override
                public void run() {
                    PowerManager pm = Helper.getSystemService(context, PowerManager.class);
                    PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                            BuildConfig.APPLICATION_ID + ":automation");
                    try {
                        wl.acquire();
                        progress.beat();
                        if (importing)
                            runImport(context, jobId, fd, items, progress, replier);
                        else
                            runExport(context, jobId, fd, items, progress, replier);
                    } catch (Throwable ex) {
                        if (AutomationJobs.isCancelled(jobId) ||
                                ex instanceof StateExport.CancelledException) {
                            EntityLog.log(context, "Automation data ERROR:cancelled job=" + jobId);
                            replier.reply("ERROR:cancelled");
                        } else {
                            Log.w(ex);
                            String message = ex.getMessage();
                            if (TextUtils.isEmpty(message))
                                message = ex.getClass().getSimpleName();
                            EntityLog.log(context, "Automation data ERROR:" + message);
                            replier.reply("ERROR:" + message);
                        }
                    } finally {
                        // Whatever happened, the caller gets exactly one answer and its file is
                        // let go of: it cannot checksum or encrypt a file this app still holds
                        // open. The guard inside makes this a no-op on every path that already
                        // answered; it exists so a caller can never wait on a run that ended.
                        replier.reply("ERROR:ended without a result");
                        progress.stop();
                        running = null;
                        AutomationJobs.finish(jobId);
                        close(fd);
                        if (wl.isHeld())
                            wl.release();
                        stopForeground(true);
                        stopSelf(id);
                    }
                }
            });
            handedOff = true;
            return START_NOT_STICKY;
        } finally {
            if (!handedOff) {
                progress.stop();
                replier.reply("ERROR:could not start the export");
                AutomationJobs.finish(jobId);
                close(fd);
            }
        }
    }

    /**
     * Write ONE backup ZIP straight into the caller's descriptor.
     *
     * <p>The byte count is taken as it goes rather than stat'ed afterwards: the caller owns the
     * file and this app may not be able to see it at all — it can be a pipe, or a descriptor
     * into a directory this app cannot list.
     */
    private void runExport(Context context, String jobId, ParcelFileDescriptor fd, String items,
                           AutomationProgress progress, Replier replier) throws Throwable {
        List<String> cats = resolve(items);
        if (cats == null) {
            replier.reply("ERROR:unknown category in items: " + items);
            return;
        }

        AtomicBoolean cancel = AutomationJobs.flag(jobId);
        Counter counter;
        try (OutputStream out = new ParcelFileDescriptor.AutoCloseOutputStream(fd)) {
            counter = new Counter(out, progress);
            StateExport.export(context, cats, counter, progress, cancel);
            counter.flush();
        }

        if (AutomationJobs.isCancelled(jobId)) {
            replier.reply("ERROR:cancelled");
            return;
        }

        progress.finish(cats.size());
        String result = "OK:" + counter.written + "|" + cats.size() + " categories";
        EntityLog.log(context, "Automation data " + result + " job=" + jobId);
        replier.reply(result);
    }

    /**
     * Spool the descriptor to disk, then restore from the spool.
     *
     * <p>The import reads a backup twice — the export JSON, then the mail store, whose index
     * entry has to come past before the payload entries it names — and a descriptor cannot be
     * rewound, so a private copy is the honest price. <b>To disk, never into a byte array</b>:
     * an email store is the largest archive in this family by some margin, and holding one in
     * RAM to sniff it is the difference between a slow restore and no restore. The guarantee is
     * unchanged — nothing is applied until the whole archive has arrived — only the bound moves
     * from memory to disk. It is deleted in a {@code finally}: a spool left behind after a
     * crash would be a complete unencrypted copy of a backup sitting in app storage.
     */
    private void runImport(Context context, String jobId, ParcelFileDescriptor fd, String items,
                           AutomationProgress progress, Replier replier) throws Throwable {
        File dir = new File(context.getCacheDir(), "automation");
        if (!dir.exists() && !dir.mkdirs())
            throw new IOException("cannot create " + dir.getAbsolutePath());
        File spool = new File(dir, jobId + ".zip");

        try {
            long written = 0;
            try (InputStream in = new ParcelFileDescriptor.AutoCloseInputStream(fd);
                 OutputStream out = new FileOutputStream(spool)) {
                byte[] buffer = new byte[SPOOL_BUFFER];
                int read;
                while ((read = in.read(buffer)) > 0) {
                    if (AutomationJobs.isCancelled(jobId))
                        throw new StateExport.CancelledException();
                    out.write(buffer, 0, read);
                    written += read;
                    progress.written(written);
                }
            }
            if (written == 0) {
                replier.reply("ERROR:empty archive");
                return;
            }

            StateExport.Source source = StateExport.sourceOf(spool);

            // Every category the archive actually carries, not every category this app knows
            // about: asking for one the archive lacks is how a restore ends up reporting
            // success over nothing. A caller that named items narrows that, never widens it.
            List<String> present = StateExport.categoriesIn(source);
            List<String> wanted = resolve(items);
            if (wanted == null) {
                replier.reply("ERROR:unknown category in items: " + items);
                return;
            }
            List<String> cats = new ArrayList<>();
            for (String cat : present)
                if (wanted.contains(cat))
                    cats.add(cat);
            if (cats.isEmpty()) {
                replier.reply("ERROR:archive carries no categories");
                return;
            }

            String summary = StateExport.performImport(context, cats, source,
                    "automation job=" + jobId);

            // 応用管理 force-stops this app the instant it hears success, with a SIGKILL that no
            // orderly shutdown follows - which is what makes StateExport commit its preferences
            // synchronously rather than apply() them. Everything the restore touched is on disk
            // by the time this line runs; see performImport.
            int restored = (TextUtils.isEmpty(summary) ? 0 : summary.split("\n").length);
            String result = "OK:" + restored + " restored";
            EntityLog.log(context, "Automation data " + result + " job=" + jobId);
            replier.reply(result);
        } finally {
            if (spool.exists() && !spool.delete())
                Log.w("Cannot delete " + spool.getAbsolutePath());
        }
    }

    /** Named items, validated; null when one of them is not a category this app has. */
    @Nullable
    private static List<String> resolve(String items) {
        if (TextUtils.isEmpty(items))
            return StateExport.defaultCats();
        List<String> cats = new ArrayList<>();
        for (String id : items.split(",")) {
            id = id.trim();
            if (id.isEmpty())
                continue;
            if (!StateExport.isKnownCat(id))
                return null;
            if (!cats.contains(id))
                cats.add(id);
        }
        return (cats.isEmpty() ? StateExport.defaultCats() : cats);
    }

    @Override
    public void onTimeout(int startId) {
        // Android 15 and later time a specialUse foreground service out. Unwind at the next
        // write boundary and let the normal path send the one terminal reply, rather than being
        // torn down mid-write with a caller still waiting.
        Log.e(new Throwable("Automation data onTimeout job=" + running));
        AutomationJobs.cancel(running);
    }

    /**
     * The typed overload only where it exists. This is the one place EMUI's {@code SDK_INT=31}
     * on a platform based on Android 13 is a live hazard rather than a curiosity, so the call is
     * guarded by the caller rather than trusted in either branch.
     */
    private void goForeground(boolean importing) {
        Notification notification = getNotification(importing);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
            startForeground(NOTIFICATION_AUTOMATION, notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        else
            startForeground(NOTIFICATION_AUTOMATION, notification);
    }

    /**
     * Every bail-out runs after the foreground call, so each one has to take it back down —
     * otherwise a stale job id leaves a live notification and a foreground service behind.
     */
    private int stop(int startId) {
        stopForeground(true);
        stopSelf(startId);
        return START_NOT_STICKY;
    }

    private static void close(ParcelFileDescriptor fd) {
        try {
            fd.close();
        } catch (Throwable ex) {
            Log.w(ex);
        }
    }

    private Notification getNotification(boolean importing) {
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this, "progress")
                        .setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_DEFERRED)
                        .setSmallIcon(R.drawable.baseline_compare_arrows_white_24)
                        .setContentTitle(getString(importing
                                ? R.string.title_ui_automation_data_import
                                : R.string.title_ui_automation_data_export))
                        .setAutoCancel(false)
                        .setShowWhen(false)
                        .setDefaults(0) // disable sound on pre Android 8
                        .setPriority(NotificationCompat.PRIORITY_LOW)
                        .setCategory(NotificationCompat.CATEGORY_SERVICE)
                        .setVisibility(NotificationCompat.VISIBILITY_SECRET)
                        .setLocalOnly(true)
                        .setOngoing(true);

        Notification notification = builder.build();
        notification.flags |= Notification.FLAG_NO_CLEAR;
        return notification;
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
     * Counts what goes through, and lets the progress sender turn that into a byte counter.
     *
     * <p>A single long step — zipping one enormous attachment — moves no category counter at
     * all. Bytes always move, so the caller sees the export is alive even mid-category.
     */
    private static class Counter extends OutputStream {
        /** Bytes between two looks at the clock: the sender throttles, this stops it costing. */
        private static final long TICK = 65536;

        private final OutputStream out;
        private final AutomationProgress progress;
        private long written = 0;
        private long ticked = 0;

        Counter(OutputStream out, AutomationProgress progress) {
            this.out = out;
            this.progress = progress;
        }

        @Override
        public void write(int b) throws IOException {
            out.write(b);
            written++;
            tick();
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            out.write(b, off, len);
            written += len;
            tick();
        }

        private void tick() {
            if (written - ticked < TICK)
                return;
            ticked = written;
            progress.written(written);
        }

        @Override
        public void flush() throws IOException {
            out.flush();
        }
    }

    /**
     * Exactly one terminal reply per job, whichever path gets there first.
     *
     * <p>No package to aim at means nobody can hear it: since API 26 an implicit broadcast
     * reaches no manifest-declared receiver, so {@code setPackage(null)} is not a wider send,
     * it is no send. This skips it rather than pretending.
     */
    private static class Replier {
        private final Context context;
        private final String action;
        private final String pkg;
        private final String jobId;
        private final AtomicBoolean fired = new AtomicBoolean(false);

        Replier(Context context, String action, String pkg, String jobId) {
            this.context = context;
            this.action = action;
            this.pkg = pkg;
            this.jobId = jobId;
        }

        void reply(String result) {
            if (TextUtils.isEmpty(action) || TextUtils.isEmpty(pkg))
                return;
            if (!fired.compareAndSet(false, true))
                return;
            Intent intent = new Intent(action);
            intent.setPackage(pkg);
            // Without this a caller that has been backgrounded never hears the answer, and on a
            // clean phone the caller may not have been launched at all.
            intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
            intent.putExtra(AutomationProvider.KEY_JOB_ID, jobId);
            intent.putExtra(AutomationProvider.KEY_RESULT, result);
            context.sendBroadcast(intent);
        }
    }
}
