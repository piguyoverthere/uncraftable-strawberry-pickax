package io.github.strawberrymc.comscript;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

// ComScript values and the expressions that compute them. Values are Long (integer), Double (float), String or Boolean.
public final class ComScriptExpression {
	private ComScriptExpression() {
	}

	public static class ExpressionError extends RuntimeException {
		public ExpressionError(String message) {
			super(message);
		}
	}

	// Runtime errors, named like the wiki's error list
	public static class ScriptError extends RuntimeException {
		public ScriptError(String message) {
			super(message);
		}

		static ScriptError undefined(String name) {
			return new ScriptError("Error: Undefined data structure(s): " + name);
		}

		static ScriptError type(String detail) {
			return new ScriptError("Error: Unexpected data type in input: " + detail);
		}
	}

	// A text style stored with store new font(<name>) as {...}: the text component JSON /tellraw takes
	public record TextStyle(String json) {
		@Override
		public String toString() {
			return json;
		}
	}

	// An entity selector like @e[type=pig,limit=1], kept as written until a fetcher looks it up
	public record Selector(String text) {
		@Override
		public String toString() {
			return text;
		}
	}

	public interface Environment {
		Object variable(String name);

		net.minecraft.commands.CommandSourceStack source();

		ServerLevel level();

		RandomSource random();
	}

	public sealed interface Node permits Literal, Variable, Unary, Binary, Call {
	}

	public record Literal(Object value) implements Node {
	}

	public record Variable(String name) implements Node {
	}

	public record Unary(String operator, Node operand) implements Node {
	}

	public record Binary(String operator, Node left, Node right) implements Node {
	}

	public record Call(String name, List<Node> arguments, List<String> raw) implements Node {
	}

	// ---- Parsing ----

	public static Node parse(String text) {
		Parser parser = new Parser(tokenize(text));
		if (parser.tokens.isEmpty())
			throw new ExpressionError("Expected a value");
		Node node = parser.comparison();
		if (parser.position < parser.tokens.size())
			throw new ExpressionError("Unexpected " + parser.tokens.get(parser.position));
		return node;
	}

	private static List<String> tokenize(String text) {
		List<String> tokens = new ArrayList<>();
		int i = 0;
		while (i < text.length()) {
			char c = text.charAt(i);
			String literal = ComScriptParser.booleanLiteralAt(text, i);
			if (literal != null) {
				tokens.add(literal);
				i += literal.length();
			} else if (Character.isWhitespace(c)) {
				i++;
			} else if (c == '@') {
				// Selectors keep their [ ] arguments
				int end = i + 1;
				while (end < text.length() && (Character.isLetterOrDigit(text.charAt(end)) || text.charAt(end) == '.' || text.charAt(end) == '_'))
					end++;
				if (end < text.length() && text.charAt(end) == '[') {
					int depth = 0;
					while (end < text.length()) {
						char d = text.charAt(end++);
						if (d == '[')
							depth++;
						else if (d == ']' && --depth == 0)
							break;
					}
				}
				tokens.add(text.substring(i, end));
				i = end;
			} else if (ComScriptParser.isQuote(c) || c == '\'') {
				int end = ComScriptParser.stringEnd(text, i);
				tokens.add(text.substring(i, end));
				i = end;
			} else if (Character.isDigit(c) || c == '.' && i + 1 < text.length() && Character.isDigit(text.charAt(i + 1))) {
				int end = i;
				while (end < text.length() && (Character.isDigit(text.charAt(end)) || text.charAt(end) == '.'))
					end++;
				tokens.add(text.substring(i, end));
				i = end;
			} else if (Character.isLetter(c) || c == '_' || c == '$') {
				int end = i + 1;
				while (end < text.length() && (Character.isLetterOrDigit(text.charAt(end)) || text.charAt(end) == '_' || text.charAt(end) == '.'))
					end++;
				// sin^, cos^, log^ and friends are the inverse functions
				if (end < text.length() && text.charAt(end) == '^' && c != '$' && end + 1 < text.length() && text.charAt(end + 1) == '(')
					end++;
				tokens.add(text.substring(i, end));
				i = end;
			} else {
				String two = i + 1 < text.length() ? text.substring(i, i + 2) : "";
				if (two.equals("/%") || two.equals("==") || two.equals("!=") || two.equals("<=") || two.equals(">=")) {
					tokens.add(two);
					i += 2;
				} else if ("+-*/^(),<>?=".indexOf(c) >= 0) {
					tokens.add(String.valueOf(c));
					i++;
				} else {
					throw new ExpressionError("Unexpected character '" + c + "'");
				}
			}
		}
		return tokens;
	}

	private static class Parser {
		final List<String> tokens;
		int position;

		Parser(List<String> tokens) {
			this.tokens = tokens;
		}

		String peek() {
			return position < tokens.size() ? tokens.get(position) : null;
		}

