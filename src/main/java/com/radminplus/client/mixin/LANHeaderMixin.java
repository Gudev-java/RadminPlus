package com.radminplus.client.mixin;

import net.minecraft.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerSelectionList.LANHeader.class)
public class LANHeaderMixin {
    @Shadow @Final private Minecraft minecraft;

    @Inject(
        method = "renderContent(Lnet/minecraft/client/gui/GuiGraphics;IIZF)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onRenderContent(
        GuiGraphics guiGraphics,
        int mouseX,
        int mouseY,
        boolean isMouseOver,
        float partialTick,
        CallbackInfo ci
    ) {
        ci.cancel();

        // Get the entry instance to access coordinates
        ServerSelectionList.Entry entry = (ServerSelectionList.Entry)(Object)this;
        int x = entry.getX();
        int y = entry.getY(); // Use the actual entry Y coordinate instead of the mouseY parameter
        int width = entry.getWidth();
        int height = entry.getHeight();
        
        // Vertically center the text and elements inside the header
        int textY = y + height / 2 - 9 / 2 - 3;
        
        // Premium horizontal line and text colors (Orange Accent Theme)
        int lineColor = 0x3FFB8200;       // Semi-transparent vibrant orange (#fb8200 at 25% opacity)
        int primaryOrange = 0xFFFB8200;   // Vibrant orange (#fb8200)
        int secondaryOrange = 0xFFBE6500; // Darker orange (#be6500)
        
        // 1. Draw a horizontal line above the text
        guiGraphics.fill(x, y + 2, x + width, y + 3, lineColor);
        
        // 2. Draw the text "Поиск миров в локальной сети..."
        Component label = Component.translatable("lanServer.scanning");
        int labelWidth = this.minecraft.font.width(label);
        int labelX = x + width / 2 - labelWidth / 2;
        guiGraphics.drawString(this.minecraft.font, label, labelX, textY + 4, primaryOrange);
        
        // 3. Draw the animated scanning dots below the text in the secondary orange color
        String dots;
        switch ((int)(Util.getMillis() / 300L % 4L)) {
            case 0:
            default:
                dots = "O o o";
                break;
            case 1:
            case 3:
                dots = "o O o";
                break;
            case 2:
                dots = "o o O";
        }
        int dotsWidth = this.minecraft.font.width(dots);
        guiGraphics.drawString(this.minecraft.font, dots, x + width / 2 - dotsWidth / 2, textY + 14, secondaryOrange);
        
        // 4. Draw another horizontal line below the dots to frame the section beautifully
        guiGraphics.fill(x, y + height - 3, x + width, y + height - 2, lineColor);
    }
}
