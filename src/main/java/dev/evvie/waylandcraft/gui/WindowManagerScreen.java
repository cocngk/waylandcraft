package dev.evvie.waylandcraft.gui;

import java.awt.Color;
import java.util.Optional;
import java.util.stream.Stream;

import org.joml.Matrix4f;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import dev.evvie.waylandcraft.BufferTexture;
import dev.evvie.waylandcraft.RenderUtils;
import dev.evvie.waylandcraft.WaylandCraft;
import dev.evvie.waylandcraft.bridge.WLCAbstractWindow;
import dev.evvie.waylandcraft.bridge.WLCSurface;
import dev.evvie.waylandcraft.bridge.WLCSurface.ViewportSource;
import dev.evvie.waylandcraft.bridge.WLCToplevel;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;

public class WindowManagerScreen extends Screen {
	
	private WaylandCraft wlc;
	private SelectorWidget selector;
	
	private final int margin = 5;
	private final int topMargin = 50;
	private final int rootHeight = 1080;
	
	private int areaWidth;
	private int areaHeight;
	private float scale;
	
	public WindowManagerScreen(WaylandCraft wlc) {
		super(Component.literal("Window Manager"));
		this.wlc = wlc;
	}
	
	@Override
	protected void init() {
		areaWidth = width - margin * 2;
		areaHeight = height - margin - topMargin;
		scale = rootHeight / (float) areaHeight;
		
		selector = new SelectorWidget(margin, topMargin - 11, 50, 12, 5);
		addRenderableWidget(selector);
	}
	
	@Override
	public boolean isPauseScreen() {
		return false;
	}
	
	@Override
	public void render(GuiGraphics context, int i, int j, float f) {
		super.render(context, i, j, f);
		
		context.hLine(margin, width - margin, topMargin, Color.white.getRGB());
		context.hLine(margin, width - margin, height - margin, Color.white.getRGB());
		
		context.vLine(margin, topMargin, height - margin, Color.white.getRGB());
		context.vLine(width - margin, topMargin, height - margin, Color.white.getRGB());
		
		WLCToplevel[] toplevels = wlc.bridge.getToplevels();
		selector.setEntries(Stream.of(toplevels).map((t) -> Optional.ofNullable(t.title).orElse("")).toArray(String[]::new));
		
		if(toplevels.length > 0) {
			WLCAbstractWindow window = toplevels[selector.selection()];
			float x = margin + Math.max(0, areaWidth / 2 - (window.geometry.width() / 2) / scale);
			float y = topMargin + Math.max(0, areaHeight / 2 - (window.geometry.height() / 2) / scale);
			renderWindow(context, window, x, y);
		}
	}
	
	private void renderWindow(GuiGraphics context, WLCAbstractWindow window, float x, float y) {
		x -= window.geometry.x() / scale;
		y -= window.geometry.y() / scale;
		for(WLCSurface surface = window.getSurfaceTree(); surface != null; surface = surface.getNextChild()) {
			renderSurface(context, surface, x + surface.xSubpos / scale, y + surface.ySubpos / scale, scale);
		}
	}
	
	private void renderSurface(GuiGraphics context, WLCSurface surface, float x, float y, float scale) {
		BufferTexture buf = surface.getBuffer();
		
		if(buf == null) return;
		
//		WaylandCraft.LOGGER.info("RENDER SURFACE X: " + x + ", Y: " + y + ", S: " + scale + ", B: " + buf);
		
		float w = surface.width() / scale;
		float h = surface.height() / scale;
		
		float crop_x1 = 0.0f;
		float crop_y1 = 0.0f;
		float crop_x2 = 1.0f;
		float crop_y2 = 1.0f;
		
		ViewportSource src = surface.getViewportSource();
		if(src != null) {
			crop_x1 = (float) (src.x / buf.width);
			crop_y1 = (float) (src.y / buf.height);
			crop_x2 = (float) ((src.x + src.width) / buf.width);
			crop_y2 = (float) ((src.y + src.height) / buf.height);
		}
		
		Matrix4f mat = context.pose().last().pose();
		
		Tesselator tesselator = Tesselator.getInstance();
		BufferBuilder vertexBuf = tesselator.getBuilder();
		vertexBuf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
		vertexBuf.vertex(mat, x,     y,     0).color(1.0f, 1.0f, 1.0f, 1.0f).uv(crop_x1, crop_y1).endVertex();
		vertexBuf.vertex(mat, x,     y + h, 0).color(1.0f, 1.0f, 1.0f, 1.0f).uv(crop_x1, crop_y2).endVertex();
		vertexBuf.vertex(mat, x + w, y + h, 0).color(1.0f, 1.0f, 1.0f, 1.0f).uv(crop_x2, crop_y2).endVertex();
		vertexBuf.vertex(mat, x + w, y,     0).color(1.0f, 1.0f, 1.0f, 1.0f).uv(crop_x2, crop_y1).endVertex();
		
		if(buf.format == BufferTexture.FORMAT_XRGB8888) {
			RenderSystem.setShader(RenderUtils::getPositionColorTexShader);
		}
		else if(buf.format == BufferTexture.FORMAT_ARGB8888) {
			RenderSystem.setShader(GameRenderer::getPositionColorTexShader);
		}
//		RenderSystem.setShader(RenderUtils::getPositionColorTexShader);
		
		RenderSystem.setShaderTexture(0, buf.id);
		RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
		tesselator.end();
	}
	
}
