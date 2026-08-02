package com.soarclient.mixin.mixins.minecraft.client.gui;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.soarclient.libraries.browser.JCefBrowser;
import com.soarclient.management.mod.impl.hud.WebBrowserMod;
import com.soarclient.utils.mouse.MouseUtils;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

@Mixin(ChatScreen.class)
public class MixinChatScreen extends Screen {

	@Unique
	private boolean isInWebBrowser;

	protected MixinChatScreen(Component title) {
		super(title);
	}

	@Inject(method = "mouseClicked", at = @At("HEAD"))
	private void onMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {

		if (JCefBrowser.getBrowser() != null) {
			JCefBrowser.getBrowser().sendMousePress(WebBrowserMod.getInstance().getMouseX(mouseX),
					WebBrowserMod.getInstance().getMouseY(mouseY), button);
			JCefBrowser.getBrowser().setFocus(true);
		}
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (JCefBrowser.getBrowser() != null) {
			JCefBrowser.getBrowser().sendMouseRelease(WebBrowserMod.getInstance().getMouseX(mouseX),
					WebBrowserMod.getInstance().getMouseY(mouseY), button);
			JCefBrowser.getBrowser().setFocus(true);
		}
		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public void mouseMoved(double mouseX, double mouseY) {
		if (JCefBrowser.getBrowser() != null) {
			JCefBrowser.getBrowser().sendMouseMove(WebBrowserMod.getInstance().getMouseX(mouseX),
					WebBrowserMod.getInstance().getMouseY(mouseY));
			isInWebBrowser = MouseUtils.isInside(mouseX, mouseY, WebBrowserMod.getInstance().getX(),
					WebBrowserMod.getInstance().getY(), WebBrowserMod.getInstance().getPosition().getWidth(),
					WebBrowserMod.getInstance().getPosition().getHeight());
		} else {
			isInWebBrowser = false;
		}
		super.mouseMoved(mouseX, mouseY);
	}

	@Inject(method = "mouseScrolled", at = @At("HEAD"))
	private void onMouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount,
			CallbackInfoReturnable<Boolean> cir) {
		if (JCefBrowser.getBrowser() != null) {
			JCefBrowser.getBrowser().sendMouseWheel(WebBrowserMod.getInstance().getMouseX(mouseX),
					WebBrowserMod.getInstance().getMouseY(mouseY), verticalAmount);
		}
	}

	@Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
	private void onKeyPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
		if (JCefBrowser.getBrowser() != null) {
			JCefBrowser.getBrowser().sendKeyPress(keyCode, scanCode, modifiers);
			JCefBrowser.getBrowser().setFocus(true);
			if(isInWebBrowser) {
				cir.setReturnValue(true);
			}
		}
	}

	@Override
	public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
		if (JCefBrowser.getBrowser() != null) {
			JCefBrowser.getBrowser().sendKeyRelease(keyCode, scanCode, modifiers);
			JCefBrowser.getBrowser().setFocus(true);
			if(isInWebBrowser) {
				return true;
			}
		}
		return super.keyReleased(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean charTyped(char codePoint, int modifiers) {

		if (codePoint == (char) 0) {
			return false;
		}

		if (JCefBrowser.getBrowser() != null) {
			JCefBrowser.getBrowser().sendKeyTyped(codePoint, modifiers);
			JCefBrowser.getBrowser().setFocus(true);
			if(isInWebBrowser) {
				return true;
			}
		}

		return super.charTyped(codePoint, modifiers);
	}
}
