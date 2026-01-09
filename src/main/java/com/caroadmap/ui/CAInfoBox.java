package com.caroadmap.ui;

import com.caroadmap.dto.TaskDTO;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.ui.overlay.infobox.InfoBox;
import net.runelite.client.ui.overlay.infobox.InfoBoxPriority;

import javax.inject.Inject;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * This class will be created when a player clicks on a boss. It will display some QOL information for various tasks.
 * If it's a speed run task it will display how much time elapsed and the amount of time left to complete the task.
 */
public class CAInfoBox extends InfoBox {
    private final TaskDTO task;

    public CAInfoBox(BufferedImage image, Plugin plugin, TaskDTO task) {
        super(image, plugin);
        this.task = task;

        setPriority(InfoBoxPriority.MED);
        setTooltip(buildTooltip());
    }

    @Override
    public String getText() {
        return task.getType();
    }

    @Override
    public Color getTextColor() {
        return Color.WHITE;
    }

    private String buildTooltip() {
        return task.getDescription();
    }
}
