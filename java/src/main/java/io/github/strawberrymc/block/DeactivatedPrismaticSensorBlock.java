package io.github.strawberrymc.block;

import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.Block;

public class DeactivatedPrismaticSensorBlock extends Block {
	public DeactivatedPrismaticSensorBlock(BlockBehaviour.Properties properties) {
		super(properties.mapColor(MapColor.DIAMOND).strength(-1, 3600000).pushReaction(PushReaction.BLOCK));
	}
}