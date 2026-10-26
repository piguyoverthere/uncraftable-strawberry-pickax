package io.github.strawberrymc.procedures;

import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;

import io.github.strawberrymc.network.StrawberrymcModVariables;

@EventBusSubscriber
public class EntityAggroHandlerProcedure {
	@SubscribeEvent
	public static void onEntitySetsAttackTarget(LivingChangeTargetEvent event) {
		execute(event, event.getEntity().level(), event.getOriginalAboutToBeSetTarget());
	}

	public static void execute(LevelAccessor world, Entity entity) {
		execute(null, world, entity);
	}

	private static void execute(@Nullable Event event, LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		if ((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null) instanceof Player) {
			{
				StrawberrymcModVariables.PlayerVariables _vars = entity.getData(StrawberrymcModVariables.PLAYER_VARIABLES);
				_vars.lastaggro = world instanceof Level _level2 ? _level2.getDefaultClockTime() : 0;
				_vars.markSyncDirty();
			}
		}
	}
}