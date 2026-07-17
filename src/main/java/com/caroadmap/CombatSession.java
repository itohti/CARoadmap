package com.caroadmap;

import com.caroadmap.data.Task;
import com.caroadmap.data.TaskType;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.NPC;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
public class CombatSession {
    @Getter
    private NPC boss;

    @Getter
    private String bossName;

    @Getter
    private boolean bossDefeated;

    @Getter
    private boolean streakValid = true;

    @Getter
    private int killStreak = 0;

    @Getter
    private final boolean instanced;

    @Getter
    @Setter
    private volatile List<Task> tasks;
    private final Set<String> failedTasks = new HashSet<>();

    private long startTime;
    private boolean active;
    private int idleTicks;

    private long finishTime;


    public CombatSession(NPC boss, boolean instanced)
    {
        this.boss = boss;
        this.bossName = boss.getName();
        this.startTime = System.currentTimeMillis();
        this.active = true;
        this.instanced = instanced;

        log.info("CombatSession created for {}", bossName);
    }

    public void updateBoss(NPC boss) {
        this.boss = boss;
    }

    public void updateKillCount(int killCount)
    {
        if (tasks == null)
        {
            return;
        }

        for (Task task : tasks)
        {
            if (task.getType() == TaskType.KILL_COUNT)
            {
                task.setCurrentKills((double) killCount);

                if (task.getRequiredKills() != null)
                {
                    double ratio =
                            killCount / task.getRequiredKills();

                    task.setKillProgressRatio(
                            Math.min(ratio, 1.0)
                    );

                    task.setKillsRemaining(
                            Math.max(
                                    task.getRequiredKills() - killCount,
                                    0
                            )
                    );
                }
            }
        }
    }

    public void failTask(String taskTitle)
    {
        failedTasks.add(taskTitle);
    }

    public boolean isFailed(Task task)
    {
        return failedTasks.contains(task.getTaskName());
    }

    public void invalidateStreak()
    {
        streakValid = false;
        killStreak = 0;
    }

    public void resetStreak()
    {
        streakValid = true;
        killStreak = 0;
    }

    public void incrementKillStreak() {
        killStreak++;
    }

    public void bossDefeated()
    {
        if (bossDefeated)
        {
            return;
        }

        finishTime = System.currentTimeMillis();
        bossDefeated = true;
    }

    public void end() {
        active = false;
    }

    public void heartbeat() {
        idleTicks = 0;
    }

    public boolean shouldEnd() {
        return idleTicks++ > 100;
    }

    public long getElapsedSeconds()
    {
        long endTime = bossDefeated
                ? finishTime
                : System.currentTimeMillis();

        return (endTime - startTime) / 1000;
    }
}
