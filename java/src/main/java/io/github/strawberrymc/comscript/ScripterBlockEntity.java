package io.github.strawberrymc.comscript;

import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

// Holds a Scripter's script and terminal, runs the script, and keeps the *{ } and *event{ } blocks the last run registered.
// Those blocks stop when the script is run again or stopped, or the Scripter is broken or unloaded.
public class ScripterBlockEntity extends BlockEntity {
	public static final char INFO = 'I', WARNING = 'W', ERROR = 'E';
	public static final int MAX_SCRIPT_LENGTH = 32000;
	private static final int MAX_LOG_LINES = 200;
	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

	// Scripters with blocks listening for events
	private static final Set<ScripterBlockEntity> LISTENING = Collections.newSetFromMap(new WeakHashMap<>());

	public record LogLine(char level, String text) {
	}

	private record Listener(String event, List<ComScriptParser.Statement> body, CommandSourceStack source, ComScriptRunner runner) {
	}

	private String script = "";
	private final List<LogLine> log = new ArrayList<>();
	private final List<Listener> listeners = new ArrayList<>();
	private boolean powered;
	// What the server last said about listening, for the editor
	private boolean clientListening;
	private boolean needsSync;
	int currentLine = -1;

	// Collects command output for the terminal; red output is vanilla's failure style
	private final CommandSource output = new CommandSource() {
		@Override
		public void sendSystemMessage(Component message) {
			boolean failed = TextColor.fromLegacyFormat(ChatFormatting.RED).equals(message.getStyle().getColor());
			log(failed ? ERROR : INFO, currentLine, message.getString());
		}

		@Override
		public boolean acceptsSuccess() {
			return true;
		}

		@Override
		public boolean acceptsFailure() {
			return true;
		}

		@Override
		public boolean shouldInformAdmins() {
			return false;
		}
	};

	public ScripterBlockEntity(BlockPos pos, BlockState state) {
		super(ComScript.SCRIPTER_ENTITY.get(), pos, state);
	}

	public String getScript() {
		return script;
	}

	public List<LogLine> getLog() {
		return log;
	}

	public boolean isListening() {
		return level != null && level.isClientSide() ? clientListening : !listeners.isEmpty();
	}

	public void setScript(String script) {
		this.script = script.length() > MAX_SCRIPT_LENGTH ? script.substring(0, MAX_SCRIPT_LENGTH) : script;
		setChanged();
	}

	boolean isPowered() {
		return powered;
	}

	void setPowered(boolean powered) {
		this.powered = powered;
	}

	public void clearLog() {
		log.clear();
		sync();
	}

	void log(char level, int line, String text) {
		String prefix = "[" + LocalTime.now().format(TIME) + "] " + (line > 0 ? "Line " + line + ": " : "");
		for (String part : text.split("\n"))
			log.add(new LogLine(level, prefix + part));
		while (log.size() > MAX_LOG_LINES)
			log.removeFirst();
		needsSync = true;
	}

	private CommandSourceStack createSource(ServerLevel level) {
		return new CommandSourceStack(output, Vec3.atCenterOf(worldPosition), Vec2.ZERO, level, LevelBasedPermissionSet.GAMEMASTER, "Scripter",
			Component.literal("Scripter"), level.getServer(), null);
	}

	public void run() {
		if (!(level instanceof ServerLevel serverLevel))
			return;
		stop(false);
		List<ComScriptParser.Statement> statements;
		try {
			statements = ComScriptParser.parse(script);
		} catch (ComScriptParser.SyntaxError e) {
			log(ERROR, e.line, "Syntax error: " + e.getMessage());
			sync();
			return;
		}
		log(INFO, -1, "Running script");
		new ComScriptRunner(this).run(statements, createSource(serverLevel));
		if (!listeners.isEmpty())
			log(INFO, -1, "Listening for " + String.join(", ", listeners.stream().map(l -> l.event().isEmpty() ? "every tick" : l.event()).distinct().toList()));
		currentLine = -1;
		sync();
	}

	public void stop(boolean announce) {
		boolean wasListening = !listeners.isEmpty();
		listeners.clear();
		LISTENING.remove(this);
		if (announce && wasListening) {
			log(INFO, -1, "Stopped");
			sync();
		}
	}

	void listen(String event, List<ComScriptParser.Statement> body, CommandSourceStack source, ComScriptRunner runner) {
		listeners.add(new Listener(event, body, source, runner));
		LISTENING.add(this);
	}

	// Runs the blocks listening for this event, at the event's position and as its entity
	public static void fire(Level level, String event, Vec3 position, @Nullable Entity entity, Map<String, Object> details) {
		for (ScripterBlockEntity scripter : List.copyOf(LISTENING))
			if (scripter.level == level && !scripter.isRemoved())
				for (Listener listener : List.copyOf(scripter.listeners))
					if (listener.event().equals(event)) {
						CommandSourceStack source = listener.source().withPosition(position);
						if (entity != null)
							source = source.withEntity(entity);
						listener.runner().variables.putAll(details);
						listener.runner().run(listener.body(), source);
						scripter.currentLine = -1;
					}
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, ScripterBlockEntity scripter) {
		for (Listener listener : List.copyOf(scripter.listeners))
			if (listener.event().isEmpty())
				listener.runner().run(listener.body(), listener.source());
		scripter.currentLine = -1;
		// Every-tick output is sent to the client a few times a second, not every tick
		if (scripter.needsSync && level.getGameTime() % 5 == 0)
			scripter.sync();
	}

	void sync() {
		needsSync = false;
		setChanged();
		if (level != null && !level.isClientSide())
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
	}

	@Override
	public void setRemoved() {
		super.setRemoved();
		listeners.clear();
		LISTENING.remove(this);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putString("Script", script);
		output.putBoolean("Powered", powered);
		StringBuilder text = new StringBuilder();
		for (LogLine line : log)
			text.append(line.level()).append(line.text().replace('\n', ' ')).append('\n');
		output.putString("Log", text.toString());
		output.putBoolean("Listening", !listeners.isEmpty());
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		script = input.getStringOr("Script", "");
		powered = input.getBooleanOr("Powered", false);
		clientListening = input.getBooleanOr("Listening", false);
		log.clear();
		for (String line : input.getStringOr("Log", "").split("\n"))
			if (!line.isEmpty())
				log.add(new LogLine(line.charAt(0), line.substring(1)));
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return saveCustomOnly(registries);
	}

	@Override
	public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}
}
