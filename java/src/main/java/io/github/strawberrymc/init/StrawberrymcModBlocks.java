/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package io.github.strawberrymc.init;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.event.BlockEntityTypeAddBlocksEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.client.renderer.Sheets;

import java.util.function.Function;

import io.github.strawberrymc.block.*;
import io.github.strawberrymc.StrawberrymcMod;

@EventBusSubscriber
public class StrawberrymcModBlocks {
	public static final DeferredRegister.Blocks REGISTRY = DeferredRegister.createBlocks(StrawberrymcMod.MODID);
	public static final DeferredBlock<Block> CHORINE_ORE;
	public static final DeferredBlock<Block> CHORINE_BLOCK;
	public static final DeferredBlock<Block> ECHOWOOD_LOG;
	public static final DeferredBlock<Block> ECHOWOOD_WOOD;
	public static final DeferredBlock<Block> STRIPPED_ECHOWOOD_LOG;
	public static final DeferredBlock<Block> STRIPPED_ECHOWOOD_WOOD;
	public static final DeferredBlock<Block> ECHOWOOD_PLANKS;
	public static final DeferredBlock<Block> ECHOWOOD_LEAVES;
	public static final DeferredBlock<Block> ECHOWOOD_STAIRS;
	public static final DeferredBlock<Block> ECHOWOOD_SLAB;
	public static final DeferredBlock<Block> ECHOWOOD_FENCE;
	public static final DeferredBlock<Block> ECHOWOOD_FENCE_GATE;
	public static final DeferredBlock<Block> ECHOWOOD_DOOR;
	public static final DeferredBlock<Block> ECHOWOOD_TRAPDOOR;
	public static final DeferredBlock<Block> ECHOWOOD_PRESSURE_PLATE;
	public static final DeferredBlock<Block> ECHOWOOD_BUTTON;
	public static final DeferredBlock<Block> ECHOWOOD_SIGN;
	public static final DeferredBlock<Block> ECHOWOOD_WALL_SIGN;
	public static final DeferredBlock<Block> ECHOWOOD_HANGING_SIGN;
	public static final DeferredBlock<Block> ECHOWOOD_WALL_HANGING_SIGN;
	public static final DeferredBlock<Block> RADONITE_ORE;
	public static final DeferredBlock<Block> RADONITE_BLOCK;
	public static final DeferredBlock<Block> PALM_LOG;
	public static final DeferredBlock<Block> PALM_WOOD;
	public static final DeferredBlock<Block> STRIPPED_PALM_LOG;
	public static final DeferredBlock<Block> STRIPPED_PALM_WOOD;
	public static final DeferredBlock<Block> PALM_PLANKS;
	public static final DeferredBlock<Block> PALM_LEAVES;
	public static final DeferredBlock<Block> PALM_STAIRS;
	public static final DeferredBlock<Block> PALM_SLAB;
	public static final DeferredBlock<Block> PALM_FENCE;
	public static final DeferredBlock<Block> PALM_FENCE_GATE;
	public static final DeferredBlock<Block> PALM_DOOR;
	public static final DeferredBlock<Block> PALM_TRAPDOOR;
	public static final DeferredBlock<Block> PALM_PRESSURE_PLATE;
	public static final DeferredBlock<Block> PALM_BUTTON;
	public static final DeferredBlock<Block> PALM_SIGN;
	public static final DeferredBlock<Block> PALM_WALL_SIGN;
	public static final DeferredBlock<Block> PALM_HANGING_SIGN;
	public static final DeferredBlock<Block> PALM_WALL_HANGING_SIGN;
	public static final DeferredBlock<Block> DENDRITE_CRYSTAL;
	public static final DeferredBlock<Block> DENDRITE_BLOCK;
	public static final DeferredBlock<Block> GARNET_CRYSTAL;
	public static final DeferredBlock<Block> GARNET_BLOCK;
	public static final DeferredBlock<Block> CRYSTALLINE_CALCITE;
	public static final DeferredBlock<Block> GLOWROOT_TUBER_STONE;
	public static final DeferredBlock<Block> GLOWROOT;
	public static final DeferredBlock<Block> GLOWROOT_DUMMY_SPAWNER;
	public static final DeferredBlock<Block> CLOVER_MAT;
	public static final DeferredBlock<Block> PALM_SEEDLING;
	public static final DeferredBlock<Block> FERTILE_FARMLAND;
	static {
		CHORINE_ORE = register("chorine_ore", ChorineOreBlock::new);
		CHORINE_BLOCK = register("chorine_block", ChorineBlockBlock::new);
		ECHOWOOD_LOG = register("echowood_log", EchowoodLogBlock::new);
		ECHOWOOD_WOOD = register("echowood_wood", EchowoodWoodBlock::new);
		STRIPPED_ECHOWOOD_LOG = register("stripped_echowood_log", StrippedEchowoodLogBlock::new);
		STRIPPED_ECHOWOOD_WOOD = register("stripped_echowood_wood", StrippedEchowoodWoodBlock::new);
		ECHOWOOD_PLANKS = register("echowood_planks", EchowoodPlanksBlock::new);
		ECHOWOOD_LEAVES = register("echowood_leaves", EchowoodLeavesBlock::new);
		ECHOWOOD_STAIRS = register("echowood_stairs", EchowoodStairsBlock::new);
		ECHOWOOD_SLAB = register("echowood_slab", EchowoodSlabBlock::new);
		ECHOWOOD_FENCE = register("echowood_fence", EchowoodFenceBlock::new);
		ECHOWOOD_FENCE_GATE = register("echowood_fence_gate", EchowoodFenceGateBlock::new);
		ECHOWOOD_DOOR = register("echowood_door", EchowoodDoorBlock::new);
		ECHOWOOD_TRAPDOOR = register("echowood_trapdoor", EchowoodTrapdoorBlock::new);
		ECHOWOOD_PRESSURE_PLATE = register("echowood_pressure_plate", EchowoodPressurePlateBlock::new);
		ECHOWOOD_BUTTON = register("echowood_button", EchowoodButtonBlock::new);
		ECHOWOOD_SIGN = register("echowood_sign", EchowoodSignBlock::new);
		ECHOWOOD_WALL_SIGN = register("echowood_wall_sign", EchowoodWallSignBlock::new);
		ECHOWOOD_HANGING_SIGN = register("echowood_hanging_sign", EchowoodHangingSignBlock::new);
		ECHOWOOD_WALL_HANGING_SIGN = register("echowood_wall_hanging_sign", EchowoodWallHangingSignBlock::new);
		RADONITE_ORE = register("radonite_ore", RadoniteOreBlock::new);
		RADONITE_BLOCK = register("radonite_block", RadoniteBlockBlock::new);
		PALM_LOG = register("palm_log", PalmLogBlock::new);
		PALM_WOOD = register("palm_wood", PalmWoodBlock::new);
		STRIPPED_PALM_LOG = register("stripped_palm_log", StrippedPalmLogBlock::new);
		STRIPPED_PALM_WOOD = register("stripped_palm_wood", StrippedPalmWoodBlock::new);
		PALM_PLANKS = register("palm_planks", PalmPlanksBlock::new);
		PALM_LEAVES = register("palm_leaves", PalmLeavesBlock::new);
		PALM_STAIRS = register("palm_stairs", PalmStairsBlock::new);
		PALM_SLAB = register("palm_slab", PalmSlabBlock::new);
		PALM_FENCE = register("palm_fence", PalmFenceBlock::new);
		PALM_FENCE_GATE = register("palm_fence_gate", PalmFenceGateBlock::new);
		PALM_DOOR = register("palm_door", PalmDoorBlock::new);
		PALM_TRAPDOOR = register("palm_trapdoor", PalmTrapdoorBlock::new);
		PALM_PRESSURE_PLATE = register("palm_pressure_plate", PalmPressurePlateBlock::new);
		PALM_BUTTON = register("palm_button", PalmButtonBlock::new);
		PALM_SIGN = register("palm_sign", PalmSignBlock::new);
		PALM_WALL_SIGN = register("palm_wall_sign", PalmWallSignBlock::new);
		PALM_HANGING_SIGN = register("palm_hanging_sign", PalmHangingSignBlock::new);
		PALM_WALL_HANGING_SIGN = register("palm_wall_hanging_sign", PalmWallHangingSignBlock::new);
		DENDRITE_CRYSTAL = register("dendrite_crystal", DendriteCrystalBlock::new);
		DENDRITE_BLOCK = register("dendrite_block", DendriteBlockBlock::new);
		GARNET_CRYSTAL = register("garnet_crystal", GarnetCrystalBlock::new);
		GARNET_BLOCK = register("garnet_block", GarnetBlockBlock::new);
		CRYSTALLINE_CALCITE = register("crystalline_calcite", CrystallineCalciteBlock::new);
		GLOWROOT_TUBER_STONE = register("glowroot_tuber_stone", GlowrootTuberStoneBlock::new);
		GLOWROOT = register("glowroot", GlowrootBlock::new);
		GLOWROOT_DUMMY_SPAWNER = register("glowroot_dummy_spawner", GlowrootDummySpawnerBlock::new);
		CLOVER_MAT = register("clover_mat", CloverMatBlock::new);
		PALM_SEEDLING = register("palm_seedling", PalmSeedlingBlock::new);
		FERTILE_FARMLAND = register("fertile_farmland", FertileFarmlandBlock::new);
	}

