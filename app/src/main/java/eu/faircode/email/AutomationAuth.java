package eu.faircode.email;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import java.security.MessageDigest;
import java.security.SecureRandom;

/**
 * External-automation gate for {@link StateExportReceiver} and {@link AutomationProvider}:
 * a master switch that is ON, and a shared secret that is only asked for when 白い熊 has
 * said to ask for it — the v2 shape of the sister-app contract, matching 自由作業盤's own
 * AutomationAuth so every 白い熊 app behaves the same.
 *
 * <p><b>Why the switch ships on and the token ships off.</b> v1 shipped every app closed:
 * a caller had to present a 48-character secret 白い熊 had pasted from this app's settings
 * into the caller's. A pasted secret cannot survive a wipe, and the case the family now
 * exists to serve is 応用管理 restoring apps <em>and their data</em> onto a clean phone,
 * where nothing has been configured and nobody has pasted anything. A gate that only works
 * once the phone is already set up is no gate for setting the phone up.
 *
 * <p><b>A token sent to an app that does not require one is IGNORED, never refused.</b>
 * Tokens live in task arguments and workspace variables that outlive the setting they were
 * pasted for, and another app on the same batch may still want one. Refusing it would turn
 * "白い熊 turned a switch off" into "half the batch mysteriously fails", which is exactly
 * the friction the switch exists to remove.
 *
 * <p><b>The check lives in {@link #refuse}, in one place.</b> Two checks written out at
 * each entry point is how "disabled" and "bad token" drift apart across forty-two apps.
 *
 * <p>Device-local by design: these live in their OWN preferences file, not the default
 * store {@link StateExport} serializes, so the token never travels in a backup ZIP and
 * never leaves the phone.
 */
public class AutomationAuth {
    private static final String PREFS_FILE = "shiroikuma_automation";
    private static final String KEY_ENABLED = "automation_enabled";
    private static final String KEY_REQUIRE_TOKEN = "automation_require_token";
    private static final String KEY_TOKEN = "automation_token";

    private static final int TOKEN_BYTES = 24;

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE);
    }

    /**
     * Default ON: this app answers the batch out of the box. It stays a switch rather than
     * being removed because it is the only way to close one app off, and a feature that can
     * be turned on but never off is one 白い熊 cannot retreat from.
     */
    static boolean enabled(Context context) {
        return prefs(context).getBoolean(KEY_ENABLED, true);
    }

    static void setEnabled(Context context, boolean value) {
        prefs(context).edit().putBoolean(KEY_ENABLED, value).apply();
    }

    /** Default OFF: the token is an extra a caller may be asked for, not the gate. */
    static boolean requireToken(Context context) {
        return prefs(context).getBoolean(KEY_REQUIRE_TOKEN, false);
    }

    static void setRequireToken(Context context, boolean value) {
        prefs(context).edit().putBoolean(KEY_REQUIRE_TOKEN, value).apply();
    }

    /** The shared secret; generated on first read so the settings row always shows a value. */
    static String token(Context context) {
        String token = prefs(context).getString(KEY_TOKEN, null);
        if (token != null && !token.isEmpty())
            return token;
        return regenerateToken(context);
    }

    static String regenerateToken(Context context) {
        byte[] bytes = new byte[TOKEN_BYTES];
        new SecureRandom().nextBytes(bytes);
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes)
            sb.append(Character.forDigit((b >> 4) & 0xF, 16))
                    .append(Character.forDigit(b & 0xF, 16));
        String token = sb.toString();
        prefs(context).edit().putString(KEY_TOKEN, token).apply();
        return token;
    }

    /** First eight and last eight hex characters, for the settings row. */
    static String abbreviate(String token) {
        if (token == null || token.length() <= 20)
            return (token == null ? "" : token);
        return token.substring(0, 8) + "…" + token.substring(token.length() - 8);
    }

    /**
     * The whole gate, in one answer: {@code null} means proceed, anything else is the exact
     * {@code ERROR:} line to reply with. "automation disabled" and "bad token" stay distinct
     * because they debug differently.
     *
     * <p>The candidate is only looked at when {@link #requireToken} is on. When it is off a
     * token that arrived anyway is dropped on the floor — see the class comment for why that
     * is required rather than merely tolerant.
     */
    @Nullable
    static String refuse(Context context, @Nullable String candidate) {
        if (!enabled(context))
            return "ERROR:automation disabled";
        if (requireToken(context) && !isTokenValid(context, candidate))
            return "ERROR:bad token";
        return null;
    }

    /**
     * True when the caller's token matches the stored secret (constant-time). Kept for the
     * case where the token IS required; {@link #refuse} is what callers should ask.
     */
    static boolean isTokenValid(Context context, @Nullable String candidate) {
        if (candidate == null || candidate.isEmpty())
            return false;
        return MessageDigest.isEqual(candidate.getBytes(), token(context).getBytes());
    }
}
