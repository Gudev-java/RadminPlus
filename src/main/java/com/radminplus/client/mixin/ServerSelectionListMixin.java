package com.radminplus.client.mixin;

import com.radminplus.client.Filterable;
import com.radminplus.client.ServerFilter;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Mixin(ServerSelectionList.class)
public abstract class ServerSelectionListMixin {
    @Shadow @Final private List<ServerSelectionList.OnlineServerEntry> onlineServers;
    @Shadow @Final private ServerSelectionList.Entry lanHeader;
    @Shadow @Final private static java.util.concurrent.ThreadPoolExecutor THREAD_POOL;

    @Unique
    private boolean radminplus$matchesFilter(ServerSelectionList.Entry entry) {
        if (entry instanceof Filterable) {
            return ((Filterable) entry).matchesFilters();
        }
        return true;
    }

    @Redirect(
        method = "refreshEntries",
        at = @At(value = "NEW", target = "(Ljava/util/Collection;)Ljava/util/ArrayList;", ordinal = 0)
    )
    private ArrayList<ServerSelectionList.Entry> onNewArrayList(Collection<? extends ServerSelectionList.Entry> c) {
        List<ServerSelectionList.Entry> filtered = new ArrayList<>();
        for (ServerSelectionList.OnlineServerEntry entry : this.onlineServers) {
            if (radminplus$matchesFilter(entry)) {
                filtered.add(entry);
            }
        }
        return new ArrayList<>(filtered);
    }

    @Redirect(
        method = "refreshEntries",
        at = @At(value = "INVOKE", target = "Ljava/util/List;addAll(Ljava/util/Collection;)Z")
    )
    private boolean onRefreshEntriesAddAll(List<ServerSelectionList.Entry> list, Collection<? extends ServerSelectionList.Entry> c) {
        List<ServerSelectionList.Entry> filtered = new ArrayList<>();
        for (ServerSelectionList.Entry entry : c) {
            if (radminplus$matchesFilter(entry)) {
                filtered.add(entry);
            }
        }

        boolean hasMatchingLan = !filtered.isEmpty();
        boolean hasQuery = (ServerFilter.textQuery != null && !ServerFilter.textQuery.isEmpty()) 
                        || (ServerFilter.playerQuery != null && !ServerFilter.playerQuery.isEmpty());

        if (!hasMatchingLan && hasQuery) {
            list.remove(this.lanHeader);
        }

        return list.addAll(filtered);
    }

    @Redirect(
        method = "updateNetworkServers",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/multiplayer/ServerSelectionList;getRowTop(I)I")
    )
    private int onGetRowTop(ServerSelectionList instance, int index) {
        if (index < 0 || index >= instance.children().size()) {
            return -99999;
        }
        return instance.getRowTop(index);
    }

    @Redirect(
        method = "updateNetworkServers",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/multiplayer/ServerSelectionList;getRowBottom(I)I")
    )
    private int onGetRowBottom(ServerSelectionList instance, int index) {
        if (index < 0 || index >= instance.children().size()) {
            return -99999;
        }
        return instance.getRowBottom(index);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen screen, net.minecraft.client.Minecraft minecraft, int width, int height, int y, int headerHeight, CallbackInfo ci) {
        try {
            THREAD_POOL.setCorePoolSize(100);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
