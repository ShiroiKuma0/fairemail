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
import android.content.ContextWrapper;
import android.content.res.Resources;

import androidx.annotation.NonNull;

/**
 * ContextWrapper that returns a {@link ColorOverrideResources} so colour
 * lookups for the Custom theme's customizable resources go through the
 * pref-aware override path.
 *
 * <p>Installed only in {@link ActivityBase#attachBaseContext(Context)};
 * <strong>never</strong> in {@link ApplicationEx#attachBaseContext(Context)}.
 * The Android framework casts the Application context to package-private
 * {@code ContextImpl} when instantiating BroadcastReceivers, and a wrapper
 * there causes {@code ClassCastException} on app startup.</p>
 *
 * <p>Placing the wrapper at the very base of the Activity context chain
 * ensures {@code ContextThemeWrapper}'s lazy Resources cache picks up the
 * wrapped instance from the start.</p>
 */
class ColorOverrideContextWrapper extends ContextWrapper {
    private Resources wrappedResources;

    ColorOverrideContextWrapper(@NonNull Context base) {
        super(base);
    }

    @Override
    public Resources getResources() {
        if (wrappedResources == null)
            wrappedResources = new ColorOverrideResources(this, super.getResources());
        return wrappedResources;
    }
}
