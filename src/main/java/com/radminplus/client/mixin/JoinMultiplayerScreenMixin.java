package com.radminplus.client.mixin;

import com.radminplus.client.ServerFilter;
import com.radminplus.client.UpdateChecker;
import com.radminplus.client.UpdateNotificationScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(JoinMultiplayerScreen.class)
public abstract class JoinMultiplayerScreenMixin extends Screen {
    @Shadow public ServerSelectionList serverSelectionList;

    private int textX;
    private int textY;
    private boolean shouldRenderText = false;

    protected JoinMultiplayerScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        UpdateChecker.check();

        int listRowWidth = 305;
        int leftSpace = (this.width - listRowWidth) / 2;
        
        int panelWidth = Math.max(50, Math.min(120, leftSpace - 20));
        int x = (this.width - listRowWidth) / 2 - panelWidth - 10;
        if (x < 2) {
            x = 2;
        }
        int y = 35;
        
        ServerFilter.playerQuery = "";

        EditBox textSearch = new EditBox(this.font, x, y + 24, panelWidth, 16, Component.literal("Поиск..."));
        textSearch.setValue(ServerFilter.textQuery);
        textSearch.setHint(Component.literal("Имя, IP..."));
        textSearch.setResponder(val -> {
            ServerFilter.textQuery = val;
            ((ServerSelectionListInvoker) this.serverSelectionList).radminplus$refreshEntries();
        });
        this.addRenderableWidget(textSearch);

        this.textX = x;
        this.textY = y + 14;
        this.shouldRenderText = true;

    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        if (this.shouldRenderText) {
            int textColor = 0xFFAAAAAA;  // Light gray
            guiGraphics.drawString(this.font, "Поиск по тексту:", this.textX, this.textY, textColor);
        }
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent click, boolean bl) {
        for (net.minecraft.client.gui.components.events.GuiEventListener listener : this.children()) {
            if (listener instanceof EditBox) {
                EditBox editBox = (EditBox) listener;
                boolean hovered = click.x() >= editBox.getX() && click.x() < editBox.getX() + editBox.getWidth()
                        && click.y() >= editBox.getY() && click.y() < editBox.getY() + editBox.getHeight();
                if (hovered) {
                    this.setFocused(editBox);
                    editBox.setFocused(true);
                    editBox.mouseClicked(click, bl);
                    return true;
                } else {
                    editBox.setFocused(false);
                }
            }
        }
        return super.mouseClicked(click, bl);
    }
}
