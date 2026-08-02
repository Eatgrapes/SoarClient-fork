package com.soarclient.mixin.mixins.minecraft.world;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.soarclient.management.mod.impl.misc.WeatherChangerMod;
import net.minecraft.world.level.Level;

@Mixin(Level.class)
public class MixinWorld {

	@Inject(method = "getRainLevel", at = @At("HEAD"), cancellable = true)
	public void getRainLevel(float delta, CallbackInfoReturnable<Float> cir) {
		
		WeatherChangerMod mod = WeatherChangerMod.getInstance();
		
		if(mod.isEnabled()) {
			if(mod.isRaining()) {
				cir.setReturnValue(1F);
			} else {
				cir.setReturnValue(0F);
			}
		}
	}
	
	@Inject(method = "isRaining", at = @At("HEAD"), cancellable = true)
	public void isRaining(CallbackInfoReturnable<Boolean> cir) {
		if(WeatherChangerMod.getInstance().isEnabled()) {
			cir.setReturnValue(WeatherChangerMod.getInstance().isRaining());
		}
	}
	
	@Inject(method = "getThunderLevel", at = @At("HEAD"), cancellable = true)
	public void getThunderLevel(float delta, CallbackInfoReturnable<Float> cir) {
		
		WeatherChangerMod mod = WeatherChangerMod.getInstance();
		
		if(mod.isEnabled()) {
			if(mod.isThundering()) {
				cir.setReturnValue(1F);
			} else {
				cir.setReturnValue(0F);
			}
		}
	}
	
	@Inject(method = "isThundering", at = @At("HEAD"), cancellable = true)
	public void isThundering(CallbackInfoReturnable<Boolean> cir) {
		if(WeatherChangerMod.getInstance().isEnabled()) {
			cir.setReturnValue(WeatherChangerMod.getInstance().isThundering());
		}
	}
}
