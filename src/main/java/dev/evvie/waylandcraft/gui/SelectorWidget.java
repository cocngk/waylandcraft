package dev.evvie.waylandcraft.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

public class SelectorWidget extends AbstractWidget {
	
	private int count = 1;
	private int selected = 0;
	
	private SelectorButton[] buttons;
	
	public SelectorWidget(int x, int y, int buttonWidth, int buttonHeight, int maxCount) {
		super(x, y, buttonWidth * maxCount, buttonHeight, Component.empty());
		
		buttons = new SelectorButton[maxCount];
		for(int i = 0; i < buttons.length; i++) {
			buttons[i] = new SelectorButton(this, x + buttonWidth * i, y, buttonWidth, buttonHeight, i);
		}
	}
	
	public void setEntries(String[] entries) {
		setCount(entries.length);
		if(entries.length == 0) buttons[0].setMessage(Component.empty());
		
		for(int i = 0; i < entries.length; i++) {
			buttons[i].setMessage(Component.literal(entries[i]));
		}
	}
	
	public void setCount(int num) {
		this.count = Math.max(num, 1);
		selected = Math.min(selected, count - 1);
	}
	
	public int selection() {
		return selected;
	}
	
	public void select(int idx) {
		this.selected = Math.clamp(idx, 0, count - 1);
	}
	
	@Override
	protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		for(int i = 0; i < buttons.length; i++) {
			SelectorButton b = buttons[i];
			b.selected = i == selected;
			b.visible = i < count;
			
			b.render(guiGraphics, mouseX, mouseY, partialTicks);
		}
	}
	
	@Override
	public boolean mouseClicked(double x, double y, int mouseButton) {
		if(!(this.active && this.visible)) return false;
		
		for(SelectorButton b : buttons) {
			if(b.mouseClicked(x, y, mouseButton)) return true;
		}
		
		return false;
	}
	
	@Override
	protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
	}
	
	private static class SelectorButton extends Button {
		
		public boolean selected = false;
		
		public SelectorButton(SelectorWidget widget, int x, int y, int width, int height, int idx) {
			super(x, y, width, height, Component.empty(), (b) -> {widget.select(idx);}, (c) -> c.get());
		}
		
		@Override
		public boolean isHoveredOrFocused() {
			// Lazy hack for rendering
			return selected;
		}
		
	}
	
}
