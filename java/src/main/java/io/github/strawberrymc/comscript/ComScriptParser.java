package io.github.strawberrymc.comscript;

import java.util.ArrayList;
import java.util.List;

// Turns ComScript source into statements. Every statement ends with a semicolon, except run { ... } and *event{ ... }
// blocks, which end at their closing brace; brackets keep blocks and multi-line NBT together. Comments are // to the end of the line, or
// // "..." across lines.
public final class ComScriptParser {
	private ComScriptParser() {
	}

	public static class SyntaxError extends RuntimeException {
		public final int line;

		public SyntaxError(int line, String message) {
			super(message);
			this.line = line;
		}
	}

	public sealed interface Statement permits Command, Store, Mark, Execute, EventBlock {
		int line();
	}

	// A Minecraft command, run as written after $variables are filled in
	public record Command(int line, String text) implements Statement {
	}

	public enum StoreOp {
		NEW, ASSIGN, AS, INCREMENT, DECREMENT, ADD, SUBTRACT, MULTIPLY, DIVIDE, REMAINDER, FONT
	}

	// For FONT, style is the raw text component JSON and value is null
	public record Store(int line, StoreOp op, List<String> names, ComScriptExpression.Node value, String style) implements Statement {
		Store(int line, StoreOp op, List<String> names, ComScriptExpression.Node value) {
			this(line, op, names, value, null);
		}
	}

	// mark add <x y z> or mark add relative <selector> <~ ~ ~>; selector is null for a fixed position
	public record Mark(int line, String selector, String position) implements Statement {
	}

	public record Comparison(boolean negate, ComScriptExpression.Node left, String operator, ComScriptExpression.Node right) {
	}

	// An execute statement: the vanilla subcommands, ComScript's if comp checks, and what it runs
	public record Execute(int line, String subcommands, List<Comparison> comparisons, List<Statement> body) implements Statement {
	}

	// *{ } runs every tick, *N{ } every N + 1 ticks, *<event>{ } when the event happens
	public record EventBlock(int line, String event, List<Statement> body) implements Statement {
	}

	private record Piece(int line, String text) {
	}

	private static final java.util.regex.Pattern OLD_RUN = java.util.regex.Pattern.compile("(?s).*\\brun\\s*\\(.*\\)");
	private static final java.util.regex.Pattern BLOCK_RUN = java.util.regex.Pattern.compile("(?s).*\\brun\\s*\\{.*\\}");

	public static List<Statement> parse(String source) {
		return parse(source, 1);
	}

	private static List<Statement> parse(String source, int firstLine) {
		List<Statement> statements = new ArrayList<>();
		for (Piece piece : split(source, firstLine))
			statements.add(statement(piece.line, piece.text));
		return statements;
	}

	// Splits source into statements, dropping comments
	private static List<Piece> split(String source, int firstLine) {
		List<Piece> pieces = new ArrayList<>();
		StringBuilder current = new StringBuilder();
		int line = firstLine;
		int start = -1;
		int depth = 0;
		int i = 0;
		while (i < source.length()) {
			char c = source.charAt(i);
			if (c == '/' && i + 1 < source.length() && source.charAt(i + 1) == '/') {
				int j = i + 2;
				while (j < source.length() && source.charAt(j) == ' ')
					j++;
				if (j < source.length() && isQuote(source.charAt(j))) {
					// Multi-line comment: // "..."
					int end = j + 1;
					while (end < source.length() && !isQuote(source.charAt(end)))
						end++;
					for (int k = i; k < Math.min(end, source.length()); k++)
						if (source.charAt(k) == '\n')
							line++;
					i = end + 1;
				} else {
					while (i < source.length() && source.charAt(i) != '\n')
						i++;
				}
				continue;
			}
			// <true;> and <false;> are values, so their ; doesn't end the statement
			String literal = booleanLiteralAt(source, i);
			if (literal != null) {
				if (start < 0)
					start = line;
				current.append(literal);
				i += literal.length();
				continue;
			}
			if (isStringStart(source, i)) {
				int end = stringEnd(source, i);
				if (start < 0)
					start = line;
				for (int k = i; k < end; k++)
					if (source.charAt(k) == '\n')
						line++;
				current.append(source, i, end);
				i = end;
				continue;
			}
			if (c == '(' || c == '[' || c == '{')
				depth++;
			else if ((c == ')' || c == ']' || c == '}') && depth > 0)
				depth--;
			if (depth == 0 && c == ';' && !endsToken(current)) {
				add(pieces, start, current);
				start = -1;
				i++;
				continue;
			}
			if (depth == 0 && c == '\n') {
				if (!current.toString().isBlank())
					throw new SyntaxError(line, OLD_RUN.matcher(current.toString().trim()).matches()
						? "Put more than one statement after run in { }, not ( )" : "Missing ; at the end of the line");
				line++;
				i++;
				continue;
			}
			if (c == '\n')
				line++;
			if (start < 0 && !Character.isWhitespace(c))
				start = line;
			current.append(c);
			i++;
			// A run { } or *event{ } block ends at its closing brace, without a ;
			if (depth == 0 && c == '}' && isBlock(current.toString())) {
				add(pieces, start, current);
				start = -1;
			}
		}
		if (depth > 0)
			throw new SyntaxError(start < 0 ? line : start, "Missing closing bracket");
		if (!current.toString().isBlank())
			throw new SyntaxError(line, "Missing ; at the end of the line");
		return pieces;
	}

