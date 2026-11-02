package io.github.strawberrymc.comscript.client;

import io.github.strawberrymc.comscript.ComScript;
import io.github.strawberrymc.comscript.ScripterBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// The Scripter's code editor: a black, JetBrains Mono text area with ComScript colouring, and the terminal underneath where
// errors are red, warnings yellow and everything else white
public class ScripterScreen extends Screen {
	private static final Style MONO = Style.EMPTY.withFont(new FontDescription.Resource(Identifier.fromNamespaceAndPath("strawberrymc", "jetbrains_mono")));
	private static final int LINE_HEIGHT = 10;
	private static final int MAX_TERMINAL_LINES = 8;
	private static final int INDENT = 4;
	private static final int BACKGROUND = 0xFF000000;
	private static final int BORDER = 0xFF3C3C3C;
	private static final int GUTTER_TEXT = 0xFF6E7681;
	private static final int GUTTER_CURRENT = 0xFFC6C6C6;
	private static final int SELECTION = 0xFF264F78;
	private static final int CURRENT_LINE = 0xFF0F0F0F;
	private static final int CARET = 0xFFAEAFAD;

	private final ScripterBlockEntity scripter;
	private String text;
	private int cursor;
	private int anchor;
	private List<Integer> lineStarts = new ArrayList<>();
	private int[] colors = new int[0];
	private Set<String> commands = Set.of();
	private int scrollLine;
	private float scrollX;
	private int terminalScroll;
	// Column the caret tries to stay in when moving up and down
	private int preferredColumn = -1;
	private boolean dragging;
	private long lastEdit;
	private final Deque<Snapshot> undo = new ArrayDeque<>();
	private final Deque<Snapshot> redo = new ArrayDeque<>();

	// Layout, set in init
	private int editorLeft, editorTop, editorRight, editorBottom, gutterWidth;
	private int terminalTop, terminalBottom;
	private float charWidth;
	private int terminalLines;

	private record Snapshot(String text, int cursor) {
	}

	public static void open(ScripterBlockEntity scripter) {
		Minecraft.getInstance().setScreen(new ScripterScreen(scripter));
	}

	private ScripterScreen(ScripterBlockEntity scripter) {
		super(Component.translatable("gui.strawberrymc.scripter.title"));
		this.scripter = scripter;
		this.text = scripter.getScript().replace("\t", " ".repeat(INDENT));
		textChanged();
	}

