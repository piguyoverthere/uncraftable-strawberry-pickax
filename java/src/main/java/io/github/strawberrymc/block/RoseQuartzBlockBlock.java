package io.github.strawberrymc.block;

import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.Block;

public class RoseQuartzBlockBlock extends Block {
	public RoseQuartzBlockBlock(BlockBehaviour.Properties properties) {
		super(properties.sound(SoundType.AMETHYST).strength(1.5f).lightLevel(s -> 3));
	}
}