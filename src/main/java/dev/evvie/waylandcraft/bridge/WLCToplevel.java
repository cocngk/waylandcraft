package dev.evvie.waylandcraft.bridge;

import org.jetbrains.annotations.Nullable;

public class WLCToplevel {
	
	// Set to zero when this toplevel no longer exists
	private long handle;
	
	@Nullable
	private WLCSurface surface;
	
	public WLCToplevel(long handle) {
		this.handle = handle;
	}
	
	protected long getHandle() {
		return this.handle;
	}
	
	protected long takeHandle() {
		long old = this.handle;
		this.handle = 0;
		return old;
	}
	
	public boolean isAlive() {
		return handle != 0;
	}
	
	protected void setSurface(WLCSurface surface) {
		this.surface = surface;
	}
	
	public WLCSurface getSurfaceTree() {
		return this.surface;
	}
	
}
