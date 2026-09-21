package com.caroadmap.data;

import net.runelite.api.gameval.VarbitID;

/**
 * Combat Achievement reward tiers, in unlock order. Each tier carries the id of
 * the varbit that holds the number of points required to unlock it.
 */
public enum RewardTier {
    EASY(VarbitID.CA_THRESHOLD_EASY, "Easy"),
    MEDIUM(VarbitID.CA_THRESHOLD_MEDIUM, "Medium"),
    HARD(VarbitID.CA_THRESHOLD_HARD, "Hard"),
    ELITE(VarbitID.CA_THRESHOLD_ELITE, "Elite"),
    MASTER(VarbitID.CA_THRESHOLD_MASTER, "Master"),
    GRANDMASTER(VarbitID.CA_THRESHOLD_GRANDMASTER, "Grandmaster");

    private final int thresholdVarbitId;
    private final String displayName;

    RewardTier(int thresholdVarbitId, String displayName) {
        this.thresholdVarbitId = thresholdVarbitId;
        this.displayName = displayName;
    }

    public int getThresholdVarbitId() {
        return thresholdVarbitId;
    }

    /** Label shown in the RuneLite config dropdown. */
    @Override
    public String toString() {
        return displayName;
    }
}
