package com.radminplus.client;

import net.fabricmc.api.ClientModInitializer;

public class ClientInit implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		UpdateChecker.check();
	}
}
