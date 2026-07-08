package com.radminplus.client;

import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.server.LanServer;
import com.radminplus.client.accessor.LanServerDuck;

public class ServerFilter {
    public static String textQuery = "";
    public static String playerQuery = "";

    public static boolean matches(ServerData server) {
        if (server == null) return true;

        if (server.state() == ServerData.State.UNREACHABLE) {
            return false;
        }

        String name = server.name;
        String motd = server.motd != null ? server.motd.getString() : "";
        String ip = server.ip;

        String playerCount = "";
        if (server.players != null) {
            playerCount = server.players.online() + "/" + server.players.max();
        } else if (server.status != null) {
            playerCount = server.status.getString();
        }

        return matches(name, motd, ip, playerCount);
    }

    public static boolean matches(LanServer lanServer, ServerData pingData) {
        if (lanServer == null) return true;

        if (pingData != null) {
            if (pingData.state() == ServerData.State.UNREACHABLE) {
                return false;
            }
        }

        String name = lanServer.getMotd();
        String motd = lanServer.getMotd();
        String ip = lanServer.getAddress();

        String playerCount = "";
        if (lanServer instanceof LanServerDuck) {
            playerCount = ((LanServerDuck) lanServer).radminplus$getPlayerCount();
        }

        return matches(name, motd, ip, playerCount);
    }

    private static boolean matches(String name, String motd, String ip, String playerCount) {
        if (textQuery != null && !textQuery.isEmpty()) {
            String query = textQuery.toLowerCase();
            boolean matchName = name != null && name.toLowerCase().contains(query);
            boolean matchMotd = motd != null && motd.toLowerCase().contains(query);
            boolean matchIp = ip != null && ip.toLowerCase().contains(query);
            if (!matchName && !matchMotd && !matchIp) {
                return false;
            }
        }

        if (playerQuery != null && !playerQuery.isEmpty()) {
            int online = -1;
            if (playerCount != null && !playerCount.isEmpty()) {
                int slash = playerCount.indexOf('/');
                if (slash > 0) {
                    try {
                        online = Integer.parseInt(playerCount.substring(0, slash).trim());
                    } catch (NumberFormatException e) {}
                } else {
                    try {
                        online = Integer.parseInt(playerCount.trim());
                    } catch (NumberFormatException e) {}
                }
            }

            if (online == -1) {
                return true;
            }

            String q = playerQuery.trim();
            if (q.endsWith("+")) {
                try {
                    int min = Integer.parseInt(q.substring(0, q.length() - 1).trim());
                    if (online < min) {
                        return false;
                    }
                } catch (NumberFormatException e) {}
            } else if (q.endsWith("-")) {
                try {
                    int max = Integer.parseInt(q.substring(0, q.length() - 1).trim());
                    if (online > max) {
                        return false;
                    }
                } catch (NumberFormatException e) {}
            } else if (q.contains("-")) {
                String[] parts = q.split("-");
                if (parts.length == 2) {
                    try {
                        int min = Integer.parseInt(parts[0].trim());
                        int max = Integer.parseInt(parts[1].trim());
                        if (online < min || online > max) {
                            return false;
                        }
                    } catch (NumberFormatException e) {}
                }
            } else {
                try {
                    int target = Integer.parseInt(q);
                    if (online != target) {
                        return false;
                    }
                } catch (NumberFormatException e) {}
            }
        }

        return true;
    }
}
