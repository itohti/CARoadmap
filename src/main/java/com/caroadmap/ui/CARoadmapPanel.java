package com.caroadmap.ui;

import com.caroadmap.CARoadmapPlugin;
import com.caroadmap.data.RecommendTasks;
import com.caroadmap.data.SortingType;
import com.caroadmap.data.Task;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
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
    private boolean ascending = true;
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

        JPanel toolbar = new JPanel();
        toolbar.setLayout(new BoxLayout(toolbar, BoxLayout.X_AXIS));
        toolbar.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        toolbar.setBorder(new EmptyBorder(5, 5, 5, 5));

        JLabel taskHeader = new JLabel("Recommended Tasks: ");
        taskHeader.setFont(FontManager.getRunescapeBoldFont());

        final BufferedImage refresh = ImageUtil.loadImageResource(CARoadmapPlugin.class, "/update_icon.png");
        JComboBox<String> sortDropdown = getSortByMenu();
        JButton recommendationBtn = getRecommendationBtn(refresh);
        final BufferedImage ascendingIcon = ImageUtil.loadImageResource(CARoadmapPlugin.class, "/a-z.png");
        final BufferedImage descendingIcon = ImageUtil.loadImageResource(CARoadmapPlugin.class, "/z-a.png");
        JButton sortingButton = getSortingButton(ascendingIcon, descendingIcon);

        toolbar.add(sortDropdown);
        toolbar.add(sortingButton);
        toolbar.add(recommendationBtn);

        this.taskContainer = new JPanel();
        taskContainer.setLayout(new BoxLayout(taskContainer, BoxLayout.Y_AXIS));
        taskContainer.setBorder(new EmptyBorder(5, 5, 5, 5));

        taskContainer.add(Box.createVerticalGlue());

        JScrollPane scrollPane = new JScrollPane(taskContainer);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        this.add(titleLabel);
        this.add(toolbar);
        this.add(Box.createRigidArea(new Dimension(0,10)));
        this.add(taskHeader);
        this.add(scrollPane);
    }

    private JButton getSortingButton(BufferedImage ascendingIcon, BufferedImage descendingIcon) {
        JButton acnOrDsc = new JButton(new ImageIcon(ascendingIcon));
        acnOrDsc.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ascending = !ascending;
                acnOrDsc.setIcon(ascending ? new ImageIcon(ascendingIcon) : new ImageIcon(descendingIcon));
                acnOrDsc.setToolTipText(ascending ? "ascending" : "descending");
                recommendTasks.setAscending(!recommendTasks.isAscending());
                recommendTasks.getRecommendations(username, 1014);
                refresh(username);
            }
        });
        acnOrDsc.setPreferredSize(new Dimension(24, 24));
        return acnOrDsc;
    }

    private JButton getRecommendationBtn(BufferedImage refresh) {
        JButton recommendationBtn = new JButton(new ImageIcon(refresh));
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
        recommendationBtn.setPreferredSize(new Dimension(24, 24));
        recommendationBtn.setToolTipText("Refresh Recommendations");
        return recommendationBtn;
    }

    private JComboBox<String> getSortByMenu() {
        String[] options = { "Recommended", "Tier", "Boss" };
        JComboBox<String> sortDropdown = new JComboBox<>(options);
        sortDropdown.addActionListener(e -> {
            String selected = (String) sortDropdown.getSelectedItem();
            if (selected.equals("Recommended")) {
                recommendTasks.setSortingType(SortingType.valueOf("SCORE"));
            }
            else {
                recommendTasks.setSortingType(SortingType.valueOf(selected.toUpperCase()));
            }
            recommendTasks.getRecommendations(username, 1014);
            refresh(username);
        });
        sortDropdown.setMaximumSize(new Dimension(150, 25));
        sortDropdown.setPreferredSize(new Dimension(150, 25));
        sortDropdown.setFont(FontManager.getRunescapeSmallFont());

        return sortDropdown;
    }

    public void refresh(String username) {
        this.taskList = recommendTasks.getRecommendedTasks();
        updateTaskDisplay();
    }

    public boolean removeTask(String taskName) {
        for (Task task: taskList) {
            if (task.getTaskName().equals(taskName)) {
                // maybe we can just make sure done tasks are at the bottom so the user can see progress. TODO
                taskList.remove(task);
                return true;
            }
        }

        return false;
    }

    public void updateTaskDisplay() {
        taskContainer.removeAll();
        for (Task task : taskList) {
            taskContainer.add(new DisplayTask(task, spriteManager));
            taskContainer.add(Box.createRigidArea(new Dimension(0, 5)));
        }
        taskContainer.revalidate();
        taskContainer.repaint();
    }
}
