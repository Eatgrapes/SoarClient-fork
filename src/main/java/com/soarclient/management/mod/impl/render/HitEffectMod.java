package com.soarclient.management.mod.impl.render;

import com.soarclient.event.EventBus;
import com.soarclient.management.mod.Mod;
import com.soarclient.management.mod.ModCategory;
import com.soarclient.management.mod.settings.impl.BooleanSetting;
import com.soarclient.management.mod.settings.impl.ComboSetting;
import com.soarclient.management.mod.settings.impl.NumberSetting;
import com.soarclient.skia.font.Icon;

import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import java.util.Arrays;

public class HitEffectMod extends Mod {

    private static HitEffectMod instance;

    private BooleanSetting enabledSetting = new BooleanSetting("setting.particle.enabled",
        "setting.particle.enabled.description", Icon.VISIBILITY, this, true);

    private ComboSetting particleTypeSetting = new ComboSetting("setting.particle.type",
        "setting.particle.type.description", Icon.EXPLOSION, this,
        Arrays.asList("setting.blood", "setting.criticals", "setting.sharpness",
            "setting.totem", "setting.hearts", "setting.magic", "setting.none"),
        "setting.blood");

    private NumberSetting particleAmountSetting = new NumberSetting("setting.particle.amount",
        "setting.particle.amount.description", Icon.FILTER_5, this, 5, 1, 20, 1);

    private BooleanSetting soundEnabledSetting = new BooleanSetting("setting.sound.enabled",
        "setting.sound.enabled.description", Icon.VOLUME_UP, this, true);

    private ComboSetting soundTypeSetting = new ComboSetting("setting.sound.type",
        "setting.sound.type.description", Icon.MUSIC_NOTE, this,
        Arrays.asList("setting.sound.hit", "setting.sound.totem", "setting.sound.bell",
            "setting.sound.anvil", "setting.sound.none"),
        "setting.sound.hit");

    private BooleanSetting alwaysActiveSetting = new BooleanSetting("setting.always.active",
        "setting.always.active.description", Icon.CONTRAST, this, false);

    private BooleanSetting criticalOnlySetting = new BooleanSetting("setting.critical.only",
        "setting.critical.only.description", Icon.FLARE, this, false);

    private BooleanSetting enchantmentOnlySetting = new BooleanSetting("setting.enchantment.only",
        "setting.enchantment.only.description", Icon.FLARE, this, false);

    public HitEffectMod() {
        super("mod.hiteffect.name", "mod.hiteffect.description", Icon.FLARE, ModCategory.RENDER);
        instance = this;
    }

    public static HitEffectMod getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        EventBus.getInstance().register(this);
        registerFabricCallbacks();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        EventBus.getInstance().unregister(this);
    }

    private void registerFabricCallbacks() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClientSide() && player instanceof LocalPlayer) {
                if (!enabledSetting.isEnabled()) return InteractionResult.PASS;
                if (!(entity instanceof LivingEntity)) return InteractionResult.PASS;

                LocalPlayer clientPlayer = (LocalPlayer) player;
                boolean shouldTrigger = shouldTriggerEffect(clientPlayer, entity);
                if (shouldTrigger) {
                    spawnParticleEffect(entity);
                    playSoundEffect(entity);
                }
            }
            return InteractionResult.PASS;
        });
    }

    private boolean shouldTriggerEffect(LocalPlayer player, Entity target) {
        if (alwaysActiveSetting.isEnabled()) return true;

        boolean isCritical = criticalOnlySetting.isEnabled() &&
            player.getAttackStrengthScale(0.5F) > 0.9F &&
            !player.onGround() && !player.onClimbable() &&
            !player.isInWater() && !player.isPassenger() &&
            !player.isSprinting();

        boolean hasEnchantment = enchantmentOnlySetting.isEnabled() &&
            EnchantmentHelper.getItemEnchantmentLevel(client.level.registryAccess()
                    .lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS),
                player.getWeaponItem()) > 0;

        return isCritical || hasEnchantment ||
            (!criticalOnlySetting.isEnabled() && !enchantmentOnlySetting.isEnabled());
    }

    private void spawnParticleEffect(Entity target) {
        String particleType = particleTypeSetting.getOption();
        int amount = (int) particleAmountSetting.getValue();

        for (int i = 0; i < amount; i++) {
            switch (particleType) {
                case "setting.blood":
                    client.particleEngine.createTrackingEmitter(target, ParticleTypes.DAMAGE_INDICATOR);
                    break;
                case "setting.criticals":
                    client.particleEngine.createTrackingEmitter(target, ParticleTypes.CRIT);
                    break;
                case "setting.sharpness":
                    client.particleEngine.createTrackingEmitter(target, ParticleTypes.ENCHANTED_HIT);
                    break;
                case "setting.totem":
                    client.particleEngine.createTrackingEmitter(target, ParticleTypes.TOTEM_OF_UNDYING);
                    break;
                case "setting.hearts":
                    client.particleEngine.createTrackingEmitter(target, ParticleTypes.HEART);
                    break;
                case "setting.magic":
                    client.particleEngine.createTrackingEmitter(target, ParticleTypes.WITCH);
                    break;
            }
        }
    }

    private void playSoundEffect(Entity target) {
        if (!soundEnabledSetting.isEnabled()) return;

        String soundType = soundTypeSetting.getOption();

        switch (soundType) {
            case "setting.sound.hit":
                client.level.playLocalSound(target.getX(), target.getY(), target.getZ(),
                    SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS,
                    1.0F, 1.0F, false);
                break;
            case "setting.sound.totem":
                client.level.playLocalSound(target.getX(), target.getY(), target.getZ(),
                    SoundEvents.TOTEM_USE, SoundSource.PLAYERS,
                    1.0F, 1.0F, false);
                break;
            case "setting.sound.bell":
                client.level.playLocalSound(target.getX(), target.getY(), target.getZ(),
                    SoundEvents.BELL_BLOCK, SoundSource.PLAYERS,
                    1.0F, 1.0F, false);
                break;
            case "setting.sound.anvil":
                client.level.playLocalSound(target.getX(), target.getY(), target.getZ(),
                    SoundEvents.ANVIL_USE, SoundSource.PLAYERS,
                    1.0F, 1.0F, false);
                break;
        }
    }
}
