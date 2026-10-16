package io.github.strawberrymc.procedures;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

import java.lang.Iterable;
import java.util.List;

public class RecoveryPearlFetchProcedure {
	public static void execute(BlockPos pos, Entity sourceentity, Level world) {
		if (pos == null || sourceentity == null) { return;}
		
		AABB itemTpRange = new AABB(pos).inflate(5.5);
		AABB entityTpRange = new AABB(pos).inflate(2.5);

		List<Entity> entitiesToTp = world.getEntities(null, entityTpRange);
		List<ItemEntity> itemsToTp = world.getEntitiesOfClass(ItemEntity.class, itemTpRange);

		entitiesToTp.forEach(target -> target.teleportTo((double) sourceentity.getX(), (double) sourceentity.getY(), (double) sourceentity.getZ()));
		itemsToTp.forEach(target -> target.teleportTo((double) sourceentity.getX(), (double) sourceentity.getY(), (double) sourceentity.getZ()));
		
	}
}