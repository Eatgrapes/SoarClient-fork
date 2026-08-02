package com.soarclient.mixin.mixins.minecraft.text;

import com.soarclient.Soar;
import com.soarclient.management.mod.impl.misc.NameProtectMod;
import net.minecraft.util.StringDecomposer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(StringDecomposer.class)
public class MixinTextVisitFactory {

    @ModifyArg(
        method = "iterateFormatted(Ljava/lang/String;ILnet/minecraft/network/chat/Style;Lnet/minecraft/util/FormattedCharSink;)Z",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/util/StringDecomposer;iterateFormatted(Ljava/lang/String;ILnet/minecraft/network/chat/Style;Lnet/minecraft/network/chat/Style;Lnet/minecraft/util/FormattedCharSink;)Z",
            ordinal = 0
        ),
        index = 0
    )
    private static String adjustText(String text) {
        if (Soar.getInstance().getModManager() == null) {
            return text;
        }

        NameProtectMod mod = Soar.getInstance().getModManager().getMod(NameProtectMod.class);
        if (mod != null && mod.isEnabled()) {
            return mod.replaceName(text);
        }
        return text;
    }
}
