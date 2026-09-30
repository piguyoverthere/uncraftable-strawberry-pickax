package io.github.strawberrymc.item;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.tags.BlockTags;
import net.minecraft.resources.Identifier;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.BlockPos;

import java.util.stream.Stream;
import java.util.List;

import io.github.strawberrymc.init.StrawberrymcModBlocks;

public class IronHammerItem extends Item {
	public IronHammerItem(Item.Properties properties) {
		super(properties.durability(100).attributes(ItemAttributeModifiers.builder().add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID, 5, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
				.add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID, -2, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND).build()).enchantable(2).setNoCombineRepair());
	}

	@Override
	public float getDestroySpeed(ItemStack itemstack, BlockState blockstate) {
		return List.of(StrawberrymcModBlocks.CHORINE_ORE.get(), StrawberrymcModBlocks.CHORINE_BLOCK.get(), StrawberrymcModBlocks.ECHOWOOD_LOG.get(), StrawberrymcModBlocks.ECHOWOOD_WOOD.get(), StrawberrymcModBlocks.STRIPPED_ECHOWOOD_LOG.get(),
				StrawberrymcModBlocks.STRIPPED_ECHOWOOD_WOOD.get(), StrawberrymcModBlocks.ECHOWOOD_PLANKS.get(), StrawberrymcModBlocks.ECHOWOOD_LEAVES.get(), StrawberrymcModBlocks.ECHOWOOD_STAIRS.get(), StrawberrymcModBlocks.ECHOWOOD_SLAB.get(),
				StrawberrymcModBlocks.ECHOWOOD_FENCE.get(), StrawberrymcModBlocks.ECHOWOOD_FENCE_GATE.get(), StrawberrymcModBlocks.ECHOWOOD_DOOR.get(), StrawberrymcModBlocks.ECHOWOOD_TRAPDOOR.get(),
				StrawberrymcModBlocks.ECHOWOOD_PRESSURE_PLATE.get(), StrawberrymcModBlocks.ECHOWOOD_BUTTON.get(), StrawberrymcModBlocks.ECHOWOOD_SIGN.get(), StrawberrymcModBlocks.ECHOWOOD_WALL_SIGN.get(),
				StrawberrymcModBlocks.ECHOWOOD_HANGING_SIGN.get(), StrawberrymcModBlocks.ECHOWOOD_WALL_HANGING_SIGN.get(), StrawberrymcModBlocks.RADONITE_ORE.get(), StrawberrymcModBlocks.RADONITE_BLOCK.get(), StrawberrymcModBlocks.PALM_LOG.get(),
				StrawberrymcModBlocks.PALM_WOOD.get(), StrawberrymcModBlocks.STRIPPED_PALM_LOG.get(), StrawberrymcModBlocks.STRIPPED_PALM_WOOD.get(), StrawberrymcModBlocks.PALM_PLANKS.get(), StrawberrymcModBlocks.PALM_LEAVES.get(),
				StrawberrymcModBlocks.PALM_STAIRS.get(), StrawberrymcModBlocks.PALM_SLAB.get(), StrawberrymcModBlocks.PALM_FENCE.get(), StrawberrymcModBlocks.PALM_FENCE_GATE.get(), StrawberrymcModBlocks.PALM_DOOR.get(),
				StrawberrymcModBlocks.PALM_TRAPDOOR.get(), StrawberrymcModBlocks.PALM_PRESSURE_PLATE.get(), StrawberrymcModBlocks.PALM_BUTTON.get(), StrawberrymcModBlocks.PALM_SIGN.get(), StrawberrymcModBlocks.PALM_WALL_SIGN.get(),
				StrawberrymcModBlocks.PALM_HANGING_SIGN.get(), StrawberrymcModBlocks.PALM_WALL_HANGING_SIGN.get(), StrawberrymcModBlocks.DENDRITE_CRYSTAL.get(), StrawberrymcModBlocks.DENDRITE_BLOCK.get(), StrawberrymcModBlocks.GARNET_CRYSTAL.get(),
				StrawberrymcModBlocks.GARNET_BLOCK.get(), StrawberrymcModBlocks.CRYSTALLINE_CALCITE.get(), StrawberrymcModBlocks.GLOWROOT_TUBER_STONE.get(), StrawberrymcModBlocks.GLOWROOT.get(), StrawberrymcModBlocks.GLOWROOT_DUMMY_SPAWNER.get(),
				StrawberrymcModBlocks.CLOVER_MAT.get(), StrawberrymcModBlocks.PALM_SEEDLING.get(), StrawberrymcModBlocks.FERTILE_FARMLAND.get(), Blocks.AIR, Blocks.VOID_AIR, Blocks.CAVE_AIR, Blocks.STONE, Blocks.STONE_STAIRS, Blocks.STONE_SLAB,
				Blocks.GRANITE, Blocks.POLISHED_GRANITE, Blocks.GRANITE_STAIRS, Blocks.POLISHED_GRANITE_STAIRS, Blocks.GRANITE_SLAB, Blocks.POLISHED_GRANITE_SLAB, Blocks.GRANITE_WALL, Blocks.DIORITE, Blocks.DIORITE_STAIRS, Blocks.DIORITE_SLAB,
				Blocks.DIORITE_WALL, Blocks.POLISHED_DIORITE, Blocks.POLISHED_DIORITE_SLAB, Blocks.POLISHED_DIORITE_STAIRS, Blocks.ANDESITE, Blocks.ANDESITE_STAIRS, Blocks.ANDESITE_SLAB, Blocks.ANDESITE_WALL, Blocks.POLISHED_ANDESITE,
				Blocks.POLISHED_ANDESITE_STAIRS, Blocks.POLISHED_ANDESITE_SLAB, Blocks.SMOOTH_STONE, Blocks.DEEPSLATE, Blocks.REINFORCED_DEEPSLATE, Blocks.COBBLED_DEEPSLATE, Blocks.COBBLED_DEEPSLATE_STAIRS, Blocks.COBBLED_DEEPSLATE_SLAB,
				Blocks.COBBLED_DEEPSLATE_WALL, Blocks.POLISHED_DEEPSLATE, Blocks.POLISHED_DEEPSLATE_STAIRS, Blocks.POLISHED_DEEPSLATE_SLAB, Blocks.POLISHED_DEEPSLATE_WALL, Blocks.DEEPSLATE_TILES, Blocks.DEEPSLATE_TILE_STAIRS,
				Blocks.DEEPSLATE_TILE_SLAB, Blocks.DEEPSLATE_TILE_WALL, Blocks.DEEPSLATE_BRICKS, Blocks.DEEPSLATE_BRICK_STAIRS, Blocks.DEEPSLATE_BRICK_SLAB, Blocks.DEEPSLATE_BRICK_WALL, Blocks.CHISELED_DEEPSLATE, Blocks.CRACKED_DEEPSLATE_BRICKS,
				Blocks.CRACKED_DEEPSLATE_TILES, Blocks.TUFF, Blocks.TUFF_SLAB, Blocks.TUFF_STAIRS, Blocks.TUFF_WALL, Blocks.POLISHED_TUFF, Blocks.POLISHED_TUFF_SLAB, Blocks.POLISHED_TUFF_STAIRS, Blocks.POLISHED_TUFF_WALL, Blocks.CHISELED_TUFF,
				Blocks.TUFF_BRICKS, Blocks.TUFF_BRICK_SLAB, Blocks.TUFF_BRICK_STAIRS, Blocks.TUFF_BRICK_WALL, Blocks.CHISELED_TUFF_BRICKS, Blocks.CALCITE, Blocks.DRIPSTONE_BLOCK, Blocks.POINTED_DRIPSTONE, Blocks.GRASS_BLOCK, Blocks.DIRT_PATH,
				Blocks.MYCELIUM, Blocks.DIRT, Blocks.COARSE_DIRT, Blocks.PODZOL, Blocks.ROOTED_DIRT, Blocks.MUD, Blocks.PACKED_MUD, Blocks.MUD_BRICKS, Blocks.MUD_BRICK_STAIRS, Blocks.MUD_BRICK_SLAB, Blocks.MUD_BRICK_WALL, Blocks.COBBLESTONE,
				Blocks.MOSSY_COBBLESTONE, Blocks.MOSSY_COBBLESTONE_STAIRS, Blocks.MOSSY_COBBLESTONE_SLAB, Blocks.MOSS_BLOCK, Blocks.MOSS_CARPET, Blocks.PALE_MOSS_BLOCK, Blocks.PALE_MOSS_CARPET, Blocks.MAGMA_BLOCK, Blocks.OBSIDIAN,
				Blocks.CRYING_OBSIDIAN, Blocks.BLACKSTONE, Blocks.BLACKSTONE_STAIRS, Blocks.BLACKSTONE_WALL, Blocks.BLACKSTONE_SLAB, Blocks.POLISHED_BLACKSTONE, Blocks.POLISHED_BLACKSTONE_BRICKS, Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS,
				Blocks.CHISELED_POLISHED_BLACKSTONE, Blocks.POLISHED_BLACKSTONE_BRICK_SLAB, Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS, Blocks.POLISHED_BLACKSTONE_BRICK_WALL, Blocks.POLISHED_BLACKSTONE_STAIRS, Blocks.POLISHED_BLACKSTONE_SLAB,
				Blocks.POLISHED_BLACKSTONE_WALL, Blocks.COAL_BLOCK, Blocks.IRON_BLOCK, Blocks.COPPER_BLOCK, Blocks.EXPOSED_COPPER, Blocks.WEATHERED_COPPER, Blocks.OXIDIZED_COPPER, Blocks.WAXED_COPPER_BLOCK, Blocks.WAXED_EXPOSED_COPPER,
				Blocks.WAXED_WEATHERED_COPPER, Blocks.WAXED_OXIDIZED_COPPER, Blocks.CUT_COPPER, Blocks.EXPOSED_CUT_COPPER, Blocks.WEATHERED_CUT_COPPER, Blocks.OXIDIZED_CUT_COPPER, Blocks.WAXED_CUT_COPPER, Blocks.WAXED_EXPOSED_CUT_COPPER,
				Blocks.WAXED_WEATHERED_CUT_COPPER, Blocks.WAXED_OXIDIZED_CUT_COPPER, Blocks.CHISELED_COPPER, Blocks.EXPOSED_CHISELED_COPPER, Blocks.WEATHERED_CHISELED_COPPER, Blocks.OXIDIZED_CHISELED_COPPER, Blocks.WAXED_CHISELED_COPPER,
				Blocks.WAXED_EXPOSED_CHISELED_COPPER, Blocks.WAXED_WEATHERED_CHISELED_COPPER, Blocks.WAXED_OXIDIZED_CHISELED_COPPER, Blocks.CUT_COPPER_STAIRS, Blocks.EXPOSED_CUT_COPPER_STAIRS, Blocks.WEATHERED_CUT_COPPER_STAIRS,
				Blocks.OXIDIZED_CUT_COPPER_STAIRS, Blocks.WAXED_CUT_COPPER_STAIRS, Blocks.WAXED_EXPOSED_CUT_COPPER_STAIRS, Blocks.WAXED_WEATHERED_CUT_COPPER_STAIRS, Blocks.WAXED_OXIDIZED_CUT_COPPER_STAIRS, Blocks.CUT_COPPER_SLAB,
				Blocks.EXPOSED_CUT_COPPER_SLAB, Blocks.WEATHERED_CUT_COPPER_SLAB, Blocks.OXIDIZED_CUT_COPPER_SLAB, Blocks.WAXED_CUT_COPPER_SLAB, Blocks.WAXED_EXPOSED_CUT_COPPER_SLAB, Blocks.WAXED_WEATHERED_CUT_COPPER_SLAB,
				Blocks.WAXED_OXIDIZED_CUT_COPPER_SLAB, Blocks.REDSTONE_BLOCK, Blocks.GOLD_BLOCK, Blocks.LAPIS_BLOCK, Blocks.DIAMOND_BLOCK, Blocks.EMERALD_BLOCK, Blocks.NETHERITE_BLOCK, Blocks.SMOOTH_QUARTZ, Blocks.CHISELED_QUARTZ_BLOCK,
				Blocks.QUARTZ_PILLAR, Blocks.QUARTZ_BLOCK, Blocks.QUARTZ_BRICKS, Blocks.SMOOTH_QUARTZ_STAIRS, Blocks.SMOOTH_QUARTZ_SLAB, Blocks.SLIME_BLOCK, Blocks.SANDSTONE, Blocks.CHISELED_SANDSTONE, Blocks.CUT_SANDSTONE, Blocks.SANDSTONE_STAIRS,
				Blocks.SMOOTH_SANDSTONE, Blocks.SMOOTH_SANDSTONE_STAIRS, Blocks.SMOOTH_SANDSTONE_SLAB, Blocks.CUT_SANDSTONE_SLAB, Blocks.SANDSTONE_WALL, Blocks.RED_SANDSTONE, Blocks.CHISELED_RED_SANDSTONE, Blocks.CUT_RED_SANDSTONE,
				Blocks.RED_SANDSTONE_STAIRS, Blocks.SMOOTH_RED_SANDSTONE, Blocks.SMOOTH_RED_SANDSTONE_STAIRS, Blocks.SMOOTH_RED_SANDSTONE_SLAB, Blocks.CUT_RED_SANDSTONE_SLAB, Blocks.RED_SANDSTONE_WALL, Blocks.NOTE_BLOCK, Blocks.IRON_DOOR,
				Blocks.COPPER_DOOR, Blocks.EXPOSED_COPPER_DOOR, Blocks.WEATHERED_COPPER_DOOR, Blocks.OXIDIZED_COPPER_DOOR, Blocks.WAXED_COPPER_DOOR, Blocks.WAXED_EXPOSED_COPPER_DOOR, Blocks.WAXED_WEATHERED_COPPER_DOOR,
				Blocks.WAXED_OXIDIZED_COPPER_DOOR, Blocks.OAK_TRAPDOOR, Blocks.SPRUCE_TRAPDOOR, Blocks.BIRCH_TRAPDOOR, Blocks.JUNGLE_TRAPDOOR, Blocks.ACACIA_TRAPDOOR, Blocks.DARK_OAK_TRAPDOOR, Blocks.PALE_OAK_TRAPDOOR, Blocks.CRIMSON_TRAPDOOR,
				Blocks.WARPED_TRAPDOOR, Blocks.MANGROVE_TRAPDOOR, Blocks.CHERRY_TRAPDOOR, Blocks.BAMBOO_TRAPDOOR, Blocks.IRON_TRAPDOOR, Blocks.COPPER_TRAPDOOR, Blocks.EXPOSED_COPPER_TRAPDOOR, Blocks.WEATHERED_COPPER_TRAPDOOR,
				Blocks.OXIDIZED_COPPER_TRAPDOOR, Blocks.WAXED_COPPER_TRAPDOOR, Blocks.WAXED_EXPOSED_COPPER_TRAPDOOR, Blocks.WAXED_WEATHERED_COPPER_TRAPDOOR, Blocks.WAXED_OXIDIZED_COPPER_TRAPDOOR, Blocks.COPPER_GRATE, Blocks.EXPOSED_COPPER_GRATE,
				Blocks.WEATHERED_COPPER_GRATE, Blocks.OXIDIZED_COPPER_GRATE, Blocks.WAXED_COPPER_GRATE, Blocks.WAXED_EXPOSED_COPPER_GRATE, Blocks.WAXED_WEATHERED_COPPER_GRATE, Blocks.WAXED_OXIDIZED_COPPER_GRATE, Blocks.COPPER_BULB,
				Blocks.EXPOSED_COPPER_BULB, Blocks.WEATHERED_COPPER_BULB, Blocks.OXIDIZED_COPPER_BULB, Blocks.WAXED_COPPER_BULB, Blocks.WAXED_EXPOSED_COPPER_BULB, Blocks.WAXED_WEATHERED_COPPER_BULB, Blocks.WAXED_OXIDIZED_COPPER_BULB, Blocks.LEVER,
				Blocks.STONE_BUTTON, Blocks.POLISHED_BLACKSTONE_BUTTON, Blocks.TRIPWIRE_HOOK, Blocks.TRIPWIRE, Blocks.DAYLIGHT_DETECTOR, Blocks.REDSTONE_TORCH, Blocks.REDSTONE_WALL_TORCH, Blocks.REDSTONE_WIRE, Blocks.REPEATER, Blocks.COMPARATOR,
				Blocks.REDSTONE_LAMP, Blocks.DISPENSER, Blocks.DROPPER, Blocks.OBSERVER, Blocks.CRAFTER, Blocks.HOPPER, Blocks.TARGET, Blocks.LIGHTNING_ROD, Blocks.EXPOSED_LIGHTNING_ROD, Blocks.WEATHERED_LIGHTNING_ROD, Blocks.OXIDIZED_LIGHTNING_ROD,
				Blocks.WAXED_LIGHTNING_ROD, Blocks.WAXED_EXPOSED_LIGHTNING_ROD, Blocks.WAXED_WEATHERED_LIGHTNING_ROD, Blocks.WAXED_OXIDIZED_LIGHTNING_ROD, Blocks.SCULK_SENSOR, Blocks.CALIBRATED_SCULK_SENSOR, Blocks.SCULK, Blocks.SCULK_CATALYST,
				Blocks.SCULK_VEIN, Blocks.SCULK_SHRIEKER).contains(blockstate.getBlock())
				|| Stream.of(BlockTags.create(Identifier.parse("c:stones")), BlockTags.create(Identifier.parse("minecraft:dirt")), BlockTags.create(Identifier.parse("c:ores/quartz")), BlockTags.create(Identifier.parse("c:sandstone/blocks")),
						BlockTags.create(Identifier.parse("c:sandstone/blocks"))).anyMatch(blockstate::is) ? 4f : 1;
	}

	@Override
	public boolean mineBlock(ItemStack itemstack, Level world, BlockState blockstate, BlockPos pos, LivingEntity entity) {
		itemstack.hurtAndBreak(1, entity, entity.getUsedItemHand().asEquipmentSlot());
		return true;
	}

	@Override
	public void hurtEnemy(ItemStack itemstack, LivingEntity entity, LivingEntity sourceentity) {
		itemstack.hurtAndBreak(2, entity, entity.getUsedItemHand().asEquipmentSlot());
	}

	@Override
	public ItemStackTemplate getCraftingRemainder(ItemInstance itemInstance) {
		ItemStack retval = new ItemStack(this);
		retval.setDamageValue(itemInstance.getOrDefault(DataComponents.DAMAGE, 0) + 1);
		if (retval.getDamageValue() >= retval.getMaxDamage()) {
			return null;
		}
		return ItemStackTemplate.fromNonEmptyStack(retval);
	}
}