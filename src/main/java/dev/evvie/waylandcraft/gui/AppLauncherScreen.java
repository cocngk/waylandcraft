package dev.evvie.waylandcraft.gui;

import java.awt.Color;
import java.util.List;
import java.util.Random;

import dev.evvie.waylandcraft.WaylandCraft;
import dev.evvie.waylandcraft.desktop.DesktopEntry;
import dev.evvie.waylandcraft.render.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FastColor;

public class AppLauncherScreen extends Screen {
	
	private WaylandCraft wlc;
	
	public AppLauncherScreen(WaylandCraft wlc) {
		super(Component.literal("App Launcher"));
		
		this.wlc = wlc;
	}
	
	@Override
	protected void init() {
		int margin = 20;
		int topMargin = 40;
		int contentWidth = width - margin * 2;
		int contentHeight = height - topMargin - margin;
		
		int elementSize = 64;
		int gapSize = 16;
		int gridSize = elementSize + gapSize;
		
		int cols = contentWidth / gridSize;
		int visibleRows = contentHeight / gridSize;
		
		int gridX = (width - cols * elementSize - (cols - 1) * gapSize) / 2;
		
		List<DesktopEntry> entries = wlc.xdgManager.entries();
		Random random = new Random();
		for(int i = 0; i < 10; i++) {
			DesktopEntry entry = null;
			while(!(entry != null && entry.visible)) entry = entries.get(random.nextInt(entries.size()));
			
			int row = i / cols;
			int col = i % cols;
			
			this.addRenderableWidget(new AppWidget(entry, gridX + col * gridSize, topMargin + row * gridSize, elementSize, elementSize));
		}
	}
	
	@Override
	public boolean isPauseScreen() {
		return false;
	}
	
	private static class AppWidget extends AbstractWidget {
		
		public final DesktopEntry entry;
		private Font font;
		
		public AppWidget(DesktopEntry entry, int x, int y, int width, int height) {
			super(x, y, width, height, Component.literal(getTitle(entry)));
			this.entry = entry;
			this.font = Minecraft.getInstance().font;
		}
		
		private static String getTitle(DesktopEntry entry) {
			return entry.name != null ? entry.name : entry.appId;
		}
		
		@Override
		protected void renderWidget(GuiGraphics context, int mouseX, int mouseY, float partialTicks) {
			String text = getTitle(entry);
			int textWidth = font.width(text);
			int textOffset = Math.max(0, width / 2 - textWidth / 2);
			int iconSize = Math.min(height - font.lineHeight - 4, width - 4);
			int iconOffset = (width - 4) / 2 - iconSize / 2;
			
			int color = FastColor.ARGB32.color(128, 25, 25, 25);
			if(isHoveredOrFocused()) {
				color = FastColor.ARGB32.color(255, 25, 25, 25);
			}
			context.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), color);
			
			context.enableScissor(getX(), getY(), getX() + getWidth(), getY() + getHeight());
			context.drawString(font, text, getX() + textOffset, getY() + getHeight() - font.lineHeight - 2, Color.white.getRGB());
			if(entry.icon != null) RenderUtils.blit(context, entry.icon, getX() + iconOffset + 2, getY() + 2, iconSize, iconSize);
			context.disableScissor();
		}
		
		@Override
		protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
			this.defaultButtonNarrationText(narrationElementOutput);
		}
		
	}
	
}
