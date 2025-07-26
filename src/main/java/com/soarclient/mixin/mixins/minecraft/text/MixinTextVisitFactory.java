package com.soarclient.mixin.mixins.minecraft.text;

import com.soarclient.Soar;
import com.soarclient.management.mod.impl.misc.NameProtectMod;
import net.minecraft.text.TextVisitFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(TextVisitFactory.class)
public class MixinTextVisitFactory {

    @ModifyArg(
        method = "visitFormatted(Ljava/lang/String;ILnet/minecraft/text/Style;Lnet/minecraft/text/CharacterVisitor;)Z",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/text/TextVisitFactory;visitFormatted(Ljava/lang/String;ILnet/minecraft/text/Style;Lnet/minecraft/text/Style;Lnet/minecraft/text/CharacterVisitor;)Z",
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
