package com.caroadmap.ui;

import lombok.Setter;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.ui.overlay.infobox.Counter;

import java.awt.*;
import java.awt.image.BufferedImage;

public class CASpeedCounter extends Counter {
    private final long startTime;
    private final int timeLimitSeconds;

    public CASpeedCounter(BufferedImage image, Plugin plugin, int timeLimitSeconds, String taskTitle) {
        super(image, plugin, timeLimitSeconds);
        this.startTime = System.currentTimeMillis();
        this.timeLimitSeconds = timeLimitSeconds;

        setTooltip(taskTitle);
    }

    @Override
    public int getCount() {
        long elapsedMillis = System.currentTimeMillis() - startTime;
        int elapsedSeconds = (int) (elapsedMillis / 1000);

        int remaining = timeLimitSeconds - elapsedSeconds;
        return Math.max(remaining, 0);
    }

    @Override
    public Color getTextColor() {
        int remaining = getCount();

        if (remaining <= 0) {
            return Color.RED;
        }
        else if (remaining < 10) {
            return Color.YELLOW;
        }

        return Color.WHITE;
    }
}
