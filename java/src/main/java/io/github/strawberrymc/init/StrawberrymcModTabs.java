/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package io.github.strawberrymc.init;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.Registries;

import io.github.strawberrymc.StrawberrymcMod;

@EventBusSubscriber
public class StrawberrymcModTabs {
	public static final DeferredRegister<CreativeModeTab> REGISTRY = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, StrawberrymcMod.MODID);
	public static final DeferredHolder<CreativeModeTab, CreativeModeTab> DEBUG = REGISTRY.register("debug",
			() -> CreativeModeTab.builder().title(Component.translatable("item_group.strawberrymc.debug")).icon(() -> new ItemStack(Blocks.COMMAND_BLOCK)).displayItems((parameters, tabData) -> {
				tabData.accept(StrawberrymcModBlocks.OASIS_SPAWNER.get().asItem());
				tabData.accept(StrawberrymcModBlocks.CHISELED_CAULDRON.get().asItem());
				tabData.accept(StrawberrymcModBlocks.TALL_FIRE.get().asItem());
				tabData.accept(StrawberrymcModBlocks.FIR_TREE_SPAWNER.get().asItem());
				tabData.accept(StrawberrymcModBlocks.FIR_SNAG_SPAWNER.get().asItem());
				tabData.accept(StrawberrymcModBlocks.DEAD_FIR_SPAWNER.get().asItem());
				tabData.accept(StrawberrymcModBlocks.MASTER_FIR_SPAWNER.get().asItem());
			}).withSearchBar().build());

	@SubscribeEvent
	public static void buildTabContentsVanilla(BuildCreativeModeTabContentsEvent tabData) {
		if (tabData.getTabKey() == CreativeModeTabs.INGREDIENTS) {
			tabData.accept(StrawberrymcModItems.CHORINE.get());
			tabData.accept(StrawberrymcModItems.RADONITE_INGOT.get());
			tabData.accept(StrawberrymcModItems.GLOWROOT_TUBER.get());
			tabData.accept(StrawberrymcModItems.CLOVER_CLUFF.get());
			tabData.accept(StrawberrymcModItems.RARE_CLOVER_CLUFF.get());
			tabData.accept(StrawberrymcModItems.PLANT_FIBER.get());
			tabData.accept(StrawberrymcModItems.FLINT_SHARD.get());
			tabData.accept(StrawberrymcModItems.COCONUT.get());
			tabData.accept(StrawberrymcModItems.SMITE_VIAL.get());
			tabData.accept(StrawberrymcModItems.GREEN_BERYL.get());
			tabData.accept(StrawberrymcModItems.COCONUT_OIL.get());
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
			tabData.accept(StrawberrymcModBlocks.GARNET_BLOCK.get().asItem());
			tabData.accept(StrawberrymcModBlocks.CRYSTALLINE_CALCITE.get().asItem());
			tabData.accept(StrawberrymcModBlocks.VOIDSTONE.get().asItem());
			tabData.accept(StrawberrymcModBlocks.JUNIPER_LOG.get().asItem());
			tabData.accept(StrawberrymcModBlocks.JUNIPER_WOOD.get().asItem());
			tabData.accept(StrawberrymcModBlocks.STRIPPED_JUNIPER_LOG.get().asItem());
			tabData.accept(StrawberrymcModBlocks.STRIPPED_JUNIPER_WOOD.get().asItem());
			tabData.accept(StrawberrymcModBlocks.JUNIPER_PLANKS.get().asItem());
			tabData.accept(StrawberrymcModBlocks.JUNIPER_STAIRS.get().asItem());
			tabData.accept(StrawberrymcModBlocks.JUNIPER_SLAB.get().asItem());
			tabData.accept(StrawberrymcModBlocks.JUNIPER_FENCE.get().asItem());
			tabData.accept(StrawberrymcModBlocks.JUNIPER_FENCE_GATE.get().asItem());
			tabData.accept(StrawberrymcModBlocks.JUNIPER_DOOR.get().asItem());
			tabData.accept(StrawberrymcModBlocks.JUNIPER_TRAPDOOR.get().asItem());
			tabData.accept(StrawberrymcModBlocks.JUNIPER_PRESSURE_PLATE.get().asItem());
			tabData.accept(StrawberrymcModBlocks.JUNIPER_BUTTON.get().asItem());
		} else if (tabData.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS) {
			tabData.accept(StrawberrymcModBlocks.ECHOWOOD_LEAVES.get().asItem());
			tabData.accept(StrawberrymcModBlocks.PALM_LEAVES.get().asItem());
			tabData.accept(StrawberrymcModBlocks.DENDRITE_CRYSTAL.get().asItem());
			tabData.accept(StrawberrymcModBlocks.GARNET_CRYSTAL.get().asItem());
			tabData.accept(StrawberrymcModBlocks.GARNET_BLOCK.get().asItem());
			tabData.accept(StrawberrymcModItems.GARNET_NODULE.get());
			tabData.accept(StrawberrymcModItems.GARNET.get());
			tabData.accept(StrawberrymcModBlocks.CRYSTALLINE_CALCITE.get().asItem());
			tabData.accept(StrawberrymcModItems.GLOWROOT_TUBER.get());
			tabData.accept(StrawberrymcModBlocks.GLOWROOT.get().asItem());
			tabData.accept(StrawberrymcModItems.CLOVER_CLUFF.get());
			tabData.accept(StrawberrymcModItems.RARE_CLOVER_CLUFF.get());
			tabData.accept(StrawberrymcModBlocks.FERTILE_FARMLAND.get().asItem());
			tabData.accept(StrawberrymcModItems.PLANT_FIBER.get());
			tabData.accept(StrawberrymcModBlocks.POOR_FARMLAND.get().asItem());
			tabData.accept(StrawberrymcModBlocks.VOIDSTONE.get().asItem());
			tabData.accept(StrawberrymcModBlocks.PALM_SAPLING.get().asItem());
			tabData.accept(StrawberrymcModItems.COCONUT.get());
			tabData.accept(StrawberrymcModBlocks.ECHOWOOD_SAPLING.get().asItem());
			tabData.accept(StrawberrymcModBlocks.JUNIPER_LEAVES.get().asItem());
		} else if (tabData.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
			tabData.accept(StrawberrymcModBlocks.ECHOWOOD_SIGN.get().asItem());
			tabData.accept(StrawberrymcModBlocks.ECHOWOOD_HANGING_SIGN.get().asItem());
			tabData.accept(StrawberrymcModBlocks.PALM_SIGN.get().asItem());
			tabData.accept(StrawberrymcModBlocks.PALM_HANGING_SIGN.get().asItem());
			tabData.accept(StrawberrymcModBlocks.JUNIPER_SIGN.get().asItem());
			tabData.accept(StrawberrymcModBlocks.JUNIPER_HANGING_SIGN.get().asItem());
			tabData.accept(StrawberrymcModBlocks.CHISELED_CAULDRON.get().asItem());
		} else if (tabData.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
			tabData.accept(StrawberrymcModItems.ECHOWOOD_BOAT.get());
			tabData.accept(StrawberrymcModItems.ECHOWOOD_CHEST_BOAT.get());
			tabData.accept(StrawberrymcModItems.RADONITE_PICKAXE.get());
			tabData.accept(StrawberrymcModItems.RADONITE_AXE.get());
			tabData.accept(StrawberrymcModItems.RADONITE_SHOVEL.get());
			tabData.accept(StrawberrymcModItems.RADONITE_HOE.get());
			tabData.accept(StrawberrymcModItems.PALM_BOAT.get());
			tabData.accept(StrawberrymcModItems.PALM_CHEST_BOAT.get());
			tabData.accept(StrawberrymcModItems.JUNIPER_BOAT.get());
			tabData.accept(StrawberrymcModItems.JUNIPER_CHEST_BOAT.get());
		} else if (tabData.getTabKey() == CreativeModeTabs.COMBAT) {
			tabData.accept(StrawberrymcModItems.RADONITE_SWORD.get());
			tabData.accept(StrawberrymcModItems.RADONITE_ARMOR_HELMET.get());
			tabData.accept(StrawberrymcModItems.RADONITE_ARMOR_CHESTPLATE.get());
			tabData.accept(StrawberrymcModItems.RADONITE_ARMOR_LEGGINGS.get());
			tabData.accept(StrawberrymcModItems.RADONITE_ARMOR_BOOTS.get());
			tabData.accept(StrawberrymcModItems.RECOVERY_PEARL.get());
			tabData.accept(StrawberrymcModItems.SMITE_VIAL.get());
			tabData.accept(StrawberrymcModItems.IRON_BOW.get());
		} else if (tabData.getTabKey() == CreativeModeTabs.COLORED_BLOCKS) {
			tabData.accept(StrawberrymcModBlocks.GARNET_BLOCK.get().asItem());
		} else if (tabData.getTabKey() == CreativeModeTabs.OP_BLOCKS) {
			if (tabData.hasPermissions()) {
				tabData.accept(StrawberrymcModBlocks.GLOWROOT_DUMMY_SPAWNER.get().asItem());
				tabData.accept(StrawberrymcModBlocks.CHISELED_CAULDRON.get().asItem());
				tabData.accept(StrawberrymcModBlocks.FIR_TREE_SPAWNER.get().asItem());
				tabData.accept(StrawberrymcModBlocks.FIR_SNAG_SPAWNER.get().asItem());
				tabData.accept(StrawberrymcModBlocks.DEAD_FIR_SPAWNER.get().asItem());
				tabData.accept(StrawberrymcModBlocks.MASTER_FIR_SPAWNER.get().asItem());
			}
		} else if (tabData.getTabKey() == CreativeModeTabs.FOOD_AND_DRINKS) {
			tabData.accept(StrawberrymcModItems.COCONUT.get());
		}
	}
}