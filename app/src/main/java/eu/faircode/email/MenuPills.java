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
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.Spanned;
import android.text.style.ImageSpan;
import android.util.SparseIntArray;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.view.menu.MenuItemImpl;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

// shiroikuma fork: what a menu row needs to know to be drawn as a pill
// (see PillMenuItemView). Menu items carry no icon, no "opens something"
// flag and no "destructive" flag of their own, so these tables supply them,
// keyed by item id. FairEmail uses the title string id as the item id for
// menus built in code, which is what makes one table serve many menus.
class MenuPills {
    static final int DISABLED_ALPHA = 97; // 38 %
    static final int TOGGLE_HEIGHT_DP = 36; // a checkable row: the switch and little more
    static final int PILL_MARGIN_DP = 3; // above and below a pill

    private static final SparseIntArray ICONS = new SparseIntArray();
    // What a row does, told by the colour of its icon: item id to colour role
    private static final SparseIntArray ROLES = new SparseIntArray();
    private static final Set<Integer> OPENS = new HashSet<>(Arrays.asList(
            R.string.title_synchronize_more,
            R.string.title_edit_rules,
            R.string.title_edit_color,
            R.string.title_edit_properties,
            R.string.title_create_channel,
            R.string.title_edit_channel,
            R.string.title_export_messages,
            R.string.title_import_messages,
            R.string.title_create_sub_folder,
            R.string.title_copy,
            R.string.title_copy_to,
            R.string.title_flag_color,
            R.string.title_move,
            R.string.title_move_to_account,
            R.string.title_move_to_folder,
            R.string.title_rule_edit_group,
            R.string.title_snooze,
            R.string.title_edit_contact,
            R.string.title_insert_contact,
            R.string.title_manage_keywords,
            R.string.title_log,
            R.string.title_search,
            R.string.title_search_sender,
            R.string.title_share,
            R.string.title_rename,
            R.string.title_legend_edit,
            R.string.title_send_dialog,
            R.string.title_setup_gmail,
            R.string.title_setup_imap,
            R.string.title_setup_other,
            R.string.title_setup_pop3,
            R.string.title_setup_help,
            R.string.title_setup_reorder_accounts,
            R.string.title_setup_reorder_folders,
            R.string.title_advanced_manage_certificates,
            R.string.title_advanced_create_alias,
            R.string.title_rule_select_sound_audio,
            R.string.title_rule_select_sound_ringtone,
            R.string.title_compose,
            R.string.title_edit_plain_text,
            R.string.title_edit_formatted_text,
            R.string.title_fullscreen,
            R.string.menu_about,
            R.string.menu_answers,
            R.string.menu_faq,
            R.string.menu_issue,
            R.string.menu_legend,
            R.string.menu_privacy,
            R.string.menu_pro,
            R.string.menu_rate,
            R.string.menu_rules,
            R.string.menu_setup,
            R.string.menu_test,
            R.string.menu_translate));
    private static final Set<Integer> DESTRUCTIVE = new HashSet<>(Arrays.asList(
            R.string.title_delete_local,
            R.string.title_delete_browsed,
            R.string.title_expunge,
            R.string.title_empty_trash,
            R.string.title_empty_spam,
            R.string.title_delete_channel,
            R.string.title_delete,
            R.string.title_delete_permanently,
            R.string.title_reset,
            R.string.title_reset_sign_key));

