package eu.faircode.email;

/*
    This file is part of FairEmail.

    FairEmail is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    FairEmail is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with FairEmail.  If not, see <http://www.gnu.org/licenses/>.

    Copyright 2018-2026 by Marcel Bokhorst (M66B)
*/

import android.content.Context;
import android.content.SharedPreferences;
import android.util.TypedValue;

import androidx.annotation.AttrRes;
import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Registry of the Custom theme's user-overridable colour roles.
 *
 * <p>Iteration 1.1 surface: the entry list (used to drive the picker UI) and a
 * helper to compute the effective colour for a swatch (user pref if set, else
 * XML default).</p>
 *
 * <p>Iteration 1.2 surface: override-resolution helpers consulted by
 * {@link Helper#resolveColor(Context, int, int)} and
 * {@link ActivityBase#onSharedPreferenceChanged(SharedPreferences, String)}.
 * Together these make picking a colour for a code-resolved theme attr (colorRead,
 * colorUnread, colorSenderAccent, colorDrawerBackground, colorCardBackground,
 * colorFabForeground) actually take effect on next activity creation.</p>
 *
 * <p>Iteration 1.3 will add a Resources wrapper at Activity level only, to also
 * catch the XML-resolved paths (windowBackground drawable, ?attr/colorPrimary
 * in toolbar layouts, layout textColor references).</p>
 */
final class CustomThemeColors {
    private CustomThemeColors() {
    }

    /** A single customizable colour role. */
    static final class Entry {
        final String section;
        @StringRes final int sectionLabelRes;
        final String prefKey;
        @StringRes final int labelRes;
        @StringRes final int descriptionRes;
        @ColorRes final int colorRes;

        Entry(String section, @StringRes int sectionLabelRes,
              String prefKey, @StringRes int labelRes,
              @StringRes int descriptionRes, @ColorRes int colorRes) {
            this.section = section;
            this.sectionLabelRes = sectionLabelRes;
            this.prefKey = prefKey;
            this.labelRes = labelRes;
            this.descriptionRes = descriptionRes;
            this.colorRes = colorRes;
        }
    }

    /**
     * Customizable roles. Adjacent entries with the same {@link Entry#section}
     * id are visually grouped under one header (rendered using
     * {@link Entry#sectionLabelRes}). Iteration 3.2 expands this list to cover
     * the remaining theme attrs that currently fall back to literal
     * customYellow / customGold / customDimYellow / customOrange / black.
     */
    static final Entry[] ENTRIES = new Entry[]{
            new Entry("backgrounds", R.string.title_custom_color_section_backgrounds,
                    "custom_color_background", R.string.title_custom_color_background,
                    R.string.title_custom_color_background_desc, R.color.customColorBackground),
            new Entry("text", R.string.title_custom_color_section_text,
                    "custom_color_text_primary", R.string.title_custom_color_text_primary,
                    R.string.title_custom_color_text_primary_desc, R.color.customColorTextPrimary),
            new Entry("accents", R.string.title_custom_color_section_accents,
                    "custom_color_read", R.string.title_custom_color_read,
                    R.string.title_custom_color_read_desc, R.color.customColorRead),
            new Entry("accents", R.string.title_custom_color_section_accents,
                    "custom_color_unread", R.string.title_custom_color_unread,
                    R.string.title_custom_color_unread_desc, R.color.customColorUnread),
            new Entry("accents", R.string.title_custom_color_section_accents,
                    "custom_color_sender", R.string.title_custom_color_sender,
                    R.string.title_custom_color_sender_desc, R.color.customColorSender),
    };

    /** Resource id → pref key map; built once at class init from {@link #ENTRIES}. */
    private static final Map<Integer, String> RES_TO_PREF;

    static {
        Map<Integer, String> m = new LinkedHashMap<>();
        for (Entry e : ENTRIES)
            m.put(e.colorRes, e.prefKey);
        RES_TO_PREF = m;
    }

    /**
     * Returns the effective colour for a given customizable entry — the user's
     * override if set, otherwise the XML default. Used by the picker UI to
     * display the swatch.
     */
    static int getEffectiveColor(@NonNull Context context, @NonNull Entry entry) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (prefs.contains(entry.prefKey))
            return prefs.getInt(entry.prefKey, 0);
        return ContextCompat.getColor(context, entry.colorRes);
    }

    /** True if the user is currently on the Custom theme. */
    static boolean isCustomTheme(@NonNull Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return "custom".equals(prefs.getString("theme", null));
    }

    /** True if the given pref key belongs to one of the customizable colour entries. */
    static boolean isCustomColorPref(@Nullable String key) {
        return key != null && key.startsWith("custom_color_");
    }

    /**
     * If {@code colorRes} is a customizable colour AND the active theme is Custom AND
     * the user has set an override pref, returns the override. Otherwise returns null
     * so the caller can fall back to the XML default.
     */
    @Nullable
    static Integer getOverrideForColorRes(@NonNull Context context, @ColorRes int colorRes) {
        String prefKey = RES_TO_PREF.get(colorRes);
        if (prefKey == null) return null;
        if (!isCustomTheme(context)) return null;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (!prefs.contains(prefKey)) return null;
        return prefs.getInt(prefKey, 0);
    }

    /**
     * If a theme attribute resolves (in the active theme) to one of the customizable
     * colour resources AND the active theme is Custom AND the user has set an override
     * pref for that resource, returns the override. Otherwise returns null. Used by
     * {@link Helper#resolveColor(Context, int, int)} to short-circuit the
     * obtainStyledAttributes path.
     */
    @Nullable
    static Integer getOverrideForAttr(@NonNull Context context, @AttrRes int attr) {
        if (!isCustomTheme(context))
            return null;
        TypedValue tv = new TypedValue();
        if (!context.getTheme().resolveAttribute(attr, tv, true))
            return null;
        if (tv.resourceId == 0)
            return null;
        return getOverrideForColorRes(context, tv.resourceId);
    }
}
