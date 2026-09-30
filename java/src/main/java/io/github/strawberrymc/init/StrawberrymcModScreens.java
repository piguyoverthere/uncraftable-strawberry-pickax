package io.github.strawberrymc.init;

import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;

import io.github.strawberrymc.client.gui.ForgingTableScreen;
import io.github.strawberrymc.StrawberrymcMod;

@EventBusSubscriber(modid = StrawberrymcMod.MODID, value = Dist.CLIENT)
public final class StrawberrymcModScreens {
	private StrawberrymcModScreens() {
	}

	public interface ScreenAccessor {
		void updateMenuState(int elementType, String name, Object elementState);
	}

	@SubscribeEvent
	public static void registerScreens(RegisterMenuScreensEvent event) {
		event.register(StrawberrymcModMenus.FORGING_TABLE.get(), ForgingTableScreen::new);
	}
}
