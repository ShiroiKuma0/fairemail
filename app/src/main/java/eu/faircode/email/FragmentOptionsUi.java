package eu.faircode.email;

import static android.app.Activity.RESULT_OK;
import static eu.faircode.email.ServiceAuthenticator.AUTH_TYPE_GMAIL;

import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
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
import android.provider.DocumentsContract;
import android.text.TextUtils;
import android.util.Base64;
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
import androidx.core.content.ContextCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.preference.PreferenceManager;

import com.flask.colorpicker.ColorPickerView;
import com.flask.colorpicker.builder.ColorPickerClickListener;
import com.flask.colorpicker.builder.ColorPickerDialogBuilder;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

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

    // Export/import state.
    // The directory URI lives in its OWN prefs file, outside the default store the
    // exporter serializes, so an export never carries a device-local SAF URI.
    private static final String EXIM_PREFS = "shiroikuma_eximport";
    private static final String KEY_DIR_URI = "dir_uri";
    private static final String EXPORT_PREFIX = "shiroikuma-fairemail-";
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
    private AlertDialog eximDialog;
    private TextView dlgDirValue;
    private TextView dlgStatus;
    private final Map<String, CheckBox> eximChecks = new LinkedHashMap<>();
    private ArrayList<String> pendingExportCats = null;

    private static final String CAT_ACCOUNTS = "accounts";
    private static final String CAT_RULES = "rules";
    private static final String CAT_CONTACTS = "contacts";
    private static final String CAT_ANSWERS = "answers";
    private static final String CAT_SEARCHES = "searches";
    private static final String CAT_CHANNELS = "channels";
    private static final String CAT_SETTINGS = "settings";
    private static final String CAT_UI = "ui";

    private static final String[] CAT_IDS = {
            CAT_ACCOUNTS, CAT_RULES, CAT_CONTACTS, CAT_ANSWERS,
            CAT_SEARCHES, CAT_CHANNELS, CAT_SETTINGS, CAT_UI
    };

    private static final int[] CAT_LABELS = {
            R.string.title_ui_eim_cat_accounts,
            R.string.title_ui_eim_cat_rules,
            R.string.title_ui_eim_cat_contacts,
            R.string.title_ui_eim_cat_answers,
            R.string.title_ui_eim_cat_searches,
            R.string.title_ui_eim_cat_channels,
            R.string.title_ui_eim_cat_settings,
            R.string.title_ui_eim_cat_ui
    };

    /**
     * The keys belonging to the "UI customization" category: the fork's custom
     * theme colours and fonts plus the theme selection and fork list toggles.
     * Everything else in the default prefs store is "App settings".
     */
    private static boolean isUiKey(String key) {
        if (key == null)
            return false;
        return CustomThemeColors.isCustomColorPref(key) ||
                key.startsWith("custom_font_") ||
                "theme".equals(key) || "beige".equals(key) ||
                "subject_lines_narrow".equals(key) || "sender_italic".equals(key);
    }

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

    private SharedPreferences eximPrefs() {
        return getContext().getSharedPreferences(EXIM_PREFS, Context.MODE_PRIVATE);
    }

    @Nullable
    private Uri dirUri() {
        String uri = eximPrefs().getString(KEY_DIR_URI, null);
        if (uri == null)
            return null;
        try {
            return Uri.parse(uri);
        } catch (Throwable ex) {
            return null;
        }
    }

    @Nullable
    private DocumentFile exportDir() {
        Uri uri = dirUri();
        if (uri == null)
            return null;
        try {
            DocumentFile dir = DocumentFile.fromTreeUri(getContext(), uri);
            return (dir != null && dir.isDirectory() ? dir : null);
        } catch (Throwable ex) {
            return null;
        }
    }

    private String exportFileName() {
        return EXPORT_PREFIX + BuildConfig.VERSION_NAME + "_" +
                new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.ROOT).format(new Date()) + ".json";
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
                    String name = file.getName();
                    if (!file.isFile() || name == null ||
                            !name.startsWith(EXPORT_PREFIX) || !name.endsWith(".json"))
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

        // Select all + one checkbox per category, everything ticked by default
        eximChecks.clear();
        CheckBox selectAll = new CheckBox(context);
        selectAll.setText(R.string.title_ui_eim_select_all);
        selectAll.setTypeface(Typeface.DEFAULT_BOLD);
        selectAll.setChecked(true);
        LinearLayout.LayoutParams salp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        salp.topMargin = dp(8);
        selectAll.setLayoutParams(salp);
        root.addView(selectAll);

        for (int i = 0; i < CAT_IDS.length; i++) {
            CheckBox cb = new CheckBox(context);
            cb.setText(CAT_LABELS[i]);
            cb.setChecked(true);
            root.addView(cb);
            eximChecks.put(CAT_IDS[i], cb);
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
        eximPrefs().edit().putString(KEY_DIR_URI, uri.toString()).apply();
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
                intent.setType("application/json");
                intent.putExtra(Intent.EXTRA_TITLE, exportFileName());
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

                JSONObject jexport = buildExport(context, cats);

                String name;
                if (uri == null) {
                    SharedPreferences eprefs = context.getSharedPreferences(EXIM_PREFS, Context.MODE_PRIVATE);
                    String dirStr = eprefs.getString(KEY_DIR_URI, null);
                    DocumentFile dir = (dirStr == null ? null
                            : DocumentFile.fromTreeUri(context, Uri.parse(dirStr)));
                    if (dir == null || !dir.isDirectory())
                        throw new FileNotFoundException("Export directory not accessible");
                    name = exportFileName();
                    DocumentFile file = dir.createFile("application/json", name);
                    if (file == null)
                        throw new FileNotFoundException("Could not create " + name);
                    uri = file.getUri();
                } else {
                    DocumentFile file = DocumentFile.fromSingleUri(context, uri);
                    name = (file == null || file.getName() == null ? uri.toString() : file.getName());
                }

                ContentResolver resolver = context.getContentResolver();
                try (OutputStream raw = resolver.openOutputStream(uri)) {
                    if (raw == null)
                        throw new FileNotFoundException(uri.toString());
                    raw.write(jexport.toString(2).getBytes());
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

    /**
     * Build the export JSON in the stock FragmentOptionsBackup format (so exports
     * stay importable via the Backup tab and vice versa), gated per category.
     * Unselected sections are emitted as empty arrays for stock-import safety.
     * Fork extension: "ui_fonts" carries the custom font binaries base64-encoded.
     */
    private static JSONObject buildExport(Context context, List<String> cats) throws Throwable {
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
        if (catAccounts)
            for (EntityAccount account : db.account().getAccounts()) {
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

        // Answers
        JSONArray janswers = new JSONArray();
        if (catAnswers)
            for (EntityAnswer answer : db.answer().getAnswers(true))
                janswers.put(answer.toJSON());

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

        return jexport;
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
            intent.putExtra(Intent.EXTRA_MIME_TYPES,
                    new String[]{"application/json", "application/octet-stream", "text/plain"});
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
                return performImport(context, cats, uri);
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

    /**
     * Import the selected categories from a stock-format (unencrypted) export.
     * Adapted from FragmentOptionsBackup.handleImport; returns a per-category
     * "Label: count" summary for the result dialog. Merges — existing accounts
     * (matched by UUID) are kept, preferences outside the selected categories
     * are untouched.
     */
    private static String performImport(Context context, List<String> cats, Uri uri) throws Throwable {
        boolean catAccounts = cats.contains(CAT_ACCOUNTS);
        boolean catRules = cats.contains(CAT_RULES);
        boolean catContacts = cats.contains(CAT_CONTACTS);
        boolean catAnswers = cats.contains(CAT_ANSWERS);
        boolean catSearches = cats.contains(CAT_SEARCHES);
        boolean catChannels = cats.contains(CAT_CHANNELS);
        boolean catSettings = cats.contains(CAT_SETTINGS);
        boolean catUi = cats.contains(CAT_UI);

        EntityLog.log(context, "UI import " + uri + " cats=" + TextUtils.join(",", cats));

        NoStreamException.check(uri, context);

        StringBuilder data = new StringBuilder();
        ContentResolver resolver = context.getContentResolver();
        InputStream is = resolver.openInputStream(uri);
        if (is == null)
            throw new FileNotFoundException(uri.toString());
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = reader.readLine()) != null)
                data.append(line);
        }

        String json = data.toString().trim();
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
                                throw new java.io.IOException("Cannot create " + dir);
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

        EntityLog.log(context, "UI import done");

        StringBuilder summary = new StringBuilder();
        if (catAccounts)
            summary.append(context.getString(R.string.title_ui_eim_cat_accounts)).append(": ").append(nAccounts).append('\n');
        if (catRules)
            summary.append(context.getString(R.string.title_ui_eim_cat_rules)).append(": ").append(nRules).append('\n');
        if (catContacts)
            summary.append(context.getString(R.string.title_ui_eim_cat_contacts)).append(": ").append(nContacts).append('\n');
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
}
