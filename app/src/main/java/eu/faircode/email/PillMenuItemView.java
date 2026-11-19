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
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.AbsListView;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.menu.ListMenuItemView;
import androidx.appcompat.view.menu.MenuItemImpl;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;

// shiroikuma fork: the row of every popup menu. In the Custom theme a menu
// must show which lines can be pressed, so:
// - an action row is an outlined pill spanning the menu, its icon inside at
//   the left and a chevron at the right when it opens something
// - a destructive action takes the warning colour, a disabled one is dimmed
// - a checkable row stays plain and gets an outlined switch, not a checkbox
// - the disabled title line a menu opens with stays plain text
// In every other theme this view behaves exactly like its superclass.
// It is swapped in through the app copies of abc_popup_menu_item_layout and
// abc_cascading_menu_item_layout; icons and flags come from MenuPills.
public class PillMenuItemView extends ListMenuItemView {
    private boolean custom;
    private LinearLayout content;
    private TextView title;
    private ImageView arrow;
    private ColorStateList titleColors;
    private int padStart, padTop, padEnd, padBottom;
    private int contentHeight;

    public PillMenuItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PillMenuItemView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        try {
            content = findViewById(androidx.appcompat.R.id.content);
            title = findViewById(androidx.appcompat.R.id.title);
            arrow = findViewById(androidx.appcompat.R.id.submenuarrow);
            custom = (content != null && title != null &&
                    CustomThemeColors.isCustomTheme(getContext()));
            if (custom) {
                titleColors = title.getTextColors();
                padStart = content.getPaddingStart();
                padTop = content.getPaddingTop();
                padEnd = content.getPaddingEnd();
                padBottom = content.getPaddingBottom();
                contentHeight = content.getLayoutParams().height;

                // The stock group divider is all but invisible on a black menu:
                // a clear line, as wide as the pills
                View divider = findViewById(androidx.appcompat.R.id.group_divider);
                if (divider != null) {
                    divider.setBackgroundColor(MenuPills.divider(getContext()));
                    ViewGroup.MarginLayoutParams dlp = (ViewGroup.MarginLayoutParams) divider.getLayoutParams();
                    dlp.leftMargin = dp(8);
                    dlp.rightMargin = dp(8);
                    divider.setLayoutParams(dlp);
                }
            }
        } catch (Throwable ex) {
            Log.e(ex);
            custom = false;
        }
    }

    @Override
    public void initialize(MenuItemImpl itemData, int menuType) {
        super.initialize(itemData, menuType);
        if (custom)
            try {
                style(itemData);
            } catch (Throwable ex) {
                Log.e(ex);
            }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        // The pill carries the ripple, so the rectangular press highlight
        // of the list would only paint over its round ends
        ViewParent parent = getParent();
        if (custom && parent instanceof AbsListView) {
            AbsListView list = (AbsListView) parent;
            if (!(list.getSelector() instanceof ColorDrawable))
                list.setSelector(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    private void style(MenuItemImpl item) {
        Context context = getContext();
        int id = item.getItemId();
        boolean checkable = item.isCheckable();
        boolean header = MenuPills.isHeader(item);
        boolean pill = (!checkable && !header);
        boolean destructive = (pill && MenuPills.isDestructive(id));

        int color = (destructive ? MenuPills.warning(context) : MenuPills.accent(context));
        if (!item.isEnabled())
            color = ColorUtils.setAlphaComponent(color, MenuPills.DISABLED_ALPHA);

        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) content.getLayoutParams();
        int h = dp(8);
        int v = (pill ? dp(MenuPills.PILL_MARGIN_DP) : 0);
        // A checkable row has nothing to frame, so it is only as tall as its switch needs
        int height = (checkable ? dp(MenuPills.TOGGLE_HEIGHT_DP) : contentHeight);
        if (lp.leftMargin != h || lp.topMargin != v || lp.rightMargin != h || lp.bottomMargin != v ||
                lp.height != height) {
            lp.height = height;
            lp.setMargins(h, v, h, v);
            content.setLayoutParams(lp);
        }

        content.setBackground(pill ? pill(color) : null);
        // The switch of a checkable row ends where the pills end
        content.setPaddingRelative(padStart, padTop, checkable ? 0 : padEnd, padBottom);

        if (destructive)
            title.setTextColor(color);
        else
            title.setTextColor(titleColors);

        if (arrow != null) {
            CharSequence text = item.getTitle();
            boolean ellipsis = (text != null && text.toString().trim().endsWith("\u2026"));
            boolean opens = (pill && (item.hasSubMenu() || ellipsis || MenuPills.opens(id)));
            if (opens) {
                Drawable chevron = ContextCompat.getDrawable(context, R.drawable.sk_menu_chevron);
                if (chevron != null) {
                    chevron = chevron.mutate();
                    chevron.setTint(color);
                    arrow.setImageDrawable(chevron);
                }
            }
            arrow.setVisibility(opens ? View.VISIBLE : View.GONE);
        }

        for (int i = 0; i < content.getChildCount(); i++) {
            View child = content.getChildAt(i);
            if (child instanceof CheckBox) {
                if (child.getTag() == null) {
                    CheckBox cb = (CheckBox) child;
                    cb.setButtonDrawable(new SwitchDrawable(
                            MenuPills.accent(context), MenuPills.switchOff(context),
                            dp(44), dp(24), dp(2)));
                    cb.setBackground(null);
                    // A long label stops short of the switch
                    ViewGroup.MarginLayoutParams clp = (ViewGroup.MarginLayoutParams) cb.getLayoutParams();
                    clp.setMarginStart(dp(8));
                    cb.setLayoutParams(clp);
                    cb.setTag(Boolean.TRUE);
                }
            } else if (child.getId() == androidx.appcompat.R.id.icon) {
                // Clear of the round end of the pill, and not squeezed in a low row
                ViewGroup.MarginLayoutParams ilp = (ViewGroup.MarginLayoutParams) child.getLayoutParams();
                if (ilp.topMargin != dp(2)) {
                    ilp.setMarginStart(dp(14));
                    ilp.setMarginEnd(-dp(6));
                    ilp.topMargin = dp(2);
                    ilp.bottomMargin = dp(2);
                    child.setLayoutParams(ilp);
                }
                // The icon is coloured by what its row does, also when the
                // item brought it itself; frame and label keep the row colour
                if (child instanceof ImageView) {
                    int tint = MenuPills.icon(context, id);
                    if (!item.isEnabled())
                        tint = ColorUtils.setAlphaComponent(tint, MenuPills.DISABLED_ALPHA);
                    ((ImageView) child).setImageTintList(ColorStateList.valueOf(tint));
                }
            }
        }

    }

    private Drawable pill(int color) {
        float radius = dp(100);
        GradientDrawable frame = new GradientDrawable();
        frame.setShape(GradientDrawable.RECTANGLE);
        frame.setCornerRadius(radius);
        frame.setColor(Color.TRANSPARENT);
        frame.setStroke(Math.max(1, Math.round(1.25f * getResources().getDisplayMetrics().density)), color);

        GradientDrawable mask = new GradientDrawable();
        mask.setShape(GradientDrawable.RECTANGLE);
        mask.setCornerRadius(radius);
        mask.setColor(Color.WHITE);

        int ripple = ColorUtils.setAlphaComponent(color, Math.round(Color.alpha(color) * 0.3f));
        return new RippleDrawable(ColorStateList.valueOf(ripple), frame, mask);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    // The outlined switch of the black and yellow theme: a round outline with
    // a solid dot, right and accent when on, left and a darker accent when off
    private static class SwitchDrawable extends Drawable {
        private final int on, off, width, height;
        private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();
        private boolean checked;

        SwitchDrawable(int on, int off, int width, int height, int strokeWidth) {
            this.on = on;
            this.off = off;
            this.width = width;
            this.height = height;
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeWidth(strokeWidth);
            fill.setStyle(Paint.Style.FILL);
        }

        @Override
        public void draw(@NonNull Canvas canvas) {
            Rect b = getBounds();
            int color = (checked ? on : off);
            float inset = stroke.getStrokeWidth() / 2f;
            rect.set(b.left + inset, b.top + inset, b.right - inset, b.bottom - inset);
            stroke.setColor(color);
            canvas.drawRoundRect(rect, rect.height() / 2f, rect.height() / 2f, stroke);
            fill.setColor(color);
            float cx = (checked ? b.right - b.height() / 2f : b.left + b.height() / 2f);
            canvas.drawCircle(cx, b.exactCenterY(), b.height() * 0.23f, fill);
        }

        @Override
        public boolean isStateful() {
            return true;
        }

        @Override
        protected boolean onStateChange(@NonNull int[] state) {
            boolean c = false;
            for (int s : state)
                if (s == android.R.attr.state_checked) {
                    c = true;
                    break;
                }
            if (c == checked)
                return false;
            checked = c;
            invalidateSelf();
            return true;
        }

        @Override
        public int getIntrinsicWidth() {
            return width;
        }

        @Override
        public int getIntrinsicHeight() {
            return height;
        }

        @Override
        public void setAlpha(int alpha) {
        }

        @Override
        public void setColorFilter(@Nullable ColorFilter colorFilter) {
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }
}
