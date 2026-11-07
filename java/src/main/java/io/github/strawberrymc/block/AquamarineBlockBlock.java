package io.github.strawberrymc.block;

import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.Block;

public class AquamarineBlockBlock extends Block {
	public AquamarineBlockBlock(BlockBehaviour.Properties properties) {
		super(properties.sound(SoundType.AMETHYST).strength(1.5f).lightLevel(s -> 6));
	}
}