    static {
        ICONS.put(R.string.menu_about, R.drawable.twotone_info_24);
        ICONS.put(R.string.menu_answers, R.drawable.twotone_text_snippet_24);
        ICONS.put(R.string.menu_faq, R.drawable.twotone_support_24);
        ICONS.put(R.string.menu_issue, R.drawable.twotone_bug_report_24);
        ICONS.put(R.string.menu_legend, R.drawable.twotone_help_outline_24);
        ICONS.put(R.string.menu_privacy, R.drawable.twotone_security_24);
        ICONS.put(R.string.menu_pro, R.drawable.twotone_monetization_on_24);
        ICONS.put(R.string.menu_rate, R.drawable.twotone_star_24);
        ICONS.put(R.string.menu_rules, R.drawable.twotone_filter_alt_24);
        ICONS.put(R.string.menu_setup, R.drawable.twotone_settings_24);
        ICONS.put(R.string.menu_test, R.drawable.twotone_science_24);
        ICONS.put(R.string.menu_translate, R.drawable.twotone_translate_24);
        ICONS.put(R.string.title_account_auth_password, R.drawable.twotone_password_24);
        ICONS.put(R.string.title_account_auth_update, R.drawable.twotone_vpn_key_24);
        ICONS.put(R.string.title_account_ondemand, R.drawable.twotone_sync_disabled_24);
        ICONS.put(R.string.title_advanced_create_alias, R.drawable.twotone_person_add_24);
        ICONS.put(R.string.title_advanced_manage_certificates, R.drawable.twotone_vpn_key_24);
        ICONS.put(R.string.title_advanced_never_favorite, R.drawable.twotone_star_border_24);
        ICONS.put(R.string.title_analyze, R.drawable.twotone_psychology_24);
        ICONS.put(R.string.title_answer_favorite, R.drawable.twotone_star_24);
        ICONS.put(R.string.title_answer_hide, R.drawable.twotone_visibility_off_24);
        ICONS.put(R.string.title_archive, R.drawable.twotone_archive_24);
        ICONS.put(R.string.title_clipboard_copy, R.drawable.twotone_file_copy_24);
        ICONS.put(R.string.title_compose, R.drawable.twotone_edit_24);
        ICONS.put(R.string.title_copy, R.drawable.twotone_file_copy_24);
        ICONS.put(R.string.title_copy_to, R.drawable.twotone_file_copy_24);
        ICONS.put(R.string.title_create_channel, R.drawable.twotone_notifications_24);
        ICONS.put(R.string.title_create_sub_folder, R.drawable.twotone_create_new_folder_24);
        ICONS.put(R.string.title_delete, R.drawable.twotone_delete_24);
        ICONS.put(R.string.title_delete_browsed, R.drawable.twotone_delete_forever_24);
        ICONS.put(R.string.title_delete_channel, R.drawable.twotone_notifications_off_24);
        ICONS.put(R.string.title_delete_local, R.drawable.twotone_delete_24);
        ICONS.put(R.string.title_delete_permanently, R.drawable.twotone_delete_forever_24);
        ICONS.put(R.string.title_download_batch_disable, R.drawable.twotone_cloud_off_24);
        ICONS.put(R.string.title_download_batch_enable, R.drawable.twotone_cloud_download_24);
        ICONS.put(R.string.title_edit_channel, R.drawable.twotone_settings_24);
        ICONS.put(R.string.title_edit_color, R.drawable.twotone_palette_24);
        ICONS.put(R.string.title_edit_contact, R.drawable.twotone_person_24);
        ICONS.put(R.string.title_edit_formatted_text, R.drawable.twotone_text_format_24);
        ICONS.put(R.string.title_edit_plain_text, R.drawable.twotone_notes_24);
        ICONS.put(R.string.title_edit_properties, R.drawable.twotone_edit_24);
        ICONS.put(R.string.title_edit_rules, R.drawable.twotone_filter_alt_24);
        ICONS.put(R.string.title_empty_spam, R.drawable.twotone_report_24);
        ICONS.put(R.string.title_empty_trash, R.drawable.twotone_delete_forever_24);
        ICONS.put(R.string.title_enabled, R.drawable.twotone_toggle_on_24);
        ICONS.put(R.string.title_execute_rules, R.drawable.twotone_play_arrow_24);
        ICONS.put(R.string.title_export_messages, R.drawable.twotone_file_upload_24);
        ICONS.put(R.string.title_expunge, R.drawable.twotone_remove_circle_outline_24);
        ICONS.put(R.string.title_fit_width, R.drawable.twotone_swap_horiz_24);
        ICONS.put(R.string.title_flag, R.drawable.twotone_star_24);
        ICONS.put(R.string.title_flag_color, R.drawable.twotone_star_24);
        ICONS.put(R.string.title_folder_drafts, R.drawable.twotone_drafts_24);
        ICONS.put(R.string.title_folder_inbox, R.drawable.twotone_inbox_24);
        ICONS.put(R.string.title_folder_junk, R.drawable.twotone_report_24);
        ICONS.put(R.string.title_full_sync_stop, R.drawable.twotone_stop_24);
        ICONS.put(R.string.title_fullscreen, R.drawable.twotone_fullscreen_24);
        ICONS.put(R.string.title_hide, R.drawable.twotone_visibility_off_24);
        ICONS.put(R.string.title_hide_folder, R.drawable.twotone_visibility_off_24);
        ICONS.put(R.string.title_imap, R.drawable.twotone_mail_24);
        ICONS.put(R.string.title_import_messages, R.drawable.twotone_save_alt_24);
        ICONS.put(R.string.title_importance_high, R.drawable.twotone_priority_high_24);
        ICONS.put(R.string.title_importance_low, R.drawable.twotone_low_priority_24);
        ICONS.put(R.string.title_importance_normal, R.drawable.twotone_horizontal_rule_24);
        ICONS.put(R.string.title_insert_brackets, R.drawable.twotone_data_array_24);
        ICONS.put(R.string.title_insert_contact, R.drawable.twotone_person_add_24);
        ICONS.put(R.string.title_insert_line, R.drawable.twotone_horizontal_rule_24);
        ICONS.put(R.string.title_insert_quotes, R.drawable.twotone_format_quote_24);
        ICONS.put(R.string.title_legend_edit, R.drawable.twotone_edit_24);
        ICONS.put(R.string.title_log, R.drawable.twotone_list_alt_24);
        ICONS.put(R.string.title_lt_add, R.drawable.twotone_add_24);
        ICONS.put(R.string.title_lt_delete, R.drawable.twotone_remove_circle_outline_24);
        ICONS.put(R.string.title_manage_keywords, R.drawable.twotone_label_24);
        ICONS.put(R.string.title_mark_all_read, R.drawable.twotone_drafts_24);
        ICONS.put(R.string.title_monospaced_pre, R.drawable.twotone_code_24);
        ICONS.put(R.string.title_move, R.drawable.twotone_drive_file_move_24);
        ICONS.put(R.string.title_move_to_account, R.drawable.twotone_drive_file_move_24);
        ICONS.put(R.string.title_move_to_folder, R.drawable.twotone_drive_file_move_24);
        ICONS.put(R.string.title_navigation_folder, R.drawable.twotone_menu_book_24);
        ICONS.put(R.string.title_navigation_folder_hide, R.drawable.twotone_visibility_off_24);
        ICONS.put(R.string.title_notify_batch_disable, R.drawable.twotone_notifications_off_24);
        ICONS.put(R.string.title_notify_batch_enable, R.drawable.twotone_notifications_24);
        ICONS.put(R.string.title_notify_folder, R.drawable.twotone_notifications_24);
        ICONS.put(R.string.title_paste_as_quote, R.drawable.twotone_format_quote_24);
        ICONS.put(R.string.title_pin, R.drawable.twotone_push_pin_24);
        ICONS.put(R.string.title_pop3, R.drawable.twotone_mail_outline_24);
        ICONS.put(R.string.title_primary, R.drawable.twotone_star_24);
        ICONS.put(R.string.title_raw_send, R.drawable.twotone_attach_email_24);
        ICONS.put(R.string.title_redo, R.drawable.twotone_redo_24);
        ICONS.put(R.string.title_rename, R.drawable.twotone_edit_24);
        ICONS.put(R.string.title_report_spam, R.drawable.twotone_report_24);
        ICONS.put(R.string.title_reset, R.drawable.twotone_restart_alt_24);
        ICONS.put(R.string.title_reset_sign_key, R.drawable.twotone_vpn_key_24);
        ICONS.put(R.string.title_rule_edit_group, R.drawable.twotone_label_24);
        ICONS.put(R.string.title_rule_enabled, R.drawable.twotone_toggle_on_24);
        ICONS.put(R.string.title_rule_execute, R.drawable.twotone_play_arrow_24);
        ICONS.put(R.string.title_rule_select_sound_audio, R.drawable.twotone_audiotrack_24);
        ICONS.put(R.string.title_rule_select_sound_ringtone, R.drawable.twotone_notifications_24);
        ICONS.put(R.string.title_save, R.drawable.twotone_save_alt_24);
        ICONS.put(R.string.title_search, R.drawable.twotone_search_24);
        ICONS.put(R.string.title_search_sender, R.drawable.twotone_person_search_24);
        ICONS.put(R.string.title_seen, R.drawable.twotone_drafts_24);
        ICONS.put(R.string.title_select_block, R.drawable.twotone_select_all_24);
        ICONS.put(R.string.title_send_dialog, R.drawable.twotone_send_24);
        ICONS.put(R.string.title_setup_close, R.drawable.twotone_close_24);
        ICONS.put(R.string.title_setup_gmail, R.drawable.twotone_mail_24);
        ICONS.put(R.string.title_setup_help, R.drawable.twotone_help_24);
        ICONS.put(R.string.title_setup_imap, R.drawable.twotone_mail_24);
        ICONS.put(R.string.title_setup_oauth_authorize, R.drawable.twotone_login_24);
        ICONS.put(R.string.title_setup_other, R.drawable.twotone_mail_outline_24);
        ICONS.put(R.string.title_setup_pop3, R.drawable.twotone_mail_outline_24);
        ICONS.put(R.string.title_setup_reorder_accounts, R.drawable.twotone_reorder_24);
        ICONS.put(R.string.title_setup_reorder_folders, R.drawable.twotone_reorder_24);
        ICONS.put(R.string.title_share, R.drawable.twotone_share_24);
        ICONS.put(R.string.title_snooze, R.drawable.twotone_timelapse_24);
        ICONS.put(R.string.title_spam, R.drawable.twotone_report_24);
        ICONS.put(R.string.title_subscribe, R.drawable.twotone_playlist_add_check_24);
        ICONS.put(R.string.title_summarize, R.drawable.twotone_auto_awesome_24);
        ICONS.put(R.string.title_synchronize_batch_disable, R.drawable.twotone_sync_disabled_24);
        ICONS.put(R.string.title_synchronize_batch_enable, R.drawable.twotone_sync_24);
        ICONS.put(R.string.title_synchronize_enabled, R.drawable.twotone_sync_alt_24);
        ICONS.put(R.string.title_synchronize_more, R.drawable.twotone_cloud_download_24);
        ICONS.put(R.string.title_synchronize_now, R.drawable.twotone_sync_24);
        ICONS.put(R.string.title_trash, R.drawable.twotone_delete_24);
        ICONS.put(R.string.title_undo, R.drawable.twotone_undo_24);
        ICONS.put(R.string.title_unflag, R.drawable.twotone_star_border_24);
        ICONS.put(R.string.title_unhide, R.drawable.twotone_visibility_24);
        ICONS.put(R.string.title_unified_folder, R.drawable.twotone_all_inbox_24);
        ICONS.put(R.string.title_unified_inbox_add, R.drawable.twotone_all_inbox_24);
        ICONS.put(R.string.title_unified_inbox_delete, R.drawable.twotone_inbox_24);
        ICONS.put(R.string.title_unseen, R.drawable.twotone_mail_24);
        ICONS.put(R.string.title_zip, R.drawable.twotone_archive_24);

        // Items of the menu resources, which are keyed by their own id
        ICONS.put(R.id.menu_changelog, R.drawable.twotone_list_alt_24);
        ICONS.put(R.id.menu_attribution, R.drawable.twotone_info_24);
        ICONS.put(R.id.menu_compact, R.drawable.twotone_unfold_less_24);
        ICONS.put(R.id.menu_show_users, R.drawable.twotone_person_24);
        ICONS.put(R.id.menu_show_folders, R.drawable.twotone_folder_24);
        ICONS.put(R.id.menu_theme, R.drawable.twotone_palette_24);
        ICONS.put(R.id.menu_force_sync, R.drawable.twotone_sync_24);
        ICONS.put(R.id.menu_pwned, R.drawable.twotone_security_24);
        ICONS.put(R.id.menu_placeholders, R.drawable.twotone_data_object_24);
        ICONS.put(R.id.menu_lines, R.drawable.twotone_format_list_numbered_24);
        ICONS.put(R.id.menu_links, R.drawable.twotone_insert_link_45_24);
        ICONS.put(R.id.menu_pretty, R.drawable.twotone_auto_fix_high_24);
        ICONS.put(R.id.menu_check_html, R.drawable.twotone_html_24);
        ICONS.put(R.id.menu_save, R.drawable.twotone_save_alt_24);
        ICONS.put(R.id.menu_show_hidden, R.drawable.twotone_visibility_24);
        ICONS.put(R.id.menu_show_flagged, R.drawable.twotone_star_24);
        ICONS.put(R.id.menu_subscribed_only, R.drawable.twotone_playlist_add_check_24);
        ICONS.put(R.id.menu_sort_unread_atop, R.drawable.twotone_vertical_align_top_24);
        ICONS.put(R.id.menu_sort_sync, R.drawable.twotone_sort_24);
        ICONS.put(R.id.menu_search_folder, R.drawable.twotone_search_24);
        ICONS.put(R.id.menu_apply_all, R.drawable.twotone_done_all_24);
        ICONS.put(R.id.menu_edit_account_name, R.drawable.twotone_edit_24);
        ICONS.put(R.id.menu_edit_account_color, R.drawable.twotone_palette_24);
        ICONS.put(R.id.menu_enabled, R.drawable.twotone_toggle_on_24);
        ICONS.put(R.id.menu_show, R.drawable.twotone_visibility_24);
        ICONS.put(R.id.menu_clear, R.drawable.twotone_clear_all_24);
        ICONS.put(R.id.menu_delete, R.drawable.twotone_delete_24);
        ICONS.put(R.id.menu_default, R.drawable.twotone_restart_alt_24);
        ICONS.put(R.id.menu_response, R.drawable.twotone_question_answer_24);
        ICONS.put(R.id.menu_export, R.drawable.twotone_file_upload_24);
        ICONS.put(R.id.menu_import, R.drawable.twotone_save_alt_24);
        ICONS.put(R.id.menu_delete_all, R.drawable.twotone_delete_forever_24);
        ICONS.put(R.id.menu_edit_html, R.drawable.twotone_code_24);
        ICONS.put(R.id.menu_import_file, R.drawable.twotone_save_alt_24);
        ICONS.put(R.id.menu_reset_order, R.drawable.twotone_restart_alt_24);
        ICONS.put(R.id.menu_unseen, R.drawable.twotone_mail_24);
        ICONS.put(R.id.menu_hide, R.drawable.twotone_visibility_off_24);
        ICONS.put(R.id.menu_raw_send_message, R.drawable.twotone_mail_24);
        ICONS.put(R.id.menu_raw_send_thread, R.drawable.twotone_message_24);
        ICONS.put(R.id.menu_style_setting_keep_selection, R.drawable.twotone_select_all_24);
        ICONS.put(R.id.menu_ascending, R.drawable.twotone_swap_vert_24);
        ICONS.put(R.id.menu_filter_seen, R.drawable.twotone_drafts_24);
        ICONS.put(R.id.menu_filter_unseen, R.drawable.twotone_mail_24);
        ICONS.put(R.id.menu_filter_flagged, R.drawable.twotone_star_24);
        ICONS.put(R.id.menu_filter_unflagged, R.drawable.twotone_star_border_24);
        ICONS.put(R.id.menu_filter_unknown, R.drawable.twotone_person_off_24);
        ICONS.put(R.id.menu_filter_snoozed, R.drawable.twotone_visibility_off_24);
        ICONS.put(R.id.menu_filter_deleted, R.drawable.twotone_delete_24);
        ICONS.put(R.id.menu_filter_language, R.drawable.twotone_language_24);
        ICONS.put(R.id.menu_filter_duplicates, R.drawable.twotone_file_copy_24);
        ICONS.put(R.id.menu_filter_sent, R.drawable.twotone_send_24);
        ICONS.put(R.id.menu_filter_trash, R.drawable.twotone_delete_24);
        OPENS.addAll(Arrays.asList(
                R.id.menu_theme,
                R.id.menu_search_folder,
                R.id.menu_edit_account_name,
                R.id.menu_edit_account_color,
                R.id.menu_filter_language,
                R.id.menu_export,
                R.id.menu_import,
                R.id.menu_import_file,
                R.id.menu_changelog,
                R.id.menu_attribution,
                R.id.menu_pwned,
                R.id.menu_show,
                R.id.menu_response));
        DESTRUCTIVE.addAll(Arrays.asList(
                R.id.menu_delete,
                R.id.menu_default,
                R.id.menu_delete_all,
                R.id.menu_reset_order,
                R.id.menu_clear));
    }

