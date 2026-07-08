package com.radminplus.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.players.NameAndId;

import java.util.Collection;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JopaUltraCommandListJavaClassImbaOxyennoKruto {
    private static NameAndId toNameAndId(com.mojang.authlib.GameProfile profile) {
        return new NameAndId(profile.id(), profile.name());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("whitelist")
                .requires(source -> Commands.LEVEL_GAMEMASTERS.check(source.permissions()))
                .executes(ctx -> {
                    ctx.getSource().sendSuccess(() -> Component.literal("§eИспользование: /whitelist <on|off|list|add|remove>"), false);
                    return 0;
                })
                .then(Commands.literal("on").executes(ctx -> whitelistSetEnabled(ctx.getSource(), true)))
                .then(Commands.literal("off").executes(ctx -> whitelistSetEnabled(ctx.getSource(), false)))
                .then(Commands.literal("list").executes(ctx -> whitelistList(ctx.getSource())))
                .then(
                    Commands.literal("add")
                        .then(
                            Commands.argument("targets", GameProfileArgument.gameProfile())
                                .suggests((ctx, builder) -> {
                                    return net.minecraft.commands.SharedSuggestionProvider.suggest(
                                        ctx.getSource().getServer().getPlayerList().getPlayers()
                                            .stream()
                                            .filter(p -> !ctx.getSource().getServer().getPlayerList().getWhiteList().isWhiteListed(p.nameAndId()))
                                            .map(p -> p.getGameProfile().name()),
                                        builder
                                    );
                                })
                                .executes(ctx -> whitelistAdd(ctx.getSource(), GameProfileArgument.getGameProfiles(ctx, "targets")))
                        )
                )
                .then(
                    Commands.literal("remove")
                        .then(
                            Commands.argument("targets", GameProfileArgument.gameProfile())
                                .suggests((ctx, builder) -> {
                                    return net.minecraft.commands.SharedSuggestionProvider.suggest(
                                        java.util.Arrays.stream(ctx.getSource().getServer().getPlayerList().getWhiteList().getUserList()),
                                        builder
                                    );
                                })
                                .executes(ctx -> whitelistRemove(ctx.getSource(), GameProfileArgument.getGameProfiles(ctx, "targets")))
                        )
                )
        );

        dispatcher.register(
            Commands.literal("ban")
                .requires(source -> Commands.LEVEL_ADMINS.check(source.permissions()))
                .executes(ctx -> {
                    ctx.getSource().sendSuccess(() -> Component.literal("§eИспользование: /ban <ник> [время] [причина]"), false);
                    return 0;
                })
                .then(
                    Commands.literal("list")
                        .executes(ctx -> {
                            Object[] list = ctx.getSource().getServer().getPlayerList().getBans().getUserList();
                            if (list.length == 0) {
                                ctx.getSource().sendSuccess(() -> Component.literal("§eСписок забаненных пуст."), false);
                            } else {
                                java.util.List<String> names = new java.util.ArrayList<>();
                                for (Object entryObj : list) {
                                    net.minecraft.server.players.UserBanListEntry entry = (net.minecraft.server.players.UserBanListEntry) entryObj;
                                    names.add(entry.getUser().name());
                                }
                                ctx.getSource().sendSuccess(() -> Component.literal("§aЗабаненные игроки: " + String.join(", ", names)), false);
                            }
                            return list.length;
                        })
                )
                .then(
                    Commands.argument("targets", GameProfileArgument.gameProfile())
                        .executes(ctx -> ban(ctx.getSource(), GameProfileArgument.getGameProfiles(ctx, "targets"), null, null))
                        .then(
                            Commands.argument("timeOrReason", StringArgumentType.string())
                                .executes(ctx -> {
                                    String arg = StringArgumentType.getString(ctx, "timeOrReason");
                                    Date expires = parseDuration(arg);
                                    if (expires != null) {
                                        return ban(ctx.getSource(), GameProfileArgument.getGameProfiles(ctx, "targets"), expires, null);
                                    } else {
                                        return ban(ctx.getSource(), GameProfileArgument.getGameProfiles(ctx, "targets"), null, arg);
                                    }
                                })
                                .then(
                                    Commands.argument("reason", StringArgumentType.greedyString())
                                        .executes(ctx -> {
                                            String timeStr = StringArgumentType.getString(ctx, "timeOrReason");
                                            String reason = StringArgumentType.getString(ctx, "reason");
                                            Date expires = parseDuration(timeStr);
                                            if (expires != null) {
                                                return ban(ctx.getSource(), GameProfileArgument.getGameProfiles(ctx, "targets"), expires, reason);
                                            } else {
                                                return ban(ctx.getSource(), GameProfileArgument.getGameProfiles(ctx, "targets"), null, timeStr + " " + reason);
                                            }
                                        })
                                )
                        )
                )
        );

        dispatcher.register(
            Commands.literal("pardon")
                .requires(source -> Commands.LEVEL_ADMINS.check(source.permissions()))
                .executes(ctx -> {
                    ctx.getSource().sendSuccess(() -> Component.literal("§eИспользование: /pardon <ник>"), false);
                    return 0;
                })
                .then(
                    Commands.argument("targets", GameProfileArgument.gameProfile())
                        .suggests((ctx, builder) -> {
                            Object[] list = ctx.getSource().getServer().getPlayerList().getBans().getUserList();
                            return net.minecraft.commands.SharedSuggestionProvider.suggest(
                                java.util.Arrays.stream(list)
                                    .map(entryObj -> ((net.minecraft.server.players.UserBanListEntry) entryObj).getUser().name()),
                                builder
                            );
                        })
                        .executes(ctx -> pardon(ctx.getSource(), GameProfileArgument.getGameProfiles(ctx, "targets")))
                )
        );

        dispatcher.register(
            Commands.literal("op")
                .requires(source -> Commands.LEVEL_ADMINS.check(source.permissions()))
                .executes(ctx -> {
                    ctx.getSource().sendSuccess(() -> Component.literal("§eИспользование: /op <ник> [уровень 0-3]"), false);
                    return 0;
                })
                .then(
                    Commands.argument("targets", GameProfileArgument.gameProfile())
                        .executes(ctx -> op(ctx.getSource(), GameProfileArgument.getGameProfiles(ctx, "targets"), 4))
                        .then(
                            Commands.argument("level", IntegerArgumentType.integer(0, 3))
                                .executes(ctx -> op(ctx.getSource(), GameProfileArgument.getGameProfiles(ctx, "targets"), IntegerArgumentType.getInteger(ctx, "level")))
                        )
                )
        );

        dispatcher.register(
            Commands.literal("deop")
                .requires(source -> Commands.LEVEL_ADMINS.check(source.permissions()))
                .executes(ctx -> {
                    ctx.getSource().sendSuccess(() -> Component.literal("§eИспользование: /deop <ник>"), false);
                    return 0;
                })
                .then(
                    Commands.argument("targets", GameProfileArgument.gameProfile())
                        .suggests((ctx, builder) -> {
                            return net.minecraft.commands.SharedSuggestionProvider.suggest(
                                ctx.getSource().getServer().getPlayerList().getPlayers()
                                    .stream()
                                    .filter(p -> ctx.getSource().getServer().getPlayerList().isOp(p.nameAndId()))
                                    .map(p -> p.getGameProfile().name()),
                                builder
                            );
                        })
                        .executes(ctx -> deop(ctx.getSource(), GameProfileArgument.getGameProfiles(ctx, "targets")))
                )
        );

        dispatcher.register(
            Commands.literal("mute")
                .requires(source -> Commands.LEVEL_ADMINS.check(source.permissions()))
                .executes(ctx -> {
                    ctx.getSource().sendSuccess(() -> Component.literal("§eИспользование: /mute <ник>"), false);
                    return 0;
                })
                .then(
                    Commands.argument("targets", GameProfileArgument.gameProfile())
                        .executes(ctx -> mute(ctx.getSource(), GameProfileArgument.getGameProfiles(ctx, "targets")))
                )
        );

        dispatcher.register(
            Commands.literal("unmute")
                .requires(source -> Commands.LEVEL_ADMINS.check(source.permissions()))
                .executes(ctx -> {
                    ctx.getSource().sendSuccess(() -> Component.literal("§eИспользование: /unmute <ник>"), false);
                    return 0;
                })
                .then(
                    Commands.argument("targets", GameProfileArgument.gameProfile())
                        .suggests((ctx, builder) -> {
                            return net.minecraft.commands.SharedSuggestionProvider.suggest(
                                ctx.getSource().getServer().getPlayerList().getPlayers()
                                    .stream()
                                    .filter(p -> MuteManager.isMuted(p.getUUID()))
                                    .map(p -> p.getGameProfile().name()),
                                builder
                            );
                        })
                        .executes(ctx -> unmute(ctx.getSource(), GameProfileArgument.getGameProfiles(ctx, "targets")))
                )
        );



        dispatcher.register(
            Commands.literal("register")
                .then(
                    Commands.argument("password", StringArgumentType.string())
                        .then(
                            Commands.argument("confirmPassword", StringArgumentType.string())
                                .executes(ctx -> registerPlayer(ctx.getSource(), StringArgumentType.getString(ctx, "password"), StringArgumentType.getString(ctx, "confirmPassword")))
                        )
                )
                .then(
                    Commands.argument("targetPlayer", GameProfileArgument.gameProfile())
                        .requires(source -> Commands.LEVEL_ADMINS.check(source.permissions()))
                        .then(
                            Commands.literal("reset")
                                .executes(ctx -> resetPlayerRegistration(ctx.getSource(), GameProfileArgument.getGameProfiles(ctx, "targetPlayer")))
                        )
                )
        );

        dispatcher.register(
            Commands.literal("login")
                .then(
                    Commands.argument("password", StringArgumentType.string())
                        .executes(ctx -> loginPlayer(ctx.getSource(), StringArgumentType.getString(ctx, "password")))
                )
        );

        dispatcher.register(
            Commands.literal("changepassword")
                .then(
                    Commands.argument("oldPassword", StringArgumentType.string())
                        .then(
                            Commands.argument("newPassword", StringArgumentType.string())
                                .executes(ctx -> changePlayerPassword(ctx.getSource(), StringArgumentType.getString(ctx, "oldPassword"), StringArgumentType.getString(ctx, "newPassword")))
                        )
                )
        );

        dispatcher.register(
            Commands.literal("ban-ip")
                .requires(source -> Commands.LEVEL_ADMINS.check(source.permissions()))
                .executes(ctx -> {
                    ctx.getSource().sendSuccess(() -> Component.literal("§eИспользование: /ban-ip <ip|player> [причина]"), false);
                    return 0;
                })
                .then(
                    Commands.argument("target", StringArgumentType.string())
                        .executes(ctx -> banIp(ctx.getSource(), StringArgumentType.getString(ctx, "target"), null))
                        .then(
                            Commands.argument("reason", StringArgumentType.greedyString())
                                .executes(ctx -> banIp(ctx.getSource(), StringArgumentType.getString(ctx, "target"), StringArgumentType.getString(ctx, "reason")))
                        )
                )
        );

        dispatcher.register(
            Commands.literal("pardon-ip")
                .requires(source -> Commands.LEVEL_ADMINS.check(source.permissions()))
                .executes(ctx -> {
                    ctx.getSource().sendSuccess(() -> Component.literal("§eИспользование: /pardon-ip <ip>"), false);
                    return 0;
                })
                .then(
                    Commands.argument("ip", StringArgumentType.string())
                        .executes(ctx -> pardonIp(ctx.getSource(), StringArgumentType.getString(ctx, "ip")))
                )
        );

        dispatcher.register(
            Commands.literal("invsee")
                .requires(source -> Commands.LEVEL_GAMEMASTERS.check(source.permissions()))
                .executes(ctx -> {
                    ctx.getSource().sendSuccess(() -> Component.literal("§eИспользование: /invsee <ник>"), false);
                    return 0;
                })
                .then(
                    Commands.argument("target", GameProfileArgument.gameProfile())
                        .executes(ctx -> {
                            Collection<net.minecraft.server.players.NameAndId> profiles = GameProfileArgument.getGameProfiles(ctx, "target");
                            if (profiles.isEmpty()) {
                                throw new SimpleCommandExceptionType(Component.literal("§cИгрок не найден.")).create();
                            }
                            net.minecraft.server.players.NameAndId profile = profiles.iterator().next();
                            net.minecraft.server.level.ServerPlayer targetPlayer = ctx.getSource().getServer().getPlayerList().getPlayer(profile.id());
                            if (targetPlayer == null) {
                                throw new SimpleCommandExceptionType(Component.literal("§cИгрок не в сети.")).create();
                            }
                            return invsee(ctx.getSource(), targetPlayer);
                        })
                )
        );
    }

    private static int whitelistSetEnabled(CommandSourceStack source, boolean enabled) {
        source.getServer().setUsingWhitelist(enabled);
        source.sendSuccess(() -> Component.literal("§aБелый список " + (enabled ? "включен" : "выключен")), true);
        return 1;
    }

    private static int whitelistList(CommandSourceStack source) {
        String[] list = source.getServer().getPlayerList().getWhiteListNames();
        if (list.length == 0) {
            source.sendSuccess(() -> Component.literal("§eБелый список пуст."), false);
        } else {
            source.sendSuccess(() -> Component.literal("§aИгроки в белом списке: " + String.join(", ", list)), false);
        }
        return list.length;
    }

    private static int whitelistAdd(CommandSourceStack source, Collection<NameAndId> targets) throws CommandSyntaxException {
        net.minecraft.server.players.UserWhiteList whitelist = source.getServer().getPlayerList().getWhiteList();
        int count = 0;
        for (NameAndId nameAndId : targets) {
            if (!whitelist.isWhiteListed(nameAndId)) {
                whitelist.add(new net.minecraft.server.players.UserWhiteListEntry(nameAndId));
                count++;
                source.sendSuccess(() -> Component.literal("§aИгрок " + nameAndId.name() + " добавлен в белый список."), true);
            }
        }
        if (count == 0) {
            throw new SimpleCommandExceptionType(Component.literal("§cИгрок уже в белом списке.")).create();
        }
        return count;
    }

    private static int whitelistRemove(CommandSourceStack source, Collection<NameAndId> targets) throws CommandSyntaxException {
        net.minecraft.server.players.UserWhiteList whitelist = source.getServer().getPlayerList().getWhiteList();
        int count = 0;
        for (NameAndId nameAndId : targets) {
            if (source.getServer().isSingleplayerOwner(nameAndId)) {
                source.sendFailure(Component.literal("§cНельзя удалить создателя сервера из вайтлиста!"));
                continue;
            }

            if (whitelist.isWhiteListed(nameAndId)) {
                whitelist.remove(nameAndId);
                count++;
                source.sendSuccess(() -> Component.literal("§aИгрок " + nameAndId.name() + " удален из белого списка."), true);

                net.minecraft.server.level.ServerPlayer player = source.getServer().getPlayerList().getPlayer(nameAndId.id());
                if (player != null && !source.getServer().isSingleplayerOwner(nameAndId)) {
                    player.connection.disconnect(Component.literal("§cВы были удалены из белого списка сервера!"));
                }
            }
        }
        if (count == 0) {
            throw new SimpleCommandExceptionType(Component.literal("§cИгроков не было в белом списке.")).create();
        }
        return count;
    }

    private static int ban(CommandSourceStack source, Collection<NameAndId> targets, Date expires, String reason) throws CommandSyntaxException {
        net.minecraft.server.players.UserBanList banList = source.getServer().getPlayerList().getBans();
        int count = 0;
        String finalReason = reason == null ? "Вы заблокированы на этом сервере" : reason;

        for (NameAndId nameAndId : targets) {
            if (source.getServer().isSingleplayerOwner(nameAndId)) {
                source.sendFailure(Component.literal("§cНельзя забанить создателя сервера!"));
                continue;
            }

            if (!banList.isBanned(nameAndId)) {
                net.minecraft.server.players.UserBanListEntry entry = new net.minecraft.server.players.UserBanListEntry(
                    nameAndId,
                    new Date(),
                    source.getTextName(),
                    expires,
                    finalReason
                );
                banList.add(entry);
                count++;

                String timeMsg = expires == null ? "навсегда" : "до " + expires.toString();
                source.sendSuccess(() -> Component.literal("§aИгрок " + nameAndId.name() + " забанен " + timeMsg + " по причине: " + finalReason), true);

                net.minecraft.server.level.ServerPlayer player = source.getServer().getPlayerList().getPlayer(nameAndId.id());
                if (player != null) {
                    player.connection.disconnect(Component.literal("§cВы заблокированы на этом сервере!\n§fПричина: " + finalReason + (expires == null ? "" : "\n§7Срок: " + expires.toString())));
                }
            }
        }

        if (count == 0) {
            throw new SimpleCommandExceptionType(Component.literal("§cИгрок уже забанен.")).create();
        }
        return count;
    }

    private static int pardon(CommandSourceStack source, Collection<NameAndId> targets) throws CommandSyntaxException {
        net.minecraft.server.players.UserBanList banList = source.getServer().getPlayerList().getBans();
        int count = 0;

        for (NameAndId nameAndId : targets) {
            if (banList.isBanned(nameAndId)) {
                banList.remove(nameAndId);
                count++;
                source.sendSuccess(() -> Component.literal("§aИгрок " + nameAndId.name() + " разбанен."), true);
            }
        }

        if (count == 0) {
            throw new SimpleCommandExceptionType(Component.literal("§cИгрок не был забанен.")).create();
        }
        return count;
    }

    private static int op(CommandSourceStack source, Collection<NameAndId> targets, int level) throws CommandSyntaxException {
        net.minecraft.server.players.PlayerList playerList = source.getServer().getPlayerList();
        int count = 0;
        for (NameAndId nameAndId : targets) {
            if (source.getServer().isSingleplayerOwner(nameAndId)) {
                source.sendFailure(Component.literal("§cНельзя изменить права создателя сервера!"));
                continue;
            }

            net.minecraft.server.permissions.PermissionLevel permLevel = net.minecraft.server.permissions.PermissionLevel.byId(level);
            net.minecraft.server.permissions.LevelBasedPermissionSet permSet = net.minecraft.server.permissions.LevelBasedPermissionSet.forLevel(permLevel);

            playerList.getOps().add(new net.minecraft.server.players.ServerOpListEntry(
                nameAndId,
                permSet,
                playerList.getOps().canBypassPlayerLimit(nameAndId)
            ));
            count++;

            source.sendSuccess(() -> Component.literal("§aИгроку " + nameAndId.name() + " выдан уровень прав: " + level), true);

            net.minecraft.server.level.ServerPlayer player = playerList.getPlayer(nameAndId.id());
            if (player != null) {
                playerList.sendPlayerPermissionLevel(player);
                player.sendSystemMessage(Component.literal("§aВы получили уровень прав: §e" + level));
            }
        }
        if (count == 0) {
            throw new SimpleCommandExceptionType(Component.literal("§cНе удалось выдать права.")).create();
        }
        return count;
    }

    private static int deop(CommandSourceStack source, Collection<NameAndId> targets) throws CommandSyntaxException {
        net.minecraft.server.players.PlayerList playerList = source.getServer().getPlayerList();
        int count = 0;
        for (NameAndId nameAndId : targets) {
            if (source.getServer().isSingleplayerOwner(nameAndId)) {
                source.sendFailure(Component.literal("§cНельзя разжаловать создателя сервера!"));
                continue;
            }

            if (playerList.isOp(nameAndId)) {
                playerList.getOps().remove(nameAndId);
                count++;
                source.sendSuccess(() -> Component.literal("§aИгрок " + nameAndId.name() + " больше не является оператором."), true);

                net.minecraft.server.level.ServerPlayer player = playerList.getPlayer(nameAndId.id());
                if (player != null) {
                    playerList.sendPlayerPermissionLevel(player);
                    player.sendSystemMessage(Component.literal("§cВы больше не являетесь оператором сервера."));
                }
            }
        }
        if (count == 0) {
            throw new SimpleCommandExceptionType(Component.literal("§cИгрок не был оператором.")).create();
        }
        return count;
    }

    private static int mute(CommandSourceStack source, Collection<NameAndId> targets) throws CommandSyntaxException {
        int count = 0;
        for (NameAndId nameAndId : targets) {
            if (source.getServer().isSingleplayerOwner(nameAndId)) {
                source.sendFailure(Component.literal("§cНельзя замутить создателя сервера!"));
                continue;
            }
            if (!MuteManager.isMuted(nameAndId.id())) {
                MuteManager.mute(nameAndId.id());
                count++;
                source.sendSuccess(() -> Component.literal("§aИгрок " + nameAndId.name() + " заблокирован в чате."), true);

                net.minecraft.server.level.ServerPlayer player = source.getServer().getPlayerList().getPlayer(nameAndId.id());
                if (player != null) {
                    player.sendSystemMessage(Component.literal("§cВы заблокированы в чате (замучены) на этом сервере!"));
                }
            }
        }
        if (count == 0) {
            throw new SimpleCommandExceptionType(Component.literal("§cИгрок уже в муте.")).create();
        }
        return count;
    }

    private static int unmute(CommandSourceStack source, Collection<NameAndId> targets) throws CommandSyntaxException {
        int count = 0;
        for (NameAndId nameAndId : targets) {
            if (MuteManager.isMuted(nameAndId.id())) {
                MuteManager.unmute(nameAndId.id());
                count++;
                source.sendSuccess(() -> Component.literal("§aИгрок " + nameAndId.name() + " разблокирован в чате."), true);

                net.minecraft.server.level.ServerPlayer player = source.getServer().getPlayerList().getPlayer(nameAndId.id());
                if (player != null) {
                    player.sendSystemMessage(Component.literal("§aВы разблокированы в чате на этом сервере!"));
                }
            }
        }
        if (count == 0) {
            throw new SimpleCommandExceptionType(Component.literal("§cИгрок не был замучен.")).create();
        }
        return count;
    }

    // бля, я заебался
    private static Date parseDuration(String str) {
        Pattern p = Pattern.compile("^(\\d+)([smhdwy])$");
        Matcher m = p.matcher(str.toLowerCase());
        if (!m.matches()) return null;
        long val = Long.parseLong(m.group(1));
        char unit = m.group(2).charAt(0);
        long ms = 0;
        if (unit == 's') { ms = val * 1000L; }
        else if (unit == 'm') { ms = val * 60L * 1000L; }
        else if (unit == 'h') { ms = val * 60L * 60L * 1000L; }
        else if (unit == 'd') { ms = val * 24L * 60L * 60L * 1000L; }
        else if (unit == 'w') { ms = val * 7L * 24L * 60L * 60L * 1000L; }
        else if (unit == 'y') { ms = val * 365L * 24L * 60L * 60L * 1000L; }
        return new Date(System.currentTimeMillis() + ms);
    }



    private static int registerPlayer(CommandSourceStack source, String password, String confirm) throws CommandSyntaxException {
        net.minecraft.server.level.ServerPlayer player = source.getPlayer();
        if (player == null) {
            throw new SimpleCommandExceptionType(Component.literal("§cЭту команду можно выполнять только игроку!")).create();
        }

        java.util.UUID uuid = player.getUUID();
        if (com.radminplus.AuthManager.LOGGED_IN_PLAYERS.contains(uuid)) {
            throw new SimpleCommandExceptionType(Component.literal("§cВы уже авторизованы!")).create();
        }

        if (com.radminplus.AuthManager.isRegistered(uuid)) {
            throw new SimpleCommandExceptionType(Component.literal("§cВы уже зарегистрированы! Войдите с помощью /login <пароль>")).create();
        }

        if (!password.equals(confirm)) {
            throw new SimpleCommandExceptionType(Component.literal("§cПароли не совпадают!")).create();
        }

        if (password.length() < 4) {
            throw new SimpleCommandExceptionType(Component.literal("§cПароль слишком короткий (минимум 4 символа).")).create();
        }

        String ip = getPlayerIp(player);
        boolean success = com.radminplus.AuthManager.register(uuid, player.getGameProfile().name(), password, ip);
        if (success) {
            com.radminplus.AuthManager.LOGGED_IN_PLAYERS.add(uuid);
            source.sendSuccess(() -> Component.literal("§aВы успешно зарегистрированы и вошли в игру!"), false);
        } else {
            throw new SimpleCommandExceptionType(Component.literal("§cОшибка регистрации.")).create();
        }
        return 1;
    }

    private static int loginPlayer(CommandSourceStack source, String password) throws CommandSyntaxException {
        net.minecraft.server.level.ServerPlayer player = source.getPlayer();
        if (player == null) {
            throw new SimpleCommandExceptionType(Component.literal("§cЭту команду можно выполнять только игроку!")).create();
        }

        java.util.UUID uuid = player.getUUID();
        if (com.radminplus.AuthManager.LOGGED_IN_PLAYERS.contains(uuid)) {
            throw new SimpleCommandExceptionType(Component.literal("§cВы уже авторизованы!")).create();
        }

        if (!com.radminplus.AuthManager.isRegistered(uuid)) {
            throw new SimpleCommandExceptionType(Component.literal("§cВы еще не зарегистрированы! Воспользуйтесь /register <пароль> <повтор>")).create();
        }

        String ip = getPlayerIp(player);
        boolean success = com.radminplus.AuthManager.checkLogin(uuid, password, ip);
        if (success) {
            com.radminplus.AuthManager.LOGGED_IN_PLAYERS.add(uuid);
            source.sendSuccess(() -> Component.literal("§aВы успешно вошли в игру!"), false);
        } else {
            throw new SimpleCommandExceptionType(Component.literal("§cНеверный пароль!")).create();
        }
        return 1;
    }

    private static int changePlayerPassword(CommandSourceStack source, String oldPassword, String newPassword) throws CommandSyntaxException {
        net.minecraft.server.level.ServerPlayer player = source.getPlayer();
        if (player == null) {
            throw new SimpleCommandExceptionType(Component.literal("§cЭту команду можно выполнять только игроку!")).create();
        }

        java.util.UUID uuid = player.getUUID();
        if (!com.radminplus.AuthManager.LOGGED_IN_PLAYERS.contains(uuid)) {
            throw new SimpleCommandExceptionType(Component.literal("§cВы должны сначала авторизоваться!")).create();
        }

        if (newPassword.length() < 4) {
            throw new SimpleCommandExceptionType(Component.literal("§cНовый пароль слишком короткий (минимум 4 символа).")).create();
        }

        boolean success = com.radminplus.AuthManager.changePassword(uuid, oldPassword, newPassword);
        if (success) {
            source.sendSuccess(() -> Component.literal("§aПароль успешно изменен!"), false);
        } else {
            throw new SimpleCommandExceptionType(Component.literal("§cНеверный старый пароль!")).create();
        }
        return 1;
    }

    private static int resetPlayerRegistration(CommandSourceStack source, Collection<net.minecraft.server.players.NameAndId> targets) throws CommandSyntaxException {
        int count = 0;
        for (net.minecraft.server.players.NameAndId profile : targets) {
            java.util.UUID uuid = profile.id();
            boolean removed = com.radminplus.AuthManager.reset(uuid);
            if (removed) {
                count++;
                source.sendSuccess(() -> Component.literal("§aРегистрация для игрока " + profile.name() + " успешно сброшена!"), true);

                net.minecraft.server.level.ServerPlayer targetPlayer = source.getServer().getPlayerList().getPlayer(uuid);
                if (targetPlayer != null) {
                    targetPlayer.sendSystemMessage(Component.literal("§cВаша регистрация была сброшена администратором! Пожалуйста, зарегистрируйтесь заново: /register <пароль> <повтор>"));
                }
            }
        }
        if (count == 0) {
            throw new SimpleCommandExceptionType(Component.literal("§cУказанный игрок не зарегистрирован на сервере.")).create();
        }
        return count;
    }

    private static final Pattern IP_PATTERN = Pattern.compile("^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");

    private static int banIp(CommandSourceStack source, String target, String reason) throws CommandSyntaxException {
        net.minecraft.server.players.IpBanList ipBans = source.getServer().getPlayerList().getIpBans();
        String ip = null;
        String name = null;
        
        Matcher matcher = IP_PATTERN.matcher(target);
        if (matcher.matches()) {
            ip = target;
        } else {
            net.minecraft.server.level.ServerPlayer player = source.getServer().getPlayerList().getPlayerByName(target);
            if (player != null) {
                ip = getPlayerIp(player);
                name = player.getGameProfile().name();
                if (source.getServer().isSingleplayerOwner(player.nameAndId())) {
                    throw new SimpleCommandExceptionType(Component.literal("§cНельзя забанить IP создателя сервера!")).create();
                }
            } else {
                throw new SimpleCommandExceptionType(Component.literal("§cНедопустимый IP-адрес или игрок не в сети.")).create();
            }
        }
        
        String finalReason = reason == null ? "Вы заблокированы по IP на этом сервере" : reason;
        
        if (ipBans.isBanned(ip)) {
            throw new SimpleCommandExceptionType(Component.literal("§cЭтот IP-адрес уже забанен.")).create();
        }
        
        net.minecraft.server.players.IpBanListEntry entry = new net.minecraft.server.players.IpBanListEntry(
            ip,
            new Date(),
            source.getTextName(),
            null,
            finalReason
        );
        ipBans.add(entry);
        
        String displayMsg = name != null ? "Игрок " + name + " (IP: " + ip + ")" : "IP-адрес " + ip;
        source.sendSuccess(() -> Component.literal("§a" + displayMsg + " забанен по причине: " + finalReason), true);
        
        for (net.minecraft.server.level.ServerPlayer p : source.getServer().getPlayerList().getPlayers()) {
            if (ip.equals(getPlayerIp(p))) {
                if (!source.getServer().isSingleplayerOwner(p.nameAndId())) {
                    p.connection.disconnect(Component.literal("§cВаш IP-адрес заблокирован на этом сервере!\n§fПричина: " + finalReason));
                }
            }
        }
        
        return 1;
    }

    private static int pardonIp(CommandSourceStack source, String ip) throws CommandSyntaxException {
        net.minecraft.server.players.IpBanList ipBans = source.getServer().getPlayerList().getIpBans();
        
        Matcher matcher = IP_PATTERN.matcher(ip);
        if (!matcher.matches()) {
            throw new SimpleCommandExceptionType(Component.literal("§cНедопустимый формат IP-адреса.")).create();
        }
        
        if (!ipBans.isBanned(ip)) {
            throw new SimpleCommandExceptionType(Component.literal("§cЭтот IP-адрес не забанен.")).create();
        }
        
        ipBans.remove(ip);
        source.sendSuccess(() -> Component.literal("§aIP-адрес " + ip + " разбанен."), true);
        return 1;
    }

    private static int invsee(CommandSourceStack source, net.minecraft.server.level.ServerPlayer target) throws CommandSyntaxException {
        net.minecraft.server.level.ServerPlayer operator = source.getPlayerOrException();
        
        operator.openMenu(new net.minecraft.world.SimpleMenuProvider(
            (containerId, playerInventory, player) -> new net.minecraft.world.inventory.ChestMenu(
                net.minecraft.world.inventory.MenuType.GENERIC_9x5,
                containerId,
                playerInventory,
                new InvseeContainer(target),
                5
            ),
            Component.literal("Инвентарь: " + target.getGameProfile().name())
        ));
        
        return 1;
    }

    private static String getPlayerIp(net.minecraft.server.level.ServerPlayer player) {
        String remoteAddress = player.connection.getRemoteAddress().toString();
        if (remoteAddress.startsWith("/")) {
            remoteAddress = remoteAddress.substring(1);
        }
        if (remoteAddress.contains(":")) {
            remoteAddress = remoteAddress.split(":")[0];
        }
        return remoteAddress;
    }
}
