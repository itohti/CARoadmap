package com.caroadmap.ui;

import com.caroadmap.data.Task;
import net.runelite.client.game.SpriteManager;

import javax.swing.*;
import java.awt.*;

public class SpeedDisplayTask extends DisplayTask{
    public SpeedDisplayTask(Task task, SpriteManager spriteManager)
    {
        super(task, spriteManager);
    }

    @Override
    protected void buildDetailPanel(JPanel detailPanel) {
        super.buildDetailPanel(detailPanel);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;

        int row = detailPanel.getComponentCount();

        addRow(
                detailPanel,
                gbc,
                row++,
                new JLabel("Target Time"),
                new JLabel(formatTime(task.getTargetTimeSeconds()))
        );

        if (task.getHasPb())
        {
            addRow(
                    detailPanel,
                    gbc,
                    row++,
                    new JLabel("Your Best"),
                    new JLabel(formatTime(task.getPlayerTimeSeconds()))
            );

            addRow(
                    detailPanel,
                    gbc,
                    row++,
                    new JLabel("Need to Save"),
                    new JLabel(formatSecondsToSave(task))
            );
        }
        else
        {
            addRow(
                    detailPanel,
                    gbc,
                    row++,
                    new JLabel("Your Best"),
                    new JLabel("Not Available")
            );
        }
    }

    private String formatTime(double totalSeconds)
    {
        int minutes = (int) totalSeconds / 60;
        int seconds = (int) totalSeconds % 60;

        return String.format("%d:%02d", minutes, seconds);
    }

    private String formatSecondsToSave(Task task)
    {
        double seconds = task.getSecondsToSave();

        if (seconds <= 0)
        {
            return "Completed";
        }

        return String.format("%.0f sec", Math.ceil(seconds));
    }
}