    static {
        for (int id : new int[]{
                R.string.title_synchronize_now,
                R.string.title_synchronize_more,
                R.string.title_synchronize_enabled,
                R.string.title_synchronize_batch_enable,
                R.string.title_download_batch_enable,
                R.string.title_execute_rules,
                R.string.title_rule_execute,
                R.string.title_setup_oauth_authorize,
                R.string.title_subscribe,
                R.string.title_setup_gmail,
                R.string.title_setup_imap,
                R.string.title_setup_other,
                R.string.title_setup_pop3,
                R.string.title_imap,
                R.string.title_pop3,
                R.string.title_enabled,
                R.string.title_rule_enabled,
                R.string.title_account_auth_password,
                R.string.title_account_auth_update,
                R.string.menu_operations,
                R.string.menu_rules,
                R.id.menu_force_sync,
                R.id.menu_enabled,
                R.id.menu_subscribed_only,
                R.id.menu_sort_sync,
                R.id.menu_apply_all})
            ROLES.put(id, R.color.customColorMenuConnect);
        for (int id : new int[]{
                R.string.title_mark_all_read,
                R.string.title_seen,
                R.string.title_unseen,
                R.string.title_compose,
                R.string.title_flag,
                R.string.title_unflag,
                R.string.title_flag_color,
                R.string.title_manage_keywords,
                R.string.title_importance_high,
                R.string.title_importance_low,
                R.string.title_importance_normal,
                R.string.title_snooze,
                R.string.title_send_dialog,
                R.string.title_raw_send,
                R.string.title_insert_brackets,
                R.string.title_insert_line,
                R.string.title_insert_quotes,
                R.string.title_paste_as_quote,
                R.string.title_edit_plain_text,
                R.string.title_edit_formatted_text,
                R.string.title_notify_folder,
                R.string.title_notify_batch_enable,
                R.string.title_create_channel,
                R.string.title_edit_channel,
                R.string.title_answer_favorite,
                R.string.title_primary,
                R.string.title_pin,
                R.string.title_summarize,
                R.string.title_rule_select_sound_audio,
                R.string.title_rule_select_sound_ringtone,
                R.string.menu_answers,
                R.id.menu_response,
                R.id.menu_unseen,
                R.id.menu_raw_send_message,
                R.id.menu_raw_send_thread,
                R.id.menu_filter_seen,
                R.id.menu_filter_unseen,
                R.id.menu_filter_flagged,
                R.id.menu_filter_unflagged,
                R.id.menu_filter_sent,
                R.id.menu_show_flagged,
                R.id.menu_sort_unread_atop})
            ROLES.put(id, R.color.customColorMenuMessage);
        for (int id : new int[]{
                R.string.title_edit_contact,
                R.string.title_insert_contact,
                R.string.title_search_sender,
                R.string.title_advanced_create_alias,
                R.string.menu_faq,
                R.string.menu_issue,
                R.string.menu_translate,
                R.string.menu_rate,
                R.string.title_setup_help,
                R.id.menu_show_users,
                R.id.menu_filter_unknown,
                R.id.menu_edit_account_name})
            ROLES.put(id, R.color.customColorMenuContact);
        for (int id : new int[]{
                R.string.title_copy,
                R.string.title_copy_to,
                R.string.title_clipboard_copy,
                R.string.title_share,
                R.string.title_export_messages,
                R.string.title_import_messages,
                R.string.title_save,
                R.string.title_zip,
                R.string.title_move,
                R.string.title_move_to_account,
                R.string.title_move_to_folder,
                R.string.title_archive,
                R.string.title_create_sub_folder,
                R.string.title_setup_reorder_accounts,
                R.string.title_setup_reorder_folders,
                R.id.menu_export,
                R.id.menu_import,
                R.id.menu_import_file,
                R.id.menu_save,
                R.id.menu_filter_duplicates})
            ROLES.put(id, R.color.customColorMenuShare);
        for (int id : new int[]{
                R.string.title_hide,
                R.string.title_hide_folder,
                R.string.title_navigation_folder_hide,
                R.string.title_notify_batch_disable,
                R.string.title_synchronize_batch_disable,
                R.string.title_download_batch_disable,
                R.string.title_spam,
                R.string.title_report_spam,
                R.string.title_folder_junk,
                R.string.title_full_sync_stop,
                R.string.title_setup_close,
                R.string.title_account_ondemand,
                R.string.title_answer_hide,
                R.string.title_unified_inbox_delete,
                R.string.title_advanced_never_favorite,
                R.string.menu_privacy,
                R.id.menu_hide,
                R.id.menu_filter_snoozed,
                R.id.menu_pwned})
            ROLES.put(id, R.color.customColorMenuBlock);
    }

