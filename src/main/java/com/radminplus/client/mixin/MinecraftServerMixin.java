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
}
