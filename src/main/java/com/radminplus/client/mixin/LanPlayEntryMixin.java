package com.radminplus.client.mixin;

import com.radminplus.client.accessor.LanServerDuck;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.server.LanServer;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

@Mixin(targets = "net.minecraft.client.gui.screens.multiplayer.ServerSelectionList$NetworkServerEntry")
public class LanPlayEntryMixin implements com.radminplus.client.Filterable {
    @Shadow @Final private JoinMultiplayerScreen screen;

    @Override
    public boolean matchesFilters() {
        return com.radminplus.client.ServerFilter.matches(this.serverData, this.radminplus$pingServerData);
    }
    @Shadow @Final protected Minecraft minecraft;
    @Shadow @Final protected LanServer serverData;

    private ServerData radminplus$pingServerData;
    private long radminplus$lastPingTime = 0L;

    private static final java.util.concurrent.ExecutorService LAN_PING_EXECUTOR = 
        java.util.concurrent.Executors.newFixedThreadPool(5, r -> {
            Thread t = new Thread(r, "RadminPlus Lan Pinger");
            t.setDaemon(true);
            return t;
        });

    private static java.lang.reflect.Method radminplus$cachedPingMethod = null;
    private static java.lang.reflect.Method radminplus$cachedRemoteMethod = null;


    @Inject(method = "renderContent", at = @At("HEAD"))
    private void onRenderContentHead(GuiGraphics guiGraphics, int mouseX, int mouseY, boolean isMouseOver, float partialTick, CallbackInfo ci) {
        radminplus$startPing();
    }

    private void radminplus$startPing() {
        try {
            if (this.serverData == null) return;

            long now = System.currentTimeMillis();

            if (this.radminplus$pingServerData == null) {
                this.radminplus$pingServerData = new ServerData(
                    this.serverData.getMotd(),
                    this.serverData.getAddress(),
                    ServerData.Type.LAN
                );

                // Load from cache instantly
                synchronized (com.radminplus.client.ServerCache.CACHED_SERVERS) {
                    for (com.radminplus.client.ServerCache.CachedServerInfo cs : com.radminplus.client.ServerCache.CACHED_SERVERS) {
                        if (cs.address.equals(this.serverData.getAddress())) {
                            if (this.serverData instanceof LanServerDuck) {
                                ((LanServerDuck) this.serverData).radminplus$setPlayerCount(cs.playerCount);
                                ((LanServerDuck) this.serverData).radminplus$setPlayerList(cs.playerList);
                            }
                            break;
                        }
                    }
                }
            }

            // Periodically reset state to trigger an auto-refresh
            ServerData.State state = this.radminplus$pingServerData.state();
            long elapsed = now - this.radminplus$lastPingTime;
            if (state == ServerData.State.PINGING) {
                if (elapsed > 35000L) { // 35s timeout for pinging (Netty timeout is 30s)
                    this.radminplus$pingServerData.setState(ServerData.State.UNREACHABLE);
                    this.radminplus$lastPingTime = now;
                }
            } else if (state == ServerData.State.UNREACHABLE) {
                if (elapsed > 60000L) { // Retry unreachable after 60s
                    this.radminplus$pingServerData.setState(ServerData.State.INITIAL);
                }
            } else {
                if (elapsed > 30000L) { // Refresh online after 30s
                    this.radminplus$pingServerData.setState(ServerData.State.INITIAL);
                }
            }

            if (this.radminplus$pingServerData.state() == ServerData.State.INITIAL) {
                this.radminplus$pingServerData.setState(ServerData.State.PINGING);
                this.radminplus$lastPingTime = now;
                LAN_PING_EXECUTOR.submit(() -> {
                    try {
                        radminplus$pingServer(
                            this.radminplus$pingServerData,
                            () -> {
                                // On update (e.g. icon/favicon updated)
                            },
                            () -> {
                                // On pong / complete
                                this.radminplus$pingServerData.setState(
                                    this.radminplus$pingServerData.protocol == net.minecraft.SharedConstants.getCurrentVersion().protocolVersion()
                                    ? ServerData.State.SUCCESSFUL : ServerData.State.INCOMPATIBLE
                                );

                                String playerCountStr = "";
                                if (this.radminplus$pingServerData.status != null) {
                                    playerCountStr = this.radminplus$pingServerData.status.getString();
                                } else if (this.radminplus$pingServerData.players != null) {
                                    playerCountStr = this.radminplus$pingServerData.players.online() + "/" + this.radminplus$pingServerData.players.max();
                                }

                                if (this.radminplus$pingServerData.state() == ServerData.State.INCOMPATIBLE && this.radminplus$pingServerData.players != null) {
                                    playerCountStr = this.radminplus$pingServerData.players.online() + "/" + this.radminplus$pingServerData.players.max();
                                }

                                List<String> playerNames = new ArrayList<>();
                                if (this.radminplus$pingServerData.playerList != null) {
                                    for (net.minecraft.network.chat.Component c : this.radminplus$pingServerData.playerList) {
                                        playerNames.add(c.getString());
                                    }
                                }

                                System.out.println("[LAN] Ping success for " + this.serverData.getAddress() + ", count: " + playerCountStr);

                                final String finalPlayerCount = playerCountStr;
                                final List<String> finalPlayerNames = playerNames;
                                this.minecraft.execute(() -> {
                                    if (this.serverData instanceof LanServerDuck) {
                                        ((LanServerDuck) this.serverData).radminplus$setPlayerCount(finalPlayerCount);
                                        ((LanServerDuck) this.serverData).radminplus$setPlayerList(finalPlayerNames);
                                        com.radminplus.client.ServerCache.updateCachedServer(
                                            this.serverData.getMotd(),
                                            this.serverData.getAddress(),
                                            finalPlayerCount,
                                            finalPlayerNames
                                        );
                                    }
                                });
                            },
                            this.minecraft.options.useNativeTransport()
                        );
                    } catch (Exception e) {
                        this.radminplus$pingServerData.setState(ServerData.State.UNREACHABLE);
                    }
                });
            }
        } catch (Throwable t) {
            System.err.println("[LAN] Error in radminplus$startPing:");
            t.printStackTrace();
        }
    }

