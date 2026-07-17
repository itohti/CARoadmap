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

public class CombatChecklistSection extends JPanel
{
    private final JPanel taskPanel = new JPanel();

    public CombatChecklistSection()
    {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        JLabel title = new JLabel("Encounter Tasks");
        title.setHorizontalAlignment(SwingConstants.CENTER);
        title.setFont(FontManager.getRunescapeBoldFont());
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        title.setMaximumSize(new Dimension(Integer.MAX_VALUE, title.getPreferredSize().height));

        taskPanel.setLayout(new BoxLayout(taskPanel, BoxLayout.Y_AXIS));
        taskPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        taskPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        taskPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        add(title);
        add(Box.createRigidArea(new Dimension(0, 5)));
        add(taskPanel);
    }

    public void update(CombatSession session)
    {
        taskPanel.removeAll();

        if (session.getTasks() == null)
        {
            return;
        }

        List<Task> checklistTasks =
                session.getTasks()
                        .stream()
                        .filter(task ->
                                task.getType() == TaskType.MECHANICAL
                                        || task.getType() == TaskType.PERFECTION
                                        || task.getType() == TaskType.RESTRICTION)
                        .collect(Collectors.toList());

        if (checklistTasks.isEmpty())
        {
            setVisible(false);
            return;
        }

        setVisible(true);

        for (Task task : checklistTasks)
        {
            JPanel row = new JPanel(new BorderLayout());
            row.setOpaque(false);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));

            JLabel label = createTaskLabel(session, task);
            label.setHorizontalAlignment(SwingConstants.LEFT);

            row.add(label, BorderLayout.WEST);

            taskPanel.add(row);
            taskPanel.add(Box.createRigidArea(new Dimension(0, 2)));
        }

        revalidate();
        repaint();
    }

    private String formatTooltip(Task task)
    {
        return "<html><body style='width:250px;'>"
                + task.getTaskDescription()
                + "</body></html>";
    }

    private JLabel createTaskLabel(
            CombatSession session,
            Task task)
    {
        boolean failed = session.isFailed(task);

        JLabel label = new JLabel(
                (failed ? "✗ " : "✓ ") + task.getTaskName()
        );

        label.setFont(FontManager.getRunescapeSmallFont());

        label.setForeground(
                failed
                        ? Color.RED
                        : new Color(0, 200, 0)
        );

        label.setToolTipText(formatTooltip(task));

        return label;
    }
}