	@Override
	protected void init() {
		ClientPacketListener connection = minecraft.getConnection();
		if (connection != null) {
			Set<String> names = new HashSet<>();
			connection.getCommands().getRoot().getChildren().forEach(node -> names.add(node.getName()));
			commands = names;
			colors = ScriptHighlighter.colors(text, commands);
		}
		charWidth = font.width(Component.literal("0000000000").withStyle(MONO)) / 10.0F;
		int buttonY = height - 26;
		terminalBottom = buttonY - 6;
		// The terminal gets about a quarter of the space, so small windows still show plenty of script
		terminalLines = Mth.clamp((terminalBottom - 22) / 4 / LINE_HEIGHT, 2, MAX_TERMINAL_LINES);
		terminalTop = terminalBottom - terminalLines * LINE_HEIGHT - 16;
		editorLeft = 8;
		editorRight = width - 8;
		editorTop = 22;
		editorBottom = terminalTop - 6;
		gutterWidth = (int) (charWidth * 4) + 8;

		int buttonWidth = Math.min(100, (width - 16 - 3 * 6) / 4);
		int x = width / 2 - (buttonWidth * 4 + 18) / 2;
		addRenderableWidget(Button.builder(Component.translatable("gui.strawberrymc.scripter.run"), b -> send(ComScript.Action.RUN))
			.bounds(x, buttonY, buttonWidth, 20).build());
		addRenderableWidget(Button.builder(Component.translatable("gui.strawberrymc.scripter.stop"), b -> send(ComScript.Action.STOP))
			.bounds(x + buttonWidth + 6, buttonY, buttonWidth, 20).build());
		addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> done()).bounds(x + 2 * (buttonWidth + 6), buttonY, buttonWidth, 20).build());
		addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> onClose()).bounds(x + 3 * (buttonWidth + 6), buttonY, buttonWidth, 20).build());
		addRenderableWidget(Button.builder(Component.translatable("gui.strawberrymc.scripter.clear"), b -> send(ComScript.Action.CLEAR_TERMINAL))
			.bounds(editorRight - 44, terminalTop + 2, 42, 12).build());
	}

	private void send(ComScript.Action action) {
		ClientPacketDistributor.sendToServer(new ComScript.EditScripter(scripter.getBlockPos(), text, action.ordinal()));
		// So the editor shows the saved script if it's reopened before the server's update arrives
		if (action != ComScript.Action.CLEAR_TERMINAL)
			scripter.setScript(text);
		terminalScroll = 0;
	}

	private void done() {
		send(ComScript.Action.SAVE);
		minecraft.setScreen(null);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public boolean isInGameUi() {
		return true;
	}

	@Override
	public void tick() {
		if (scripter.isRemoved())
			onClose();
	}

	// ---- Text model ----

	private void textChanged() {
		lineStarts = new ArrayList<>();
		lineStarts.add(0);
		for (int i = 0; i < text.length(); i++)
			if (text.charAt(i) == '\n')
				lineStarts.add(i + 1);
		colors = ScriptHighlighter.colors(text, commands);
	}

	private int lineOf(int index) {
		int low = 0, high = lineStarts.size() - 1;
		while (low < high) {
			int mid = (low + high + 1) / 2;
			if (lineStarts.get(mid) <= index)
				low = mid;
			else
				high = mid - 1;
		}
		return low;
	}

	private int lineEnd(int line) {
		return line + 1 < lineStarts.size() ? lineStarts.get(line + 1) - 1 : text.length();
	}

	private String lineText(int line) {
		return text.substring(lineStarts.get(line), lineEnd(line));
	}

	private boolean hasSelection() {
		return cursor != anchor;
	}

	private String selectedText() {
		return text.substring(Math.min(cursor, anchor), Math.max(cursor, anchor));
	}

	// Typing in a burst counts as one undo step
	private void remember(boolean typing) {
		long now = System.currentTimeMillis();
		if (!typing || now - lastEdit > 1000 || undo.isEmpty())
			undo.push(new Snapshot(text, cursor));
		lastEdit = typing ? now : 0;
		while (undo.size() > 200)
			undo.removeLast();
		redo.clear();
	}

	private void replaceSelection(String insert, boolean typing) {
		remember(typing);
		int start = Math.min(cursor, anchor);
		int end = Math.max(cursor, anchor);
		insert = insert.replace("\r\n", "\n").replace('\r', '\n').replace("\t", " ".repeat(INDENT));
		if (text.length() - (end - start) + insert.length() > ScripterBlockEntity.MAX_SCRIPT_LENGTH)
			return;
		text = text.substring(0, start) + insert + text.substring(end);
		cursor = anchor = start + insert.length();
		preferredColumn = -1;
		textChanged();
		scrollToCursor();
	}

	private void moveCursor(int index, boolean select) {
		cursor = Mth.clamp(index, 0, text.length());
		if (!select)
			anchor = cursor;
		scrollToCursor();
	}

	private void restore(Deque<Snapshot> from, Deque<Snapshot> to) {
		if (from.isEmpty())
			return;
		to.push(new Snapshot(text, cursor));
		Snapshot snapshot = from.pop();
		text = snapshot.text();
		cursor = anchor = Math.min(snapshot.cursor(), text.length());
		lastEdit = 0;
		textChanged();
		scrollToCursor();
	}

	private int wordBoundary(int from, int direction) {
		int i = from;
		if (direction < 0) {
			while (i > 0 && Character.isWhitespace(text.charAt(i - 1)) && text.charAt(i - 1) != '\n')
				i--;
			boolean word = i > 0 && isWordChar(text.charAt(i - 1));
			while (i > 0 && isWordChar(text.charAt(i - 1)) == word && !Character.isWhitespace(text.charAt(i - 1)))
				i--;
			return i == from && i > 0 ? i - 1 : i;
		}
		while (i < text.length() && Character.isWhitespace(text.charAt(i)) && text.charAt(i) != '\n')
			i++;
		boolean word = i < text.length() && isWordChar(text.charAt(i));
		while (i < text.length() && isWordChar(text.charAt(i)) == word && !Character.isWhitespace(text.charAt(i)))
			i++;
		return i == from && i < text.length() ? i + 1 : i;
	}

	private static boolean isWordChar(char c) {
		return Character.isLetterOrDigit(c) || c == '_' || c == '$';
	}

	// ---- Layout helpers ----

	private int visibleLines() {
		return Math.max(1, (editorBottom - editorTop - 4) / LINE_HEIGHT);
	}

	private int textLeft() {
		return editorLeft + gutterWidth + 4;
	}

	private int columnX(int column) {
		return textLeft() + Math.round(column * charWidth - scrollX);
	}

	private void scrollToCursor() {
		int line = lineOf(cursor);
		if (line < scrollLine)
			scrollLine = line;
		else if (line >= scrollLine + visibleLines())
			scrollLine = line - visibleLines() + 1;
		float x = (cursor - lineStarts.get(line)) * charWidth;
		float width = editorRight - textLeft() - 6;
		if (x < scrollX)
			scrollX = Math.max(0, x - width / 3);
		else if (x > scrollX + width)
			scrollX = x - width + width / 3;
	}

	private int indexAt(double mouseX, double mouseY) {
		int line = Mth.clamp(scrollLine + (int) Math.floor((mouseY - editorTop - 2) / LINE_HEIGHT), 0, lineStarts.size() - 1);
		int column = Math.round((float) (mouseX - textLeft() + scrollX) / charWidth);
		return lineStarts.get(line) + Mth.clamp(column, 0, lineText(line).length());
	}

	private boolean inEditor(double x, double y) {
		return x >= editorLeft && x < editorRight && y >= editorTop && y < editorBottom;
	}

	private boolean inTerminal(double x, double y) {
		return x >= editorLeft && x < editorRight && y >= terminalTop && y < terminalBottom;
	}

	// ---- Input ----

	@Override
	public boolean charTyped(CharacterEvent event) {
		if (!event.isAllowedChatCharacter())
			return false;
		replaceSelection(event.codepointAsString(), true);
		return true;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		boolean shift = event.hasShiftDown();
		boolean control = event.hasControlDownWithQuirk();
		// Ctrl (Option on a Mac) moves and deletes by word
		boolean word = event.hasControlDown() || event.hasAltDown();
		int key = event.key();
		if (event.isEscape()) {
			done();
			return true;
		}
		if (event.isSelectAll()) {
			anchor = 0;
			cursor = text.length();
			return true;
		}
		if (event.isCopy()) {
			if (hasSelection())
				minecraft.keyboardHandler.setClipboard(selectedText());
			return true;
		}
		if (event.isCut()) {
			if (hasSelection()) {
				minecraft.keyboardHandler.setClipboard(selectedText());
				replaceSelection("", false);
			}
			return true;
		}
		if (event.isPaste()) {
			replaceSelection(minecraft.keyboardHandler.getClipboard(), false);
			return true;
		}
		if (control && key == GLFW.GLFW_KEY_Z) {
			if (shift)
				restore(redo, undo);
			else
				restore(undo, redo);
			return true;
		}
		if (control && key == GLFW.GLFW_KEY_Y) {
			restore(redo, undo);
			return true;
		}
		int line = lineOf(cursor);
		int column = cursor - lineStarts.get(line);
		switch (key) {
			case GLFW.GLFW_KEY_BACKSPACE -> {
				if (!hasSelection()) {
					if (cursor == 0)
						return true;
					// Backspace in leading spaces removes a whole indent
					String before = text.substring(lineStarts.get(line), cursor);
					int remove = word ? cursor - wordBoundary(cursor, -1)
						: !before.isEmpty() && before.isBlank() ? (before.length() - 1) % INDENT + 1 : 1;
					anchor = cursor - remove;
				}
				replaceSelection("", false);
			}
			case GLFW.GLFW_KEY_DELETE -> {
				if (!hasSelection()) {
					if (cursor == text.length())
						return true;
					anchor = word ? wordBoundary(cursor, 1) : cursor + 1;
				}
				replaceSelection("", false);
			}
			case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
				// Keep the line's indent, and indent one more after an opening bracket
				String current = lineText(line);
				int indent = 0;
				while (indent < current.length() && current.charAt(indent) == ' ')
					indent++;
				String before = current.substring(0, Math.min(column, current.length())).stripTrailing();
				if (before.endsWith("{") || before.endsWith("("))
					indent += INDENT;
				replaceSelection("\n" + " ".repeat(indent), false);
			}
			case GLFW.GLFW_KEY_TAB -> replaceSelection(" ".repeat(INDENT - column % INDENT), false);
			case GLFW.GLFW_KEY_LEFT -> {
				if (hasSelection() && !shift)
					moveCursor(Math.min(cursor, anchor), false);
				else
					moveCursor(word ? wordBoundary(cursor, -1) : cursor - 1, shift);
				preferredColumn = -1;
			}
			case GLFW.GLFW_KEY_RIGHT -> {
				if (hasSelection() && !shift)
					moveCursor(Math.max(cursor, anchor), false);
				else
					moveCursor(word ? wordBoundary(cursor, 1) : cursor + 1, shift);
				preferredColumn = -1;
			}
			case GLFW.GLFW_KEY_UP, GLFW.GLFW_KEY_DOWN, GLFW.GLFW_KEY_PAGE_UP, GLFW.GLFW_KEY_PAGE_DOWN -> {
				int lines = key == GLFW.GLFW_KEY_UP ? -1 : key == GLFW.GLFW_KEY_DOWN ? 1 : key == GLFW.GLFW_KEY_PAGE_UP ? -visibleLines() : visibleLines();
				if (preferredColumn < 0)
					preferredColumn = column;
				int target = Mth.clamp(line + lines, 0, lineStarts.size() - 1);
				int keep = preferredColumn;
				if (target == line)
					moveCursor(lines < 0 ? 0 : text.length(), shift);
				else
					moveCursor(lineStarts.get(target) + Math.min(preferredColumn, lineText(target).length()), shift);
				preferredColumn = keep;
			}
			case GLFW.GLFW_KEY_HOME -> {
				// Home goes to the first non-space character, then to the start of the line
				String current = lineText(line);
				int indent = current.length() - current.stripLeading().length();
				moveCursor(control ? 0 : lineStarts.get(line) + (column == indent ? 0 : indent), shift);
				preferredColumn = -1;
			}
			case GLFW.GLFW_KEY_END -> {
				moveCursor(control ? text.length() : lineEnd(line), shift);
				preferredColumn = -1;
			}
			default -> {
				return super.keyPressed(event);
			}
		}
		return true;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() == 0 && inEditor(event.x(), event.y())) {
			int index = indexAt(event.x(), event.y());
			if (doubleClick) {
				anchor = wordBoundary(Math.min(index + 1, text.length()), -1);
				cursor = wordBoundary(anchor, 1);
			} else {
				moveCursor(index, event.hasShiftDown());
				dragging = true;
			}
			preferredColumn = -1;
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		if (dragging) {
			moveCursor(indexAt(event.x(), event.y()), true);
			return true;
		}
		return super.mouseDragged(event, dx, dy);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		dragging = false;
		return super.mouseReleased(event);
	}

	@Override
	public boolean mouseScrolled(double x, double y, double scrollXAmount, double scrollYAmount) {
		if (inEditor(x, y)) {
			scrollLine = Mth.clamp(scrollLine - (int) Math.signum(scrollYAmount) * 3, 0, Math.max(0, lineStarts.size() - visibleLines()));
			scrollX = Math.max(0, scrollX - (float) scrollXAmount * charWidth * 3);
			return true;
		}
		if (inTerminal(x, y)) {
			terminalScroll = Mth.clamp(terminalScroll + (int) Math.signum(scrollYAmount), 0, Math.max(0, scripter.getLog().size() - terminalLines));
			return true;
		}
		return super.mouseScrolled(x, y, scrollXAmount, scrollYAmount);
	}

	// ---- Drawing ----

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		graphics.text(font, title, editorLeft, 8, 0xFFFFFFFF, true);
		String position = scripter.getBlockPos().toShortString() + (scripter.isListening() ? "  ● listening" : "");
		graphics.text(font, position, editorRight - font.width(position), 8, scripter.isListening() ? 0xFF6A9955 : 0xFF9D9D9D, true);
		extractEditor(graphics);
		extractTerminal(graphics);
		super.extractRenderState(graphics, mouseX, mouseY, a);
	}

	private void extractEditor(GuiGraphicsExtractor graphics) {
		graphics.fill(editorLeft - 1, editorTop - 1, editorRight + 1, editorBottom + 1, BORDER);
		graphics.fill(editorLeft, editorTop, editorRight, editorBottom, BACKGROUND);
		int cursorLine = lineOf(cursor);
		int selectionStart = Math.min(cursor, anchor), selectionEnd = Math.max(cursor, anchor);
		int last = Math.min(lineStarts.size(), scrollLine + visibleLines());

		// Line numbers
		for (int line = scrollLine; line < last; line++) {
			String number = Integer.toString(line + 1);
			int y = editorTop + 2 + (line - scrollLine) * LINE_HEIGHT;
			graphics.text(font, Component.literal(number).withStyle(MONO), editorLeft + gutterWidth - 4 - font.width(Component.literal(number).withStyle(MONO)), y,
				line == cursorLine ? GUTTER_CURRENT : GUTTER_TEXT, false);
		}

		graphics.enableScissor(textLeft() - 2, editorTop, editorRight - 1, editorBottom);
		for (int line = scrollLine; line < last; line++) {
			int y = editorTop + 2 + (line - scrollLine) * LINE_HEIGHT;
			int start = lineStarts.get(line);
			int end = lineEnd(line);
			if (line == cursorLine && !hasSelection())
				graphics.fill(textLeft() - 2, y - 1, editorRight, y + LINE_HEIGHT - 1, CURRENT_LINE);
			// Selection, including the newline at the end of a selected line
			int from = Math.max(selectionStart, start), to = Math.min(selectionEnd, end);
			if (hasSelection() && from <= to && selectionStart <= end && selectionEnd >= start) {
				int right = columnX(to - start) + (selectionEnd > end ? (int) charWidth : 0);
				graphics.fill(columnX(from - start), y - 1, Math.max(right, columnX(from - start) + 1), y + LINE_HEIGHT - 1, SELECTION);
			}
			graphics.text(font, colored(start, end), columnX(0), y, 0xFFFFFFFF, false);
		}
		// Blinking caret
		if (System.currentTimeMillis() / 530 % 2 == 0 && cursorLine >= scrollLine && cursorLine < last) {
			int x = columnX(cursor - lineStarts.get(cursorLine));
			int y = editorTop + 2 + (cursorLine - scrollLine) * LINE_HEIGHT;
			graphics.fill(x, y - 1, x + 1, y + LINE_HEIGHT - 1, CARET);
		}
		graphics.disableScissor();
	}

	// The line's text in runs of the same colour
	private Component colored(int start, int end) {
		MutableComponent line = Component.empty();
		int i = start;
		while (i < end) {
			int color = colors[i];
			int j = i + 1;
			while (j < end && colors[j] == color)
				j++;
			line.append(Component.literal(text.substring(i, j)).withStyle(MONO.withColor(color)));
			i = j;
		}
		return line;
	}

	private void extractTerminal(GuiGraphicsExtractor graphics) {
		graphics.fill(editorLeft - 1, terminalTop - 1, editorRight + 1, terminalBottom + 1, BORDER);
		graphics.fill(editorLeft, terminalTop, editorRight, terminalBottom, 0xFF0C0C0C);
		graphics.text(font, Component.translatable("gui.strawberrymc.scripter.terminal").withStyle(MONO), editorLeft + 4, terminalTop + 4, 0xFF9D9D9D, false);
		List<ScripterBlockEntity.LogLine> log = scripter.getLog();
		int first = Math.max(0, log.size() - terminalLines - terminalScroll);
		graphics.enableScissor(editorLeft + 2, terminalTop + 14, editorRight - 2, terminalBottom);
		for (int i = 0; i < terminalLines && first + i < log.size(); i++) {
			ScripterBlockEntity.LogLine entry = log.get(first + i);
			int color = entry.level() == ScripterBlockEntity.ERROR ? 0xFFF14C4C : entry.level() == ScripterBlockEntity.WARNING ? 0xFFE5E510 : 0xFFFFFFFF;
			graphics.text(font, Component.literal(entry.text()).withStyle(MONO), editorLeft + 4, terminalTop + 15 + i * LINE_HEIGHT, color, false);
		}
		graphics.disableScissor();
	}
}
