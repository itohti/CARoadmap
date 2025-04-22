package com.caroadmap.ui;

import com.caroadmap.CARoadmapConfig;
import com.caroadmap.CARoadmapPlugin;
import com.caroadmap.data.RecommendTasks;
import com.caroadmap.data.SortingType;
import com.caroadmap.data.Task;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import net.runelite.api.Client;
import net.runelite.client.config.ConfigManager;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class CARoadmapPanel extends PluginPanel{
    @Setter
    private RecommendTasks recommendTasks;
    @Setter
    private String username;
    private ConfigManager configManager;
    private ArrayList<Task> recommendedList;
    private final Set<Task> completedList;
    private final JPanel recommendedContainer;
    private final JPanel completedContainer;
    private final SpriteManager spriteManager;
    private Boolean ascending;
    private SortingType sortingType;
    @Inject
    public CARoadmapPanel(SpriteManager spriteManager, ConfigManager configManager) {
        super(false);
        this.spriteManager = spriteManager;
        this.configManager = configManager;

        this.ascending = configManager.getConfiguration("CARoadmap", "isAscending", Boolean.class);
        if (this.ascending == null) {
            this.ascending = true;
            configManager.setConfiguration("CARoadmap", "isAscending", this.ascending);
        }

        this.sortingType = configManager.getConfiguration("CARoadmap", "sortingType", SortingType.class);
        if (this.sortingType == null) {
            this.sortingType = SortingType.SCORE;
            configManager.setConfiguration("CARoadmap", "sortingType", this.sortingType);
        }

        this.recommendedList = new ArrayList<>();
        this.completedList = new HashSet<>();
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

        JPanel recommendedTitlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0,0));
        recommendedTitlePanel.setOpaque(false);

        JLabel recommendedTitle = new JLabel("Recommended: ");
        recommendedTitle.setFont(FontManager.getRunescapeBoldFont());
        recommendedTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        recommendedTitlePanel.add(recommendedTitle);
        recommendedTitlePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, recommendedTitle.getPreferredSize().height));

        JPanel completedTitlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0,0));
        completedTitlePanel.setOpaque(false);

        JLabel completedTitle = new JLabel("Completed: ");
        completedTitle.setFont(FontManager.getRunescapeBoldFont());
        completedTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        completedTitlePanel.add(completedTitle);
        completedTitlePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, completedTitle.getPreferredSize().height));

        final BufferedImage refresh = ImageUtil.loadImageResource(CARoadmapPlugin.class, "/update_icon.png");
        JComboBox<String> sortDropdown = getSortByMenu();
        JButton recommendationBtn = getRecommendationBtn(refresh);
        final BufferedImage ascendingIcon = ImageUtil.loadImageResource(CARoadmapPlugin.class, "/a-z.png");
        final BufferedImage descendingIcon = ImageUtil.loadImageResource(CARoadmapPlugin.class, "/z-a.png");
        JButton sortingButton = getSortingButton(ascendingIcon, descendingIcon);

        toolbar.add(sortDropdown);
        toolbar.add(sortingButton);
        toolbar.add(recommendationBtn);

        this.recommendedContainer = new JPanel();
        recommendedContainer.setLayout(new BoxLayout(recommendedContainer, BoxLayout.Y_AXIS));
        recommendedContainer.setBorder(new EmptyBorder(5, 5, 5, 5));

        recommendedContainer.add(Box.createVerticalGlue());

        JScrollPane scrollPane = new JScrollPane(recommendedContainer);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setPreferredSize(new Dimension(0, 300));
        scrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, 300));

        this.completedContainer = new JPanel();
        completedContainer.setLayout(new BoxLayout(completedContainer, BoxLayout.Y_AXIS));
        completedContainer.setBorder(new EmptyBorder(5,5,5,5));

        completedContainer.add(Box.createVerticalGlue());

        JScrollPane completedScroll = new JScrollPane(completedContainer);
        completedScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        completedScroll.setPreferredSize(new Dimension(0, 300));
        completedScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 300));

        this.add(titleLabel);
        this.add(toolbar);
        this.add(Box.createRigidArea(new Dimension(0,10)));
        this.add(recommendedTitlePanel);
        this.add(scrollPane);
        this.add(Box.createRigidArea(new Dimension(0,10)));
        this.add(completedTitlePanel);
        this.add(completedScroll);
    }

    private JButton getSortingButton(BufferedImage ascendingIcon, BufferedImage descendingIcon) {
        JButton acnOrDsc = new JButton(new ImageIcon(ascending != null && ascending ? ascendingIcon : descendingIcon));
        acnOrDsc.setToolTipText(ascending ? "ascending" : "descending");
        acnOrDsc.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (ascending == null) {
                    ascending = true;
                }
                ascending = !ascending;
                configManager.setConfiguration("CARoadmap", "isAscending", ascending);
                acnOrDsc.setIcon(ascending ? new ImageIcon(ascendingIcon) : new ImageIcon(descendingIcon));
                acnOrDsc.setToolTipText(ascending ? "ascending" : "descending");
                recommendTasks.setAscending(!recommendTasks.isAscending());
                recommendTasks.getRecommendations(username, 1014);
                refresh();
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
                    refresh();
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
        if (sortingType != null) {
            if (sortingType == SortingType.SCORE) {
                sortDropdown.setSelectedItem("Recommended");
            }
            else if (sortingType == SortingType.TIER) {
                sortDropdown.setSelectedItem("Tier");
            }
            else if (sortingType == SortingType.BOSS) {
                sortDropdown.setSelectedItem("Boss");
            }
        }
        sortDropdown.addActionListener(e -> {
            String selected = (String) sortDropdown.getSelectedItem();
            if (selected != null) {
                if (selected.equals("Recommended")) {
                    recommendTasks.setSortingType(SortingType.valueOf("SCORE"));
                    configManager.setConfiguration("CARoadmap", "sortingType", SortingType.valueOf("SCORE"));
                }
                else {
                    recommendTasks.setSortingType(SortingType.valueOf(selected.toUpperCase()));
                    configManager.setConfiguration("CARoadmap", "sortingType", SortingType.valueOf(selected.toUpperCase()));
                }
                recommendTasks.getRecommendations(username, 1014);
                refresh();
            }
        });
        sortDropdown.setMaximumSize(new Dimension(150, 25));
        sortDropdown.setPreferredSize(new Dimension(150, 25));
        sortDropdown.setFont(FontManager.getRunescapeSmallFont());

        return sortDropdown;
    }

    public void refresh() {
        this.recommendedList = recommendTasks.getRecommendedTasks();
        separateCompletedTasks();
        updateTaskDisplay();
    }

    public boolean taskCompleted(String taskName) {
        Task toRemove = null;
        for (Task task: recommendedList) {
            if (task.getTaskName().equals(taskName)) {
                task.setDone(true);
                toRemove = task;
                completedList.add(task);
            }
        }

        if (toRemove != null) {
            recommendedList.remove(toRemove);
            return true;
        }

        return false;
    }

    public void separateCompletedTasks() {
        List<Task> completed = recommendedList.stream()
                .filter(Task::isDone)
                .collect(Collectors.toList());

        recommendedList.removeAll(completed);
        completedList.addAll(completed);
    }

    public void updateTaskDisplay() {
        recommendedContainer.removeAll();
        completedContainer.removeAll();
        for (Task task : recommendedList) {
            recommendedContainer.add(new DisplayTask(task, spriteManager));
            recommendedContainer.add(Box.createRigidArea(new Dimension(0, 5)));
        }
        for (Task task: completedList) {
            completedContainer.add(new DisplayTask(task, spriteManager));
            completedContainer.add(Box.createRigidArea(new Dimension(0, 5)));
        }
        recommendedContainer.revalidate();
        recommendedContainer.repaint();
        completedContainer.revalidate();
        completedContainer.repaint();
    }
}
