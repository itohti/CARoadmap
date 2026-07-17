package com.caroadmap.ui;

import com.caroadmap.CombatSession;
import com.caroadmap.data.Task;
import com.caroadmap.data.TaskType;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;

public class SpeedCombatSection extends JPanel {
    private final JLabel timerLabel = new JLabel();
    private final JPanel targetsPanel = new JPanel();

    public SpeedCombatSection()
    {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        JLabel title = new JLabel("Speed Tasks");
        title.setFont(FontManager.getRunescapeBoldFont());
        title.setAlignmentX(Component.CENTER_ALIGNMENT);


        timerLabel.setFont(
                FontManager.getRunescapeBoldFont()
                        .deriveFont(28f)
        );
        timerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);


        targetsPanel.setLayout(
                new BoxLayout(targetsPanel, BoxLayout.Y_AXIS)
        );
        targetsPanel.setAlignmentX(Component.CENTER_ALIGNMENT);


        add(title);
        add(Box.createRigidArea(new Dimension(0, 5)));
        add(timerLabel);
        add(Box.createRigidArea(new Dimension(0, 10)));
        add(targetsPanel);
    }


    public void update(CombatSession session)
    {
        renderTimer(session);
        renderTargets(session);
    }


    private void renderTimer(CombatSession session)
    {
        long elapsed = session.getElapsedSeconds();

        timerLabel.setText(
                formatTime(elapsed)
        );
    }


    private void renderTargets(CombatSession session)
    {
        targetsPanel.removeAll();

        if (session.getTasks() == null) {
            return;
        }


        List<Task> speedTasks =
                session.getTasks()
                        .stream()
                        .filter(t -> t.getType() == TaskType.SPEED)
                        .collect(Collectors.toList());


        for (Task task : speedTasks)
        {
            JLabel taskLabel = new JLabel(
                    formatTask(task, session)
            );

            taskLabel.setFont(
                    FontManager.getRunescapeSmallFont()
            );


            if (hasFailed(task, session))
            {
                taskLabel.setForeground(Color.RED);
            }
            else
            {
                taskLabel.setForeground(Color.GREEN);
            }


            targetsPanel.add(taskLabel);
        }


        targetsPanel.revalidate();
        targetsPanel.repaint();
    }


    private boolean hasFailed(Task task, CombatSession session)
    {
        return session.getElapsedSeconds()
                > task.getTargetTimeSeconds();
    }


    private String formatTime(long seconds)
    {
        return String.format(
                "%02d:%02d",
                seconds / 60,
                seconds % 60
        );
    }


    private String formatTask(Task task, CombatSession session)
    {
        long target =
                task.getTargetTimeSeconds().longValue();


        return String.format(
                "%s %s - %d:%02d",
                hasFailed(task, session)
                        ? "✗"
                        : "✓",
                task.getTaskName(),
                target / 60,
                target % 60
        );
    }
}