    @Inject(method = "renderContent", at = @At("RETURN"))
    private void onRenderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, boolean isMouseOver, float partialTick, CallbackInfo ci) {
        try {
            if (this.serverData == null) return;

            String playerCount = ((LanServerDuck) this.serverData).radminplus$getPlayerCount();
            ServerData.State state = this.radminplus$pingServerData != null ? this.radminplus$pingServerData.state() : ServerData.State.INITIAL;

            int right = ((ServerSelectionList.Entry)(Object)this).getContentRight();
            int contentY = ((ServerSelectionList.Entry)(Object)this).getContentY();

            if (playerCount != null && !playerCount.isEmpty()) {
                int color = radminplus$getColorForCount(playerCount, 0xFF55FF55);
                int countWidth = this.minecraft.font.width(playerCount);
                int x = right - countWidth - 5;
                int y = contentY + 1;
                guiGraphics.drawString(this.minecraft.font, playerCount, x, y, color, false);

                if (mouseX >= x && mouseX <= right - 5 && mouseY >= y && mouseY <= y + 9) {
                    List<String> players = ((LanServerDuck) this.serverData).radminplus$getPlayerList();
                    if (players != null && !players.isEmpty()) {
                        List<net.minecraft.util.FormattedCharSequence> tooltip = new ArrayList<>();
                        for (String player : players) {
                            tooltip.add(Component.literal(player).getVisualOrderText());
                        }
                        guiGraphics.setTooltipForNextFrame(tooltip, mouseX, mouseY);
                    }
                }
            } else {
                Identifier statusSprite;
                Component statusTooltip = null;

                if (state == ServerData.State.UNREACHABLE) {
                    statusSprite = Identifier.withDefaultNamespace("server_list/pinging_1");
                    statusTooltip = Component.literal("§eСоединение не проверено\n§7Пинг не отвечает, но подключение\n§7может работать напрямую.");
                } else if (state == ServerData.State.PINGING || state == ServerData.State.INITIAL) {
                    long time = System.currentTimeMillis();
                    int j = (int)((time / 100L) & 7L);
                    if (j > 4) {
                        j = 8 - j;
                    }
                    int frame = switch (j) {
                        case 1 -> 2;
                        case 2 -> 3;
                        case 3 -> 4;
                        case 4 -> 5;
                        default -> 1;
                    };
                    statusSprite = Identifier.withDefaultNamespace("server_list/pinging_" + frame);
                } else {
                    statusSprite = Identifier.withDefaultNamespace("server_list/ping_5");
                }

                int iconX = right - 10 - 5;
                guiGraphics.blitSprite(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, statusSprite, iconX, contentY, 10, 8);

                if (statusTooltip != null && mouseX >= iconX && mouseX <= iconX + 10 && mouseY >= contentY && mouseY <= contentY + 8) {
                    List<net.minecraft.util.FormattedCharSequence> tooltipList = new ArrayList<>();
                    for (String line : statusTooltip.getString().split("\n")) {
                        tooltipList.add(Component.literal(line).getVisualOrderText());
                    }
                    guiGraphics.setTooltipForNextFrame(tooltipList, mouseX, mouseY);
                }
            }
        } catch (Throwable t) {
            System.err.println("[Server] Error in onRenderContent mixin:");
            t.printStackTrace();
        }
    }
    @Redirect(
        method = "renderContent",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)V")
    )
    private void onDrawString(GuiGraphics guiGraphics, net.minecraft.client.gui.Font font, String text, int x, int y, int color) {
        try {
            if (text != null && this.serverData != null && text.equals(this.serverData.getMotd())) {
                net.minecraft.network.chat.Component component = parseMotd(text);
                guiGraphics.drawString(font, component, x, y, color);
                return;
            }
        } catch (Throwable t) {
            System.err.println("[RadminPlus] Error in onDrawString redirect:");
            t.printStackTrace();
        }
        guiGraphics.drawString(font, text, x, y, color);
    }

    private net.minecraft.network.chat.Style applyColor(net.minecraft.network.chat.Style style, net.minecraft.ChatFormatting color) {
        return style.withColor(color)
                    .withBold(false)
                    .withItalic(false)
                    .withUnderlined(false)
                    .withStrikethrough(false)
                    .withObfuscated(false);
    }

    private net.minecraft.network.chat.Component parseMotd(String text) {
        if (text == null) return net.minecraft.network.chat.Component.empty();

        text = text.replaceAll("(?i)§x§([0-9a-f])§([0-9a-f])§([0-9a-f])§([0-9a-f])§([0-9a-f])§([0-9a-f])", "&#$1$2$3$4$5$6");
        text = text.replace("&#", "§#");
        text = text.replaceAll("&([0-9a-fk-orA-FK-OR])", "§$1");

        net.minecraft.network.chat.MutableComponent root = net.minecraft.network.chat.Component.empty();
        String[] parts = text.split("§");

        if (parts.length > 0 && !parts[0].isEmpty()) {
            root.append(net.minecraft.network.chat.Component.literal(parts[0]));
        }

        net.minecraft.network.chat.Style style = net.minecraft.network.chat.Style.EMPTY;

        for (int i = 1; i < parts.length; i++) {
            String part = parts[i];
            if (part.isEmpty()) continue;

            char code = part.charAt(0);
            String content = part.substring(1);

            if (code == '#' && part.length() >= 7) {
                String hex = part.substring(1, 7);
                content = part.substring(7);
                try {
                    int color = Integer.parseInt(hex, 16);
                    style = style.withColor(color)
                                 .withBold(false)
                                 .withItalic(false)
                                 .withUnderlined(false)
                                 .withStrikethrough(false)
                                 .withObfuscated(false);
                } catch (NumberFormatException e) {
                    // Ignore
                }
            } else {
                switch (Character.toLowerCase(code)) {
                    case '0': style = applyColor(style, net.minecraft.ChatFormatting.BLACK); break;
                    case '1': style = applyColor(style, net.minecraft.ChatFormatting.DARK_BLUE); break;
                    case '2': style = applyColor(style, net.minecraft.ChatFormatting.DARK_GREEN); break;
                    case '3': style = applyColor(style, net.minecraft.ChatFormatting.DARK_AQUA); break;
                    case '4': style = applyColor(style, net.minecraft.ChatFormatting.DARK_RED); break;
                    case '5': style = applyColor(style, net.minecraft.ChatFormatting.DARK_PURPLE); break;
                    case '6': style = applyColor(style, net.minecraft.ChatFormatting.GOLD); break;
                    case '7': style = applyColor(style, net.minecraft.ChatFormatting.GRAY); break;
                    case '8': style = applyColor(style, net.minecraft.ChatFormatting.DARK_GRAY); break;
                    case '9': style = applyColor(style, net.minecraft.ChatFormatting.BLUE); break;
                    case 'a': style = applyColor(style, net.minecraft.ChatFormatting.GREEN); break;
                    case 'b': style = applyColor(style, net.minecraft.ChatFormatting.AQUA); break;
                    case 'c': style = applyColor(style, net.minecraft.ChatFormatting.RED); break;
                    case 'd': style = applyColor(style, net.minecraft.ChatFormatting.LIGHT_PURPLE); break;
                    case 'e': style = applyColor(style, net.minecraft.ChatFormatting.YELLOW); break;
                    case 'f': style = applyColor(style, net.minecraft.ChatFormatting.WHITE); break;
                    case 'k': break;
                    case 'l': style = style.withBold(true); break;
                    case 'm': style = style.withStrikethrough(true); break;
                    case 'n': style = style.withUnderlined(true); break;
                    case 'o': style = style.withItalic(true); break;
                    case 'r': style = net.minecraft.network.chat.Style.EMPTY; break;
                    default:
                        content = code + content;
                        break;
                }
            }

            if (!content.isEmpty()) {
                root.append(net.minecraft.network.chat.Component.literal(content).withStyle(style));
            }
        }

        return root;
    }

    @org.spongepowered.asm.mixin.injection.Inject(method = "mouseClicked", at = @org.spongepowered.asm.mixin.injection.At("HEAD"))
    private void onMouseClicked(net.minecraft.client.input.MouseButtonEvent click, boolean bl, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> cir) {
        if (click != null && click.button() == 0) {
            if (this.radminplus$pingServerData != null) {
                this.radminplus$pingServerData.setState(ServerData.State.INITIAL);
                this.radminplus$lastPingTime = 0L;
            }
        }
    }

    private void radminplus$pingServer(ServerData serverData, Runnable onUpdate, Runnable onPong, boolean useNative) {
        try {
            Object pinger = this.screen.getPinger();
            if (radminplus$cachedPingMethod == null) {
                for (java.lang.reflect.Method m : pinger.getClass().getDeclaredMethods()) {
                    if (m.getParameterCount() == 4 && m.getParameterTypes()[1] == Runnable.class) {
                        radminplus$cachedPingMethod = m;
                        break;
                    }
                }
                if (radminplus$cachedPingMethod == null) {
                    for (java.lang.reflect.Method m : pinger.getClass().getMethods()) {
                        if (m.getParameterCount() == 4 && m.getParameterTypes()[1] == Runnable.class) {
                            radminplus$cachedPingMethod = m;
                            break;
                        }
                    }
                }
                if (radminplus$cachedPingMethod != null) {
                    radminplus$cachedPingMethod.setAccessible(true);
                }
            }
            
            if (radminplus$cachedPingMethod == null) {
                throw new NoSuchMethodException("pingServer method not found on " + pinger.getClass());
            }

            if (radminplus$cachedRemoteMethod == null) {
                Class<?> holderClass = radminplus$cachedPingMethod.getParameterTypes()[3];
                for (java.lang.reflect.Method m : holderClass.getDeclaredMethods()) {
                    if (java.lang.reflect.Modifier.isStatic(m.getModifiers()) 
                            && m.getParameterCount() == 1 
                            && m.getParameterTypes()[0] == boolean.class) {
                        radminplus$cachedRemoteMethod = m;
                        break;
                    }
                }
                if (radminplus$cachedRemoteMethod == null) {
                    for (java.lang.reflect.Method m : holderClass.getMethods()) {
                        if (java.lang.reflect.Modifier.isStatic(m.getModifiers()) 
                                && m.getParameterCount() == 1 
                                && m.getParameterTypes()[0] == boolean.class) {
                            radminplus$cachedRemoteMethod = m;
                            break;
                        }
                    }
                }
                if (radminplus$cachedRemoteMethod != null) {
                    radminplus$cachedRemoteMethod.setAccessible(true);
                }
            }
            
            if (radminplus$cachedRemoteMethod == null) {
                throw new NoSuchMethodException("remote method not found on EventLoopGroupHolder");
            }
            
            Object holderInstance = radminplus$cachedRemoteMethod.invoke(null, useNative);
            radminplus$cachedPingMethod.invoke(pinger, serverData, onUpdate, onPong, holderInstance);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private int radminplus$getColorForCount(String count, int activeColor) {
        try {
            if (count.contains("/")) {
                String[] parts = count.split("/");
                int online = Integer.parseInt(parts[0].trim());
                if (online > 0) {
                    return activeColor;
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return -8355712; // Default gray
    }
}
