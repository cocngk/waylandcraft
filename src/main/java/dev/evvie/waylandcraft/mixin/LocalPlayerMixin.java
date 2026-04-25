package dev.evvie.waylandcraft.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import dev.evvie.waylandcraft.WaylandCraft;
import net.minecraft.client.player.LocalPlayer;

@Mixin(LocalPlayer.class)
public class LocalPlayerMixin {
	
	@Redirect(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z"))
	public boolean moveIsUsingItem(LocalPlayer player) {
		// Stop player item use slowdown
		if(WaylandCraft.instance.playerUsingWindowItem) return false;
		return player.isUsingItem();
	}
	
	@Redirect(method = "canStartSprinting", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z"))
	public boolean sprintIsUsingItem(LocalPlayer player) {
		// Stop player item use slowdown
		if(WaylandCraft.instance.playerUsingWindowItem) return false;
		return player.isUsingItem();
	}
	
	@Redirect(method = "modifyInput", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z"))
	public boolean modifyInputIsUsingItem(LocalPlayer player) {
		// Stop player item use slowdown
		if(WaylandCraft.instance.playerUsingWindowItem) return false;
		return player.isUsingItem();
	}
	
}
