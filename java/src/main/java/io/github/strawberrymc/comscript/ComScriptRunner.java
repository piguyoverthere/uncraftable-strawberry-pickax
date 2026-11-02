package io.github.strawberrymc.comscript;

import io.github.strawberrymc.comscript.ComScriptExpression.ScriptError;
import io.github.strawberrymc.comscript.ComScriptParser.Statement;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Runs parsed ComScript for a Scripter. One runner is one run of the script: its variables are kept for the *{ } and
// *event{ } blocks the run registers, so they can keep counting between ticks and events.
public class ComScriptRunner implements ComScriptExpression.Environment {
	// How many statements one run (or one tick or event) may execute, so a script can't freeze the server
	private static final int STATEMENT_LIMIT = 10000;
	private static final Pattern VARIABLE = Pattern.compile("\\$([A-Za-z_][A-Za-z0-9_]*)");

	// execute ... run { block } runs vanilla's execute up to a callback command that records each context it reaches, then
	// runs the block in each of them. The block can't run inside the callback, because commands run there would be queued
	// until after the whole execute finished instead of running in order with the ComScript statements around them
	private static final Map<Integer, List<CommandSourceStack>> CALLBACKS = new HashMap<>();
	private static int nextCallback;

	private final ScripterBlockEntity scripter;
	final Map<String, Object> variables = new HashMap<>();
	private int budget;

	private static class StopRun extends RuntimeException {
		StopRun() {
			super(null, null, false, false);
		}
	}

	ComScriptRunner(ScripterBlockEntity scripter) {
		this.scripter = scripter;
	}

	public static void callback(int id, CommandSourceStack source) {
		List<CommandSourceStack> sources = CALLBACKS.get(id);
		if (sources != null)
			sources.add(source);
	}

	// Runs statements as one run, tick or event, with its own statement budget
	void run(List<Statement> statements, CommandSourceStack source) {
		budget = STATEMENT_LIMIT;
		try {
			runBody(statements, source);
		} catch (StopRun ignored) {
			scripter.log(ScripterBlockEntity.ERROR, -1, "Stopped: more than " + STATEMENT_LIMIT + " statements ran at once");
		}
	}

	private void runBody(List<Statement> statements, CommandSourceStack source) {
		for (Statement statement : statements) {
			if (--budget < 0)
				throw new StopRun();
			scripter.currentLine = statement.line();
			try {
				source = execute(statement, source);
			} catch (ScriptError e) {
				scripter.log(ScripterBlockEntity.ERROR, statement.line(), e.getMessage());
			} catch (StopRun e) {
				throw e;
			} catch (RuntimeException e) {
				scripter.log(ScripterBlockEntity.ERROR, statement.line(), "Error: " + e);
			}
		}
	}

	// Runs one statement; returns the source later statements use, which mark changes
	private CommandSourceStack execute(Statement statement, CommandSourceStack source) {
		switch (statement) {
			case ComScriptParser.Command command -> runCommand(fill(command.text()), source);
			case ComScriptParser.Store store -> store(store);
			case ComScriptParser.Mark mark -> {
				String target = "positioned " + fill(mark.position());
				if (mark.selector() != null)
					target = "at " + fill(mark.selector()) + " " + target;
				List<CommandSourceStack> found = contexts(target, source);
				if (found.isEmpty())
					throw new ScriptError("Error: No mandatory execution marker found: nothing matched " + mark.selector());
				return source.withPosition(found.getFirst().getPosition());
			}
			case ComScriptParser.Execute execute -> {
				for (ComScriptParser.Comparison comparison : execute.comparisons()) {
					Object left = ComScriptExpression.evaluate(comparison.left(), this);
					Object right = ComScriptExpression.evaluate(comparison.right(), this);
					if ((Boolean) ComScriptExpression.binary(comparison.operator(), left, right) == comparison.negate())
						return source;
				}
				if (execute.subcommands().isEmpty())
					runBody(execute.body(), source);
				else
					for (CommandSourceStack context : contexts(fill(execute.subcommands()), source))
						runBody(execute.body(), context);
			}
			case ComScriptParser.EventBlock block -> scripter.listen(block.event(), block.body(), source, this);
		}
		return source;
	}

