package com.soarclient.mixin.mixins.minecraft.world;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.soarclient.management.mod.impl.misc.WeatherChangerMod;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;

@Mixin(Biome.class)
public class MixinBiome {

	@Inject(method = "getPrecipitation", at = @At("HEAD"), cancellable = true)
	public void getPrecipitation(BlockPos pos, int seaLevel, CallbackInfoReturnable<Biome.Precipitation> cir) {
		
		if(WeatherChangerMod.getInstance().isEnabled() && WeatherChangerMod.getInstance().isSnowing()) {
			cir.setReturnValue(Biome.Precipitation.SNOW);
		}
	}
}
