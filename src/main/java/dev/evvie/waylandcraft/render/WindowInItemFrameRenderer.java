package dev.evvie.waylandcraft.render;

import com.mojang.blaze3d.vertex.PoseStack;

import dev.evvie.waylandcraft.bridge.WLCToplevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class WindowInItemFrameRenderer {
	
	public void render(WLCToplevel toplevel, PoseStack poseStack, MultiBufferSource multiBufferSource) {
		if(toplevel.framebuffer == null) return;
		
		poseStack.pushPose();
		poseStack.translate(-1.0f, -1.0f, -0.01f);
		poseStack.scale(2.0f, 2.0f, 1.0f);
		
		float width = toplevel.geometry.width();
		float height = toplevel.geometry.height();
		
		float wscale;
		float hscale;
		
		if(width > height) {
			wscale = 1.0f;
			hscale = height / width;
		}
		else {
			hscale = 1.0f;
			wscale = width / height;
		}
		
		poseStack.translate(0.5f - wscale / 2, 0.5 - hscale / 2, 0.0f);
		poseStack.scale(wscale, hscale, 1);
		
		Vec3 pos1 = new Vec3(1, 1, 0);
		Vec3 pos2 = new Vec3(1, 0, 0);
		Vec3 pos3 = new Vec3(0, 0, 0);
		Vec3 pos4 = new Vec3(0, 1, 0);
		
		Vec2 uv1 = new Vec2(0, 0);
		Vec2 uv2 = new Vec2(0, 1);
		Vec2 uv3 = new Vec2(1, 1);
		Vec2 uv4 = new Vec2(1, 0);
		
		RenderUtils.renderFramebuffer(toplevel.framebuffer, true, poseStack.last(), pos1, pos2, pos3, pos4, uv1, uv2, uv3, uv4);
		
		poseStack.popPose();
	}
	
}
