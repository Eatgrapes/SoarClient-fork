package com.soarclient.mixin.interfaces;

import net.minecraft.world.InteractionHand;

public interface IMixinLivingEntity {
	void fakeSwingHand(InteractionHand hand);
}
