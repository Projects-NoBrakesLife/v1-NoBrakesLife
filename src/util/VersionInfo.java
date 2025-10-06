package util;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class VersionInfo {
    private static String cachedVersion = null;
    private static String cachedBranch = null;
    private static String cachedCommitHash = null;

    public static String getVersion() {
        if (cachedVersion == null) {
            loadVersionInfo();
        }
        return cachedVersion != null ? cachedVersion : "Unknown";
    }

    public static String getBranch() {
        if (cachedBranch == null) {
            loadVersionInfo();
        }
        return cachedBranch != null ? cachedBranch : "Unknown";
    }

    public static String getCommitHash() {
        if (cachedCommitHash == null) {
            loadVersionInfo();
        }
        return cachedCommitHash != null ? cachedCommitHash : "Unknown";
    }

    public static String getFullVersion() {
        if (cachedVersion == null) {
            loadVersionInfo();
        }

        StringBuilder version = new StringBuilder();

        if (cachedBranch != null) {
            version.append(cachedBranch);
        }

        if (cachedCommitHash != null) {
            if (version.length() > 0) {
                version.append(" @ ");
            }
            version.append(cachedCommitHash);
        }

        return version.length() > 0 ? version.toString() : "Development Build";
    }

    private static void loadVersionInfo() {
        try {
            // Get commit hash
            Process commitProcess = Runtime.getRuntime().exec("git rev-parse --short HEAD");
            BufferedReader commitReader = new BufferedReader(new InputStreamReader(commitProcess.getInputStream()));
            cachedCommitHash = commitReader.readLine();
            commitReader.close();
            commitProcess.waitFor();

            // Get branch name
            Process branchProcess = Runtime.getRuntime().exec("git rev-parse --abbrev-ref HEAD");
            BufferedReader branchReader = new BufferedReader(new InputStreamReader(branchProcess.getInputStream()));
            cachedBranch = branchReader.readLine();
            branchReader.close();
            branchProcess.waitFor();

            // Get commit count as version number
            Process countProcess = Runtime.getRuntime().exec("git rev-list --count HEAD");
            BufferedReader countReader = new BufferedReader(new InputStreamReader(countProcess.getInputStream()));
            String count = countReader.readLine();
            countReader.close();
            countProcess.waitFor();

            if (count != null && !count.isEmpty()) {
                cachedVersion = "v1.0." + count;
            }

        } catch (Exception e) {
            // If git is not available, use default values
            cachedVersion = "v1.0.0";
            cachedBranch = "unknown";
            cachedCommitHash = "unknown";
        }
    }

    public static void main(String[] args) {
        System.out.println("Version: " + getVersion());
        System.out.println("Branch: " + getBranch());
        System.out.println("Commit: " + getCommitHash());
        System.out.println("Full: " + getFullVersion());
    }
}
