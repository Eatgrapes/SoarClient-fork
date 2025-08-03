package com.soarclient.management.mod.impl.render;

import com.soarclient.event.EventBus;
import com.soarclient.management.mod.Mod;
import com.soarclient.management.mod.ModCategory;
import com.soarclient.management.mod.settings.impl.BooleanSetting;
import com.soarclient.management.mod.settings.impl.ComboSetting;
import com.soarclient.management.mod.settings.impl.NumberSetting;
import com.soarclient.skia.font.Icon;

import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.SoundEvents;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.ActionResult;

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
            if (world.isClient && player instanceof ClientPlayerEntity) {
                if (!enabledSetting.isEnabled()) return ActionResult.PASS;
                if (!(entity instanceof LivingEntity)) return ActionResult.PASS;

                ClientPlayerEntity clientPlayer = (ClientPlayerEntity) player;
                boolean shouldTrigger = shouldTriggerEffect(clientPlayer, entity);
                if (shouldTrigger) {
                    spawnParticleEffect(entity);
                    playSoundEffect(entity);
                }
            }
            return ActionResult.PASS;
        });
    }

    private boolean shouldTriggerEffect(ClientPlayerEntity player, Entity target) {
        if (alwaysActiveSetting.isEnabled()) return true;

        boolean isCritical = criticalOnlySetting.isEnabled() &&
            player.getAttackCooldownProgress(0.5F) > 0.9F &&
            !player.isOnGround() && !player.isClimbing() &&
            !player.isTouchingWater() && !player.hasVehicle() &&
            !player.isSprinting();

        boolean hasEnchantment = enchantmentOnlySetting.isEnabled() &&
            EnchantmentHelper.getLevel(client.world.getRegistryManager()
                    .getOrThrow(RegistryKeys.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS),
                player.getWeaponStack()) > 0;

        return isCritical || hasEnchantment ||
            (!criticalOnlySetting.isEnabled() && !enchantmentOnlySetting.isEnabled());
    }

    private void spawnParticleEffect(Entity target) {
        String particleType = particleTypeSetting.getOption();
        int amount = (int) particleAmountSetting.getValue();

        for (int i = 0; i < amount; i++) {
            switch (particleType) {
                case "setting.blood":
                    client.particleManager.addEmitter(target, ParticleTypes.DAMAGE_INDICATOR);
                    break;
                case "setting.criticals":
                    client.particleManager.addEmitter(target, ParticleTypes.CRIT);
                    break;
                case "setting.sharpness":
                    client.particleManager.addEmitter(target, ParticleTypes.ENCHANTED_HIT);
                    break;
                case "setting.totem":
                    client.particleManager.addEmitter(target, ParticleTypes.TOTEM_OF_UNDYING);
                    break;
                case "setting.hearts":
                    client.particleManager.addEmitter(target, ParticleTypes.HEART);
                    break;
                case "setting.magic":
                    client.particleManager.addEmitter(target, ParticleTypes.WITCH);
                    break;
            }
        }
    }

    private void playSoundEffect(Entity target) {
        if (!soundEnabledSetting.isEnabled()) return;

        String soundType = soundTypeSetting.getOption();

        switch (soundType) {
            case "setting.sound.hit":
                client.world.playSound(target.getX(), target.getY(), target.getZ(),
                    SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS,
                    1.0F, 1.0F, false);
                break;
            case "setting.sound.totem":
                client.world.playSound(target.getX(), target.getY(), target.getZ(),
                    SoundEvents.ITEM_TOTEM_USE, SoundCategory.PLAYERS,
                    1.0F, 1.0F, false);
                break;
            case "setting.sound.bell":
                client.world.playSound(target.getX(), target.getY(), target.getZ(),
                    SoundEvents.BLOCK_BELL_USE, SoundCategory.PLAYERS,
                    1.0F, 1.0F, false);
                break;
            case "setting.sound.anvil":
                client.world.playSound(target.getX(), target.getY(), target.getZ(),
                    SoundEvents.BLOCK_ANVIL_USE, SoundCategory.PLAYERS,
                    1.0F, 1.0F, false);
                break;
        }
    }
}
