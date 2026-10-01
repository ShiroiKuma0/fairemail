package eu.faircode.email;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

import androidx.core.app.NotificationCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

/**
 * Progress and cancellation of a whole folder fetch ("Fetch more/all messages", Entire folder).
 *
 * <p>The fetch itself is the upstream initialize pass in {@link Core}: the dialog sets the
 * folder initialize and keep values to {@link Integer#MAX_VALUE}, the next sync asks the server
 * for every message and pulls the headers in batches, and when the folder downloads message
 * texts a second pass is queued for them. Upstream shows none of this beyond a spinner, and has
 * no way to stop it. This class is the side channel: the sync loop reports where it is, the
 * folder list and a notification show it, and a stop request is a flag the loop checks between
 * batches.
 *
 * <p>Everything lives in memory. The sync runs in the same process as the UI, and a fetch that
 * outlives the process is resumed by the persisted initialize value anyway, at which point it
 * reports again from the start of the pass.
 */
class FullSync {
    static final int PHASE_LISTING = 0;
    static final int PHASE_HEADERS = 1;
    static final int PHASE_BODIES = 2;

    private static final long UI_INTERVAL = 500L; // ms
    private static final long NOTIFY_INTERVAL = 1000L; // ms, the system drops faster updates
    private static final String NOTIFICATION_TAG = "fullsync:";
    private static final String EXTRA_FOLDER = "folder";

    static class Progress {
        final long folder;
        final String name;
        volatile int phase;
        volatile int done;
        volatile int total;
        volatile boolean stopping;
        long notified = 0;

        Progress(long folder, String name) {
            this.folder = folder;
            this.name = name;
        }
    }

    private static final ExecutorService executor = Helper.getBackgroundExecutor(1, "fullsync");

    private static final Map<Long, Progress> running = new ConcurrentHashMap<>();
    private static final Set<Long> cancelled = Collections.newSetFromMap(new ConcurrentHashMap<>());
    // Folders started together from one dialog (a folder and its subfolders) stop together
    private static final Map<Long, Long> groups = new ConcurrentHashMap<>();
    private static final MutableLiveData<Integer> changed = new MutableLiveData<>(0);
    private static long ticked = 0;
    private static int tick = 0;

    /** Fires (throttled) whenever any folder progress changes, starts or ends. */
    static LiveData<Integer> getChanged() {
        return changed;
    }

    static Progress get(long folder) {
        return running.get(folder);
    }

    static boolean isRunning(long folder) {
        return running.containsKey(folder);
    }

    static void begin(Context context, EntityFolder folder, int phase, int total) {
        Progress p = running.get(folder.id);
        if (p == null) {
            p = new Progress(folder.id, folder.getDisplayName(context));
            running.put(folder.id, p);
        }
        p.phase = phase;
        p.done = 0;
        p.total = total;
        p.stopping = cancelled.contains(folder.id);
        report(context, p, true);
    }

    static void update(Context context, long folder, int done) {
        Progress p = running.get(folder);
        if (p == null)
            return;
        p.done = done;
        report(context, p, false);
    }

    /**
     * @param finished the pass ran to its end, stopped or not. A pass that failed is retried by
     *                 the operation queue with its original arguments, so a stop request has to
     *                 outlive it and turn that retry into an ordinary sync.
     */
    static void end(Context context, long folder, boolean finished) {
        if (finished) {
            cancelled.remove(folder);
            groups.remove(folder);
        }
        if (running.remove(folder) == null)
            return;
        try {
            NotificationManager nm = Helper.getSystemService(context, NotificationManager.class);
            nm.cancel(NOTIFICATION_TAG + folder, NotificationHelper.NOTIFICATION_FULL_SYNC);
        } catch (Throwable ex) {
            Log.w(ex);
        }
        signal(true);
    }

    /** Remembers which folders one dialog started, so that one stop request covers them all. */
    static void setGroup(List<Long> folders) {
        if (folders.size() < 2) {
            for (Long id : folders)
                groups.remove(id);
            return;
        }
        long group = SystemClock.elapsedRealtimeNanos();
        for (Long id : folders)
            groups.put(id, group);
    }

    /** The folder plus every folder started with it that has not finished yet. */
    static List<Long> getGroup(long folder) {
        List<Long> result = new ArrayList<>();
        result.add(folder);
        Long group = groups.get(folder);
        if (group != null)
            for (Map.Entry<Long, Long> e : groups.entrySet())
                if (group.equals(e.getValue()) && !e.getKey().equals(folder))
                    result.add(e.getKey());
        return result;
    }

