package com.caroadmap.ui;

import com.caroadmap.CARoadmapPlugin;
import com.caroadmap.data.Task;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.hiscore.HiscoreSkill;
import net.runelite.client.util.ImageUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static net.runelite.api.SpriteID.TAB_COMBAT;

@Slf4j
public class DisplayTask extends JPanel {
    protected final Task task;
    protected final JPanel detailPanel;
    private final BufferedImage downArrow = ImageUtil.loadImageResource(CARoadmapPlugin.class, "/down_arrow.png");
    private final BufferedImage rightArrow = ImageUtil.loadImageResource(CARoadmapPlugin.class, "/right_arrow.png");
    protected final JLabel toggleArrow = new JLabel(new ImageIcon(rightArrow));
    protected final JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
    protected boolean expanded;
    public static final Map<Integer, String> TIER_ICON_MAP = new HashMap<>();

    protected JLabel bossIcon;
    protected JLabel taskNameLabel;
    protected JLabel taskTierLabel;

    static {
        TIER_ICON_MAP.put(1, "/easy_tier.png");
        TIER_ICON_MAP.put(2, "/medium_tier.png");
        TIER_ICON_MAP.put(3, "/hard_tier.png");
        TIER_ICON_MAP.put(4, "/elite_tier.png");
        TIER_ICON_MAP.put(5, "/master_tier.png");
        TIER_ICON_MAP.put(6, "/grandmaster_tier.png");
    }

    public DisplayTask(Task task, SpriteManager spriteManager) {
        this.task = task;

        bossIcon = BossIconUtil.createBossIcon(task.getBoss(), spriteManager);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(ColorScheme.DARKER_GRAY_COLOR);
        setBorder(new EmptyBorder(5, 5, 5, 5));
        setMaximumSize(new Dimension(1000, 200));

        // Create main content panel
        headerPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        headerPanel.setBorder(BorderFactory.createEmptyBorder());

        if (task.isDone()) {
            headerPanel.setBackground(new Color(0x006600));
        }

        // create components
        String fullTaskName = task.getTaskName();
        taskNameLabel = new JLabel(fullTaskName);
        taskNameLabel.setToolTipText(fullTaskName);
        taskNameLabel.setPreferredSize(new Dimension(120, 20));

        taskTierLabel = new JLabel();
        taskTierLabel.setIcon(new ImageIcon(getTierIcon(task.getTier())));
        taskTierLabel.setToolTipText("+" + task.getTier());


        // Create detail panel that will be shown/hidden
        detailPanel = new JPanel();
        detailPanel.setLayout(new GridBagLayout()); // this will probably change
        detailPanel.setBorder(new EmptyBorder(10, 5, 5, 5));
        detailPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

        detailPanel.setVisible(false);

        // Add components to main content
        buildHeaderPanel();

        // Add mouse listener to the main content panel
        headerPanel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                toggleDetailPanel();
            }

            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                headerPanel.setBackground(task.isDone() ? new Color(0x006600) : ColorScheme.DARK_GRAY_COLOR);
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                headerPanel.setBackground(task.isDone() ? new Color(0x006600) : ColorScheme.DARKER_GRAY_COLOR);
            }
        });

        // Make the cursor change to indicate clickable area
        headerPanel.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Add both panels to this container
        this.add(headerPanel);
        this.add(detailPanel);

        buildDetailPanel(detailPanel);
    }

    protected void buildHeaderPanel()
    {
        headerPanel.add(bossIcon);
        headerPanel.add(taskNameLabel);
        headerPanel.add(taskTierLabel);
        headerPanel.add(toggleArrow);
    }

    protected void buildDetailPanel(JPanel detailPanel) {
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;

        int row = 0;

        JTextArea description = new JTextArea(task.getTaskDescription());
        description.setLineWrap(true);
        description.setWrapStyleWord(true);
        description.setEditable(false);
        description.setFocusable(false);
        description.setOpaque(false);
        description.setBorder(null);
        description.setMargin(new Insets(0, 0, 0, 0));
        description.setFont(FontManager.getRunescapeSmallFont());

        JLabel typeLabel =
                new JLabel(task.getType().name());

        addFullWidthRow(
                detailPanel,
                gbc,
                row++,
                description
        );

        addRow(
                detailPanel,
                gbc,
                row++,
                new JLabel("Type"),
                typeLabel
        );


        // NEW: shared ML metric
        JLabel completionLabel =
                new JLabel("Completion Chance");

        JLabel completionValue =
                new JLabel(
                        String.format(
                                "%.0f%%",
                                task.getCompletionProbability() * 100
                        )
                );

        completionValue.setForeground(
                getProbabilityColor(
                        task.getCompletionProbability()
                )
        );

        addRow(
                detailPanel,
                gbc,
                row++,
                completionLabel,
                completionValue
        );
    }

    private void toggleDetailPanel() {
        expanded = !expanded;
        detailPanel.setVisible(expanded);
        toggleArrow.setIcon(expanded ? new ImageIcon(downArrow) : new ImageIcon(rightArrow));
        revalidate();
        repaint();

        Container parent = getParent();
        while (parent != null && !(parent instanceof JScrollPane)) {
            parent = parent.getParent();
        }
        if (parent != null) {
            parent.revalidate();
        }
    }

    protected void addRow(
            JPanel panel,
            GridBagConstraints gbc,
            int row,
            Component label,
            Component value
    ) {
        label.setFont(FontManager.getRunescapeSmallFont());
        value.setFont(FontManager.getRunescapeSmallFont());
        gbc.gridy = row;

        // Label column
        gbc.gridx = 0;
        gbc.weightx = 0.2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(label, gbc);

        // Value column
        gbc.gridx = 1;
        gbc.weightx = 0.8;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(value, gbc);
    }

    protected void addFullWidthRow(
            JPanel panel,
            GridBagConstraints gbc,
            int row,
            Component component
    ) {
        component.setFont(FontManager.getRunescapeSmallFont());

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 0, 5, 0);

        panel.add(component, gbc);

        gbc.gridwidth = 1;
    }

    private BufferedImage getTierIcon(int tier) {
        BufferedImage icon = ImageUtil.loadImageResource(CARoadmapPlugin.class, TIER_ICON_MAP.get(tier));
        return ImageUtil.resizeImage(ImageUtil.resizeCanvas(icon, 25, 25), 20, 20);
    }

    protected Color getProbabilityColor(double probability) {
        double percent = probability * 100;

        if (percent >= 80) {
            return new Color(0x00AA00);
        }

        if (percent >= 50) {
            return new Color(0xCCAA00);
        }

        return new Color(0xCC3333);
    }
}