	// Every context vanilla's execute subcommands reach from source
	private List<CommandSourceStack> contexts(String subcommands, CommandSourceStack source) {
		int id = nextCallback++;
		List<CommandSourceStack> found = new ArrayList<>();
		CALLBACKS.put(id, found);
		try {
			runCommand("execute " + subcommands + " run " + ComScript.CALLBACK_COMMAND + " " + id, source);
		} finally {
			CALLBACKS.remove(id);
		}
		return found;
	}

	private void runCommand(String command, CommandSourceStack source) {
		source.getServer().getCommands().performPrefixedCommand(source, command);
	}

	private void store(ComScriptParser.Store store) {
		switch (store.op()) {
			case NEW -> {
				Object value = ComScriptExpression.evaluate(store.value(), this);
				for (String name : store.names()) {
					if (variables.containsKey(name))
						scripter.log(ScripterBlockEntity.WARNING, store.line(), "Warning: " + name + " already exists and was replaced");
					variables.put(name, value);
				}
			}
			case AS -> variables.put(store.names().getFirst(), ComScriptExpression.evaluate(store.value(), this));
			case FONT -> variables.put(store.names().getFirst(), style(fill(store.style())));
			default -> {
				String name = store.names().getFirst();
				Object current = variables.get(name);
				if (current == null)
					throw ScriptError.undefined(name);
				Object updated = switch (store.op()) {
					case ASSIGN -> {
						Object value = ComScriptExpression.evaluate(store.value(), this);
						// Changing a variable's type needs a conversion like int() or str()
						boolean converted = store.value() instanceof ComScriptExpression.Call call && ComScriptSyntax.CONVERSIONS.contains(call.name());
						if (!converted && !ComScriptExpression.sameKind(current, value))
							throw ScriptError.type(name + " is " + article(ComScriptExpression.describe(current)) + "; use a conversion like "
								+ conversionFor(value) + "(...) to store " + article(ComScriptExpression.describe(value)));
						yield value;
					}
					case INCREMENT, DECREMENT -> {
						if (!(current instanceof Number))
							throw ScriptError.type(name + " is " + article(ComScriptExpression.describe(current)) + ", and ++ and -- only work on integers and floats");
						yield ComScriptExpression.binary(store.op() == ComScriptParser.StoreOp.INCREMENT ? "+" : "-", current, 1L);
					}
					default -> ComScriptExpression.binary(switch (store.op()) {
						case ADD -> "+";
						case SUBTRACT -> "-";
						case MULTIPLY -> "*";
						case DIVIDE -> "/";
						default -> "/%";
					}, current, ComScriptExpression.evaluate(store.value(), this));
				};
				variables.put(name, updated);
			}
		}
	}

	// Checks the JSON is a text component /tellraw would accept, then keeps it as written
	private ComScriptExpression.TextStyle style(String json) {
		com.google.gson.JsonElement parsed;
		try {
			parsed = com.google.gson.JsonParser.parseString(json);
		} catch (RuntimeException e) {
			throw ScriptError.type("the style isn't valid JSON: " + json);
		}
		var result = net.minecraft.network.chat.ComponentSerialization.CODEC.parse(
			level().registryAccess().createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE), parsed);
		if (result.error().isPresent())
			throw ScriptError.type("the style isn't a valid text component: " + result.error().get().message());
		return new ComScriptExpression.TextStyle(parsed.toString());
	}

	private static String article(String word) {
		return ("aeiou".indexOf(word.charAt(0)) >= 0 ? "an " : "a ") + word;
	}

	private static String conversionFor(Object value) {
		return value instanceof Long ? "int" : value instanceof Double ? "float" : value instanceof Boolean ? "bool" : "str";
	}

	// Fills in $variables, then turns tokens like &dollar; into their characters
	private String fill(String text) {
		Matcher matcher = VARIABLE.matcher(text);
		StringBuilder out = new StringBuilder();
		while (matcher.find()) {
			Object value = variables.get(matcher.group(1));
			if (value == null)
				throw ScriptError.undefined("$" + matcher.group(1));
			matcher.appendReplacement(out, Matcher.quoteReplacement(ComScriptExpression.format(value)));
		}
		matcher.appendTail(out);
		return ComScriptSyntax.substituteTokens(out.toString());
	}

	@Override
	public Object variable(String name) {
		return variables.get(name);
	}

	@Override
	public ServerLevel level() {
		return (ServerLevel) scripter.getLevel();
	}

	@Override
	public RandomSource random() {
		return scripter.getLevel().getRandom();
	}
}
