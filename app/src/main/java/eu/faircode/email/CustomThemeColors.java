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

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

/**
 * Registry of the Custom theme's user-overridable colour roles.
 *
 * <p>Iteration 1.1 surface: the entry list (used to drive the picker UI) and a
 * helper to compute the effective colour for a swatch (user pref if set, else
 * XML default). The runtime override mechanism — that actually swaps in the
 * user's chosen colour at colour-resolution time — is added in iteration 1.2
 * (Helper.resolveColor short-circuit) and 1.3 (Resources wrapper at Activity
 * level only).</p>
 */
final class CustomThemeColors {
    private CustomThemeColors() {
    }

    /** A single customizable colour role. */
    static final class Entry {
        final String prefKey;
        @StringRes final int labelRes;
        @ColorRes final int colorRes;

        Entry(String prefKey, @StringRes int labelRes, @ColorRes int colorRes) {
            this.prefKey = prefKey;
            this.labelRes = labelRes;
            this.colorRes = colorRes;
        }
    }

    /**
     * Iteration 1 set of customizable roles. Order is the order shown in the picker UI.
     * Iteration 2+ extends this list to cover the remaining theme attrs.
     */
    static final Entry[] ENTRIES = new Entry[]{
            new Entry("custom_color_background", R.string.title_custom_color_background, R.color.customColorBackground),
            new Entry("custom_color_text_primary", R.string.title_custom_color_text_primary, R.color.customColorTextPrimary),
            new Entry("custom_color_read", R.string.title_custom_color_read, R.color.customColorRead),
            new Entry("custom_color_unread", R.string.title_custom_color_unread, R.color.customColorUnread),
            new Entry("custom_color_sender", R.string.title_custom_color_sender, R.color.customColorSender),
    };

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
}