	private static boolean isBlock(String text) {
		String trimmed = text.trim();
		for (String definer : new String[] {"local:", "global:"})
			if (trimmed.startsWith(definer))
				trimmed = trimmed.substring(definer.length()).trim();
		return trimmed.startsWith("*") || BLOCK_RUN.matcher(trimmed).matches();
	}

	// Whether a ; closes a token like &dollar; rather than ending the statement
	private static boolean endsToken(StringBuilder text) {
		int amp = text.lastIndexOf("&");
		return amp >= 0 && text.length() - amp <= 11 && text.substring(amp).matches("&[#A-Za-z0-9\\\\]+");
	}

	private static void add(List<Piece> pieces, int line, StringBuilder text) {
		String trimmed = text.toString().trim();
		if (!trimmed.isEmpty())
			pieces.add(new Piece(line, trimmed));
		text.setLength(0);
	}

	static String booleanLiteralAt(String text, int i) {
		for (String literal : new String[] {"<true;>", "<false;>"})
			if (text.startsWith(literal, i))
				return literal;
		return null;
	}

	static boolean isQuote(char c) {
		return c == '"' || c == '“' || c == '”';
	}

	// "...", the curly quotes, and '...' when the ' starts a word (so "don't" isn't a string)
	static boolean isStringStart(String text, int i) {
		char c = text.charAt(i);
		if (isQuote(c))
			return true;
		return c == '\'' && (i == 0 || " \t\n:[{(=,".indexOf(text.charAt(i - 1)) >= 0);
	}

	// Index just past the end of the string starting at i
	static int stringEnd(String text, int i) {
		char open = text.charAt(i);
		int j = i + 1;
		while (j < text.length()) {
			char c = text.charAt(j);
			if (c == '\\' && open == '\'' && j + 1 < text.length()) {
				j += 2;
				continue;
			}
			if (open == '\'' ? c == '\'' : isQuote(c))
				return j + 1;
			j++;
		}
		return text.length();
	}

	private static Statement statement(int line, String text) {
		if (text.startsWith("*"))
			return eventBlock(line, text);
		// local: and global: don't change anything while a script is a single file
		for (String definer : new String[] {"local:", "global:"})
			if (text.startsWith(definer))
				return statement(line, text.substring(definer.length()).trim());
		if (text.startsWith("/"))
			text = text.substring(1);
		List<String> words = words(text);
		String first = words.get(0);
		if (first.equals("execute"))
			return execute(line, text.substring(first.length()).trim());
		if (first.equals("store") && words.size() > 1 && (words.get(1).equals("new") || words.get(1).equals("set")))
			return store(line, text.substring(text.indexOf(words.get(1))));
		if (first.equals("store.global"))
			return storeGlobal(line, text);
		if (first.equals("mark"))
			return mark(line, text);
		// A bare if or unless is ComScript's if comp; Minecraft's execute conditions need execute in front
		if (first.equals("if") || first.equals("unless")) {
			if (words.size() < 2 || !words.get(1).equals("comp"))
				throw new SyntaxError(line, "Minecraft's " + first + " needs execute in front: execute " + text);
			return execute(line, text);
		}
		return new Command(line, text);
	}

