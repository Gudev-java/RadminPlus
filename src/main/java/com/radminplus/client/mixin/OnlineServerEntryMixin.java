package com.radminplus.client.mixin;

import com.radminplus.client.Filterable;
import com.radminplus.client.ServerFilter;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.client.multiplayer.ServerData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ServerSelectionList.OnlineServerEntry.class)
public class OnlineServerEntryMixin implements Filterable {
    @Shadow @Final private ServerData serverData;

    @Override
    public boolean matchesFilters() {
        return ServerFilter.matches(this.serverData);
    }
}
