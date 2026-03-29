package dev.evvie.waylandcraft.gui;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import dev.evvie.waylandcraft.WaylandCraft;
import dev.evvie.waylandcraft.desktop.DesktopEntry;
import dev.evvie.waylandcraft.render.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractContainerWidget;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;

public class AppLauncherScreen extends Screen {
	
	private WaylandCraft wlc;
	private static final ResourceLocation SLOT_THINGY = new ResourceLocation(WaylandCraft.MOD_ID, "textures/gui/slot_thingy.png");
	private static final ResourceLocation SLOT_THINGY_SELECTED = new ResourceLocation(WaylandCraft.MOD_ID, "textures/gui/slot_thingy_selected_overlay.png");
	
	public AppLauncherScreen(WaylandCraft wlc) {
		super(Component.literal("App Launcher"));
		
		this.wlc = wlc;
	}
	
	@Override
	protected void init() {
		int listWidth = 210;
		int listHeight = 170;
		this.addRenderableWidget(new AppList(wlc, width / 2 - listWidth / 2, height / 2 - listHeight / 2, listWidth, listHeight));
	}
	
	@Override
	public boolean isPauseScreen() {
		return false;
	}
	
	private static class AppList extends AbstractContainerWidget {
		
		private static final int SLOT_GAPS = 4;
		private static final int ELEMENT_WIDTH = 200 + 2;
		private static final int ELEMENT_HEIGHT = 32 + 2;
		
		private WaylandCraft wlc;
		private ArrayList<AppWidget> children = new ArrayList<AppWidget>();
		
		private int maxScroll = 0;
		private int scroll = 0;
		private int contentHeight = 0;
		
		public AppList(WaylandCraft wlc, int x, int y, int width, int height) {
			super(x, y, width, height, Component.literal("App List"));
			this.wlc = wlc;
			
			List<DesktopEntry> allEntries = wlc.xdgManager.entries();
			Random random = new Random();
			
			int count = 10;
			for(int i = 0; i < count; i++) {
				DesktopEntry entry = null;
				while(!(entry != null && entry.visible)) entry = allEntries.get(random.nextInt(allEntries.size()));
				
				children.add(new AppWidget(entry));
			}
			
			rearrangeChildren();
		}
		
		private void rearrangeChildren() {
			int x = getX();
			int y = getY() + 4;
			y -= scroll;
			
			for(int i = 0; i < children.size(); i++) {
				AppWidget widget = children.get(i);
				widget.setRectangle(ELEMENT_WIDTH, ELEMENT_HEIGHT, x + width / 2 - ELEMENT_WIDTH / 2, y + i * (ELEMENT_HEIGHT + SLOT_GAPS));
			}
			
			contentHeight = 4 + children.size() * (ELEMENT_HEIGHT + SLOT_GAPS) - SLOT_GAPS;
			maxScroll = Math.max(contentHeight - height, 0);
		}
		
		private void scrollTo(AppWidget widget) {
			boolean topCondition = widget.getY() >= getY() + 4;
			boolean bottomCondition = widget.getBottom() <= getBottom();
			if(topCondition && bottomCondition) {
				/* Widget already in view */
				return;
			}
			
			int top = children.get(0).getY() - 4;
			int bottomScroll = widget.getBottom() - top - height;
			int topScroll = widget.getY() - top - 4;
			
			if(!bottomCondition) scroll = bottomScroll;
			else scroll = topScroll;
			
			if(scroll < 0) scroll = 0;
			if(scroll > maxScroll) scroll = maxScroll;
		}
		
		@Override
		public void setFocused(GuiEventListener guiEventListener) {
			super.setFocused(guiEventListener);
			if(guiEventListener instanceof AppWidget) scrollTo((AppWidget) guiEventListener);
		}
		
		@Override
		public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
			scroll -= (int) scrollY * 10;
			if(scroll < 0) scroll = 0;
			if(scroll > maxScroll) scroll = maxScroll;
			
			return true;
		}
		
		@Override
		protected void renderWidget(GuiGraphics context, int mouseX, int mouseY, float partialTicks) {
			rearrangeChildren();
			
			int x = getX();
			int y = getY();
			int width = getWidth();
			int height = getHeight();
			
			context.fill(x, y, x + width, y + height, Color.black.getRGB());
			
			context.enableScissor(x, y, x + width, y + height);
			
			for(AppWidget child : children) {
				child.render(context, mouseX, mouseY, partialTicks);
			}
			
			context.disableScissor();
		}
		
		@Override
		protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
		}
		
		@Override
		public List<? extends GuiEventListener> children() {
			return children;
		}
		
	}
	
	private static class AppWidget extends AbstractWidget {
		
		public final DesktopEntry entry;
		private Font font;
		
		public AppWidget(DesktopEntry entry) {
			super(0, 0, 0, 0, Component.literal(getTitle(entry)));
			this.entry = entry;
			this.font = Minecraft.getInstance().font;
		}
		
		private static String getTitle(DesktopEntry entry) {
			return entry.name != null ? entry.name : entry.appId;
		}
		
		@Override
		protected void renderWidget(GuiGraphics context, int mouseX, int mouseY, float partialTicks) {
			int x = getX() + 1;
			int y = getY() + 1;
			int width = getWidth() - 2;
			int height = getHeight() - 2;
			boolean selected = isHoveredOrFocused();
			
			RenderUtils.blit(context, SLOT_THINGY, x, y, width, height);
			if(selected) RenderUtils.blit(context, SLOT_THINGY_SELECTED, x - 1, y - 1, width + 2, height + 2);
			
			int iconSize = entry.icon != null ? height - 10 : 0;
			
			context.enableScissor(x + 4, y + 4, x + width - 4, y + height - 4);
			if(entry.icon != null) RenderUtils.blit(context, entry.icon, x + 5, y + 5, iconSize, iconSize);
			context.drawString(font, getTitle(entry), x + 5 + iconSize + 5, y + height / 2 - font.lineHeight / 2, Color.white.getRGB());
			context.disableScissor();
			
			if(selected) {
				context.renderOutline(x - 1, y - 1, width + 2, height + 2, Color.white.getRGB());
				context.fill(x + 4, y + 4, x + width - 4, y + height - 4, 1, FastColor.ARGB32.color(128, Color.black.getRGB()));
			}
		}
		
		@Override
		protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
			this.defaultButtonNarrationText(narrationElementOutput);
		}
		
	}
	
}
