package dev.evvie.waylandcraft.item;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.Codec;

import dev.evvie.waylandcraft.WaylandCraft;
import dev.evvie.waylandcraft.bridge.WLCToplevel;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public class WindowItem extends Item {
	
	public static Item WINDOW;
	public static DataComponentType<Long> WINDOW_HANDLE;
	public static Component BROKEN_WINDOW_TEXT = Component.literal("Broken Window");
	public static Component UNKNOWN_WINDOW_TEXT = Component.literal("Unknown Window");
	public static final ResourceLocation BROKEN_WINDOW_MODEL = new ResourceLocation(WaylandCraft.MOD_ID, "item/broken_window");
	
	public static void register() {
		WINDOW = Registry.register(BuiltInRegistries.ITEM, new ResourceLocation(WaylandCraft.MOD_ID, "window"), new WindowItem());
		WINDOW_HANDLE = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, new ResourceLocation(WaylandCraft.MOD_ID, "window_handle"), DataComponentType.<Long>builder().persistent(Codec.LONG).build());
		
		ModelLoadingPlugin.register(new ModelLoadingPlugin() {
			
			@Override
			public void onInitializeModelLoader(Context pluginContext) {
				pluginContext.addModels(BROKEN_WINDOW_MODEL);
			}
			
		});
	}
	
	public WindowItem() {
		super(new Properties());
	}
	
	@Nullable
	public static WLCToplevel getToplevel(ItemStack item) {
		Long data = item.get(WINDOW_HANDLE);
		if(data == null) return null;
		
		long handle = data.longValue();
		return WaylandCraft.instance.bridge.getToplevel(handle);
	}
	
	@Override
	public Component getName(ItemStack itemStack) {
		WLCToplevel toplevel = getToplevel(itemStack);
		if(toplevel == null) return BROKEN_WINDOW_TEXT;
		
		String name = WaylandCraft.instance.xdgManager.getName(toplevel.appID);
		if(name == null) return UNKNOWN_WINDOW_TEXT;
		
		return Component.literal(name);
	}
	
	@Override
	public void appendHoverText(ItemStack itemStack, TooltipContext tooltipContext, List<Component> list, TooltipFlag tooltipFlag) {
		Long handle = itemStack.get(WINDOW_HANDLE);
		
		if(handle != null) list.add(Component.literal("Handle 0x" + Long.toHexString(handle.longValue())).withStyle(ChatFormatting.GRAY));
	}
	
	public static ItemStack createItem(WLCToplevel toplevel) {
		ItemStack stack = new ItemStack(WindowItem.WINDOW, 1);
		stack.set(WINDOW_HANDLE, toplevel.getHandle());
		return stack;
	}
	
}