	private static Statement eventBlock(int line, String text) {
		int open = text.indexOf('{');
		if (open < 0 || !text.endsWith("}"))
			throw new SyntaxError(line, "Expected { ... } after " + text.split("\\s")[0]);
		String event = text.substring(1, open).trim();
		// *N{ } skips N ticks between runs, so *1{ } runs every other tick
		if (event.matches("\\d+") && event.length() > 9)
			throw new SyntaxError(line, "Too many ticks to skip in *" + event + "{ }");
		if (!event.isEmpty() && !event.matches("\\d+") && !ComScriptSyntax.EVENTS.contains(event))
			throw new SyntaxError(line, "Unknown event " + event + " (known events: " + String.join(", ", ComScriptSyntax.EVENTS) + ")");
		int bodyLine = line + count(text.substring(0, open), '\n');
		return new EventBlock(line, event, parse(text.substring(open + 1, text.length() - 1), bodyLine));
	}

	private static Statement execute(int line, String text) {
		List<String> words = words(text);
		List<String> subcommands = new ArrayList<>();
		List<Comparison> comparisons = new ArrayList<>();
		int offset = 0;
		int runAt = -1;
		for (int i = 0; i < words.size(); i++) {
			String word = words.get(i);
			offset = text.indexOf(word, offset) + word.length();
			if (word.equals("run")) {
				runAt = offset;
				break;
			}
			if ((word.equals("if") || word.equals("unless")) && i + 1 < words.size() && words.get(i + 1).equals("comp")) {
				if (i + 4 >= words.size())
					throw new SyntaxError(line, "if comp needs <value1> <operator> <value2>");
				String operator = words.get(i + 3);
				if (!ComScriptSyntax.COMPARISONS.contains(operator))
					throw new SyntaxError(line, "Unknown comparison " + operator + " (use ==, !=, <, >, <= or >=)");
				comparisons.add(new Comparison(word.equals("unless"), expression(line, words.get(i + 2)), operator, expression(line, words.get(i + 4))));
				for (int k = 1; k <= 4; k++)
					offset = text.indexOf(words.get(i + k), offset) + words.get(i + k).length();
				i += 4;
				continue;
			}
			subcommands.add(word);
		}
		if (runAt < 0) {
			if (!comparisons.isEmpty())
				throw new SyntaxError(line, "Expected run after if comp");
			return new Command(line, "execute " + text);
		}
		String rest = text.substring(runAt).trim();
		if (rest.isEmpty())
			throw new SyntaxError(line, "Expected a command after run");
		List<Statement> body;
		if (rest.startsWith("{") && rest.endsWith("}"))
			body = parse(rest.substring(1, rest.length() - 1), line + count(text.substring(0, text.indexOf('{', runAt) + 1), '\n'));
		else if (rest.startsWith("(") && rest.endsWith(")"))
			throw new SyntaxError(line, "Put more than one statement after run in { }, not ( )");
		else
			body = List.of(statement(line, rest));
		return new Execute(line, String.join(" ", subcommands), comparisons, body);
	}

	private static Statement store(int line, String text) {
		boolean isNew = text.startsWith("new");
		String rest = text.substring(3).trim();
		// store new font(<name>) as {<text component JSON>} stores a style, like the JSON /tellraw takes
		if (rest.startsWith("font(")) {
			int close = rest.indexOf(')');
			String after = close < 0 ? "" : rest.substring(close + 1).trim();
			if (close < 0 || !after.startsWith("as ") && !after.startsWith("as{") && !after.equals("as"))
				throw new SyntaxError(line, "Expected store " + (isNew ? "new" : "set") + " font(<name>) as {\"text\":\"...\", ...}");
			String style = after.substring(2).trim();
			if (style.isEmpty())
				throw new SyntaxError(line, "Expected a text component after as, like {\"text\":\"WARNING!\",\"color\":\"red\"}");
			return new Store(line, StoreOp.FONT, List.of(variableName(line, rest.substring(5, close))), null, style);
		}
		if (isNew) {
			int equals = topLevelIndex(rest, "=");
			if (equals < 0)
				throw new SyntaxError(line, "store new needs a starting value: store new <name> = <value>");
			List<String> names = new ArrayList<>();
			for (String name : rest.substring(0, equals).split(","))
				names.add(variableName(line, name));
			return new Store(line, StoreOp.NEW, names, expression(line, rest.substring(equals + 1)));
		}
		int space = 0;
		while (space < rest.length() && !Character.isWhitespace(rest.charAt(space)) && "=+-*/".indexOf(rest.charAt(space)) < 0)
			space++;
		String name = variableName(line, rest.substring(0, space));
		String op = rest.substring(space).trim();
		List<String> names = List.of(name);
		if (op.equals("++"))
			return new Store(line, StoreOp.INCREMENT, names, null);
		if (op.equals("--"))
			return new Store(line, StoreOp.DECREMENT, names, null);
		if (op.startsWith("as "))
			return new Store(line, StoreOp.AS, names, expression(line, op.substring(3)));
		String[][] operators = {{"/%", "REMAINDER"}, {"=", "ASSIGN"}, {"+", "ADD"}, {"-", "SUBTRACT"}, {"*", "MULTIPLY"}, {"/", "DIVIDE"}};
		for (String[] operator : operators)
			if (op.startsWith(operator[0]))
				return new Store(line, StoreOp.valueOf(operator[1]), names, expression(line, op.substring(operator[0].length())));
		throw new SyntaxError(line, "Expected =, ++, --, +, -, *, /, /% or as after store set " + name);
	}