    // The colour of the icon of a row, by what the row does: delete is red,
    // connect green, message blue, contact cyan, share orange, block magenta
    // and everything that only views or opens something keeps the accent
    static int icon(Context context, int id) {
        if (isDestructive(id))
            return warning(context);
        int role = ROLES.get(id);
        return (role == 0 ? accent(context) : color(context, role));
    }

    // The same for a row outside a popup menu (the navigation menu), or null
    // when the table has no role for it
    static Integer role(Context context, int id) {
        try {
            if (!CustomThemeColors.isCustomTheme(context))
                return null;
            int role = ROLES.get(id);
            return (role == 0 ? null : color(context, role));
        } catch (Throwable ex) {
            Log.e(ex);
            return null;
        }
    }

    static boolean opens(int id) {
        return OPENS.contains(id);
    }

    static boolean isDestructive(int id) {
        return DESTRUCTIVE.contains(id);
    }

    static int accent(Context context) {
        return color(context, R.color.customColorAccent);
    }

    static int warning(Context context) {
        return color(context, R.color.customColorMenuDelete);
    }

    // The colour of a switch that is off: an opaque darker accent, 55 % of the
    // way from the background to the accent, never a faded one.
    static int switchOff(Context context) {
        return ColorUtils.blendARGB(color(context, R.color.customColorBackground), accent(context), 0.55f);
    }

