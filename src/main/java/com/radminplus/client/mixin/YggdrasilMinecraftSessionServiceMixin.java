package com.radminplus.client.mixin;

import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftProfileTextures;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.SignatureState;
import com.mojang.authlib.yggdrasil.YggdrasilMinecraftSessionService;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Mixin(value = YggdrasilMinecraftSessionService.class, remap = false)
public class YggdrasilMinecraftSessionServiceMixin {
    @Inject(method = "unpackTextures", at = @At("HEAD"), cancellable = true)
    private void onUnpackTextures(Property packedTextures, CallbackInfoReturnable<MinecraftProfileTextures> cir) {
        try {
            if (packedTextures != null) {
                String value = packedTextures.value();
                
                // Decode Base64 JSON
                String decodedJson = new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
                JsonObject root = JsonParser.parseString(decodedJson).getAsJsonObject();
                
                if (root.has("textures")) {
                    JsonObject textures = root.getAsJsonObject("textures");
                    
                    MinecraftProfileTexture skin = null;
                    MinecraftProfileTexture cape = null;
                    MinecraftProfileTexture elytra = null;
                    
                    if (textures.has("SKIN")) {
                        try {
                            JsonObject texObj = textures.getAsJsonObject("SKIN");
                            String url = texObj.get("url").getAsString();
                            Map<String, String> metadata = null;
                            if (texObj.has("metadata")) {
                                JsonObject metaObj = texObj.getAsJsonObject("metadata");
                                metadata = new java.util.HashMap<>();
                                for (Map.Entry<String, com.google.gson.JsonElement> metaEntry : metaObj.entrySet()) {
                                    metadata.put(metaEntry.getKey(), metaEntry.getValue().getAsString());
                                }
                            }
                            skin = new MinecraftProfileTexture(url, metadata);
                        } catch (Exception ignored) {}
                    }
                    
                    if (textures.has("CAPE")) {
                        try {
                            JsonObject texObj = textures.getAsJsonObject("CAPE");
                            String url = texObj.get("url").getAsString();
                            cape = new MinecraftProfileTexture(url, null);
                        } catch (Exception ignored) {}
                    }
                    
                    if (textures.has("ELYTRA")) {
                        try {
                            JsonObject texObj = textures.getAsJsonObject("ELYTRA");
                            String url = texObj.get("url").getAsString();
                            elytra = new MinecraftProfileTexture(url, null);
                        } catch (Exception ignored) {}
                    }
                    
                    // Return mapped textures with SIGNED signature state to bypass security check
                    cir.setReturnValue(new MinecraftProfileTextures(skin, cape, elytra, SignatureState.SIGNED));
                }
            }
        } catch (Exception ignored) {
            // Fall back to vanilla signature checking on error
        }
    }
}
