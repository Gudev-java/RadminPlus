package com.radminplus.client.mixin;

import com.radminplus.client.ServerCache;
import com.radminplus.client.accessor.LanServerDuck;
import net.minecraft.client.server.LanServer;
import net.minecraft.client.server.LanServerPinger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Mixin(targets = "net.minecraft.client.server.LanServerDetection$LanServerList")
public class LanServerListMixin {
    @Shadow @Final private List<LanServer> servers;
    @Shadow private boolean isDirty;


    @Inject(method = "addServer", at = @At("HEAD"), cancellable = true)
    private void onAddServerHead(String pingResponse, InetAddress ipAddress, CallbackInfo ci) {
        try {
            String motd = LanServerPinger.parseMotd(pingResponse);
            String ip = ipAddress.getHostAddress();

            for (LanServer s : this.servers) {
                String addr = s.getAddress();
                String s_ip = addr.contains(":") ? addr.split(":")[0] : addr;

                if (s_ip.equals(ip)) {
                    // 1. Deduplicate similar MOTDs on the same IP
                    if (radminplus$isSimilar(motd, s.getMotd())) {
                        ci.cancel();
                        return;
                    }
                    
                    // 2. Limit servers per IP (maximum of 2)
                    if (radminplus$countIp(ip) >= 2) {
                        ci.cancel();
                        return;
                    }
                }
                
                // 3. Block exact duplicate titles from different IPs if they are generic spam
                if (radminplus$isSpam(motd) && motd.equalsIgnoreCase(s.getMotd())) {
                    ci.cancel();
                    return;
                }
            }
        } catch (Throwable t) {
            System.err.println("[RadminPlus] Error in onAddServerHead mixin:");
            t.printStackTrace();
        }
    }

    private boolean radminplus$isSimilar(String a, String b) {
        if (a == null || b == null) return false;
        String c1 = a.replaceAll("[^a-zA-Zа-яА-Я0-9]", "");
        String c2 = b.replaceAll("[^a-zA-Zа-яА-Я0-9]", "");
        if (c1.isEmpty() || c2.isEmpty()) return a.equals(b);
        return c1.equalsIgnoreCase(c2);
    }

    private int radminplus$countIp(String ip) {
        int c = 0;
        for (LanServer s : this.servers) {
            String addr = s.getAddress();
            String s_ip = addr.contains(":") ? addr.split(":")[0] : addr;
            if (s_ip.equals(ip)) c++;
        }
        return c;
    }

    private boolean radminplus$isSpam(String s) {
        if (s == null) return false;
        String low = s.toLowerCase();
        // Allow Telegram ("t.me", "@") and links ("http") to let players promote their channels/groups.
        // We only filter out actual flooding/malicious spam indicators:
        if (low.contains("antiattack") || low.contains("спам") || low.contains("anti-attack")) return true;
        if (low.contains("player sample") || low.contains("скрой ники")) return true;
        if (s.matches(".*#\\d{2,}$")) return true; // Matches ending with e.g. #12, #456 (common bot formats)
        return s.length() > 80; // Block extremely long spam names that corrupt list layout
    }

    @Inject(method = "addServer", at = @At("RETURN"))
    private void onAddServer(String pingResponse, InetAddress ipAddress, CallbackInfo ci) {
        try {
            String adonis = LanServerPinger.parseAddress(pingResponse);
            if (adonis == null) return;
            String address = ipAddress.getHostAddress() + ":" + adonis;
            String motd = LanServerPinger.parseMotd(pingResponse);

            String playerCount = parseTag(pingResponse, "PLAYERS");
            String namesStr = parseTag(pingResponse, "NAMES");
            List<String> names = new ArrayList<>();
            if (namesStr != null && !namesStr.isEmpty()) {
                names = Arrays.asList(namesStr.split(","));
            }

            String finalCount = playerCount;
            List<String> finalNames = names;
            if (playerCount == null) {
                synchronized (ServerCache.CACHED_SERVERS) {
                    for (ServerCache.CachedServerInfo cs : ServerCache.CACHED_SERVERS) {
                        if (cs.address.equals(address)) {
                            finalCount = cs.playerCount;
                            finalNames = cs.playerList;
                            break;
                        }
                    }
                }
            }

            for (LanServer s : this.servers) {
                if (s.getAddress().equals(address)) {
                    if (s instanceof LanServerDuck) {
                        if (finalCount != null) {
                            ((LanServerDuck) s).radminplus$setPlayerCount(finalCount);
                        }
                        if (finalNames != null) {
                            ((LanServerDuck) s).radminplus$setPlayerList(finalNames);
                        }
                    }
                    break;
                }
            }

            if (finalCount != null) {
                ServerCache.updateCachedServer(motd, address, finalCount, finalNames);
            }
        } catch (Throwable t) {
            System.err.println("[LAN] Error in onAddServer mixin:");
            t.printStackTrace();
        }
    }

    private String parseTag(String raw, String tag) {
        String startTag = "[" + tag + "]";
        String endTag = "[/" + tag + "]";
        int start = raw.indexOf(startTag);
        if (start < 0) return null;
        int end = raw.indexOf(endTag, start + startTag.length());
        if (end < 0) return null;
        return raw.substring(start + startTag.length(), end);
    }
}


