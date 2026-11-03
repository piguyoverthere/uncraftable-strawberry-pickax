package io.github.strawberrymc.monument;

import io.github.strawberrymc.custommixin.MonumentBuildingAccessor;
import io.github.strawberrymc.init.StrawberrymcModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.structures.OceanMonumentPieces;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Builds the expanded Ocean Monument: vanilla's outer shell (walls, wings, entrance, roof and pillars) at double scale
// with nothing inside, so the hall can be filled by puzzle jigsaws later. On the roof stands a prismarine and copper
// gazebo with an upside-down dome of Cracked Prismarine on its ceiling over an Ancient Prismarine-capped vent, and a Prismatic Sensor stands
// on a pedestal in each quadrant of the hall.
//
// The shell is recorded once per orientation by running vanilla's own building code, minus its rooms, against a
// recording stand-in for the world; each recorded block then becomes a 2x2x2 cube.
public final class MonumentShell {
	public static final int SCALE = 2;
	private static final int VANILLA_WIDTH = 58;
	public static final int WIDTH = VANILLA_WIDTH * SCALE;
	public static final int SHELL_HEIGHT = 23 * SCALE;
	// Room above the shell for the gazebo, so it's part of the building's box (and protected by the sensors)
	public static final int GAZEBO_HEIGHT = 14;
	public static final int HEIGHT = SHELL_HEIGHT + GAZEBO_HEIGHT;
	// The recording covers one block of vanilla's support pillars under the floor up to just above the roof
	private static final int MIN_LY = -1;
	private static final int MAX_LY = 30;
	private static final int RECORD_BASE_Y = 39;

	private static final BlockState FLOOR = Blocks.PRISMARINE_BRICKS.defaultBlockState();
	private static final BlockState WATER = Blocks.WATER.defaultBlockState();
	private static final ThreadLocal<Boolean> RECORDING = ThreadLocal.withInitial(() -> false);
	private static final Map<Direction, Recording> RECORDINGS = new ConcurrentHashMap<>();

	private MonumentShell() {
	}

	public static boolean isRecording() {
		return RECORDING.get();
	}

	// Vanilla's box, doubled around the same centre and tall enough for the gazebo
	public static BoundingBox scaledBox(BoundingBox vanilla) {
		int grow = (WIDTH - VANILLA_WIDTH) / 2;
		int minX = vanilla.minX() - grow, minZ = vanilla.minZ() - grow;
		return new BoundingBox(minX, vanilla.minY(), minZ, minX + WIDTH - 1, vanilla.minY() + HEIGHT - 1, minZ + WIDTH - 1);
	}

	// Vanilla's solid shell blocks, indexed by vanilla-scale local position
	record Recording(BlockState[] solids, int roofTop) {
		BlockState get(int lx, int ly, int lz) {
			if (lx < 0 || lz < 0 || lx >= VANILLA_WIDTH || lz >= VANILLA_WIDTH || ly < MIN_LY || ly > MAX_LY)
				return null;
			return solids[index(lx, ly, lz)];
		}
	}

	private static int index(int lx, int ly, int lz) {
		return ((ly - MIN_LY) * VANILLA_WIDTH + lx) * VANILLA_WIDTH + lz;
	}

	static Recording recording(Direction orientation) {
		return RECORDINGS.computeIfAbsent(orientation, MonumentShell::record);
	}