    // The vertical offset at which a menu has to be shown to get its full
    // height, or null when it fits below its anchor anyway. A popup is given
    // the larger of the room above and the room below its anchor and scrolls
    // beyond that, however much taller the screen is. Shifting it up by what
    // is missing below lets it use the whole screen. The height is estimated
    // from the rows as PillMenuItemView draws them, a little generously, since
    // an estimate that is too large only moves the menu slightly higher.
    static Integer verticalOffset(Context themed, View anchor, Menu menu) {
        try {
            if (anchor == null || !(menu instanceof MenuBuilder) ||
                    !CustomThemeColors.isCustomTheme(themed))
                return null;

            float density = themed.getResources().getDisplayMetrics().density;
            TypedValue tv = new TypedValue();
            if (!themed.getTheme().resolveAttribute(
                    androidx.appcompat.R.attr.dropdownListPreferredItemHeight, tv, true))
                return null;
            float row = tv.getDimension(themed.getResources().getDisplayMetrics());

            MenuBuilder builder = (MenuBuilder) menu;
            float needed = 24 * density;
            Integer group = null;
            for (MenuItemImpl item : builder.getVisibleItems()) {
                if (item.isCheckable())
                    needed += TOGGLE_HEIGHT_DP * density;
                else if (isHeader(item))
                    needed += row;
                else
                    needed += row + 2 * PILL_MARGIN_DP * density;
                if (group != null && group != item.getGroupId() && builder.isGroupDividerEnabled())
                    needed += 9 * density;
                group = item.getGroupId();
            }

            boolean overlap;
            TypedArray ta = themed.obtainStyledAttributes(null,
                    new int[]{androidx.appcompat.R.attr.overlapAnchor},
                    androidx.appcompat.R.attr.popupMenuStyle, 0);
            try {
                overlap = ta.getBoolean(0, false);
            } finally {
                ta.recycle();
            }

            Rect frame = new Rect();
            anchor.getWindowVisibleDisplayFrame(frame);
            int[] pos = new int[2];
            anchor.getLocationOnScreen(pos);
            int base = pos[1] + (overlap ? 0 : anchor.getHeight());

            float below = frame.bottom - base;
            if (needed <= below)
                return null;
            return Math.round(Math.max(frame.top - base, below - needed));
        } catch (Throwable ex) {
            Log.e(ex);
            return null;
        }
    }

