package com.radminplus.client;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.minecraft.network.protocol.status.ServerStatus;

public class CustomStatusCodec implements Codec<ServerStatus> {
    public static final CustomStatusCodec INSTANCE = new CustomStatusCodec();

    @Override
    public <T> DataResult<T> encode(ServerStatus input, DynamicOps<T> ops, T prefix) {
        DataResult<T> result = ServerStatus.CODEC.encode(input, ops, prefix);
        try {
            if (result.result().isPresent()) {
                T encoded = result.result().get();
                if (encoded instanceof JsonObject) {
                    JsonObject obj = (JsonObject) encoded;

                    JsonObject rpObj = new JsonObject();
                    rpObj.addProperty("modded", true);
                    rpObj.addProperty("commit", "RadminPlus");

                    obj.add("radminplus", rpObj);
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return result;
    }

    @Override
    public <T> DataResult<com.mojang.datafixers.util.Pair<ServerStatus, T>> decode(DynamicOps<T> ops, T input) {
        return ServerStatus.CODEC.decode(ops, input);
    }
}
