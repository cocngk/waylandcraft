package dev.evvie.waylandcraft.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.blaze3d.vertex.PoseStack;

import dev.evvie.waylandcraft.bridge.WLCToplevel;
import dev.evvie.waylandcraft.item.WindowItem;
import net.minecraft.client.renderer.ItemModelShaper;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

@Mixin(ItemRenderer.class)
public class ItemRendererMixin {
	
	@Shadow
	private ItemModelShaper itemModelShaper;
	
	@Inject(method = "render", at = @At("HEAD"))
	public void render(
		ItemStack itemStack,
		ItemDisplayContext itemDisplayContext,
		boolean bl,
		PoseStack poseStack,
		MultiBufferSource multiBufferSource,
		int i,
		int j,
		BakedModel bakedModel,
		CallbackInfo info,
		@Local LocalRef<BakedModel> bakedModelLocal
	) {
		if(itemStack.is(WindowItem.WINDOW)) {
			WLCToplevel toplevel = WindowItem.getToplevel(itemStack);
			if(toplevel == null) {
				bakedModelLocal.set(itemModelShaper.getModelManager().getModel(WindowItem.BROKEN_WINDOW_MODEL));
			}
		}
	}
	
}
