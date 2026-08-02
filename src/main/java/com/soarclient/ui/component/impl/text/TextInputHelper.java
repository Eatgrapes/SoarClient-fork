package com.soarclient.ui.component.impl.text;

import org.lwjgl.glfw.GLFW;

import com.soarclient.utils.MathUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.util.StringUtil;

public class TextInputHelper {

	private Minecraft client = Minecraft.getInstance();

	private String text;
	private boolean focused;
	private int cursorPosition;
	private int selectionEnd;
	private int maxStringLength;

	public TextInputHelper() {
		this.focused = false;
		this.text = "";
		this.maxStringLength = 256;
	}

	public void keyPressed(int keyCode, int scanCode, int modifiers) {

		if (!focused) {
			return;
		} else if (modifiers == GLFW.GLFW_MOD_CONTROL && keyCode == GLFW.GLFW_KEY_C) {
			client.keyboardHandler.setClipboard(this.getSelectedText());
			return;
		} else if (modifiers == GLFW.GLFW_MOD_CONTROL && keyCode == GLFW.GLFW_KEY_V) {
			writeText(client.keyboardHandler.getClipboard());
			return;
		} else if (modifiers == GLFW.GLFW_MOD_CONTROL && keyCode == GLFW.GLFW_KEY_X) {
			client.keyboardHandler.setClipboard(this.getSelectedText());
			this.writeText("");
			return;
		} else if (modifiers == GLFW.GLFW_MOD_CONTROL && keyCode == GLFW.GLFW_KEY_A) {
			this.setCursorPosition(this.text.length());
			this.setSelectionPos(0);
			return;
		} else {
			switch (keyCode) {
			case GLFW.GLFW_KEY_BACKSPACE:
				if (modifiers == GLFW.GLFW_MOD_CONTROL) {
					this.deleteWords(-1);
				} else {
					this.deleteFromCursor(-1);
				}
				return;
			case GLFW.GLFW_KEY_HOME:
				if (keyCode == GLFW.GLFW_KEY_LEFT_SHIFT) {
					this.setSelectionPos(0);
				} else {
					this.setCursorPosition(0);
				}
				return;
			case GLFW.GLFW_KEY_LEFT:
				if (modifiers == GLFW.GLFW_MOD_SHIFT) {
					if (modifiers == GLFW.GLFW_MOD_CONTROL) {
						this.setSelectionPos(this.getNthWordFromPos(-1, this.selectionEnd));
					} else {
						this.setSelectionPos(this.selectionEnd - 1);
					}
				} else if (modifiers == GLFW.GLFW_MOD_CONTROL) {
					this.setCursorPosition(this.getNthWordFromCursor(-1));
				} else {
					this.moveCursorBy(-1);
				}
				return;
			case GLFW.GLFW_KEY_RIGHT:
				if (keyCode == GLFW.GLFW_KEY_LEFT_SHIFT) {
					if (modifiers == GLFW.GLFW_MOD_CONTROL) {
						this.setSelectionPos(this.getNthWordFromPos(1, this.selectionEnd));
					} else {
						this.setSelectionPos(this.selectionEnd + 1);
					}
				} else if (modifiers == GLFW.GLFW_MOD_CONTROL) {
					this.setCursorPosition(this.getNthWordFromCursor(1));
				} else {
					this.moveCursorBy(1);
				}
				return;
			case GLFW.GLFW_KEY_END:
				if (modifiers == GLFW.GLFW_MOD_SHIFT) {
					this.setSelectionPos(this.text.length());
				} else {
					this.setCursorPosition(this.text.length());
				}
				return;
			case GLFW.GLFW_KEY_DELETE:
				if (modifiers == GLFW.GLFW_MOD_CONTROL) {
					this.deleteWords(1);
				} else {
					this.deleteFromCursor(1);
				}
				return;
			default:
				return;
			}
		}
	}

	public void charTyped(char chr, int modifiers) {
		if (StringUtil.isAllowedChatCharacter(chr) && this.isFocused()) {
			this.writeText(Character.toString(chr));
		}
	}

