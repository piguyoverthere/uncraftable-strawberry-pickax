package io.github.strawberrymc.comscript.client;

import io.github.strawberrymc.comscript.ComScriptSyntax;

import java.util.Arrays;
import java.util.Set;

// Colours ComScript for the Scripter editor, one colour per character, using VS Code's dark theme colours
public final class ScriptHighlighter {
	public static final int TEXT = 0xD4D4D4;
	public static final int COMMENT = 0x6A9955;
	public static final int STRING = 0xCE9178;
	public static final int NUMBER = 0xB5CEA8;
	public static final int VARIABLE = 0x9CDCFE;
	public static final int SELECTOR = 0x4FC1FF;
	public static final int KEYWORD = 0xC586C0;
	public static final int SUBCOMMAND = 0x569CD6;
	public static final int COMMAND = 0xDCDCAA;
	public static final int FUNCTION = 0x4EC9B0;
	public static final int EVENT = 0xD16969;
	public static final int TOKEN = 0xD7BA7D;

	private ScriptHighlighter() {
	}

	public static int[] colors(String text, Set<String> commands) {
		int[] colors = new int[text.length()];
		Arrays.fill(colors, TEXT);
		// Whether the next word starts a statement, so it's a command name
		boolean statementStart = true;
		int i = 0;
		while (i < text.length()) {
			char c = text.charAt(i);
			if (c == '/' && i + 1 < text.length() && text.charAt(i + 1) == '/') {
				int j = i + 2;
				while (j < text.length() && text.charAt(j) == ' ')
					j++;
				int end;
				if (j < text.length() && isQuote(text.charAt(j))) {
					end = j + 1;
					while (end < text.length() && !isQuote(text.charAt(end)))
						end++;
					end = Math.min(end + 1, text.length());
				} else {
					end = text.indexOf('\n', i);
					if (end < 0)
						end = text.length();
				}
				Arrays.fill(colors, i, end, COMMENT);
				i = end;
				continue;
			}
			if (isQuote(c)) {
				int end = i + 1;
				while (end < text.length() && !isQuote(text.charAt(end)) && text.charAt(end) != '\n')
					end++;
				end = Math.min(end + 1, text.length());
				Arrays.fill(colors, i, end, STRING);
				i = end;
				statementStart = false;
				continue;
			}
			if (c == '<' && (text.startsWith("<true;>", i) || text.startsWith("<false;>", i))) {
				int end = i + (text.startsWith("<true;>", i) ? 7 : 8);
				Arrays.fill(colors, i, end, KEYWORD);
				i = end;
				statementStart = false;
				continue;
			}
			if (c == ';' || c == '\n' || c == '(' || c == '{') {
				statementStart = true;
				i++;
				continue;
			}
			if (Character.isWhitespace(c)) {
				i++;
				continue;
			}
			if (c == '*' && statementStart) {
				int end = i + 1;
				while (end < text.length() && (Character.isLetterOrDigit(text.charAt(end)) || text.charAt(end) == '.'))
					end++;
				Arrays.fill(colors, i, end, EVENT);
				i = end;
				continue;
			}
			if (c == '$') {
				int end = i + 1;
				while (end < text.length() && (Character.isLetterOrDigit(text.charAt(end)) || text.charAt(end) == '_'))
					end++;
				Arrays.fill(colors, i, end, VARIABLE);
				i = end;
				statementStart = false;
				continue;
			}
			if (c == '@') {
				int end = i + 1;
				while (end < text.length() && (Character.isLetterOrDigit(text.charAt(end)) || text.charAt(end) == '.' || text.charAt(end) == '_'))
					end++;
				// Selector arguments in [ ] belong to the selector
				if (end < text.length() && text.charAt(end) == '[') {
					int depth = 0;
					while (end < text.length()) {
						char d = text.charAt(end++);
						if (d == '[')
							depth++;
						else if (d == ']' && --depth == 0)
							break;
						else if (d == '\n')
							break;
					}
				}
				Arrays.fill(colors, i, end, SELECTOR);
				i = end;
				statementStart = false;
				continue;
			}
			if (c == '&') {
				int end = text.indexOf(';', i);
				if (end > i && end - i <= 12 && text.substring(i, end).matches("&[#A-Za-z0-9\\\\]+")) {
					Arrays.fill(colors, i, end + 1, TOKEN);
					i = end + 1;
					continue;
				}
			}
			if (Character.isDigit(c) || (c == '~' || c == '^') || (c == '-' || c == '.') && i + 1 < text.length() && Character.isDigit(text.charAt(i + 1))
					&& (i == 0 || !Character.isLetterOrDigit(text.charAt(i - 1)))) {
				int end = i + 1;
				while (end < text.length() && (Character.isDigit(text.charAt(end)) || text.charAt(end) == '.'))
					end++;
				if (i == 0 || !Character.isLetter(text.charAt(i - 1))) {
					Arrays.fill(colors, i, end, NUMBER);
					i = end;
					statementStart = false;
					continue;
				}
			}
			if (Character.isLetter(c) || c == '_') {
				int end = i + 1;
				while (end < text.length() && (Character.isLetterOrDigit(text.charAt(end)) || text.charAt(end) == '_' || text.charAt(end) == '.'
						|| text.charAt(end) == ':' && end + 1 < text.length() && Character.isLetter(text.charAt(end + 1))))
					end++;
				// local: and global:
				if (end < text.length() && text.charAt(end) == ':' && (text.startsWith("local", i) || text.startsWith("global", i)) && end - i <= 6)
					end++;
				if (end < text.length() && text.charAt(end) == '^' && end + 1 < text.length() && text.charAt(end + 1) == '(')
					end++;
				String word = text.substring(i, end);
				int color = TEXT;
				boolean call = end < text.length() && text.charAt(end) == '(';
				if (call && ComScriptSyntax.isFunction(word))
					color = FUNCTION;
				else if (ComScriptSyntax.KEYWORDS.contains(word) || word.equals("mod"))
					color = KEYWORD;
				else if (ComScriptSyntax.SUBCOMMANDS.contains(word))
					color = SUBCOMMAND;
				else if (statementStart && (commands.contains(word) || word.equals("store")))
					color = COMMAND;
				Arrays.fill(colors, i, end, color);
				// After "run" or a definer, the next word starts a command again
				statementStart = word.equals("run") || word.equals("local:") || word.equals("global:");
				i = end;
				continue;
			}
			statementStart = false;
			i++;
		}
		return colors;
	}

	private static boolean isQuote(char c) {
		return c == '"' || c == '“' || c == '”';
	}
}
