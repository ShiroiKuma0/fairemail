package eu.faircode.email;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import java.security.MessageDigest;
import java.security.SecureRandom;

/**
 * External-automation gate for {@link StateExportReceiver}: a master switch plus a
 * shared secret that every automation broadcast must carry — the same model as the
 * renrakusaki fork (automation_enabled / automation_token) and 自由作業盤's
 * AutomationAuth, so every 白い熊 app looks and behaves the same.
 *
 * <p>Device-local by design: these live in their OWN preferences file, not the default
 * store {@link StateExport} serializes, so the token never travels in a backup ZIP and
 * never leaves the phone.
 */
public class AutomationAuth {
    private static final String PREFS_FILE = "shiroikuma_automation";
    private static final String KEY_ENABLED = "automation_enabled";
    private static final String KEY_TOKEN = "automation_token";

    private static final int TOKEN_BYTES = 24;

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE);
    }

    /** Default OFF: nothing is reachable until 白い熊 turns the switch on. */
    static boolean enabled(Context context) {
        return prefs(context).getBoolean(KEY_ENABLED, false);
    }

    static void setEnabled(Context context, boolean value) {
        prefs(context).edit().putBoolean(KEY_ENABLED, value).apply();
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
     * True when the caller's token matches the stored secret (constant-time). The enabled
     * check is kept separate so callers can report "automation disabled" and "bad token"
     * as distinct failures — they debug differently.
     */
    static boolean isTokenValid(Context context, @Nullable String candidate) {
        if (candidate == null || candidate.isEmpty())
            return false;
        return MessageDigest.isEqual(candidate.getBytes(), token(context).getBytes());
    }
}
