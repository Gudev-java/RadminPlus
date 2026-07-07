package com.radminplus.client.mixin;

import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.Commands;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;

@Mixin(Commands.class)
public class CommandsMixin {
    @Shadow @Final private CommandDispatcher<CommandSourceStack> dispatcher;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(Commands.CommandSelection selection, CommandBuildContext context, CallbackInfo ci) {
        if (selection != null && !selection.includeDedicated) {
            System.out.println("[LAN] Registering custom LAN commands (whitelist, ban, pardon, op, deop, mute, unmute, safemode).");
            try {
                com.radminplus.client.JopaUltraCommandListJavaClassImbaOxyennoKruto.register(this.dispatcher);
            } catch (Throwable t) {
                System.err.println("[LAN] Failed to register custom LAN commands:");
                t.printStackTrace();
            }
        }
    }
}
