package com.radminplus.client.mixin;

import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ServerSelectionList.class)
public interface ServerSelectionListInvoker {
    @Invoker("refreshEntries")
    void radminplus$refreshEntries();
}
