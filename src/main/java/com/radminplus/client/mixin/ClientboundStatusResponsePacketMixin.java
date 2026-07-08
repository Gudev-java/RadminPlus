package com.radminplus.client.mixin;

import com.radminplus.client.CustomStatusCodec;
import com.mojang.serialization.Codec;
import net.minecraft.network.protocol.status.ClientboundStatusResponsePacket;
import net.minecraft.network.protocol.status.ServerStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ClientboundStatusResponsePacket.class)
public class ClientboundStatusResponsePacketMixin {
    
    @Redirect(
        method = "<clinit>",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/network/protocol/status/ServerStatus;CODEC:Lcom/mojang/serialization/Codec;"
        )
    )
    private static Codec<ServerStatus> onGetCodec() {
        return CustomStatusCodec.INSTANCE;
    }
}
