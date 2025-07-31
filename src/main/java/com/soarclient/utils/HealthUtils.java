package com.soarclient.utils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;

public class HealthUtils {
    private static final String[] HP_KEYWORDS = {"hp", "health", "♥", "lives"};

    public static float getActualHealth(LivingEntity entity, boolean fromScoreboard) {
        if (fromScoreboard) {
            Float health = getHealthFromScoreboard(entity);
            if (health != null) {
                return health;
            }
        }
        return entity.getHealth();
    }

    public static float getActualHealth(LivingEntity entity) {
        return getActualHealth(entity, true);
    }

    private static Float getHealthFromScoreboard(LivingEntity entity) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return null;

        Scoreboard scoreboard = client.world.getScoreboard();
        ScoreboardObjective objective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.BELOW_NAME);

        if (objective == null) return null;

        try {
            var score = objective.getScoreboard().getScore(entity, objective);
            if (score == null) return null;

            var displayName = objective.getDisplayName();

            if (score.getScore() <= 0 || displayName == null || !containsHealthKeyword(displayName.getString())) {
                return null;
            }

            return (float) score.getScore();
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean containsHealthKeyword(String text) {
        String lowerText = text.toLowerCase();
        for (String keyword : HP_KEYWORDS) {
            if (lowerText.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
