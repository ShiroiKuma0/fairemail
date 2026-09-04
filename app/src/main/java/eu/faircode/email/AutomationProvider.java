package eu.faircode.email;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

/**
 * The data door: export this app's own state, and put it back, for a caller we can identify.
 *
 * <p><b>Why a provider and not the broadcast receiver next to it.</b> Two reasons, and the
 * first is the whole point of the redesign. <b>A broadcast cannot tell you who sent it.</b>
 * The old contract's answer to that was a shared secret, which cannot survive the wipe this
 * feature exists to recover from. A provider gets the caller's identity from the framework —
 * see {@link AutomationCallers} for what is actually checked and why a package-name prefix
 * would have been worse than the token it replaced. And <b>a list needs a synchronous
 * answer</b>: 応用管理 draws a row per installed app before any export exists, and a broadcast
 * round trip per app to fill a list is the wrong shape entirely.
 *
 * <p><b>What does NOT happen here: the payload.</b> {@link #call} validates, starts a
 * foreground service and returns. This app's mail store is the largest export in the family —
 * gigabytes over minutes inside a binder call would block the caller, report no progress,
 * refuse cancellation and die silently if this process were killed.
 *
 * <p><b>Why a descriptor and not a path.</b> A backup is not a stable directory while it is
 * being assembled: 応用管理 writes into a temporary path and renames on commit, and it
 * encrypts and checksums <em>per file it knows about</em>. A file this app dropped into that
 * directory itself would be renamed out from under it, would sit in plaintext inside an
 * otherwise encrypted backup, and would be unverified rather than verified-and-failing. A
 * descriptor is also a capability that expires when it is closed. It also means the automation
 * path no longer needs All-files access, which was only ever required because the old contract
 * handed this app an absolute path.
 *
 * <p><b>{@code import} exists ONLY here.</b> It never gets a broadcast action: an import
 * overwrites this app's mail and accounts, and {@link StateExportReceiver} is exported with no
 * permission, so an import there would let any app on the phone wipe any sister app.
 */
public class AutomationProvider extends ContentProvider {
    static final String METHOD_DESCRIBE = "describe";
    static final String METHOD_EXPORT = "export";
    static final String METHOD_IMPORT = "import";
    static final String METHOD_CANCEL = "cancel";

    static final String KEY_RESULT = "result";
    static final String KEY_FD = "fd";
    static final String KEY_TOKEN = "token";
    static final String KEY_JOB_ID = "job_id";
    static final String KEY_ITEMS = "items";
    static final String KEY_REPLY_ACTION = "reply_action";
    static final String KEY_REPLY_PACKAGE = "reply_package";
    static final String KEY_PROGRESS_ACTION = "progress_action";

    /**
     * The oldest archive this build can still read. Version skew has a direction: old data
     * into a newer app is normally fine, because an app migrates its own storage; newer data
     * into an older app is not. This is what lets a caller refuse the second case at discovery
     * time rather than halfway through a stream. One, because the import still accepts the
     * bare stock-format JSON that predates the family ZIP.
     */
    static final int MIN_FORMAT_READABLE = 1;

    @Override
    public boolean onCreate() {
        return true;
    }

    /**
     * Every method answers a {@link Bundle} with {@link #KEY_RESULT} — {@code OK…} or
     * {@code ERROR:…}, the same vocabulary the broadcast contract uses, so a caller has one
     * grammar to parse rather than two.
     *
     * <p>A refusal is returned, never thrown: an exception across a binder reaches the caller
     * as a {@code RuntimeException} with our stack trace in it, which tells 白い熊 nothing and
     * tells a misbehaving caller rather more than it should.
     */
    @Nullable
    @Override
    public Bundle call(@NonNull String method, @Nullable String arg, @Nullable Bundle extras) {
        try {
            Context context = getContext();
            if (context == null)
                return result("ERROR:not ready");
            final Context app = context.getApplicationContext();

            // WHO, before WHAT. A caller we cannot identify gets the same answer whatever it
            // asked for.
            String refused = AutomationCallers.refuse(app, getCallingPackage());
            if (refused != null) {
                Log.i("Automation door " + refused);
                return result(refused);
            }

            // Then this app's own switches — a token is ignored unless this app asks for one.
            refused = AutomationAuth.refuse(app, extras == null ? null : extras.getString(KEY_TOKEN));
            if (refused != null)
                return result(refused);

            // Log.i and not EntityLog.log: a provider is published BEFORE Application.onCreate,
            // and on a clean phone a call() is what starts this process at all - so a describe
            // can land on a binder thread while the main thread is still initialising. EntityLog
            // writes a row, which would build the Room database right there. Nothing on this
            // path may be heavier than the manifest, a plain array and SharedPreferences read
            // directly; everything else belongs in the service, whose onStartCommand is queued
            // behind Application.onCreate.
            Log.i("Automation door " + method + " from=" + getCallingPackage());

            switch (method) {
                case METHOD_DESCRIBE:
                    return result("OK:" + describe(app));
                case METHOD_EXPORT:
                    return start(app, extras, false);
                case METHOD_IMPORT:
                    return start(app, extras, true);
                case METHOD_CANCEL:
                    AutomationJobs.cancel(extras == null ? null : extras.getString(KEY_JOB_ID));
                    return result("OK:cancelled");
                default:
                    return result("ERROR:unknown method: " + method);
            }
        } catch (Throwable ex) {
            Log.w(ex);
            String message = ex.getMessage();
            if (message == null || message.isEmpty())
                message = ex.getClass().getSimpleName();
            return result("ERROR:" + message);
        }
    }

