package com.radminplus.client.mixin;

import net.minecraft.client.server.LanServerPinger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

@Mixin(LanServerPinger.class)
public class LanServerPingerMixin {
    @Shadow @Final private String motd;
    @Shadow @Final private String serverAddress;

    @Redirect(
        method = "run",
        at = @At(value = "INVOKE", target = "Ljava/net/DatagramSocket;send(Ljava/net/DatagramPacket;)V")
    )
    private void onSend(DatagramSocket socket, DatagramPacket packet) throws IOException {
        String baseMotd = this.motd;
        String port = this.serverAddress;

        int currentPlayers = 0;
        int maxPlayers = 0;
        List<String> playerNames = new ArrayList<>();

        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc != null) {
            net.minecraft.server.MinecraftServer server = mc.getSingleplayerServer();
            if (server != null) {
                currentPlayers = server.getPlayerCount();
                int max = server.getMaxPlayers();
                if (com.radminplus.ModInit.RULE_MAXONLINE != null && server.getWorldData() != null) {
                    try {
                        max = server.getWorldData().getGameRules().get(com.radminplus.ModInit.RULE_MAXONLINE);
                    } catch (Throwable ignored) {}
                }
                maxPlayers = max;
                if (server.getPlayerList() != null) {
                    for (net.minecraft.server.level.ServerPlayer player : server.getPlayerList().getPlayers()) {
                        playerNames.add(player.getGameProfile().name());
                    }
                }
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("[MOTD]").append(baseMotd).append("[/MOTD]");
        sb.append("[AD]").append(port).append("[/AD]");
        sb.append("[PLAYERS]").append(currentPlayers).append("/").append(maxPlayers).append("[/PLAYERS]");
        if (!playerNames.isEmpty()) {
            sb.append("[NAMES]").append(String.join(",", playerNames)).append("[/NAMES]");
        }

        byte[] bytes = sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        DatagramPacket newPacket = new DatagramPacket(bytes, bytes.length, packet.getAddress(), packet.getPort());

        // 1. Always send on the default socket (handles same-machine loopback and default routing)
        try {
            socket.send(newPacket);
        } catch (Exception ignored) {}

        // 2. Broadcast on all other active interfaces (including virtual adapters like Radmin VPN)
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface netInterface = interfaces.nextElement();
                try {
                    if (!netInterface.isUp() || netInterface.isLoopback()) {
                        continue;
                    }
                    try (MulticastSocket ms = new MulticastSocket()) {
                        ms.setNetworkInterface(netInterface);
                        ms.setTimeToLive(4);
                        DatagramPacket ifacePacket = new DatagramPacket(bytes, bytes.length, packet.getAddress(), packet.getPort());
                        ms.send(ifacePacket);
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
    }
}
