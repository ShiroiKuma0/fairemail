/*
    Custom font + weight overrides for message and chrome text.

    Each customizable surface ("role") has independent font and weight settings.
    Font picks cascade: a role with no picked font falls back to the Default role's
    font; if Default is also unset, no override is applied. Weight does NOT cascade
    — each role's weight is independent, with 0 meaning "use the typeface's natural
    weight" and 100..900 meaning "force this CSS weight via Typeface.create on API 28+".

    The user picks a TTF / OTF file per role via SAF; the bytes are copied into the
    app's internal files dir under custom_fonts/<filename>, with a separate file slot
    per role so picks don't overwrite each other. Each role's pref keys are derived
    from its role id; the Default role keeps the legacy bare names ("custom_font_path",
    "custom_font_name", "custom_font_weight") so existing users' picks survive.

    Style preservation: CustomFont.apply reads the italic and bold flags from whatever
    typeface the bind code just installed and routes them through to the result. Italic
    propagates unchanged; bold is mapped onto a +300 weight boost (capped at 900) so
    unread rows stay visually heavier than read rows at the same user-chosen base.
*/

package eu.faircode.email;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class CustomFont {
    static final String ROLE_DEFAULT = "default";
    static final String ROLE_LIST_SENDER = "list_sender";
    static final String ROLE_LIST_SUBJECT = "list_subject";
    static final String ROLE_LIST_PREVIEW = "list_preview";
    static final String ROLE_VIEW_BODY = "view_body";
    static final String ROLE_VIEW_SUBJECT = "view_subject";
    static final String ROLE_VIEW_SENDER = "view_sender";
    static final String ROLE_TOP_BAR = "top_bar";

    static final String SECTION_DEFAULT = "default";
    static final String SECTION_LIST = "list";
    static final String SECTION_VIEW = "view";
    static final String SECTION_CHROME = "chrome";

    private static final long MAX_FONT_BYTES = 20L * 1024L * 1024L; // 20 MB
    private static final String INTERNAL_DIR = "custom_fonts";
    private static final String MIGRATION_KEY = "custom_font_migrated_v2";

    /**
     * One entry per customizable role. Adjacent entries with the same section id
     * render under one section header in the picker. Order here is display order.
     */
    static final class Entry {
        final String role;
        final String section;
        final int sectionLabelRes;
        final int labelRes;
        final int descriptionRes;

        Entry(String role, String section, int sectionLabelRes, int labelRes, int descriptionRes) {
            this.role = role;
            this.section = section;
            this.sectionLabelRes = sectionLabelRes;
            this.labelRes = labelRes;
            this.descriptionRes = descriptionRes;
        }
    }

    static final Entry[] ENTRIES = new Entry[]{
            new Entry(ROLE_DEFAULT, SECTION_DEFAULT,
                    R.string.title_custom_font_section_default,
                    R.string.title_custom_font_default,
                    R.string.title_custom_font_default_desc),

            new Entry(ROLE_LIST_SENDER, SECTION_LIST,
                    R.string.title_custom_font_section_list,
                    R.string.title_custom_font_list_sender,
                    R.string.title_custom_font_list_sender_desc),
            new Entry(ROLE_LIST_SUBJECT, SECTION_LIST,
                    R.string.title_custom_font_section_list,
                    R.string.title_custom_font_list_subject,
                    R.string.title_custom_font_list_subject_desc),
            new Entry(ROLE_LIST_PREVIEW, SECTION_LIST,
                    R.string.title_custom_font_section_list,
                    R.string.title_custom_font_list_preview,
                    R.string.title_custom_font_list_preview_desc),

            new Entry(ROLE_VIEW_BODY, SECTION_VIEW,
                    R.string.title_custom_font_section_view,
                    R.string.title_custom_font_view_body,
                    R.string.title_custom_font_view_body_desc),
            new Entry(ROLE_VIEW_SUBJECT, SECTION_VIEW,
                    R.string.title_custom_font_section_view,
                    R.string.title_custom_font_view_subject,
                    R.string.title_custom_font_view_subject_desc),
            new Entry(ROLE_VIEW_SENDER, SECTION_VIEW,
                    R.string.title_custom_font_section_view,
                    R.string.title_custom_font_view_sender,
                    R.string.title_custom_font_view_sender_desc),

            new Entry(ROLE_TOP_BAR, SECTION_CHROME,
                    R.string.title_custom_font_section_chrome,
                    R.string.title_custom_font_top_bar,
                    R.string.title_custom_font_top_bar_desc),
    };

    /** Cache keyed by file path so multiple roles pointing at the same path share state. */
    private static final Map<String, Typeface> typefaceCache = new ConcurrentHashMap<>();

    static String prefPath(String role) {
        return ROLE_DEFAULT.equals(role) ? "custom_font_path" : "custom_font_path_" + role;
    }

    static String prefName(String role) {
        return ROLE_DEFAULT.equals(role) ? "custom_font_name" : "custom_font_name_" + role;
    }

    static String prefWeight(String role) {
        return ROLE_DEFAULT.equals(role) ? "custom_font_weight" : "custom_font_weight_" + role;
    }

    private static String filename(String role) {
        return ROLE_DEFAULT.equals(role) ? "picked.ttf" : role + ".ttf";
    }

    /**
     * Returns true for any pref key that should trigger an activity recreate when changed.
     * Path and weight prefs trigger recreates; name prefs do not (display metadata only)
     * for the SAF-callback lifecycle reason documented on FragmentOptionsDisplay.onFontPicked.
     */
    static boolean isPrefKey(String key) {
        for (Entry entry : ENTRIES) {
            if (prefPath(entry.role).equals(key)) return true;
            if (prefWeight(entry.role).equals(key)) return true;
        }
        return false;
    }

    /**
     * Returns true for any pref key the Display "reset" action should wipe alongside
     * everything else. Includes name keys (since reset should clear the display label too).
     */
    static boolean isResetKey(String key) {
        for (Entry entry : ENTRIES) {
            if (prefPath(entry.role).equals(key)) return true;
            if (prefName(entry.role).equals(key)) return true;
            if (prefWeight(entry.role).equals(key)) return true;
        }
        return false;
    }

    /**
     * Resolve the typeface to use for the given role. Cascades to the Default role's
     * font if the role has no picked font of its own. Returns null when neither role
     * nor Default has a picked font.
     */
    @Nullable
    static Typeface getTypeface(Context context, String role) {
        Typeface tf = loadTypefaceForRole(context, role);
        if (tf != null) return tf;
        if (!ROLE_DEFAULT.equals(role))
            return loadTypefaceForRole(context, ROLE_DEFAULT);
        return null;
    }

    @Nullable
    private static Typeface loadTypefaceForRole(Context context, String role) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String path = prefs.getString(prefPath(role), null);
        if (TextUtils.isEmpty(path)) return null;

        Typeface cached = typefaceCache.get(path);
        if (cached != null) return cached;

        try {
            File f = new File(path);
            if (!f.exists()) return null;
            Typeface tf = Typeface.createFromFile(f);
            if (tf != null) typefaceCache.put(path, tf);
            return tf;
        } catch (Throwable ex) {
            Log.w(ex);
            return null;
        }
    }

    /** Per-role weight (0..900). Does NOT cascade to Default — each role is independent. */
    static int getWeight(Context context, String role) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getInt(prefWeight(role), 0);
    }

    /**
     * Apply the custom font + weight for the given role to the view, preserving the
     * italic and bold style flags of whatever typeface is currently set on the view.
     * Idempotent — calling this with both prefs unset for the role (and Default, after
     * font cascade) leaves the TextView's typeface unchanged.
     */
    static void apply(Context context, @Nullable TextView view, String role) {
        if (view == null) return;
        Typeface custom = getTypeface(context, role);
        int weight = getWeight(context, role);
        if (custom == null && weight <= 0) return; // no override at all

        Typeface current = (view.getTypeface() != null ? view.getTypeface() : Typeface.DEFAULT);
        int currentStyle = current.getStyle();
        boolean italic = (currentStyle & Typeface.ITALIC) != 0;
        boolean bold = (currentStyle & Typeface.BOLD) != 0;
        Typeface base = (custom != null ? custom : current);
        Typeface result;
        if (weight > 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            int effectiveWeight = bold ? Math.min(900, weight + 300) : weight;
            result = Typeface.create(base, effectiveWeight, italic);
        } else if (currentStyle != Typeface.NORMAL) {
            result = Typeface.create(base, currentStyle);
        } else {
            result = base;
        }
        view.setTypeface(result);
    }

    /**
     * Copies the font bytes from {@code uri} into the app's internal storage under the
     * slot for {@code role} and returns the absolute path on success. The cache entry
     * for any previous file at this role's slot is invalidated.
     */
    static String copyToInternal(Context context, Uri uri, String role) throws IOException {
        File dir = new File(context.getFilesDir(), INTERNAL_DIR);
        if (!dir.exists() && !dir.mkdirs())
            throw new IOException("Cannot create " + dir);
        File dest = new File(dir, filename(role));
        try (InputStream in = context.getContentResolver().openInputStream(uri);
             FileOutputStream out = new FileOutputStream(dest)) {
            if (in == null)
                throw new IOException("Cannot open " + uri);
            byte[] buf = new byte[8192];
            int n;
            long total = 0;
            while ((n = in.read(buf)) > 0) {
                total += n;
                if (total > MAX_FONT_BYTES) {
                    out.close();
                    if (!dest.delete())
                        Log.w("Could not delete oversized font file " + dest);
                    throw new IOException("Font file exceeds " + MAX_FONT_BYTES + " bytes");
                }
                out.write(buf, 0, n);
            }
        }
        // Invalidate cache for whatever was previously at this role's path so the next
        // getTypeface re-reads from disk and picks up the new file's contents.
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String oldPath = prefs.getString(prefPath(role), null);
        if (!TextUtils.isEmpty(oldPath))
            typefaceCache.remove(oldPath);
        typefaceCache.remove(dest.getAbsolutePath());
        return dest.getAbsolutePath();
    }

    /**
     * The internal-storage slot for {@code role}'s picked font file. Used by the
     * UI page export/import to carry the font binaries across devices; the file
     * may or may not exist.
     */
    static File storedFile(Context context, String role) {
        return new File(new File(context.getFilesDir(), INTERNAL_DIR), filename(role));
    }

    /** Removes the picked font file for {@code role} and drops its cache entry. */
    static void clearStoredFile(Context context, String role) {
        File dest = new File(new File(context.getFilesDir(), INTERNAL_DIR), filename(role));
        if (dest.exists() && !dest.delete())
            Log.w("Could not delete font file " + dest);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String oldPath = prefs.getString(prefPath(role), null);
        if (!TextUtils.isEmpty(oldPath))
            typefaceCache.remove(oldPath);
    }

    /**
     * One-time migration on first launch with the role-based design. The previous
     * iteration had a single global weight pref ("custom_font_weight") that applied
     * to all 3 hooked views (tvFrom, tvSubject, tvBody). If the user had set that
     * weight, copy it to each non-Default role's weight pref so their existing config
     * keeps working — they are not silently dropped to weight=0 just because the
     * pref key shape changed. Idempotent via the MIGRATION_KEY flag.
     */
    static void migrateLegacyWeightIfNeeded(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (prefs.getBoolean(MIGRATION_KEY, false)) return;
        int legacyWeight = prefs.getInt(prefWeight(ROLE_DEFAULT), 0);
        SharedPreferences.Editor editor = prefs.edit();
        if (legacyWeight > 0) {
            for (Entry entry : ENTRIES) {
                if (ROLE_DEFAULT.equals(entry.role)) continue;
                String key = prefWeight(entry.role);
                if (!prefs.contains(key))
                    editor.putInt(key, legacyWeight);
            }
        }
        editor.putBoolean(MIGRATION_KEY, true);
        editor.apply();
    }

    /**
     * Reads the OpenableColumns.DISPLAY_NAME for a content URI. Returns null if not
     * available (caller should fall back to a generic label).
     */
    @Nullable
    static String getDisplayName(Context context, Uri uri) {
        try (Cursor c = context.getContentResolver().query(
                uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                int idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0)
                    return c.getString(idx);
            }
        } catch (Throwable ex) {
            Log.w(ex);
        }
        return null;
    }
}
