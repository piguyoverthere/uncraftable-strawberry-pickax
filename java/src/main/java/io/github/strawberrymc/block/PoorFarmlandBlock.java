package io.github.strawberrymc.block;

import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;

import io.github.strawberrymc.procedures.TrampleFertileFarmlandProcedure;
import io.github.strawberrymc.procedures.PoorFarmlandFertilizeProcedure;

public class PoorFarmlandBlock extends Block {

	public PoorFarmlandBlock(BlockBehaviour.Properties properties) {
		super(properties.mapColor(MapColor.DIRT).sound(SoundType.GRAVEL).strength(0.6f).noOcclusion().isRedstoneConductor((bs, br, bp) -> false));
	}

	/* 
	private static final VoxelShape SHAPE = box(0, 0, 0, 16, 15, 16);

	public PoorFarmlandBlock(BlockBehaviour.Properties properties) {
		super(properties.mapColor(MapColor.DIRT).sound(SoundType.GRAVEL).strength(0.6f).noOcclusion().isRedstoneConductor((bs, br, bp) -> false));
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	public VoxelShape getVisualShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		return Shapes.empty();
	}

	@Override
	public void fallOn(Level world, BlockState blockstate, BlockPos pos, Entity entity, double distance) {
		super.fallOn(world, blockstate, pos, entity, distance);
		TrampleFertileFarmlandProcedure.execute(world, pos.getX(), pos.getY(), pos.getZ());
	}

	@Override
	public InteractionResult useWithoutItem(BlockState blockstate, Level world, BlockPos pos, Player entity, BlockHitResult hit) {
		super.useWithoutItem(blockstate, world, pos, entity, hit);
		int x = pos.getX();
		int y = pos.getY();
		int z = pos.getZ();
		double hitX = hit.getLocation().x;
		double hitY = hit.getLocation().y;
		double hitZ = hit.getLocation().z;
		Direction direction = hit.getDirection();
		PoorFarmlandFertilizeProcedure.execute(world, x, y, z, entity);
		return InteractionResult.SUCCESS;
	}
	*/
}