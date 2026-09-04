package eu.faircode.email;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.pm.SigningInfo;
import android.os.Binder;
import android.os.Build;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Who is allowed through the automation door, and how that is decided.
 *
 * <p><b>Why not a token.</b> The token this replaces was a 48-character secret 白い熊 pasted
 * from one app's settings into another's. It cannot survive a wipe, which is fatal for the
 * case the whole family now exists to serve: 応用管理 restoring apps and their data onto a
 * clean phone, where nothing is configured yet.
 *
 * <p><b>Why not a {@code shiroikuma.*} prefix.</b> Because that is not an identity. What
 * makes {@link android.content.ContentProvider#getCallingPackage()} worth anything is that a
 * package name <b>cannot be taken while the real package is installed</b> — package names are
 * not a namespace anyone owns, so any sideloaded app may call itself {@code shiroikuma.evil}
 * and pass a prefix test. Since the caller supplies the file descriptor an export is written
 * into, a prefix check would hand such an app the complete data of every sister app in turn:
 * strictly weaker than the token it replaces.
 *
 * <p><b>What is actually checked, in order</b> — each exists because the one before it is not
 * enough:
 * <ol>
 * <li>An exact name from {@link #CALLERS}. Never a prefix.</li>
 * <li>The uid agrees. {@code getCallingPackage()} reflects the caller's <em>declared</em>
 * attribution, and packages sharing a uid are not distinguished by it, so it is confirmed
 * against the uid the kernel reports — that answer cannot be borrowed.</li>
 * <li>The signing certificate matches a pinned hash. This closes the real gap:
 * <em>whichever caller package is absent from the device is a name anyone can take</em>, and a
 * clean phone is precisely a device where not everything is installed yet. The moment the
 * assumption is weakest is the moment it is most needed.</li>
 * </ol>
 *
 * <p>App-independent: this file is the same in every sister app, deliberately.
 */
class AutomationCallers {
    /**
     * The apps allowed to drive this one's data door.
     *
     * <p>応用管理 backs up and restores; 自由作業盤 runs the 保存復元 batch. Nothing else has
     * any business exporting this app's mail, and an entry added here is a deliberate act.
     *
     * <p>Where the hashes come from, so the next person can re-derive them rather than trust
     * them: {@code apksigner verify --print-certs <that app's signed release APK>}. Every app
     * in the family has <b>its own keystore</b> — some forty of them — so there is no shared
     * signing key to compare against and each caller must be pinned by name. That is also why
     * a {@code protectionLevel="signature"} permission was never an option here.
     *
     * <p><b>If a caller's key is ever rotated its calls stop working and the fix is here.</b>
     * That is the intended failure: a signing key changing without anyone noticing is exactly
     * what a pin exists to catch.
     */
    private static final Map<String, String> CALLERS;

    static {
        Map<String, String> callers = new HashMap<>();
        callers.put("shiroikuma.oyokanri",
                "9c585f4d118cb97ff653f949a8872875548403b9083ce6b9baa2e8f0c55ac6cc");
        callers.put("shiroikuma.jiyusagyoban",
                "efd0d352192651593a92288ecdc64fc87262ec8648c24ed8f51a5587d46ac602");
        CALLERS = Collections.unmodifiableMap(callers);
    }

    /**
     * Why the check answers a STRING and not a boolean: a refusal that says only "no" is a
     * refusal nobody can debug from the other side of an IPC boundary. Each of these is a
     * different mistake with a different fix, and the caller shows them to 白い熊 verbatim.
     *
     * @return {@code null} to proceed, otherwise the exact {@code ERROR:} line to answer with.
     */
    @Nullable
    static String refuse(Context context, @Nullable String declared) {
        if (declared == null || declared.isEmpty())
            return "ERROR:caller unknown";

        String pin = CALLERS.get(declared);
        if (pin == null)
            return "ERROR:caller not permitted: " + declared;

        // The kernel's answer, not the caller's. A package may declare an attribution it does
        // not own; the uid cannot be borrowed.
        String[] real;
        try {
            real = context.getPackageManager().getPackagesForUid(Binder.getCallingUid());
        } catch (Throwable ex) {
            Log.w(ex);
            real = null;
        }
        List<String> names = (real == null
                ? Collections.<String>emptyList() : Arrays.asList(real));
        if (!names.contains(declared))
            return "ERROR:caller uid mismatch: " + declared;

        String signature = signingSha256(context, declared);
        if (signature == null)
            return "ERROR:caller signature unreadable: " + declared;

        // Constant-time, like the token compare it replaces — the value is a public hash, but
        // the habit is worth keeping and costs nothing.
        if (!MessageDigest.isEqual(signature.getBytes(), pin.getBytes()))
            return "ERROR:caller signature mismatch: " + declared;

        return null;
    }

    /**
     * The SHA-256 of the caller's current signing certificate, lower-case hex.
     *
     * <p>{@code signingInfo} rather than the deprecated {@code signatures}: a rotated key
     * reports its whole history and we want the certificate actually in force. This app's
     * minSdk is 23, well below the API 28 those live at, and on an older device the flag is
     * accepted while {@code signingInfo} comes back null — so WITHOUT the branch below the
     * door would refuse every caller, a total failure that never appears on 白い熊's phone
     * (API 31) and would only surface on an older one. The deprecated array is the correct
     * answer there, not a compromise: before key rotation existed, {@code signatures} WAS the
     * signing certificate.
     *
     * <p>Exactly one signer, or we decline to guess. "Several signers, one of which matches"
     * is a question about key rotation that nothing in this family needs to answer — every app
     * here has one key and has never rotated it.
     */
    @Nullable
    private static String signingSha256(Context context, String pkg) {
        try {
            PackageManager pm = context.getPackageManager();
            Signature[] certs = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
                    ? currentSigners(pm, pkg) : legacySigners(pm, pkg));
            if (certs == null || certs.length != 1)
                return null;

            byte[] digest = MessageDigest.getInstance("SHA-256").digest(certs[0].toByteArray());
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest)
                sb.append(Character.forDigit((b >> 4) & 0xF, 16))
                        .append(Character.forDigit(b & 0xF, 16));
            return sb.toString();
        } catch (Throwable ex) {
            Log.w(ex);
            return null;
        }
    }

    /**
     * Kept in its own method so {@link SigningInfo} is only ever resolved on a device that
     * has it. Naming an API 28 type in a method that also runs on API 23 leaves the verifier
     * to sort it out at first call, which is not a bet worth taking for a door that fails
     * closed.
     */
    @RequiresApi(api = Build.VERSION_CODES.P)
    private static Signature[] currentSigners(PackageManager pm, String pkg) throws Throwable {
        SigningInfo info = pm.getPackageInfo(pkg,
                PackageManager.GET_SIGNING_CERTIFICATES).signingInfo;
        return (info == null ? null : info.getApkContentsSigners());
    }

    @SuppressWarnings("deprecation")
    private static Signature[] legacySigners(PackageManager pm, String pkg) throws Throwable {
        return pm.getPackageInfo(pkg, PackageManager.GET_SIGNATURES).signatures;
    }
}
