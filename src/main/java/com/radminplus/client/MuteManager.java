package com.radminplus.client;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class MuteManager {
    private static final File MUTE_FILE = new File("muted-players.txt");
    private static final Set<UUID> MUTED = new HashSet<>();

    static {
        load();
    }

    public static synchronized void load() {
        MUTED.clear();
        if (MUTE_FILE.exists()) {
            try {
                for (String line : Files.readAllLines(MUTE_FILE.toPath())) {
                    String trimmed = line.trim();
                    if (!trimmed.isEmpty()) {
                        try {
                            MUTED.add(UUID.fromString(trimmed));
                        } catch (Exception e) {}
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static synchronized void save() {
        try {
            StringBuilder sb = new StringBuilder();
            for (UUID uuid : MUTED) {
                sb.append(uuid.toString()).append("\n");
            }
            Files.writeString(MUTE_FILE.toPath(), sb.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static synchronized boolean isMuted(UUID uuid) {
        return MUTED.contains(uuid);
    }

    public static synchronized void mute(UUID uuid) {
        MUTED.add(uuid);
        save();
    }

    public static synchronized void unmute(UUID uuid) {
        MUTED.remove(uuid);
        save();
    }
}
