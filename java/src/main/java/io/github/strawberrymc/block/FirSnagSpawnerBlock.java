package io.github.strawberrymc.block;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;

import io.github.strawberrymc.procedures.FirSnagGenerationProcedure;

public class FirSnagSpawnerBlock extends Block {
	public FirSnagSpawnerBlock(BlockBehaviour.Properties properties) {
		super(properties.sound(SoundType.GRAVEL).strength(1f, 10f));
	}

	@Override
	public void onPlace(BlockState blockstate, Level world, BlockPos pos, BlockState oldState, boolean moving) {
		super.onPlace(blockstate, world, pos, oldState, moving);
		FirSnagGenerationProcedure.execute(world, pos.getX(), pos.getY(), pos.getZ());
	}
}