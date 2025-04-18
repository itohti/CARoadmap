package com.caroadmap.ui;

import com.caroadmap.data.RecommendTasks;
import com.caroadmap.data.Task;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

import javax.inject.Inject;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
@Slf4j
public class CARoadmapPanel extends PluginPanel{
    @Setter
    private RecommendTasks recommendTasks;
    private ArrayList<Task> taskList;
    @Setter
    private String username;
    private JPanel taskContainer;
    private SpriteManager spriteManager;
    @Inject
    public CARoadmapPanel(SpriteManager spriteManager) {
        super(false);
        this.recommendTasks = recommendTasks;
        this.spriteManager = spriteManager;
        this.taskList = new ArrayList<>();
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
                if (username != null) {
                    refresh(username);
                }
                else {
                    log.error("Could not fetch recommendations. You need to be logged in.");
                }
            }
        });

        JLabel taskHeader = new JLabel("Combat Achievements To Complete: ");
        taskHeader.setFont(FontManager.getRunescapeBoldFont());
        this.taskContainer = new JPanel();
        taskContainer.setLayout(new BoxLayout(taskContainer, BoxLayout.Y_AXIS));
        taskContainer.setBorder(new EmptyBorder(5, 5, 5, 5));

        taskContainer.add(Box.createVerticalGlue());

        JScrollPane scrollPane = new JScrollPane(taskContainer);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        this.add(titleLabel);
        this.add(recommendationBtn);
        this.add(Box.createRigidArea(new Dimension(0,10)));
        this.add(taskHeader);
        this.add(scrollPane);
    }

    public void refresh(String username) {
        this.taskList = recommendTasks.getRecommendedTasks();
        updateTaskDisplay();
    }

    private void updateTaskDisplay() {
        taskContainer.removeAll();
        for (Task task : taskList) {
            taskContainer.add(new DisplayTask(task, spriteManager));
            taskContainer.add(Box.createRigidArea(new Dimension(0, 5)));
        }
        taskContainer.revalidate();
        taskContainer.repaint();
    }
}
