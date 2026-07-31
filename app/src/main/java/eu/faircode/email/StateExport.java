package eu.faircode.email;

import static eu.faircode.email.ServiceAuthenticator.AUTH_TYPE_GMAIL;

import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.content.ContentResolver;
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
    static final int VERSION = 1;
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
    static final String CAT_ANSWERS = "answers";
    static final String CAT_SEARCHES = "searches";
    static final String CAT_CHANNELS = "channels";
    static final String CAT_SETTINGS = "settings";
    static final String CAT_UI = "ui";

    static final String[] CAT_IDS = {
            CAT_ACCOUNTS, CAT_RULES, CAT_CONTACTS, CAT_MESSAGES, CAT_ANSWERS,
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
            false,  // messages: the local mail store is the largest thing here and re-syncs
                    // from the server using the account definitions in accounts, which is on
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
     * Write ONE backup ZIP of the selected categories to {@code out}. The caller owns
     * the stream and closes it; this only finishes the ZIP central directory.
     */
    static void export(Context context, List<String> cats, OutputStream out,
                       @Nullable Progress progress) throws Throwable {
        int[] done = {0};
        JSONObject jexport = buildExport(context, cats, progress, done);

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
            exportMessages(context, zos, cats, progress, done);

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
                                  @Nullable Progress progress, int[] done) throws Throwable {
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
                                       @Nullable Progress progress, int[] done) throws Throwable {
        DB db = DB.getInstance(context);

        List<MessageRef> refs = new ArrayList<>();
        for (EntityAccount account : db.account().getAccounts())
            for (EntityFolder folder : db.folder().getFolders(account.id, false, true))
                for (Long id : db.message().getMessageByFolder(folder.id))
                    if (id != null)
                        refs.add(new MessageRef(id, account.uuid, folder.name));

        int total = refs.size();

        zos.putNextEntry(new ZipEntry(ENTRY_MESSAGES_INDEX));
        for (int i = 0; i < total; i++) {
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
            MessageRef ref = refs.get(i);
            EntityMessage message = db.message().getMessage(ref.id);
            if (message == null)
                continue;

            String base = ENTRY_MESSAGES + i + "/";

            File body = message.getFile(context);
            if (body.exists())
                copyEntry(zos, base + "body.html", body);

            File raw = message.getRawFile(context);
            if (raw.exists())
                copyEntry(zos, base + "raw.eml", raw);

            List<EntityAttachment> attachments = db.attachment().getAttachments(message.id);
            for (int a = 0; a < attachments.size(); a++) {
                File file = attachments.get(a).getFile(context);
                if (file.exists())
                    copyEntry(zos, base + "att-" + a, file);
            }

            if (progress != null)
                progress.report(i + 1, total, UNIT_MESSAGE,
                        UNIT_MESSAGE + " " + (i + 1) + "/" + total);
        }

        step(context, progress, cats, done, CAT_MESSAGES);
    }

    private static void copyEntry(ZipOutputStream zos, String name, File file) throws IOException {
        zos.putNextEntry(new ZipEntry(name));
        try (FileInputStream in = new FileInputStream(file)) {
            copy(in, zos);
        }
        zos.closeEntry();
    }

    private static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[8192];
        int n;
        while ((n = in.read(buffer)) > 0)
            out.write(buffer, 0, n);
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

    /**
     * Read the export JSON out of a backup: the {@link #ENTRY_EXPORT} entry of a
     * family-convention ZIP, or the whole file when it is a bare JSON export (an
     * older fork backup or a stock unencrypted upstream one).
     */
    private static String readExportJson(Context context, Uri uri) throws Throwable {
        ContentResolver resolver = context.getContentResolver();
        InputStream is = resolver.openInputStream(uri);
        if (is == null)
            throw new FileNotFoundException(uri.toString());

        try (BufferedInputStream bis = new BufferedInputStream(is)) {
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
        while ((n = is.read(buffer)) > 0)
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
        boolean catAccounts = cats.contains(CAT_ACCOUNTS);
        boolean catRules = cats.contains(CAT_RULES);
        boolean catContacts = cats.contains(CAT_CONTACTS);
        boolean catMessages = cats.contains(CAT_MESSAGES);
        boolean catAnswers = cats.contains(CAT_ANSWERS);
        boolean catSearches = cats.contains(CAT_SEARCHES);
        boolean catChannels = cats.contains(CAT_CHANNELS);
        boolean catSettings = cats.contains(CAT_SETTINGS);
        boolean catUi = cats.contains(CAT_UI);

        EntityLog.log(context, "UI import " + uri + " cats=" + TextUtils.join(",", cats));

        NoStreamException.check(uri, context);

        String json = readExportJson(context, uri).trim();
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

            if (catAccounts && jimport.has("accounts")) {
                JSONArray jaccounts = jimport.getJSONArray("accounts");
                for (int a = 0; a < jaccounts.length(); a++) {
                    JSONObject jaccount = (JSONObject) jaccounts.get(a);
                    EntityAccount account = EntityAccount.fromJSON(jaccount);

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

                editor.apply();
                ApplicationEx.upgrade(context);
            }

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

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        // Outside the transaction: restoring the mail store writes thousands of rows and
        // their payload files, and it needs the folders the transaction above committed.
        int nMessages = (catMessages ? importMessages(context, uri) : 0);

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
    private static int importMessages(Context context, Uri uri) throws Throwable {
        DB db = DB.getInstance(context);
        ContentResolver resolver = context.getContentResolver();

        Map<String, EntityAccount> accounts = new HashMap<>();
        Map<String, EntityFolder> folders = new HashMap<>();
        Map<String, File> targets = new HashMap<>();
        int imported = 0;
        int skipped = 0;

        InputStream is = resolver.openInputStream(uri);
        if (is == null)
            throw new FileNotFoundException(uri.toString());

        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(is))) {
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
                                    accounts, folders, targets))
                                imported++;
                            else
                                skipped++;
                        } catch (Throwable ex) {
                            Log.w(ex);
                            skipped++;
                        }
                    }
                } else if (name.startsWith(ENTRY_MESSAGES)) {
                    File target = targets.get(name);
                    if (target == null)
                        continue;
                    File dir = target.getParentFile();
                    if (dir != null && !dir.exists() && !dir.mkdirs())
                        throw new IOException("Cannot create " + dir);
                    try (FileOutputStream out = new FileOutputStream(target)) {
                        copy(zis, out);
                    }
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
                                         Map<String, File> targets) throws Throwable {
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
            attachment.id = db.attachment().insertAttachment(attachment);

            String file = jattachment.optString("file", null);
            if (file != null)
                targets.put(base + file, attachment.getFile(context));
        }

        return true;
    }
}
