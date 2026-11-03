/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package io.github.strawberrymc.init;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredHolder;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.*;

import java.util.function.Function;

import io.github.strawberrymc.item.*;
import io.github.strawberrymc.StrawberrymcMod;

public class StrawberrymcModItems {
	public static final DeferredRegister.Items REGISTRY = DeferredRegister.createItems(StrawberrymcMod.MODID);
	public static final DeferredItem<Item> CHORINE;
	public static final DeferredItem<Item> CHORINE_ORE;
	public static final DeferredItem<Item> CHORINE_BLOCK;
	public static final DeferredItem<Item> CHORITE;
	public static final DeferredItem<Item> ECHOWOOD_LOG;
	public static final DeferredItem<Item> ECHOWOOD_WOOD;
	public static final DeferredItem<Item> STRIPPED_ECHOWOOD_LOG;
	public static final DeferredItem<Item> STRIPPED_ECHOWOOD_WOOD;
	public static final DeferredItem<Item> ECHOWOOD_PLANKS;
	public static final DeferredItem<Item> ECHOWOOD_LEAVES;
	public static final DeferredItem<Item> ECHOWOOD_STAIRS;
	public static final DeferredItem<Item> ECHOWOOD_SLAB;
	public static final DeferredItem<Item> ECHOWOOD_FENCE;
	public static final DeferredItem<Item> ECHOWOOD_FENCE_GATE;
	public static final DeferredItem<Item> ECHOWOOD_DOOR;
	public static final DeferredItem<Item> ECHOWOOD_TRAPDOOR;
	public static final DeferredItem<Item> ECHOWOOD_PRESSURE_PLATE;
	public static final DeferredItem<Item> ECHOWOOD_BUTTON;
	public static final DeferredItem<Item> ECHOWOOD_SIGN;
	public static final DeferredItem<Item> ECHOWOOD_HANGING_SIGN;
	public static final DeferredItem<Item> ECHOWOOD_BOAT;
	public static final DeferredItem<Item> ECHOWOOD_CHEST_BOAT;
	public static final DeferredItem<Item> RADONITE_INGOT;
	public static final DeferredItem<Item> RADONITE_ORE;
	public static final DeferredItem<Item> RADONITE_BLOCK;
	public static final DeferredItem<Item> RADONITE_PICKAXE;
	public static final DeferredItem<Item> RADONITE_AXE;
	public static final DeferredItem<Item> RADONITE_SWORD;
	public static final DeferredItem<Item> RADONITE_SHOVEL;
	public static final DeferredItem<Item> RADONITE_HOE;
	public static final DeferredItem<Item> RADONITE_ARMOR_HELMET;
	public static final DeferredItem<Item> RADONITE_ARMOR_CHESTPLATE;
	public static final DeferredItem<Item> RADONITE_ARMOR_LEGGINGS;
	public static final DeferredItem<Item> RADONITE_ARMOR_BOOTS;
	public static final DeferredItem<Item> RAW_RADONITE;
	public static final DeferredItem<Item> PALM_LOG;
	public static final DeferredItem<Item> PALM_WOOD;
	public static final DeferredItem<Item> STRIPPED_PALM_LOG;
	public static final DeferredItem<Item> STRIPPED_PALM_WOOD;
	public static final DeferredItem<Item> PALM_PLANKS;
	public static final DeferredItem<Item> PALM_LEAVES;
	public static final DeferredItem<Item> PALM_STAIRS;
	public static final DeferredItem<Item> PALM_SLAB;
	public static final DeferredItem<Item> PALM_FENCE;
	public static final DeferredItem<Item> PALM_FENCE_GATE;
	public static final DeferredItem<Item> PALM_DOOR;
	public static final DeferredItem<Item> PALM_TRAPDOOR;
	public static final DeferredItem<Item> PALM_PRESSURE_PLATE;
	public static final DeferredItem<Item> PALM_BUTTON;
	public static final DeferredItem<Item> PALM_SIGN;
	public static final DeferredItem<Item> PALM_HANGING_SIGN;
	public static final DeferredItem<Item> PALM_BOAT;
	public static final DeferredItem<Item> PALM_CHEST_BOAT;
	public static final DeferredItem<Item> DENDRITE_CRYSTAL;
	public static final DeferredItem<Item> DENDRITE_BLOCK;
	public static final DeferredItem<Item> GARNET_CRYSTAL;
	public static final DeferredItem<Item> GARNET_BLOCK;
	public static final DeferredItem<Item> GARNET_NODULE;
	public static final DeferredItem<Item> GARNET;
	public static final DeferredItem<Item> CRYSTALLINE_CALCITE;
	public static final DeferredItem<Item> RECOVERY_PEARL;
	public static final DeferredItem<Item> GLOWROOT_TUBER;
	public static final DeferredItem<Item> GLOWROOT_TUBER_STONE;
	public static final DeferredItem<Item> GLOWROOT;
	public static final DeferredItem<Item> GLOWROOT_DUMMY_SPAWNER;
	public static final DeferredItem<Item> CLOVER_CLUFF;
	public static final DeferredItem<Item> CLOVER_MAT;
	public static final DeferredItem<Item> RARE_CLOVER_CLUFF;
	public static final DeferredItem<Item> FERTILE_FARMLAND;
	public static final DeferredItem<Item> SULFUR_SLAG;
	public static final DeferredItem<Item> PLANT_FIBER;
	public static final DeferredItem<Item> POOR_FARMLAND;
	public static final DeferredItem<Item> FLINT_SHARD;
	public static final DeferredItem<Item> FARMLAND_STRAWBERRY;
	public static final DeferredItem<Item> VOIDSTONE;
	public static final DeferredItem<Item> OASIS_SPAWNER;
	public static final DeferredItem<Item> PALM_SAPLING;
	public static final DeferredItem<Item> COCONUT;
	public static final DeferredItem<Item> ECHOWOOD_SAPLING;
	public static final DeferredItem<Item> JUNIPER_LOG;
	public static final DeferredItem<Item> JUNIPER_WOOD;
	public static final DeferredItem<Item> STRIPPED_JUNIPER_LOG;
	public static final DeferredItem<Item> STRIPPED_JUNIPER_WOOD;
	public static final DeferredItem<Item> JUNIPER_PLANKS;
	public static final DeferredItem<Item> JUNIPER_LEAVES;
	public static final DeferredItem<Item> JUNIPER_STAIRS;
	public static final DeferredItem<Item> JUNIPER_SLAB;
	public static final DeferredItem<Item> JUNIPER_FENCE;
	public static final DeferredItem<Item> JUNIPER_FENCE_GATE;
	public static final DeferredItem<Item> JUNIPER_DOOR;
	public static final DeferredItem<Item> JUNIPER_TRAPDOOR;
	public static final DeferredItem<Item> JUNIPER_PRESSURE_PLATE;
	public static final DeferredItem<Item> JUNIPER_BUTTON;
	public static final DeferredItem<Item> JUNIPER_SIGN;
	public static final DeferredItem<Item> JUNIPER_HANGING_SIGN;
	public static final DeferredItem<Item> JUNIPER_BOAT;
	public static final DeferredItem<Item> JUNIPER_CHEST_BOAT;
	public static final DeferredItem<Item> CHISELED_CAULDRON;
	public static final DeferredItem<Item> SMITE_VIAL;
	public static final DeferredItem<Item> TALL_FIRE;
	public static final DeferredItem<Item> GREEN_BERYL;
	public static final DeferredItem<Item> COCONUT_OIL;
	public static final DeferredItem<Item> IRON_BOW;
	public static final DeferredItem<Item> FIR_TREE_SPAWNER;
	public static final DeferredItem<Item> FIR_SNAG_SPAWNER;
	public static final DeferredItem<Item> DEAD_FIR_SPAWNER;
	public static final DeferredItem<Item> MASTER_FIR_SPAWNER;
	public static final DeferredItem<Item> PRISMATIC_SENSOR;
	public static final DeferredItem<Item> DEACTIVATED_PRISMATIC_SENSOR;
	public static final DeferredItem<Item> ANCIENT_PRISMARINE;
	public static final DeferredItem<Item> CRACKED_PRISMARINE;
	public static final DeferredItem<Item> MONUMENT_KEY;
	public static final DeferredItem<Item> PRISMATIC_RUNE;
	public static final DeferredItem<Item> PRISMATIC_VAULT;
	static {
		CHORINE = register("chorine", ChorineItem::new);
		CHORINE_ORE = block(StrawberrymcModBlocks.CHORINE_ORE);
		CHORINE_BLOCK = block(StrawberrymcModBlocks.CHORINE_BLOCK);
		CHORITE = register("chorite", ChoriteItem::new);
		ECHOWOOD_LOG = block(StrawberrymcModBlocks.ECHOWOOD_LOG);
		ECHOWOOD_WOOD = block(StrawberrymcModBlocks.ECHOWOOD_WOOD);
		STRIPPED_ECHOWOOD_LOG = block(StrawberrymcModBlocks.STRIPPED_ECHOWOOD_LOG);
		STRIPPED_ECHOWOOD_WOOD = block(StrawberrymcModBlocks.STRIPPED_ECHOWOOD_WOOD);
		ECHOWOOD_PLANKS = block(StrawberrymcModBlocks.ECHOWOOD_PLANKS);
		ECHOWOOD_LEAVES = block(StrawberrymcModBlocks.ECHOWOOD_LEAVES);
		ECHOWOOD_STAIRS = block(StrawberrymcModBlocks.ECHOWOOD_STAIRS);
		ECHOWOOD_SLAB = block(StrawberrymcModBlocks.ECHOWOOD_SLAB);
		ECHOWOOD_FENCE = block(StrawberrymcModBlocks.ECHOWOOD_FENCE);
		ECHOWOOD_FENCE_GATE = block(StrawberrymcModBlocks.ECHOWOOD_FENCE_GATE);
		ECHOWOOD_DOOR = doubleBlock(StrawberrymcModBlocks.ECHOWOOD_DOOR);
		ECHOWOOD_TRAPDOOR = block(StrawberrymcModBlocks.ECHOWOOD_TRAPDOOR);
		ECHOWOOD_PRESSURE_PLATE = block(StrawberrymcModBlocks.ECHOWOOD_PRESSURE_PLATE);
		ECHOWOOD_BUTTON = block(StrawberrymcModBlocks.ECHOWOOD_BUTTON);
		ECHOWOOD_SIGN = signBlock(StrawberrymcModBlocks.ECHOWOOD_SIGN, StrawberrymcModBlocks.ECHOWOOD_WALL_SIGN, new Item.Properties().stacksTo(16));
		ECHOWOOD_HANGING_SIGN = hangingSignBlock(StrawberrymcModBlocks.ECHOWOOD_HANGING_SIGN, StrawberrymcModBlocks.ECHOWOOD_WALL_HANGING_SIGN, new Item.Properties().stacksTo(16));
		ECHOWOOD_BOAT = register("echowood_boat", properties -> new BoatItem(StrawberrymcModEntities.ECHOWOOD_BOAT.get(), properties.stacksTo(1)));
		ECHOWOOD_CHEST_BOAT = register("echowood_chest_boat", properties -> new BoatItem(StrawberrymcModEntities.ECHOWOOD_CHEST_BOAT.get(), properties.stacksTo(1)));
		RADONITE_INGOT = register("radonite_ingot", RadoniteIngotItem::new);
		RADONITE_ORE = block(StrawberrymcModBlocks.RADONITE_ORE);
		RADONITE_BLOCK = block(StrawberrymcModBlocks.RADONITE_BLOCK);
		RADONITE_PICKAXE = register("radonite_pickaxe", RadonitePickaxeItem::new);
		RADONITE_AXE = register("radonite_axe", RadoniteAxeItem::new);
		RADONITE_SWORD = register("radonite_sword", RadoniteSwordItem::new);
		RADONITE_SHOVEL = register("radonite_shovel", RadoniteShovelItem::new);
		RADONITE_HOE = register("radonite_hoe", RadoniteHoeItem::new);
		RADONITE_ARMOR_HELMET = register("radonite_armor_helmet", RadoniteArmorItem.Helmet::new);
		RADONITE_ARMOR_CHESTPLATE = register("radonite_armor_chestplate", RadoniteArmorItem.Chestplate::new);
		RADONITE_ARMOR_LEGGINGS = register("radonite_armor_leggings", RadoniteArmorItem.Leggings::new);
		RADONITE_ARMOR_BOOTS = register("radonite_armor_boots", RadoniteArmorItem.Boots::new);
		RAW_RADONITE = register("raw_radonite", RawRadoniteItem::new);
		PALM_LOG = block(StrawberrymcModBlocks.PALM_LOG);
		PALM_WOOD = block(StrawberrymcModBlocks.PALM_WOOD);
		STRIPPED_PALM_LOG = block(StrawberrymcModBlocks.STRIPPED_PALM_LOG);
		STRIPPED_PALM_WOOD = block(StrawberrymcModBlocks.STRIPPED_PALM_WOOD);
		PALM_PLANKS = block(StrawberrymcModBlocks.PALM_PLANKS);
		PALM_LEAVES = block(StrawberrymcModBlocks.PALM_LEAVES);
		PALM_STAIRS = block(StrawberrymcModBlocks.PALM_STAIRS);
		PALM_SLAB = block(StrawberrymcModBlocks.PALM_SLAB);
		PALM_FENCE = block(StrawberrymcModBlocks.PALM_FENCE);
		PALM_FENCE_GATE = block(StrawberrymcModBlocks.PALM_FENCE_GATE);
		PALM_DOOR = doubleBlock(StrawberrymcModBlocks.PALM_DOOR);
		PALM_TRAPDOOR = block(StrawberrymcModBlocks.PALM_TRAPDOOR);
		PALM_PRESSURE_PLATE = block(StrawberrymcModBlocks.PALM_PRESSURE_PLATE);
		PALM_BUTTON = block(StrawberrymcModBlocks.PALM_BUTTON);
		PALM_SIGN = signBlock(StrawberrymcModBlocks.PALM_SIGN, StrawberrymcModBlocks.PALM_WALL_SIGN, new Item.Properties().stacksTo(16));
		PALM_HANGING_SIGN = hangingSignBlock(StrawberrymcModBlocks.PALM_HANGING_SIGN, StrawberrymcModBlocks.PALM_WALL_HANGING_SIGN, new Item.Properties().stacksTo(16));
		PALM_BOAT = register("palm_boat", properties -> new BoatItem(StrawberrymcModEntities.PALM_BOAT.get(), properties.stacksTo(1)));
		PALM_CHEST_BOAT = register("palm_chest_boat", properties -> new BoatItem(StrawberrymcModEntities.PALM_CHEST_BOAT.get(), properties.stacksTo(1)));
		DENDRITE_CRYSTAL = block(StrawberrymcModBlocks.DENDRITE_CRYSTAL);
		DENDRITE_BLOCK = block(StrawberrymcModBlocks.DENDRITE_BLOCK);
		GARNET_CRYSTAL = block(StrawberrymcModBlocks.GARNET_CRYSTAL);
		GARNET_BLOCK = block(StrawberrymcModBlocks.GARNET_BLOCK);
		GARNET_NODULE = register("garnet_nodule", GarnetNoduleItem::new);
		GARNET = register("garnet", GarnetItem::new);
		CRYSTALLINE_CALCITE = block(StrawberrymcModBlocks.CRYSTALLINE_CALCITE);
		RECOVERY_PEARL = register("recovery_pearl", RecoveryPearlItem::new);
		GLOWROOT_TUBER = register("glowroot_tuber", GlowrootTuberItem::new);
		GLOWROOT_TUBER_STONE = block(StrawberrymcModBlocks.GLOWROOT_TUBER_STONE);
		GLOWROOT = block(StrawberrymcModBlocks.GLOWROOT);
		GLOWROOT_DUMMY_SPAWNER = block(StrawberrymcModBlocks.GLOWROOT_DUMMY_SPAWNER);
		CLOVER_CLUFF = register("clover_cluff", CloverCluffItem::new);
		CLOVER_MAT = block(StrawberrymcModBlocks.CLOVER_MAT);
		RARE_CLOVER_CLUFF = register("rare_clover_cluff", RareCloverCluffItem::new);
		FERTILE_FARMLAND = block(StrawberrymcModBlocks.FERTILE_FARMLAND);
		SULFUR_SLAG = register("sulfur_slag", SulfurSlagItem::new);
		PLANT_FIBER = register("plant_fiber", PlantFiberItem::new);
		POOR_FARMLAND = block(StrawberrymcModBlocks.POOR_FARMLAND);
		FLINT_SHARD = register("flint_shard", FlintShardItem::new);
		FARMLAND_STRAWBERRY = block(StrawberrymcModBlocks.FARMLAND_STRAWBERRY);
		VOIDSTONE = block(StrawberrymcModBlocks.VOIDSTONE, new Item.Properties().fireResistant());
		OASIS_SPAWNER = block(StrawberrymcModBlocks.OASIS_SPAWNER);
		PALM_SAPLING = block(StrawberrymcModBlocks.PALM_SAPLING);
		COCONUT = register("coconut", CoconutItem::new);
		ECHOWOOD_SAPLING = block(StrawberrymcModBlocks.ECHOWOOD_SAPLING);
		JUNIPER_LOG = block(StrawberrymcModBlocks.JUNIPER_LOG);
		JUNIPER_WOOD = block(StrawberrymcModBlocks.JUNIPER_WOOD);
		STRIPPED_JUNIPER_LOG = block(StrawberrymcModBlocks.STRIPPED_JUNIPER_LOG);
		STRIPPED_JUNIPER_WOOD = block(StrawberrymcModBlocks.STRIPPED_JUNIPER_WOOD);
		JUNIPER_PLANKS = block(StrawberrymcModBlocks.JUNIPER_PLANKS);
		JUNIPER_LEAVES = block(StrawberrymcModBlocks.JUNIPER_LEAVES);
		JUNIPER_STAIRS = block(StrawberrymcModBlocks.JUNIPER_STAIRS);
		JUNIPER_SLAB = block(StrawberrymcModBlocks.JUNIPER_SLAB);
		JUNIPER_FENCE = block(StrawberrymcModBlocks.JUNIPER_FENCE);
		JUNIPER_FENCE_GATE = block(StrawberrymcModBlocks.JUNIPER_FENCE_GATE);
		JUNIPER_DOOR = doubleBlock(StrawberrymcModBlocks.JUNIPER_DOOR);
		JUNIPER_TRAPDOOR = block(StrawberrymcModBlocks.JUNIPER_TRAPDOOR);
		JUNIPER_PRESSURE_PLATE = block(StrawberrymcModBlocks.JUNIPER_PRESSURE_PLATE);
		JUNIPER_BUTTON = block(StrawberrymcModBlocks.JUNIPER_BUTTON);
		JUNIPER_SIGN = signBlock(StrawberrymcModBlocks.JUNIPER_SIGN, StrawberrymcModBlocks.JUNIPER_WALL_SIGN, new Item.Properties().stacksTo(16));
		JUNIPER_HANGING_SIGN = hangingSignBlock(StrawberrymcModBlocks.JUNIPER_HANGING_SIGN, StrawberrymcModBlocks.JUNIPER_WALL_HANGING_SIGN, new Item.Properties().stacksTo(16));
		JUNIPER_BOAT = register("juniper_boat", properties -> new BoatItem(StrawberrymcModEntities.JUNIPER_BOAT.get(), properties.stacksTo(1)));
		JUNIPER_CHEST_BOAT = register("juniper_chest_boat", properties -> new BoatItem(StrawberrymcModEntities.JUNIPER_CHEST_BOAT.get(), properties.stacksTo(1)));
		CHISELED_CAULDRON = block(StrawberrymcModBlocks.CHISELED_CAULDRON, new Item.Properties().stacksTo(1).rarity(Rarity.RARE));
		SMITE_VIAL = register("smite_vial", SmiteVialItem::new);
		TALL_FIRE = block(StrawberrymcModBlocks.TALL_FIRE);
		GREEN_BERYL = register("green_beryl", GreenBerylItem::new);
		COCONUT_OIL = register("coconut_oil", CoconutOilItem::new);
		IRON_BOW = register("iron_bow", IronBowItem::new);
		FIR_TREE_SPAWNER = block(StrawberrymcModBlocks.FIR_TREE_SPAWNER);
		FIR_SNAG_SPAWNER = block(StrawberrymcModBlocks.FIR_SNAG_SPAWNER);
		DEAD_FIR_SPAWNER = block(StrawberrymcModBlocks.DEAD_FIR_SPAWNER);
		MASTER_FIR_SPAWNER = block(StrawberrymcModBlocks.MASTER_FIR_SPAWNER);
		PRISMATIC_SENSOR = block(StrawberrymcModBlocks.PRISMATIC_SENSOR);
		DEACTIVATED_PRISMATIC_SENSOR = block(StrawberrymcModBlocks.DEACTIVATED_PRISMATIC_SENSOR);
		ANCIENT_PRISMARINE = block(StrawberrymcModBlocks.ANCIENT_PRISMARINE);
		CRACKED_PRISMARINE = block(StrawberrymcModBlocks.CRACKED_PRISMARINE);
		MONUMENT_KEY = register("monument_key", MonumentKeyItem::new);
		PRISMATIC_RUNE = register("prismatic_rune", PrismaticRuneItem::new);
		PRISMATIC_VAULT = block(StrawberrymcModBlocks.PRISMATIC_VAULT);
	}

