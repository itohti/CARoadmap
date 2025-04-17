package com.caroadmap.ui;

import com.caroadmap.Task;
import com.caroadmap.TaskType;
import com.google.api.Http;
import lombok.extern.slf4j.Slf4j;

import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

import javax.inject.Inject;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest;
import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
public class CARoadmapPanel extends PluginPanel{
    private List<Task> taskList = new ArrayList<>();
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
        JButton recommendationBtn = new JButton("Refresh Recommendations");
        recommendationBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("Something to work on later.");
            }
        });

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
        this.add(recommendationBtn);
        this.add(Box.createRigidArea(new Dimension(0,10)));
        this.add(taskHeader);
        this.add(scrollPane);
    }
}
