package com.radminplus.client;

import com.google.gson.Gson;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class UpdateChecker {
    public static boolean updateAvailable = false;
    public static String updateVersion = null;
    public static String updateUrl = null;
    public static String updateModrinthUrl = null;
    public static List<String> updateChangelog = null;
    public static boolean mandatory = false;
    public static boolean userSkipped = false;

    // Custom text overrides from JSON
    public static String updateTitle = null;
    public static String updateGithubBtn = null;
    public static String updateModrinthBtn = null;
    public static String updateContinueBtn = null;
    public static String updateExitBtn = null;

    private static boolean checked = false;

    public static void check() {
        if (checked) return;
        checked = true;

        CompletableFuture.runAsync(() -> {
            try {
                URL url = new URL("https://raw.githubusercontent.com/Gudev-java/RadminPlus/main/update.json?t=" + System.currentTimeMillis());
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "RadminPlusMod");

                if (conn.getResponseCode() == 200) {
                    try (InputStreamReader reader = new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)) {
                        UpdateData data = new Gson().fromJson(reader, UpdateData.class);
                        if (data != null && data.version != null) {
                            String currentVersion = net.fabricmc.loader.api.FabricLoader.getInstance()
                                    .getModContainer("radminplus")
                                    .map(container -> container.getMetadata().getVersion().getFriendlyString())
                                    .orElse("1.0.0");

                            if (isNewerVersion(currentVersion, data.version)) {
                                updateVersion = data.version;
                                updateUrl = data.url != null ? data.url : "https://github.com/Gudev-java/RadminPlus/releases";
                                updateModrinthUrl = data.modrinth_url;
                                updateChangelog = data.changelog;
                                mandatory = data.mandatory;

                                updateTitle = data.title;
                                updateGithubBtn = data.github_btn;
                                updateModrinthBtn = data.modrinth_btn;
                                updateContinueBtn = data.continue_btn;
                                updateExitBtn = data.exit_btn;

                                updateAvailable = true;
                                System.out.println("[RadminPlus] New update available: " + data.version + " (Mandatory: " + mandatory + ")");
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("[RadminPlus] Failed to check for updates: " + e.getMessage());
            }
        });
    }

    private static boolean isNewerVersion(String current, String remote) {
        try {
            String[] currParts = current.split("\\.");
            String[] remParts = remote.split("\\.");
            int length = Math.max(currParts.length, remParts.length);
            for (int i = 0; i < length; i++) {
                int curr = i < currParts.length ? Integer.parseInt(currParts[i].replaceAll("[^0-9]", "")) : 0;
                int rem = i < remParts.length ? Integer.parseInt(remParts[i].replaceAll("[^0-9]", "")) : 0;
                if (rem > curr) return true;
                if (curr > rem) return false;
            }
        } catch (Exception e) {
            return !current.equals(remote);
        }
        return false;
    }

    public static class UpdateData {
        public String version;
        public String url;
        public String modrinth_url;
        public boolean mandatory;
        public List<String> changelog;

        // Custom localization keys
        public String title;
        public String github_btn;
        public String modrinth_btn;
        public String continue_btn;
        public String exit_btn;
    }
}
