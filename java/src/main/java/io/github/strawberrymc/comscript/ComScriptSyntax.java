package io.github.strawberrymc.comscript;

import java.util.Map;
import java.util.Set;

// The words ComScript knows, shared by the interpreter and the Scripter editor's colouring
public final class ComScriptSyntax {
	private ComScriptSyntax() {
	}

	// ComScript's own statements and their sub-keywords
	public static final Set<String> KEYWORDS = Set.of("store", "new", "set", "store.global", "excrd", "mark", "add", "relative", "local:", "global:",
		"static", "mobile");

	// The execute subcommands, written without "execute" in front, plus run and if comp
	public static final Set<String> SUBCOMMANDS = Set.of("execute", "as", "at", "in", "align", "anchored", "positioned", "rotated", "facing", "if",
		"unless", "on", "run", "comp");

	// Statements that start with these are execute statements; "store" only counts when followed by result or success
	public static final Set<String> EXECUTE_STARTS = Set.of("as", "at", "in", "align", "anchored", "positioned", "rotated", "facing", "if", "unless",
		"on");

	public static final Set<String> CONVERSIONS = Set.of("string", "str", "boolean", "bool", "booleanstate", "bin", "createbin", "float", "flt", "integer",
		"int");

	public static final Set<String> FUNCTIONS = Set.of("string", "str", "boolean", "bool", "booleanstate", "bin", "createbin", "float", "flt", "integer",
		"int", "log", "log^", "cos", "cosine", "cos^", "cosine^", "sin", "sine", "sin^", "sine^", "tan", "tangent", "tan^", "tangent^", "sqrt",
		"squareroot", "sqaureroot", "fact", "factorial", "random.randint", "random.n.randint", "online", "loadchunk", "font");

	// Events a *<event>{ } block can listen for; *{ } runs every tick
	public static final Set<String> EVENTS = Set.of("blockEvent.blockBreak", "blockEvent.blockPlace", "playerEvent.join", "entityEvent.death");

	public static final Set<String> COMPARISONS = Set.of("==", "=", "!=", "<", ">", "<=", ">=");

	// Invalid token substitutions from the wiki's table, plus the decimal codes
	public static final Map<String, String> TOKENS = Map.ofEntries(Map.entry("&lt;", "<"), Map.entry("&gt;", ">"), Map.entry("&\\3C;", ">"),
		Map.entry("&amp;", "&"), Map.entry("&quot;", "\""), Map.entry("&excl;", "!"), Map.entry("&commat;", "@"), Map.entry("&num;", "#"),
		Map.entry("&dollar;", "$"), Map.entry("&percent;", "%"), Map.entry("&hat;", "^"), Map.entry("&ast;", "*"), Map.entry("&lowast;", "∗"),
		Map.entry("&oast;", "⊛"), Map.entry("&sp;", "]"), Map.entry("&cp;", "}"), Map.entry("&bs;", "\\"), Map.entry("&sc;", ";"));

	public static boolean isRandomFloat(String name) {
		return name.matches("random\\.\\d+\\.rand");
	}

	public static boolean isFunction(String name) {
		String lower = name.toLowerCase();
		return FUNCTIONS.contains(lower) || isRandomFloat(lower);
	}

	// Replaces &name; and &#123; tokens with the characters they stand for
	public static String substituteTokens(String text) {
		if (text.indexOf('&') < 0)
			return text;
		StringBuilder out = new StringBuilder();
		int i = 0;
		while (i < text.length()) {
			char c = text.charAt(i);
			int end = c == '&' ? text.indexOf(';', i) : -1;
			if (end > i && end - i <= 12) {
				String token = text.substring(i, end + 1);
				String replacement = TOKENS.get(token);
				if (replacement == null && token.matches("&#\\d+;"))
					replacement = Character.toString(Integer.parseInt(token.substring(2, token.length() - 1)));
				if (replacement != null) {
					out.append(replacement);
					i = end + 1;
					continue;
				}
			}
			out.append(c);
			i++;
		}
		return out.toString();
	}
}
