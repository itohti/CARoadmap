package com.caroadmap.ui;

import com.caroadmap.CARoadmapPlugin;
import com.caroadmap.data.RecommendTasks;
import com.caroadmap.data.SortingType;
import com.caroadmap.data.Task;
import com.caroadmap.data.TaskType;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

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
    public enum PanelMode
    {
        RECOMMENDATIONS,
        COMBAT
    }

    @Setter
    private PanelMode panelMode = PanelMode.RECOMMENDATIONS;

    private final JPanel mainContainer = new JPanel(new CardLayout());

    private final JPanel recommendationView = new JPanel();
    private final JPanel combatView = new JPanel();

    private final JPanel recommendedContainer;
    private final JPanel completedContainer;

    private final JPanel combatContainer = new JPanel();

    @Setter
    private RecommendTasks recommendTasks;
    @Setter
    private Long characterId = null;
    private final ConfigManager configManager;
    private ArrayList<Task> recommendedList;
    private final Set<Task> completedList;
    private final SpriteManager spriteManager;
    private Boolean ascending;
    private SortingType sortingType;

    @Inject
    public CARoadmapPanel(SpriteManager spriteManager, ConfigManager configManager)
    {
        super(false);
        this.spriteManager = spriteManager;
        this.configManager = configManager;

        this.ascending = configManager.getConfiguration("CARoadmap", "isAscending", Boolean.class);
        if (this.ascending == null)
        {
            this.ascending = true;
            configManager.setConfiguration("CARoadmap", "isAscending", this.ascending);
        }

        this.sortingType = configManager.getConfiguration("CARoadmap", "sortingType", SortingType.class);
        if (this.sortingType == null)
        {
            this.sortingType = SortingType.SCORE;
            configManager.setConfiguration("CARoadmap", "sortingType", this.sortingType);
        }

        this.recommendedList = new ArrayList<>();
        this.completedList = new HashSet<>();

        // =========================
        // ROOT PANEL SETUP
        // =========================
        setLayout(new BorderLayout());
        setBackground(ColorScheme.DARK_GRAY_COLOR);
        setBorder(new EmptyBorder(5, 5, 5, 5));

        JLabel titleLabel = new JLabel("CA Roadmap");
        titleLabel.setFont(FontManager.getRunescapeBoldFont());

        // =========================
        // TOOLBAR (shared)
        // =========================
        JPanel tabBar = new JPanel();
        tabBar.setLayout(new FlowLayout(FlowLayout.LEFT));
        tabBar.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        JButton recTab = new JButton("Recommendations");
        JButton combatTab = new JButton("Combat");

        recTab.addActionListener(e -> setMode(PanelMode.RECOMMENDATIONS));
        combatTab.addActionListener(e -> setMode(PanelMode.COMBAT));

        tabBar.add(recTab);
        tabBar.add(combatTab);

        JPanel toolbar = new JPanel();
        toolbar.setLayout(new BoxLayout(toolbar, BoxLayout.X_AXIS));
        toolbar.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        toolbar.setBorder(new EmptyBorder(5, 5, 5, 5));

        final BufferedImage refresh = ImageUtil.loadImageResource(CARoadmapPlugin.class, "/update_icon.png");
        final BufferedImage ascendingIcon = ImageUtil.loadImageResource(CARoadmapPlugin.class, "/a-z.png");
        final BufferedImage descendingIcon = ImageUtil.loadImageResource(CARoadmapPlugin.class, "/z-a.png");

        JComboBox<String> sortDropdown = getSortByMenu();
        JButton recommendationBtn = getRecommendationBtn(refresh);
        JButton sortingButton = getSortingButton(ascendingIcon, descendingIcon);

        toolbar.add(sortDropdown);
        toolbar.add(sortingButton);
        toolbar.add(recommendationBtn);

        // =========================
        // RECOMMENDATION VIEW
        // =========================
        recommendationView.setLayout(new BoxLayout(recommendationView, BoxLayout.Y_AXIS));
        recommendationView.setBackground(ColorScheme.DARK_GRAY_COLOR);

        JPanel recommendedTitlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        recommendedTitlePanel.setOpaque(false);

        JLabel recommendedTitle = new JLabel("Recommended: ");
        recommendedTitle.setFont(FontManager.getRunescapeBoldFont());
        recommendedTitlePanel.add(recommendedTitle);

        JPanel completedTitlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        completedTitlePanel.setOpaque(false);

        JLabel completedTitle = new JLabel("Completed: ");
        completedTitle.setFont(FontManager.getRunescapeBoldFont());
        completedTitlePanel.add(completedTitle);

        this.recommendedContainer = new JPanel();
        recommendedContainer.setLayout(new BoxLayout(recommendedContainer, BoxLayout.Y_AXIS));
        recommendedContainer.setBorder(new EmptyBorder(5, 5, 5, 5));

        this.completedContainer = new JPanel();
        completedContainer.setLayout(new BoxLayout(completedContainer, BoxLayout.Y_AXIS));
        completedContainer.setBorder(new EmptyBorder(5, 5, 5, 5));

        JScrollPane scrollPane = new JScrollPane(recommendedContainer);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setPreferredSize(new Dimension(0, 300));

        JScrollPane completedScroll = new JScrollPane(completedContainer);
        completedScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        completedScroll.setPreferredSize(new Dimension(0, 300));

        recommendationView.add(toolbar);
        recommendationView.add(Box.createRigidArea(new Dimension(0, 10)));
        recommendationView.add(recommendedTitlePanel);
        recommendationView.add(scrollPane);
        recommendationView.add(Box.createRigidArea(new Dimension(0, 10)));
        recommendationView.add(completedTitlePanel);
        recommendationView.add(completedScroll);

        // =========================
        // COMBAT VIEW
        // =========================
        combatView.setLayout(new BoxLayout(combatView, BoxLayout.Y_AXIS));
        combatView.setBackground(ColorScheme.DARK_GRAY_COLOR);

        JLabel combatLabel = new JLabel("Combat Mode Active");
        combatLabel.setFont(FontManager.getRunescapeBoldFont());

        combatContainer.setLayout(new BoxLayout(combatContainer, BoxLayout.Y_AXIS));

        combatView.add(combatLabel);
        combatView.add(combatContainer);

        // =========================
        // CARD LAYOUT SWITCHER
        // =========================
        mainContainer.setLayout(new CardLayout());
        mainContainer.add(recommendationView, PanelMode.RECOMMENDATIONS.name());
        mainContainer.add(combatView, PanelMode.COMBAT.name());

        // =========================
        // ROOT ATTACH
        // =========================
        add(titleLabel, BorderLayout.NORTH);
        add(tabBar, BorderLayout.NORTH);
        add(mainContainer, BorderLayout.CENTER);

        setMode(PanelMode.RECOMMENDATIONS);
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
                recommendTasks.getRecommendations(characterId);
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
                if (characterId != null) {
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
                recommendTasks.getRecommendations(characterId);
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

    public void setMode(PanelMode mode)
    {
        this.panelMode = mode;

        CardLayout cl = (CardLayout) mainContainer.getLayout();
        cl.show(mainContainer, mode.name());

        log.info("Switching panel mode to {}", mode);

        revalidate();
        repaint();
    }

    public void setCombatTasks(List<Task> combatTasks)
    {
        log.info("UI TASKS SIZE: {}", combatTasks.size());
        combatContainer.removeAll();

        for (Task task : combatTasks)
        {
            addTaskToContainers(task, combatContainer);
        }

        combatContainer.revalidate();
        combatContainer.repaint();
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
            addTaskToContainers(task, recommendedContainer);
        }
        for (Task task: completedList) {
            addTaskToContainers(task, completedContainer);
        }
        recommendedContainer.revalidate();
        recommendedContainer.repaint();
        completedContainer.revalidate();
        completedContainer.repaint();
    }

    private void addTaskToContainers(Task task, JPanel container) {
        if (task.getType() == TaskType.KILL_COUNT) {
            container.add(new KillCountDisplayTask(task, spriteManager));
            container.add(Box.createRigidArea(new Dimension(0, 5)));
        }
        else {
            container.add(new DisplayTask(task, spriteManager));
            container.add(Box.createRigidArea(new Dimension(0, 5)));
        }
    }
}
