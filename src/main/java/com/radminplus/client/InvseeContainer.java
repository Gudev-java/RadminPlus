package com.radminplus.client;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Inventory;

public class InvseeContainer implements Container {
    private final net.minecraft.server.level.ServerPlayer target;
    private final Inventory targetInv;

    public InvseeContainer(net.minecraft.server.level.ServerPlayer target) {
        this.target = target;
        this.targetInv = target.getInventory();
    }

    @Override
    public int getContainerSize() {
        return 45; // 9x5 chest
    }

    @Override
    public boolean isEmpty() {
        return targetInv.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        if (slot < 41) {
            return targetInv.getItem(slot);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        // Read-only: cannot remove items
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        // Read-only: cannot remove items
        return ItemStack.EMPTY;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        // Read-only: cannot modify slots
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        // Read-only: cannot place items
        return false;
    }

    @Override
    public void setChanged() {
        targetInv.setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return target.isAlive() && player.isAlive();
    }

    @Override
    public void clearContent() {
        // Read-only: cannot clear
    }
}