    static String getStopTitle(Context context, long folder) {
        int n = getGroup(folder).size();
        return (n > 1
                ? context.getString(R.string.title_full_sync_stop_group, n)
                : context.getString(R.string.title_full_sync_stop));
    }

    /** Asks a running (or queued) whole folder fetch, and the ones started with it, to stop at the next batch. */
    static void cancel(Context context, long folder) {
        for (Long id : getGroup(folder))
            cancelOne(context, id);
    }

    private static void cancelOne(Context context, long folder) {
        cancelled.add(folder);
        Progress p = running.get(folder);
        if (p != null) {
            p.stopping = true;
            report(context, p, true);
        }
        // The persisted value is what would restart the fetch on the next sync
        DB.getInstance(context).folder().setFolderInitialize(folder, 0);
        EntityLog.log(context, "Full sync stop requested folder=" + folder);
    }

    static boolean isCancelled(long folder) {
        return cancelled.contains(folder);
    }

    /** A new fetch started from the dialog, or a stopped one consumed, must not inherit an old stop request. */
    static void clearCancel(long folder) {
        cancelled.remove(folder);
        groups.remove(folder);
    }

    static String describe(Context context, Progress p) {
        if (p.stopping)
            return context.getString(R.string.title_full_sync_stopping);
        NumberFormat NF = NumberFormat.getNumberInstance();
        switch (p.phase) {
            case PHASE_HEADERS:
                return context.getString(R.string.title_full_sync_headers,
                        NF.format(p.done), NF.format(p.total));
            case PHASE_BODIES:
                return context.getString(R.string.title_full_sync_bodies,
                        NF.format(p.done), NF.format(p.total));
            default:
                return context.getString(R.string.title_full_sync_listing);
        }
    }

    private static void report(Context context, Progress p, boolean now) {
        signal(now);

        long t = SystemClock.elapsedRealtime();
        if (!now && t - p.notified < NOTIFY_INTERVAL)
            return;
        p.notified = t;

        try {
            NotificationManager nm = Helper.getSystemService(context, NotificationManager.class);
            nm.notify(NOTIFICATION_TAG + p.folder, NotificationHelper.NOTIFICATION_FULL_SYNC,
                    getNotification(context, p));
        } catch (Throwable ex) {
            Log.w(ex);
        }
    }

    private static synchronized void signal(boolean now) {
        long t = SystemClock.elapsedRealtime();
        if (!now && t - ticked < UI_INTERVAL)
            return;
        ticked = t;
        changed.postValue(++tick);
    }

    private static Notification getNotification(Context context, Progress p) {
        Intent stop = new Intent(context, StopReceiver.class)
                .putExtra(EXTRA_FOLDER, p.folder);
        PendingIntent piStop = PendingIntent.getBroadcast(context, (int) p.folder, stop,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        boolean indeterminate = (p.stopping || p.phase == PHASE_LISTING || p.total <= 0);

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context, "progress")
                        .setSmallIcon(R.drawable.baseline_compare_arrows_white_24)
                        .setContentTitle(context.getString(R.string.title_full_sync_notification, p.name))
                        .setContentText(describe(context, p))
                        .setProgress(indeterminate ? 0 : p.total, indeterminate ? 0 : p.done, indeterminate)
                        .setOnlyAlertOnce(true)
                        .setAutoCancel(false)
                        .setShowWhen(false)
                        .setDefaults(0) // disable sound on pre Android 8
                        .setPriority(NotificationCompat.PRIORITY_LOW)
                        .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                        .setVisibility(NotificationCompat.VISIBILITY_SECRET)
                        .setLocalOnly(true)
                        .setOngoing(true);

        if (!p.stopping)
            builder.addAction(R.drawable.twotone_stop_24,
                    getStopTitle(context, p.folder), piStop);

        return builder.build();
    }

    /** The notification Stop button. Not exported: only our own PendingIntent reaches it. */
    public static class StopReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            long folder = intent.getLongExtra(EXTRA_FOLDER, -1L);
            if (folder < 0)
                return;
            Context app = context.getApplicationContext();
            PendingResult result = goAsync();
            executor.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        cancel(app, folder);
                    } catch (Throwable ex) {
                        Log.e(ex);
                    } finally {
                        result.finish();
                    }
                }
            });
        }
    }
}
