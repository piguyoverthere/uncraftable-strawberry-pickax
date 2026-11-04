package io.github.strawberrymc.procedures;

import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.GameType;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.InteractionHand;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.tags.BlockTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.BlockPos;

import javax.annotation.Nullable;

import io.github.strawberrymc.init.StrawberrymcModBlocks;

@EventBusSubscriber
public class CloverMatOnBoneMealSuccessProcedure {
	@SubscribeEvent
	public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
		if (event.getHand() != InteractionHand.MAIN_HAND)
			return;
		execute(event, event.getLevel(), event.getPos().getX(), event.getPos().getY(), event.getPos().getZ(), event.getEntity());
	}

	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		execute(null, world, x, y, z, entity);
	}

	private static void execute(@Nullable Event event, LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		double spread_dir = 0;
		if ((world.getBlockState(BlockPos.containing(x, y, z))).getBlock() == StrawberrymcModBlocks.CLOVER_MAT.get() && (entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == Items.BONE_MEAL) {
			if (Mth.nextInt(RandomSource.create(), 1, 3) == 2) {
				spread_dir = Mth.nextInt(RandomSource.create(), 1, 4);
				if (spread_dir == 1 && (world.getBlockState(BlockPos.containing(x, y, z - 1))).getBlock() == Blocks.AIR && (world.getBlockState(BlockPos.containing(x, y - 1, z - 1))).is(BlockTags.create(Identifier.parse("minecraft:dirt")))) {
					world.setBlock(BlockPos.containing(x, y, z - 1), StrawberrymcModBlocks.CLOVER_MAT.get().defaultBlockState(), 3);
					if (world instanceof ServerLevel _level)
						_level.sendParticles(ParticleTypes.COMPOSTER, x, y, z, 5, 0.5, 0.5, 0.5, 0.5);
				}
				if (spread_dir == 3 && (world.getBlockState(BlockPos.containing(x, y, z + 1))).getBlock() == Blocks.AIR && (world.getBlockState(BlockPos.containing(x, y - 1, z + 1))).is(BlockTags.create(Identifier.parse("minecraft:dirt")))) {
					world.setBlock(BlockPos.containing(x, y, z + 1), StrawberrymcModBlocks.CLOVER_MAT.get().defaultBlockState(), 3);
					if (world instanceof ServerLevel _level)
						_level.sendParticles(ParticleTypes.COMPOSTER, x, y, z, 5, 0.5, 0.5, 0.5, 0.5);
				}
				if (spread_dir == 2 && (world.getBlockState(BlockPos.containing(x + 1, y, z))).getBlock() == Blocks.AIR && (world.getBlockState(BlockPos.containing(x + 1, y - 1, z))).is(BlockTags.create(Identifier.parse("minecraft:dirt")))) {
					world.setBlock(BlockPos.containing(x + 1, y, z), StrawberrymcModBlocks.CLOVER_MAT.get().defaultBlockState(), 3);
					if (world instanceof ServerLevel _level)
						_level.sendParticles(ParticleTypes.COMPOSTER, x, y, z, 5, 0.5, 0.5, 0.5, 0.5);
				}
				if (spread_dir == 4 && (world.getBlockState(BlockPos.containing(x - 1, y, z))).getBlock() == Blocks.AIR && (world.getBlockState(BlockPos.containing(x - 1, y - 1, z))).is(BlockTags.create(Identifier.parse("minecraft:dirt")))) {
					world.setBlock(BlockPos.containing(x - 1, y, z), StrawberrymcModBlocks.CLOVER_MAT.get().defaultBlockState(), 3);
					if (world instanceof ServerLevel _level)
						_level.sendParticles(ParticleTypes.COMPOSTER, x, y, z, 5, 0.5, 0.5, 0.5, 0.5);
				}
			}
			if (!(entity instanceof Player _plr30 && _plr30.gameMode() == GameType.CREATIVE)) {
				(entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).shrink(1);
			}
		}
	}
}