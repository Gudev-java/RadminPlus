package com.radminplus.mixin;

import com.radminplus.accessor.ChunkMapDuck;
import com.radminplus.accessor.TrackedEntityDuck;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import java.util.ArrayList;

@Mixin(ChunkMap.class)
public class ChunkMapMixin implements ChunkMapDuck {
    @Shadow @Final private Int2ObjectMap<?> entityMap;

    @Override
    public void radminplus$reloadEntity(ServerPlayer player) {
        try {
            Object obj = this.entityMap.get(player.getId());
            if (obj instanceof TrackedEntityDuck trackedEntity) {
                // Remove player from all tracking players
                java.util.List<Object> seenByCopy = new ArrayList<>(trackedEntity.radminplus$getSeenBy());
                for (Object conn : seenByCopy) {
                    net.minecraft.server.network.ServerPlayerConnection connection = (net.minecraft.server.network.ServerPlayerConnection) conn;
                    trackedEntity.radminplus$removePlayer(connection.getPlayer());
                }
                
                // Re-add player to all tracking players in range
                trackedEntity.radminplus$updatePlayers(((net.minecraft.server.level.ServerLevel) player.level()).players());
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
