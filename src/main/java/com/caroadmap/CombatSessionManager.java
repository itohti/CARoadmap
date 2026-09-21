package com.caroadmap;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.NPC;

@Slf4j
public class CombatSessionManager {
    @Getter
    private CombatSession currentSession;

    public void startSession(NPC boss, boolean instanced) {
        currentSession = new CombatSession(boss, instanced);

        log.info(
                "Started combat session: {}",
                boss.getName()
        );
    }

    public void endSession() {
        if (currentSession != null)
        {
            currentSession.end();
            log.info(
                    "Ending combat session: {}",
                    currentSession.getBossName()
            );
        }

        currentSession = null;
    }

    public void updateKillCount(int killCount)
    {
        if (currentSession == null)
        {
            return;
        }

        currentSession.updateKillCount(killCount);
    }
}
