package com.radminplus;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.MinecraftServer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SkinManager {
    private static final ExecutorService THREAD_POOL = Executors.newCachedThreadPool();
    private static final Gson GSON = new Gson();

    public static void fetchAndApplySkin(ServerPlayer player) {
        String username = player.getGameProfile().name();
        UUID uuid = player.getUUID();
        
        THREAD_POOL.submit(() -> {
            try {
                // 1. Try Mojang (for premium users)
                Property texturesProp = fetchMojangSkin(username);
                
                // 2. Try Ely.by
                if (texturesProp == null) {
                    texturesProp = fetchElyBySkin(username);
                }
                
                // 3. Try TLauncher
                if (texturesProp == null) {
                    texturesProp = fetchTLauncherSkin(username, uuid);
                }
                
                if (texturesProp != null) {
                    final Property finalProp = texturesProp;
                    MinecraftServer server = player.level().getServer();
                    if (server != null) {
                        server.execute(() -> {
                            GameProfile profile = player.getGameProfile();
                            profile.properties().removeAll("textures");
                            profile.properties().put("textures", finalProp);
                            
                            // 1. Broadcast player removal from player list (tab list)
                            server.getPlayerList().broadcastAll(
                                new net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket(java.util.List.of(player.getUUID()))
                            );

                            // 2. Broadcast player add to player list (tab list) with updated textures
                            server.getPlayerList().broadcastAll(
                                new ClientboundPlayerInfoUpdatePacket(
                                    EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER), 
                                    List.of(player)
                                )
                            );

                            // 3. Reload 3D model/entity tracking for other players
                            net.minecraft.server.level.ServerLevel serverLevel = (net.minecraft.server.level.ServerLevel) player.level();
                            net.minecraft.server.level.ChunkMap chunkMap = serverLevel.getChunkSource().chunkMap;
                            ((com.radminplus.accessor.ChunkMapDuck) chunkMap).radminplus$reloadEntity(player);
                        });
                    }
                }
            } catch (Exception e) {
                System.err.println("[RadminPlus] Error fetching skin for " + username + ": " + e.getMessage());
            }
        });
    }

    private static Property fetchMojangSkin(String username) {
        try {
            // Resolve username to UUID
            String uuidStr = getUrlContent("https://api.mojang.com/users/profiles/minecraft/" + username);
            if (uuidStr == null || uuidStr.isEmpty()) return null;
            
            JsonObject obj = GSON.fromJson(uuidStr, JsonObject.class);
            if (obj == null || !obj.has("id")) return null;
            String premiumUuid = obj.get("id").getAsString();
            
            // Fetch profile
            String profileStr = getUrlContent("https://sessionserver.mojang.com/session/minecraft/profile/" + premiumUuid + "?unsigned=false");
            if (profileStr == null || profileStr.isEmpty()) return null;
            
            JsonObject profileObj = GSON.fromJson(profileStr, JsonObject.class);
            if (profileObj == null || !profileObj.has("properties")) return null;
            
            JsonArray props = profileObj.getAsJsonArray("properties");
            for (JsonElement propEl : props) {
                JsonObject prop = propEl.getAsJsonObject();
                if ("textures".equals(prop.get("name").getAsString())) {
                    String value = prop.get("value").getAsString();
                    String signature = prop.has("signature") ? prop.get("signature").getAsString() : "";
                    return new Property("textures", value, signature);
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static Property fetchElyBySkin(String username) {
        try {
            String profileStr = getUrlContent("https://skinsystem.ely.by/textures/signed/" + username + "?unsigned=false");
            if (profileStr == null || profileStr.isEmpty()) return null;
            
            JsonObject profileObj = GSON.fromJson(profileStr, JsonObject.class);
            if (profileObj == null || !profileObj.has("properties")) return null;
            
            JsonArray props = profileObj.getAsJsonArray("properties");
            for (JsonElement propEl : props) {
                JsonObject prop = propEl.getAsJsonObject();
                if ("textures".equals(prop.get("name").getAsString())) {
                    String value = prop.get("value").getAsString();
                    String signature = prop.has("signature") ? prop.get("signature").getAsString() : "";
                    return new Property("textures", value, signature);
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static Property fetchTLauncherSkin(String username, UUID uuid) {
        try {
            String skinInfoStr = getUrlContent("https://auth.tlauncher.org/skin/profile/texture/login/" + username);
            if (skinInfoStr == null || skinInfoStr.isEmpty()) return null;
            
            JsonObject skinObj = GSON.fromJson(skinInfoStr, JsonObject.class);
            if (skinObj == null || !skinObj.has("SKIN")) return null;
            
            JsonObject skinInfo = skinObj.getAsJsonObject("SKIN");
            if (!skinInfo.has("url")) return null;
            String skinUrl = skinInfo.get("url").getAsString();
            
            String model = "default";
            if (skinInfo.has("metadata")) {
                JsonObject meta = skinInfo.getAsJsonObject("metadata");
                if (meta.has("model")) {
                    model = meta.get("model").getAsString();
                }
            }
            
            // Build textures JSON
            JsonObject texturesObj = new JsonObject();
            JsonObject skinTex = new JsonObject();
            skinTex.addProperty("url", skinUrl);
            if ("slim".equals(model)) {
                JsonObject meta = new JsonObject();
                meta.addProperty("model", "slim");
                skinTex.add("metadata", meta);
            }
            texturesObj.add("SKIN", skinTex);
            
            // Add cape if exists
            if (skinObj.has("CAPE")) {
                JsonObject capeInfo = skinObj.getAsJsonObject("CAPE");
                if (capeInfo.has("url")) {
                    JsonObject capeTex = new JsonObject();
                    capeTex.addProperty("url", capeInfo.get("url").getAsString());
                    texturesObj.add("CAPE", capeTex);
                }
            }
            
            JsonObject root = new JsonObject();
            root.addProperty("timestamp", System.currentTimeMillis());
            root.addProperty("profileId", uuid.toString().replace("-", ""));
            root.addProperty("profileName", username);
            root.add("textures", texturesObj);
            
            String jsonStr = GSON.toJson(root);
            String base64Value = Base64.getEncoder().encodeToString(jsonStr.getBytes(StandardCharsets.UTF_8));
            
            return new Property("textures", base64Value, "");
        } catch (Exception ignored) {}
        return null;
    }

    private static String getUrlContent(String urlStr) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            
            if (conn.getResponseCode() == 200) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    return sb.toString();
                }
            }
        } catch (Exception ignored) {}
        return null;
    }
}
