/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package io.github.strawberrymc.init;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.core.registries.Registries;

import io.github.strawberrymc.StrawberrymcMod;

@EventBusSubscriber
public class StrawberrymcModTabs {
	public static final DeferredRegister<CreativeModeTab> REGISTRY = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, StrawberrymcMod.MODID);

	@SubscribeEvent
	public static void buildTabContentsVanilla(BuildCreativeModeTabContentsEvent tabData) {
		if (tabData.getTabKey() == CreativeModeTabs.INGREDIENTS) {
			tabData.accept(StrawberrymcModItems.CHORINE.get());
			tabData.accept(StrawberrymcModItems.RADONITE_INGOT.get());
			tabData.accept(StrawberrymcModItems.GLOWROOT_TUBER.get());
			tabData.accept(StrawberrymcModItems.CLOVER_CLUFF.get());
			tabData.accept(StrawberrymcModItems.RARE_CLOVER_CLUFF.get());
		} else if (tabData.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
			tabData.accept(StrawberrymcModBlocks.CHORINE_ORE.get().asItem());
			tabData.accept(StrawberrymcModBlocks.CHORINE_BLOCK.get().asItem());
			tabData.accept(StrawberrymcModBlocks.ECHOWOOD_LOG.get().asItem());
			tabData.accept(StrawberrymcModBlocks.ECHOWOOD_WOOD.get().asItem());
			tabData.accept(StrawberrymcModBlocks.STRIPPED_ECHOWOOD_LOG.get().asItem());
			tabData.accept(StrawberrymcModBlocks.STRIPPED_ECHOWOOD_WOOD.get().asItem());
			tabData.accept(StrawberrymcModBlocks.ECHOWOOD_PLANKS.get().asItem());
			tabData.accept(StrawberrymcModBlocks.ECHOWOOD_STAIRS.get().asItem());
			tabData.accept(StrawberrymcModBlocks.ECHOWOOD_SLAB.get().asItem());
			tabData.accept(StrawberrymcModBlocks.ECHOWOOD_FENCE.get().asItem());
			tabData.accept(StrawberrymcModBlocks.ECHOWOOD_FENCE_GATE.get().asItem());
			tabData.accept(StrawberrymcModBlocks.ECHOWOOD_DOOR.get().asItem());
			tabData.accept(StrawberrymcModBlocks.ECHOWOOD_TRAPDOOR.get().asItem());
			tabData.accept(StrawberrymcModBlocks.ECHOWOOD_PRESSURE_PLATE.get().asItem());
			tabData.accept(StrawberrymcModBlocks.ECHOWOOD_BUTTON.get().asItem());
			tabData.accept(StrawberrymcModBlocks.RADONITE_ORE.get().asItem());
			tabData.accept(StrawberrymcModBlocks.RADONITE_BLOCK.get().asItem());
			tabData.accept(StrawberrymcModBlocks.PALM_LOG.get().asItem());
			tabData.accept(StrawberrymcModBlocks.PALM_WOOD.get().asItem());
			tabData.accept(StrawberrymcModBlocks.STRIPPED_PALM_LOG.get().asItem());
			tabData.accept(StrawberrymcModBlocks.STRIPPED_PALM_WOOD.get().asItem());
			tabData.accept(StrawberrymcModBlocks.PALM_PLANKS.get().asItem());
			tabData.accept(StrawberrymcModBlocks.PALM_STAIRS.get().asItem());
			tabData.accept(StrawberrymcModBlocks.PALM_SLAB.get().asItem());
			tabData.accept(StrawberrymcModBlocks.PALM_FENCE.get().asItem());
			tabData.accept(StrawberrymcModBlocks.PALM_FENCE_GATE.get().asItem());
			tabData.accept(StrawberrymcModBlocks.PALM_DOOR.get().asItem());
			tabData.accept(StrawberrymcModBlocks.PALM_TRAPDOOR.get().asItem());
			tabData.accept(StrawberrymcModBlocks.PALM_PRESSURE_PLATE.get().asItem());
			tabData.accept(StrawberrymcModBlocks.PALM_BUTTON.get().asItem());
		} else if (tabData.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS) {
			tabData.accept(StrawberrymcModBlocks.ECHOWOOD_LEAVES.get().asItem());
			tabData.accept(StrawberrymcModBlocks.PALM_LEAVES.get().asItem());
			tabData.accept(StrawberrymcModItems.GLOWROOT_TUBER.get());
			tabData.accept(StrawberrymcModBlocks.GLOWROOT.get().asItem());
			tabData.accept(StrawberrymcModItems.CLOVER_CLUFF.get());
			tabData.accept(StrawberrymcModItems.RARE_CLOVER_CLUFF.get());
			tabData.accept(StrawberrymcModBlocks.PALM_SEEDLING.get().asItem());
		} else if (tabData.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
			tabData.accept(StrawberrymcModBlocks.ECHOWOOD_SIGN.get().asItem());
			tabData.accept(StrawberrymcModBlocks.ECHOWOOD_HANGING_SIGN.get().asItem());
			tabData.accept(StrawberrymcModBlocks.PALM_SIGN.get().asItem());
			tabData.accept(StrawberrymcModBlocks.PALM_HANGING_SIGN.get().asItem());
		} else if (tabData.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
			tabData.accept(StrawberrymcModItems.ECHOWOOD_BOAT.get());
			tabData.accept(StrawberrymcModItems.ECHOWOOD_CHEST_BOAT.get());
			tabData.accept(StrawberrymcModItems.RADONITE_PICKAXE.get());
			tabData.accept(StrawberrymcModItems.RADONITE_AXE.get());
			tabData.accept(StrawberrymcModItems.RADONITE_SHOVEL.get());
			tabData.accept(StrawberrymcModItems.RADONITE_HOE.get());
			tabData.accept(StrawberrymcModItems.PALM_BOAT.get());
			tabData.accept(StrawberrymcModItems.PALM_CHEST_BOAT.get());
			tabData.accept(StrawberrymcModItems.IRON_HAMMER.get());
		} else if (tabData.getTabKey() == CreativeModeTabs.COMBAT) {
			tabData.accept(StrawberrymcModItems.RADONITE_SWORD.get());
			tabData.accept(StrawberrymcModItems.RADONITE_ARMOR_HELMET.get());
			tabData.accept(StrawberrymcModItems.RADONITE_ARMOR_CHESTPLATE.get());
			tabData.accept(StrawberrymcModItems.RADONITE_ARMOR_LEGGINGS.get());
			tabData.accept(StrawberrymcModItems.RADONITE_ARMOR_BOOTS.get());
			tabData.accept(StrawberrymcModItems.RECOVERY_PEARL.get());
		} else if (tabData.getTabKey() == CreativeModeTabs.OP_BLOCKS) {
			if (tabData.hasPermissions()) {
				tabData.accept(StrawberrymcModBlocks.GLOWROOT_DUMMY_SPAWNER.get().asItem());
			}
		}
	}
}