	// Start of user code block custom items
	// End of user code block custom items
	private static <I extends Item> DeferredItem<I> register(String name, Function<Item.Properties, ? extends I> supplier) {
		return REGISTRY.registerItem(name, supplier, Item.Properties::new);
	}

	private static DeferredItem<Item> block(DeferredHolder<Block, Block> block) {
		return block(block, new Item.Properties());
	}

	private static DeferredItem<Item> block(DeferredHolder<Block, Block> block, Item.Properties properties) {
		return REGISTRY.registerItem(block.getId().getPath(), prop -> new BlockItem(block.get(), prop), () -> properties);
	}

	private static DeferredItem<Item> doubleBlock(DeferredHolder<Block, Block> block) {
		return doubleBlock(block, new Item.Properties());
	}

	private static DeferredItem<Item> doubleBlock(DeferredHolder<Block, Block> block, Item.Properties properties) {
		return REGISTRY.registerItem(block.getId().getPath(), prop -> new DoubleHighBlockItem(block.get(), prop), () -> properties);
	}

	private static DeferredItem<Item> signBlock(DeferredHolder<Block, Block> block, DeferredHolder<Block, Block> wallBlock) {
		return signBlock(block, wallBlock, new Item.Properties());
	}

	private static DeferredItem<Item> signBlock(DeferredHolder<Block, Block> block, DeferredHolder<Block, Block> wallBlock, Item.Properties properties) {
		return REGISTRY.registerItem(block.getId().getPath(), prop -> new SignItem(block.get(), wallBlock.get(), prop), () -> properties);
	}

	private static DeferredItem<Item> hangingSignBlock(DeferredHolder<Block, Block> block, DeferredHolder<Block, Block> wallBlock) {
		return hangingSignBlock(block, wallBlock, new Item.Properties());
	}

	private static DeferredItem<Item> hangingSignBlock(DeferredHolder<Block, Block> block, DeferredHolder<Block, Block> wallBlock, Item.Properties properties) {
		return REGISTRY.registerItem(block.getId().getPath(), prop -> new HangingSignItem(block.get(), wallBlock.get(), prop), () -> properties);
	}
}