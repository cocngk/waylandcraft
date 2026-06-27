package dev.evvie.waylandcraft.network;

import dev.evvie.waylandcraft.WaylandCraftCommon;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public class WaylandCraftNetworking {
	
	public static void register() {
		PayloadTypeRegistry.serverboundPlay().register(ServerboundGiveItemsPayload.TYPE, ServerboundGiveItemsPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(ServerboundAliveWindowsPayload.TYPE, ServerboundAliveWindowsPayload.CODEC);
		
		ServerPlayNetworking.registerGlobalReceiver(ServerboundGiveItemsPayload.TYPE, WaylandCraftCommon.instance.serverItemManager::handleGiveItemsPayload);
		ServerPlayNetworking.registerGlobalReceiver(ServerboundAliveWindowsPayload.TYPE, WaylandCraftCommon.instance.serverItemManager::handleAliveWindowsPayload);
		
	}
	
}
