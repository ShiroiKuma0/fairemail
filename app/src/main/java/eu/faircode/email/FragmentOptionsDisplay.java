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
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.constraintlayout.widget.Group;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceManager;

import com.flask.colorpicker.ColorPickerView;
import com.flask.colorpicker.builder.ColorPickerClickListener;
import com.flask.colorpicker.builder.ColorPickerDialogBuilder;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class FragmentOptionsDisplay extends FragmentBase implements SharedPreferences.OnSharedPreferenceChangeListener {
    private View view;
    private ImageButton ibHelp;
    private Button btnTheme;
    private Spinner spStartup;
    private SwitchCompat swDate;
    private SwitchCompat swDateWeek;
    private SwitchCompat swDateFixed;
    private SwitchCompat swDateBold;
    private SwitchCompat swDateTime;
    private SwitchCompat swCategory;
    private SwitchCompat swCards;
    private SwitchCompat swBeige;
    private SwitchCompat swTabularBackground;
    private SwitchCompat swShadow;
    private SwitchCompat swShadowBorder;
    private SwitchCompat swShadowHighlight;
    private SwitchCompat swTabularDividers;
    private SwitchCompat swTabularUnreadBg;
    private SwitchCompat swPortrait2;
    private SwitchCompat swPortrait2c;
    private Spinner spPortraitMinSize;
    private SwitchCompat swLandscape;
    private Spinner spLandscapeMinSize;
    private SwitchCompat swClosePane;
    private TextView tvColumnWidth;
    private SeekBar sbColumnWidth;
    private SwitchCompat swHideToolbar;
    private SwitchCompat swEdgeToEdge;
    private SwitchCompat swNavOptions;
    private SwitchCompat swNavCategories;
    private SwitchCompat swNavLastSync;
    private SwitchCompat swNavMessageCount;
    private SwitchCompat swNavUnseenDrafts;
    private SwitchCompat swNavPinnedCount;
    private SwitchCompat swShowUnexposed;

    private SwitchCompat swThreading;
    private SwitchCompat swThreadingUnread;
    private SwitchCompat swIndentation;
    private Spinner spSpacing;
    private SwitchCompat swSeekbar;
    private SwitchCompat swActionbar;
    private SwitchCompat swActionbarSwap;
    private SwitchCompat swActionbarColor;

    private SwitchCompat swHighlightUnread;
    private ViewButtonColor btnHighlightColor;
    private Spinner spAccountColor;
    private Spinner spAccountColorSize;
    private SwitchCompat swAvatars;
    private ImageButton ibBimi;
    private TextView tvBimiHint;
    private TextView tvBimiUnverified;
    private SwitchCompat swBimi;
    private SwitchCompat swBimiVmc;
    private SwitchCompat swGravatars;
    private TextView tvGravatarPrivacy;
    private SwitchCompat swLibravatars;
    private TextView tvLibravatarPrivacy;
    private SwitchCompat swFavicons;
    private TextView tvFaviconsHint;
    private SwitchCompat swFaviconsPartial;
    private SwitchCompat swFaviconsManifest;
    private ImageButton ibFavicons;
    private SwitchCompat swDdg;
    private TextView tvDdgPrivacy;
    private SwitchCompat swFaviconsDmarc;
    private SwitchCompat swGeneratedIcons;
    private SwitchCompat swIdenticons;
    private SwitchCompat swCircular;
    private ImageView ivRed;
    private ImageView ivGreen;
    private ImageView ivBlue;
    private TextView tvSaturation;
    private SeekBar sbSaturation;
    private TextView tvBrightness;
    private SeekBar sbBrightness;
    private TextView tvThreshold;
    private SeekBar sbThreshold;

    private Spinner spNameEmail;
    private SwitchCompat swPreferContact;
    private SwitchCompat swOnlyContact;
    private SwitchCompat swDistinguishContacts;
    private SwitchCompat swShowRecipients;
    private SwitchCompat swReverseAddresses;
    private Spinner spFontSizeSender;
    private Spinner spSenderEllipsize;
    private SwitchCompat swSenderItalic;

    private SwitchCompat swSubjectTop;
    private SwitchCompat swSubjectLinesNarrow;
    private SwitchCompat swSubjectItalic;
    private SwitchCompat swHighlightSubject;
    private Spinner spFontSizeSubject;
    private Spinner spSubjectEllipsize;

    private SwitchCompat swKeywords;
    private SwitchCompat swLabels;
    private SwitchCompat swFlags;
    private SwitchCompat swFlagsBackground;
    private SwitchCompat swPreview;
    private SwitchCompat swPreviewItalic;
    private Spinner spPreviewLines;
    private TextView tvPreviewLinesHint;
    private SwitchCompat swAlignHeader;

    private SwitchCompat swAddresses;
    private SwitchCompat swHideAttachments;
    private TextView tvMessageZoom;
    private SeekBar sbMessageZoom;
    private SwitchCompat swEditorZoom;
    private SwitchCompat swOverviewMode;
    private TextView tvLineSpacing;
    private SeekBar sbLineSpacing;

    private SwitchCompat swContrast;
    private SwitchCompat swHyphenation;
    private TextView tvHyphenationHint;
    private Spinner spDisplayFont;
    private SwitchCompat swMonospacedPre;
    private SwitchCompat swTextSeparators;
    private SwitchCompat swCollapseQuotes;
    private SwitchCompat swImagesPlaceholders;
    private SwitchCompat swImagesInline;
    private SwitchCompat swButtonExtra;
    private SwitchCompat swUnzip;
    private TextView tvUnzipHint;
    private SwitchCompat swAttachmentsAlt;
    private SwitchCompat swThumbnails;
    private SwitchCompat swPdfPreview;
    private SwitchCompat swVideoPreview;
    private SwitchCompat swAudioPreview;
    private SwitchCompat swBarcodePreview;

    private SwitchCompat swListCount;
    private SwitchCompat swBundledFonts;
    private SwitchCompat swNarrowFonts;
    private SwitchCompat swParseClasses;
    private SwitchCompat swBackgroundColor;
    private SwitchCompat swTextColor;
    private SwitchCompat swTextSize;
    private SwitchCompat swTextFont;
    private SwitchCompat swTextAlign;
    private SwitchCompat swTextTitles;
    private SwitchCompat swAuthentication;
    private SwitchCompat swAuthenticationIndicator;

    private Group grpAvatar;
    private Group grpUnzip;

    private androidx.cardview.widget.CardView cardCustomColors;
    private android.widget.LinearLayout containerCustomColors;
    private final java.util.List<ViewButtonColor> customColorButtons = new java.util.ArrayList<>();

    private android.widget.LinearLayout containerCustomFont;
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

    private NumberFormat NF = NumberFormat.getNumberInstance();

    final static int[] account_color_sizes = {3, 6, 12};

    static List<String> RESET_OPTIONS = Collections.unmodifiableList(Arrays.asList(
            "theme", "startup",
            "date", "date_week", "date_fixed", "date_bold", "date_time", "group_category",
            "cards", "beige", "tabular_card_bg", "shadow_unread", "shadow_border", "shadow_highlight", "dividers", "tabular_unread_bg",
            "portrait2", "portrait2c", "landscape", "close_pane", "column_width",
            "hide_toolbar", "edge_to_edge", "nav_options", "nav_categories", "nav_last_sync", "nav_count", "nav_unseen_drafts", "nav_count_pinned", "show_unexposed",
            "threading", "threading_unread", "indentation", "spacing", "seekbar", "actionbar", "actionbar_swap", "actionbar_color",
            "highlight_unread", "highlight_color", "account_color", "account_color_size",
            "avatars", "bimi", "bimi_vmc", "gravatars", "libravatars", "favicons", "favicons_partial", "favicons_manifest", "ddg_icons", "favicons_dmarc", "generated_icons", "identicons",
            "circular", "saturation", "brightness", "threshold",
            "email_format", "prefer_contact", "only_contact", "distinguish_contacts", "show_recipients", "reverse_addresses",
            "font_size_sender", "sender_ellipsize", "sender_italic",
            "subject_top", "subject_lines_narrow", "subject_italic", "highlight_subject", "font_size_subject", "subject_ellipsize",
            "keywords_header", "labels_header", "flags", "flags_background",
            "preview", "preview_italic", "preview_lines", "align_header",
            "addresses", "hide_attachments",
            "message_zoom", "editor_zoom", "overview_mode", "line_spacing",
            "hyphenation", "display_font", "contrast", "monospaced_pre",
            "text_separators",
            "collapse_quotes", "image_placeholders", "inline_images", "button_extra",
            "unzip", "attachments_alt", "thumbnails", "pdf_preview", "video_preview", "audio_preview", "barcode_preview",
            "list_count", "bundled_fonts", "narrow_fonts", "parse_classes",
            "background_color", "text_color", "text_size", "text_font", "text_align", "text_titles",
            "authentication", "authentication_indicator"
    ));

    static {
        // Append every custom_color_* pref key from CustomThemeColors.ENTRIES so the
        // global Display reset wipes user overrides too. Doing this in a static
        // initializer means iter 3.2's expansion to new entries is picked up
        // automatically without touching this list.
        java.util.List<String> mutable = new java.util.ArrayList<>(RESET_OPTIONS);
        for (CustomThemeColors.Entry entry : CustomThemeColors.ENTRIES)
            mutable.add(entry.prefKey);
        // Likewise for every custom_font_*_<role> key
        for (CustomFont.Entry entry : CustomFont.ENTRIES) {
            mutable.add(CustomFont.prefPath(entry.role));
            mutable.add(CustomFont.prefName(entry.role));
            mutable.add(CustomFont.prefWeight(entry.role));
        }
        RESET_OPTIONS = java.util.Collections.unmodifiableList(mutable);
    }

    @Override
    @Nullable
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        setSubtitle(R.string.title_setup);
        setHasOptionsMenu(true);

        view = inflater.inflate(R.layout.fragment_options_display, container, false);

        // Get controls

        ibHelp = view.findViewById(R.id.ibHelp);
        btnTheme = view.findViewById(R.id.btnTheme);
        spStartup = view.findViewById(R.id.spStartup);
        swDate = view.findViewById(R.id.swDate);
        swDateWeek = view.findViewById(R.id.swDateWeek);
        swDateFixed = view.findViewById(R.id.swDateFixed);
        swDateBold = view.findViewById(R.id.swDateBold);
        swDateTime = view.findViewById(R.id.swDateTime);
        swCategory = view.findViewById(R.id.swCategory);
        swCards = view.findViewById(R.id.swCards);
        swBeige = view.findViewById(R.id.swBeige);
        swTabularBackground = view.findViewById(R.id.swTabularCardBackground);
        swShadow = view.findViewById(R.id.swShadow);
        swShadowBorder = view.findViewById(R.id.swShadowBorder);
        swShadowHighlight = view.findViewById(R.id.swShadowHighlight);
        swTabularDividers = view.findViewById(R.id.swTabularDividers);
        swTabularUnreadBg = view.findViewById(R.id.swTabularUnreadBg);
        swPortrait2 = view.findViewById(R.id.swPortrait2);
        swPortrait2c = view.findViewById(R.id.swPortrait2c);
        spPortraitMinSize = view.findViewById(R.id.spPortraitMinSize);
        swLandscape = view.findViewById(R.id.swLandscape);
        spLandscapeMinSize = view.findViewById(R.id.spLandscapeMinSize);
        swClosePane = view.findViewById(R.id.swClosePane);
        tvColumnWidth = view.findViewById(R.id.tvColumnWidth);
        sbColumnWidth = view.findViewById(R.id.sbColumnWidth);
        swHideToolbar = view.findViewById(R.id.swHideToolbar);
        swEdgeToEdge = view.findViewById(R.id.swEdgeToEdge);
        swNavOptions = view.findViewById(R.id.swNavOptions);
        swNavCategories = view.findViewById(R.id.swNavCategories);
        swNavLastSync = view.findViewById(R.id.swNavLastSync);
        swNavMessageCount = view.findViewById(R.id.swNavMessageCount);
        swNavUnseenDrafts = view.findViewById(R.id.swNavUnseenDrafts);
        swNavPinnedCount = view.findViewById(R.id.swNavPinnedCount);
        swShowUnexposed = view.findViewById(R.id.swShowUnexposed);

        swThreading = view.findViewById(R.id.swThreading);
        swThreadingUnread = view.findViewById(R.id.swThreadingUnread);
        swIndentation = view.findViewById(R.id.swIndentation);
        spSpacing = view.findViewById(R.id.spSpacing);
        swSeekbar = view.findViewById(R.id.swSeekbar);
        swActionbar = view.findViewById(R.id.swActionbar);
        swActionbarSwap = view.findViewById(R.id.swActionbarSwap);
        swActionbarColor = view.findViewById(R.id.swActionbarColor);

        swHighlightUnread = view.findViewById(R.id.swHighlightUnread);
        btnHighlightColor = view.findViewById(R.id.btnHighlightColor);
        spAccountColor = view.findViewById(R.id.spAccountColor);
        spAccountColorSize = view.findViewById(R.id.spAccountColorSize);
        swAvatars = view.findViewById(R.id.swAvatars);
        swBimi = view.findViewById(R.id.swBimi);
        swBimiVmc = view.findViewById(R.id.swBimiVmc);
        tvBimiHint = view.findViewById(R.id.tvBimiHint);
        tvBimiUnverified = view.findViewById(R.id.tvBimiUnverified);
        ibBimi = view.findViewById(R.id.ibBimi);
        swGravatars = view.findViewById(R.id.swGravatars);
        tvGravatarPrivacy = view.findViewById(R.id.tvGravatarPrivacy);
        swLibravatars = view.findViewById(R.id.swLibravatars);
        tvLibravatarPrivacy = view.findViewById(R.id.tvLibravatarPrivacy);
        swFavicons = view.findViewById(R.id.swFavicons);
        tvFaviconsHint = view.findViewById(R.id.tvFaviconsHint);
        swFaviconsPartial = view.findViewById(R.id.swFaviconsPartial);
        swFaviconsManifest = view.findViewById(R.id.swFaviconsManifest);
        ibFavicons = view.findViewById(R.id.ibFavicons);
        swDdg = view.findViewById(R.id.swDdg);
        tvDdgPrivacy = view.findViewById(R.id.tvDdgPrivacy);
        swFaviconsDmarc = view.findViewById(R.id.swFaviconsDmarc);
        swGeneratedIcons = view.findViewById(R.id.swGeneratedIcons);
        swIdenticons = view.findViewById(R.id.swIdenticons);
        swCircular = view.findViewById(R.id.swCircular);
        ivRed = view.findViewById(R.id.ivRed);
        ivGreen = view.findViewById(R.id.ivGreen);
        ivBlue = view.findViewById(R.id.ivBlue);
        tvSaturation = view.findViewById(R.id.tvSaturation);
        sbSaturation = view.findViewById(R.id.sbSaturation);
        tvBrightness = view.findViewById(R.id.tvBrightness);
        sbBrightness = view.findViewById(R.id.sbBrightness);
        tvThreshold = view.findViewById(R.id.tvThreshold);
        sbThreshold = view.findViewById(R.id.sbThreshold);

        spNameEmail = view.findViewById(R.id.spNameEmail);
        swPreferContact = view.findViewById(R.id.swPreferContact);
        swOnlyContact = view.findViewById(R.id.swOnlyContact);
        swDistinguishContacts = view.findViewById(R.id.swDistinguishContacts);
        swShowRecipients = view.findViewById(R.id.swShowRecipients);
        swReverseAddresses = view.findViewById(R.id.swReverseAddresses);
        spFontSizeSender = view.findViewById(R.id.spFontSizeSender);
        spSenderEllipsize = view.findViewById(R.id.spSenderEllipsize);
        swSenderItalic = view.findViewById(R.id.swSenderItalic);

        swSubjectTop = view.findViewById(R.id.swSubjectTop);
        swSubjectLinesNarrow = view.findViewById(R.id.swSubjectLinesNarrow);
        swSubjectItalic = view.findViewById(R.id.swSubjectItalic);
        swHighlightSubject = view.findViewById(R.id.swHighlightSubject);
        spFontSizeSubject = view.findViewById(R.id.spFontSizeSubject);
        spSubjectEllipsize = view.findViewById(R.id.spSubjectEllipsize);

        swKeywords = view.findViewById(R.id.swKeywords);
        swLabels = view.findViewById(R.id.swLabels);
        swFlags = view.findViewById(R.id.swFlags);
        swFlagsBackground = view.findViewById(R.id.swFlagsBackground);
        swPreview = view.findViewById(R.id.swPreview);
        swPreviewItalic = view.findViewById(R.id.swPreviewItalic);
        spPreviewLines = view.findViewById(R.id.spPreviewLines);
        tvPreviewLinesHint = view.findViewById(R.id.tvPreviewLinesHint);
        swAlignHeader = view.findViewById(R.id.swAlignHeader);
        swAddresses = view.findViewById(R.id.swAddresses);
        swHideAttachments = view.findViewById(R.id.swHideAttachments);
        tvMessageZoom = view.findViewById(R.id.tvMessageZoom);
        sbMessageZoom = view.findViewById(R.id.sbMessageZoom);
        swEditorZoom = view.findViewById(R.id.swEditorZoom);
        swOverviewMode = view.findViewById(R.id.swOverviewMode);
        tvLineSpacing = view.findViewById(R.id.tvLineSpacing);
        sbLineSpacing = view.findViewById(R.id.sbLineSpacing);
        swContrast = view.findViewById(R.id.swContrast);
        swHyphenation = view.findViewById(R.id.swHyphenation);
        tvHyphenationHint = view.findViewById(R.id.tvHyphenationHint);
        spDisplayFont = view.findViewById(R.id.spDisplayFont);
        swMonospacedPre = view.findViewById(R.id.swMonospacedPre);
        swTextSeparators = view.findViewById(R.id.swTextSeparators);
        swCollapseQuotes = view.findViewById(R.id.swCollapseQuotes);
        swImagesPlaceholders = view.findViewById(R.id.swImagesPlaceholders);
        swImagesInline = view.findViewById(R.id.swImagesInline);
        swButtonExtra = view.findViewById(R.id.swButtonExtra);
        swUnzip = view.findViewById(R.id.swUnzip);
        tvUnzipHint = view.findViewById(R.id.tvUnzipHint);
        swAttachmentsAlt = view.findViewById(R.id.swAttachmentsAlt);
        swThumbnails = view.findViewById(R.id.swThumbnails);
        swPdfPreview = view.findViewById(R.id.swPdfPreview);
        swVideoPreview = view.findViewById(R.id.swVideoPreview);
        swAudioPreview = view.findViewById(R.id.swAudioPreview);
        swBarcodePreview = view.findViewById(R.id.swBarcodePreview);

        swListCount = view.findViewById(R.id.swListCount);
        swBundledFonts = view.findViewById(R.id.swBundledFonts);
        swNarrowFonts = view.findViewById(R.id.swNarrowFonts);
        swParseClasses = view.findViewById(R.id.swParseClasses);
        swBackgroundColor = view.findViewById(R.id.swBackgroundColor);
        swTextColor = view.findViewById(R.id.swTextColor);
        swTextSize = view.findViewById(R.id.swTextSize);
        swTextFont = view.findViewById(R.id.swTextFont);
        swTextAlign = view.findViewById(R.id.swTextAlign);
        swTextTitles = view.findViewById(R.id.swTextTitles);
        swAuthentication = view.findViewById(R.id.swAuthentication);
        swAuthenticationIndicator = view.findViewById(R.id.swAuthenticationIndicator);

        grpAvatar = view.findViewById(R.id.grpAvatar);
        grpUnzip = view.findViewById(R.id.grpUnzip);

        cardCustomColors = view.findViewById(R.id.cardCustomColors);
        containerCustomColors = view.findViewById(R.id.containerCustomColors);
        populateCustomColorPicker();

        containerCustomFont = view.findViewById(R.id.containerCustomFont);
        populateCustomFontPicker();

        List<StyleHelper.FontDescriptor> fonts = StyleHelper.getFonts(getContext());

        List<CharSequence> fn = new ArrayList<>();
        fn.add("-");
        for (int i = 0; i < fonts.size(); i++) {
            StyleHelper.FontDescriptor font = fonts.get(i);
            SpannableStringBuilder ssb = new SpannableStringBuilderEx(font.toString());
            ssb.setSpan(StyleHelper.getTypefaceSpan(font.type, getContext()),
                    0, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            fn.add(ssb);
        }

        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, fn);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spDisplayFont.setAdapter(adapter);

        setOptions();

        // Wire controls

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        boolean debug = prefs.getBoolean("debug", false);

        ibHelp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Helper.view(v.getContext(), Helper.getSupportUri(v.getContext(), "Options:display"), false);
            }
        });

        btnTheme.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Bundle args = new Bundle();
                args.putBoolean("settings", true);
                FragmentDialogTheme dialog = new FragmentDialogTheme();
                dialog.setArguments(args);
                dialog.show(getParentFragmentManager(), "setup:theme");
            }
        });

        spStartup.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                String[] values = getResources().getStringArray(R.array.startupValues);
                prefs.edit().putString("startup", values[position]).apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                prefs.edit().remove("startup").apply();
            }
        });

        swDate.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                prefs.edit().putBoolean("date", checked).apply();
                swDateWeek.setEnabled(checked);
                swDateFixed.setEnabled(!checked);
                swDateBold.setEnabled(checked || swDateFixed.isChecked());
            }
        });

        swDateWeek.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                prefs.edit().putBoolean("date_week", checked).apply();
            }
        });

        swDateFixed.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                prefs.edit().putBoolean("date_fixed", checked).apply();
                swDateBold.setEnabled(swDate.isChecked() || checked);
            }
        });

        swDateBold.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                prefs.edit().putBoolean("date_bold", checked).apply();
            }
        });

        swDateTime.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                prefs.edit().putBoolean("date_time", checked).apply();
            }
        });

        swCategory.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                prefs.edit().putBoolean("group_category", checked).apply();
            }
        });

        swCards.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                prefs.edit()
                        .putBoolean("cards", checked)
                        .remove("view_padding")
                        .apply();
                swBeige.setEnabled(checked);
                swTabularBackground.setEnabled(!checked);
                swShadow.setEnabled(checked);
                swShadowBorder.setEnabled(swShadow.isEnabled() && checked);
                swShadowHighlight.setEnabled(swShadow.isEnabled() && checked);
                swTabularDividers.setEnabled(!checked);
                swTabularUnreadBg.setEnabled(!checked);
                swIndentation.setEnabled(checked);
            }
        });

        swBeige.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                getActivity().getIntent().putExtra("tab", "display");
                prefs.edit().putBoolean("beige", checked).apply();
            }
        });

        swTabularBackground.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                prefs.edit().putBoolean("tabular_card_bg", checked).apply();
            }
        });

        swShadow.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                prefs.edit().putBoolean("shadow_unread", checked).apply();
                swShadowBorder.setEnabled(swShadow.isEnabled() && checked);
                swShadowHighlight.setEnabled(swShadow.isEnabled() && checked);
            }
        });

        swShadowBorder.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                prefs.edit().putBoolean("shadow_border", checked).apply();
            }
        });

        swShadowHighlight.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                prefs.edit().putBoolean("shadow_highlight", checked).apply();
            }
        });

        swTabularDividers.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                prefs.edit().putBoolean("dividers", checked).apply();
            }
        });

        swTabularUnreadBg.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                prefs.edit().putBoolean("tabular_unread_bg", checked).apply();
            }
        });

        swPortrait2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                prefs.edit().putBoolean("portrait2", checked).apply();
                if (checked)
                    prefs.edit().putBoolean("portrait2c", false).apply();
            }
        });

        swPortrait2c.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                prefs.edit().putBoolean("portrait2c", checked).apply();
                if (checked)
                    prefs.edit().putBoolean("portrait2", false).apply();
            }
        });

        spPortraitMinSize.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                prefs.edit().putInt("portrait_min_size", position).apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                prefs.edit().remove("portrait_min_size").apply();
            }
        });

        swLandscape.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                prefs.edit().putBoolean("landscape", checked).apply();
            }
        });

        spLandscapeMinSize.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                prefs.edit().putInt("landscape_min_size", position).apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                prefs.edit().remove("landscape_min_size").apply();
            }
        });

        swClosePane.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                prefs.edit().putBoolean("close_pane", checked).apply();
            }
        });

        sbColumnWidth.setEnabled(!Helper.isSurfaceDuo());
        sbColumnWidth.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress < 10)
                    progress = 10;
                if (progress > 90)
                    progress = 90;
                prefs.edit().putInt("column_width", progress).apply();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Do nothing
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Do nothing
            }
        });

        swHideToolbar.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                prefs.edit().putBoolean("hide_toolbar", checked).apply();
            }
        });

        swEdgeToEdge.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                prefs.edit().putBoolean("edge_to_edge", checked).apply();
            }
        });

        swNavOptions.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                prefs.edit().putBoolean("nav_options", checked).apply();
            }
        });

        swNavCategories.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                prefs.edit().putBoolean("nav_categories", checked).apply();
            }
        });

        swNavMessageCount.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                prefs.edit().putBoolean("nav_count", checked).apply();
            }
        });

        swNavLastSync.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                prefs.edit().putBoolean("nav_last_sync", checked).apply();
            }
        });

        swNavUnseenDrafts.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                prefs.edit().putBoolean("nav_unseen_drafts", checked).apply();
            }
        });

        swNavPinnedCount.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                prefs.edit().putBoolean("nav_count_pinned", checked).apply();
            }
        });

        swShowUnexposed.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                prefs.edit().putBoolean("show_unexposed", checked).apply();
            }
        });

        swThreading.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                prefs.edit().putBoolean("threading", checked).apply();
                swThreadingUnread.setEnabled(checked);
                WidgetUnified.updateData(getContext());
            }
        });

        swThreadingUnread.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                prefs.edit().putBoolean("threading_unread", checked).apply();
            }
        });

        swIndentation.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                prefs.edit().putBoolean("indentation", checked).apply();
            }
        });

        spSpacing.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                prefs.edit().putInt("spacing", position).apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                prefs.edit().remove("spacing").apply();
            }
        });

        swSeekbar.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                prefs.edit().putBoolean("seekbar", checked).apply();
            }
        });

        swActionbar.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                prefs.edit().putBoolean("actionbar", checked).apply();
                swActionbarSwap.setEnabled(checked);
                swActionbarColor.setEnabled(checked);
            }
        });

        swActionbarSwap.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                prefs.edit().putBoolean("actionbar_swap", checked).apply();
            }
        });

        swActionbarColor.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                prefs.edit().putBoolean("actionbar_color", checked).apply();
            }
        });

        swHighlightUnread.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                prefs.edit().putBoolean("highlight_unread", checked).apply();
            }
        });

        btnHighlightColor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Context context = getContext();
                int editTextColor = Helper.resolveColor(context, android.R.attr.editTextColor);
                int highlightColor = prefs.getInt("highlight_color", Helper.resolveColor(context, R.attr.colorUnreadHighlight));

                ColorPickerDialogBuilder builder = ColorPickerDialogBuilder
                        .with(context)
                        .setTitle(R.string.title_advanced_highlight_color)
                        .initialColor(highlightColor)
                        .showColorEdit(true)
                        .setColorEditTextColor(editTextColor)
                        .wheelType(ColorPickerView.WHEEL_TYPE.FLOWER)
                        .density(6)
                        .lightnessSliderOnly()
                        .setPositiveButton(android.R.string.ok, new ColorPickerClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int selectedColor, Integer[] allColors) {
                                prefs.edit().putInt("highlight_color", selectedColor).apply();
                                btnHighlightColor.setColor(selectedColor);
                                WidgetUnified.updateData(context);
                            }
                        })
                        .setNegativeButton(R.string.title_reset, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                prefs.edit().remove("highlight_color").apply();
                                btnHighlightColor.setColor(Helper.resolveColor(context, R.attr.colorUnreadHighlight));
                                WidgetUnified.updateData(context);
                            }
                        });

                builder.build().show();
            }
        });

        spAccountColor.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                prefs.edit().putInt("account_color", position).apply();
                spAccountColorSize.setEnabled(position == 1);
                WidgetUnified.updateData(getContext());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                prefs.edit().remove("account_color").apply();
                WidgetUnified.updateData(getContext());
            }
        });

        spAccountColorSize.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int s = account_color_sizes[position];
                prefs.edit().putInt("account_color_size", s).apply();
                WidgetUnified.updateData(getContext());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                prefs.edit().remove("account_color_size").apply();
                WidgetUnified.updateData(getContext());
            }
        });

        swAvatars.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                prefs.edit().putBoolean("avatars", checked).apply();
                ContactInfo.clearCache(compoundButton.getContext());
            }
        });

        swBimi.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                prefs.edit().putBoolean("bimi", checked).apply();
                swBimiVmc.setEnabled(checked);
                ContactInfo.clearCache(compoundButton.getContext());
            }
        });

        swBimiVmc.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                prefs.edit().putBoolean("bimi_vmc", checked).apply();
                ContactInfo.clearCache(compoundButton.getContext());
            }
        });

        tvBimiHint.getPaint().setUnderlineText(true);
        tvBimiHint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Helper.view(v.getContext(), Uri.parse(Helper.BIMI_PRIVACY_URI), true);
            }
        });

        ibBimi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Helper.view(v.getContext(), Uri.parse("https://bimigroup.org/"), true);
            }
        });

        swGravatars.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                prefs.edit().putBoolean("gravatars", checked).apply();
                ContactInfo.clearCache(compoundButton.getContext());
            }
        });

        tvGravatarPrivacy.getPaint().setUnderlineText(true);
        tvGravatarPrivacy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Helper.view(v.getContext(), Uri.parse(Avatar.GRAVATAR_PRIVACY_URI), true);
            }
        });

        swLibravatars.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                prefs.edit().putBoolean("libravatars", checked).apply();
                ContactInfo.clearCache(compoundButton.getContext());
            }
        });

        tvLibravatarPrivacy.getPaint().setUnderlineText(true);
        tvLibravatarPrivacy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Helper.view(v.getContext(), Uri.parse(Avatar.LIBRAVATAR_PRIVACY_URI), true);
            }
        });

        swFavicons.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                prefs.edit().putBoolean("favicons", checked).apply();
                swFaviconsPartial.setEnabled(checked);
                swFaviconsManifest.setEnabled(checked);
                ContactInfo.clearCache(compoundButton.getContext());
            }
        });

        tvFaviconsHint.getPaint().setUnderlineText(true);
        tvFaviconsHint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Helper.view(v.getContext(), Uri.parse(Helper.FAVICON_PRIVACY_URI), true);
            }
        });

        swFaviconsPartial.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                prefs.edit().putBoolean("favicons_partial", checked).apply();
                ContactInfo.clearCache(compoundButton.getContext());
            }
        });

        swFaviconsManifest.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                prefs.edit().putBoolean("favicons_manifest", checked).apply();
                ContactInfo.clearCache(compoundButton.getContext());
            }
        });

        ibFavicons.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Helper.viewFAQ(view.getContext(), 154);
            }
        });

        swDdg.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                prefs.edit().putBoolean("ddg_icons", checked).apply();
                ContactInfo.clearCache(compoundButton.getContext());
            }
        });

        tvDdgPrivacy.getPaint().setUnderlineText(true);
        tvDdgPrivacy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Helper.view(v.getContext(), Uri.parse(Avatar.DDG_PRIVACY_URI), true);
            }
        });

        swFaviconsDmarc.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                prefs.edit().putBoolean("favicons_dmarc", checked).apply();
                ContactInfo.clearCache(compoundButton.getContext());
            }
        });

        swGeneratedIcons.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                prefs.edit().putBoolean("generated_icons", checked).apply();
                swIdenticons.setEnabled(checked);
                sbSaturation.setEnabled(checked);
                sbBrightness.setEnabled(checked);
                sbThreshold.setEnabled(checked);
                ContactInfo.clearCache(compoundButton.getContext());
            }
        });

        swIdenticons.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                prefs.edit().putBoolean("identicons", checked).apply();
                ContactInfo.clearCache(compoundButton.getContext());
            }
        });

        swCircular.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                prefs.edit().putBoolean("circular", checked).apply();
                updateColor();
                ContactInfo.clearCache(compoundButton.getContext());
            }
        });

        sbSaturation.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                prefs.edit().putInt("saturation", progress).apply();
                updateColor();
                ContactInfo.clearCache(seekBar.getContext());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Do nothing
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Do nothing
            }
        });

        sbBrightness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                prefs.edit().putInt("brightness", progress).apply();
                updateColor();
                ContactInfo.clearCache(seekBar.getContext());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Do nothing
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Do nothing
            }
        });

        sbThreshold.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                prefs.edit().putInt("threshold", progress).apply();
                updateColor();
                ContactInfo.clearCache(seekBar.getContext());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Do nothing
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Do nothing
            }
        });

        spNameEmail.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                prefs.edit().putInt("email_format", position).apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                prefs.edit().remove("email_format").apply();
            }
        });

        swPreferContact.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                prefs.edit().putBoolean("prefer_contact", checked).apply();
                ContactInfo.clearCache(compoundButton.getContext());
                WidgetUnified.updateData(compoundButton.getContext());
            }
        });

        swOnlyContact.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                prefs.edit().putBoolean("only_contact", checked).apply();
                WidgetUnified.updateData(compoundButton.getContext());
            }
        });

        swDistinguishContacts.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                prefs.edit().putBoolean("distinguish_contacts", checked).apply();
                ContactInfo.clearCache(compoundButton.getContext());
                WidgetUnified.updateData(compoundButton.getContext());
            }
        });

        swShowRecipients.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                prefs.edit().putBoolean("show_recipients", checked).apply();
            }
        });

        swReverseAddresses.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                prefs.edit().putBoolean("reverse_addresses", checked).apply();
            }
        });

        spFontSizeSender.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                int[] values = getResources().getIntArray(R.array.fontSizeValues);
                prefs.edit().putInt("font_size_sender", values[position]).apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                prefs.edit().remove("font_size_sender").apply();
            }
        });

        spSenderEllipsize.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                String[] values = getResources().getStringArray(R.array.ellipsizeValues);
                prefs.edit().putString("sender_ellipsize", values[position]).apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                prefs.edit().remove("sender_ellipsize").apply();
            }
        });

        swSubjectTop.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                prefs.edit().putBoolean("subject_top", checked).apply();
                WidgetUnified.updateData(getContext());
            }
        });

        swSubjectLinesNarrow.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                prefs.edit().putBoolean("subject_lines_narrow", checked).apply();
                WidgetUnified.updateData(getContext());
            }
        });

        swSubjectItalic.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                prefs.edit().putBoolean("subject_italic", checked).apply();
                WidgetUnified.updateData(getContext());
            }
        });

        swSenderItalic.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                prefs.edit().putBoolean("sender_italic", checked).apply();
                WidgetUnified.updateData(getContext());
            }
        });

        swHighlightSubject.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                prefs.edit().putBoolean("highlight_subject", checked).apply();
            }
        });

        spFontSizeSubject.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                int[] values = getResources().getIntArray(R.array.fontSizeValues);
                prefs.edit().putInt("font_size_subject", values[position]).apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                prefs.edit().remove("font_size_subject").apply();
            }
        });

        spSubjectEllipsize.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                String[] values = getResources().getStringArray(R.array.ellipsizeValues);
                prefs.edit().putString("subject_ellipsize", values[position]).apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                prefs.edit().remove("subject_ellipsize").apply();
            }
        });

        swKeywords.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                prefs.edit().putBoolean("keywords_header", checked).apply();
            }
        });

        swLabels.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                prefs.edit().putBoolean("labels_header", checked).apply();
            }
        });

        swFlags.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                prefs.edit().putBoolean("flags", checked).apply();
            }
        });

        swFlagsBackground.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                prefs.edit().putBoolean("flags_background", checked).apply();
            }
        });

        swPreview.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                prefs.edit().putBoolean("preview", checked).apply();
                swPreviewItalic.setEnabled(checked);
                spPreviewLines.setEnabled(checked);
            }
        });

        swPreviewItalic.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                prefs.edit().putBoolean("preview_italic", checked).apply();
            }
        });

        tvPreviewLinesHint.setText(getString(R.string.title_advanced_preview_lines_hint, NF.format(HtmlHelper.PREVIEW_SIZE)));

        spPreviewLines.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                prefs.edit().putInt("preview_lines", position + 1).apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                prefs.edit().remove("preview_lines").apply();
            }
        });

        swAlignHeader.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                prefs.edit().putBoolean("align_header", checked).apply();
            }
        });

        swAddresses.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                prefs.edit().putBoolean("addresses", checked).apply();
            }
        });

        swHideAttachments.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                prefs.edit().putBoolean("hide_attachments", checked).apply();
            }
        });

        sbMessageZoom.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int message_zoom = progress + 50;
                if (message_zoom == 100)
                    prefs.edit().remove("message_zoom").apply();
                else
                    prefs.edit().putInt("message_zoom", message_zoom).apply();
                tvMessageZoom.setText(getString(R.string.title_advanced_message_text_zoom2, NF.format(message_zoom)));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Do nothing
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Do nothing
            }
        });

        swEditorZoom.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                prefs.edit().putBoolean("editor_zoom", checked).apply();
            }
        });

        swOverviewMode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                prefs.edit().putBoolean("overview_mode", checked).apply();
            }
        });

        sbLineSpacing.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int line_spacing = progress + 50;
                if (line_spacing == 100)
                    prefs.edit().remove("line_spacing").apply();
                else
                    prefs.edit().putInt("line_spacing", line_spacing).apply();
                tvLineSpacing.setText(getString(R.string.title_advanced_line_spacing, NF.format(line_spacing)));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Do nothing
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Do nothing
            }
        });

        swContrast.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                prefs.edit().putBoolean("contrast", checked).apply();
            }
        });

        swHyphenation.setVisibility(Build.VERSION.SDK_INT < Build.VERSION_CODES.M ? View.GONE : View.VISIBLE);
        tvHyphenationHint.setVisibility(Build.VERSION.SDK_INT < Build.VERSION_CODES.M ? View.GONE : View.VISIBLE);
        swHyphenation.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                prefs.edit().putBoolean("hyphenation", checked).apply();
            }
        });

        spDisplayFont.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                if (position == 0)
                    prefs.edit().remove("display_font").apply();
                else
                    prefs.edit().putString("display_font", fonts.get(position - 1).type).apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                prefs.edit().remove("display_font").apply();
            }
        });

        swMonospacedPre.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                prefs.edit().putBoolean("monospaced_pre", checked).apply();
            }
        });

        String theme = prefs.getString("theme", "blue_orange_system");
        boolean bw = "black_and_white".equals(theme);

        swTextSeparators.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                prefs.edit().putBoolean("text_separators", checked).apply();
            }
        });

        swCollapseQuotes.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                prefs.edit().putBoolean("collapse_quotes", checked).apply();
            }
        });

        swImagesPlaceholders.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                prefs.edit().putBoolean("image_placeholders", checked).apply();
            }
        });

        swImagesInline.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                prefs.edit().putBoolean("inline_images", checked).apply();
            }
        });

        swButtonExtra.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                prefs.edit().putBoolean("button_extra", checked).apply();
            }
        });

        swUnzip.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                prefs.edit().putBoolean("unzip", checked).apply();
            }
        });

        tvUnzipHint.setText(getString(R.string.compressed,
                TextUtils.join(", ", MessageHelper.UNZIP_FORMATS),
                Integer.toString(MessageHelper.MAX_UNZIP_COUNT),
                Helper.humanReadableByteCount(MessageHelper.MAX_UNZIP_SIZE)));

        swAttachmentsAlt.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                prefs.edit().putBoolean("attachments_alt", checked).apply();
            }
        });

        swThumbnails.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                prefs.edit().putBoolean("thumbnails", checked).apply();
                swPdfPreview.setEnabled(checked);
                swVideoPreview.setEnabled(checked);
                swAudioPreview.setEnabled(checked);
                swBarcodePreview.setEnabled(checked);
            }
        });

        swPdfPreview.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                prefs.edit().putBoolean("pdf_preview", checked).apply();
            }
        });

        swVideoPreview.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                prefs.edit().putBoolean("video_preview", checked).apply();
            }
        });

        swAudioPreview.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                prefs.edit().putBoolean("audio_preview", checked).apply();
            }
        });

        swBarcodePreview.setVisibility(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ? View.VISIBLE : View.GONE);
        swBarcodePreview.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                prefs.edit().putBoolean("barcode_preview", checked).apply();
            }
        });

        swListCount.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                prefs.edit().putBoolean("list_count", checked).apply();
            }
        });

        swBundledFonts.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                prefs.edit().putBoolean("bundled_fonts", checked).apply();
                swNarrowFonts.setEnabled(checked);
            }
        });

        swNarrowFonts.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                prefs.edit().putBoolean("narrow_fonts", checked).apply();
            }
        });

        swParseClasses.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                prefs.edit().putBoolean("parse_classes", checked).apply();
            }
        });

        swBackgroundColor.setEnabled(!bw);
        swBackgroundColor.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                prefs.edit().putBoolean("background_color", checked).apply();
            }
        });

        swTextColor.setEnabled(!bw);
        swTextColor.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                prefs.edit().putBoolean("text_color", checked).apply();
            }
        });

        swTextSize.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                prefs.edit().putBoolean("text_size", checked).apply();
            }
        });

        swTextFont.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                prefs.edit().putBoolean("text_font", checked).apply();
            }
        });

        swTextAlign.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                prefs.edit().putBoolean("text_align", checked).apply();
            }
        });

        swTextTitles.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                prefs.edit().putBoolean("text_titles", checked).apply();
            }
        });

        swAuthentication.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                prefs.edit().putBoolean("authentication", checked).apply();
                swAuthenticationIndicator.setEnabled(checked);
            }
        });

        swAuthenticationIndicator.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                prefs.edit().putBoolean("authentication_indicator", checked).apply();
            }
        });

        // Initialize
        swFaviconsPartial.setText(getString(R.string.title_advanced_favicons_partial,
                Helper.humanReadableByteCount(ContactInfo.FAVICON_READ_BYTES, false)));
        grpAvatar.setVisibility(BuildConfig.PLAY_STORE_RELEASE ? View.GONE : View.VISIBLE);
        grpUnzip.setVisibility(Build.VERSION.SDK_INT < Build.VERSION_CODES.O ? View.GONE : View.VISIBLE);
        tvBimiUnverified.setVisibility(BuildConfig.DEBUG ? View.VISIBLE : View.GONE);

        PreferenceManager.getDefaultSharedPreferences(getContext()).registerOnSharedPreferenceChangeListener(this);

        return view;
    }

    @Override
    public void onDestroyView() {
        PreferenceManager.getDefaultSharedPreferences(getContext()).unregisterOnSharedPreferenceChangeListener(this);
        super.onDestroyView();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        if (!RESET_OPTIONS.contains(key))
            return;

        if ("message_zoom".equals(key))
            return;
        if ("line_spacing".equals(key))
            return;

        getMainHandler().removeCallbacks(update);
        getMainHandler().postDelayed(update, FragmentOptions.DELAY_SETOPTIONS);
    }

    private Runnable update = new RunnableEx("display") {
        @Override
        protected void delegate() {
            setOptions();
        }
    };

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_options, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_default) {
            FragmentOptions.reset(getContext(), RESET_OPTIONS, new Runnable() {
                @Override
                public void run() {
                    setNavigationBarColor(Color.BLACK);
                }
            });
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setOptions() {
        try {
            if (view == null || getContext() == null)
                return;

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());

            String startup = prefs.getString("startup", "unified");
            String[] startupValues = getResources().getStringArray(R.array.startupValues);
            for (int pos = 0; pos < startupValues.length; pos++)
                if (startupValues[pos].equals(startup)) {
                    spStartup.setSelection(pos);
                    break;
                }

            swDate.setChecked(prefs.getBoolean("date", true));
            swDateWeek.setChecked(prefs.getBoolean("date_week", false));
            swDateWeek.setEnabled(swDate.isChecked());
            swDateFixed.setChecked(prefs.getBoolean("date_fixed", false));
            swDateFixed.setEnabled(!swDate.isChecked());
            swDateBold.setChecked(prefs.getBoolean("date_bold", false));
            swDateBold.setEnabled(swDate.isChecked() || swDateFixed.isChecked());
            swDateTime.setChecked(prefs.getBoolean("date_time", false));
            swCategory.setChecked(prefs.getBoolean("group_category", false));
            swCards.setChecked(prefs.getBoolean("cards", true));
            swBeige.setChecked(prefs.getBoolean("beige", true));
            swTabularBackground.setChecked(prefs.getBoolean("tabular_card_bg", false));
            swShadow.setChecked(prefs.getBoolean("shadow_unread", false));
            swShadowBorder.setChecked(prefs.getBoolean("shadow_border", true));
            swShadowHighlight.setChecked(prefs.getBoolean("shadow_highlight", false));
            swBeige.setEnabled(swCards.isChecked());
            swTabularBackground.setEnabled(!swCards.isChecked());
            swShadow.setEnabled(swCards.isChecked());
            swShadowBorder.setEnabled(swShadow.isEnabled() && swShadow.isChecked());
            swShadowHighlight.setEnabled(swShadow.isEnabled() && swShadow.isChecked());
            swTabularDividers.setChecked(prefs.getBoolean("dividers", true));
            swTabularDividers.setEnabled(!swCards.isChecked());
            swTabularUnreadBg.setChecked(prefs.getBoolean("tabular_unread_bg", true));
            swTabularUnreadBg.setEnabled(!swCards.isChecked());
            swPortrait2.setChecked(prefs.getBoolean("portrait2", false));
            swPortrait2c.setChecked(prefs.getBoolean("portrait2c", false) && !swPortrait2.isChecked());
            spPortraitMinSize.setSelection(prefs.getInt("portrait_min_size", 0));
            swLandscape.setChecked(prefs.getBoolean("landscape", true));
            spLandscapeMinSize.setSelection(prefs.getInt("landscape_min_size", 0));
            swClosePane.setChecked(prefs.getBoolean("close_pane", !Helper.isSurfaceDuo()));

            int column_width = prefs.getInt("column_width", 67);
            tvColumnWidth.setText(getString(R.string.title_advanced_column_width, NF.format(column_width)));
            sbColumnWidth.setProgress(column_width);

            swHideToolbar.setChecked(prefs.getBoolean("hide_toolbar", !BuildConfig.PLAY_STORE_RELEASE));
            swEdgeToEdge.setChecked(prefs.getBoolean("edge_to_edge", false));
            swNavOptions.setChecked(prefs.getBoolean("nav_options", true));
            swNavCategories.setChecked(prefs.getBoolean("nav_categories", false));
            swNavLastSync.setChecked(prefs.getBoolean("nav_last_sync", true));
            swNavMessageCount.setChecked(prefs.getBoolean("nav_count", false));
            swNavUnseenDrafts.setChecked(prefs.getBoolean("nav_unseen_drafts", false));
            swNavPinnedCount.setChecked(prefs.getBoolean("nav_count_pinned", false));
            swShowUnexposed.setChecked(prefs.getBoolean("show_unexposed", false));

            swThreading.setChecked(prefs.getBoolean("threading", true));
            swThreadingUnread.setChecked(prefs.getBoolean("threading_unread", false));
            swThreadingUnread.setEnabled(swThreading.isChecked());
            swIndentation.setChecked(prefs.getBoolean("indentation", false));
            swIndentation.setEnabled(swCards.isChecked() && swThreading.isChecked());
            spSpacing.setSelection(prefs.getInt("spacing", 0));
            swSeekbar.setChecked(prefs.getBoolean("seekbar", false));
            swActionbar.setChecked(prefs.getBoolean("actionbar", true));
            swActionbarSwap.setChecked(prefs.getBoolean("actionbar_swap", false));
            swActionbarSwap.setEnabled(swActionbar.isChecked());
            swActionbarColor.setChecked(prefs.getBoolean("actionbar_color", false));
            swActionbarColor.setEnabled(swActionbar.isChecked());

            swHighlightUnread.setChecked(prefs.getBoolean("highlight_unread", true));

            btnHighlightColor.setColor(prefs.getInt("highlight_color",
                    Helper.resolveColor(getContext(), R.attr.colorUnreadHighlight)));

            // Custom theme colour pickers — visible only when theme=custom
            boolean isCustomTheme = "custom".equals(prefs.getString("theme", null));
            cardCustomColors.setVisibility(isCustomTheme ? View.VISIBLE : View.GONE);
            for (int i = 0; i < customColorButtons.size() && i < CustomThemeColors.ENTRIES.length; i++)
                customColorButtons.get(i).setColor(
                        CustomThemeColors.getEffectiveColor(getContext(), CustomThemeColors.ENTRIES[i]));


            spAccountColor.setSelection(prefs.getInt("account_color", 1));

            int s = prefs.getInt("account_color_size", 6);
            int account_color_size = 1;
            for (int pos = 0; pos < account_color_sizes.length; pos++)
                if (s == account_color_sizes[pos]) {
                    account_color_size = pos;
                    break;
                }

            spAccountColorSize.setSelection(account_color_size);

            spAccountColorSize.setEnabled(spAccountColor.getSelectedItemPosition() == 1);
            swAvatars.setChecked(prefs.getBoolean("avatars", true));
            swBimi.setChecked(prefs.getBoolean("bimi", false));
            swBimiVmc.setChecked(prefs.getBoolean("bimi_vmc", false));
            swBimiVmc.setEnabled(swBimi.isChecked());
            swGravatars.setChecked(prefs.getBoolean("gravatars", false));
            swLibravatars.setChecked(prefs.getBoolean("libravatars", false));
            swFavicons.setChecked(prefs.getBoolean("favicons", false));
            swFaviconsPartial.setChecked(prefs.getBoolean("favicons_partial", true));
            swFaviconsPartial.setEnabled(swFavicons.isChecked());
            swFaviconsManifest.setChecked(prefs.getBoolean("favicons_manifest", false));
            swFaviconsManifest.setEnabled(swFavicons.isChecked());
            swDdg.setChecked(prefs.getBoolean("ddg_icons", false));
            swFaviconsDmarc.setChecked(prefs.getBoolean("favicons_dmarc", false));
            swGeneratedIcons.setChecked(prefs.getBoolean("generated_icons", true));
            swIdenticons.setChecked(prefs.getBoolean("identicons", false));
            swIdenticons.setEnabled(swGeneratedIcons.isChecked());
            swCircular.setChecked(prefs.getBoolean("circular", true));

            int saturation = prefs.getInt("saturation", 100);
            tvSaturation.setText(getString(R.string.title_advanced_color_saturation, NF.format(saturation)));
            sbSaturation.setProgress(saturation);
            sbSaturation.setEnabled(swGeneratedIcons.isChecked());

            int brightness = prefs.getInt("brightness", 100);
            tvBrightness.setText(getString(R.string.title_advanced_color_value, NF.format(brightness)));
            sbBrightness.setProgress(brightness);
            sbBrightness.setEnabled(swGeneratedIcons.isChecked());

            int threshold = prefs.getInt("threshold", 50);
            tvThreshold.setText(getString(R.string.title_advanced_color_threshold, NF.format(threshold)));
            sbThreshold.setProgress(threshold);
            sbThreshold.setEnabled(swGeneratedIcons.isChecked());

            MessageHelper.AddressFormat email_format = MessageHelper.getAddressFormat(getContext());
            spNameEmail.setSelection(email_format.ordinal());
            swPreferContact.setChecked(prefs.getBoolean("prefer_contact", false));
            swOnlyContact.setChecked(prefs.getBoolean("only_contact", false));
            swDistinguishContacts.setChecked(prefs.getBoolean("distinguish_contacts", false));
            swShowRecipients.setChecked(prefs.getBoolean("show_recipients", false));
            swReverseAddresses.setChecked(prefs.getBoolean("reverse_addresses", true));
            swSenderItalic.setChecked(prefs.getBoolean("sender_italic", false));

            swSubjectTop.setChecked(prefs.getBoolean("subject_top", false));
            swSubjectLinesNarrow.setChecked(prefs.getBoolean("subject_lines_narrow", false));
            swSubjectItalic.setChecked(prefs.getBoolean("subject_italic", true));
            swHighlightSubject.setChecked(prefs.getBoolean("highlight_subject", false));

            int[] fontSizeValues = getResources().getIntArray(R.array.fontSizeValues);
            String[] ellipsizeValues = getResources().getStringArray(R.array.ellipsizeValues);

            int font_size_sender = prefs.getInt("font_size_sender", -1);
            for (int pos = 0; pos < fontSizeValues.length; pos++)
                if (fontSizeValues[pos] == font_size_sender) {
                    spFontSizeSender.setSelection(pos);
                    break;
                }

            int font_size_subject = prefs.getInt("font_size_subject", -1);
            for (int pos = 0; pos < fontSizeValues.length; pos++)
                if (fontSizeValues[pos] == font_size_subject) {
                    spFontSizeSubject.setSelection(pos);
                    break;
                }

            boolean compact = prefs.getBoolean("compact", false);
            String sender_ellipsize = prefs.getString("sender_ellipsize", compact ? "end" : "full");
            for (int pos = 0; pos < ellipsizeValues.length; pos++)
                if (ellipsizeValues[pos].equals(sender_ellipsize)) {
                    spSenderEllipsize.setSelection(pos);
                    break;
                }

            String subject_ellipsize = prefs.getString("subject_ellipsize", "full");
            for (int pos = 0; pos < ellipsizeValues.length; pos++)
                if (ellipsizeValues[pos].equals(subject_ellipsize)) {
                    spSubjectEllipsize.setSelection(pos);
                    break;
                }

            swKeywords.setChecked(prefs.getBoolean("keywords_header", false));
            swLabels.setChecked(prefs.getBoolean("labels_header", true));
            swFlags.setChecked(prefs.getBoolean("flags", true));
            swFlagsBackground.setChecked(prefs.getBoolean("flags_background", false));
            swPreview.setChecked(prefs.getBoolean("preview", false));
            swPreviewItalic.setChecked(prefs.getBoolean("preview_italic", true));
            swPreviewItalic.setEnabled(swPreview.isChecked());
            spPreviewLines.setSelection(prefs.getInt("preview_lines", 1) - 1);
            spPreviewLines.setEnabled(swPreview.isChecked());
            swAlignHeader.setChecked(prefs.getBoolean("align_header", false));

            swAddresses.setChecked(prefs.getBoolean("addresses", false));
            swHideAttachments.setChecked(prefs.getBoolean("hide_attachments", false));

            int message_zoom = prefs.getInt("message_zoom", 100);
            tvMessageZoom.setText(getString(R.string.title_advanced_message_text_zoom2, NF.format(message_zoom)));
            if (message_zoom >= 50 && message_zoom <= 250)
                sbMessageZoom.setProgress(message_zoom - 50);

            swEditorZoom.setChecked(prefs.getBoolean("editor_zoom", true));
            swOverviewMode.setChecked(prefs.getBoolean("overview_mode", false));

            int line_spacing = prefs.getInt("line_spacing", 100);
            tvLineSpacing.setText(getString(R.string.title_advanced_line_spacing, NF.format(line_spacing)));
            if (line_spacing >= 50 && line_spacing <= 150)
                sbLineSpacing.setProgress(line_spacing - 50);

            swContrast.setChecked(prefs.getBoolean("contrast", false));
            swHyphenation.setChecked(prefs.getBoolean("hyphenation", false));

            String display_font = prefs.getString("display_font", "");
            List<StyleHelper.FontDescriptor> fonts = StyleHelper.getFonts(getContext());
            for (int pos = 0; pos < fonts.size(); pos++) {
                StyleHelper.FontDescriptor font = fonts.get(pos);
                if (font.type.equals(display_font)) {
                    spDisplayFont.setSelection(pos + 1);
                    break;
                }
            }

            swMonospacedPre.setChecked(prefs.getBoolean("monospaced_pre", false));
            swTextSeparators.setChecked(prefs.getBoolean("text_separators", true));
            swCollapseQuotes.setChecked(prefs.getBoolean("collapse_quotes", false));
            swImagesPlaceholders.setChecked(prefs.getBoolean("image_placeholders", true));
            swImagesInline.setChecked(prefs.getBoolean("inline_images", false));
            swButtonExtra.setChecked(prefs.getBoolean("button_extra", false));
            swUnzip.setChecked(prefs.getBoolean("unzip", !BuildConfig.PLAY_STORE_RELEASE));
            swAttachmentsAlt.setChecked(prefs.getBoolean("attachments_alt", false));
            swThumbnails.setChecked(prefs.getBoolean("thumbnails", true));
            swPdfPreview.setChecked(prefs.getBoolean("pdf_preview", true));
            swPdfPreview.setEnabled(swThumbnails.isChecked());
            swVideoPreview.setChecked(prefs.getBoolean("video_preview", true));
            swVideoPreview.setEnabled(swThumbnails.isChecked());
            swAudioPreview.setChecked(prefs.getBoolean("audio_preview", true));
            swAudioPreview.setEnabled(swThumbnails.isChecked());
            swBarcodePreview.setChecked(prefs.getBoolean("barcode_preview", true));
            swBarcodePreview.setEnabled(swThumbnails.isChecked());

            swListCount.setChecked(prefs.getBoolean("list_count", false));
            swBundledFonts.setChecked(prefs.getBoolean("bundled_fonts", true));
            swNarrowFonts.setChecked(prefs.getBoolean("narrow_fonts", false));
            swNarrowFonts.setEnabled(swBundledFonts.isChecked());
            swParseClasses.setChecked(prefs.getBoolean("parse_classes", true));
            swBackgroundColor.setChecked(prefs.getBoolean("background_color", false));
            swTextColor.setChecked(prefs.getBoolean("text_color", true));
            swTextSize.setChecked(prefs.getBoolean("text_size", true));
            swTextFont.setChecked(prefs.getBoolean("text_font", true));
            swTextAlign.setChecked(prefs.getBoolean("text_align", true));
            swTextTitles.setChecked(prefs.getBoolean("text_titles", false));
            swAuthentication.setChecked(prefs.getBoolean("authentication", true));
            swAuthenticationIndicator.setChecked(prefs.getBoolean("authentication_indicator", false));
            swAuthenticationIndicator.setEnabled(swAuthentication.isChecked());

            updateColor();
        } catch (Throwable ex) {
            Log.e(ex);
        }
    }

    private void setNavigationBarColor(int color) {
        FragmentActivity activity = getActivity();
        if (activity == null)
            return;
        Window window = activity.getWindow();
        if (window == null)
            return;
        window.setNavigationBarColor(color);
    }

    private void updateColor() {
        Context context = getContext();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean identicons = prefs.getBoolean("identicons", false);
        boolean circular = prefs.getBoolean("circular", true);

        int size = Helper.dp2pixels(context, 36);
        byte[] ahash = ImageHelper.getHash("abc@example.com");
        byte[] bhash = ImageHelper.getHash("bcd@example.com");
        byte[] chash = ImageHelper.getHash("cde@example.com");
        Integer radius = (circular && !identicons ? null : Helper.dp2pixels(context, 3));

        Bitmap red = (identicons
                ? ImageHelper.generateIdenticon(ahash, 0f, size, 5, context)
                : ImageHelper.generateLetterIcon("A", 0f, size, context));

        Bitmap green = (identicons
                ? ImageHelper.generateIdenticon(bhash, 120f, size, 5, context)
                : ImageHelper.generateLetterIcon("B", 120f, size, context));

        Bitmap blue = (identicons
                ? ImageHelper.generateIdenticon(chash, 240f, size, 5, context)
                : ImageHelper.generateLetterIcon("C", 240f, size, context));

        ivRed.setImageBitmap(ImageHelper.makeCircular(red, radius));
        ivGreen.setImageBitmap(ImageHelper.makeCircular(green, radius));
        ivBlue.setImageBitmap(ImageHelper.makeCircular(blue, radius));
    }

    /**
     * Populate the custom-colour picker container by iterating
     * {@link CustomThemeColors#ENTRIES}. Adjacent entries with the same
     * {@link CustomThemeColors.Entry#section} share one header above the first.
     * Each entry renders as: {@code [section header (if new section)] [swatch
     * with label] [italic description]}. Called once from onCreateView; the
     * resulting button list is updated by {@link #setOptions()} on every
     * resume.
     */
    private void populateCustomColorPicker() {
        Context context = getContext();
        if (context == null) return;
        Resources res = getResources();
        int dp4 = (int) (4 * res.getDisplayMetrics().density);
        int dp6 = (int) (6 * res.getDisplayMetrics().density);
        int dp12 = (int) (12 * res.getDisplayMetrics().density);
        int dp18 = (int) (18 * res.getDisplayMetrics().density);

        String currentSection = null;
        for (CustomThemeColors.Entry entry : CustomThemeColors.ENTRIES) {
            // Section header — render once when section changes
            if (!entry.section.equals(currentSection)) {
                currentSection = entry.section;
                TextView header = new TextView(context);
                header.setText(entry.sectionLabelRes);
                header.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 16);
                header.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
                LinearLayout.LayoutParams headerLp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
                headerLp.topMargin = dp18;
                headerLp.bottomMargin = dp4;
                header.setLayoutParams(headerLp);
                containerCustomColors.addView(header);
            }

            // Swatch — uses the small-button style via the 3-arg constructor with defStyleAttr
            ViewButtonColor button = new ViewButtonColor(context, null, android.R.attr.buttonStyleSmall);
            button.setText(entry.labelRes);
            button.setPadding(dp6, button.getPaddingTop(), dp6, button.getPaddingBottom());
            LinearLayout.LayoutParams buttonLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            buttonLp.topMargin = dp6;
            button.setLayoutParams(buttonLp);
            containerCustomColors.addView(button);
            wireCustomColorButton(button, entry);
            customColorButtons.add(button);

            // Per-row description — italic small text under the swatch
            if (entry.descriptionRes != 0) {
                TextView desc = new TextView(context);
                desc.setText(entry.descriptionRes);
                desc.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12);
                desc.setTypeface(desc.getTypeface(), android.graphics.Typeface.ITALIC);
                LinearLayout.LayoutParams descLp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
                descLp.topMargin = dp4;
                descLp.rightMargin = dp12;
                desc.setLayoutParams(descLp);
                containerCustomColors.addView(desc);
            }
        }
    }

    /**
     * Build the custom-font picker rows programmatically from {@link CustomFont#ENTRIES}.
     * For each role: section header (when section id changes from the previous entry),
     * role label, [Pick button | Name TextView | Reset button], weight label + SeekBar,
     * italic description. The Default role's weight slider exists for symmetry but does
     * not directly drive any view in the current build.
     */
    private void populateCustomFontPicker() {
        final Context context = getContext();
        if (context == null || containerCustomFont == null)
            return;
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        final float density = context.getResources().getDisplayMetrics().density;
        final int dp4 = Math.round(4 * density);
        final int dp8 = Math.round(8 * density);
        final int dp12 = Math.round(12 * density);
        final int dp16 = Math.round(16 * density);
        final int dp24 = Math.round(24 * density);

        String previousSection = null;
        for (final CustomFont.Entry entry : CustomFont.ENTRIES) {
            // Section header when the section id changes from the previous entry
            if (!entry.section.equals(previousSection)) {
                previousSection = entry.section;
                TextView header = new TextView(context);
                header.setText(entry.sectionLabelRes);
                header.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 16);
                header.setTypeface(header.getTypeface(), android.graphics.Typeface.BOLD);
                LinearLayout.LayoutParams headerLp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
                headerLp.topMargin = dp24;
                headerLp.bottomMargin = dp4;
                header.setLayoutParams(headerLp);
                containerCustomFont.addView(header);
            }

            // Role label
            TextView label = new TextView(context);
            label.setText(entry.labelRes);
            label.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 14);
            LinearLayout.LayoutParams labelLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            labelLp.topMargin = dp12;
            label.setLayoutParams(labelLp);
            containerCustomFont.addView(label);

            // Pick button + Name TextView + Reset button row
            android.widget.LinearLayout pickRow = new android.widget.LinearLayout(context);
            pickRow.setOrientation(android.widget.LinearLayout.HORIZONTAL);
            pickRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams pickRowLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            pickRowLp.topMargin = dp4;
            pickRow.setLayoutParams(pickRowLp);
            containerCustomFont.addView(pickRow);

            android.widget.Button pickBtn = new android.widget.Button(context);
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
            nameLp.leftMargin = dp12;
            nameLp.rightMargin = dp8;
            nameTv.setLayoutParams(nameLp);
            pickRow.addView(nameTv);

            android.widget.Button resetBtn = new android.widget.Button(
                    new android.view.ContextThemeWrapper(context,
                            androidx.appcompat.R.style.Widget_AppCompat_Button_Borderless),
                    null, 0);
            resetBtn.setText(R.string.title_reset);
            pickRow.addView(resetBtn);

            // Weight label
            final TextView weightLabel = new TextView(context);
            weightLabel.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 14);
            LinearLayout.LayoutParams weightLabelLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            weightLabelLp.topMargin = dp8;
            weightLabel.setLayoutParams(weightLabelLp);
            containerCustomFont.addView(weightLabel);

            // Weight slider
            android.widget.SeekBar weightSlider = new android.widget.SeekBar(context);
            weightSlider.setMax(9);
            int currentWeight = prefs.getInt(CustomFont.prefWeight(entry.role), 0);
            int sliderPos = (currentWeight <= 0
                    ? 0
                    : Math.min(9, Math.max(1, currentWeight / 100)));
            weightSlider.setProgress(sliderPos);
            weightLabel.setText(weightLabelText(context, sliderPos));
            LinearLayout.LayoutParams sliderLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            weightSlider.setLayoutParams(sliderLp);
            containerCustomFont.addView(weightSlider);

            // Description
            if (entry.descriptionRes != 0) {
                TextView desc = new TextView(context);
                desc.setText(entry.descriptionRes);
                desc.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12);
                desc.setTypeface(desc.getTypeface(), android.graphics.Typeface.ITALIC);
                LinearLayout.LayoutParams descLp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
                descLp.topMargin = dp4;
                descLp.rightMargin = dp16;
                desc.setLayoutParams(descLp);
                containerCustomFont.addView(desc);
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

    /**
     * Wire one ViewButtonColor to its colour picker dialog.
     * Tap opens the picker; long-press resets the colour to default.
     * In iteration 1.1 saving the pref does not visibly change anything other
     * than the swatch — the override-resolution mechanism is added in 1.2.
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
}