    // The disabled title line a menu opens with
    static boolean isHeader(MenuItem item) {
        return (!item.isEnabled() && item.getItemId() == 0 && !item.hasSubMenu());
    }

    // The line between two groups of a menu: an opaque darker accent, bright
    // enough to be seen on the background, quieter than a pill frame
    static int divider(Context context) {
        return ColorUtils.blendARGB(color(context, R.color.customColorBackground), accent(context), 0.7f);
    }

    private static int color(Context context, int res) {
        Integer override = CustomThemeColors.getOverrideForColorRes(context, res);
        return (override == null ? ContextCompat.getColor(context, res) : override);
    }

    // Gives the items of a menu their table icon, in the colour of their row,
    // and makes the menu show its icons. A menu that carries its icons inside
    // the titles (PopupMenuLifecycle.insertIcons) is left alone, and so is an
    // item that sits on the toolbar, where an icon would replace its text.
    static void decorate(Context context, Menu menu) {
        try {
            if (menu == null || !CustomThemeColors.isCustomTheme(context))
                return;

            for (int i = 0; i < menu.size(); i++) {
                CharSequence title = menu.getItem(i).getTitle();
                if (title instanceof Spanned &&
                        ((Spanned) title).getSpans(0, title.length(), ImageSpan.class).length > 0)
                    return;
            }

            boolean any = false;
            for (int i = 0; i < menu.size(); i++) {
                MenuItem item = menu.getItem(i);
                if (item.hasSubMenu())
                    decorate(context, item.getSubMenu());

                if (item instanceof MenuItemImpl &&
                        (((MenuItemImpl) item).requestsActionButton() ||
                                ((MenuItemImpl) item).requiresActionButton()))
                    continue;

                if (item.getIcon() != null) {
                    any = true;
                    continue;
                }

                int res = ICONS.get(item.getItemId());
                if (res == 0)
                    continue;

                Drawable icon = ContextCompat.getDrawable(context, res);
                if (icon == null)
                    continue;
                icon = icon.mutate();
                icon.setTint(icon(context, item.getItemId()));
                if (!item.isEnabled())
                    icon.setAlpha(DISABLED_ALPHA);
                item.setIcon(icon);
                any = true;
            }

            if (any && menu instanceof MenuBuilder)
                ((MenuBuilder) menu).setOptionalIconsVisible(true);
        } catch (Throwable ex) {
            Log.e(ex);
        }
    }
}
