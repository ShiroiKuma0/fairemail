package eu.faircode.email;

import static eu.faircode.email.ServiceAuthenticator.AUTH_TYPE_GMAIL;

import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.util.Base64;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.documentfile.provider.DocumentFile;
import androidx.preference.PreferenceManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.mail.Address;

/**
 * The headless export/import core of the 白い熊 UI page, shared by the panel in
 * {@link FragmentOptionsUi} and by the automation surface in
 * {@link StateExportReceiver} — neither duplicates any of it.
 *
 * <p>A backup is ONE ZIP named by the sister-app family convention,
 * {@code shiroikuma-fairemail_<yyyy-MM-dd_HH-mm-ss>.zip}, holding
 * {@link #ENTRY_MANIFEST} (format/version/app/appVersion/createdTs/categories) and
 * {@link #ENTRY_EXPORT}, which is the stock unencrypted FairEmail export JSON so an
 * unzipped backup stays importable by upstream builds. The import accepts the ZIP,
 * an older bare {@code .json} fork export, and a stock upstream backup alike.
 */
public class StateExport {
    /**
     * The export directory (a SAF tree URI) lives in its OWN preferences file, outside
     * the default store the exporter serializes, so an export never carries a
     * device-local URI — and, per the automation contract, never the automation token
     * either (that has its own file too, see {@link AutomationAuth}).
     */
    static final String EXIM_PREFS = "shiroikuma_eximport";
    static final String KEY_DIR_URI = "dir_uri";

    /**
     * Mandatory family convention: {@code <english-dash-separated-app-name>_<stamp>.zip},
     * no version and no other decoration, so every 白い熊 app's backups sort and read
     * uniformly in one directory. {@link #LEGACY_EXPORT_PREFIX} keeps the pre-convention
     * exports (which carried the version and were bare JSON) recognised by the
     * "last export" query.
     */
    static final String EXPORT_PREFIX = "shiroikuma-fairemail_";
    static final String LEGACY_EXPORT_PREFIX = "shiroikuma-fairemail-";
    static final String EXPORT_MIME = "application/zip";

    static final String FORMAT = "shiroikuma-fairemail";
    /**
     * 2 since 2026-09-08, when {@link #CAT_ATTACHMENTS} split off {@link #CAT_MESSAGES} and the
     * manifest's category vocabulary gained an id.
     *
     * <p>Both directions still work and neither needs a branch, which is why
     * {@code MIN_FORMAT_READABLE} stays at 1. A format 1 archive names only {@code messages}
     * and carries its attachment payloads inside it; they restore because the payload pass is
     * driven by the entries actually present, not by the category list. A format 2 archive read
     * by a build that predates the split loses the unknown id from its category list and
     * restores the payloads the same way.
     */
    static final int VERSION = 2;
    static final String ENTRY_MANIFEST = "manifest.json";
    static final String ENTRY_EXPORT = "fairemail-export.json";

    /**
     * The locally stored mail rides in the same ZIP: one JSON-Lines record per message in
     * {@link #ENTRY_MESSAGES_INDEX} (written FIRST, because the importer walks the archive
     * once and must insert a message row before its payload entries arrive), then
     * {@code messages/<n>/body.html}, {@code raw.eml} and {@code att-<n>} for each.
     * JSON Lines rather than one array so neither side ever holds the whole index in memory.
     */
    static final String ENTRY_MESSAGES = "messages/";
    static final String ENTRY_MESSAGES_INDEX = "messages/index.jsonl";

    static final String CAT_ACCOUNTS = "accounts";
    static final String CAT_RULES = "rules";
    static final String CAT_CONTACTS = "contacts";
    static final String CAT_MESSAGES = "messages";
    /**
     * Attachment payloads, split out of {@link #CAT_MESSAGES} (白い熊, 2026-09-08).
     *
     * <p>Measured on a real backup: 1,088 bodies came to 44,996,387 bytes and 286 attachment
     * payloads to 46,346,285 — attachments were more than half the mail by volume and, being
     * already-compressed formats, rather more than half the archive. Bodies are the searchable
     * text of the mail and are cheap; attachments are the weight. Keeping them as one category
     * meant the only way to leave the weight out was to leave the mail out.
     *
     * <p><b>It rides with {@link #CAT_MESSAGES} and cannot travel alone.</b> An attachment
     * payload is written under the message that owns it and is named by the index entry, so
     * without the index there is nothing to attach it to.
     *
     * <p>The attachment ROWS always travel with the messages, whether or not the payloads do:
     * that is what lets a restore without payloads still show the mail as having attachments
     * and fetch them from the server on demand.
     */
    static final String CAT_ATTACHMENTS = "attachments";
    static final String CAT_ANSWERS = "answers";
    static final String CAT_SEARCHES = "searches";
    static final String CAT_CHANNELS = "channels";
    static final String CAT_SETTINGS = "settings";
    static final String CAT_UI = "ui";

    static final String[] CAT_IDS = {
            CAT_ACCOUNTS, CAT_RULES, CAT_CONTACTS, CAT_MESSAGES, CAT_ATTACHMENTS, CAT_ANSWERS,
            CAT_SEARCHES, CAT_CHANNELS, CAT_SETTINGS, CAT_UI
    };

    /**
     * Whether a category starts ticked, in {@link #CAT_IDS} order — one answer behind three
     * surfaces: the panel's checkboxes, the fourth field of the automation contract's
     * LIST_CATEGORIES reply, and what an export takes when the caller names no items.
     * Off is for what is large and re-obtainable from what the backup already carries.
     */
    static final boolean[] CAT_DEFAULTS = {
            true,   // accounts
            true,   // rules
            true,   // contacts
            true,   // messages: on since 2026-09-08. It was off while it also meant the
                    // attachments, which is what made it heavy; the bodies on their own are
                    // the searchable text of the mail and a backup without them is not a
                    // backup of a mail client
            true,   // attachments: 白い熊's choice. Off is the sensible setting for a small
                    // archive, since a payload re-downloads from the server, but a backup is
                    // for the day the server copy is not there either
            true,   // answers
            true,   // searches
            true,   // channels
            true,   // settings
            true    // ui
    };

    static final int[] CAT_LABELS = {
            R.string.title_ui_eim_cat_accounts,
            R.string.title_ui_eim_cat_rules,
            R.string.title_ui_eim_cat_contacts,
            R.string.title_ui_eim_cat_messages,
            R.string.title_ui_eim_cat_attachments,
            R.string.title_ui_eim_cat_answers,
            R.string.title_ui_eim_cat_searches,
            R.string.title_ui_eim_cat_channels,
            R.string.title_ui_eim_cat_settings,
            R.string.title_ui_eim_cat_ui
    };

    /**
     * Categories the pickers leave unticked: the local mail store can run to gigabytes, so
     * it is opt-in per export rather than part of the usual settings backup.
     */
    static boolean isDefaultOff(String id) {
        for (int i = 0; i < CAT_IDS.length; i++)
            if (CAT_IDS[i].equals(id))
                return !CAT_DEFAULTS[i];
        return false;
    }

    /** Progress units — numbers first, never a percentage (the automation contract). */
    private static final String UNIT_CATEGORY = "区分";
    private static final String UNIT_ACCOUNT = "アカウント";
    private static final String UNIT_MESSAGE = "メール";

    /** Id references are remapped on import, never carried across as raw row ids. */
    private static final Set<String> MESSAGE_SKIP = new HashSet<>(Arrays.asList(
            "id", "account", "folder", "identity", "replying", "forwarding"));
    private static final Set<String> ATTACHMENT_SKIP = new HashSet<>(Arrays.asList(
            "id", "message", "selected"));

    /**
     * Export progress sink. Callers get real counts ({@code current}/{@code total} of
     * {@code unit}) plus the ready-made display line; the automation receiver forwards
     * them as progress broadcasts, the panel could show them too.
     */
    interface Progress {
        void report(long current, long total, String unit, String text);
    }

