package io.github.strawberrymc.block;

import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.Block;

public class CinnabarBlock extends Block {
	public CinnabarBlock(BlockBehaviour.Properties properties) {
		super(properties.strength(1.5f, 6f));
	}
}