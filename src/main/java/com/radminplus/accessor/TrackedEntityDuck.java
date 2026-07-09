package com.radminplus.accessor;

import net.minecraft.server.level.ServerPlayer;
import java.util.List;
import java.util.Set;

public interface TrackedEntityDuck {
    void radminplus$removePlayer(ServerPlayer player);
    void radminplus$updatePlayers(List<ServerPlayer> players);
    Set<?> radminplus$getSeenBy();
}
