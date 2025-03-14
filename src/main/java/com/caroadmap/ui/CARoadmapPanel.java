package com.caroadmap.ui;

import com.caroadmap.Task;
import com.caroadmap.TaskType;
import lombok.extern.slf4j.Slf4j;

import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

import javax.inject.Inject;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Arrays;
import java.util.List;

@Slf4j
public class CARoadmapPanel extends PluginPanel{
    @Inject
    public CARoadmapPanel() {
        super(false);
        // setting up layout
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(ColorScheme.DARK_GRAY_COLOR);
        setBorder(new EmptyBorder(5,5,5,5));

        // setting up title label
        JLabel titleLabel = new JLabel("CA Roadmap");
        titleLabel.setFont(FontManager.getRunescapeBoldFont());

        List<Task> taskList = Arrays.asList(
                new Task("Abyssal Sire", "Demonic Rebound", "Vengeance the boss", TaskType.MECHANICAL, 4, true)
        );
        JLabel taskHeader = new JLabel("Combat Achievements To Complete: ");
        taskHeader.setFont(FontManager.getRunescapeBoldFont());
        JPanel taskContainer = new JPanel();
        taskContainer.setLayout(new BoxLayout(taskContainer, BoxLayout.Y_AXIS));
        taskContainer.setBorder(new EmptyBorder(5, 5, 5, 5));

        for (Task task : taskList) {
            taskContainer.add(new DisplayTask(task));
            taskContainer.add(Box.createRigidArea(new Dimension(0, 5)));
        }

        taskContainer.add(Box.createVerticalGlue());

        JScrollPane scrollPane = new JScrollPane(taskContainer);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        this.add(titleLabel);
        this.add(Box.createRigidArea(new Dimension(0,10)));
        this.add(taskHeader);
        this.add(scrollPane);
    }
}
