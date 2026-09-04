package eu.faircode.email;

import static android.app.Activity.RESULT_OK;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.RippleDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.preference.PreferenceManager;

import com.flask.colorpicker.ColorPickerView;
import com.flask.colorpicker.builder.ColorPickerClickListener;
import com.flask.colorpicker.builder.ColorPickerDialogBuilder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The 白い熊 UI settings page: kxkb-styled (text-wide underlined section
 * headings separated by 1px hairlines) with a Kōjiki-style export/import of
 * every settable item as the first section, followed by the custom theme
 * colour picker and the custom font picker (both moved here from the bottom
 * of the Display tab).
 */
public class FragmentOptionsUi extends FragmentBase {
    private View view;
    private LinearLayout containerUi;

    private int colorAccent;
    private int colorText;
    private float density;

    // Custom colour picker state
    private LinearLayout colorsSection;
    private final java.util.List<ViewButtonColor> customColorButtons = new ArrayList<>();

    // Custom font picker state
    private String pendingFontRole = null;
    private final androidx.activity.result.ActivityResultLauncher<String[]> fontPicker =
            registerForActivityResult(
                    new androidx.activity.result.contract.ActivityResultContracts.OpenDocument(),
                    new androidx.activity.result.ActivityResultCallback<android.net.Uri>() {
                        @Override
                        public void onActivityResult(android.net.Uri uri) {
                            onFontPicked(uri, pendingFontRole);
                            pendingFontRole = null;
                        }
                    });

    // Export/import state. The format, the category table, and the headless export and
    // import cores all live in StateExport — this page and StateExportReceiver are two
    // thin callers of the same code.
    private static final int WARN_COLOR = 0xFFFF5252;

    private static final int REQUEST_EXIM_DIR = 1;
    private static final int REQUEST_EXIM_EXPORT = 2;
    private static final int REQUEST_EXIM_IMPORT = 3;

    /**
     * True while an import is writing preferences in bulk. ActivityBase checks this
     * to suppress its per-key finish/relaunch reaction — otherwise importing dozens
     * of theme/colour/font keys tears the settings activity down mid-flow. Changes
     * take effect on the next (re)start, which the result dialog offers.
     */
    static volatile boolean eximportApplying = false;

    private TextView tvEximStatus;
    private TextView tvAutomationToken;
    private View vAutomationToken;
    private AlertDialog eximDialog;
    private TextView dlgDirValue;
    private TextView dlgStatus;
    private final Map<String, CheckBox> eximChecks = new LinkedHashMap<>();
    private ArrayList<String> pendingExportCats = null;

    @Override
    @Nullable
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        setSubtitle(R.string.title_setup);

        view = inflater.inflate(R.layout.fragment_options_ui, container, false);
        containerUi = view.findViewById(R.id.containerUi);

        Context context = getContext();
        density = context.getResources().getDisplayMetrics().density;
        colorAccent = Helper.resolveColor(context, androidx.appcompat.R.attr.colorAccent);
        colorText = Helper.resolveColor(context, android.R.attr.textColorPrimary);

        buildPage();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        Context context = getContext();
        if (context == null)
            return;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        // Custom theme colour pickers — visible only when theme=custom
        boolean isCustomTheme = "custom".equals(prefs.getString("theme", null));
        if (colorsSection != null)
            colorsSection.setVisibility(isCustomTheme ? View.VISIBLE : View.GONE);
        for (int i = 0; i < customColorButtons.size() && i < CustomThemeColors.ENTRIES.length; i++)
            customColorButtons.get(i).setColor(
                    CustomThemeColors.getEffectiveColor(context, CustomThemeColors.ENTRIES[i]));

