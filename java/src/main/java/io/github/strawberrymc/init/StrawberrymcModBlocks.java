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
	public static final DeferredBlock<Block> CINNABAR;
	public static final DeferredBlock<Block> CITRINE_BLOCK;
	public static final DeferredBlock<Block> CITRINE_CRYSTAL;
	public static final DeferredBlock<Block> AQUAMARINE_BLOCK;
	public static final DeferredBlock<Block> AQUAMARINE_CRYSTAL;
	public static final DeferredBlock<Block> ROSE_QUARTZ_BLOCK;
	public static final DeferredBlock<Block> ROSE_QUARTZ_CRYSTAL;
	public static final DeferredBlock<Block> GLOWROOT_TUBER_STONE;
	public static final DeferredBlock<Block> GLOWROOT;
	public static final DeferredBlock<Block> GLOWROOT_DUMMY_SPAWNER;
	public static final DeferredBlock<Block> CLOVER_MAT;
	public static final DeferredBlock<Block> FERTILE_FARMLAND;
	public static final DeferredBlock<Block> POOR_FARMLAND;
	public static final DeferredBlock<Block> FARMLAND_STRAWBERRY;
	public static final DeferredBlock<Block> VOIDSTONE;
	public static final DeferredBlock<Block> VOID_TEARS;
	public static final DeferredBlock<Block> OASIS_SPAWNER;
	public static final DeferredBlock<Block> PALM_SAPLING;
	public static final DeferredBlock<Block> ECHOWOOD_SAPLING;
	public static final DeferredBlock<Block> JUNIPER_LOG;
	public static final DeferredBlock<Block> JUNIPER_WOOD;
	public static final DeferredBlock<Block> STRIPPED_JUNIPER_LOG;
	public static final DeferredBlock<Block> STRIPPED_JUNIPER_WOOD;
	public static final DeferredBlock<Block> JUNIPER_PLANKS;
	public static final DeferredBlock<Block> JUNIPER_LEAVES;
	public static final DeferredBlock<Block> JUNIPER_STAIRS;
	public static final DeferredBlock<Block> JUNIPER_SLAB;
	public static final DeferredBlock<Block> JUNIPER_FENCE;
	public static final DeferredBlock<Block> JUNIPER_FENCE_GATE;
	public static final DeferredBlock<Block> JUNIPER_DOOR;
	public static final DeferredBlock<Block> JUNIPER_TRAPDOOR;
	public static final DeferredBlock<Block> JUNIPER_PRESSURE_PLATE;
	public static final DeferredBlock<Block> JUNIPER_BUTTON;
	public static final DeferredBlock<Block> JUNIPER_SIGN;
	public static final DeferredBlock<Block> JUNIPER_WALL_SIGN;
	public static final DeferredBlock<Block> JUNIPER_HANGING_SIGN;
	public static final DeferredBlock<Block> JUNIPER_WALL_HANGING_SIGN;
	public static final DeferredBlock<Block> CHISELED_CAULDRON;
	public static final DeferredBlock<Block> TALL_FIRE;
	public static final DeferredBlock<Block> FIR_TREE_SPAWNER;
	public static final DeferredBlock<Block> FIR_SNAG_SPAWNER;
	public static final DeferredBlock<Block> DEAD_FIR_SPAWNER;
	public static final DeferredBlock<Block> MASTER_FIR_SPAWNER;
	public static final DeferredBlock<Block> PRISMATIC_SENSOR;
	public static final DeferredBlock<Block> DEACTIVATED_PRISMATIC_SENSOR;
	public static final DeferredBlock<Block> ANCIENT_PRISMARINE;
	public static final DeferredBlock<Block> CRACKED_PRISMARINE;
	public static final DeferredBlock<Block> PRISMATIC_VAULT;
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
		CINNABAR = register("cinnabar", CinnabarBlock::new);
		CITRINE_BLOCK = register("citrine_block", CitrineBlockBlock::new);
		CITRINE_CRYSTAL = register("citrine_crystal", CitrineCrystalBlock::new);
		AQUAMARINE_BLOCK = register("aquamarine_block", AquamarineBlockBlock::new);
		AQUAMARINE_CRYSTAL = register("aquamarine_crystal", AquamarineCrystalBlock::new);
		ROSE_QUARTZ_BLOCK = register("rose_quartz_block", RoseQuartzBlockBlock::new);
		ROSE_QUARTZ_CRYSTAL = register("rose_quartz_crystal", RoseQuartzCrystalBlock::new);
		GLOWROOT_TUBER_STONE = register("glowroot_tuber_stone", GlowrootTuberStoneBlock::new);
		GLOWROOT = register("glowroot", GlowrootBlock::new);
		GLOWROOT_DUMMY_SPAWNER = register("glowroot_dummy_spawner", GlowrootDummySpawnerBlock::new);
		CLOVER_MAT = register("clover_mat", CloverMatBlock::new);
		FERTILE_FARMLAND = register("fertile_farmland", FertileFarmlandBlock::new);
		POOR_FARMLAND = register("poor_farmland", PoorFarmlandBlock::new);
		FARMLAND_STRAWBERRY = register("farmland_strawberry", FarmlandStrawberryBlock::new);
		VOIDSTONE = register("voidstone", VoidstoneBlock::new);
		VOID_TEARS = register("void_tears", VoidTearsBlock::new);
		OASIS_SPAWNER = register("oasis_spawner", OasisSpawnerBlock::new);
		PALM_SAPLING = register("palm_sapling", PalmSaplingBlock::new);
		ECHOWOOD_SAPLING = register("echowood_sapling", EchowoodSaplingBlock::new);
		JUNIPER_LOG = register("juniper_log", JuniperLogBlock::new);
		JUNIPER_WOOD = register("juniper_wood", JuniperWoodBlock::new);
		STRIPPED_JUNIPER_LOG = register("stripped_juniper_log", StrippedJuniperLogBlock::new);
		STRIPPED_JUNIPER_WOOD = register("stripped_juniper_wood", StrippedJuniperWoodBlock::new);
		JUNIPER_PLANKS = register("juniper_planks", JuniperPlanksBlock::new);
		JUNIPER_LEAVES = register("juniper_leaves", JuniperLeavesBlock::new);
		JUNIPER_STAIRS = register("juniper_stairs", JuniperStairsBlock::new);
		JUNIPER_SLAB = register("juniper_slab", JuniperSlabBlock::new);
		JUNIPER_FENCE = register("juniper_fence", JuniperFenceBlock::new);
		JUNIPER_FENCE_GATE = register("juniper_fence_gate", JuniperFenceGateBlock::new);
		JUNIPER_DOOR = register("juniper_door", JuniperDoorBlock::new);
		JUNIPER_TRAPDOOR = register("juniper_trapdoor", JuniperTrapdoorBlock::new);
		JUNIPER_PRESSURE_PLATE = register("juniper_pressure_plate", JuniperPressurePlateBlock::new);
		JUNIPER_BUTTON = register("juniper_button", JuniperButtonBlock::new);
		JUNIPER_SIGN = register("juniper_sign", JuniperSignBlock::new);
		JUNIPER_WALL_SIGN = register("juniper_wall_sign", JuniperWallSignBlock::new);
		JUNIPER_HANGING_SIGN = register("juniper_hanging_sign", JuniperHangingSignBlock::new);
		JUNIPER_WALL_HANGING_SIGN = register("juniper_wall_hanging_sign", JuniperWallHangingSignBlock::new);
		CHISELED_CAULDRON = register("chiseled_cauldron", ChiseledCauldronBlock::new);
		TALL_FIRE = register("tall_fire", TallFireBlock::new);
		FIR_TREE_SPAWNER = register("fir_tree_spawner", FirTreeSpawnerBlock::new);
		FIR_SNAG_SPAWNER = register("fir_snag_spawner", FirSnagSpawnerBlock::new);
		DEAD_FIR_SPAWNER = register("dead_fir_spawner", DeadFirSpawnerBlock::new);
		MASTER_FIR_SPAWNER = register("master_fir_spawner", MasterFirSpawnerBlock::new);
		PRISMATIC_SENSOR = register("prismatic_sensor", PrismaticSensorBlock::new);
		DEACTIVATED_PRISMATIC_SENSOR = register("deactivated_prismatic_sensor", DeactivatedPrismaticSensorBlock::new);
		ANCIENT_PRISMARINE = register("ancient_prismarine", AncientPrismarineBlock::new);
		CRACKED_PRISMARINE = register("cracked_prismarine", CrackedPrismarineBlock::new);
		PRISMATIC_VAULT = register("prismatic_vault", PrismaticVaultBlock::new);
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
			Sheets.addWoodType(StrawberrymcModWoodTypes.JUNIPER_SIGN_WOOD_TYPE);
			Sheets.addWoodType(StrawberrymcModWoodTypes.JUNIPER_HANGING_SIGN_WOOD_TYPE);
		}
	}

	@SubscribeEvent
	public static void registerSigns(BlockEntityTypeAddBlocksEvent event) {
		event.modify(BlockEntityType.SIGN, ECHOWOOD_SIGN.get(), ECHOWOOD_WALL_SIGN.get());
		event.modify(BlockEntityType.HANGING_SIGN, ECHOWOOD_HANGING_SIGN.get(), ECHOWOOD_WALL_HANGING_SIGN.get());
		event.modify(BlockEntityType.SIGN, PALM_SIGN.get(), PALM_WALL_SIGN.get());
		event.modify(BlockEntityType.HANGING_SIGN, PALM_HANGING_SIGN.get(), PALM_WALL_HANGING_SIGN.get());
		event.modify(BlockEntityType.SIGN, JUNIPER_SIGN.get(), JUNIPER_WALL_SIGN.get());
		event.modify(BlockEntityType.HANGING_SIGN, JUNIPER_HANGING_SIGN.get(), JUNIPER_WALL_HANGING_SIGN.get());
	}
}