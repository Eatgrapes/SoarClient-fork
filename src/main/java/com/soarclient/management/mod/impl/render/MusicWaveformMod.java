package com.soarclient.management.mod.impl.render;

import com.soarclient.Soar;
import com.soarclient.event.EventBus;
import com.soarclient.event.client.RenderSkiaEvent;
import com.soarclient.management.mod.Mod;
import com.soarclient.management.mod.ModCategory;
import com.soarclient.management.music.Music;
import com.soarclient.management.music.MusicManager;
import com.soarclient.management.music.MusicPlayer;
import com.soarclient.skia.Skia;
import com.soarclient.skia.font.Icon;
import com.soarclient.utils.ColorUtils;

public class MusicWaveformMod extends Mod {

	public MusicWaveformMod() {
		super("mod.musicwaveform.name", "mod.musicwaveform.description", Icon.AIRWAVE, ModCategory.RENDER);
	}

	public EventBus.EventListener<RenderSkiaEvent> onRenderSkia = event -> {

		MusicManager musicManager = Soar.getInstance().getMusicManager();
		Music m = musicManager.getCurrentMusic();

		int offsetX = 0;

		if (musicManager.isPlaying()) {
			for (int i = 0; i < MusicPlayer.SPECTRUM_BANDS; i++) {
				MusicPlayer.ANIMATIONS[i].onTick(MusicPlayer.VISUALIZER[i], 15);
				Skia.drawRect(offsetX, client.getWindow().getGuiScaledHeight() + (MusicPlayer.ANIMATIONS[i].getValue() * 0.6f), 19,
						client.getWindow().getGuiScaledHeight(), ColorUtils.applyAlpha(m.getColor(), 80));

				offsetX += 19;
			}
		}
	};
}
