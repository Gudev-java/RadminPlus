package com.radminplus.client.mixin;

import com.radminplus.client.UpdateChecker;
import com.radminplus.client.UpdateNotificationScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {
    protected TitleScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        if (UpdateChecker.updateAvailable && !UpdateChecker.userSkipped) {
            this.minecraft.setScreen(new UpdateNotificationScreen(this));
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(CallbackInfo ci) {
        if (UpdateChecker.updateAvailable && !UpdateChecker.userSkipped) {
            this.minecraft.setScreen(new UpdateNotificationScreen(this));
        }
    }
}
