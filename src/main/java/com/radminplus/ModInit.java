package com.radminplus;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleCategory;
import net.minecraft.world.level.gamerules.GameRuleType;
import net.minecraft.world.level.gamerules.GameRuleTypeVisitor;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlagSet;

public class ModInit implements ModInitializer {
	public static final String MOD_ID = "radminplus";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static GameRule<Boolean> RULE_ILLEGALNICK;
	public static GameRule<Boolean> RULE_JOINUNDERNAME;
	public static GameRule<Boolean> RULE_REGISTER;

	@Override
	public void onInitialize() {
		try {
			RULE_ILLEGALNICK = Registry.register(
				BuiltInRegistries.GAME_RULE,
				"illegalnick",
				new GameRule<>(
					GameRuleCategory.PLAYER,
					GameRuleType.BOOL,
					BoolArgumentType.bool(),
					GameRuleTypeVisitor::visitBoolean,
					Codec.BOOL,
					val -> val ? 1 : 0,
					true,
					FeatureFlagSet.of()
				)
			);
			RULE_JOINUNDERNAME = Registry.register(
				BuiltInRegistries.GAME_RULE,
				"joinundername",
				new GameRule<>(
					GameRuleCategory.PLAYER,
					GameRuleType.BOOL,
					BoolArgumentType.bool(),
					GameRuleTypeVisitor::visitBoolean,
					Codec.BOOL,
					val -> val ? 1 : 0,
					true,
					FeatureFlagSet.of()
				)
			);
			RULE_REGISTER = Registry.register(
				BuiltInRegistries.GAME_RULE,
				"register",
				new GameRule<>(
					GameRuleCategory.PLAYER,
					GameRuleType.BOOL,
					BoolArgumentType.bool(),
					GameRuleTypeVisitor::visitBoolean,
					Codec.BOOL,
					val -> val ? 1 : 0,
					false,
					FeatureFlagSet.of()
				)
			);
		} catch (Throwable t) {
			LOGGER.error("Failed to register gamerules", t);
		}

		net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(server -> {
			AuthManager.tick(server);

			// Automatically force dependency gamerules if registration is active
			if (RULE_REGISTER != null && server.getWorldData().getGameRules().get(RULE_REGISTER)) {
				if (RULE_ILLEGALNICK != null && server.getWorldData().getGameRules().get(RULE_ILLEGALNICK)) {
					server.getWorldData().getGameRules().set(RULE_ILLEGALNICK, false, server);
				}
				if (RULE_JOINUNDERNAME != null && server.getWorldData().getGameRules().get(RULE_JOINUNDERNAME)) {
					server.getWorldData().getGameRules().set(RULE_JOINUNDERNAME, false, server);
				}
			}
		});

		net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTING.register(server -> {
			AuthManager.init(server);
		});

		net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			net.minecraft.server.level.ServerPlayer player = handler.player;

			if (RULE_REGISTER == null || !server.getWorldData().getGameRules().get(RULE_REGISTER)) {
				AuthManager.LOGGED_IN_PLAYERS.add(player.getUUID());
				return;
			}

			// If it's the host, auto-login
			if (server.isSingleplayerOwner(player.nameAndId())) {
				AuthManager.LOGGED_IN_PLAYERS.add(player.getUUID());
				player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§aВы автоматически вошли в игру как хост сервера."));
				return;
			}

			// Extract IP address from connection
			String remoteAddress = handler.getRemoteAddress().toString();
			if (remoteAddress.startsWith("/")) {
				remoteAddress = remoteAddress.substring(1);
			}
			if (remoteAddress.contains(":")) {
				remoteAddress = remoteAddress.split(":")[0];
			}
			String ip = remoteAddress;

			// If already registered, check IP
			if (AuthManager.isRegistered(player.getUUID())) {
				String lastIp = AuthManager.getLastIp(player.getUUID());
				if (lastIp != null && lastIp.equals(ip)) {
					AuthManager.LOGGED_IN_PLAYERS.add(player.getUUID());
					player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§aАвтоматический вход выполнен (IP совпадает с предыдущим входом)!"));
				} else {
					player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§eЭтот аккаунт зарегистрирован. Войдите: §6/login <пароль>"));
				}
			} else {
				player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§eЗарегистрируйтесь: §6/register <пароль> <повтор_пароля>"));
			}
		});
	}
}
