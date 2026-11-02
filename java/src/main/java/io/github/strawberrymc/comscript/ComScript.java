package io.github.strawberrymc.comscript;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import io.netty.buffer.ByteBuf;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.GameMasterBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.Map;

// Registers the Scripter, the ComScript block (see the wiki's ComScript section). Its block model, texture, editor font and
// names live in the built-in comscript resource pack (src/main/resources/comscript), so MCreator can't overwrite them.
public class ComScript {
	public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks("strawberrymc");
	public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems("strawberrymc");
	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, "strawberrymc");

	// Unbreakable in survival and drops nothing, like a command block
	public static final DeferredBlock<ScripterBlock> SCRIPTER = BLOCKS.registerBlock("scripter", ScripterBlock::new,
		() -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).requiresCorrectToolForDrops().strength(-1.0F, 3600000.0F).noLootTable());
	public static final DeferredItem<Item> SCRIPTER_ITEM = ITEMS.registerItem("scripter", properties -> new GameMasterBlockItem(SCRIPTER.get(), properties),
		() -> new Item.Properties().rarity(Rarity.EPIC).useBlockDescriptionPrefix());
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ScripterBlockEntity>> SCRIPTER_ENTITY = BLOCK_ENTITIES.register("scripter",
		() -> new BlockEntityType<>(ScripterBlockEntity::new, SCRIPTER.get()));

	// Internal command used by execute ... run { block }; see ComScriptRunner
	static final String CALLBACK_COMMAND = "comscript_callback";

	public enum Action {
		SAVE, RUN, STOP, CLEAR_TERMINAL
	}

	// Sent by the editor: the edited script and what to do with it
	public record EditScripter(BlockPos pos, String script, int action) implements CustomPacketPayload {
		public static final Type<EditScripter> TYPE = new Type<>(Identifier.fromNamespaceAndPath("strawberrymc", "edit_scripter"));
		public static final StreamCodec<ByteBuf, EditScripter> STREAM_CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC, EditScripter::pos,
			ByteBufCodecs.stringUtf8(ScripterBlockEntity.MAX_SCRIPT_LENGTH), EditScripter::script, ByteBufCodecs.VAR_INT, EditScripter::action,
			EditScripter::new);

		@Override
		public Type<EditScripter> type() {
			return TYPE;
		}
	}

	public static void init(IEventBus eventBus) {
		BLOCKS.register(eventBus);
		ITEMS.register(eventBus);
		BLOCK_ENTITIES.register(eventBus);
		eventBus.addListener(ComScript::registerPayloads);
		eventBus.addListener(ComScript::addPack);
		eventBus.addListener(ComScript::addToCreativeTab);
		NeoForge.EVENT_BUS.addListener(ComScript::registerCommands);
		NeoForge.EVENT_BUS.addListener(ComScript::onBlockBreak);
		NeoForge.EVENT_BUS.addListener(ComScript::onBlockPlace);
		NeoForge.EVENT_BUS.addListener(ComScript::onPlayerJoin);
		NeoForge.EVENT_BUS.addListener(ComScript::onDeath);
	}

	private static void registerPayloads(RegisterPayloadHandlersEvent event) {
		event.registrar("1").playToServer(EditScripter.TYPE, EditScripter.STREAM_CODEC, (payload, context) -> {
			if (!(context.player() instanceof ServerPlayer player) || !player.canUseGameMasterBlocks())
				return;
			if (!player.level().isLoaded(payload.pos()) || !(player.level().getBlockEntity(payload.pos()) instanceof ScripterBlockEntity scripter))
				return;
			Action action = Action.values()[Math.floorMod(payload.action(), Action.values().length)];
			if (action != Action.CLEAR_TERMINAL)
				scripter.setScript(payload.script());
			switch (action) {
				case SAVE -> scripter.sync();
				case RUN -> scripter.run();
				case STOP -> scripter.stop(true);
				case CLEAR_TERMINAL -> scripter.clearLog();
			}
		});
	}

	private static void addPack(AddPackFindersEvent event) {
		event.addPackFinders(Identifier.fromNamespaceAndPath("strawberrymc", "comscript"), PackType.CLIENT_RESOURCES, Component.literal("Strawberry MC ComScript"),
			PackSource.BUILT_IN, true, Pack.Position.TOP);
	}

	private static void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
		if (event.getTabKey() == CreativeModeTabs.OP_BLOCKS && event.hasPermissions())
			event.accept(SCRIPTER_ITEM.get());
	}

	private static void registerCommands(RegisterCommandsEvent event) {
		event.getDispatcher().register(Commands.literal(CALLBACK_COMMAND)
			.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
			.then(Commands.argument("id", IntegerArgumentType.integer(0)).executes(context -> {
				ComScriptRunner.callback(IntegerArgumentType.getInteger(context, "id"), context.getSource());
				return 1;
			})));
	}

	// Event blocks see where the event happened as $event_x, $event_y and $event_z, plus $event_block or $event_entity
	private static Map<String, Object> details(BlockPos pos, String key, String value) {
		return Map.of("event_x", (long) pos.getX(), "event_y", (long) pos.getY(), "event_z", (long) pos.getZ(), key, value);
	}

	private static void onBlockBreak(BreakBlockEvent event) {
		if (event.getLevel() instanceof net.minecraft.world.level.Level level && !level.isClientSide())
			ScripterBlockEntity.fire(level, "blockEvent.blockBreak", Vec3.atCenterOf(event.getPos()), event.getPlayer(),
				details(event.getPos(), "event_block", BuiltInRegistries.BLOCK.getKey(event.getState().getBlock()).toString()));
	}

	private static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
		if (event.getLevel() instanceof net.minecraft.world.level.Level level && !level.isClientSide())
			ScripterBlockEntity.fire(level, "blockEvent.blockPlace", Vec3.atCenterOf(event.getPos()), event.getEntity(),
				details(event.getPos(), "event_block", BuiltInRegistries.BLOCK.getKey(event.getPlacedBlock().getBlock()).toString()));
	}

	private static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
		Entity player = event.getEntity();
		for (net.minecraft.server.level.ServerLevel level : player.level().getServer().getAllLevels())
			ScripterBlockEntity.fire(level, "playerEvent.join", player.position(), player, details(player.blockPosition(), "event_entity", player.getName().getString()));
	}

	private static void onDeath(LivingDeathEvent event) {
		Entity entity = event.getEntity();
		if (!entity.level().isClientSide())
			ScripterBlockEntity.fire(entity.level(), "entityEvent.death", entity.position(), entity,
				details(entity.blockPosition(), "event_entity", entity.getName().getString()));
	}
}