    /**
     * A backup this app can open, more than once.
     *
     * <p>The import needs two passes — the export JSON, then the mail store, whose index entry
     * has to be read before the payload entries it names — so it cannot be given a plain
     * stream. A {@link Uri} covers what 白い熊 picks by hand; the automation data door is
     * handed a descriptor its caller opened, which may be a pipe and is in any case not
     * seekable twice, so it spools a private copy and opens that.
     */
    interface Source {
        InputStream open() throws IOException;
    }

    /**
     * Thrown out of the export when a cancel arrives. The cancel signal is an
     * {@link AtomicBoolean} the caller flips from another thread and the export tests at
     * entry and file boundaries only — never mid-{@code write()}, and never by interrupting
     * or killing anything. Whoever started the export unwinds, deletes the partial file and
     * reports it: the run ends leaving the backup directory exactly as it found it.
     */
    static class CancelledException extends IOException {
        CancelledException() {
            super("cancelled");
        }
    }

    private static void checkCancelled(@Nullable AtomicBoolean cancel) throws CancelledException {
        if (cancel != null && cancel.get())
            throw new CancelledException();
    }

    static boolean isKnownCat(String id) {
        for (String cat : CAT_IDS)
            if (cat.equals(id))
                return true;
        return false;
    }

    @StringRes
    static int catLabel(String id) {
        for (int i = 0; i < CAT_IDS.length; i++)
            if (CAT_IDS[i].equals(id))
                return CAT_LABELS[i];
        return R.string.title_ui_eim_cat_settings;
    }

    /**
     * The set an export takes when the caller names no items: the categories flagged on in
     * {@link #CAT_DEFAULTS}. The automation contract reads an absent items extra as "your
     * default set", which is what this app recommends, never its whole footprint.
     */
    static List<String> defaultCats() {
        List<String> cats = new ArrayList<>();
        for (int i = 0; i < CAT_IDS.length; i++)
            if (CAT_DEFAULTS[i])
                cats.add(CAT_IDS[i]);
        return cats;
    }

    /**
     * The keys belonging to the "UI customization" category: the fork's custom
     * theme colours and fonts plus the theme selection and fork list toggles.
     * Everything else in the default prefs store is "App settings".
     */
    static boolean isUiKey(String key) {
        if (key == null)
            return false;
        return CustomThemeColors.isCustomColorPref(key) ||
                key.startsWith("custom_font_") ||
                "theme".equals(key) || "beige".equals(key) ||
                "subject_lines_narrow".equals(key) || "sender_italic".equals(key);
    }

    // --- export directory ---

    static SharedPreferences eximPrefs(Context context) {
        return context.getSharedPreferences(EXIM_PREFS, Context.MODE_PRIVATE);
    }

    @Nullable
    static Uri dirUri(Context context) {
        String value = eximPrefs(context).getString(KEY_DIR_URI, null);
        return (value == null ? null : Uri.parse(value));
    }

    /** The configured export directory, or null when unset or no longer accessible. */
    @Nullable
    static DocumentFile exportDir(Context context) {
        Uri uri = dirUri(context);
        if (uri == null)
            return null;
        try {
            DocumentFile dir = DocumentFile.fromTreeUri(context, uri);
            return (dir != null && dir.isDirectory() ? dir : null);
        } catch (Throwable ex) {
            return null;
        }
    }

