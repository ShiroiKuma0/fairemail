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
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.RippleDrawable;
import android.widget.Button;

import androidx.appcompat.widget.AppCompatButton;
import androidx.core.graphics.ColorUtils;

// shiroikuma fork: the button of the Custom theme. Every push button is an
// outlined pill that is never filled: the main action of a block (the
// positive button of a dialog) has a 3 dp frame and bold text, every other
// one a 1.25 dp frame and normal text. Applied to each inflated Button by
// AppCompatViewInflaterEx.createButton, which also reaches the buttons of
// an AlertDialog, so no dialog needs to be touched.
class Pills {
    private static final int[][] STATES = new int[][]{
            new int[]{-android.R.attr.state_enabled},
            new int[]{}
    };

    static void apply(Button button) {
        Context context = button.getContext();
        if (!CustomThemeColors.isCustomTheme(context))
            return;

        int id = button.getId();
        boolean dialog = (id == android.R.id.button1 ||
                id == android.R.id.button2 ||
                id == android.R.id.button3);
        boolean main = (id == android.R.id.button1);

        float density = context.getResources().getDisplayMetrics().density;
        int accent = MenuPills.accent(context);

        // A tint would paint over the frame
        button.setBackgroundTintList(null);
        if (button instanceof AppCompatButton)
            ((AppCompatButton) button).setSupportBackgroundTintList(null);

        style(button, accent, main, dialog);
    }

    private static void style(Button button, int accent, boolean main, boolean dialog) {
        Context context = button.getContext();
        float density = context.getResources().getDisplayMetrics().density;

        int left = button.getPaddingLeft();
        int top = button.getPaddingTop();
        int right = button.getPaddingRight();
        int bottom = button.getPaddingBottom();

        Drawable pill = drawable(context, accent, main);
        if (dialog) {
            // The buttons of a dialog touch each other: keep the pills apart
            int h = Math.round(4 * density);
            int v = Math.round(5 * density);
            button.setBackground(new InsetDrawable(pill, h, v, h, v));
            int pad = Math.round(22 * density);
            button.setPadding(pad, top, pad, bottom);
            button.setMinWidth(Math.round(80 * density));
            button.setMinimumWidth(Math.round(80 * density));
            button.setTextColor(states(accent));
        } else {
            button.setBackground(pill);
            button.setPadding(left, top, right, bottom);
        }

        if (main)
            button.setTypeface(button.getTypeface(), Typeface.BOLD);
    }

    static Drawable drawable(Context context, int color, boolean main) {
        float density = context.getResources().getDisplayMetrics().density;
        float radius = 100 * density;

        GradientDrawable frame = new GradientDrawable();
        frame.setShape(GradientDrawable.RECTANGLE);
        frame.setCornerRadius(radius);
        frame.setColor(Color.TRANSPARENT);
        frame.setStroke(Math.max(1, Math.round((main ? 3f : 1.25f) * density)), states(color));

        GradientDrawable mask = new GradientDrawable();
        mask.setShape(GradientDrawable.RECTANGLE);
        mask.setCornerRadius(radius);
        mask.setColor(Color.WHITE);

        int ripple = ColorUtils.setAlphaComponent(color, Math.round(Color.alpha(color) * 0.3f));
        return new RippleDrawable(ColorStateList.valueOf(ripple), frame, mask);
    }

    private static ColorStateList states(int color) {
        return new ColorStateList(STATES, new int[]{
                ColorUtils.setAlphaComponent(color, MenuPills.DISABLED_ALPHA),
                color
        });
    }
}
