package com.radminplus.client;

import java.util.ArrayList;
import java.util.List;

public class ServerCache {
    public static class CachedServerInfo {
        public final String motd;
        public final String address;
        public final String playerCount;
        public final List<String> playerList;

        public CachedServerInfo(String motd, String address, String playerCount, List<String> playerList) {
            this.motd = motd;
            this.address = address;
            this.playerCount = playerCount == null ? "" : playerCount;
            this.playerList = playerList == null ? new ArrayList<>() : playerList;
        }
    }

    public static final List<CachedServerInfo> CACHED_SERVERS = new ArrayList<>();

    public static void updateCachedServer(String motd, String address, String playerCount, List<String> playerList) {
        synchronized (CACHED_SERVERS) {
            CACHED_SERVERS.removeIf(cs -> cs.address.equals(address));
            CACHED_SERVERS.add(new CachedServerInfo(motd, address, playerCount, playerList));
        }
    }
}
