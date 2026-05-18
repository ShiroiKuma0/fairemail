/*
    Custom font + weight overrides for message text.

    The user picks a TTF / OTF file via SAF (no permission required); the bytes are
    copied into the app's internal files dir at pick time so the original URI is not
    referenced again. A separate weight preference, expressed as the standard CSS
    weight scale 100-900 with 0 meaning "use the typeface's natural weight", is
    applied via Typeface.create(Typeface, int weight, boolean italic) on API 28+.
    Both knobs are independent — weight applies to the default typeface when no
    custom font is set, and a custom font renders at its natural weight when the
    weight pref is 0.

    See CustomFont.apply(Context, TextView) for the single call site convention
    used in AdapterMessage to keep tvFrom, tvSubject and tvBody in sync with the
    user's choices on every view holder bind.
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

class CustomFont {
    static final String PREF_PATH = "custom_font_path";
    static final String PREF_NAME = "custom_font_name";
    static final String PREF_WEIGHT = "custom_font_weight";

    private static final long MAX_FONT_BYTES = 20L * 1024L * 1024L; // 20 MB
    private static final String INTERNAL_DIR = "custom_fonts";
    private static final String INTERNAL_FILE = "picked.ttf";

    private static volatile Typeface cachedTypeface = null;
    private static volatile String cachedPath = null;

    static boolean isPrefKey(String key) {
        // PREF_NAME is display-metadata only and intentionally not in this set:
        // including it would fire onSharedPreferenceChanged twice from a single
        // edit().putString(PATH).putString(NAME).apply() call (once per key),
        // and each call recreates the activity, which during the SAF result
        // callback was observed to crash. PATH and WEIGHT are the only knobs
        // that materially change what gets rendered.
        return PREF_PATH.equals(key) || PREF_WEIGHT.equals(key);
    }

    @Nullable
    static Typeface getTypeface(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String path = prefs.getString(PREF_PATH, null);
        if (TextUtils.isEmpty(path)) {
            cachedTypeface = null;
            cachedPath = null;
            return null;
        }
        if (path.equals(cachedPath) && cachedTypeface != null)
            return cachedTypeface;
        try {
            File f = new File(path);
            if (!f.exists())
                return null;
            Typeface tf = Typeface.createFromFile(f);
            cachedTypeface = tf;
            cachedPath = path;
            return tf;
        } catch (Throwable ex) {
            Log.w(ex);
            return null;
        }
    }

    /**
     * Returns the user-selected weight (0 means "no override"; otherwise 100-900).
     */
    static int getWeight(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getInt(PREF_WEIGHT, 0);
    }

    /**
     * Applies the user's custom font and/or weight to the given TextView while
     * preserving the italic and bold style flags of whatever typeface is currently
     * set on the view. Idempotent — calling this with both prefs unset leaves the
     * TextView's typeface unchanged.
     *
     * Style preservation matters because the AdapterMessage bind code sets the
     * italic flag for the sender_italic / subject_italic preferences and uses bold
     * to mark unread messages. If we just called {@code setTypeface(customFont)}
     * we would silently drop those distinctions. The bold flag is mapped onto a
     * weight boost of +300 (capped at 900) when the user has set a weight, so
     * unread items remain visually heavier than read items at the same base.
     */
    static void apply(Context context, @Nullable TextView view) {
        if (view == null)
            return;
        Typeface custom = getTypeface(context);
        int weight = getWeight(context);
        if (custom == null && weight <= 0)
            return; // nothing to apply
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
            // No weight override (or below API 28): preserve italic/bold by re-deriving
            result = Typeface.create(base, currentStyle);
        } else {
            result = base;
        }
        view.setTypeface(result);
    }

    /**
     * Copies the font bytes from {@code uri} into the app's internal storage and
     * returns the absolute path on success. Replaces any previously-picked font.
     * The cached Typeface is invalidated so the next {@link #getTypeface} re-reads
     * from disk.
     *
     * @throws IOException on read/write failure or if the file exceeds {@link #MAX_FONT_BYTES}.
     */
    static String copyToInternal(Context context, Uri uri) throws IOException {
        File dir = new File(context.getFilesDir(), INTERNAL_DIR);
        if (!dir.exists() && !dir.mkdirs())
            throw new IOException("Cannot create " + dir);
        File dest = new File(dir, INTERNAL_FILE);
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
        cachedTypeface = null;
        cachedPath = null;
        return dest.getAbsolutePath();
    }

    /**
     * Removes any picked font file from internal storage and clears the cached Typeface.
     * Callers are responsible for clearing the path and name prefs.
     */
    static void clearStoredFile(Context context) {
        File dest = new File(new File(context.getFilesDir(), INTERNAL_DIR), INTERNAL_FILE);
        if (dest.exists() && !dest.delete())
            Log.w("Could not delete font file " + dest);
        cachedTypeface = null;
        cachedPath = null;
    }

    /**
     * Reads the OpenableColumns.DISPLAY_NAME for a content URI. Returns null if
     * not available (caller should fall back to a generic label).
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
