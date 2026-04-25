package dev.evvie.waylandcraft.render;

import java.util.function.Function;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;

import dev.evvie.waylandcraft.WaylandCraft;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;
import net.minecraft.util.TriState;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class RenderUtils {
	
	/* Window Cutout */
	private static final RenderPipeline WINDOW_CUTOUT_PIPELINE = RenderPipeline.builder(RenderPipelines.MATRICES_SNIPPET)
			.withLocation(ResourceLocation.fromNamespaceAndPath(WaylandCraft.MOD_ID, "pipeline/window_cutout"))
			.withVertexShader(ResourceLocation.fromNamespaceAndPath(WaylandCraft.MOD_ID, "core/rendertype_window"))
			.withFragmentShader(ResourceLocation.fromNamespaceAndPath(WaylandCraft.MOD_ID, "core/rendertype_window"))
			.withShaderDefine("ALPHA_CUTOUT")
			.withSampler("Sampler0")
			.withVertexFormat(DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.QUADS)
			.build();
	
	public static final Function<ResourceLocation, RenderType> WINDOW_CUTOUT = Util.memoize(
		(location) -> {
			RenderType.CompositeState compositeState = RenderType.CompositeState.builder()
					.setTextureState(new RenderStateShard.TextureStateShard(location, TriState.FALSE, false))
					.createCompositeState(true);
			return RenderType.create("window_cutout", RenderType.TRANSIENT_BUFFER_SIZE, true, true, WINDOW_CUTOUT_PIPELINE, compositeState);
		}
	);
	
	/* Window Translucent */
	private static final RenderPipeline WINDOW_TRANSLUCENT_PIPELINE = RenderPipeline.builder(RenderPipelines.MATRICES_SNIPPET)
			.withLocation(ResourceLocation.fromNamespaceAndPath(WaylandCraft.MOD_ID, "pipeline/window_translucent"))
			.withVertexShader(ResourceLocation.fromNamespaceAndPath(WaylandCraft.MOD_ID, "core/rendertype_window"))
			.withFragmentShader(ResourceLocation.fromNamespaceAndPath(WaylandCraft.MOD_ID, "core/rendertype_window"))
			.withSampler("Sampler0")
			.withBlend(BlendFunction.TRANSLUCENT)
			.withVertexFormat(DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.QUADS)
			.build();
	
	public static final Function<ResourceLocation, RenderType> WINDOW_TRANSLUCENT = Util.memoize(
		(location) -> {
			RenderType.CompositeState compositeState = RenderType.CompositeState.builder()
					.setTextureState(new RenderStateShard.TextureStateShard(location, TriState.FALSE, false))
					.createCompositeState(true);
			return RenderType.create("window_translucent", RenderType.TRANSIENT_BUFFER_SIZE, true, true, WINDOW_TRANSLUCENT_PIPELINE, compositeState);
		}
	);
	
	/* Window Cutout Background */
	private static final RenderPipeline WINDOW_CUTOUT_BACKGROUND_PIPELINE = RenderPipeline.builder(RenderPipelines.MATRICES_SNIPPET)
			.withLocation(ResourceLocation.fromNamespaceAndPath(WaylandCraft.MOD_ID, "pipeline/window_cutout"))
			.withVertexShader(ResourceLocation.fromNamespaceAndPath(WaylandCraft.MOD_ID, "core/rendertype_window"))
			.withFragmentShader(ResourceLocation.fromNamespaceAndPath(WaylandCraft.MOD_ID, "core/rendertype_window"))
			.withShaderDefine("ALPHA_CUTOUT")
			.withShaderDefine("NO_COLOR")
			.withSampler("Sampler0")
			.withVertexFormat(DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.QUADS)
			.build();
	
	public static final Function<ResourceLocation, RenderType> WINDOW_BACKGROUND_CUTOUT = Util.memoize(
		(location) -> {
			RenderType.CompositeState compositeState = RenderType.CompositeState.builder()
					.setTextureState(new RenderStateShard.TextureStateShard(location, TriState.FALSE, false))
					.createCompositeState(true);
			return RenderType.create("window_cutout", RenderType.TRANSIENT_BUFFER_SIZE, true, true, WINDOW_CUTOUT_BACKGROUND_PIPELINE, compositeState);
		}
	);
	
	/* Window Translucent Background */
	private static final RenderPipeline WINDOW_TRANSLUCENT_BACKGROUND_PIPELINE = RenderPipeline.builder(RenderPipelines.MATRICES_SNIPPET)
			.withLocation(ResourceLocation.fromNamespaceAndPath(WaylandCraft.MOD_ID, "pipeline/window_translucent"))
			.withVertexShader(ResourceLocation.fromNamespaceAndPath(WaylandCraft.MOD_ID, "core/rendertype_window"))
			.withFragmentShader(ResourceLocation.fromNamespaceAndPath(WaylandCraft.MOD_ID, "core/rendertype_window"))
			.withSampler("Sampler0")
			.withShaderDefine("NO_COLOR")
			.withBlend(BlendFunction.TRANSLUCENT)
			.withVertexFormat(DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.QUADS)
			.build();
	
	public static final Function<ResourceLocation, RenderType> WINDOW_BACKGROUND_TRANSLUCENT = Util.memoize(
		(location) -> {
			RenderType.CompositeState compositeState = RenderType.CompositeState.builder()
					.setTextureState(new RenderStateShard.TextureStateShard(location, TriState.FALSE, false))
					.createCompositeState(true);
			return RenderType.create("window_translucent", RenderType.TRANSIENT_BUFFER_SIZE, true, true, WINDOW_TRANSLUCENT_BACKGROUND_PIPELINE, compositeState);
		}
	);
	
	public static void renderFramebuffer(WindowFramebuffer framebuffer, boolean cutout, Pose pose, Vec3 pos1, Vec3 pos2, Vec3 pos3, Vec3 pos4, Vec2 uv1, Vec2 uv2, Vec2 uv3, Vec2 uv4) {
		if(!framebuffer.isValid()) return;
		
		BufferSource source = Minecraft.getInstance().renderBuffers().bufferSource();
		
		// Front quad
		Function<ResourceLocation, RenderType> renderType = cutout ? WINDOW_CUTOUT : WINDOW_TRANSLUCENT;
		VertexConsumer buffer = source.getBuffer(renderType.apply(framebuffer.getTextureLocation()));
		buffer.addVertex(pose, pos1.toVector3f()).setUv(uv1.x, uv1.y);
		buffer.addVertex(pose, pos2.toVector3f()).setUv(uv2.x, uv2.y);
		buffer.addVertex(pose, pos3.toVector3f()).setUv(uv3.x, uv3.y);
		buffer.addVertex(pose, pos4.toVector3f()).setUv(uv4.x, uv4.y);
		source.endBatch();
		
		// Back quad
		renderType = cutout ? WINDOW_BACKGROUND_CUTOUT : WINDOW_BACKGROUND_TRANSLUCENT;
		buffer = source.getBuffer(renderType.apply(framebuffer.getTextureLocation()));
		buffer.addVertex(pose, pos4.toVector3f()).setUv(uv4.x, uv4.y);
		buffer.addVertex(pose, pos3.toVector3f()).setUv(uv3.x, uv3.y);
		buffer.addVertex(pose, pos2.toVector3f()).setUv(uv2.x, uv2.y);
		buffer.addVertex(pose, pos1.toVector3f()).setUv(uv1.x, uv1.y);
		source.endBatch();
	}
	
	public static Pose cameraTransform(Camera camera) {
		PoseStack poseStack = new PoseStack();
		poseStack.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));
		poseStack.mulPose(Axis.YP.rotationDegrees(camera.getYRot() + 180.0F));
		poseStack.translate(-camera.getPosition().x, -camera.getPosition().y, -camera.getPosition().z);
		return poseStack.last();
	}
	
	public static void blit(GuiGraphics context, ResourceLocation location, float x, float y, float width, float height) {
		Pose pose = context.pose().last();
		BufferSource source = Minecraft.getInstance().renderBuffers().bufferSource();
		VertexConsumer buffer = source.getBuffer(RenderType.guiTextured(location));
		buffer.addVertex(pose, x        , y         , 0).setUv(0, 0).setColor(ARGB.white(1.0f));
		buffer.addVertex(pose, x        , y + height, 0).setUv(0, 1).setColor(ARGB.white(1.0f));
		buffer.addVertex(pose, x + width, y + height, 0).setUv(1, 1).setColor(ARGB.white(1.0f));
		buffer.addVertex(pose, x + width, y         , 0).setUv(1, 0).setColor(ARGB.white(1.0f));
	}
	
}