	private static Recording record(Direction orientation) {
		BlockState[] recorded = new BlockState[(MAX_LY - MIN_LY + 1) * VANILLA_WIDTH * VANILLA_WIDTH];
		RECORDING.set(true);
		try {
			OceanMonumentPieces.MonumentBuilding building = new OceanMonumentPieces.MonumentBuilding(RandomSource.create(0), 0, 0, orientation);
			// Hollow: without its rooms, only the outer shell is built
			((MonumentBuildingAccessor) building).strawberrymc$getChildPieces().clear();
			BoundingBox box = building.getBoundingBox();
			building.postProcess(recorder(recorded, box), null, null, RandomSource.create(0), new BoundingBox(-64, -64, -64, 128, 128, 128),
				new ChunkPos(0, 0), BlockPos.ZERO);
		} finally {
			RECORDING.set(false);
		}
		BlockState[] solids = new BlockState[recorded.length];
		for (int i = 0; i < recorded.length; i++)
			if (recorded[i] != null && !recorded[i].isAir() && !recorded[i].is(Blocks.WATER))
				solids[i] = recorded[i];
		// The top of the solid roof slab around the centre, where the gazebo stands. Vanilla's sparse decoration above
		// it doesn't count, so the gazebo sits right on the roof
		int roofTop = 0;
		int middle = VANILLA_WIDTH / 2;
		for (int ly = MAX_LY; ly > 0 && roofTop == 0; ly--) {
			int solid = 0;
			for (int lx = middle - 5; lx < middle + 5; lx++)
				for (int lz = middle - 5; lz < middle + 5; lz++)
					if (solids[index(lx, ly, lz)] != null)
						solid++;
			if (solid >= 50)
				roofTop = ly;
		}
		return new Recording(solids, roofTop);
	}

	// A stand-in world that records what vanilla's building code places, relative to its box
	private static WorldGenLevel recorder(BlockState[] recorded, BoundingBox box) {
		InvocationHandler handler = (proxy, method, args) -> {
			switch (method.getName()) {
				case "setBlock" -> {
					BlockPos pos = (BlockPos) args[0];
					int lx = pos.getX() - box.minX(), ly = pos.getY() - box.minY(), lz = pos.getZ() - box.minZ();
					if (lx >= 0 && lz >= 0 && lx < VANILLA_WIDTH && lz < VANILLA_WIDTH && ly >= MIN_LY && ly <= MAX_LY)
						recorded[index(lx, ly, lz)] = (BlockState) args[1];
					return true;
				}
				case "getBlockState" -> {
					BlockPos pos = (BlockPos) args[0];
					int lx = pos.getX() - box.minX(), ly = pos.getY() - box.minY(), lz = pos.getZ() - box.minZ();
					BlockState state = lx >= 0 && lz >= 0 && lx < VANILLA_WIDTH && lz < VANILLA_WIDTH && ly >= MIN_LY && ly <= MAX_LY
						? recorded[index(lx, ly, lz)] : null;
					return state != null ? state : Blocks.AIR.defaultBlockState();
				}
				case "getFluidState" -> {
					return ((BlockState) handlerState(recorded, box, (BlockPos) args[0])).getFluidState();
				}
				// Water fills up to MAX_LY, and support pillars stop one block under the floor
				case "getSeaLevel" -> {
					return RECORD_BASE_Y + MAX_LY;
				}
				case "getMinY" -> {
					return RECORD_BASE_Y + MIN_LY - 2;
				}
				case "scheduleTick" -> {
					return null;
				}
				case "isClientSide" -> {
					return false;
				}
				case "hashCode" -> {
					return System.identityHashCode(proxy);
				}
				case "equals" -> {
					return proxy == args[0];
				}
				case "toString" -> {
					return "MonumentShell recorder";
				}
				default -> {
					if (method.isDefault())
						return InvocationHandler.invokeDefault(proxy, method, args);
					throw new UnsupportedOperationException("Monument recording can't use " + method.getName());
				}
			}
		};
		return (WorldGenLevel) Proxy.newProxyInstance(MonumentShell.class.getClassLoader(), new Class<?>[] {WorldGenLevel.class}, handler);
	}

	private static BlockState handlerState(BlockState[] recorded, BoundingBox box, BlockPos pos) {
		int lx = pos.getX() - box.minX(), ly = pos.getY() - box.minY(), lz = pos.getZ() - box.minZ();
		BlockState state = lx >= 0 && lz >= 0 && lx < VANILLA_WIDTH && lz < VANILLA_WIDTH && ly >= MIN_LY && ly <= MAX_LY ? recorded[index(lx, ly, lz)] : null;
		return state != null ? state : Blocks.AIR.defaultBlockState();
	}

	// ---- Building ----

