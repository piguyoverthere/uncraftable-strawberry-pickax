package io.github.strawberrymc;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;

// The fir procedures place blocks one by one with their default states. During worldgen nothing updates them
// afterwards, so fences never connect and every leaf keeps distance 7 (too far from a log), which makes it decay.
// This fixes up the finished tree the way vanilla tree features do.
public class FirTreeFinisher {
	// Widest foliage tier is 4 blocks out, roots 1; tallest tree is 25 blocks plus a 4 block spire
	private static final int RADIUS = 5;
	private static final int HEIGHT = 31;

	public static void finish(LevelAccessor world, BlockPos base) {
		BlockPos min = base.offset(-RADIUS, -1, -RADIUS);
		BlockPos max = base.offset(RADIUS, HEIGHT, RADIUS);

		// Leaf distance = steps to the nearest log through other leaves, like LeavesBlock works it out
		Map<BlockPos, Integer> distances = new HashMap<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
			BlockState state = world.getBlockState(pos);
			if (state.getBlock() instanceof FenceBlock) {
				world.setBlock(pos, Block.updateFromNeighbourShapes(state, world, pos), 2);
			} else {
				OptionalInt distance = LeavesBlock.getOptionalDistanceAt(state);
				if (distance.isPresent() && distance.getAsInt() == 0) {
					distances.put(pos.immutable(), 0);
					queue.add(pos.immutable());
				}
			}
		}
		while (!queue.isEmpty()) {
			BlockPos pos = queue.poll();
			int next = distances.get(pos) + 1;
			if (next >= LeavesBlock.DECAY_DISTANCE)
				continue;
			for (Direction direction : Direction.values()) {
				BlockPos neighbor = pos.relative(direction);
				if (!distances.containsKey(neighbor) && isInside(neighbor, min, max) && world.getBlockState(neighbor).getBlock() instanceof LeavesBlock) {
					distances.put(neighbor, next);
					queue.add(neighbor);
				}
			}
		}

		for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
			BlockState state = world.getBlockState(pos);
			if (!(state.getBlock() instanceof LeavesBlock))
				continue;
			Integer distance = distances.get(pos);
			// Leaves the sparse foliage cut off from the trunk would decay, so they are kept like player-placed leaves
			BlockState fixed = distance != null ? state.setValue(LeavesBlock.DISTANCE, distance) : state.setValue(LeavesBlock.PERSISTENT, true);
			if (fixed != state)
				world.setBlock(pos, fixed, 2);
		}
	}

	private static boolean isInside(BlockPos pos, BlockPos min, BlockPos max) {
		return pos.getX() >= min.getX() && pos.getX() <= max.getX() && pos.getY() >= min.getY() && pos.getY() <= max.getY() && pos.getZ() >= min.getZ()
				&& pos.getZ() <= max.getZ();
	}
}
