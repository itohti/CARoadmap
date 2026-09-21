package com.caroadmap.data;

import net.runelite.api.Client;
import net.runelite.api.gameval.VarbitID;

import java.util.EnumMap;
import java.util.Map;

/**
 * Reads the player's Combat Achievement points progress straight from the game
 * client via varbits.
 * <p>
 * Every method here reads a varbit, so they MUST be called on the client thread
 * (from a {@code GameTick}/event handler, or inside {@code ClientThread.invoke}).
 * The varbits are populated on login; if a threshold reads {@code 0} it has not
 * synced yet and the call should be retried on a later tick.
 */
public final class CombatAchievementProgress {

    private CombatAchievementProgress() {
    }

    /**
     * The player's current total Combat Achievement points (sum across every
     * completed task).
     */
    public static int getCurrentPoints(Client client) {
        return client.getVarbitValue(VarbitID.CA_POINTS);
    }

    /**
     * Points required to unlock the given reward tier. Returns {@code 0} if the
     * client has not synced the threshold yet.
     */
    public static int getThreshold(Client client, RewardTier tier) {
        return client.getVarbitValue(tier.getThresholdVarbitId());
    }

    /**
     * All six reward-tier thresholds, in unlock order.
     */
    public static Map<RewardTier, Integer> getAllThresholds(Client client) {
        Map<RewardTier, Integer> thresholds = new EnumMap<>(RewardTier.class);
        for (RewardTier tier : RewardTier.values()) {
            thresholds.put(tier, getThreshold(client, tier));
        }
        return thresholds;
    }

    /**
     * Points the player still needs to reach {@code tier}. {@code 0} once the
     * tier is reached (or if the threshold has not synced yet).
     */
    public static int pointsNeededFor(Client client, RewardTier tier) {
        int gap = getThreshold(client, tier) - getCurrentPoints(client);
        return Math.max(gap, 0);
    }

    /**
     * The highest reward tier the player has already unlocked, or {@code null}
     * if they have not reached Easy yet (or the thresholds have not synced).
     * {@code RewardTier.values()} is in ascending unlock order, so the last
     * match wins.
     */
    public static RewardTier getCompletedTier(Client client) {
        int points = getCurrentPoints(client);
        RewardTier completed = null;
        for (RewardTier tier : RewardTier.values()) {
            int threshold = getThreshold(client, tier);
            if (threshold > 0 && points >= threshold) {
                completed = tier;
            }
        }
        return completed;
    }
}