    static String exportFileName() {
        return EXPORT_PREFIX +
                new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.ROOT).format(new Date()) + ".zip";
    }

    /** True for anything this app wrote as a backup, current convention or legacy. */
    static boolean isExportName(@Nullable String name) {
        if (name == null)
            return false;
        if (!name.endsWith(".zip") && !name.endsWith(".json"))
            return false;
        return name.startsWith(EXPORT_PREFIX) || name.startsWith(LEGACY_EXPORT_PREFIX);
    }

    // --- export ---

    /**
     * A backup is written under {@code <final-name>.part} and renamed only once the archive
     * is closed and complete, so nothing half-written is ever left looking like a backup —
     * 白い熊 keeps every app's backups in one directory sorted by date, where a truncated ZIP
     * would silently become "the latest one". The part file is created as octet-stream so
     * the SAF provider keeps the name given instead of appending another {@code .zip}.
     */
    static final String PART_SUFFIX = ".part";
    private static final String PART_MIME = "application/octet-stream";

    /**
     * Write one backup into a plain directory, atomically: on any failure — a cancel
     * included — the part file goes and the directory is left as it was found.
     */
    static File exportToDir(Context context, List<String> cats, File dir, String name,
                            @Nullable Progress progress, @Nullable AtomicBoolean cancel) throws Throwable {
        if (!dir.exists() && !dir.mkdirs())
            throw new IOException("cannot create " + dir.getAbsolutePath());
        if (!dir.isDirectory())
            throw new IOException("not a directory: " + dir.getAbsolutePath());

        File part = new File(dir, name + PART_SUFFIX);
        File file = new File(dir, name);
        boolean complete = false;
        try {
            try (OutputStream out = new FileOutputStream(part)) {
                export(context, cats, out, progress, cancel);
            }
            if (!part.renameTo(file))
                throw new IOException("cannot rename " + part.getName() + " to " + name);
            complete = true;
            return file;
        } finally {
            if (!complete && part.exists() && !part.delete())
                Log.w("Cannot delete " + part.getAbsolutePath());
        }
    }

    /** The same atomic write through a SAF tree, for when there is no All files access. */
    static DocumentFile exportToDir(Context context, List<String> cats, DocumentFile dir, String name,
                                    @Nullable Progress progress, @Nullable AtomicBoolean cancel) throws Throwable {
        DocumentFile part = dir.createFile(PART_MIME, name + PART_SUFFIX);
        if (part == null)
            throw new IOException("cannot create " + name + PART_SUFFIX + " in the export directory");

        boolean complete = false;
        try {
            try (OutputStream out = context.getContentResolver().openOutputStream(part.getUri())) {
                if (out == null)
                    throw new IOException("cannot open " + name + PART_SUFFIX + " for writing");
                export(context, cats, out, progress, cancel);
            }
            // renameTo repoints the DocumentFile itself, so this is the finished backup
            if (!part.renameTo(name))
                throw new IOException("cannot rename " + name + PART_SUFFIX + " to " + name);
            complete = true;
            return part;
        } finally {
            if (!complete)
                try {
                    part.delete();
                } catch (Throwable ex) {
                    Log.w(ex);
                }
        }
    }

    /**
     * Write ONE backup ZIP of the selected categories to {@code out}. The caller owns
     * the stream and closes it; this only finishes the ZIP central directory. Prefer
     * {@link #exportToDir(Context, List, File, String, Progress, AtomicBoolean)} — a caller
     * writing the stream itself owns deleting what it wrote when this throws.
     */
    static void export(Context context, List<String> cats, OutputStream out,
                       @Nullable Progress progress, @Nullable AtomicBoolean cancel) throws Throwable {
        int[] done = {0};
        JSONObject jexport = buildExport(context, cats, progress, done, cancel);

        JSONObject jmanifest = new JSONObject();
        jmanifest.put("format", FORMAT);
        jmanifest.put("version", VERSION);
        jmanifest.put("app", BuildConfig.APPLICATION_ID);
        jmanifest.put("appVersion", BuildConfig.VERSION_NAME);
        jmanifest.put("createdTs", new Date().getTime());
        JSONArray jcats = new JSONArray();
        for (String cat : CAT_IDS)
            if (cats.contains(cat))
                jcats.put(cat);
        jmanifest.put("categories", jcats);

        ZipOutputStream zos = new ZipOutputStream(out);

        zos.putNextEntry(new ZipEntry(ENTRY_MANIFEST));
        zos.write(jmanifest.toString(2).getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();

        zos.putNextEntry(new ZipEntry(ENTRY_EXPORT));
        zos.write(jexport.toString(2).getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();

        if (cats.contains(CAT_MESSAGES))
            exportMessages(context, zos, cats, progress, done, cancel);
        else
            // Attachments cannot travel alone - there would be no index to attach them to.
            // Stepped anyway so the category counter still reaches its own total.
            step(context, progress, cats, done, CAT_ATTACHMENTS);

        // Last boundary: a cancel arriving here still leaves no finished archive behind
        checkCancelled(cancel);

        zos.finish();
        zos.flush();
    }

    /** Report one finished category, when it was selected at all. */
    private static void step(Context context, @Nullable Progress progress,
                             List<String> cats, int[] done, String cat) {
        if (progress == null || !cats.contains(cat))
            return;
        done[0]++;
        progress.report(done[0], cats.size(), UNIT_CATEGORY,
                UNIT_CATEGORY + " " + done[0] + "/" + cats.size() + " — " +
                        context.getString(catLabel(cat)));
    }

    /**
     * Build the export JSON in the stock FragmentOptionsBackup format (so exports
     * stay importable by upstream builds and vice versa), gated per category.
     * Unselected sections are emitted as empty arrays for stock-import safety.
     * Fork extension: "ui_fonts" carries the custom font binaries base64-encoded.
     */
    static JSONObject buildExport(Context context, List<String> cats,
                                  @Nullable Progress progress, int[] done,
                                  @Nullable AtomicBoolean cancel) throws Throwable {
        checkCancelled(cancel);

        boolean catAccounts = cats.contains(CAT_ACCOUNTS);
        boolean catRules = cats.contains(CAT_RULES);
        boolean catContacts = cats.contains(CAT_CONTACTS);
        boolean catAnswers = cats.contains(CAT_ANSWERS);
        boolean catSearches = cats.contains(CAT_SEARCHES);
        boolean catChannels = cats.contains(CAT_CHANNELS);
        boolean catSettings = cats.contains(CAT_SETTINGS);
        boolean catUi = cats.contains(CAT_UI);

        DB db = DB.getInstance(context);
        NotificationManager nm = Helper.getSystemService(context, NotificationManager.class);

        // Accounts
        JSONArray jaccounts = new JSONArray();
        if (catAccounts) {
            List<EntityAccount> accounts = db.account().getAccounts();
            int total = (accounts == null ? 0 : accounts.size());
            int index = 0;
            for (EntityAccount account : accounts) {
                checkCancelled(cancel);
                index++;
                if (progress != null)
                    progress.report(index, total, UNIT_ACCOUNT,
                            UNIT_ACCOUNT + " " + index + "/" + total + " — " + account.name);

                JSONObject jaccount = account.toJSON();

                if (catChannels && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    if (account.notify) {
                        NotificationChannel channel = nm.getNotificationChannel(
                                EntityAccount.getNotificationChannelId(account.id));
                        if (channel != null && channel.getImportance() != NotificationManager.IMPORTANCE_NONE)
                            jaccount.put("channel", NotificationHelper.channelToJSON(channel));
                    }
                }

                JSONArray jidentities = new JSONArray();
                for (EntityIdentity identity : db.identity().getIdentities(account.id))
                    jidentities.put(identity.toJSON());
                jaccount.put("identities", jidentities);

                JSONArray jfolders = new JSONArray();
                for (EntityFolder folder : db.folder().getFolders(account.id, false, true)) {
                    checkCancelled(cancel);
                    JSONObject jfolder = folder.toJSON();

                    if (catChannels && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        NotificationChannel channel = nm.getNotificationChannel(
                                EntityFolder.getNotificationChannelId(folder.id));
                        if (channel != null && channel.getImportance() != NotificationManager.IMPORTANCE_NONE)
                            jfolder.put("channel", NotificationHelper.channelToJSON(channel));
                    }

                    JSONArray jrules = new JSONArray();
                    if (catRules)
                        for (EntityRule rule : db.rule().getRules(folder.id)) {
                            try {
                                JSONObject jaction = new JSONObject(rule.action);
                                int type = jaction.getInt("type");
                                switch (type) {
                                    case EntityRule.TYPE_MOVE:
                                    case EntityRule.TYPE_COPY:
                                        long target = jaction.optLong("target", -1L);
                                        EntityFolder f = db.folder().getFolder(target);
                                        EntityAccount a = (f == null ? null : db.account().getAccount(f.account));
                                        if (a != null)
                                            jaction.put("targetAccountUuid", a.uuid);
                                        if (f != null)
                                            jaction.put("targetFolderName", f.name);
                                        break;
                                    case EntityRule.TYPE_ANSWER:
                                        long identity = jaction.optLong("identity", -1L);
                                        long answer = jaction.optLong("answer", -1L);
                                        EntityIdentity i = db.identity().getIdentity(identity);
                                        EntityAnswer t = db.answer().getAnswer(answer);
                                        if (i != null)
                                            jaction.put("identityUuid", i.uuid);
                                        if (t != null)
                                            jaction.put("answerUuid", t.uuid);
                                        break;
                                }
                                rule.action = jaction.toString();
                            } catch (Throwable ex) {
                                Log.e(ex);
                            }
                            jrules.put(rule.toJSON());
                        }
                    jfolder.put("rules", jrules);

                    jfolders.put(jfolder);
                }
                jaccount.put("folders", jfolders);

                JSONArray jcontacts = new JSONArray();
                if (catContacts)
                    for (EntityContact contact : db.contact().getContacts(account.id))
                        jcontacts.put(contact.toJSON());
                jaccount.put("contacts", jcontacts);

                jaccounts.put(jaccount);
            }
        }
        step(context, progress, cats, done, CAT_ACCOUNTS);
        step(context, progress, cats, done, CAT_RULES);
        step(context, progress, cats, done, CAT_CONTACTS);

        // Answers
        JSONArray janswers = new JSONArray();
        if (catAnswers)
            for (EntityAnswer answer : db.answer().getAnswers(true))
                janswers.put(answer.toJSON());
        step(context, progress, cats, done, CAT_ANSWERS);

        // Searches
        JSONArray jsearches = new JSONArray();
        if (catSearches)
            for (EntitySearch search : db.search().getSearches()) {
                if (Objects.equals(search.name, context.getString(R.string.title_search_with_flagged)))
                    continue;
                if (Objects.equals(search.name, context.getString(R.string.title_search_with_unseen)))
                    continue;
                jsearches.put(search.toJSON());
            }
        step(context, progress, cats, done, CAT_SEARCHES);

        // Certificates (part of App settings, like the stock import)
        JSONArray jcertificates = new JSONArray();
        if (catSettings)
            for (EntityCertificate certificate : db.certificate().getCertificates())
                jcertificates.put(certificate.toJSON());

        // Settings, split into App settings and UI customization by key
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        JSONArray jsettings = new JSONArray();
        Map<String, ?> all = prefs.getAll();
        for (String key : all.keySet()) {
            boolean ui = isUiKey(key);
            if (ui ? !catUi : !catSettings)
                continue;
            JSONObject jsetting = new JSONObject();
            Object value = all.get(key);
            jsetting.put("key", key);
            jsetting.put("value", value);
            if (value instanceof Boolean)
                jsetting.put("type", "bool");
            else if (value instanceof Integer)
                jsetting.put("type", "int");
            else if (value instanceof Long)
                jsetting.put("type", "long");
            else if (value instanceof Float)
                jsetting.put("type", "float");
            else if (value instanceof String)
                jsetting.put("type", "string");
            else if (value != null) {
                Log.w("Unknown type=" + value.getClass().getName());
                jsetting.put("type", value.getClass().getName());
            }
            jsettings.put(jsetting);
        }

        if (catSettings) {
            JSONObject jsearch = new JSONObject();
            jsearch.put("key", "external_search");
            jsearch.put("value", Helper.isComponentEnabled(context, ActivitySearch.class));
            jsearch.put("type", "bool");
            jsettings.put(jsearch);
        }

        JSONObject jexport = new JSONObject();
        jexport.put("accounts", jaccounts);
        jexport.put("answers", janswers);
        jexport.put("searches", jsearches);
        jexport.put("certificates", jcertificates);
        jexport.put("settings", jsettings);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            JSONArray jchannels = new JSONArray();
            if (catChannels)
                for (NotificationChannel channel : nm.getNotificationChannels()) {
                    String id = channel.getId();
                    if (id.startsWith("notification.") && id.contains("@") &&
                            channel.getImportance() != NotificationManager.IMPORTANCE_NONE)
                        jchannels.put(NotificationHelper.channelToJSON(channel));
                }
            jexport.put("channels", jchannels);
        }
        step(context, progress, cats, done, CAT_CHANNELS);
        step(context, progress, cats, done, CAT_SETTINGS);

        // Fork extension: font binaries, so UI imports do not leave dangling paths
        JSONObject jfonts = new JSONObject();
        if (catUi)
            for (CustomFont.Entry entry : CustomFont.ENTRIES) {
                File file = null;
                String path = prefs.getString(CustomFont.prefPath(entry.role), null);
                if (path != null) {
                    File f = new File(path);
                    if (f.exists())
                        file = f;
                }
                if (file == null) {
                    File f = CustomFont.storedFile(context, entry.role);
                    if (f.exists())
                        file = f;
                }
                if (file == null)
                    continue;
                byte[] bytes = new byte[(int) file.length()];
                try (FileInputStream in = new FileInputStream(file)) {
                    int off = 0;
                    while (off < bytes.length) {
                        int n = in.read(bytes, off, bytes.length - off);
                        if (n < 0)
                            break;
                        off += n;
                    }
                }
                jfonts.put(entry.role, Base64.encodeToString(bytes, Base64.NO_WRAP));
            }
        jexport.put("ui_fonts", jfonts);
        step(context, progress, cats, done, CAT_UI);

        return jexport;
    }

    // --- export: the local mail store ---

    /** Where a message lives, in terms that survive a restore onto fresh row ids. */
    private static class MessageRef {
        final long id;
        final String account;
        final String folder;

        MessageRef(long id, String account, String folder) {
            this.id = id;
            this.account = account;
            this.folder = folder;
        }
    }

    /**
     * Stream the locally stored mail into the same ZIP: the JSON-Lines index first, then
     * each message's body, raw MIME and attachment payloads. Two passes over the id list
     * rather than one, because a ZIP can only hold one entry open at a time and the index
     * has to precede the payloads it names.
     */
    private static void exportMessages(Context context, ZipOutputStream zos, List<String> cats,
                                       @Nullable Progress progress, int[] done,
                                       @Nullable AtomicBoolean cancel) throws Throwable {
        DB db = DB.getInstance(context);
        final boolean withAttachments = cats.contains(CAT_ATTACHMENTS);

        List<MessageRef> refs = new ArrayList<>();
        for (EntityAccount account : db.account().getAccounts())
            for (EntityFolder folder : db.folder().getFolders(account.id, false, true))
                for (Long id : db.message().getMessageByFolder(folder.id))
                    if (id != null)
                        refs.add(new MessageRef(id, account.uuid, folder.name));

        int total = refs.size();

        zos.putNextEntry(new ZipEntry(ENTRY_MESSAGES_INDEX));
        for (int i = 0; i < total; i++) {
            checkCancelled(cancel);
            MessageRef ref = refs.get(i);
            EntityMessage message = db.message().getMessage(ref.id);
            if (message == null)
                continue;

            JSONObject jrecord = new JSONObject();
            jrecord.put("index", i);
            jrecord.put("account", ref.account);
            jrecord.put("folder", ref.folder);
            jrecord.put("message", entityToJSON(message, MESSAGE_SKIP));

            JSONArray jattachments = new JSONArray();
            List<EntityAttachment> attachments = db.attachment().getAttachments(message.id);
            for (int a = 0; a < attachments.size(); a++) {
                JSONObject jattachment = entityToJSON(attachments.get(a), ATTACHMENT_SKIP);
                jattachment.put("file", "att-" + a);
                jattachments.put(jattachment);
            }
            jrecord.put("attachments", jattachments);

            zos.write(jrecord.toString().getBytes(StandardCharsets.UTF_8));
            zos.write('\n');

            if (progress != null)
                progress.report(i + 1, total, UNIT_MESSAGE,
                        UNIT_MESSAGE + " 目録 " + (i + 1) + "/" + total);
        }
        zos.closeEntry();

        for (int i = 0; i < total; i++) {
            checkCancelled(cancel);
            MessageRef ref = refs.get(i);
            EntityMessage message = db.message().getMessage(ref.id);
            if (message == null)
                continue;

            String base = ENTRY_MESSAGES + i + "/";

            File body = message.getFile(context);
            if (body.exists())
                copyEntry(zos, base + "body.html", body, cancel);

            File raw = message.getRawFile(context);
            if (raw.exists())
                copyEntry(zos, base + "raw.eml", raw, cancel);

            // The rows went out with the index unconditionally; only the payloads are optional.
            // A message therefore still knows it has attachments in a bodies-only backup, and
            // the import leaves them marked not-downloaded so they come from the server.
            if (withAttachments) {
                List<EntityAttachment> attachments = db.attachment().getAttachments(message.id);
                for (int a = 0; a < attachments.size(); a++) {
                    File file = attachments.get(a).getFile(context);
                    if (file.exists())
                        copyEntry(zos, base + "att-" + a, file, cancel);
                }
            }

            if (progress != null)
                progress.report(i + 1, total, UNIT_MESSAGE,
                        UNIT_MESSAGE + " " + (i + 1) + "/" + total);
        }

        step(context, progress, cats, done, CAT_MESSAGES);
        step(context, progress, cats, done, CAT_ATTACHMENTS);
    }

    private static void copyEntry(ZipOutputStream zos, String name, File file,
                                  @Nullable AtomicBoolean cancel) throws IOException {
        zos.putNextEntry(new ZipEntry(name));
        try (FileInputStream in = new FileInputStream(file)) {
            copy(in, zos, cancel);
        }
        zos.closeEntry();
    }

    private static void copy(InputStream in, OutputStream out) throws IOException {
        copy(in, out, null);
    }

    /** Block boundaries are cancel boundaries too: one attachment can be very large. */
    private static void copy(InputStream in, OutputStream out,
                             @Nullable AtomicBoolean cancel) throws IOException {
        byte[] buffer = new byte[8192];
        int n;
        // -1, not > 0: a read of 0 bytes is allowed and does not mean the entry is finished,
        // and taking it for the end would write a truncated attachment out under its full name.
        while ((n = in.read(buffer)) != -1) {
            checkCancelled(cancel);
            out.write(buffer, 0, n);
        }
    }

    /**
     * Serialize a Room entity by reflection rather than by a hand-written field list:
     * EntityMessage alone has over a hundred columns, and upstream adds to it. Null fields
     * are omitted; the two array types go through the same converters Room itself uses.
     * Safe under R8 because proguard-rules.pro keeps every eu.faircode.email member.
     */
    private static JSONObject entityToJSON(Object entity, Set<String> skip) throws Throwable {
        JSONObject jentity = new JSONObject();
        for (Field field : entity.getClass().getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic())
                continue;
            String name = field.getName();
            if (skip.contains(name))
                continue;
            field.setAccessible(true);
            Object value = field.get(entity);
            if (value == null)
                continue;
            Class<?> type = field.getType();
            if (Address[].class.equals(type))
                jentity.put(name, DB.Converters.encodeAddresses((Address[]) value));
            else if (String[].class.equals(type))
                jentity.put(name, DB.Converters.fromStringArray((String[]) value));
            else if (value instanceof Boolean || value instanceof Integer ||
                    value instanceof Long || value instanceof String)
                jentity.put(name, value);
        }
        return jentity;
    }

    private static void jsonToEntity(JSONObject jentity, Object entity) throws Throwable {
        for (Field field : entity.getClass().getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic())
                continue;
            String name = field.getName();
            if (!jentity.has(name) || jentity.isNull(name))
                continue;
            field.setAccessible(true);
            Class<?> type = field.getType();
            try {
                if (Address[].class.equals(type))
                    field.set(entity, DB.Converters.decodeAddresses(jentity.getString(name)));
                else if (String[].class.equals(type))
                    field.set(entity, DB.Converters.toStringArray(jentity.getString(name)));
                else if (Boolean.class.equals(type) || boolean.class.equals(type))
                    field.set(entity, jentity.getBoolean(name));
                else if (Integer.class.equals(type) || int.class.equals(type))
                    field.set(entity, jentity.getInt(name));
                else if (Long.class.equals(type) || long.class.equals(type))
                    field.set(entity, jentity.getLong(name));
                else if (String.class.equals(type))
                    field.set(entity, jentity.getString(name));
            } catch (Throwable ex) {
                // An upstream column that changed type is not worth losing the message over
                Log.w(ex);
            }
        }
    }

    // --- import ---

    /** The panel's route: whatever the content resolver can open, opened afresh each pass. */
    static Source sourceOf(Context context, Uri uri) {
        return new Source() {
            @Override
            public InputStream open() throws IOException {
                InputStream is = context.getContentResolver().openInputStream(uri);
                if (is == null)
                    throw new FileNotFoundException(uri.toString());
                return is;
            }
        };
    }

    /** A file this app wrote itself, for a descriptor that could not be read twice. */
    static Source sourceOf(File file) {
        return new Source() {
            @Override
            public InputStream open() throws IOException {
                return new FileInputStream(file);
            }
        };
    }

    /**
     * The categories a backup actually carries, read from its {@link #ENTRY_MANIFEST}.
     *
     * <p>Restoring what is present rather than what was asked for is what keeps an import from
     * reporting success over nothing. A backup with no manifest — an older bare fork export or
     * a stock upstream one — carries no such list, so every category is offered and the import
     * skips the ones the JSON does not have.
     */
    static List<String> categoriesIn(Source source) {
        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(source.open()))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory() || !ENTRY_MANIFEST.equals(entry.getName()))
                    continue;
                JSONObject jmanifest = new JSONObject(readAll(zis));
                JSONArray jcats = jmanifest.optJSONArray("categories");
                if (jcats == null)
                    break;
                List<String> cats = new ArrayList<>();
                for (int i = 0; i < jcats.length(); i++) {
                    String cat = jcats.optString(i, null);
                    if (isKnownCat(cat) && !cats.contains(cat))
                        cats.add(cat);
                }
                return cats;
            }
        } catch (Throwable ex) {
            Log.w(ex);
        }
        return Arrays.asList(CAT_IDS);
    }

    /**
     * Read the export JSON out of a backup: the {@link #ENTRY_EXPORT} entry of a
     * family-convention ZIP, or the whole file when it is a bare JSON export (an
     * older fork backup or a stock unencrypted upstream one).
     */
    private static String readExportJson(Context context, Source source) throws Throwable {
        try (BufferedInputStream bis = new BufferedInputStream(source.open())) {
            bis.mark(4);
            byte[] magic = new byte[4];
            int read = 0;
            while (read < magic.length) {
                int n = bis.read(magic, read, magic.length - read);
                if (n < 0)
                    break;
                read += n;
            }
            bis.reset();

            boolean zip = (read == 4 &&
                    magic[0] == 'P' && magic[1] == 'K' && magic[2] == 3 && magic[3] == 4);
            if (!zip)
                return readAll(bis);

            ZipInputStream zis = new ZipInputStream(bis);
            String fallback = null;
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                if (name == null || entry.isDirectory() || ENTRY_MANIFEST.equals(name))
                    continue;
                if (ENTRY_EXPORT.equals(name))
                    return readAll(zis);
                if (fallback == null && name.endsWith(".json"))
                    fallback = readAll(zis);
            }
            if (fallback != null)
                return fallback;
            throw new IllegalArgumentException(context.getString(R.string.title_ui_eim_invalid));
        }
    }

    private static String readAll(InputStream is) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int n;
        while ((n = is.read(buffer)) != -1)
            bos.write(buffer, 0, n);
        return new String(bos.toByteArray(), StandardCharsets.UTF_8);
    }

    /**
     * Import the selected categories from a backup ZIP (or a bare stock-format JSON).
     * Adapted from FragmentOptionsBackup.handleImport; returns a per-category
     * "Label: count" summary for the result dialog. Merges — existing accounts
     * (matched by UUID) are kept, preferences outside the selected categories
     * are untouched.
     */
    static String performImport(Context context, List<String> cats, Uri uri) throws Throwable {
        // The Uri route is the one 白い熊 drives by hand from the panel, and the only one that
        // can be handed a file it may not read - so the friendly check belongs here rather
        // than in the body, which also serves descriptors this app opened itself.
        NoStreamException.check(uri, context);
        return performImport(context, cats, sourceOf(context, uri), uri.toString(), null);
    }

    /**
     * The same import over anything that can be opened twice — the second pass is what
     * restores the mail store, whose index has to be read before its payload entries. The
     * automation data door supplies a spooled copy of the descriptor its caller opened, which
     * is why this cannot simply take a {@link Uri}.
     *
     * <p><b>Reported exactly as the export reports</b>: one step per finished category, with
     * the per-account and per-message counters inside the two categories long enough to need
     * them. Without this a restore is silent for its whole length — the automation door has
     * nothing left to broadcast but the byte count of a spool that finished minutes ago, and a
     * repeated number cannot be told apart from a dead app. The categories inside the
     * transaction go past quickly; the mail store is where the time actually goes, so that is
     * where the counter has to keep moving.
     *
     * @param label    what the log should call the source; a Uri, or a descriptor's job.
     * @param progress where to report, or null for the panel's own import, which shows a
     *                 spinner and a summary dialog rather than a running count.
     */
    static String performImport(Context context, List<String> cats, Source source, String label,
                                @Nullable Progress progress) throws Throwable {
        boolean catAccounts = cats.contains(CAT_ACCOUNTS);
        boolean catRules = cats.contains(CAT_RULES);
        boolean catContacts = cats.contains(CAT_CONTACTS);
        boolean catMessages = cats.contains(CAT_MESSAGES);
        boolean catAnswers = cats.contains(CAT_ANSWERS);
        boolean catSearches = cats.contains(CAT_SEARCHES);
        boolean catChannels = cats.contains(CAT_CHANNELS);
        boolean catSettings = cats.contains(CAT_SETTINGS);
        boolean catUi = cats.contains(CAT_UI);

        EntityLog.log(context, "UI import " + label + " cats=" + TextUtils.join(",", cats));

        // Said BEFORE the work rather than after it: pulling the export JSON out of the ZIP and
        // parsing it runs to seconds on a large backup, and until this line the caller is still
        // looking at the byte count of a transfer that is already over. 0/N is true, and naming
        // the phase is what tells a slow restore from a stalled one.
        int[] done = {0};
        if (progress != null)
            progress.report(0, cats.size(), UNIT_CATEGORY,
                    UNIT_CATEGORY + " 0/" + cats.size() + " — 書庫を読み込み");

        String json = readExportJson(context, source).trim();
        if (!json.startsWith("{") || !json.endsWith("}"))
            throw new IllegalArgumentException(context.getString(R.string.title_ui_eim_invalid));

        JSONObject jimport = new JSONObject(json);

        ServiceSynchronize.stop(context);
        ServiceSend.stop(context);

        int nAccounts = 0, nRules = 0, nContacts = 0, nAnswers = 0,
                nSearches = 0, nChannels = 0, nSettings = 0, nUi = 0;

        DB db = DB.getInstance(context);
        NotificationManager nm = Helper.getSystemService(context, NotificationManager.class);
        try {
            db.beginTransaction();

            Map<Long, Long> xAnswer = new HashMap<>();
            Map<Long, Long> xIdentity = new HashMap<>();
            Map<Long, Long> xFolder = new HashMap<>();
            List<EntityRule> rules = new ArrayList<>();

            if (catAnswers && jimport.has("answers")) {
                JSONArray janswers = jimport.getJSONArray("answers");
                for (int a = 0; a < janswers.length(); a++) {
                    JSONObject janswer = (JSONObject) janswers.get(a);
                    EntityAnswer answer = EntityAnswer.fromJSON(janswer);
                    long id = answer.id;
                    answer.id = null;

                    EntityAnswer existing = db.answer().getAnswerByUUID(answer.uuid);
                    if (existing != null)
                        db.answer().deleteAnswer(existing.id);

                    answer.id = db.answer().insertAnswer(answer);
                    xAnswer.put(id, answer.id);
                    nAnswers++;
                }
            }
            step(context, progress, cats, done, CAT_ANSWERS);

            if (catSearches && jimport.has("searches")) {
                JSONArray jsearches = jimport.getJSONArray("searches");
                for (int s = 0; s < jsearches.length(); s++) {
                    JSONObject jsearch = (JSONObject) jsearches.get(s);
                    EntitySearch search = EntitySearch.fromJSON(jsearch);

                    boolean found = false;
                    for (EntitySearch other : db.search().getSearches())
                        if (other.equals(search)) {
                            found = true;
                            break;
                        }

                    if (!found) {
                        search.id = null;
                        db.search().insertSearch(search);
                        nSearches++;
                    }
                }
            }
            step(context, progress, cats, done, CAT_SEARCHES);

            if (catAccounts && jimport.has("accounts")) {
                JSONArray jaccounts = jimport.getJSONArray("accounts");
                for (int a = 0; a < jaccounts.length(); a++) {
                    JSONObject jaccount = (JSONObject) jaccounts.get(a);
                    EntityAccount account = EntityAccount.fromJSON(jaccount);

                    // Named before it is known whether this one will be kept: what the caller
                    // wants to see is which account is being worked on, and an account already
                    // present is skipped too fast to be worth a line of its own.
                    if (progress != null)
                        progress.report(a + 1, jaccounts.length(), UNIT_ACCOUNT,
                                UNIT_ACCOUNT + " " + (a + 1) + "/" + jaccounts.length() +
                                        " — " + account.name);

                    EntityAccount existing = db.account().getAccountByUUID(account.uuid);
                    if (existing != null) {
                        EntityLog.log(context, "UI import: existing account=" + account.name);
                        continue;
                    }

                    if (account.auth_type == AUTH_TYPE_GMAIL &&
                            GmailState.getAccount(context, account.user) == null) {
                        EntityLog.log(context, "UI import: Gmail wizard needed account=" + account.name);
                        account.synchronize = false;
                    }

                    Long aid = account.id;
                    account.id = null;

                    if (jaccounts.length() == 1)
                        account.primary = true;

                    EntityAccount primary = db.account().getPrimaryAccount();
                    if (primary != null)
                        account.primary = false;

                    // Forward referenced
                    Long swipe_left = account.swipe_left;
                    Long swipe_right = account.swipe_right;
                    Long move_to = account.move_to;
                    if (account.swipe_left != null && account.swipe_left > 0)
                        account.swipe_left = null;
                    if (account.swipe_right != null && account.swipe_right > 0)
                        account.swipe_right = null;
                    account.move_to = null;

                    account.created = new Date().getTime();
                    account.id = db.account().insertAccount(account);
                    nAccounts++;
                    EntityLog.log(context, "UI import: account=" + account.name +
                            " id=" + account.id + " (" + aid + ")");

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        account.deleteNotificationChannel(context);

                        if (account.notify)
                            if (jaccount.has("channel"))
                                try {
                                    NotificationChannelGroup group = new NotificationChannelGroup("group." + account.id, account.name);
                                    nm.createNotificationChannelGroup(group);

                                    JSONObject jchannel = (JSONObject) jaccount.get("channel");
                                    jchannel.put("id", EntityAccount.getNotificationChannelId(account.id));
                                    jchannel.put("group", group.getId());
                                    nm.createNotificationChannel(NotificationHelper.channelFromJSON(context, jchannel));
                                } catch (Throwable ex) {
                                    Log.e(ex);
                                    account.createNotificationChannel(context);
                                }
                            else
                                account.createNotificationChannel(context);
                    }

                    JSONArray jidentities = (JSONArray) jaccount.get("identities");
                    for (int i = 0; i < jidentities.length(); i++) {
                        JSONObject jidentity = (JSONObject) jidentities.get(i);
                        EntityIdentity identity = EntityIdentity.fromJSON(jidentity);

                        long id = identity.id;
                        identity.id = null;

                        identity.account = account.id;
                        identity.id = db.identity().insertIdentity(identity);
                        xIdentity.put(id, identity.id);
                    }

                    JSONArray jfolders = (JSONArray) jaccount.get("folders");
                    for (int f = 0; f < jfolders.length(); f++) {
                        JSONObject jfolder = (JSONObject) jfolders.get(f);
                        EntityFolder folder = EntityFolder.fromJSON(jfolder);
                        long id = folder.id;
                        folder.id = null;

                        folder.account = account.id;
                        folder.id = db.folder().insertFolder(folder);
                        xFolder.put(id, folder.id);

                        if (Objects.equals(swipe_left, id))
                            account.swipe_left = folder.id;
                        if (Objects.equals(swipe_right, id))
                            account.swipe_right = folder.id;
                        if (Objects.equals(move_to, id))
                            account.move_to = folder.id;

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            String channelId = EntityFolder.getNotificationChannelId(folder.id);
                            nm.deleteNotificationChannel(channelId);

                            if (jfolder.has("channel"))
                                try {
                                    NotificationChannelGroup group = new NotificationChannelGroup("group." + account.id, account.name);
                                    nm.createNotificationChannelGroup(group);

                                    JSONObject jchannel = (JSONObject) jfolder.get("channel");
                                    jchannel.put("id", channelId);
                                    jchannel.put("group", group.getId());
                                    nm.createNotificationChannel(NotificationHelper.channelFromJSON(context, jchannel));
                                } catch (Throwable ex) {
                                    Log.e(ex);
                                }
                        }

                        if (jfolder.has("rules")) {
                            JSONArray jrules = jfolder.getJSONArray("rules");
                            for (int r = 0; r < jrules.length(); r++) {
                                JSONObject jrule = (JSONObject) jrules.get(r);
                                EntityRule rule = EntityRule.fromJSON(jrule);
                                rule.folder = folder.id;
                                rules.add(rule);
                            }
                        }
                    }

                    if (catContacts && jaccount.has("contacts")) {
                        JSONArray jcontacts = jaccount.getJSONArray("contacts");
                        for (int c = 0; c < jcontacts.length(); c++) {
                            JSONObject jcontact = (JSONObject) jcontacts.get(c);
                            EntityContact contact = EntityContact.fromJSON(jcontact);
                            contact.account = account.id;
                            contact.identity = xIdentity.get(contact.identity);
                            if (db.contact().getContact(contact.account, contact.type, contact.email) == null) {
                                contact.id = db.contact().insertContact(contact);
                                nContacts++;
                            }
                        }
                    }

                    // Update swipe left/right
                    db.account().updateAccount(account);
                }

                if (catRules) {
                    for (EntityRule rule : rules) {
                        try {
                            JSONObject jaction = new JSONObject(rule.action);

                            int type = jaction.getInt("type");
                            switch (type) {
                                case EntityRule.TYPE_MOVE:
                                case EntityRule.TYPE_COPY:
                                    String targetAccountUuid = jaction.optString("targetAccountUuid");
                                    String targetFolderName = jaction.optString("targetFolderName");
                                    if (!TextUtils.isEmpty(targetAccountUuid) && !TextUtils.isEmpty(targetFolderName)) {
                                        EntityAccount a = db.account().getAccountByUUID(targetAccountUuid);
                                        if (a != null) {
                                            EntityFolder f = db.folder().getFolderByName(a.id, targetFolderName);
                                            if (f != null) {
                                                jaction.put("target", f.id);
                                                break;
                                            }
                                        }
                                    }

                                    // Legacy
                                    long target = jaction.getLong("target");
                                    Long tid = xFolder.get(target);
                                    if (tid != null)
                                        jaction.put("target", tid);
                                    break;
                                case EntityRule.TYPE_ANSWER:
                                    String identityUuid = jaction.optString("identityUuid");
                                    String answerUuid = jaction.optString("answerUuid");
                                    if (!TextUtils.isEmpty(identityUuid) && !TextUtils.isEmpty(answerUuid)) {
                                        EntityIdentity i = db.identity().getIdentityByUUID(identityUuid);
                                        EntityAnswer a = db.answer().getAnswerByUUID(answerUuid);
                                        if (i != null && a != null) {
                                            jaction.put("identity", i.id);
                                            jaction.put("answer", a.id);
                                            break;
                                        }
                                    }

                                    // Legacy
                                    long identity = jaction.getLong("identity");
                                    long answer = jaction.getLong("answer");
                                    Long iid = xIdentity.get(identity);
                                    Long aid2 = xAnswer.get(answer);
                                    jaction.put("identity", iid);
                                    jaction.put("answer", aid2);
                                    break;
                            }

                            rule.action = jaction.toString();
                        } catch (Throwable ex) {
                            Log.e(ex);
                        }

                        db.rule().insertRule(rule);
                        nRules++;
                    }
                }
            }
            step(context, progress, cats, done, CAT_ACCOUNTS);
            step(context, progress, cats, done, CAT_CONTACTS);
            step(context, progress, cats, done, CAT_RULES);

            if ((catSettings || catUi)) {
                // Certificates ride with App settings, like the stock import
                if (catSettings && jimport.has("certificates")) {
                    JSONArray jcertificates = jimport.getJSONArray("certificates");
                    for (int c = 0; c < jcertificates.length(); c++) {
                        JSONObject jcertificate = (JSONObject) jcertificates.get(c);
                        EntityCertificate certificate = EntityCertificate.fromJSON(jcertificate);
                        EntityCertificate record = db.certificate().getCertificate(certificate.fingerprint, certificate.email);
                        if (record == null)
                            db.certificate().insertCertificate(certificate);
                    }
                }

                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                SharedPreferences.Editor editor = prefs.edit();

                // Clear pass — only keys belonging to a selected category
                for (String[] options : FragmentOptionsBackup.RESET_ALL_OPTIONS)
                    for (String key : options) {
                        boolean ui = isUiKey(key);
                        if (ui ? !catUi : !catSettings)
                            continue;
                        if (FragmentOptionsBackup.skipOption(key) ||
                                FragmentOptionsBackup.RESTART_OPTIONS.contains(key))
                            continue;
                        editor.remove(key);
                    }

                if (jimport.has("settings")) {
                    JSONArray jsettings = jimport.getJSONArray("settings");
                    for (int s = 0; s < jsettings.length(); s++) {
                        JSONObject jsetting = (JSONObject) jsettings.get(s);
                        String key = jsetting.getString("key");

                        boolean ui = isUiKey(key);
                        if (ui ? !catUi : !catSettings)
                            continue;

                        // Prevent restart
                        if (FragmentOptionsBackup.RESTART_OPTIONS.contains(key))
                            continue;

                        if (FragmentOptionsBackup.skipOption(key))
                            continue;

                        Object value = jsetting.get("value");
                        String type = jsetting.optString("type");
                        switch (type) {
                            case "bool":
                                editor.putBoolean(key, (Boolean) value);
                                break;
                            case "int":
                                editor.putInt(key, (Integer) value);
                                break;
                            case "long":
                                if (value instanceof Integer)
                                    editor.putLong(key, Long.valueOf((Integer) value));
                                else
                                    editor.putLong(key, (Long) value);
                                break;
                            case "float":
                                if (value instanceof Double)
                                    editor.putFloat(key, ((Double) value).floatValue());
                                else if (value instanceof Integer)
                                    editor.putFloat(key, ((Integer) value).floatValue());
                                else
                                    editor.putFloat(key, (Float) value);
                                break;
                            case "string":
                                editor.putString(key, (String) value);
                                break;
                            default:
                                Log.w("Inferring type of value=" + value);
                                if (value instanceof Boolean)
                                    editor.putBoolean(key, (Boolean) value);
                                else if (value instanceof Integer) {
                                    Integer i = (Integer) value;
                                    if (key.endsWith(".account"))
                                        editor.putLong(key, Long.valueOf(i));
                                    else
                                        editor.putInt(key, i);
                                } else if (value instanceof Long)
                                    editor.putLong(key, (Long) value);
                                else if (value instanceof Double)
                                    editor.putFloat(key, ((Double) value).floatValue());
                                else if (value instanceof Float)
                                    editor.putFloat(key, (Float) value);
                                else if (value instanceof String)
                                    editor.putString(key, (String) value);
                                else
                                    Log.e("Unknown settings type key=" + key);
                        }

                        if (ui)
                            nUi++;
                        else
                            nSettings++;
                    }
                }

                // Fork extension: restore the font binaries and point the path
                // prefs at the local files (imported paths belong to the source
                // device and may not exist here)
                if (catUi && jimport.has("ui_fonts")) {
                    JSONObject jfonts = jimport.getJSONObject("ui_fonts");
                    Iterator<String> roles = jfonts.keys();
                    while (roles.hasNext()) {
                        String role = roles.next();
                        boolean known = false;
                        for (CustomFont.Entry entry : CustomFont.ENTRIES)
                            if (entry.role.equals(role)) {
                                known = true;
                                break;
                            }
                        if (!known)
                            continue;
                        try {
                            byte[] bytes = Base64.decode(jfonts.getString(role), Base64.NO_WRAP);
                            File dest = CustomFont.storedFile(context, role);
                            File dir = dest.getParentFile();
                            if (dir != null && !dir.exists() && !dir.mkdirs())
                                throw new IOException("Cannot create " + dir);
                            try (FileOutputStream out = new FileOutputStream(dest)) {
                                out.write(bytes);
                            }
                            editor.putString(CustomFont.prefPath(role), dest.getAbsolutePath());
                        } catch (Throwable ex) {
                            Log.w(ex);
                        }
                    }
                }

                // commit(), not apply(): the automation door replies success and 応用管理 then
                // force-stops this app with a SIGKILL, so an asynchronous write still in flight
                // is simply lost - the restore reports success over data that is gone, and it is
                // invisible in testing because a hand-run import is followed by a normal
                // lifecycle that flushes properly. Free on both callers: the panel imports
                // inside SimpleTask.onExecute and the door on a parallel executor, so neither
                // path is on the main thread and neither trades a truncated restore for an ANR.
                editor.commit();
                ApplicationEx.upgrade(context);
            }
            step(context, progress, cats, done, CAT_SETTINGS);
            step(context, progress, cats, done, CAT_UI);

            if (catChannels && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                    jimport.has("channels")) {
                JSONArray jchannels = jimport.getJSONArray("channels");
                for (int i = 0; i < jchannels.length(); i++)
                    try {
                        JSONObject jchannel = (JSONObject) jchannels.get(i);

                        String channelId = jchannel.getString("id");
                        nm.deleteNotificationChannel(channelId);

                        nm.createNotificationChannel(NotificationHelper.channelFromJSON(context, jchannel));
                        nChannels++;
                    } catch (Throwable ex) {
                        Log.e(ex);
                    }
            }

            step(context, progress, cats, done, CAT_CHANNELS);

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        // Outside the transaction: restoring the mail store writes thousands of rows and
        // their payload files, and it needs the folders the transaction above committed.
        // ATTACHMENTS ARE TAKEN UNLESS THE ARCHIVE ITSELF DISTINGUISHES THEM AND THEY WERE NOT
        // ASKED FOR. A format 1 archive names only "messages" and has the payloads inside it, so
        // reading the caller's selection literally there would silently drop every attachment in
        // every backup written before the split - which is the exact failure the split was made
        // to avoid. Only an archive that declares the category separately can have it withheld.
        boolean withAttachments = (cats.contains(CAT_ATTACHMENTS) ||
                !categoriesIn(source).contains(CAT_ATTACHMENTS));
        int nMessages = (catMessages
                ? importMessages(context, source, withAttachments, progress) : 0);
        step(context, progress, cats, done, CAT_MESSAGES);
        step(context, progress, cats, done, CAT_ATTACHMENTS);

        // And flush what this app does not own the editor for. ApplicationEx.upgrade writes
        // with apply() behind our back, which our own edit() cannot reach; an empty commit()
        // blocks on that file's write lock until everything already published into its
        // in-memory map has been written, so there is no need to track which keys were pending.
        PreferenceManager.getDefaultSharedPreferences(context).edit().commit();

        EntityLog.log(context, "UI import done");

        StringBuilder summary = new StringBuilder();
        if (catAccounts)
            summary.append(context.getString(R.string.title_ui_eim_cat_accounts)).append(": ").append(nAccounts).append('\n');
        if (catRules)
            summary.append(context.getString(R.string.title_ui_eim_cat_rules)).append(": ").append(nRules).append('\n');
        if (catContacts)
            summary.append(context.getString(R.string.title_ui_eim_cat_contacts)).append(": ").append(nContacts).append('\n');
        if (catMessages)
            summary.append(context.getString(R.string.title_ui_eim_cat_messages)).append(": ").append(nMessages).append('\n');
        if (catAnswers)
            summary.append(context.getString(R.string.title_ui_eim_cat_answers)).append(": ").append(nAnswers).append('\n');
        if (catSearches)
            summary.append(context.getString(R.string.title_ui_eim_cat_searches)).append(": ").append(nSearches).append('\n');
        if (catChannels)
            summary.append(context.getString(R.string.title_ui_eim_cat_channels)).append(": ").append(nChannels).append('\n');
        if (catSettings)
            summary.append(context.getString(R.string.title_ui_eim_cat_settings)).append(": ").append(nSettings).append('\n');
        if (catUi)
            summary.append(context.getString(R.string.title_ui_eim_cat_ui)).append(": ").append(nUi).append('\n');
        if (summary.length() > 0)
            summary.setLength(summary.length() - 1);

        return summary.toString();
    }

    /**
     * Restore the local mail store in a second, single pass over the ZIP: the index is the
     * first messages entry, so every message row and attachment row exists by the time its
     * payload entry comes past, and each payload streams straight to its destination file.
     * Messages whose account (by UUID) or folder (by name) is not present are skipped, as
     * are ones already in that folder with the same message id — the import merges, it
     * never duplicates. A backup written without this category simply has no such entries.
     */
    private static int importMessages(Context context, Source source, boolean withAttachments,
                                      @Nullable Progress progress) throws Throwable {
        DB db = DB.getInstance(context);

        Map<String, EntityAccount> accounts = new HashMap<>();
        Map<String, EntityFolder> folders = new HashMap<>();
        Map<String, File> targets = new HashMap<>();
        // Entry name to attachment id, for the rows that go in marked absent and are only
        // marked present once their payload has actually been written.
        Map<String, Long> arrivals = new HashMap<>();
        int imported = 0;
        int skipped = 0;
        // Index records read, which is the message count the payload pass then counts against:
        // the index is the first messages entry, so it is complete before the first payload
        // arrives. Not known any earlier than that - the index is JSON-Lines read as a stream,
        // and counting its lines up front would mean decompressing the whole thing twice.
        int records = 0;
        // The messages/<n>/ prefix of the previous payload entry, and how many distinct ones
        // have gone past.
        String group = null;
        int passed = 0;

        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(source.open()))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                if (name == null || entry.isDirectory())
                    continue;

                if (ENTRY_MESSAGES_INDEX.equals(name)) {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(zis, StandardCharsets.UTF_8));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty())
                            continue;
                        try {
                            if (importMessage(context, db, new JSONObject(line),
                                    accounts, folders, targets, arrivals))
                                imported++;
                            else
                                skipped++;
                        } catch (Throwable ex) {
                            Log.w(ex);
                            skipped++;
                        }
                        records++;
                        if (progress != null)
                            progress.report(records, 0, UNIT_MESSAGE,
                                    UNIT_MESSAGE + " 目録 " + records);
                    }
                } else if (name.startsWith(ENTRY_MESSAGES)) {
                    // A message's payload entries are contiguous under messages/<n>/, so a
                    // change of that prefix is one more message gone by. Counted here, ahead of
                    // the two skips below, because this counts the archive's own numbering -
                    // the same thing the export counted on the way out. A message dropped for a
                    // folder this device does not have still went past, and a counter that
                    // stalled on it would be reporting this device's decisions, not progress.
                    int slash = name.indexOf('/', ENTRY_MESSAGES.length());
                    String base = (slash < 0 ? name : name.substring(0, slash + 1));
                    if (!base.equals(group)) {
                        group = base;
                        passed++;
                        if (progress != null)
                            progress.report(passed, records, UNIT_MESSAGE,
                                    UNIT_MESSAGE + " " + passed + "/" + records);
                    }

                    File target = targets.get(name);
                    if (target == null)
                        continue;
                    // Left where it was inserted: the row stays marked absent and the
                    // attachment is fetched from the server the first time it is opened.
                    if (!withAttachments && arrivals.containsKey(name))
                        continue;
                    File dir = target.getParentFile();
                    if (dir != null && !dir.exists() && !dir.mkdirs())
                        throw new IOException("Cannot create " + dir);
                    try (FileOutputStream out = new FileOutputStream(target)) {
                        copy(zis, out);
                    }
                    // The payload is on disk now, so the row may claim it. Size from the file
                    // rather than from the JSON: what was written is the truth, and a
                    // half-written entry must not be advertised at its intended length.
                    Long attachment = arrivals.get(name);
                    if (attachment != null)
                        db.attachment().setDownloaded(attachment, target.length());
                }
            }
        }

        EntityLog.log(context, "UI import messages=" + imported + " skipped=" + skipped);
        return imported;
    }

    /** One index record: insert the message and its attachments, and register their files. */
    private static boolean importMessage(Context context, DB db, JSONObject jrecord,
                                         Map<String, EntityAccount> accounts,
                                         Map<String, EntityFolder> folders,
                                         Map<String, File> targets,
                                         Map<String, Long> arrivals) throws Throwable {
        String uuid = jrecord.optString("account", null);
        String folderName = jrecord.optString("folder", null);
        if (uuid == null || folderName == null || !jrecord.has("message"))
            return false;

        EntityAccount account = accounts.get(uuid);
        if (account == null) {
            account = db.account().getAccountByUUID(uuid);
            if (account == null)
                return false;
            accounts.put(uuid, account);
        }

        String folderKey = uuid + "\n" + folderName;
        EntityFolder folder = folders.get(folderKey);
        if (folder == null) {
            folder = db.folder().getFolderByName(account.id, folderName);
            if (folder == null)
                return false;
            folders.put(folderKey, folder);
        }

        EntityMessage message = new EntityMessage();
        jsonToEntity(jrecord.getJSONObject("message"), message);
        message.id = null;
        message.account = account.id;
        message.folder = folder.id;
        message.identity = null;
        message.replying = null;
        message.forwarding = null;
        message.fts = false;

        if (!TextUtils.isEmpty(message.msgid))
            for (EntityMessage other : db.message().getMessagesByMsgId(account.id, message.msgid))
                if (Objects.equals(other.folder, folder.id))
                    return false;

        message.id = db.message().insertMessage(message);

        String base = ENTRY_MESSAGES + jrecord.optInt("index", -1) + "/";
        targets.put(base + "body.html", message.getFile(context));
        targets.put(base + "raw.eml", message.getRawFile(context));

        JSONArray jattachments = jrecord.optJSONArray("attachments");
        for (int a = 0; jattachments != null && a < jattachments.length(); a++) {
            JSONObject jattachment = jattachments.getJSONObject(a);
            EntityAttachment attachment = new EntityAttachment();
            jsonToEntity(jattachment, attachment);
            attachment.id = null;
            attachment.message = message.id;

            // NOT AVAILABLE UNTIL THE PAYLOAD ACTUALLY ARRIVES. The row carries available=true
            // from the export, and believing it is how an attachment ends up listed, opened and
            // failing, with the app convinced it already holds the file and never fetching it.
            // Since the attachments category became separable the archive may legitimately
            // carry the row without the payload, and a truncated archive always could. So the
            // row goes in marked absent and is flipped by importMessages when its entry comes
            // past; anything that never arrives is simply re-downloaded from the server.
            attachment.available = false;
            attachment.progress = null;
            attachment.id = db.attachment().insertAttachment(attachment);

            String file = jattachment.optString("file", null);
            if (file != null) {
                targets.put(base + file, attachment.getFile(context));
                arrivals.put(base + file, attachment.id);
            }
        }

        return true;
    }
}
