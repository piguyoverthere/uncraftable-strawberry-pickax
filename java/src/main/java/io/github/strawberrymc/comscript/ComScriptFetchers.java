package io.github.strawberrymc.comscript;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.strawberrymc.comscript.ComScriptExpression.Call;
import io.github.strawberrymc.comscript.ComScriptExpression.Environment;
import io.github.strawberrymc.comscript.ComScriptExpression.Node;
import io.github.strawberrymc.comscript.ComScriptExpression.ScriptError;
import io.github.strawberrymc.comscript.ComScriptExpression.Selector;
import io.github.strawberrymc.comscript.ComScriptExpression.Variable;
import io.github.strawberrymc.custommixin.BiomeTemperatureInvoker;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseRouterData;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Locale;
import java.util.Set;

// ComScript's native values: functions that read the world, entities, weather and time
public final class ComScriptFetchers {
	private ComScriptFetchers() {
	}

	public static final Set<String> NAMES = Set.of(
		// Entities
		"getdir", "getpitch", "getdeltax", "getdeltay", "getdeltaz", "getspeed", "getx", "gety", "getz", "gethealth", "getmaxhealth", "getarmor",
		"getfood", "getxplevel", "getitem", "getitemname", "getname", "gettype", "getgamemode", "getcount", "getdistentities", "isalive", "isonground",
		"isinwater", "issneaking", "issprinting", "isonfire", "isplayer", "hastag",
		// Positions and terrain
		"getdistvector", "gettemperature", "getclimatetemperature", "gethumidity", "getcontinentalness", "geterosion", "getdepth", "getweirdness",
		"getpeaksvalleys", "getprecipitation", "getbiome", "getblock", "getlight", "getheight", "getredstone",
		// World
		"gettime", "getday", "getgametime", "getplayercount", "getdimension", "isdownfall", "isthundering", "isday", "isnight");

	// Fetchers that can be written without brackets, like isDownfall
	public static final Set<String> BARE = Set.of("isdownfall", "isthundering", "isday", "isnight", "gettime", "getday", "getgametime", "getplayercount",
		"getdimension");

	public static Object bare(String name, Environment env) {
		return call(new Call(name.toLowerCase(Locale.ROOT), List.of(), List.of()), env);
	}

