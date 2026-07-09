package com.radminplus.client.mixin;

import com.radminplus.client.accessor.LanServerDuck;
import net.minecraft.client.server.LanServer;
import org.spongepowered.asm.mixin.Mixin;
import java.util.ArrayList;
import java.util.List;

@Mixin(LanServer.class)
public class LanServerMixin implements LanServerDuck {
    private String radminplus$playerCount = "";
    private List<String> radminplus$playerList = new ArrayList<>();

    @org.spongepowered.asm.mixin.injection.Inject(method = "getMotd", at = @org.spongepowered.asm.mixin.injection.At("RETURN"), cancellable = true)
    private void onGetMotd(org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<String> cir) {
        String original = cir.getReturnValue();
        if (original != null) {
            String parsed = original.replaceAll("(?i)§x§([0-9a-f])§([0-9a-f])§([0-9a-f])§([0-9a-f])§([0-9a-f])&([0-9a-f])", "&#$1$2$3$4$5$6")
                                    .replace("&#", "§#")
                                    .replaceAll("&([0-9a-fk-orA-FK-OR])", "§$1");
            cir.setReturnValue(parsed);
        }
    }

    @Override
    public String radminplus$getPlayerCount() {
        return this.radminplus$playerCount;
    }

    @Override
    public void radminplus$setPlayerCount(String playerCount) {
        this.radminplus$playerCount = playerCount == null ? "" : playerCount;
    }

    @Override
    public List<String> radminplus$getPlayerList() {
        return this.radminplus$playerList;
    }

    @Override
    public void radminplus$setPlayerList(List<String> playerList) {
        this.radminplus$playerList = playerList == null ? new ArrayList<>() : playerList;
    }
}