		boolean accept(String token) {
			if (token.equals(peek())) {
				position++;
				return true;
			}
			return false;
		}

		Node comparison() {
			Node left = additive();
			String op = peek();
			if (op != null && ComScriptSyntax.COMPARISONS.contains(op)) {
				position++;
				return new Binary(op, left, additive());
			}
			return left;
		}

		Node additive() {
			Node node = multiplicative();
			while ("+".equals(peek()) || "-".equals(peek())) {
				String op = tokens.get(position++);
				node = new Binary(op, node, multiplicative());
			}
			return node;
		}

		Node multiplicative() {
			Node node = power();
			while ("*".equals(peek()) || "/".equals(peek()) || "/%".equals(peek()) || "mod".equals(peek())) {
				String op = tokens.get(position++);
				node = new Binary(op.equals("mod") ? "/%" : op, node, power());
			}
			return node;
		}

		Node power() {
			Node base = unary();
			if (accept("^"))
				return new Binary("^", base, power());
			return base;
		}

		Node unary() {
			if (accept("-"))
				return new Unary("-", unary());
			Node node = primary();
			// The ? after Online(name)? and LoadChunk(x,y,z)? just marks a yes/no question
			accept("?");
			return node;
		}

		Node primary() {
			String token = peek();
			if (token == null)
				throw new ExpressionError("Expected a value");
			position++;
			if (token.equals("(")) {
				Node inner = comparison();
				if (!accept(")"))
					throw new ExpressionError("Missing )");
				return inner;
			}
			if (Character.isDigit(token.charAt(0)) || token.charAt(0) == '.') {
				try {
					return token.contains(".") ? new Literal(Double.parseDouble(token)) : new Literal(Long.parseLong(token));
				} catch (NumberFormatException e) {
					throw new ExpressionError("Invalid number " + token);
				}
			}
			if (ComScriptParser.isQuote(token.charAt(0)) || token.charAt(0) == '\'') {
				String inner = token.length() >= 2 ? token.substring(1, token.length() - 1) : "";
				return new Literal(ComScriptSyntax.substituteTokens(inner));
			}
			if (token.equals("<true;>") || token.equals("<false;>"))
				return new Literal(token.equals("<true;>"));
			if (token.equals("true") || token.equals("false"))
				throw new ExpressionError("Write booleans as <" + token + ";>");
			if (token.startsWith("@"))
				return new Literal(new Selector(token));
			if (token.startsWith("$"))
				return new Variable(token.substring(1));
			if ("(".equals(peek()) && Character.isLetter(token.charAt(0))) {
				if (!ComScriptSyntax.isFunction(token))
					throw new ExpressionError("Unknown function " + token + "()");
				position++;
				List<Node> arguments = new ArrayList<>();
				List<String> raw = new ArrayList<>();
				if (!accept(")")) {
					do {
						int start = position;
						arguments.add(comparison());
						raw.add(String.join("", tokens.subList(start, position)));
					} while (accept(","));
					if (!accept(")"))
						throw new ExpressionError("Missing ) after " + token + "(");
				}
				return new Call(token.toLowerCase(), arguments, raw);
			}
			if (Character.isLetter(token.charAt(0)) || token.charAt(0) == '_')
				return new Variable(token);
			throw new ExpressionError("Unexpected " + token);
		}
	}

	// ---- Evaluation ----

	public static Object evaluate(Node node, Environment env) {
		return switch (node) {
			case Literal literal -> literal.value();
			case Variable variable -> {
				Object value = env.variable(variable.name());
				// isDownfall and other fetchers that need no brackets
				if (value == null && ComScriptFetchers.BARE.contains(variable.name().toLowerCase()))
					value = ComScriptFetchers.bare(variable.name(), env);
				if (value == null)
					throw ScriptError.undefined(variable.name());
				yield value;
			}
			case Unary unary -> {
				Object value = evaluate(unary.operand(), env);
				if (value instanceof Long l)
					yield -l;
				if (value instanceof Double d)
					yield -d;
				throw ScriptError.type("- needs a number, got " + describe(value));
			}
			case Binary binary -> binary(binary.operator(), evaluate(binary.left(), env), evaluate(binary.right(), env));
			case Call call -> call(call, env);
		};
	}