	public static void place(WorldGenLevel level, BoundingBox chunkBB, BoundingBox box, Direction orientation) {
		Recording recording = recording(orientation);
		Layout layout = new Layout(box, orientation, box.minY() + SCALE * (recording.roofTop() + 1));
		int seaLevel = level.getSeaLevel();
		int top = Math.max(seaLevel, box.minY() + SHELL_HEIGHT);
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int x = Math.max(box.minX(), chunkBB.minX()); x <= Math.min(box.maxX(), chunkBB.maxX()); x++) {
			for (int z = Math.max(box.minZ(), chunkBB.minZ()); z <= Math.min(box.maxZ(), chunkBB.maxZ()); z++) {
				int lx = (x - box.minX()) / SCALE, lz = (z - box.minZ()) / SCALE;
				// Vanilla's support pillars reach down to the sea floor
				BlockState pillar = recording.get(lx, -1, lz);
				if (pillar != null) {
					pos.set(x, box.minY() - 1, z);
					while (pos.getY() > level.getMinY() + 1 && isReplaceable(level.getBlockState(pos))) {
						level.setBlock(pos, pillar, 2);
						pos.move(Direction.DOWN);
					}
				}
				for (int y = box.minY(); y < top; y++) {
					BlockState state = recording.get(lx, (y - box.minY()) / SCALE, lz);
					if (state == null)
						state = y < box.minY() + SCALE ? FLOOR : y < seaLevel ? WATER : Blocks.AIR.defaultBlockState();
					pos.set(x, y, z);
					level.setBlock(pos, state, 2);
				}
			}
		}
		buildGazebo(level, chunkBB, layout);
		buildArena(level, chunkBB, layout);
		buildSensors(level, chunkBB, layout);
	}

	private static boolean isReplaceable(BlockState state) {
		return state.isAir() || state.liquid() || state.is(Blocks.SEAGRASS) || state.is(Blocks.TALL_SEAGRASS) || state.is(Blocks.KELP)
			|| state.is(Blocks.KELP_PLANT);
	}

	private static void set(WorldGenLevel level, BoundingBox chunkBB, BlockPos pos, BlockState state) {
		if (chunkBB.isInside(pos))
			level.setBlock(pos, state, 2);
	}

	private static void buildGazebo(WorldGenLevel level, BoundingBox chunkBB, Layout layout) {
		int cx = layout.centerX(), cz = layout.centerZ();
		int floor = layout.floorY();
		BlockState darkPrismarine = Blocks.DARK_PRISMARINE.defaultBlockState();
		BlockState bricks = Blocks.PRISMARINE_BRICKS.defaultBlockState();
		BlockState copper = Blocks.CUT_COPPER.defaultBlockState();
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int dx = -Layout.GAZEBO_RADIUS; dx <= Layout.GAZEBO_RADIUS; dx++) {
			for (int dz = -Layout.GAZEBO_RADIUS; dz <= Layout.GAZEBO_RADIUS; dz++) {
				boolean edge = Math.max(Math.abs(dx), Math.abs(dz)) == Layout.GAZEBO_RADIUS;
				set(level, chunkBB, pos.set(cx + dx, floor, cz + dz), edge ? bricks : darkPrismarine);
				// Prop the floor up where it overhangs the roof
				for (int y = floor - 1; y > floor - 8; y--) {
					pos.set(cx + dx, y, cz + dz);
					if (!chunkBB.isInside(pos) || !level.getBlockState(pos).is(Blocks.WATER))
						break;
					level.setBlock(pos, bricks, 2);
				}
			}
		}
		int roofY = floor + Layout.PILLAR_HEIGHT + 2;
		// Clear vanilla's roof decoration out of the gazebo
		for (int dx = -Layout.GAZEBO_RADIUS; dx <= Layout.GAZEBO_RADIUS; dx++)
			for (int dz = -Layout.GAZEBO_RADIUS; dz <= Layout.GAZEBO_RADIUS; dz++)
				for (int y = floor + 1; y < roofY; y++)
					set(level, chunkBB, pos.set(cx + dx, y, cz + dz), Blocks.WATER.defaultBlockState());
		// Pillars at the corners and the middle of each side, with a sea lantern partway up
		for (int dx = -Layout.GAZEBO_RADIUS; dx <= Layout.GAZEBO_RADIUS; dx += Layout.GAZEBO_RADIUS)
			for (int dz = -Layout.GAZEBO_RADIUS; dz <= Layout.GAZEBO_RADIUS; dz += Layout.GAZEBO_RADIUS) {
				if (dx == 0 && dz == 0)
					continue;
				for (int y = 1; y <= Layout.PILLAR_HEIGHT; y++)
					set(level, chunkBB, pos.set(cx + dx, floor + y, cz + dz), y == 3 ? Blocks.SEA_LANTERN.defaultBlockState() : bricks);
				set(level, chunkBB, pos.set(cx + dx, floor + Layout.PILLAR_HEIGHT + 1, cz + dz), darkPrismarine);
			}
		// Stepped copper roof
		int steps = (Layout.GAZEBO_RADIUS + 2) / 2;
		for (int step = 0; step < steps; step++) {
			int radius = Layout.GAZEBO_RADIUS + 1 - step * 2;
			for (int dx = -radius; dx <= radius; dx++)
				for (int dz = -radius; dz <= radius; dz++)
					if (step == 0 || step == steps - 1 || Math.max(Math.abs(dx), Math.abs(dz)) >= radius - 1)
						set(level, chunkBB, pos.set(cx + dx, roofY + step, cz + dz), copper);
		}
		set(level, chunkBB, pos.set(cx, roofY + steps, cz), Blocks.LIGHTNING_ROD.defaultBlockState());
		// The upside-down dome of Cracked Prismarine stuck to the underside of the roof
		for (BlockPos lump : layout.lump())
			set(level, chunkBB, lump, StrawberrymcModBlocks.CRACKED_PRISMARINE.get().defaultBlockState());
		// The vent: a shaft into the roof with soul sand at the bottom, so a bubble column rises once the cap is mined
		for (int dx = -2; dx <= 2; dx++)
			for (int dz = -2; dz <= 2; dz++)
				for (int depth = 1; depth <= Layout.VENT_DEPTH + 1; depth++) {
					boolean shaft = Math.abs(dx) <= 1 && Math.abs(dz) <= 1;
					BlockState state = !shaft ? darkPrismarine : depth == Layout.VENT_DEPTH + 1 ? Blocks.SOUL_SAND.defaultBlockState() : Blocks.WATER.defaultBlockState();
					set(level, chunkBB, pos.set(cx + dx, floor - depth, cz + dz), state);
				}
		for (BlockPos cap : layout.ventCap())
			set(level, chunkBB, cap, StrawberrymcModBlocks.ANCIENT_PRISMARINE.get().defaultBlockState());
	}

	// The boss arena in front of the gazebo: a round glass floor over a water tank sunk into the roof, decorated with sand,
	// gravel, seagrass, coral and fish. The Prismatic Vault sits in the middle of the glass, and the Prismatic Rune waits in a
	// fixed item frame on a pedestal at the bottom of the tank
	private static void buildArena(WorldGenLevel level, BoundingBox chunkBB, Layout layout) {
		BlockPos center = layout.arenaCenter();
		int top = center.getY();
		int radius = Layout.ARENA_RADIUS;
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int dx = -radius - 1; dx <= radius + 1; dx++) {
			for (int dz = -radius - 1; dz <= radius + 1; dz++) {
				double distance = Math.sqrt(dx * dx + dz * dz);
				if (distance > radius + 1.2)
					continue;
				int x = center.getX() + dx, z = center.getZ() + dz;
				if (x < chunkBB.minX() || x > chunkBB.maxX() || z < chunkBB.minZ() || z > chunkBB.maxZ())
					continue;
				boolean wall = distance > radius + 0.2;
				RandomSource random = RandomSource.create(x * 341873128712L + z * 132897987541L);
				for (int depth = 0; depth <= Layout.TANK_DEPTH + 2; depth++) {
					int y = top - depth;
					BlockState state;
					if (depth == 0)
						state = wall ? Blocks.DARK_PRISMARINE.defaultBlockState() : Blocks.GLASS.defaultBlockState();
					else if (depth == Layout.TANK_DEPTH + 2)
						state = Blocks.PRISMARINE_BRICKS.defaultBlockState();
					else if (wall)
						state = depth == 4 && (dx * 7 + dz * 3) % 5 == 0 ? Blocks.SEA_LANTERN.defaultBlockState() : Blocks.PRISMARINE_BRICKS.defaultBlockState();
					else if (depth == Layout.TANK_DEPTH + 1)
						state = random.nextFloat() < 0.3F ? Blocks.GRAVEL.defaultBlockState() : Blocks.SAND.defaultBlockState();
					else
						state = Blocks.WATER.defaultBlockState();
					level.setBlock(pos.set(x, y, z), state, 2);
				}
				// Prop the wall up where the roof falls away under it
				if (wall)
					for (int y = top - Layout.TANK_DEPTH - 3; y > top - Layout.TANK_DEPTH - 20; y--) {
						if (!level.getBlockState(pos.set(x, y, z)).is(Blocks.WATER))
							break;
						level.setBlock(pos, Blocks.PRISMARINE_BRICKS.defaultBlockState(), 2);
					}
				else if (Math.abs(dx) > 1 || Math.abs(dz) > 1)
					decorate(level, pos.set(x, top - Layout.TANK_DEPTH, z), random);
			}
		}
		set(level, chunkBB, center, StrawberrymcModBlocks.PRISMATIC_VAULT.get().defaultBlockState());
		set(level, chunkBB, layout.runeFrame().below(), Blocks.DARK_PRISMARINE.defaultBlockState());
		if (chunkBB.isInside(layout.runeFrame()))
			spawnArenaEntities(level, layout);
	}

	// The tank's bottom layer of water: seagrass, coral, sea pickles and the odd kelp
	private static void decorate(WorldGenLevel level, BlockPos.MutableBlockPos pos, RandomSource random) {
		float roll = random.nextFloat();
		if (roll < 0.22F) {
			level.setBlock(pos, Blocks.SEAGRASS.defaultBlockState(), 2);
		} else if (roll < 0.28F) {
			level.setBlock(pos, Blocks.TALL_SEAGRASS.defaultBlockState().setValue(net.minecraft.world.level.block.TallSeagrassBlock.HALF,
				net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER), 2);
			level.setBlock(pos.move(0, 1, 0), Blocks.TALL_SEAGRASS.defaultBlockState().setValue(net.minecraft.world.level.block.TallSeagrassBlock.HALF,
				net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER), 2);
		} else if (roll < 0.34F) {
			net.minecraft.world.level.block.Block[] corals = {Blocks.TUBE_CORAL, Blocks.BRAIN_CORAL, Blocks.BUBBLE_CORAL, Blocks.FIRE_CORAL, Blocks.HORN_CORAL,
				Blocks.TUBE_CORAL_FAN, Blocks.BRAIN_CORAL_FAN, Blocks.BUBBLE_CORAL_FAN, Blocks.FIRE_CORAL_FAN, Blocks.HORN_CORAL_FAN};
			level.setBlock(pos, corals[random.nextInt(corals.length)].defaultBlockState(), 2);
		} else if (roll < 0.37F) {
			level.setBlock(pos, Blocks.SEA_PICKLE.defaultBlockState().setValue(net.minecraft.world.level.block.SeaPickleBlock.PICKLES, 1 + random.nextInt(4)), 2);
		} else if (roll < 0.39F) {
			int height = 2 + random.nextInt(3);
			for (int i = 0; i < height; i++)
				level.setBlock(pos.move(0, i == 0 ? 0 : 1, 0), i == height - 1 ? Blocks.KELP.defaultBlockState() : Blocks.KELP_PLANT.defaultBlockState(), 2);
		}
	}

	private static void spawnArenaEntities(WorldGenLevel level, Layout layout) {
		net.minecraft.server.level.ServerLevel serverLevel = level.getLevel();
		net.minecraft.world.entity.decoration.ItemFrame frame = new net.minecraft.world.entity.decoration.ItemFrame(serverLevel, layout.runeFrame(), Direction.UP);
		frame.setItem(new net.minecraft.world.item.ItemStack(io.github.strawberrymc.init.StrawberrymcModItems.PRISMATIC_RUNE.get()), false);
		((io.github.strawberrymc.custommixin.ItemFrameAccessor) frame).strawberrymc$setFixed(true);
		frame.setInvulnerable(true);
		frame.addTag(MonumentArena.RUNE_FRAME_TAG);
		level.addFreshEntity(frame);
		RandomSource random = RandomSource.create(layout.arenaCenter().asLong());
		BlockPos center = layout.arenaCenter();
		for (int i = 0; i < 9; i++) {
			net.minecraft.world.entity.EntityType<? extends net.minecraft.world.entity.Mob> type = i < 6 ? net.minecraft.world.entity.EntityType.TROPICAL_FISH
				: net.minecraft.world.entity.EntityType.COD;
			net.minecraft.world.entity.Mob fish = type.create(serverLevel, net.minecraft.world.entity.EntitySpawnReason.STRUCTURE);
			if (fish == null)
				continue;
			double angle = random.nextDouble() * Math.PI * 2, distance = 2 + random.nextDouble() * (Layout.ARENA_RADIUS - 4);
			fish.snapTo(center.getX() + 0.5 + Math.cos(angle) * distance, center.getY() - 2 - random.nextInt(Layout.TANK_DEPTH - 2),
				center.getZ() + 0.5 + Math.sin(angle) * distance, random.nextFloat() * 360, 0);
			fish.finalizeSpawn(level, level.getCurrentDifficultyAt(fish.blockPosition()), net.minecraft.world.entity.EntitySpawnReason.STRUCTURE, null);
			fish.setPersistenceRequired();
			level.addFreshEntity(fish);
		}
	}

	private static void buildSensors(WorldGenLevel level, BoundingBox chunkBB, Layout layout) {
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (BlockPos sensor : layout.sensors()) {
			for (int dx = -1; dx <= 1; dx++)
				for (int dz = -1; dz <= 1; dz++)
					for (int y = sensor.getY() - Layout.PEDESTAL_HEIGHT; y < sensor.getY(); y++) {
						boolean lantern = dx != 0 && dz != 0 && y == sensor.getY() - 1;
						set(level, chunkBB, pos.set(sensor.getX() + dx, y, sensor.getZ() + dz),
							lantern ? Blocks.SEA_LANTERN.defaultBlockState() : Blocks.DARK_PRISMARINE.defaultBlockState());
					}
			set(level, chunkBB, sensor, StrawberrymcModBlocks.PRISMATIC_SENSOR.get().defaultBlockState());
		}
	}

	// ---- Where things are ----

	public static Layout layout(StructurePiece building) {
		BoundingBox box = building.getBoundingBox();
		Direction orientation = building.getOrientation() == null ? Direction.NORTH : building.getOrientation();
		return new Layout(box, orientation, box.minY() + SCALE * (recording(orientation).roofTop() + 1));
	}

	// Positions in an expanded monument. floorY is the gazebo's floor, the top of the roof
	public record Layout(BoundingBox box, Direction orientation, int floorY) {
		static final int GAZEBO_RADIUS = 9;
		static final int PILLAR_HEIGHT = 7;
		// The Cracked Prismarine dome hanging from the roof: its radius and how far it hangs down
		static final double DOME_RADIUS = 3.6;
		static final int DOME_DEPTH = 3;
		// The arena's glass circle and the water under it
		public static final int ARENA_RADIUS = 14;
		static final int TANK_DEPTH = 7;
		// How far the arena's centre is from the gazebo's, towards the entrance
		private static final int ARENA_OFFSET = 25;
		static final int VENT_DEPTH = 3;
		static final int PEDESTAL_HEIGHT = 3;
		// Pedestal spots in the hall (local, double scale, entrance at z=0), one per quadrant
		private static final int[][] SENSOR_SPOTS = {{38, 52}, {77, 52}, {38, 82}, {77, 82}};

		public int centerX() {
			return box.minX() + WIDTH / 2;
		}

		public int centerZ() {
			return box.minZ() + WIDTH / 2;
		}

		// The Ancient Prismarine cap over the vent, set into the gazebo floor
		public List<BlockPos> ventCap() {
			List<BlockPos> cap = new ArrayList<>();
			for (int dx = -1; dx <= 1; dx++)
				for (int dz = -1; dz <= 1; dz++)
					cap.add(new BlockPos(centerX() + dx, floorY, centerZ() + dz));
			return cap;
		}

		public BlockPos ventCenter() {
			return new BlockPos(centerX(), floorY, centerZ());
		}

		// Just under the middle of the dome's flat top, against the roof
		public BlockPos lumpCenter() {
			return new BlockPos(centerX(), domeTop() - 1, centerZ());
		}

		private int domeTop() {
			return floorY + PILLAR_HEIGHT + 1;
		}

		// An upside-down dome: flat against the underside of the roof, rounding down to a point
		public List<BlockPos> lump() {
			List<BlockPos> lump = new ArrayList<>();
			int r = (int) Math.ceil(DOME_RADIUS);
			for (int dy = 0; dy <= DOME_DEPTH; dy++)
				for (int dx = -r; dx <= r; dx++)
					for (int dz = -r; dz <= r; dz++)
						if (dx * dx + dz * dz + dy * dy <= DOME_RADIUS * DOME_RADIUS)
							lump.add(new BlockPos(centerX() + dx, domeTop() - dy, centerZ() + dz));
			return lump;
		}

		// The dome's lowest point, where the jet from the vent hits it
		public BlockPos domeTip() {
			return new BlockPos(centerX(), domeTop() - DOME_DEPTH, centerZ());
		}

		// The middle of the arena's glass floor, where the Prismatic Vault is, level with the gazebo floor
		public BlockPos arenaCenter() {
			return world(WIDTH / 2, floorY, WIDTH / 2 - ARENA_OFFSET);
		}

		// Where the Prismatic Rune's item frame hangs, on a pedestal at the bottom of the tank
		public BlockPos runeFrame() {
			return arenaCenter().below(TANK_DEPTH - 1);
		}

		// Whether pos is on the arena's glass (or just above it)
		public boolean onArena(net.minecraft.world.phys.Vec3 pos) {
			BlockPos center = arenaCenter();
			double dx = pos.x - (center.getX() + 0.5), dz = pos.z - (center.getZ() + 0.5);
			return dx * dx + dz * dz <= ARENA_RADIUS * ARENA_RADIUS && pos.y >= center.getY() && pos.y <= center.getY() + 6;
		}

		public List<BlockPos> sensors() {
			List<BlockPos> sensors = new ArrayList<>();
			for (int[] spot : SENSOR_SPOTS)
				sensors.add(world(spot[0], box.minY() + SCALE + PEDESTAL_HEIGHT, spot[1]));
			return sensors;
		}

		// The sensor guarding the quadrant pos is in
		public BlockPos sensorFor(BlockPos pos) {
			boolean east = pos.getX() >= centerX(), south = pos.getZ() >= centerZ();
			for (BlockPos sensor : sensors())
				if ((sensor.getX() >= centerX()) == east && (sensor.getZ() >= centerZ()) == south)
					return sensor;
			return sensors().getFirst();
		}

		// Whether pos is inside the monument, from its floor to the top of the gazebo
		public boolean contains(BlockPos pos) {
			return pos.getX() >= box.minX() && pos.getX() <= box.maxX() && pos.getZ() >= box.minZ() && pos.getZ() <= box.maxZ()
				&& pos.getY() >= box.minY() - 1 && pos.getY() <= box.maxY();
		}

		// Local position (entrance side at z=0) to world, the same way StructurePiece orients its pieces
		private BlockPos world(int x, int y, int z) {
			return switch (orientation) {
				case SOUTH -> new BlockPos(box.minX() + x, y, box.minZ() + z);
				case WEST -> new BlockPos(box.maxX() - z, y, box.minZ() + x);
				case EAST -> new BlockPos(box.minX() + z, y, box.minZ() + x);
				default -> new BlockPos(box.minX() + x, y, box.maxZ() - z);
			};
		}
	}
}
