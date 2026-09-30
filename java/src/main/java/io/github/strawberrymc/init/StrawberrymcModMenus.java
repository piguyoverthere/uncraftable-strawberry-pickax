package io.github.strawberrymc.init;

import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.core.registries.Registries;

import java.util.Map;

import io.github.strawberrymc.world.inventory.ForgingTableMenu;
import io.github.strawberrymc.StrawberrymcMod;

public class StrawberrymcModMenus {
	public static final DeferredRegister<MenuType<?>> REGISTRY = DeferredRegister.create(Registries.MENU, StrawberrymcMod.MODID);
	public static final DeferredHolder<MenuType<?>, MenuType<ForgingTableMenu>> FORGING_TABLE = REGISTRY.register("forging_table", () -> IMenuTypeExtension.create(ForgingTableMenu::new));

	public interface MenuAccessor {
		Map<Integer, Slot> getSlots();
		Map<String, Object> getMenuState();
	}
}