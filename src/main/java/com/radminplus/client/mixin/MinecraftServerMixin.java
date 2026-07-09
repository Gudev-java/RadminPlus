package com.radminplus.client.mixin;

import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = MinecraftServer.class, priority = 1500)
public class MinecraftServerMixin {
    @Inject(method = "getServerModName", at = @At("RETURN"), cancellable = true, remap = false)
    private void onGetServerModName(CallbackInfoReturnable<String> cir) {
        try {
            String original = cir.getReturnValue();
            if (original != null) {
                if (original.contains("ᴿᴾ")) {
                    return;
                }
                cir.setReturnValue(original + "ᴿᴾ");
            } else {
                cir.setReturnValue("vanillaᴿᴾ");
            }
        } catch (Throwable t) {
            System.err.println("[Server] Error in MinecraftServerMixin:");
            t.printStackTrace();
        }
    }

    @org.spongepowered.asm.mixin.injection.Redirect(
        method = "buildPlayerStatus",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;getMaxPlayers()I")
    )
    private int redirectGetMaxPlayers(MinecraftServer server) {
        if (com.radminplus.ModInit.RULE_MAXONLINE != null && server.getWorldData() != null) {
            try {
                return server.getWorldData().getGameRules().get(com.radminplus.ModInit.RULE_MAXONLINE);
            } catch (Throwable ignored) {}
        }
        return server.getMaxPlayers();
    }
}
