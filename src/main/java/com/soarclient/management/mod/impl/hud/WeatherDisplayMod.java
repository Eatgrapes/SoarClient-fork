package com.soarclient.management.mod.impl.hud;

import com.soarclient.event.EventBus;
import com.soarclient.event.client.RenderSkiaEvent;
import com.soarclient.management.mod.api.hud.SimpleHUDMod;
import com.soarclient.skia.font.Icon;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;

public class WeatherDisplayMod extends SimpleHUDMod {

	public WeatherDisplayMod() {
		super("mod.weatherdisplay.name", "mod.weatherdisplay.description", Icon.SUNNY);
	}

	public final EventBus.EventListener<RenderSkiaEvent> onRenderSkia = event -> {
		this.draw();
	};

	@Override
	public String getText() {

		String prefix = "Weather: ";
		ClientLevel world = client.level;
		LocalPlayer player = client.player;
		BlockPos playerPos = player.blockPosition();
		Holder<Biome> biomeEntry = world.getBiome(playerPos);

		if (world.isThundering()) {
			return prefix + "Thundering";
		}
		
		if (world.isRaining()) {
			if (biomeEntry.value().getPrecipitationAt(playerPos, world.getSeaLevel()).equals(Biome.Precipitation.SNOW)) {
				return prefix + "Snowing";
			} else {
				return prefix + "Raining";
			}
		}

		return prefix + "Cleaning";
	}

	@Override
	public String getIcon() {

		ClientLevel world = client.level;
		LocalPlayer player = client.player;
		BlockPos playerPos = player.blockPosition();
		Holder<Biome> biomeEntry = world.getBiome(playerPos);

		String iconFont = Icon.SUNNY;

		if (world.isThundering()) {
			iconFont = Icon.THUNDERSTORM;
		}
		
		if (world.isRaining()) {
			if (biomeEntry.value().getPrecipitationAt(playerPos, world.getSeaLevel()).equals(Biome.Precipitation.SNOW)) {
				iconFont = Icon.WEATHER_SNOWY;
			} else {
				iconFont = Icon.RAINY;
			}
		}

		return iconFont;
	}
}