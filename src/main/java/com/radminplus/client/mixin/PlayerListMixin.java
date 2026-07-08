package com.radminplus.client.mixin;

import com.radminplus.ModInit;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.net.SocketAddress;

@Mixin(PlayerList.class)
public abstract class PlayerListMixin {
    @Shadow @Final private MinecraftServer server;

    @Shadow public abstract net.minecraft.server.level.ServerPlayer getPlayerByName(String name);

    @Inject(method = "canPlayerLogin", at = @At("HEAD"), cancellable = true)
    private void onCanPlayerLogin(SocketAddress address, NameAndId nameAndId, CallbackInfoReturnable<Component> cir) {
        try {
            if (ModInit.RULE_ILLEGALNICK != null) {
                boolean allowIllegal = this.server.getWorldData().getGameRules().get(ModInit.RULE_ILLEGALNICK);
                if (!allowIllegal) {
                    String name = nameAndId.name();
                    if (name == null || name.isEmpty() || !name.matches("^[a-zA-Z0-9_а-яА-ЯёЁ\\-]+$")) {
                        cir.setReturnValue(Component.literal("§cНедопустимые символы в нике! Разрешены только буквы, цифры, _ и -"));
                        return;
                    }
                }
            }

            if (ModInit.RULE_JOINUNDERNAME != null) {
                boolean allowDuplicate = this.server.getWorldData().getGameRules().get(ModInit.RULE_JOINUNDERNAME);
                if (!allowDuplicate) {
                    if (this.getPlayerByName(nameAndId.name()) != null) {
                        cir.setReturnValue(Component.literal("§cИгрок с таким ником уже на сервере!"));
                    }
                }
            }
        } catch (Throwable t) {
            System.err.println("[Server] Error checking login gamerules:");
            t.printStackTrace();
        }
    }

    @Inject(method = "getMaxPlayers", at = @At("HEAD"), cancellable = true)
    private void onGetMaxPlayers(CallbackInfoReturnable<Integer> cir) {
        if (ModInit.RULE_MAXONLINE != null && this.server != null && this.server.getWorldData() != null) {
            try {
                int max = this.server.getWorldData().getGameRules().get(ModInit.RULE_MAXONLINE);
                cir.setReturnValue(max);
            } catch (Throwable ignored) {}
        }
    }
}