	private void writeText(String text) {

		String s = "";
		String s1 = StringUtil.filterText(text);
		int min = Math.min(this.cursorPosition, this.selectionEnd);
		int max = Math.max(this.cursorPosition, this.selectionEnd);
		int len = this.maxStringLength - this.text.length() - (min - max);
		int l;

		if (this.text.length() > 0) {
			s = s + this.text.substring(0, min);
		}

		if (len < s1.length()) {
			s = s + s1.substring(0, len);
			l = len;
		} else {
			s = s + s1;
			l = s1.length();
		}

		if (this.text.length() > 0 && max < this.text.length()) {
			s = s + this.text.substring(max);
		}

		this.text = s;
		this.moveCursorBy(min - this.selectionEnd + l);
	}

	private void deleteWords(int num) {
		if (this.text.length() != 0) {
			if (this.selectionEnd != this.cursorPosition) {
				this.writeText("");
			} else {
				this.deleteFromCursor(this.getNthWordFromCursor(num) - this.cursorPosition);
			}
		}
	}

	private String getSelectedText() {

		int min = Math.min(this.cursorPosition, this.selectionEnd);
		int max = Math.max(this.cursorPosition, this.selectionEnd);

		return this.text.substring(min, max);
	}

	private void deleteFromCursor(int num) {

		if (this.text.length() != 0) {

			if (this.selectionEnd != this.cursorPosition) {
				this.writeText("");
			} else {

				boolean negative = num < 0;
				int i = negative ? this.cursorPosition + num : this.cursorPosition;
				int j = negative ? this.cursorPosition : this.cursorPosition + num;
				String s = "";

				if (i > 0) {
					s = this.text.substring(0, i);
				}

				if (j < this.text.length()) {
					s = s + this.text.substring(j);
				}

				this.text = s;

				if (negative) {
					this.moveCursorBy(num);
				}
			}
		}
	}

	private int getNthWordFromCursor(int num) {
		return getNthWordFromPos(num, this.cursorPosition);
	}

	private int getNthWordFromPos(int num, int pos) {

		int i = pos;
		boolean negative = num < 0;
		int j = Math.abs(num);

		for (int k = 0; k < j; ++k) {
			if (!negative) {
				int l = this.text.length();
				i = this.text.indexOf(32, i);

				if (i == -1) {
					i = l;
				} else {
					while (i < l && this.text.charAt(i) == 32) {
						++i;
					}
				}
			} else {
				while (i > 0 && this.text.charAt(i - 1) == 32) {
					--i;
				}

				while (i > 0 && this.text.charAt(i - 1) != 32) {
					--i;
				}
			}
		}

		return i;
	}

	private void moveCursorBy(int i) {
		this.setCursorPosition(this.selectionEnd + i);
	}

	private void setCursorPosition(int i) {

		this.cursorPosition = i;

		int len = this.text.length();

		this.cursorPosition = MathUtils.clamp(this.cursorPosition, 0, len);
		this.setSelectionPos(this.cursorPosition);
	}

	private void setSelectionPos(int selectionPos) {

		int len = this.text.length();

		if (selectionPos > len) {
			selectionPos = len;
		}

		if (selectionPos < 0) {
			selectionPos = 0;
		}

		this.selectionEnd = selectionPos;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
		this.setCursorPosition(this.getText().length());
	}

	public boolean isFocused() {
		return focused;
	}

	public void setFocused(boolean focused) {
		this.focused = focused;
	}

	public int getSelectionEnd() {
		return selectionEnd;
	}

	public void setSelectionEnd(int selectionEnd) {
		this.selectionEnd = selectionEnd;
	}

	public int getMaxStringLength() {
		return maxStringLength;
	}

	public void setMaxStringLength(int maxStringLength) {
		this.maxStringLength = maxStringLength;
	}

	public int getCursorPosition() {
		return cursorPosition;
	}
}