package io.github.strawberrymc.procedures;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.sounds.SoundSource;
import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.BuiltInRegistries;
import io.github.strawberrymc.init.StrawberrymcModBlocks;
import io.github.strawberrymc.init.StrawberrymcModItems;
import net.minecraft.core.BlockPos;

import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

@EventBusSubscriber(modid = "strawberrymc")
public class TrampleFertileFarmlandProcedure {
	public static void onFarmlandTrample(BlockEvent.FarmlandTrampleEvent event) {
		Entity entity = event.getEntity();
		Level world = entity.level();
		double x = event.getPos().getX();
		double y = event.getPos().getY();
		double z = event.getPos().getZ();

		if (world instanceof Level && (world.getBlockState(event.getPos()).is(StrawberrymcModBlocks.FERTILE_FARMLAND))) {
			if (!world.isClientSide()) {
				world.playSound(null, BlockPos.containing(x, y, z), BuiltInRegistries.SOUND_EVENT.getValue(Identifier.parse("block.crop.break")), SoundSource.NEUTRAL, 1, 1);
			}
			world.setBlock(BlockPos.containing(x, y, z), Blocks.FARMLAND.defaultBlockState(), 3);
			world.addFreshEntity(new ItemEntity(world, x, y+1, z, (new ItemStack(StrawberrymcModItems.SULFUR_SLAG.get(), 1))));
		}
	}
}