	// store.global set excrd = <x y z> sets the execution marker, like mark add
	private static Statement storeGlobal(int line, String text) {
		String rest = text.substring("store.global".length()).trim();
		if (!rest.startsWith("set") || !rest.substring(3).trim().startsWith("excrd") || rest.indexOf('=') < 0)
			throw new SyntaxError(line, "Expected store.global set excrd = <x y z>");
		return new Mark(line, null, rest.substring(rest.indexOf('=') + 1).trim());
	}

	private static Statement mark(int line, String text) {
		List<String> words = words(text);
		if (words.size() < 2 || !words.get(1).equals("add"))
			throw new SyntaxError(line, "Expected mark add <x y z> or mark add relative <selector> <~ ~ ~>");
		if (words.size() > 2 && words.get(2).equals("relative")) {
			if (words.size() < 4)
				throw new SyntaxError(line, "Expected mark add relative <selector> <~ ~ ~>");
			String position = String.join(" ", words.subList(4, words.size()));
			return new Mark(line, words.get(3), position.isEmpty() ? "~ ~ ~" : position);
		}
		List<String> position = new ArrayList<>(words.subList(2, words.size()));
		// The optional mobility word doesn't change anything for a fixed position
		if (!position.isEmpty() && (position.getLast().equals("static") || position.getLast().equals("mobile")))
			position.removeLast();
		if (position.isEmpty())
			throw new SyntaxError(line, "Expected mark add <x y z>");
		return new Mark(line, null, String.join(" ", position));
	}

	private static String variableName(int line, String name) {
		name = name.trim();
		if (name.startsWith("$"))
			name = name.substring(1);
		if (!name.matches("[A-Za-z_][A-Za-z0-9_]*"))
			throw new SyntaxError(line, "Invalid variable name \"" + name + "\"");
		return name;
	}

	private static ComScriptExpression.Node expression(int line, String text) {
		try {
			return ComScriptExpression.parse(text.trim());
		} catch (ComScriptExpression.ExpressionError e) {
			throw new SyntaxError(line, e.getMessage());
		}
	}

	// Space-separated words, keeping strings and bracketed groups (selectors, NBT, function calls) whole
	static List<String> words(String text) {
		List<String> words = new ArrayList<>();
		StringBuilder word = new StringBuilder();
		int depth = 0;
		int i = 0;
		while (i < text.length()) {
			char c = text.charAt(i);
			if (isStringStart(text, i)) {
				int end = stringEnd(text, i);
				word.append(text, i, end);
				i = end;
				continue;
			}
			if (c == '(' || c == '[' || c == '{')
				depth++;
			else if ((c == ')' || c == ']' || c == '}') && depth > 0)
				depth--;
			if (depth == 0 && Character.isWhitespace(c)) {
				if (!word.isEmpty())
					words.add(word.toString());
				word.setLength(0);
			} else {
				word.append(c);
			}
			i++;
		}
		if (!word.isEmpty())
			words.add(word.toString());
		if (words.isEmpty())
			words.add("");
		return words;
	}

	private static int topLevelIndex(String text, String target) {
		int depth = 0;
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if (isStringStart(text, i)) {
				i = stringEnd(text, i) - 1;
				continue;
			}
			if (c == '(' || c == '[' || c == '{')
				depth++;
			else if (c == ')' || c == ']' || c == '}')
				depth--;
			else if (depth == 0 && text.startsWith(target, i))
				return i;
		}
		return -1;
	}

	private static int count(String text, char c) {
		return (int) text.chars().filter(ch -> ch == c).count();
	}
}
