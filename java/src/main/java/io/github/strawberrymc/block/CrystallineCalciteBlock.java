package io.github.strawberrymc.block;

import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;

import java.util.function.Function;

// Faces the side it was placed on: flat on floors (UP, what worldgen uses), hanging on ceilings, upright on walls
public class CrystallineCalciteBlock extends Block {
	public static final EnumProperty<Direction> FACING = DirectionalBlock.FACING;
	private final Function<BlockState, VoxelShape> shapes = this.makeShapes();

	public CrystallineCalciteBlock(BlockBehaviour.Properties properties) {
		super(properties.sound(SoundType.CALCITE).strength(1f, 10f).noOcclusion().isRedstoneConductor((bs, br, bp) -> false));
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.UP));
	}

	@Override
	public int getLightDampening(BlockState state) {
		return 0;
	}

	private Function<BlockState, VoxelShape> makeShapes() {
		return this.getShapeForEachState(state -> {
			return switch (state.getValue(FACING)) {
				case DOWN -> box(0, 8, 0, 16, 16, 16);
				case NORTH -> box(0, 0, 8, 16, 16, 16);
				case SOUTH -> box(0, 0, 0, 16, 16, 8);
				case EAST -> box(0, 0, 0, 8, 16, 16);
				case WEST -> box(8, 0, 0, 16, 16, 16);
				default -> box(0, 0, 0, 16, 8, 16);
			};
		});
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		return shapes.apply(state);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return this.defaultBlockState().setValue(FACING, context.getClickedFace());
	}

	public BlockState rotate(BlockState state, Rotation rot) {
		return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
	}

	public BlockState mirror(BlockState state, Mirror mirrorIn) {
		return state.rotate(mirrorIn.getRotation(state.getValue(FACING)));
	}
}