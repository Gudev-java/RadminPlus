package com.radminplus.client.mixin;

import net.minecraft.client.multiplayer.resolver.ServerNameResolver;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.multiplayer.resolver.AddressCheck;
import net.minecraft.client.multiplayer.resolver.ResolvedServerAddress;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.net.InetAddress;
import java.util.Optional;

@Mixin(ServerNameResolver.class)
public class ServerNameResolverMixin {
    @Shadow @Final private AddressCheck addressCheck;

    @Inject(method = "resolveAddress", at = @At("HEAD"), cancellable = true)
    private void onResolveAddress(ServerAddress serverAddress, CallbackInfoReturnable<Optional<ResolvedServerAddress>> cir) {
        if (serverAddress != null && serverAddress.getHost() != null 
                && serverAddress.getHost().matches("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$")) {
            try {
                InetAddress inetAddress = InetAddress.getByName(serverAddress.getHost());
                InetSocketAddress socketAddress = new InetSocketAddress(inetAddress, serverAddress.getPort());
                ResolvedServerAddress resolved = ResolvedServerAddress.from(socketAddress);
                cir.setReturnValue(Optional.of(resolved).filter(this.addressCheck::isAllowed));
            } catch (UnknownHostException e) {
                cir.setReturnValue(Optional.empty());
            }
        }
    }
}
