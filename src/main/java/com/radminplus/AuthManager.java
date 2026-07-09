package com.radminplus;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AuthManager {
    public static final Set<UUID> LOGGED_IN_PLAYERS = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, UserData> database = new ConcurrentHashMap<>();
    private static File dbFile = null;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static class UserData {
        public String username;
        public String hash;
        public String salt;
        public String ip;

        public UserData(String username, String hash, String salt, String ip) {
            this.username = username;
            this.hash = hash;
            this.salt = salt;
            this.ip = ip;
        }
    }

    public static void init(MinecraftServer server) {
        LOGGED_IN_PLAYERS.clear();
        database.clear();
        try {
            dbFile = server.getWorldPath(LevelResource.ROOT).resolve("radminplus_auth.json").toFile();
            if (dbFile.exists()) {
                try (Reader reader = new FileReader(dbFile, StandardCharsets.UTF_8)) {
                    Map<String, UserData> loaded = GSON.fromJson(reader, new TypeToken<Map<String, UserData>>(){}.getType());
                    if (loaded != null) {
                        for (Map.Entry<String, UserData> entry : loaded.entrySet()) {
                            database.put(UUID.fromString(entry.getKey()), entry.getValue());
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[Server] Failed to initialize Auth database:");
            e.printStackTrace();
        }
    }

    private static int authReminderCounter = 0;

    public static void tick(MinecraftServer server) {
        if (ModInit.RULE_REGISTER == null || !server.getWorldData().getGameRules().get(ModInit.RULE_REGISTER)) {
            return;
        }

        authReminderCounter++;
        if (authReminderCounter >= 100) { // every 5 seconds
            authReminderCounter = 0;
            for (net.minecraft.server.level.ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (!LOGGED_IN_PLAYERS.contains(player.getUUID())) {
                    if (isRegistered(player.getUUID())) {
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§eЭтот аккаунт зарегистрирован. Войдите: §6/login <пароль>"));
                    } else {
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§eЗарегистрируйтесь: §6/register <пароль> <повтор_пароля>"));
                    }
                }
            }
        }
    }

    public static boolean isRegistered(UUID uuid) {
        return database.containsKey(uuid);
    }

    public static String getLastIp(UUID uuid) {
        UserData data = database.get(uuid);
        return data != null ? data.ip : null;
    }

    public static boolean register(UUID uuid, String username, String password, String ip) {
        if (isRegistered(uuid)) return false;

        String salt = generateSalt();
        String hash = hashPassword(password, salt);
        database.put(uuid, new UserData(username, hash, salt, ip));
        saveDatabase();
        return true;
    }

    public static boolean checkLogin(UUID uuid, String password, String ip) {
        UserData data = database.get(uuid);
        if (data == null) return false;
        String hash = hashPassword(password, data.salt);
        boolean success = hash.equals(data.hash);
        if (success) {
            data.ip = ip; // Update IP on successful manual login
            saveDatabase();
        }
        return success;
    }

    public static boolean changePassword(UUID uuid, String oldPassword, String newPassword) {
        UserData data = database.get(uuid);
        if (data == null) return false;
        
        // We temporarily verify with data.ip (or passing null as we check manually)
        String hash = hashPassword(oldPassword, data.salt);
        if (!hash.equals(data.hash)) return false;

        String salt = generateSalt();
        String newHash = hashPassword(newPassword, salt);
        data.salt = salt;
        data.hash = newHash;
        saveDatabase();
        return true;
    }

    public static boolean reset(UUID uuid) {
        if (!database.containsKey(uuid)) return false;
        database.remove(uuid);
        LOGGED_IN_PLAYERS.remove(uuid);
        saveDatabase();
        return true;
    }

    private static String generateSalt() {
        byte[] saltBytes = new byte[16];
        new SecureRandom().nextBytes(saltBytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : saltBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static String hashPassword(String password, String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((password + salt).getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void saveDatabase() {
        if (dbFile == null) return;
        try {
            Map<String, UserData> toSave = new HashMap<>();
            for (Map.Entry<UUID, UserData> entry : database.entrySet()) {
                toSave.put(entry.getKey().toString(), entry.getValue());
            }
            try (Writer writer = new FileWriter(dbFile, StandardCharsets.UTF_8)) {
                GSON.toJson(toSave, writer);
            }
        } catch (Exception e) {
            System.err.println("[Server] Failed to save Auth database:");
            e.printStackTrace();
        }
    }
}
