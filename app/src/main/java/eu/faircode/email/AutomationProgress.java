package eu.faircode.email;

import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.text.TextUtils;

import java.util.Timer;
import java.util.TimerTask;

/**
 * The ONE progress sender both automation doors use — {@link StateExportReceiver}'s §1 export
 * and {@link AutomationDataService}'s §2a data export.
 *
 * <p>Parameterised on the correlation id rather than written twice: two implementations of the
 * same watchdog drift, and the one that drifts is always the one nobody is looking at. The id
 * goes out as both {@code reply_id} and {@code job_id}, so one reader serves both doors —
 * §1 names a run by the former, the data door by the latter.
 *
 * <p><b>Real numbers, never a percentage</b> (白い熊's explicit requirement), throttled to at
 * most one broadcast every {@link #INTERVAL} ms, with a mandatory final one at completion.
 *
 * <p><b>A throttle is not a heartbeat.</b> They solve opposite problems: the throttle caps a
 * chatty engine, the heartbeat covers one that is not chatty at all. This app's export reports
 * once per category, so a single category — zipping a mail store that can reach gigabytes —
 * ticks once and then says nothing for far longer than the two minutes after which the caller
 * presumes the app dead. So a timer re-sends the last <em>true</em> line every
 * {@link #HEARTBEAT} ms. It never invents a moving number: a fabricated count is worse than a
 * repeated one, because it cannot be told apart from progress.
 *
 * <p><b>{@code setPackage} is unconditional, and nothing is sent without it.</b> Since API 26
 * an implicit broadcast reaches no manifest-declared receiver at all, so a progress broadcast
 * without a package is not a weak one — it is no broadcast. Without a reply package there is
 * nobody to tell, so this sends nothing rather than pretending.
 */
class AutomationProgress implements StateExport.Progress {
    private static final long INTERVAL = 500L; // ms, at most one broadcast per
    private static final long HEARTBEAT = 20000L; // ms, re-send the last true line at least this often

    private final Context context;
    private final String action;
    private final String pkg;
    private final String id;
    private final String label;

    private Timer timer;
    private long last = 0;
    private long current = 0;
    private long total = 0;
    private long bytes = 0;
    private String unit = "";
    private String text = "";

    AutomationProgress(Context context, String action, String pkg, String id, String label) {
        this.context = context;
        this.action = action;
        this.pkg = pkg;
        this.id = id;
        this.label = label;
    }

    /** Nothing is ever sent without both an action and a package to aim it at. */
    private boolean silent() {
        return (TextUtils.isEmpty(action) || TextUtils.isEmpty(pkg));
    }

    /**
     * Start the heartbeat. Only the data door needs it: §1 writes to a local file and can only
     * be as slow as this app is, while §2a writes into a descriptor the caller supplied, which
     * may be a pipe — so a category blocks for exactly as long as the caller is slow to drain
     * it, however little data there is.
     */
    void beat() {
        if (silent() || timer != null)
            return;
        timer = new Timer("automation-progress", true);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                synchronized (AutomationProgress.this) {
                    if (SystemClock.elapsedRealtime() - last >= HEARTBEAT)
                        send(true);
                }
            }
        }, HEARTBEAT, HEARTBEAT);
    }

    @Override
    public synchronized void report(long current, long total, String unit, String text) {
        this.current = current;
        this.total = total;
        this.unit = unit;
        this.text = text;
        send(false);
    }

    /** The second counter, for a caller that can show bytes beside the item count. */
    synchronized void written(long bytes) {
        this.bytes = bytes;
        send(false);
    }

    /**
     * The import's counter, carried by the fields the contract actually defines.
     *
     * <p>{@link #written(long)} puts the same number in the additive {@code bytes} extra, which
     * is the right place beside a category count and the wrong place on its own: the caller
     * reads {@code current}/{@code total}/{@code unit} and knows nothing about a {@code bytes}
     * extra at all. Nothing calls {@link #report} while an archive is being spooled — an import
     * has no category to count until the whole thing has arrived — so an import that set only
     * {@code bytes} broadcast 0/0 for its entire length while tens of megabytes went past, and
     * every number it did send was dropped at the receiver. Here the bytes ARE the progress, so
     * they travel in the field that is read.
     *
     * <p>The total stays 0 because a descriptor has no length this app may ask for: it can be a
     * pipe, and an invented total is worse than none. The caller renders that as N/0 — a number
     * that climbs, which is the whole of what is wanted.
     */
    synchronized void spooled(long bytes) {
        this.bytes = bytes;
        // The sister apps' unit for a byte count, read literally by the caller: not a
        // translated word, however Japanese the item units beside it are.
        report(bytes, 0, "bytes", "書庫を受信 " + Helper.humanReadableByteCount(bytes));
    }

    /** The mandatory final broadcast: the export is done, counts are complete. */
    synchronized void finish(int categories) {
        if (total <= 0) {
            current = categories;
            total = categories;
        }
        if (TextUtils.isEmpty(unit))
            unit = "区分";
        if (TextUtils.isEmpty(text))
            text = unit + " " + total + "/" + total;
        send(true);
    }

    void stop() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    private void send(boolean force) {
        if (silent())
            return;
        long now = SystemClock.elapsedRealtime();
        if (!force && now - last < INTERVAL)
            return;
        last = now;

        Intent intent = new Intent(action);
        intent.setPackage(pkg);
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        intent.putExtra("reply_id", id);
        intent.putExtra("job_id", id);
        intent.putExtra("app", label);
        intent.putExtra("text", text);
        intent.putExtra("current", current);
        intent.putExtra("total", total);
        intent.putExtra("unit", unit);
        // Additive: omitted entirely where the byte count means nothing, as on the §1 path,
        // which lets the export directory own the stream.
        if (bytes > 0)
            intent.putExtra("bytes", bytes);
        context.sendBroadcast(intent);
    }
}
