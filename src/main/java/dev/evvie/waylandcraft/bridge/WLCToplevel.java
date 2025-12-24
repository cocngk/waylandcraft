package dev.evvie.waylandcraft.bridge;

import org.jetbrains.annotations.Nullable;

public class WLCToplevel extends WLCAbstractWindow {
	
	@Nullable
	public String title;
	
	public WLCToplevel(long handle) {
		super(handle);
	}
	
}