        refreshEximStatus();
    }

    private int dp(float value) {
        return Math.max(1, Math.round(value * density));
    }

    // --- kxkb-style building blocks ---

    /** A 1px full-width accent hairline (the between-sections separator). */
    private View hairline(Context context) {
        View line = new View(context);
        line.setBackgroundColor(colorAccent);
        line.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 1));
        return line;
    }

    /**
     * kxkb-style section header: an optional full-width 1px hairline above, then a
     * 20sp bold accent heading with an underline exactly as wide as the text (the
     * wrap_content inner layout shrinks to the text; the underline matches it).
     */
    private void addSectionHeader(LinearLayout parent, int titleRes, boolean first) {
        Context context = parent.getContext();

        LinearLayout block = new LinearLayout(context);
        block.setOrientation(LinearLayout.VERTICAL);
        block.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        block.setPadding(0, dp(first ? 12 : 10), 0, dp(2));

        if (!first)
            block.addView(hairline(context));

        LinearLayout inner = new LinearLayout(context);
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        inner.setPadding(dp(36), first ? 0 : dp(8), 0, 0);

        TextView title = new TextView(context);
        title.setText(titleRes);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(colorAccent);
        title.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        inner.addView(title);

        View underline = new View(context);
        underline.setBackgroundColor(colorAccent);
        LinearLayout.LayoutParams ulp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(2.5f));
        ulp.topMargin = dp(2);
        underline.setLayoutParams(ulp);
        inner.addView(underline);

        block.addView(inner);
        parent.addView(block);
    }

    /** kxkb-style sub-heading: 17sp bold accent, deeper indent, thinner underline. */
    private void addSubHeader(LinearLayout parent, int titleRes) {
        Context context = parent.getContext();

        LinearLayout block = new LinearLayout(context);
        block.setOrientation(LinearLayout.VERTICAL);
        block.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        block.setPadding(dp(54), dp(10), 0, dp(2));

        LinearLayout inner = new LinearLayout(context);
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView title = new TextView(context);
        title.setText(titleRes);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(colorAccent);
        title.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        inner.addView(title);

        View underline = new View(context);
        underline.setBackgroundColor(colorAccent);
        LinearLayout.LayoutParams ulp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(1.5f));
        ulp.topMargin = dp(2);
        underline.setLayoutParams(ulp);
        inner.addView(underline);

        block.addView(inner);
        parent.addView(block);
    }

    /** Small italic remark line under a section header. */
    private void addRemark(LinearLayout parent, int textRes) {
        Context context = parent.getContext();
        TextView remark = new TextView(context);
        remark.setText(textRes);
        remark.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        remark.setTypeface(remark.getTypeface(), Typeface.ITALIC);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = dp(54);
        lp.rightMargin = dp(16);
        lp.topMargin = dp(4);
        remark.setLayoutParams(lp);
        parent.addView(remark);
    }

    private LinearLayout.LayoutParams rowParams(int width, int topDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                width, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = dp(72);
        lp.rightMargin = dp(16);
        lp.topMargin = dp(topDp);
        return lp;
    }

    // --- page assembly ---

    private void buildPage() {
        Context context = getContext();
        if (context == null)
            return;

        containerUi.removeAllViews();
        customColorButtons.clear();

        // --- Export / import (first section, Kōjiki-style) ---
        addSectionHeader(containerUi, R.string.title_ui_eim_row, true);

        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        row.setPadding(dp(72), dp(14), dp(16), dp(14));
        TypedValue tv = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, tv, true);
        row.setBackgroundResource(tv.resourceId);
        row.setClickable(true);

        TextView rowTitle = new TextView(context);
        rowTitle.setText(R.string.title_ui_eim_row);
        rowTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        rowTitle.setTextColor(colorText);
        row.addView(rowTitle);

        tvEximStatus = new TextView(context);
        tvEximStatus.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        LinearLayout.LayoutParams vlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        vlp.topMargin = dp(3);
        tvEximStatus.setLayoutParams(vlp);
        row.addView(tvEximStatus);

        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openEximDialog();
            }
        });
        containerUi.addView(row);

        // The automation controls belong to the Export/Import section itself — this is a
        // backup feature, so it sits where backup lives, the same in every sister app.
        addAutomationRows(containerUi);

        // --- Custom theme colours (hidden unless theme=custom) ---
        colorsSection = new LinearLayout(context);
        colorsSection.setOrientation(LinearLayout.VERTICAL);
        colorsSection.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        containerUi.addView(colorsSection);

        addSectionHeader(colorsSection, R.string.title_custom_colors_section, false);
        addRemark(colorsSection, R.string.title_custom_colors_remark);
        populateCustomColorPicker(colorsSection);

        // --- Custom font ---
        addSectionHeader(containerUi, R.string.title_custom_font_section, false);
        addRemark(containerUi, R.string.title_custom_font_remark);
        populateCustomFontPicker(containerUi);
    }

    /**
     * The three automation rows, appended directly below the Export / import row: a master
     * switch (default ON), a 「Use authorization token?」 switch (default OFF), and the token
     * row — shown only when the token is being asked for, because a 48-character secret
     * sitting under an off switch invites 白い熊 to paste it somewhere it will do nothing.
     * The token row copies the full token on tap and carries Regenerate on the right.
     *
     * <p>This is the shared 白い熊 automation surface. {@link StateExportReceiver} answers a
     * sister-app task whenever the switch is on, so 自由作業盤's 保存復元 backs this app up in
     * the same run as every other one, and {@link AutomationProvider} lets 応用管理 take this
     * app's data with it — that one identifies its caller by name, uid and signing
     * certificate whatever these switches say.
     *
     * <p>The switch ships ON deliberately: the case this exists for is a phone that has just
     * been wiped, where nothing has been configured and nobody has pasted anything. It stays
     * a switch because closing one app off has to remain possible.
     */
    private void addAutomationRows(LinearLayout parent) {
        final Context context = parent.getContext();

        final SwitchCompat swAutomation = new SwitchCompat(context);
        swAutomation.setText(R.string.title_ui_automation_switch);
        swAutomation.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        swAutomation.setTextColor(colorText);
        swAutomation.setLayoutParams(rowParams(ViewGroup.LayoutParams.MATCH_PARENT, 12));
        swAutomation.setChecked(AutomationAuth.enabled(context));
        parent.addView(swAutomation);

        TextView swDesc = new TextView(context);
        swDesc.setText(R.string.title_ui_automation_switch_desc);
        swDesc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        swDesc.setTypeface(swDesc.getTypeface(), Typeface.ITALIC);
        swDesc.setAlpha(0.85f);
        swDesc.setLayoutParams(rowParams(ViewGroup.LayoutParams.MATCH_PARENT, 2));
        parent.addView(swDesc);

        swAutomation.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // The automation preferences live in their own file, so writing them here
                // never trips the settings activity's default-prefs restart listener.
                AutomationAuth.setEnabled(context, isChecked);
                if (isChecked)
                    checkAllFilesAccess();
            }
        });

        final SwitchCompat swRequireToken = new SwitchCompat(context);
        swRequireToken.setText(R.string.title_ui_automation_require_token);
        swRequireToken.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        swRequireToken.setTextColor(colorText);
        swRequireToken.setLayoutParams(rowParams(ViewGroup.LayoutParams.MATCH_PARENT, 12));
        swRequireToken.setChecked(AutomationAuth.requireToken(context));
        parent.addView(swRequireToken);

        TextView tokenDesc = new TextView(context);
        tokenDesc.setText(R.string.title_ui_automation_require_token_desc);
        tokenDesc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tokenDesc.setTypeface(tokenDesc.getTypeface(), Typeface.ITALIC);
        tokenDesc.setAlpha(0.85f);
        tokenDesc.setLayoutParams(rowParams(ViewGroup.LayoutParams.MATCH_PARENT, 2));
        parent.addView(tokenDesc);

        swRequireToken.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                AutomationAuth.setRequireToken(context, isChecked);
                if (vAutomationToken != null)
                    vAutomationToken.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            }
        });

        // The token row: tap to copy, Regenerate on the right
        LinearLayout tokenRow = new LinearLayout(context);
        tokenRow.setOrientation(LinearLayout.HORIZONTAL);
        tokenRow.setGravity(Gravity.CENTER_VERTICAL);
        tokenRow.setLayoutParams(rowParams(ViewGroup.LayoutParams.MATCH_PARENT, 14));

        LinearLayout tokenText = new LinearLayout(context);
        tokenText.setOrientation(LinearLayout.VERTICAL);
        tokenText.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        tokenText.setPadding(0, dp(6), dp(8), dp(6));
        TypedValue tv = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, tv, true);
        tokenText.setBackgroundResource(tv.resourceId);
        tokenText.setClickable(true);

        TextView tokenCaption = new TextView(context);
        tokenCaption.setText(R.string.title_ui_automation_token);
        tokenCaption.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        tokenCaption.setTextColor(colorText);
        tokenText.addView(tokenCaption);

        tvAutomationToken = new TextView(context);
        tvAutomationToken.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tvAutomationToken.setTypeface(Typeface.MONOSPACE);
        tvAutomationToken.setTextColor(colorAccent);
        tvAutomationToken.setText(AutomationAuth.abbreviate(AutomationAuth.token(context)));
        tokenText.addView(tvAutomationToken);

        tokenText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copyAutomationToken();
            }
        });
        tokenRow.addView(tokenText);

        Button btnRegenerate = new Button(context, null, android.R.attr.borderlessButtonStyle);
        btnRegenerate.setText(R.string.title_ui_automation_regenerate);
        btnRegenerate.setAllCaps(false);
        btnRegenerate.setTextColor(colorAccent);
        btnRegenerate.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        GradientDrawable pill = new GradientDrawable();
        pill.setColor(0xFF000000);
        pill.setCornerRadius(50 * density);
        pill.setStroke(dp(1.5f), colorAccent);
        btnRegenerate.setBackground(new RippleDrawable(
                ColorStateList.valueOf((colorAccent & 0x00FFFFFF) | 0x33000000), pill, null));
        btnRegenerate.setPadding(dp(16), dp(4), dp(16), dp(4));
        btnRegenerate.setMinWidth(0);
        btnRegenerate.setMinimumWidth(0);
        btnRegenerate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmRegenerateToken();
            }
        });
        tokenRow.addView(btnRegenerate);

        // Hidden until the token is actually being asked for - see the method comment.
        vAutomationToken = tokenRow;
        tokenRow.setVisibility(AutomationAuth.requireToken(context) ? View.VISIBLE : View.GONE);
        parent.addView(tokenRow);
    }

    private void copyAutomationToken() {
        Context context = getContext();
        if (context == null)
            return;
        try {
            ClipboardManager cbm = Helper.getSystemService(context, ClipboardManager.class);
            cbm.setPrimaryClip(ClipData.newPlainText(
                    context.getString(R.string.title_ui_automation_token),
                    AutomationAuth.token(context)));
            ToastEx.makeText(context, R.string.title_ui_automation_copied, Toast.LENGTH_LONG).show();
        } catch (Throwable ex) {
            Log.unexpectedError(getParentFragmentManager(), ex);
        }
    }

    private void confirmRegenerateToken() {
        final Context context = getContext();
        if (context == null)
            return;
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(R.string.title_ui_automation_regenerate)
                .setMessage(R.string.title_ui_automation_regenerate_warn)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int which) {
                        String token = AutomationAuth.regenerateToken(context);
                        if (tvAutomationToken != null)
                            tvAutomationToken.setText(AutomationAuth.abbreviate(token));
                        copyAutomationToken();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
        styleEximDialog(dialog);
    }

    /**
     * Writing the backup to the directory the sister-app task names needs All files
     * access; without it the headless export can only use the SAF directory above.
     */
    private void checkAllFilesAccess() {
        final Context context = getContext();
        if (context == null)
            return;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R || Environment.isExternalStorageManager())
            return;

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(R.string.title_ui_automation_allfiles_title)
                .setMessage(R.string.title_ui_automation_allfiles_body)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int which) {
                        try {
                            Intent intent = new Intent(
                                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                                    Uri.parse("package:" + BuildConfig.APPLICATION_ID));
                            startActivity(intent);
                        } catch (Throwable ex) {
                            Log.w(ex);
                            try {
                                startActivity(new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION));
                            } catch (Throwable ignored) {
                                Log.unexpectedError(getParentFragmentManager(), ex);
                            }
                        }
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
        styleEximDialog(dialog);
    }

    /**
     * Populate the custom-colour picker by iterating {@link CustomThemeColors#ENTRIES}.
     * Adjacent entries with the same section render under one kxkb-style sub-heading.
     * Each entry renders as a swatch button plus an italic description.
     */
    private void populateCustomColorPicker(LinearLayout parent) {
        Context context = parent.getContext();

        String currentSection = null;
        for (CustomThemeColors.Entry entry : CustomThemeColors.ENTRIES) {
            if (!entry.section.equals(currentSection)) {
                currentSection = entry.section;
                addSubHeader(parent, entry.sectionLabelRes);
            }

            // Swatch — uses the small-button style via the 3-arg constructor with defStyleAttr
            ViewButtonColor button = new ViewButtonColor(context, null, android.R.attr.buttonStyleSmall);
            button.setText(entry.labelRes);
            button.setPadding(dp(6), button.getPaddingTop(), dp(6), button.getPaddingBottom());
            button.setLayoutParams(rowParams(ViewGroup.LayoutParams.WRAP_CONTENT, 6));
            parent.addView(button);
            wireCustomColorButton(button, entry);
            customColorButtons.add(button);

            if (entry.descriptionRes != 0) {
                TextView desc = new TextView(context);
                desc.setText(entry.descriptionRes);
                desc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                desc.setTypeface(desc.getTypeface(), Typeface.ITALIC);
                desc.setLayoutParams(rowParams(ViewGroup.LayoutParams.MATCH_PARENT, 4));
                parent.addView(desc);
            }
        }
    }

    /**
     * Wire one ViewButtonColor to its colour picker dialog.
     * Tap opens the picker; long-press resets the colour to default.
     */
    private void wireCustomColorButton(final ViewButtonColor button, final CustomThemeColors.Entry entry) {
        final Context context = getContext();
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        button.setColor(CustomThemeColors.getEffectiveColor(context, entry));

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int initialColor = CustomThemeColors.getEffectiveColor(context, entry);
                int editTextColor = Helper.resolveColor(context, android.R.attr.editTextColor);
                ColorPickerDialogBuilder
                        .with(context)
                        .setTitle(entry.labelRes)
                        .initialColor(initialColor)
                        .showColorEdit(true)
                        .setColorEditTextColor(editTextColor)
                        .wheelType(ColorPickerView.WHEEL_TYPE.FLOWER)
                        .density(6)
                        .lightnessSliderOnly()
                        .setPositiveButton(android.R.string.ok, new ColorPickerClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int selectedColor, Integer[] allColors) {
                                prefs.edit().putInt(entry.prefKey, selectedColor).apply();
                                button.setColor(selectedColor);
                            }
                        })
                        .setNegativeButton(R.string.title_reset, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                prefs.edit().remove(entry.prefKey).apply();
                                button.setColor(ContextCompat.getColor(context, entry.colorRes));
                            }
                        })
                        .build()
                        .show();
            }
        });

        button.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                prefs.edit().remove(entry.prefKey).apply();
                button.setColor(ContextCompat.getColor(context, entry.colorRes));
                return true;
            }
        });
    }

    /**
     * Build the custom-font picker rows from {@link CustomFont#ENTRIES}: kxkb-style
     * sub-heading per section, then per role a label, [Pick | name | Reset] row,
     * weight label + slider, and an italic description.
     */
    private void populateCustomFontPicker(LinearLayout parent) {
        final Context context = parent.getContext();
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        String previousSection = null;
        for (final CustomFont.Entry entry : CustomFont.ENTRIES) {
            if (!entry.section.equals(previousSection)) {
                previousSection = entry.section;
                addSubHeader(parent, entry.sectionLabelRes);
            }

            // Role label
            TextView label = new TextView(context);
            label.setText(entry.labelRes);
            label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            label.setLayoutParams(rowParams(ViewGroup.LayoutParams.MATCH_PARENT, 12));
            parent.addView(label);

            // Pick button + Name TextView + Reset button row
            LinearLayout pickRow = new LinearLayout(context);
            pickRow.setOrientation(LinearLayout.HORIZONTAL);
            pickRow.setGravity(Gravity.CENTER_VERTICAL);
            pickRow.setLayoutParams(rowParams(ViewGroup.LayoutParams.MATCH_PARENT, 4));
            parent.addView(pickRow);

            Button pickBtn = new Button(context);
            pickBtn.setText(R.string.title_custom_font_pick);
            pickRow.addView(pickBtn);

            String currentName = prefs.getString(CustomFont.prefName(entry.role), null);
            TextView nameTv = new TextView(context);
            nameTv.setText(TextUtils.isEmpty(currentName)
                    ? getString(R.string.title_custom_font_none)
                    : currentName);
            nameTv.setEllipsize(TextUtils.TruncateAt.MIDDLE);
            nameTv.setSingleLine(true);
            LinearLayout.LayoutParams nameLp = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            nameLp.leftMargin = dp(12);
            nameLp.rightMargin = dp(8);
            nameTv.setLayoutParams(nameLp);
            pickRow.addView(nameTv);

            Button resetBtn = new Button(
                    new android.view.ContextThemeWrapper(context,
                            androidx.appcompat.R.style.Widget_AppCompat_Button_Borderless),
                    null, 0);
            resetBtn.setText(R.string.title_reset);
            pickRow.addView(resetBtn);

            // Weight label
            final TextView weightLabel = new TextView(context);
            weightLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            weightLabel.setLayoutParams(rowParams(ViewGroup.LayoutParams.MATCH_PARENT, 8));
            parent.addView(weightLabel);

            // Weight slider
            android.widget.SeekBar weightSlider = new android.widget.SeekBar(context);
            weightSlider.setMax(9);
            int currentWeight = prefs.getInt(CustomFont.prefWeight(entry.role), 0);
            int sliderPos = (currentWeight <= 0
                    ? 0
                    : Math.min(9, Math.max(1, currentWeight / 100)));
            weightSlider.setProgress(sliderPos);
            weightLabel.setText(weightLabelText(context, sliderPos));
            weightSlider.setLayoutParams(rowParams(ViewGroup.LayoutParams.MATCH_PARENT, 0));
            parent.addView(weightSlider);

            // Description
            if (entry.descriptionRes != 0) {
                TextView desc = new TextView(context);
                desc.setText(entry.descriptionRes);
                desc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                desc.setTypeface(desc.getTypeface(), Typeface.ITALIC);
                desc.setLayoutParams(rowParams(ViewGroup.LayoutParams.MATCH_PARENT, 4));
                parent.addView(desc);
            }

            // Listeners
            pickBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        pendingFontRole = entry.role;
                        fontPicker.launch(new String[]{"*/*"});
                    } catch (Throwable ex) {
                        Log.unexpectedError(getParentFragmentManager(), ex);
                    }
                }
            });

            resetBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    CustomFont.clearStoredFile(context, entry.role);
                    prefs.edit()
                            .remove(CustomFont.prefPath(entry.role))
                            .remove(CustomFont.prefName(entry.role))
                            .apply();
                }
            });

            weightSlider.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(android.widget.SeekBar seekBar, int progress, boolean fromUser) {
                    weightLabel.setText(weightLabelText(context, progress));
                }

                @Override
                public void onStartTrackingTouch(android.widget.SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(android.widget.SeekBar seekBar) {
                    int weight = (seekBar.getProgress() == 0 ? 0 : seekBar.getProgress() * 100);
                    if (prefs.getInt(CustomFont.prefWeight(entry.role), 0) == weight)
                        return; // no-op after drag-back-to-same
                    prefs.edit().putInt(CustomFont.prefWeight(entry.role), weight).apply();
                }
            });
        }
    }

    private CharSequence weightLabelText(Context context, int sliderPos) {
        if (sliderPos == 0)
            return context.getString(R.string.title_custom_font_weight_natural);
        return context.getString(R.string.title_custom_font_weight_label, sliderPos * 100);
    }

    /**
     * Called by the {@link #fontPicker} ActivityResultLauncher when the user picks
     * a font file for {@code role}. Copies the bytes into internal storage and saves
     * the resulting path plus display name to that role's prefs; ActivityBase's pref
     * listener then triggers a recreate so the new typeface takes effect immediately.
     *
     * The pref save is deferred via Handler.post because this callback fires inside
     * super.onResume before ActivityBase sets visible=true, and the listener only
     * relaunches when visible is true. Without the defer, the activity finishes
     * itself without relaunching and the app appears to vanish silently.
     */
    private void onFontPicked(@Nullable android.net.Uri uri, @Nullable final String role) {
        if (uri == null || role == null)
            return; // user cancelled or no pending role
        final Context context = getContext();
        if (context == null)
            return;
        try {
            String name = CustomFont.getDisplayName(context, uri);
            if (TextUtils.isEmpty(name))
                name = uri.getLastPathSegment();
            if (TextUtils.isEmpty(name))
                name = "font.ttf";
            final String fname = name;
            final String fpath = CustomFont.copyToInternal(context, uri, role);
            android.graphics.Typeface probe = android.graphics.Typeface.createFromFile(new java.io.File(fpath));
            if (probe == null)
                throw new java.io.IOException("Typeface.createFromFile returned null");
            final Context appContext = context.getApplicationContext();
            new android.os.Handler(android.os.Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    PreferenceManager.getDefaultSharedPreferences(appContext).edit()
                            .putString(CustomFont.prefPath(role), fpath)
                            .putString(CustomFont.prefName(role), fname)
                            .apply();
                }
            });
        } catch (Throwable ex) {
            Log.w(ex);
            CustomFont.clearStoredFile(context, role);
            android.widget.Toast.makeText(context,
                    getString(R.string.title_custom_font_error, ex.getMessage() == null ? "?" : ex.getMessage()),
                    android.widget.Toast.LENGTH_LONG).show();
        }
    }

    // --- export/import: directory + status ---

    @Nullable
    private Uri dirUri() {
        return StateExport.dirUri(getContext());
    }

    @Nullable
    private DocumentFile exportDir() {
        return StateExport.exportDir(getContext());
    }

    /**
     * Query the export directory for the newest export (by last-modified) and show
     * the result on the page row and, when the panel is open, in the panel too.
     */
    private void refreshEximStatus() {
        Context context = getContext();
        if (context == null)
            return;

        String msg;
        boolean warn;
        DocumentFile dir = exportDir();
        if (dir == null) {
            msg = getString(R.string.title_ui_eim_warn_nodir);
            warn = true;
        } else {
            DocumentFile newest = null;
            try {
                for (DocumentFile file : dir.listFiles()) {
                    if (!file.isFile() || !StateExport.isExportName(file.getName()))
                        continue;
                    if (newest == null || file.lastModified() > newest.lastModified())
                        newest = file;
                }
            } catch (Throwable ex) {
                Log.w(ex);
            }
            if (newest == null) {
                msg = getString(R.string.title_ui_eim_warn_none);
                warn = true;
            } else {
                msg = getString(R.string.title_ui_eim_last,
                        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT)
                                .format(new Date(newest.lastModified())));
                warn = false;
            }
        }

        if (tvEximStatus != null) {
            tvEximStatus.setText(msg);
            tvEximStatus.setTextColor(warn ? WARN_COLOR : colorText);
            tvEximStatus.setAlpha(warn ? 1f : 0.7f);
        }

        if (dlgStatus != null) {
            dlgStatus.setText(msg);
            dlgStatus.setTextColor(warn ? WARN_COLOR : colorText);
            dlgStatus.setAlpha(warn ? 1f : 0.8f);
        }

        if (dlgDirValue != null) {
            String name = (dir != null ? dir.getName() : null);
            if (name == null && dirUri() != null)
                name = dirUri().getLastPathSegment();
            dlgDirValue.setText(name != null ? name : getString(R.string.title_ui_eim_dir_unset));
            dlgDirValue.setTextColor(name != null ? colorText : WARN_COLOR);
        }
    }

    // --- export/import: the panel dialog ---

    private void openEximDialog() {
        final Context context = getContext();
        if (context == null || eximDialog != null)
            return;

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(16), dp(20), dp(8));

        TextView title = new TextView(context);
        title.setText(R.string.title_ui_eim_title);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(colorAccent);
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(title);

        TextView desc = new TextView(context);
        desc.setText(R.string.title_ui_eim_desc);
        desc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        desc.setAlpha(0.85f);
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dlp.topMargin = dp(6);
        desc.setLayoutParams(dlp);
        root.addView(desc);

        // Directory box — bordered, tappable
        LinearLayout dirBox = new LinearLayout(context);
        dirBox.setOrientation(LinearLayout.VERTICAL);
        dirBox.setPadding(dp(12), dp(12), dp(12), dp(12));
        GradientDrawable border = new GradientDrawable();
        border.setCornerRadius(8 * density);
        border.setStroke(dp(1.5f), colorAccent);
        dirBox.setBackground(border);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        blp.topMargin = dp(12);
        dirBox.setLayoutParams(blp);

        TextView dirCaption = new TextView(context);
        dirCaption.setText(R.string.title_ui_eim_dir);
        dirCaption.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        dirCaption.setTextColor(colorAccent);
        dirBox.addView(dirCaption);

        dlgDirValue = new TextView(context);
        dlgDirValue.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        dlgDirValue.setTypeface(Typeface.DEFAULT_BOLD);
        dirBox.addView(dlgDirValue);

        dirBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                    Uri current = dirUri();
                    if (current != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, current);
                    startActivityForResult(intent, REQUEST_EXIM_DIR);
                } catch (Throwable ex) {
                    Log.unexpectedError(getParentFragmentManager(), ex);
                }
            }
        });
        root.addView(dirBox);

        dlgStatus = new TextView(context);
        dlgStatus.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        slp.topMargin = dp(8);
        dlgStatus.setLayoutParams(slp);
        root.addView(dlgStatus);

        View divider = new View(context);
        divider.setBackgroundColor(colorAccent);
        divider.setAlpha(0.4f);
        LinearLayout.LayoutParams dvlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
        dvlp.topMargin = dp(12);
        divider.setLayoutParams(dvlp);
        root.addView(divider);

        // Select all + one checkbox per category, ticked except the opt-in heavy ones
        eximChecks.clear();
        CheckBox selectAll = new CheckBox(context);
        selectAll.setText(R.string.title_ui_eim_select_all);
        selectAll.setTypeface(Typeface.DEFAULT_BOLD);
        // Unticked to start with: the local mail store is opt-in, so not everything is
        // selected. Tapping it is still the plain tick-everything / untick-everything toggle.
        selectAll.setChecked(false);
        LinearLayout.LayoutParams salp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        salp.topMargin = dp(8);
        selectAll.setLayoutParams(salp);
        root.addView(selectAll);

        for (int i = 0; i < StateExport.CAT_IDS.length; i++) {
            CheckBox cb = new CheckBox(context);
            cb.setText(StateExport.CAT_LABELS[i]);
            cb.setChecked(!StateExport.isDefaultOff(StateExport.CAT_IDS[i]));
            root.addView(cb);
            eximChecks.put(StateExport.CAT_IDS[i], cb);
        }

        selectAll.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                for (CheckBox cb : eximChecks.values())
                    cb.setChecked(isChecked);
            }
        });

        ScrollView scroll = new ScrollView(context);
        scroll.addView(root);

        eximDialog = new AlertDialog.Builder(context)
                .setView(scroll)
                .setPositiveButton(R.string.title_ui_eim_export, null)
                .setNegativeButton(R.string.title_ui_eim_import, null)
                .setNeutralButton(android.R.string.cancel, null)
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        eximDialog = null;
                        dlgDirValue = null;
                        dlgStatus = null;
                        eximChecks.clear();
                    }
                })
                .show();

        styleEximDialog(eximDialog);

        // Wire the buttons AFTER show() so a tap does not auto-dismiss the panel:
        // failures must leave it open; success closes the whole chain itself.
        eximDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onEximExport();
                    }
                });
        eximDialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onEximImport();
                    }
                });

        refreshEximStatus();
    }

    /**
     * ArcaneChat-style restyle: black rounded dialog window with an accent border,
     * and every button repainted as a round pill (black fill, thin accent stroke).
     * Call after show() — the buttons only exist then.
     */
    private void styleEximDialog(AlertDialog dialog) {
        if (dialog.getWindow() != null) {
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(0xFF000000);
            bg.setCornerRadius(8 * density);
            bg.setStroke(dp(2), colorAccent);
            dialog.getWindow().setBackgroundDrawable(new InsetDrawable(bg, dp(16)));
        }

        int[] which = {
                AlertDialog.BUTTON_POSITIVE, AlertDialog.BUTTON_NEGATIVE, AlertDialog.BUTTON_NEUTRAL
        };
        for (int w : which) {
            Button b = dialog.getButton(w);
            if (b == null || b.getVisibility() != View.VISIBLE)
                continue;
            GradientDrawable pill = new GradientDrawable();
            pill.setColor(0xFF000000);
            pill.setCornerRadius(50 * density);
            pill.setStroke(dp(1.5f), colorAccent);
            RippleDrawable ripple = new RippleDrawable(
                    ColorStateList.valueOf((colorAccent & 0x00FFFFFF) | 0x33000000), pill, null);
            b.setBackground(ripple);
            b.setTextColor(colorAccent);
            b.setPadding(dp(20), dp(6), dp(20), dp(6));
            ViewGroup.LayoutParams lp = b.getLayoutParams();
            if (lp instanceof ViewGroup.MarginLayoutParams) {
                ((ViewGroup.MarginLayoutParams) lp).setMarginStart(dp(8));
                b.setLayoutParams(lp);
            }
        }
    }

    private ArrayList<String> selectedCats() {
        ArrayList<String> cats = new ArrayList<>();
        for (Map.Entry<String, CheckBox> entry : eximChecks.entrySet())
            if (entry.getValue().isChecked())
                cats.add(entry.getKey());
        return cats;
    }

    // --- info dialogs (yellow border) + chain closing ---

    private void closeEximChain() {
        if (eximDialog != null)
            eximDialog.dismiss();
        if (getActivity() != null)
            getActivity().finish();
    }

    /** Failure info dialog: acknowledging it leaves the export/import panel open. */
    private void showEximInfo(String message) {
        Context context = getContext();
        if (context == null)
            return;
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .show();
        styleEximDialog(dialog);
    }

    /** Export success: OK closes the info dialog, the panel, and the UI settings page. */
    private void showExportDone(String name) {
        Context context = getContext();
        if (context == null)
            return;
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(R.string.title_ui_eim_export_done_title)
                .setMessage(getString(R.string.title_ui_eim_export_ok, name))
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int which) {
                        closeEximChain();
                    }
                })
                .show();
        styleEximDialog(dialog);
    }

    /**
     * Import success: "Restart now" restarts the app; "Later" closes the info
     * dialog, the panel, and the UI settings page (changes apply on next start).
     */
    private void showImportDone(String summary) {
        final Context context = getContext();
        if (context == null)
            return;
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(R.string.title_ui_eim_import_done_title)
                .setMessage(getString(R.string.title_ui_eim_import_done_body, summary))
                .setCancelable(false)
                .setPositiveButton(R.string.title_ui_eim_restart_now, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int which) {
                        ApplicationEx.restart(context, "ui eximport");
                    }
                })
                .setNegativeButton(R.string.title_ui_eim_restart_later, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int which) {
                        closeEximChain();
                    }
                })
                .show();
        styleEximDialog(dialog);
    }

    // --- SAF results ---

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        try {
            switch (requestCode) {
                case REQUEST_EXIM_DIR:
                    if (resultCode == RESULT_OK && data != null && data.getData() != null)
                        onDirPicked(data.getData());
                    break;
                case REQUEST_EXIM_EXPORT:
                    if (resultCode == RESULT_OK && data != null && data.getData() != null &&
                            pendingExportCats != null)
                        runExport(pendingExportCats, data.getData());
                    pendingExportCats = null;
                    break;
                case REQUEST_EXIM_IMPORT:
                    if (resultCode == RESULT_OK && data != null && data.getData() != null)
                        runImport(selectedCats(), data.getData());
                    break;
            }
        } catch (Throwable ex) {
            Log.e(ex);
        }
    }

    private void onDirPicked(Uri uri) {
        try {
            getContext().getContentResolver().takePersistableUriPermission(uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        } catch (Throwable ex) {
            Log.w(ex);
        }
        StateExport.eximPrefs(getContext()).edit()
                .putString(StateExport.KEY_DIR_URI, uri.toString()).apply();
        refreshEximStatus();
    }

    // --- export ---

    private void onEximExport() {
        ArrayList<String> cats = selectedCats();
        if (cats.isEmpty()) {
            showEximInfo(getString(R.string.title_ui_eim_none_selected));
            return;
        }

        if (exportDir() != null)
            runExport(cats, null);
        else {
            // No directory chosen yet: fall back to a save-as picker
            pendingExportCats = cats;
            try {
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType(StateExport.EXPORT_MIME);
                intent.putExtra(Intent.EXTRA_TITLE, StateExport.exportFileName());
                startActivityForResult(intent, REQUEST_EXIM_EXPORT);
            } catch (Throwable ex) {
                pendingExportCats = null;
                Log.unexpectedError(getParentFragmentManager(), ex);
            }
        }
    }

    private void runExport(ArrayList<String> cats, @Nullable Uri target) {
        Bundle args = new Bundle();
        args.putStringArrayList("cats", cats);
        args.putParcelable("uri", target);

        new SimpleTask<String>() {
            private Toast toast = null;

            @Override
            protected void onPreExecute(Bundle args) {
                toast = ToastEx.makeText(getContext(), R.string.title_executing, Toast.LENGTH_LONG);
                toast.show();
            }

            @Override
            protected void onPostExecute(Bundle args) {
                if (toast != null)
                    toast.cancel();
            }

            @Override
            protected String onExecute(Context context, Bundle args) throws Throwable {
                List<String> cats = args.getStringArrayList("cats");
                Uri uri = args.getParcelable("uri");

                String name;
                if (uri == null) {
                    // Into the configured directory: written as a part file and renamed only
                    // when the archive is complete, so a failure leaves nothing that looks
                    // like a backup (see StateExport.exportToDir)
                    DocumentFile dir = StateExport.exportDir(context);
                    if (dir == null)
                        throw new FileNotFoundException("Export directory not accessible");
                    name = StateExport.exportFileName();
                    DocumentFile file = StateExport.exportToDir(context, cats, dir, name, null, null);
                    if (file.getName() != null)
                        name = file.getName();
                    EntityLog.log(context, "UI export done uri=" + file.getUri());
                    return name;
                }

                // Save-as: the picker created the file before we ever saw it, so the most we
                // can do is take it away again when the export does not finish
                DocumentFile file = DocumentFile.fromSingleUri(context, uri);
                name = (file == null || file.getName() == null ? uri.toString() : file.getName());

                ContentResolver resolver = context.getContentResolver();
                boolean complete = false;
                try {
                    try (OutputStream raw = resolver.openOutputStream(uri)) {
                        if (raw == null)
                            throw new FileNotFoundException(uri.toString());
                        StateExport.export(context, cats, raw, null, null);
                    }
                    complete = true;
                } finally {
                    if (!complete)
                        try {
                            DocumentsContract.deleteDocument(resolver, uri);
                        } catch (Throwable ex) {
                            Log.w(ex);
                        }
                }

                EntityLog.log(context, "UI export done uri=" + uri);
                return name;
            }

            @Override
            protected void onExecuted(Bundle args, String name) {
                refreshEximStatus();
                showExportDone(name);
            }

            @Override
            protected void onException(Bundle args, Throwable ex) {
                Log.w(ex);
                showEximInfo(getString(R.string.title_ui_eim_export_fail,
                        ex.getMessage() == null ? ex.toString() : ex.getMessage()));
            }
        }.execute(this, args, "ui:export");
    }


    // --- import ---

    private void onEximImport() {
        ArrayList<String> cats = selectedCats();
        if (cats.isEmpty()) {
            showEximInfo(getString(R.string.title_ui_eim_none_selected));
            return;
        }

        try {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                    "application/zip", "application/json", "application/octet-stream", "text/plain"});
            startActivityForResult(intent, REQUEST_EXIM_IMPORT);
        } catch (Throwable ex) {
            Log.unexpectedError(getParentFragmentManager(), ex);
        }
    }

    private void runImport(ArrayList<String> cats, Uri uri) {
        Bundle args = new Bundle();
        args.putStringArrayList("cats", cats);
        args.putParcelable("uri", uri);

        new SimpleTask<String>() {
            private Toast toast = null;

            @Override
            protected void onPreExecute(Bundle args) {
                eximportApplying = true;
                toast = ToastEx.makeText(getContext(), R.string.title_executing, Toast.LENGTH_LONG);
                toast.show();
            }

            @Override
            protected void onPostExecute(Bundle args) {
                if (toast != null)
                    toast.cancel();
            }

            @Override
            protected String onExecute(Context context, Bundle args) throws Throwable {
                List<String> cats = args.getStringArrayList("cats");
                Uri uri = args.getParcelable("uri");
                return StateExport.performImport(context, cats, uri);
            }

            @Override
            protected void onExecuted(Bundle args, String summary) {
                eximportApplying = false;
                ServiceSynchronize.eval(getContext(), "ui import");
                refreshEximStatus();
                showImportDone(summary);
            }

            @Override
            protected void onException(Bundle args, Throwable ex) {
                eximportApplying = false;
                Log.w(ex);
                showEximInfo(getString(R.string.title_ui_eim_import_fail,
                        ex.getMessage() == null ? ex.toString() : ex.getMessage()));
            }

            @Override
            protected void onDestroyed(Bundle args) {
                eximportApplying = false;
                if (toast != null) {
                    toast.cancel();
                    toast = null;
                }
            }
        }.execute(this, args, "ui:import");
    }

}
