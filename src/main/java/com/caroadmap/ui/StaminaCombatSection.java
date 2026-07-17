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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class StaminaCombatSection extends JPanel
{
    private static final Pattern STREAK_PATTERN =
            Pattern.compile("(\\d+)\\s+times?\\s+without",
                    Pattern.CASE_INSENSITIVE);

    private final JLabel taskLabel = new JLabel();

    private final JProgressBar progressBar =
            new JProgressBar();

    public StaminaCombatSection()
    {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        JLabel title = new JLabel("Stamina Tasks");
        title.setFont(FontManager.getRunescapeBoldFont());
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        taskLabel.setFont(FontManager.getRunescapeSmallFont());
        taskLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        progressBar.setMaximum(100);
        progressBar.setStringPainted(true);
        progressBar.setAlignmentX(Component.CENTER_ALIGNMENT);

        add(title);
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
        taskLabel.setText("");

        if (session.getTasks() == null)
        {
            setVisible(false);
            return;
        }

        List<Task> staminaTasks =
                session.getTasks()
                        .stream()
                        .filter(t -> t.getType() == TaskType.STAMINA)
                        .collect(Collectors.toList());

        if (staminaTasks.isEmpty())
        {
            setVisible(false);
            return;
        }

        Task nextTask =
                staminaTasks.stream()
                        .filter(t -> getRequiredStreak(t) != null)
                        .filter(t ->
                                session.getKillStreak() < getRequiredStreak(t))
                        .min(Comparator.comparingInt(this::getRequiredStreak))
                        .orElse(null);

        // Player has completed every stamina task.
        if (nextTask == null)
        {
            setVisible(false);
            return;
        }

        setVisible(true);

        int required = getRequiredStreak(nextTask);
        int current = session.getKillStreak();

        double ratio = (double) current / required;

        progressBar.setValue(
                (int) (Math.min(ratio, 1.0) * 100)
        );

        progressBar.setString(
                String.format("%d / %d", current, required)
        );

        taskLabel.setText(nextTask.getTaskName());

        if (!session.isStreakValid())
        {
            progressBar.setValue(0);
            progressBar.setString("Streak Broken");
        }

        revalidate();
        repaint();
    }

    private Integer getRequiredStreak(Task task)
    {
        Matcher matcher =
                STREAK_PATTERN.matcher(task.getTaskDescription());

        if (!matcher.find())
        {
            return null;
        }

        return Integer.parseInt(matcher.group(1));
    }
}