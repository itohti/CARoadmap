package com.caroadmap.ui;

import com.caroadmap.data.Task;
import net.runelite.client.game.SpriteManager;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.FontManager;

import javax.swing.*;
import java.awt.*;

@Slf4j
public class KillCountDisplayTask extends DisplayTask{
    public KillCountDisplayTask(Task task, SpriteManager spriteManager)
    {
        super(task, spriteManager);
    }

    @Override
    protected void buildDetailPanel(JPanel detailPanel)
    {
        super.buildDetailPanel(detailPanel);

        GridBagConstraints gbc = new GridBagConstraints();

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;

        int row = 3;

        JProgressBar progressBar =
                new JProgressBar(
                        0,
                        task.getRequiredKills().intValue()
                );

        progressBar.setValue(task.getCurrentKills().intValue());
        progressBar.setStringPainted(true);

        progressBar.setString(
                task.getCurrentKills().intValue()
                        + " / "
                        + task.getRequiredKills().intValue()
        );

        progressBar.setFont(FontManager.getRunescapeSmallFont());
        progressBar.setPreferredSize(new Dimension(200, 20));

        addFullWidthRow(detailPanel, gbc, row++, progressBar);

        JLabel remainingLabel = new JLabel("Remaining");

        JLabel remainingValue =
                new JLabel(task.getKillsRemaining().intValue() + " kills");

        addRow(detailPanel, gbc, row++, remainingLabel, remainingValue);
    }
}
