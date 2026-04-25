package dev.evvie.waylandcraft.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;

import dev.evvie.waylandcraft.WaylandCraft;
import dev.evvie.waylandcraft.bridge.WLCToplevel;
import dev.evvie.waylandcraft.item.WindowItem;
import dev.evvie.waylandcraft.render.IMyItemFrameRenderState;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemFrameRenderer;
import net.minecraft.client.renderer.entity.state.ItemFrameRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.BlockStateDefinitions;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

@Mixin(ItemFrameRenderer.class)
public class ItemFrameRendererMixin {
	
	@Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V"))
	public void renderItem(ItemStackRenderState itemStackRenderState, PoseStack poseStack, MultiBufferSource multiBufferSource, int light, int overlay, @Local ItemFrameRenderState itemFrameRenderState) {
		WLCToplevel toplevel = ((IMyItemFrameRenderState) itemFrameRenderState).getToplevel();
		
		if(toplevel == null) {
			itemStackRenderState.render(poseStack, multiBufferSource, light, overlay);
			return;
		}
		
		WaylandCraft.instance.windowInItemFrameRenderer.render(toplevel, poseStack, multiBufferSource);
	}
	
	@Inject(method = "extractRenderState", at = @At("TAIL"))
	public void extractRenderState(ItemFrame itemFrame, ItemFrameRenderState itemFrameRenderState, float f, CallbackInfo info) {
		WLCToplevel toplevel = WindowItem.getToplevel(itemFrame.getItem());
		((IMyItemFrameRenderState) itemFrameRenderState).setToplevel(toplevel);
	}
	
	@Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/resources/model/BlockStateDefinitions;getItemFrameFakeState(ZZ)Lnet/minecraft/world/level/block/state/BlockState;"))
	public BlockState changeItemFrameModel(boolean glowFrame, boolean map, @Local ItemFrameRenderState itemFrameRenderState) {
		BlockState state = BlockStateDefinitions.getItemFrameFakeState(glowFrame, map);
		
		WLCToplevel toplevel = ((IMyItemFrameRenderState) itemFrameRenderState).getToplevel();
		if(toplevel != null) {
			state = state.setValue(BlockStateProperties.MAP, true);
		}
		return state;
	}
	
}
