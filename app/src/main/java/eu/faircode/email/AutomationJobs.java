package eu.faircode.email;

import androidx.annotation.Nullable;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The jobs {@link AutomationProvider}'s data door has started, and the flag each of them
 * watches to stop.
 *
 * <p>What this owns is the mapping from the id a caller was handed to a cancellation it can
 * act on, which must outlive the binder call that created it and be reachable from a service
 * that never saw the caller. The flag is the very {@link AtomicBoolean} {@link StateExport}
 * already tests at its file boundaries, so a cancelled export unwinds and deletes what it was
 * writing by exactly the path the UI panel and the broadcast receiver use.
 *
 * <p>Process-local and removed on completion — never persisted. A stored "in progress" flag
 * survives a crash and wedges every later export for good.
 */
class AutomationJobs {
    private static final ConcurrentHashMap<String, AtomicBoolean> jobs = new ConcurrentHashMap<>();

    static String begin() {
        String id = UUID.randomUUID().toString();
        jobs.put(id, new AtomicBoolean(false));
        return id;
    }

    /** The flag the export tests, or null when the job is already finished or was never real. */
    @Nullable
    static AtomicBoolean flag(String jobId) {
        return (jobId == null ? null : jobs.get(jobId));
    }

    /**
     * Ask a job to stop. A no-op for an id that is finished or was never real — deliberately
     * silent, because a cancel arriving after the work completed is the normal race, not an
     * error, and answering it as one would make every well-behaved caller look broken.
     */
    static void cancel(@Nullable String jobId) {
        AtomicBoolean cancel = flag(jobId);
        if (cancel != null)
            cancel.set(true);
    }

    static boolean isCancelled(@Nullable String jobId) {
        AtomicBoolean cancel = flag(jobId);
        return (cancel != null && cancel.get());
    }

    static void finish(@Nullable String jobId) {
        if (jobId != null)
            jobs.remove(jobId);
    }
}