	public static Object call(Call call, Environment env) {
		ServerLevel level = env.source().getLevel();
		List<Node> args = call.arguments();
		switch (call.name()) {
			// ---- Entities ----
			case "getdir" -> {
				return (double) Mth.wrapDegrees(entity(call, env).getYRot());
			}
			case "getpitch" -> {
				return (double) entity(call, env).getXRot();
			}
			case "getdeltax" -> {
				return entity(call, env).getDeltaMovement().x;
			}
			case "getdeltay" -> {
				return entity(call, env).getDeltaMovement().y;
			}
			case "getdeltaz" -> {
				return entity(call, env).getDeltaMovement().z;
			}
			case "getspeed" -> {
				return entity(call, env).getDeltaMovement().length();
			}
			case "getx" -> {
				return entity(call, env).getX();
			}
			case "gety" -> {
				return entity(call, env).getY();
			}
			case "getz" -> {
				return entity(call, env).getZ();
			}
			case "gethealth" -> {
				return (double) living(call, env).getHealth();
			}
			case "getmaxhealth" -> {
				return (double) living(call, env).getMaxHealth();
			}
			case "getarmor" -> {
				return (long) living(call, env).getArmorValue();
			}
			case "getfood" -> {
				return (long) player(call, env).getFoodData().getFoodLevel();
			}
			case "getxplevel" -> {
				return (long) player(call, env).experienceLevel;
			}
			case "getitem", "getitemname" -> {
				expect(call, 2);
				Entity entity = firstEntity(args.get(0), env);
				int slot = (int) number(args.get(1), env);
				SlotAccess access = entity.getSlot(slot);
				if (access == null)
					throw new ScriptError("Error: " + entity.getName().getString() + " has no slot " + slot);
				ItemStack stack = access.get();
				if (call.name().equals("getitem"))
					return (long) stack.getCount();
				return stack.isEmpty() ? "minecraft:air" : BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
			}
			case "getname" -> {
				return entity(call, env).getName().getString();
			}
			case "gettype" -> {
				return BuiltInRegistries.ENTITY_TYPE.getKey(entity(call, env).getType()).toString();
			}
			case "getgamemode" -> {
				if (!(entity(call, env) instanceof ServerPlayer player))
					throw ScriptError.type("getGamemode() needs a player");
				return player.gameMode.getGameModeForPlayer().getName();
			}
			case "getcount" -> {
				expect(call, 1);
				return (long) entities(args.getFirst(), env).size();
			}
			case "getdistentities" -> {
				expect(call, 2);
				return firstEntity(args.get(0), env).position().distanceTo(firstEntity(args.get(1), env).position());
			}
			case "isalive" -> {
				expect(call, 1);
				return entities(args.getFirst(), env).stream().anyMatch(Entity::isAlive);
			}
			case "isonground" -> {
				return entity(call, env).onGround();
			}
			case "isinwater" -> {
				return entity(call, env).isInWater();
			}
			case "issneaking" -> {
				return entity(call, env).isShiftKeyDown();
			}
			case "issprinting" -> {
				return entity(call, env).isSprinting();
			}
			case "isonfire" -> {
				return entity(call, env).isOnFire();
			}
			case "isplayer" -> {
				expect(call, 1);
				List<? extends Entity> found = entities(args.getFirst(), env);
				return !found.isEmpty() && found.getFirst() instanceof Player;
			}
			case "hastag" -> {
				expect(call, 2);
				return firstEntity(args.get(0), env).entityTags().contains(ComScriptExpression.format(ComScriptExpression.evaluate(args.get(1), env)));
			}

			// ---- Positions and terrain ----
			case "getdistvector" -> {
				expect(call, 6);
				Vec3 a = new Vec3(number(args.get(0), env), number(args.get(1), env), number(args.get(2), env));
				Vec3 b = new Vec3(number(args.get(3), env), number(args.get(4), env), number(args.get(5), env));
				return a.distanceTo(b);
			}
			case "gettemperature" -> {
				BlockPos pos = loadedPosition(call, env);
				Biome biome = level.getBiome(pos).value();
				return (double) ((BiomeTemperatureInvoker) (Object) biome).strawberrymc$getTemperature(pos, level.getSeaLevel());
			}
			case "getclimatetemperature", "gethumidity", "getcontinentalness", "geterosion", "getdepth", "getweirdness", "getpeaksvalleys" -> {
				BlockPos pos = position(call, env);
				Climate.TargetPoint point = level.getChunkSource().randomState().sampler()
					.sample(QuartPos.fromBlock(pos.getX()), QuartPos.fromBlock(pos.getY()), QuartPos.fromBlock(pos.getZ()));
				long value = switch (call.name()) {
					case "getclimatetemperature" -> point.temperature();
					case "gethumidity" -> point.humidity();
					case "getcontinentalness" -> point.continentalness();
					case "geterosion" -> point.erosion();
					case "getdepth" -> point.depth();
					default -> point.weirdness();
				};
				float unquantized = Climate.unquantizeCoord(value);
				return (double) (call.name().equals("getpeaksvalleys") ? NoiseRouterData.peaksAndValleys(unquantized) : unquantized);
			}
			case "getprecipitation" -> {
				BlockPos pos = loadedPosition(call, env);
				return level.getBiome(pos).value().getPrecipitationAt(pos, level.getSeaLevel()).getSerializedName();
			}
			case "getbiome" -> {
				return level.getBiome(loadedPosition(call, env)).unwrapKey().map(key -> key.identifier().toString()).orElse("unknown");
			}
			case "getblock" -> {
				return BuiltInRegistries.BLOCK.getKey(level.getBlockState(loadedPosition(call, env)).getBlock()).toString();
			}
			case "getlight" -> {
				return (long) level.getMaxLocalRawBrightness(loadedPosition(call, env));
			}
			case "getredstone" -> {
				return (long) level.getBestNeighborSignal(loadedPosition(call, env));
			}
			case "getheight" -> {
				expect(call, 2);
				int x = (int) Math.floor(number(args.get(0), env)), z = (int) Math.floor(number(args.get(1), env));
				if (!level.isLoaded(new BlockPos(x, 0, z)))
					throw new ScriptError("Error: the chunk at " + x + " " + z + " isn't loaded");
				return (long) level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
			}

			// ---- World ----
			case "gettime" -> {
				expect(call, 0);
				return level.getDefaultClockTime() % 24000;
			}
			case "getday" -> {
				expect(call, 0);
				return level.getDefaultClockTime() / 24000;
			}
			case "getgametime" -> {
				expect(call, 0);
				return level.getGameTime();
			}
			case "getplayercount" -> {
				expect(call, 0);
				return (long) level.getServer().getPlayerCount();
			}
			case "getdimension" -> {
				expect(call, 0);
				return level.dimension().identifier().toString();
			}
			case "isdownfall" -> {
				expect(call, 0);
				return level.isRaining();
			}
			case "isthundering" -> {
				expect(call, 0);
				return level.isThundering();
			}
			case "isday" -> {
				expect(call, 0);
				return level.isBrightOutside();
			}
			case "isnight" -> {
				expect(call, 0);
				return level.isDarkOutside();
			}
			default -> {
				return null;
			}
		}
	}

