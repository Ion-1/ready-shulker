package net.ion1.readyshulker;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReadyShulker implements ModInitializer {
	public static final String MOD_ID = "ready-shulker";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ServerTickEvents.END_SERVER_TICK.register(server -> QueuedMenuProvider.tick());
	}
}