package com.caroadmap.ui;

import lombok.Getter;
import lombok.Setter;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.ui.overlay.infobox.Counter;

import java.awt.*;
import java.awt.image.BufferedImage;

public class CAKillCounter extends Counter {
    private final int killsToComplete;

    @Getter
    public final String boss;
    @Setter
    private int killCount;

    public CAKillCounter(BufferedImage image, Plugin plugin, int killsToComplete, int killCount, String boss) {
        super(image, plugin, killsToComplete);

        this.killsToComplete = killsToComplete;
        this.killCount = killCount;
        this.boss = boss;
    }

    @Override
    public int getCount() {
        return Math.max(killsToComplete - killCount, 0);
    }

    @Override
    public Color getTextColor() {
        int remaining = getCount();
        if (remaining == 0) {
            return Color.GREEN;
        }

        return Color.WHITE;
    }
}