	// Every entity an argument names: a selector like @e[type=pig], a player name, or a variable holding either
	static List<? extends Entity> entities(Node arg, Environment env) {
		String text;
		if (arg instanceof Variable variable && env.variable(variable.name()) == null)
			text = variable.name();
		else {
			Object value = ComScriptExpression.evaluate(arg, env);
			text = value instanceof Selector selector ? selector.text() : ComScriptExpression.format(value);
		}
		try {
			return EntityArgument.entities().parse(new StringReader(text)).findEntities(env.source());
		} catch (CommandSyntaxException e) {
			throw new ScriptError("Error: " + e.getMessage());
		}
	}

	private static Entity firstEntity(Node arg, Environment env) {
		List<? extends Entity> found = entities(arg, env);
		if (found.isEmpty())
			throw new ScriptError("Error: No entity matched " + describe(arg, env));
		return found.getFirst();
	}

	private static String describe(Node arg, Environment env) {
		if (arg instanceof Variable variable && env.variable(variable.name()) == null)
			return variable.name();
		return ComScriptExpression.format(ComScriptExpression.evaluate(arg, env));
	}

	private static Entity entity(Call call, Environment env) {
		expect(call, 1);
		return firstEntity(call.arguments().getFirst(), env);
	}

	private static LivingEntity living(Call call, Environment env) {
		if (!(entity(call, env) instanceof LivingEntity living))
			throw ScriptError.type(call.name() + "() needs a living entity");
		return living;
	}

	private static Player player(Call call, Environment env) {
		if (!(entity(call, env) instanceof Player player))
			throw ScriptError.type(call.name() + "() needs a player");
		return player;
	}

	private static BlockPos position(Call call, Environment env) {
		expect(call, 3);
		List<Node> args = call.arguments();
		return BlockPos.containing(number(args.get(0), env), number(args.get(1), env), number(args.get(2), env));
	}

	private static BlockPos loadedPosition(Call call, Environment env) {
		BlockPos pos = position(call, env);
		if (!env.source().getLevel().isLoaded(pos))
			throw new ScriptError("Error: the chunk at " + pos.toShortString() + " isn't loaded");
		return pos;
	}

	private static double number(Node node, Environment env) {
		Object value = ComScriptExpression.evaluate(node, env);
		if (value instanceof Number n)
			return n.doubleValue();
		throw ScriptError.type("expected a number, got " + ComScriptExpression.describe(value));
	}

	private static void expect(Call call, int count) {
		if (call.arguments().size() != count)
			throw new ScriptError(call.name() + "() takes " + count + " value" + (count == 1 ? "" : "s") + ", got " + call.arguments().size());
	}
}