	public static Object binary(String op, Object a, Object b) {
		if (ComScriptSyntax.COMPARISONS.contains(op))
			return compare(op, a, b);
		if (op.equals("+") && (a instanceof String || b instanceof String))
			return format(a) + format(b);
		if (!(a instanceof Number) || !(b instanceof Number))
			throw ScriptError.type(op + " needs numbers, got " + describe(a) + " and " + describe(b));
		boolean integers = a instanceof Long && b instanceof Long;
		double x = ((Number) a).doubleValue();
		double y = ((Number) b).doubleValue();
		switch (op) {
			case "+":
				return integers ? (Object) ((Long) a + (Long) b) : x + y;
			case "-":
				return integers ? (Object) ((Long) a - (Long) b) : x - y;
			case "*":
				return integers ? (Object) ((Long) a * (Long) b) : x * y;
			case "/":
				// Remainderless division; dividing by 0 gives 0
				if (y == 0)
					return 0L;
				return integers ? (Object) Math.floorDiv((Long) a, (Long) b) : x / y;
			case "/%":
				if (y == 0)
					return 0L;
				return integers ? (Object) Math.floorMod((Long) a, (Long) b) : x - Math.floor(x / y) * y;
			case "^":
				double result = Math.pow(x, y);
				return integers && y >= 0 && Math.abs(result) < 9.0E18 ? (Object) (long) result : result;
			default:
				throw new ScriptError("Unknown operator " + op);
		}
	}

	private static Boolean compare(String op, Object a, Object b) {
		if (op.equals("==") || op.equals("=") || op.equals("!=")) {
			boolean equal = a instanceof Number x && b instanceof Number y ? x.doubleValue() == y.doubleValue() : a.equals(b);
			return op.equals("!=") != equal;
		}
		int order;
		if (a instanceof Number x && b instanceof Number y)
			order = Double.compare(x.doubleValue(), y.doubleValue());
		else if (a instanceof String x && b instanceof String y)
			order = x.compareTo(y);
		else
			throw ScriptError.type(op + " can't compare " + describe(a) + " with " + describe(b));
		return switch (op) {
			case "<" -> order < 0;
			case ">" -> order > 0;
			case "<=" -> order <= 0;
			default -> order >= 0;
		};
	}

	private static Object call(Call call, Environment env) {
		String name = call.name();
		List<Node> args = call.arguments();
		if (ComScriptFetchers.NAMES.contains(name))
			return ComScriptFetchers.call(call, env);
		// Online(Steve)? can name a player without quotes
		if (name.equals("online")) {
			expectArguments(call, 1);
			Object player = args.getFirst() instanceof Variable v && env.variable(v.name()) == null ? v.name() : evaluate(args.getFirst(), env);
			return env.level().getServer().getPlayerList().getPlayerByName(format(player)) != null;
		}
		List<Object> values = new ArrayList<>();
		for (Node arg : args)
			values.add(evaluate(arg, env));
		if (ComScriptSyntax.isRandomFloat(name)) {
			expectArguments(call, 2);
			int places = Integer.parseInt(name.split("\\.")[1]);
			double min = number(values.get(0)), max = number(values.get(1));
			double value = min + env.random().nextDouble() * (max - min);
			double scale = Math.pow(10, places);
			return Math.round(value * scale) / scale;
		}
		switch (name) {
			// font(<style>, <text>) is the style with different text
			case "font" -> {
				expectArguments(call, 2);
				if (!(values.get(0) instanceof TextStyle style))
					throw ScriptError.type("font() needs a style stored with store new font(<name>) as {...}, got " + describe(values.get(0)));
				com.google.gson.JsonElement json = com.google.gson.JsonParser.parseString(style.json());
				com.google.gson.JsonObject object = json.isJsonObject() ? json.getAsJsonObject().deepCopy() : new com.google.gson.JsonObject();
				object.addProperty("text", format(values.get(1)));
				return new TextStyle(object.toString());
			}
			case "string", "str" -> {
				expectArguments(call, 1);
				return format(values.getFirst());
			}
			case "boolean", "bool", "booleanstate" -> {
				expectArguments(call, 1);
				return toBoolean(values.getFirst());
			}
			case "bin", "createbin" -> {
				expectArguments(call, 1);
				return toBinary(values.getFirst(), call.raw().getFirst());
			}
			case "float", "flt" -> {
				expectArguments(call, 1);
				return toFloat(values.getFirst());
			}
			case "integer", "int" -> {
				expectArguments(call, 1);
				return toInteger(values.getFirst());
			}
			case "log" -> {
				return Math.log10(numberArgument(call, values));
			}
			case "log^" -> {
				return Math.pow(10, numberArgument(call, values));
			}
			// Angles are in degrees, like Minecraft's rotations
			case "cos", "cosine" -> {
				return Math.cos(Math.toRadians(numberArgument(call, values)));
			}
			case "sin", "sine" -> {
				return Math.sin(Math.toRadians(numberArgument(call, values)));
			}
			case "tan", "tangent" -> {
				return Math.tan(Math.toRadians(numberArgument(call, values)));
			}
			case "cos^", "cosine^" -> {
				return Math.toDegrees(Math.acos(numberArgument(call, values)));
			}
			case "sin^", "sine^" -> {
				return Math.toDegrees(Math.asin(numberArgument(call, values)));
			}
			case "tan^", "tangent^" -> {
				return Math.toDegrees(Math.atan(numberArgument(call, values)));
			}
			case "sqrt", "squareroot", "sqaureroot" -> {
				return Math.sqrt(numberArgument(call, values));
			}
			case "fact", "factorial" -> {
				long n = (long) numberArgument(call, values);
				if (n < 0 || n > 20)
					throw ScriptError.type("factorial needs a whole number from 0 to 20");
				long result = 1;
				for (long i = 2; i <= n; i++)
					result *= i;
				return result;
			}
			case "random.randint", "random.n.randint" -> {
				expectArguments(call, 2);
				long min = (long) number(values.get(0)), max = (long) number(values.get(1));
				// random.n.randint leaves the maximum out
				if (name.equals("random.n.randint"))
					max--;
				if (max < min)
					throw ScriptError.type(name + " needs a maximum above its minimum");
				return min + (long) Math.floor(env.random().nextDouble() * (max - min + 1));
			}
			case "loadchunk" -> {
				expectArguments(call, 3);
				BlockPos pos = BlockPos.containing(number(values.get(0)), number(values.get(1)), number(values.get(2)));
				return env.level().isLoaded(pos);
			}
			default -> throw new ScriptError("Unknown function " + name + "()");
		}
	}

