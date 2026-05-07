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
import android.content.res.Resources;

import androidx.annotation.NonNull;

/**
 * Resources subclass that intercepts {@link #getColor(int)} and
 * {@link #getColor(int, Theme)} for the colour resources registered in
 * {@link CustomThemeColors}. When the active theme is {@code custom} and the
 * user has set a {@code custom_color_*} preference, the override is returned;
 * otherwise the call falls through to the XML default.
 *
 * <p>Catches the layout-XML attr-to-colour-resource resolution path that
 * {@link Helper#resolveColor(Context, int, int)} (added in iteration 1.2)
 * cannot reach — for example, the window background drawable's
 * {@code <solid android:color="@color/customColorBackground"/>} reference, and
 * {@code TextView} declarations using {@code android:textColor="?attr/...".}</p>
 *
 * <p>Subclassing {@code Resources} relies on the deprecated three-arg
 * constructor; this still works on all supported Android versions but does not
 * carry over the system's internal {@code ResourcesImpl}, so the override
 * surface is kept narrow on purpose. Anything we don't override goes through
 * the base {@code Resources}'s implementation via super.</p>
 *
 * <p>This wrapper is installed at {@link ActivityBase#attachBaseContext(Context)}
 * only — never at the Application level, since the Android framework casts
 * the application context directly to {@code ContextImpl} when instantiating
 * BroadcastReceivers and a wrapper there causes a {@code ClassCastException}
 * crash on startup.</p>
 */
class ColorOverrideResources extends Resources {
    private final Context context;

    @SuppressWarnings("deprecation")
    ColorOverrideResources(@NonNull Context context, @NonNull Resources base) {
        super(base.getAssets(), base.getDisplayMetrics(), base.getConfiguration());
        this.context = context;
    }

    @Override
    public int getColor(int id) throws NotFoundException {
        Integer override = tryGetOverride(id);
        return override != null ? override : super.getColor(id);
    }

    @Override
    public int getColor(int id, Theme theme) throws NotFoundException {
        Integer override = tryGetOverride(id);
        return override != null ? override : super.getColor(id, theme);
    }

    /**
     * Defensive wrapper around {@link CustomThemeColors#getOverrideForColorRes}.
     * Any exception from the override path (e.g. SharedPreferences I/O issues
     * during early activity attach, before the user settings are fully initialized)
     * falls through to the system default rather than crashing.
     */
    private Integer tryGetOverride(int id) {
        try {
            return CustomThemeColors.getOverrideForColorRes(context, id);
        } catch (Throwable t) {
            Log.w(t);
            return null;
        }
    }
}