	// Start of user code block custom blocks
	// End of user code block custom blocks
	private static <B extends Block> DeferredBlock<B> register(String name, Function<BlockBehaviour.Properties, ? extends B> supplier) {
		return REGISTRY.registerBlock(name, supplier);
	}

	@EventBusSubscriber(Dist.CLIENT)
	public static class BlocksClientSideHandler {
		@SubscribeEvent
		public static void clientSetup(FMLClientSetupEvent event) {
			Sheets.addWoodType(StrawberrymcModWoodTypes.ECHOWOOD_SIGN_WOOD_TYPE);
			Sheets.addWoodType(StrawberrymcModWoodTypes.ECHOWOOD_HANGING_SIGN_WOOD_TYPE);
			Sheets.addWoodType(StrawberrymcModWoodTypes.PALM_SIGN_WOOD_TYPE);
			Sheets.addWoodType(StrawberrymcModWoodTypes.PALM_HANGING_SIGN_WOOD_TYPE);
		}
	}

	@SubscribeEvent
	public static void registerSigns(BlockEntityTypeAddBlocksEvent event) {
		event.modify(BlockEntityType.SIGN, ECHOWOOD_SIGN.get(), ECHOWOOD_WALL_SIGN.get());
		event.modify(BlockEntityType.HANGING_SIGN, ECHOWOOD_HANGING_SIGN.get(), ECHOWOOD_WALL_HANGING_SIGN.get());
		event.modify(BlockEntityType.SIGN, PALM_SIGN.get(), PALM_WALL_SIGN.get());
		event.modify(BlockEntityType.HANGING_SIGN, PALM_HANGING_SIGN.get(), PALM_WALL_HANGING_SIGN.get());
	}
}