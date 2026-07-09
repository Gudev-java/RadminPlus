package com.radminplus.mixin;

import com.radminplus.accessor.TrackedEntityDuck;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import java.util.List;
import java.util.Set;

@Mixin(targets = "net.minecraft.server.level.ChunkMap$TrackedEntity")
public abstract class TrackedEntityMixin implements TrackedEntityDuck {
    @Shadow public abstract void removePlayer(ServerPlayer player);
    @Shadow public abstract void updatePlayers(List<ServerPlayer> list);
    @Shadow @Final public Set<?> seenBy; // Set<ServerPlayerConnection>

    @Override
    public void radminplus$removePlayer(ServerPlayer player) {
        this.removePlayer(player);
    }

    @Override
    public void radminplus$updatePlayers(List<ServerPlayer> players) {
        this.updatePlayers(players);
    }

    @Override
    public Set<?> radminplus$getSeenBy() {
        return this.seenBy;
    }
}
