package com.radminplus.client.mixin;

import com.radminplus.client.MuteManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerImplMixin {
    @Shadow public ServerPlayer player;

    @Inject(method = "tryHandleChat", at = @At("HEAD"), cancellable = true)
    private void onTryHandleChat(String text, boolean isCommand, Runnable handler, CallbackInfo ci) {
        try {
            if (this.player != null) {
                // If not logged in, block all chat except /register and /login
                if (!com.radminplus.AuthManager.LOGGED_IN_PLAYERS.contains(this.player.getUUID())) {
                    String cmd = text.trim().toLowerCase();
                    boolean isAuthCmd = cmd.startsWith("register ") || cmd.startsWith("/register ")
                                     || cmd.startsWith("login ") || cmd.startsWith("/login ");
                    if (!isAuthCmd) {
                        this.player.sendSystemMessage(Component.literal("§cВы должны авторизоваться! Воспользуйтесь /login <пароль> или /register <пароль> <повтор>"));
                        ci.cancel();
                        return;
                    }
                }

                if (MuteManager.isMuted(this.player.getUUID())) {
                    boolean block = !isCommand;
                    if (isCommand) {
                        String cmd = text.trim().toLowerCase();
                        if (cmd.startsWith("me ") || cmd.startsWith("say ") || cmd.startsWith("msg ") || cmd.startsWith("tell ") || cmd.startsWith("w ")) {
                            block = true;
                        }
                    }
                    if (block) {
                        this.player.sendSystemMessage(Component.literal("§cВы заблокированы в чате (замучены) на этом сервере!"));
                        ci.cancel();
                    }
                }
            }
        } catch (Throwable t) {
            System.err.println("[Server] Error in tryHandleChat interceptor:");
            t.printStackTrace();
        }
    }

    @Inject(method = "handleMovePlayer", at = @At("HEAD"), cancellable = true)
    private void onHandleMovePlayer(net.minecraft.network.protocol.game.ServerboundMovePlayerPacket packet, CallbackInfo ci) {
        if (this.player != null) {
            // Block movement if not logged in
            if (!com.radminplus.AuthManager.LOGGED_IN_PLAYERS.contains(this.player.getUUID())) {
                this.player.connection.teleport(this.player.getX(), this.player.getY(), this.player.getZ(), this.player.getYRot(), this.player.getXRot());
                ci.cancel();
                return;
            }
        }
    }

    @Inject(method = "handlePlayerAction", at = @At("HEAD"), cancellable = true)
    private void onHandlePlayerAction(net.minecraft.network.protocol.game.ServerboundPlayerActionPacket packet, CallbackInfo ci) {
        if (this.player != null && !com.radminplus.AuthManager.LOGGED_IN_PLAYERS.contains(this.player.getUUID())) {
            ci.cancel();
        }
    }

    @Inject(method = "handleUseItemOn", at = @At("HEAD"), cancellable = true)
    private void onHandleUseItemOn(net.minecraft.network.protocol.game.ServerboundUseItemOnPacket packet, CallbackInfo ci) {
        if (this.player != null && !com.radminplus.AuthManager.LOGGED_IN_PLAYERS.contains(this.player.getUUID())) {
            ci.cancel();
        }
    }

    @Inject(method = "handleUseItem", at = @At("HEAD"), cancellable = true)
    private void onHandleUseItem(net.minecraft.network.protocol.game.ServerboundUseItemPacket packet, CallbackInfo ci) {
        if (this.player != null && !com.radminplus.AuthManager.LOGGED_IN_PLAYERS.contains(this.player.getUUID())) {
            ci.cancel();
        }
    }

    @Inject(method = "handleInteract", at = @At("HEAD"), cancellable = true)
    private void onHandleInteract(net.minecraft.network.protocol.game.ServerboundInteractPacket packet, CallbackInfo ci) {
        if (this.player != null && !com.radminplus.AuthManager.LOGGED_IN_PLAYERS.contains(this.player.getUUID())) {
            ci.cancel();
        }
    }
}
