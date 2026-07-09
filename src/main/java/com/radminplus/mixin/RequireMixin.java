package com.radminplus.mixin;

import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.server.permissions.Permissions;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.server.permissions.PermissionCheck$Require")
public class RequireMixin {
    @Shadow @Final private Permission permission;

    @Inject(method = "check", at = @At("HEAD"), cancellable = true)
    private void onCheck(PermissionSet permissionSet, CallbackInfoReturnable<Boolean> cir) {
        try {
            if (this.permission == Permissions.COMMANDS_GAMEMASTER) {
                if (permissionSet.hasPermission(Permissions.COMMANDS_MODERATOR)) {
                    cir.setReturnValue(true);
                }
            }
        } catch (Throwable ignored) {}
    }
}
