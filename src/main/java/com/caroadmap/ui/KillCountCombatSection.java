package com.caroadmap.ui;

import com.caroadmap.CombatSession;
import com.caroadmap.data.Task;
import com.caroadmap.data.TaskType;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

import javax.swing.*;
import java.awt.*;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class KillCountCombatSection extends JPanel {
    private final JLabel taskLabel = new JLabel();

    private final JProgressBar progressBar =
            new JProgressBar();


    public KillCountCombatSection()
    {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        JLabel titleLabel = new JLabel("Kill Count Tasks");
        titleLabel.setFont(FontManager.getRunescapeBoldFont());
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);


        taskLabel.setFont(
                FontManager.getRunescapeSmallFont()
        );
        taskLabel.setAlignmentX(Component.CENTER_ALIGNMENT);


        progressBar.setStringPainted(true);
        progressBar.setMaximum(100);
        progressBar.setAlignmentX(Component.CENTER_ALIGNMENT);
        progressBar.setPreferredSize(
                new Dimension(200, 20)
        );


        add(titleLabel);
        add(Box.createRigidArea(new Dimension(0, 5)));
        add(taskLabel);
        add(Box.createRigidArea(new Dimension(0, 5)));
        add(progressBar);
    }


    public void update(CombatSession session)
    {
        renderProgress(session);
    }


    private void renderProgress(CombatSession session)
    {
        if (session.getTasks() == null)
        {
            return;
        }


        List<Task> killTasks =
                session.getTasks()
                        .stream()
                        .filter(t ->
                                t.getType() == TaskType.KILL_COUNT
                        )
                        .collect(Collectors.toList());


        if (killTasks.isEmpty())
        {
            setVisible(false);
            return;
        }


        setVisible(true);


        Task closestTask =
                killTasks.stream()
                        .min(
                                Comparator.comparingDouble(
                                        Task::getKillProgressRatio
                                )
                        )
                        .orElse(null);


        if (closestTask == null)
        {
            return;
        }


        double progress =
                closestTask.getKillProgressRatio() * 100;


        progressBar.setValue(
                (int) progress
        );


        progressBar.setString(
                String.format(
                        "%.0f%% (%d / %d)",
                        progress,
                        closestTask.getCurrentKills().intValue(),
                        closestTask.getRequiredKills().intValue()
                )
        );


        taskLabel.setText(
                closestTask.getTaskName()
        );


        revalidate();
        repaint();
    }
}