    /**
     * What this app would export, answered without exporting anything.
     *
     * <p>Returned from the call rather than written into the archive, deliberately: 応用管理
     * must draw a row before an export exists, and at restore must judge compatibility
     * <em>before</em> streaming tens of megabytes into an app that would reject them — which
     * it cannot do if the header is buried inside an encrypted archive.
     *
     * <p>{@code contains} names the default set, not the whole catalogue: the local mail store
     * is opt-in per export because it re-syncs from the server using the account definitions
     * the backup already carries.
     */
    private String describe(Context context) throws Throwable {
        JSONArray jcontains = new JSONArray();
        List<String> cats = StateExport.defaultCats();
        for (String cat : cats)
            // Said plainly rather than left to be discovered: the accounts category carries
            // every account's and identity's password, and for an OAuth account that column IS
            // the token - and this app's archive is plaintext JSON in an unencrypted ZIP. That
            // belongs where 白い熊 reads what a backup contains, not in a source comment.
            jcontains.put(StateExport.CAT_ACCOUNTS.equals(cat)
                    ? context.getString(R.string.title_ui_automation_contains_credentials)
                    : context.getString(StateExport.catLabel(cat)));

        JSONObject jheader = new JSONObject();
        jheader.put("app_id", BuildConfig.APPLICATION_ID);
        jheader.put("version_code", BuildConfig.VERSION_CODE);
        jheader.put("version_name", BuildConfig.VERSION_NAME);
        jheader.put("format", StateExport.VERSION);
        jheader.put("min_format_readable", MIN_FORMAT_READABLE);
        // Nothing here writes first-run defaults an import would have to merge against that
        // starting the process has not already written.
        jheader.put("requires_launch_first", false);
        // Empty, and checked rather than assumed from what kind of app this is. The question is
        // which provider the IMPORT writes to, not what the app does: FairEmail's contacts are
        // its own Room rows (EntityContact), not ContactsContract, and every other category is
        // its own database, its own preferences or its own files. Nothing on the restore path
        // touches a permission-guarded system provider, so a freshly installed app with no
        // runtime grants can be restored in full.
        jheader.put("requires_permissions", new JSONArray());
        jheader.put("contains", jcontains);
        return jheader.toString();
    }

    /**
     * Hand the descriptor to a foreground service and get out of the way.
     *
     * <p>The descriptor is <b>duplicated</b> before it leaves this method. The one in
     * {@code extras} belongs to the binder transaction and is closed when {@code call()}
     * returns; a service reading it afterwards would find it shut. That is a bug you only see
     * under load, so it is not left to the service to remember.
     */
    private Bundle start(Context context, @Nullable Bundle extras, boolean importing) {
        if (extras == null)
            return result("ERROR:no descriptor");

        ParcelFileDescriptor fd;
        try {
            extras.setClassLoader(ParcelFileDescriptor.class.getClassLoader());
            fd = extras.getParcelable(KEY_FD);
        } catch (Throwable ex) {
            Log.w(ex);
            fd = null;
        }
        if (fd == null)
            return result("ERROR:no descriptor");

        ParcelFileDescriptor dup;
        try {
            dup = fd.dup();
        } catch (Throwable ex) {
            Log.w(ex);
            return result("ERROR:descriptor unusable");
        }

        String jobId = AutomationJobs.begin();
        try {
            AutomationDataService.start(context, jobId, dup, importing, extras);
        } catch (Throwable ex) {
            Log.w(ex);
            AutomationJobs.finish(jobId);
            try {
                dup.close();
            } catch (Throwable ignored) {
            }
            String message = ex.getMessage();
            if (message == null || message.isEmpty())
                message = ex.getClass().getSimpleName();
            return result("ERROR:" + message);
        }
        return result("OK:" + jobId);
    }

    private static Bundle result(String result) {
        Bundle bundle = new Bundle();
        bundle.putString(KEY_RESULT, result);
        return bundle;
    }

    // A provider that is only ever call()ed still has to answer these. Refusing loudly beats
    // returning an empty cursor, which reads downstream as "there is no data" rather than
    // "wrong door".
    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection,
                        @Nullable String[] args, @Nullable String sort) {
        throw new UnsupportedOperationException("automation is call() only");
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        throw new UnsupportedOperationException("automation is call() only");
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] args) {
        throw new UnsupportedOperationException("automation is call() only");
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection,
                      @Nullable String[] args) {
        throw new UnsupportedOperationException("automation is call() only");
    }
}
