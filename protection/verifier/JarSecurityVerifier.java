package protection.verifier;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Automated Verification & Security Scanner (Phases 10, 23, 24).
 * Validates the protected release artifact against all integrity, ABI,
 * vendor artifact hash, and secret leak invariants.
 */
public final class JarSecurityVerifier {

    private static final String EXPECTED_SPOOFER_SHA256 = "c4dabf0cc8bca6f8b582f0ee7c4aff43c535f4d282458cbf48baa32131159ea9";
    private static final String SPOOFER_PATH = "META-INF/jars/ClientSpoofer-1.21.11-1.4.0.jar";
    private static final String SPOOFER_LICENSE = "META-INF/licenses/ClientSpoofer-MIT.txt";

    private JarSecurityVerifier() {}

    public static void verify(File jarFile) throws Exception {
        if (jarFile == null || !jarFile.exists() || !jarFile.isFile()) {
            throw new IllegalArgumentException("Target JAR does not exist: " + jarFile);
        }

        System.out.println("================================================================================");
        System.out.println("[JarSecurityVerifier] Auditing: " + jarFile.getAbsolutePath());
        System.out.println("================================================================================");

        try (ZipFile zip = new ZipFile(jarFile)) {
            Set<String> entries = new HashSet<>();
            Enumeration<? extends ZipEntry> en = zip.entries();
            while (en.hasMoreElements()) {
                entries.add(en.nextElement().getName());
            }

            // 1. Verify fabric.mod.json
            ZipEntry fabricEntry = zip.getEntry("fabric.mod.json");
            if (fabricEntry == null) {
                throw new IllegalStateException("FAIL: fabric.mod.json is missing from release JAR!");
            }
            String fabricJsonText = readEntry(zip, fabricEntry);
            JsonObject modJson = JsonParser.parseString(fabricJsonText).getAsJsonObject();

            String modId = modJson.has("id") ? modJson.get("id").getAsString() : "";
            if (!"activity".equals(modId)) {
                throw new IllegalStateException("FAIL: Unexpected mod ID: " + modId + " (expected 'activity')");
            }
            System.out.println("  [PASS] fabric.mod.json exists with ID: " + modId);

            // 2. Verify Entrypoints
            JsonObject entrypoints = modJson.getAsJsonObject("entrypoints");
            if (entrypoints == null) {
                throw new IllegalStateException("FAIL: entrypoints block missing in fabric.mod.json");
            }

            JsonArray clientEps = entrypoints.getAsJsonArray("client");
            if (clientEps == null || clientEps.isEmpty()) {
                throw new IllegalStateException("FAIL: 'client' entrypoint missing");
            }
            for (JsonElement epElem : clientEps) {
                String epClass = epElem.getAsString().replace('.', '/') + ".class";
                if (!entries.contains(epClass)) {
                    throw new IllegalStateException("FAIL: Client entrypoint class missing in JAR: " + epClass);
                }
                System.out.println("  [PASS] Client entrypoint resolved: " + epElem.getAsString());
            }

            JsonArray ecoEps = entrypoints.getAsJsonArray("nivorat:settings_v1");
            if (ecoEps != null) {
                for (JsonElement epElem : ecoEps) {
                    String epClass = epElem.getAsString().replace('.', '/') + ".class";
                    if (!entries.contains(epClass)) {
                        throw new IllegalStateException("FAIL: Nivorat ecosystem entrypoint class missing in JAR: " + epClass);
                    }
                    System.out.println("  [PASS] Ecosystem entrypoint resolved: " + epElem.getAsString());
                }
            }

            // 3. Verify Mixin configs & classes
            JsonArray mixinConfigs = modJson.getAsJsonArray("mixins");
            if (mixinConfigs == null || mixinConfigs.isEmpty()) {
                throw new IllegalStateException("FAIL: mixins declaration missing in fabric.mod.json");
            }

            int totalMixinsChecked = 0;
            for (JsonElement mElem : mixinConfigs) {
                String mixinConfigPath = mElem.getAsString();
                ZipEntry mConfigEntry = zip.getEntry(mixinConfigPath);
                if (mConfigEntry == null) {
                    throw new IllegalStateException("FAIL: Mixin config missing in JAR: " + mixinConfigPath);
                }

                String mixinJsonText = readEntry(zip, mConfigEntry);
                JsonObject mixinJson = JsonParser.parseString(mixinJsonText).getAsJsonObject();
                String pkg = mixinJson.has("package") ? mixinJson.get("package").getAsString() : "";
                JsonArray clientMixins = mixinJson.getAsJsonArray("client");
                if (clientMixins != null) {
                    for (JsonElement cElem : clientMixins) {
                        String className = cElem.getAsString();
                        String fullClassPath = (pkg + "." + className).replace('.', '/') + ".class";
                        if (!entries.contains(fullClassPath)) {
                            throw new IllegalStateException("FAIL: Mixin class not found in JAR: " + fullClassPath + " (config: " + mixinConfigPath + ")");
                        }
                        totalMixinsChecked++;
                    }
                }
                System.out.println("  [PASS] Mixin config verified: " + mixinConfigPath);
            }
            System.out.println("  [PASS] All " + totalMixinsChecked + " mixin implementation classes verified.");

            // 4. Verify Vendor ClientSpoofer JAR & SHA-256
            ZipEntry spooferEntry = zip.getEntry(SPOOFER_PATH);
            if (spooferEntry == null) {
                throw new IllegalStateException("FAIL: Vendor ClientSpoofer JAR missing at: " + SPOOFER_PATH);
            }
            byte[] spooferBytes = readEntryBytes(zip, spooferEntry);
            String actualSha256 = sha256(spooferBytes);
            if (!EXPECTED_SPOOFER_SHA256.equalsIgnoreCase(actualSha256)) {
                throw new IllegalStateException("FAIL: ClientSpoofer SHA-256 mismatch!\n  Expected: " + EXPECTED_SPOOFER_SHA256 + "\n  Actual:   " + actualSha256);
            }
            System.out.println("  [PASS] Vendor ClientSpoofer verified byte-for-byte (SHA-256: " + actualSha256 + ")");

            // 5. Verify Vendor License
            if (!entries.contains(SPOOFER_LICENSE)) {
                throw new IllegalStateException("FAIL: Vendor license missing at: " + SPOOFER_LICENSE);
            }
            System.out.println("  [PASS] Vendor license file present.");

            // 6. Verify Shader Assets
            long shaderCount = entries.stream()
                    .filter(n -> n.startsWith("assets/nivorat/shaders/") && (n.endsWith(".fsh") || n.endsWith(".vsh") || n.endsWith(".glsl")))
                    .count();
            if (shaderCount == 0) {
                throw new IllegalStateException("FAIL: No GLSL shaders found under assets/nivorat/shaders/!");
            }
            System.out.println("  [PASS] GLSL shaders verified (" + shaderCount + " files).");

            // 7. Security: Disallow forbidden/sensitive files
            for (String entry : entries) {
                String lower = entry.toLowerCase();
                if (lower.endsWith(".java")) {
                    throw new IllegalStateException("SECURITY VIOLATION: Source .java file leaked into release JAR: " + entry);
                }
                if (lower.startsWith("src/")) {
                    throw new IllegalStateException("SECURITY VIOLATION: Source tree folder leaked into release JAR: " + entry);
                }
                if (lower.endsWith(".map") || lower.endsWith("mapping.txt") || lower.endsWith("change.log")) {
                    throw new IllegalStateException("SECURITY VIOLATION: Obfuscation mapping/changelog leaked into release JAR: " + entry);
                }
                if (lower.endsWith(".dox") || lower.contains("dasho")) {
                    // Check if it's dasho config
                    if (lower.endsWith(".dox") || lower.endsWith(".dasho")) {
                        throw new IllegalStateException("SECURITY VIOLATION: DashO project file leaked into release JAR: " + entry);
                    }
                }
                if (lower.endsWith(".jks") || lower.endsWith(".keystore") || lower.endsWith(".p12")) {
                    throw new IllegalStateException("SECURITY VIOLATION: Keystore leaked into release JAR: " + entry);
                }
            }
            System.out.println("  [PASS] Absence of sources (.java), mappings, keystores, and DashO configs confirmed.");

            // 8. Security: Scan text entries for leaked credentials / tokens
            for (String entry : entries) {
                if (entry.endsWith(".json") || entry.endsWith(".txt") || entry.endsWith(".properties") || entry.endsWith("MANIFEST.MF")) {
                    ZipEntry ze = zip.getEntry(entry);
                    if (ze != null && ze.getSize() < 100_000) {
                        String text = readEntry(zip, ze);
                        scanForSecrets(entry, text);
                    }
                }
            }
            System.out.println("  [PASS] Repository and text resource secret scan completed without violations.");

            System.out.println("================================================================================");
            System.out.println("[JarSecurityVerifier] ALL VERIFICATION CHECKS PASSED!");
            System.out.println("================================================================================");
        }
    }

    private static void scanForSecrets(String entryName, String content) {
        String lower = content.toLowerCase();
        List<String> suspicious = List.of(
                "ghp_", "github_pat_", "eyj", "bearer ", "-----begin private key-----",
                "-----begin rsa private key-----", "-----begin openconnect private key-----"
        );
        for (String pattern : suspicious) {
            if (lower.contains(pattern)) {
                throw new IllegalStateException("SECURITY VIOLATION: Potential secret pattern '" + pattern + "' detected in " + entryName);
            }
        }
    }

    private static String readEntry(ZipFile zip, ZipEntry entry) throws IOException {
        try (InputStream is = zip.getInputStream(entry)) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static byte[] readEntryBytes(ZipFile zip, ZipEntry entry) throws IOException {
        try (InputStream is = zip.getInputStream(entry)) {
            return is.readAllBytes();
        }
    }

    private static String sha256(byte[] data) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(data);
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: JarSecurityVerifier <path-to-jar>");
            System.exit(1);
        }
        try {
            verify(new File(args[0]));
        } catch (Exception e) {
            System.err.println("\n[ERROR] Verification failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(2);
        }
    }
}
