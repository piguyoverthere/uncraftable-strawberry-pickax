package io.github.strawberrymc.monument;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.strawberrymc.MonumentIntro;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

// Each player's monument objectives and the pings that point to them. After the entrance cutscene the objective is to
// reach the arena on the roof (pinged). Standing on its glass shows that the Prismatic Rune is locked away, and once that's
// been read comes the next objective, to retrieve the key (pinged at the gazebo vent, where it will come out once the
// puzzles exist). Pings are drawn by the client (QuestHud) through blocks.
//
// /ping places and edits extra pings for the player running it, lists them, and sets the objective, for debugging.
public class MonumentQuest {
	public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, "strawberrymc");

	public enum Stage {
		NONE(""), ARENA("Reach the arena on the roof"), KEY("Retrieve the Monument Key");

		public final String objective;

		Stage(String objective) {
			this.objective = objective;
		}
	}

	// The player's objective and the monument it belongs to
	public record State(Stage stage, BlockPos arena, BlockPos vent) {
		static final State NONE = new State(Stage.NONE, BlockPos.ZERO, BlockPos.ZERO);
		static final Codec<State> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.STRING.xmap(Stage::valueOf, Stage::name).fieldOf("stage").forGetter(State::stage),
			BlockPos.CODEC.fieldOf("arena").forGetter(State::arena),
			BlockPos.CODEC.fieldOf("vent").forGetter(State::vent)).apply(i, State::new));
	}

	public static final Supplier<AttachmentType<State>> STATE = ATTACHMENTS.register("monument_quest",
		() -> AttachmentType.builder(() -> State.NONE).serialize(State.CODEC.fieldOf("quest")).copyOnDeath().build());

	// The pink and cyan of the objective ping, and the default colours for debug pings
	public static final int OBJECTIVE_COLOR = 0x00E5FF;
	public static final int ACCENT_COLOR = 0xFF2E97;

	public record Ping(String id, Vec3 pos, int color, float size, boolean blink, String label) {
		public static final StreamCodec<RegistryFriendlyByteBuf, Ping> STREAM_CODEC = StreamCodec.of((buf, ping) -> {
			buf.writeUtf(ping.id);
			buf.writeDouble(ping.pos.x);
			buf.writeDouble(ping.pos.y);
			buf.writeDouble(ping.pos.z);
			buf.writeInt(ping.color);
			buf.writeFloat(ping.size);
			buf.writeBoolean(ping.blink);
			buf.writeUtf(ping.label);
		}, buf -> new Ping(buf.readUtf(), new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()), buf.readInt(), buf.readFloat(), buf.readBoolean(),
			buf.readUtf()));
	}

	// Everything the client shows: the objective (empty for none), whether to announce it, the pings, and a line of story
	// to show on screen (empty for none)
	public record SyncQuest(String objective, boolean announce, List<Ping> pings, String notice) implements CustomPacketPayload {
		public static final Type<SyncQuest> TYPE = new Type<>(Identifier.fromNamespaceAndPath("strawberrymc", "sync_quest"));
		public static final StreamCodec<RegistryFriendlyByteBuf, SyncQuest> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.STRING_UTF8, SyncQuest::objective,
			ByteBufCodecs.BOOL, SyncQuest::announce, Ping.STREAM_CODEC.apply(ByteBufCodecs.list()), SyncQuest::pings, ByteBufCodecs.STRING_UTF8,
			SyncQuest::notice, SyncQuest::new);

		@Override
		public Type<SyncQuest> type() {
			return TYPE;
		}
	}

	// Reaching the arena first tells the player the rune is locked away; the key objective comes after they've read it
	public static final String LOCKED_NOTICE = "Access to the Prismatic Rune seems to be locked...\nThe vault needs a key.";
	public static final int NOTICE_TICKS = 110;
	// Players reading the locked notice, between reaching the arena and getting the key objective
	private static final java.util.Set<UUID> READING = new java.util.HashSet<>();

	// Pings placed with /ping, per player; they're for debugging, so they aren't saved
	private static final Map<UUID, LinkedHashMap<String, Ping>> DEBUG_PINGS = new HashMap<>();

	public static void init(IEventBus eventBus) {
		ATTACHMENTS.register(eventBus);
		eventBus.addListener(MonumentQuest::registerPayloads);
		NeoForge.EVENT_BUS.addListener(MonumentQuest::onPlayerTick);
		NeoForge.EVENT_BUS.addListener(MonumentQuest::onLogin);
		NeoForge.EVENT_BUS.addListener(MonumentQuest::registerCommands);
	}

	private static void registerPayloads(RegisterPayloadHandlersEvent event) {
		event.registrar("2").playToClient(SyncQuest.TYPE, SyncQuest.STREAM_CODEC,
			(payload, context) -> io.github.strawberrymc.monument.client.QuestHud.receive(payload));
	}

	private static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
		if (event.getEntity() instanceof ServerPlayer player)
			sync(player, false);
	}

	private static void onPlayerTick(PlayerTickEvent.Post event) {
		if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 10 != 0 || player.isSpectator()
				|| MonumentIntro.isInCutscene(player))
			return;
		State state = player.getData(STATE);
		if (state.stage() == Stage.KEY)
			return;
		MonumentShell.Layout layout = PrismaticSensors.monumentAt(player.level(), player.blockPosition());
		if (layout == null)
			return;
		if (state.stage() == Stage.NONE)
			setStage(player, Stage.ARENA, layout);
		else if (layout.onArena(player.position()) && READING.add(player.getUUID()))
			reachArena(player, layout);
	}

	// The arena objective is done: show that the rune is locked away, then give the key objective once it's been read
	private static void reachArena(ServerPlayer player, MonumentShell.Layout layout) {
		sync(player, false, LOCKED_NOTICE);
		io.github.strawberrymc.StrawberrymcMod.queueServerWork(NOTICE_TICKS, () -> {
			if (READING.remove(player.getUUID()) && !player.isRemoved() && player.getData(STATE).stage() == Stage.ARENA)
				setStage(player, Stage.KEY, layout);
		});
	}

	public static void setStage(ServerPlayer player, Stage stage, MonumentShell.Layout layout) {
		READING.remove(player.getUUID());
		State state = layout == null ? new State(stage, player.getData(STATE).arena(), player.getData(STATE).vent())
			: new State(stage, layout.arenaCenter(), layout.ventCenter());
		player.setData(STATE, stage == Stage.NONE ? State.NONE : state);
		sync(player, stage != Stage.NONE);
	}

	public static void sync(ServerPlayer player, boolean announce) {
		sync(player, announce, "");
	}

	private static void sync(ServerPlayer player, boolean announce, String notice) {
		State state = player.getData(STATE);
		List<Ping> pings = new ArrayList<>();
		// While the locked notice is up there's no objective or ping
		boolean reading = READING.contains(player.getUUID());
		if (!reading && state.stage() == Stage.ARENA)
			pings.add(new Ping("objective", Vec3.atBottomCenterOf(state.arena()).add(0, 2.5, 0), OBJECTIVE_COLOR, 1, true, "Arena"));
		else if (!reading && state.stage() == Stage.KEY)
			pings.add(new Ping("objective", Vec3.atBottomCenterOf(state.vent()).add(0, 2.5, 0), OBJECTIVE_COLOR, 1, true, "Key"));
		pings.addAll(DEBUG_PINGS.getOrDefault(player.getUUID(), new LinkedHashMap<>()).values());
		PacketDistributor.sendToPlayer(player, new SyncQuest(reading ? "" : state.stage().objective, announce, pings, notice));
	}

	// ---- /ping ----

	private static void registerCommands(RegisterCommandsEvent event) {
		var root = Commands.literal("ping").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
		root.then(Commands.literal("add").then(Commands.argument("id", StringArgumentType.word())
			.executes(c -> add(c, c.getSource().getPosition(), ACCENT_COLOR))
			.then(Commands.argument("pos", Vec3Argument.vec3())
				.executes(c -> add(c, Vec3Argument.getVec3(c, "pos"), ACCENT_COLOR))
				.then(Commands.argument("color", StringArgumentType.greedyString())
					.executes(c -> add(c, Vec3Argument.getVec3(c, "pos"), color(c)))))));
		root.then(Commands.literal("move").then(id().then(Commands.argument("pos", Vec3Argument.vec3())
			.executes(c -> edit(c, ping -> new Ping(ping.id(), Vec3Argument.getVec3(c, "pos"), ping.color(), ping.size(), ping.blink(), ping.label()))))));
		root.then(Commands.literal("color").then(id().then(Commands.argument("color", StringArgumentType.greedyString())
			.executes(c -> {
				int color = color(c);
				return edit(c, ping -> new Ping(ping.id(), ping.pos(), color, ping.size(), ping.blink(), ping.label()));
			}))));
		root.then(Commands.literal("size").then(id().then(Commands.argument("size", FloatArgumentType.floatArg(0.25F, 4))
			.executes(c -> edit(c, ping -> new Ping(ping.id(), ping.pos(), ping.color(), FloatArgumentType.getFloat(c, "size"), ping.blink(), ping.label()))))));
		root.then(Commands.literal("blink").then(id().then(Commands.argument("blink", BoolArgumentType.bool())
			.executes(c -> edit(c, ping -> new Ping(ping.id(), ping.pos(), ping.color(), ping.size(), BoolArgumentType.getBool(c, "blink"), ping.label()))))));
		root.then(Commands.literal("label").then(id().then(Commands.argument("label", StringArgumentType.greedyString())
			.executes(c -> edit(c, ping -> new Ping(ping.id(), ping.pos(), ping.color(), ping.size(), ping.blink(), StringArgumentType.getString(c, "label")))))));
		root.then(Commands.literal("remove").then(id().executes(MonumentQuest::remove)));
		root.then(Commands.literal("clear").executes(MonumentQuest::clear));
		root.then(Commands.literal("list").executes(MonumentQuest::list));
		root.then(Commands.literal("objective")
			.then(Commands.literal("none").executes(c -> objective(c, Stage.NONE)))
			.then(Commands.literal("arena").executes(c -> objective(c, Stage.ARENA)))
			.then(Commands.literal("key").executes(c -> objective(c, Stage.KEY))));
		event.getDispatcher().register(root);
	}

	// An existing debug ping's id, suggested from the player's pings
	private static RequiredArgumentBuilder<CommandSourceStack, String> id() {
		SuggestionProvider<CommandSourceStack> ids = (context, builder) -> SharedSuggestionProvider.suggest(
			DEBUG_PINGS.getOrDefault(context.getSource().getPlayerOrException().getUUID(), new LinkedHashMap<>()).keySet(), builder);
		return Commands.argument("id", StringArgumentType.word()).suggests(ids);
	}

	private interface Edit {
		Ping apply(Ping ping) throws CommandSyntaxException;
	}

	private static LinkedHashMap<String, Ping> pings(ServerPlayer player) {
		return DEBUG_PINGS.computeIfAbsent(player.getUUID(), uuid -> new LinkedHashMap<>());
	}

	// #RRGGBB, RRGGBB, or one of a few names
	private static int color(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		String text = StringArgumentType.getString(context, "color").toLowerCase(Locale.ROOT);
		Map<String, Integer> names = Map.of("pink", ACCENT_COLOR, "cyan", OBJECTIVE_COLOR, "lime", 0x9DFF00, "gold", 0xFFC800, "violet", 0x9B5CFF,
			"orange", 0xFF7A00, "red", 0xFF3B3B, "white", 0xFFFFFF);
		if (names.containsKey(text))
			return names.get(text);
		try {
			return Integer.parseInt(text.startsWith("#") ? text.substring(1) : text, 16) & 0xFFFFFF;
		} catch (NumberFormatException e) {
			throw new com.mojang.brigadier.exceptions.SimpleCommandExceptionType(Component.literal("Unknown color " + text
				+ " (use #RRGGBB or pink, cyan, lime, gold, violet, orange, red, white)")).create();
		}
	}

	private static int add(CommandContext<CommandSourceStack> context, Vec3 pos, int color) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		String id = StringArgumentType.getString(context, "id");
		pings(player).put(id, new Ping(id, pos, color, 1, true, id));
		sync(player, false);
		context.getSource().sendSuccess(() -> Component.literal("Placed ping " + id + " at " + format(pos)), false);
		return 1;
	}

	private static int edit(CommandContext<CommandSourceStack> context, Edit edit) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		String id = StringArgumentType.getString(context, "id");
		Ping ping = pings(player).get(id);
		if (ping == null) {
			context.getSource().sendFailure(Component.literal("No ping called " + id));
			return 0;
		}
		pings(player).put(id, edit.apply(ping));
		sync(player, false);
		context.getSource().sendSuccess(() -> Component.literal("Updated ping " + id), false);
		return 1;
	}

	private static int remove(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		String id = StringArgumentType.getString(context, "id");
		if (pings(player).remove(id) == null) {
			context.getSource().sendFailure(Component.literal("No ping called " + id));
			return 0;
		}
		sync(player, false);
		context.getSource().sendSuccess(() -> Component.literal("Removed ping " + id), false);
		return 1;
	}

	private static int clear(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		int count = pings(player).size();
		pings(player).clear();
		sync(player, false);
		context.getSource().sendSuccess(() -> Component.literal("Removed " + count + " ping(s)"), false);
		return count;
	}

	private static int list(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		State state = player.getData(STATE);
		StringBuilder text = new StringBuilder("Objective: " + (state.stage() == Stage.NONE ? "none" : state.stage().objective));
		if (state.stage() != Stage.NONE) {
			Vec3 pos = Vec3.atBottomCenterOf(state.stage() == Stage.ARENA ? state.arena() : state.vent()).add(0, 2.5, 0);
			text.append(" -> ").append(format(pos)).append(" (").append((int) player.position().distanceTo(pos)).append(" blocks)");
		}
		for (Ping ping : pings(player).values())
			text.append("\n").append(ping.id()).append(": ").append(format(ping.pos())).append(" (").append((int) player.position().distanceTo(ping.pos()))
				.append(" blocks), color #").append(String.format("%06X", ping.color())).append(", size ").append(ping.size()).append(", blink ")
				.append(ping.blink()).append(", label \"").append(ping.label()).append("\"");
		context.getSource().sendSuccess(() -> Component.literal(text.toString()), false);
		return pings(player).size();
	}

	private static int objective(CommandContext<CommandSourceStack> context, Stage stage) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		MonumentShell.Layout layout = PrismaticSensors.monumentAt((ServerLevel) player.level(), player.blockPosition());
		if (layout == null && stage != Stage.NONE && player.getData(STATE).stage() == Stage.NONE) {
			context.getSource().sendFailure(Component.literal("Stand inside an ocean monument to point this objective at it"));
			return 0;
		}
		setStage(player, stage, layout);
		context.getSource().sendSuccess(() -> Component.literal("Objective set to " + (stage == Stage.NONE ? "none" : stage.objective)), false);
		return 1;
	}

	private static String format(Vec3 pos) {
		return String.format(Locale.ROOT, "%.1f %.1f %.1f", pos.x, pos.y, pos.z);
	}
}
