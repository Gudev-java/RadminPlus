package com.radminplus.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.fabricmc.fabric.impl.registry.sync.RegistrySyncManager")
public class RegistrySyncManagerMixin {
    @Inject(
        method = "configureClient(Lnet/minecraft/server/network/ServerConfigurationPacketListenerImpl;Lnet/minecraft/server/MinecraftServer;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void onConfigureClient(net.minecraft.server.network.ServerConfigurationPacketListenerImpl handler, net.minecraft.server.MinecraftServer server, CallbackInfo ci) {
        try {
            if (server != null && !server.isDedicatedServer()) {
                System.out.println("[LAN] Integrated server detected. Bypassing Fabric mod/registry handshake.");
                ci.cancel();
            }
        } catch (Throwable t) {
            System.err.println("[LAN] Error in RegistrySyncManagerMixin:");
            t.printStackTrace();
        }
    }
}
