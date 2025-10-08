package com.soarclient.gui.modmenu.component;

import com.soarclient.Soar;
import com.soarclient.animation.SimpleAnimation;
import com.soarclient.libraries.material3.hct.Hct;
import com.soarclient.management.color.api.ColorPalette;
import com.soarclient.management.mod.impl.settings.ModMenuSettings;
import com.soarclient.management.mod.settings.Setting;
import com.soarclient.management.mod.settings.impl.*;
import com.soarclient.skia.Skia;
import com.soarclient.skia.font.Fonts;
import com.soarclient.ui.component.Component;
import com.soarclient.ui.component.handler.impl.*;
import com.soarclient.ui.component.impl.*;
import com.soarclient.ui.component.impl.text.TextField;
import com.soarclient.utils.language.I18n;
import net.minecraft.client.util.InputUtil;

import java.io.File;

public class SettingBar extends Component {

	private final SimpleAnimation yAnimation = new SimpleAnimation();
	private final String title, description, icon;
	private Component component;

	public SettingBar(Setting setting, float x, float y, float width) {
		super(x, y);
		this.title = setting.getName();
		this.description = setting.getDescription();
		this.icon = setting.getIcon();
		this.width = width;
		this.height = 68;

		if (setting instanceof BooleanSetting bSetting) {

			Switch switchComp = new Switch(x, y, bSetting.isEnabled());

			switchComp.setHandler(new SwitchHandler() {

				@Override
				public void onEnabled() {
					bSetting.setEnabled(true);
				}

				@Override
				public void onDisabled() {
					bSetting.setEnabled(false);
				}
			});

			component = switchComp;
		}

		if (setting instanceof NumberSetting nSetting) {

			Slider slider = new Slider(0, 0, 200, nSetting.getValue(), nSetting.getMinValue(), nSetting.getMaxValue(),
					nSetting.getStep());

			slider.setHandler(new SliderHandler() {

				@Override
				public void onValueChanged(float value) {
					nSetting.setValue(value);
				}
			});

			component = slider;
		}

		if (setting instanceof ComboSetting cSetting) {

			ComboButton button = new ComboButton(0, 0, cSetting.getOptions(), cSetting.getOption());

			button.setHandler(new ComboButtonHandler() {

				@Override
				public void onChanged(String option) {
					cSetting.setOption(option);
				}
			});

			component = button;
		}

		if (setting instanceof KeybindSetting kSetting) {

			Keybind bind = new Keybind(0, 0, kSetting.getKey());

			bind.setHandler(new KeybindHandler() {

				@Override
				public void onBinded(InputUtil.Key key) {
					kSetting.setKey(key);
				}
			});

			component = bind;
		}

		if (setting instanceof HctColorSetting hSetting) {

			HctColorPicker picker = new HctColorPicker(0, 0, hSetting.getHct());

			picker.setHandler(new HctColorPickerHandler() {

				@Override
				public void onPicking(Hct hct) {
					hSetting.setHct(hct);
				}
			});

			component = picker;
		}

		if (setting instanceof StringSetting sSetting) {

			TextField textField = new TextField(0, 0, 150, sSetting.getValue());

			textField.setHandler(new TextHandler() {

				@Override
				public void onTyped(String value) {
					sSetting.setValue(value);
				}
			});

			component = textField;
		}
		
		if(setting instanceof FileSetting fSetting) {
			
			FileSelector fileSelector = new FileSelector(0, 0, fSetting.getFile(), fSetting.getExtensions());
			
			fileSelector.setHandler(new FileSelectorHandler() {

				@Override
				public void onSelect(File file) {
					fSetting.setFile(file);
				}
			});
			
			component = fileSelector;
		}
	}

	@Override
	public void draw(double mouseX, double mouseY) {

		boolean isWinStyle = ModMenuSettings.getInstance().getUiStyleSetting().getOption().equals("win");
		ColorPalette palette = Soar.getInstance().getColorManager().getPalette();

		float itemY = y;

		yAnimation.onTick(itemY, 14);
		itemY = yAnimation.getValue();

		if (component != null) {
			component.setX(x + width - component.getWidth() - (isWinStyle ? 16 : 22));
			component.setY(itemY + (height - component.getHeight()) / 2);
		}

		if (isWinStyle) {
			height = 56;
			Skia.drawRect(x, itemY, width, height, palette.getSurface());
			Skia.drawLine(x, itemY + height, x + width, itemY + height, 1, palette.getOutline());
			Skia.drawText(I18n.get(title), x + 16, itemY + 12, palette.getOnSurface(), Fonts.getRegular(16));
			Skia.drawText(I18n.get(description), x + 16, itemY + 28, palette.getOnSurfaceVariant(), Fonts.getRegular(14));
		} else {
			height = 68;
			Skia.drawRoundedRect(x, itemY, width, height, 18, palette.getSurface());
			Skia.drawFullCenteredText(icon, x + 30, itemY + (height / 2), palette.getOnSurface(), Fonts.getIcon(32));
			Skia.drawText(I18n.get(title), x + 52, itemY + 20, palette.getOnSurface(), Fonts.getRegular(17));
			Skia.drawText(I18n.get(description), x + 52, itemY + 37, palette.getOnSurfaceVariant(), Fonts.getRegular(14));
		}

		if (component != null) {
			component.draw(mouseX, mouseY);
		}
	}

	@Override
	public void mousePressed(double mouseX, double mouseY, int button) {

		if (component != null) {
			component.mousePressed(mouseX, mouseY, button);
		}
	}

	@Override
	public void mouseReleased(double mouseX, double mouseY, int button) {

		if (component != null) {
			component.mouseReleased(mouseX, mouseY, button);
		}
	}

	@Override
	public void charTyped(char chr, int modifiers) {

		if (component != null) {
			component.charTyped(chr, modifiers);
		}
	}

	@Override
	public void keyPressed(int keyCode, int scanCode, int modifiers) {

		if (component != null) {
			component.keyPressed(keyCode, scanCode, modifiers);
		}
	}

	public String getTitle() {
		return title;
	}

	public String getDescription() {
		return description;
	}

	public String getIcon() {
		return icon;
	}
}