	private static void expectArguments(Call call, int count) {
		if (call.arguments().size() != count)
			throw new ScriptError(call.name() + "() takes " + count + " value" + (count == 1 ? "" : "s") + ", got " + call.arguments().size());
	}

	private static double numberArgument(Call call, List<Object> values) {
		expectArguments(call, 1);
		return number(values.getFirst());
	}

	private static double number(Object value) {
		if (value instanceof Number n)
			return n.doubleValue();
		throw ScriptError.type("expected a number, got " + describe(value));
	}

	public static Boolean toBoolean(Object value) {
		if (value instanceof Boolean b)
			return b;
		if (value instanceof Long l && (l == 0 || l == 1))
			return l == 1;
		if (value instanceof String s && (s.equalsIgnoreCase("true") || s.equalsIgnoreCase("false")))
			return Boolean.parseBoolean(s.toLowerCase());
		throw ScriptError.type("only 0, 1, \"true\" and \"false\" convert to a boolean, got " + format(value));
	}

	public static Double toFloat(Object value) {
		if (value instanceof Number n)
			return n.doubleValue();
		if (value instanceof Boolean b)
			return b ? 1.0 : 0.0;
		try {
			return Double.parseDouble(((String) value).trim());
		} catch (NumberFormatException e) {
			throw ScriptError.type("\"" + value + "\" isn't a number");
		}
	}

	public static Long toInteger(Object value) {
		if (value instanceof Long l)
			return l;
		if (value instanceof Double d)
			return (long) (double) d;
		if (value instanceof Boolean b)
			return b ? 1L : 0L;
		String text = ((String) value).trim();
		try {
			return text.matches("[01]+") && text.length() > 1 ? Long.parseLong(text, 2) : (long) Double.parseDouble(text);
		} catch (NumberFormatException e) {
			throw ScriptError.type("\"" + value + "\" isn't a number");
		}
	}

	// Binary is the raw data: numbers in base 2, text as the bits of its UTF-8 bytes. A literal like bin(0101) is kept as written
	private static String toBinary(Object value, String raw) {
		if (raw.matches("[01]+"))
			return raw;
		if (value instanceof Long l)
			return Long.toBinaryString(l);
		if (value instanceof Boolean b)
			return b ? "1" : "0";
		StringBuilder bits = new StringBuilder();
		for (byte b : format(value).getBytes(StandardCharsets.UTF_8))
			bits.append(String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0'));
		return bits.toString();
	}

	// How a value appears when it's put into a command or the terminal
	public static String format(Object value) {
		if (value instanceof Double d) {
			if (Double.isNaN(d) || Double.isInfinite(d))
				return Double.toString(d);
			// Rounded to 10 decimal places so sin(30) shows as 0.5
			java.math.BigDecimal rounded = java.math.BigDecimal.valueOf(d).setScale(10, java.math.RoundingMode.HALF_UP).stripTrailingZeros();
			return rounded.scale() <= 0 ? rounded.toBigInteger().toString() : rounded.toPlainString();
		}
		return String.valueOf(value);
	}

	public static String describe(Object value) {
		if (value instanceof Long)
			return "integer";
		if (value instanceof Double)
			return "float";
		if (value instanceof Boolean)
			return "boolean";
		if (value instanceof TextStyle)
			return "style";
		if (value instanceof Selector)
			return "selector";
		return "string";
	}

	// Numbers count as one kind, so integers and floats can be mixed without converting
	public static boolean sameKind(Object a, Object b) {
		return a instanceof Number && b instanceof Number || a.getClass() == b.getClass();
	}
}
