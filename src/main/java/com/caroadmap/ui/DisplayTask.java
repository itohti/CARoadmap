package com.caroadmap.ui;

import com.caroadmap.CARoadmapPlugin;
import com.caroadmap.data.Task;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.hiscore.HiscoreClient;
import net.runelite.client.hiscore.HiscoreResult;
import net.runelite.client.hiscore.HiscoreSkill;
import net.runelite.client.util.ImageUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

import static net.runelite.api.SpriteID.TAB_COMBAT;

@Slf4j
public class DisplayTask extends JPanel {
    private final JPanel detailPanel;
    private boolean expanded = false;
    public static final Map<Integer, String> TIER_ICON_MAP = new HashMap<>();
    private static final Map<String, HiscoreSkill> BOSS_TO_SKILL_MAP = new HashMap<>();

    static {
        BOSS_TO_SKILL_MAP.put("Theatre of Blood: Entry Mode", HiscoreSkill.THEATRE_OF_BLOOD);
        BOSS_TO_SKILL_MAP.put("Tombs of Amascut: Expert Mode", HiscoreSkill.TOMBS_OF_AMASCUT_EXPERT);
        BOSS_TO_SKILL_MAP.put("Barrows", HiscoreSkill.BARROWS_CHESTS);
        BOSS_TO_SKILL_MAP.put("Crystalline Hunllef", HiscoreSkill.THE_GAUNTLET);
        BOSS_TO_SKILL_MAP.put("Corrupted Hunllef", HiscoreSkill.THE_CORRUPTED_GAUNTLET);
        BOSS_TO_SKILL_MAP.put("Moons of Peril", HiscoreSkill.LUNAR_CHESTS);
    }

    static {
        TIER_ICON_MAP.put(1, "/easy_tier.png");
        TIER_ICON_MAP.put(2, "/medium_tier.png");
        TIER_ICON_MAP.put(3, "/hard_tier.png");
        TIER_ICON_MAP.put(4, "/elite_tier.png");
        TIER_ICON_MAP.put(5, "/master_tier.png");
        TIER_ICON_MAP.put(6, "/grandmaster_tier.png");
    }


    public DisplayTask(Task task, SpriteManager spriteManager) {
        JLabel icon = new JLabel();
        try {
            HiscoreSkill skill = BOSS_TO_SKILL_MAP.get(task.getBoss());
            if (skill == null) {
                try {
                    skill = HiscoreSkill.valueOf(task.getBoss().toUpperCase()
                            .replace(" ", "_")
                            .replace("'", "")
                            .replace("-", "_"));
                }
                catch (IllegalArgumentException e) {
                    log.info("Could not find skill for {}", task.getBoss());
                }
            }
            icon.setToolTipText(task.getBoss());
            // Credit goes to runelite Hiscore. This code is not mine.
            // https://github.com/runelite/runelite/blob/master/runelite-client/src/main/java/net/runelite/client/plugins/hiscore/HiscorePanel.java#L336
            spriteManager.getSpriteAsync(skill == null ? TAB_COMBAT : skill.getSpriteId(), 0, (sprite) ->
                    SwingUtilities.invokeLater(() ->
                    {
                        final BufferedImage scaledSprite = ImageUtil.resizeImage(ImageUtil.resizeCanvas(sprite, 25, 25), 20, 20);
                        icon.setIcon(new ImageIcon(scaledSprite));
                    }));

        }
        catch (Exception e) {
            log.error("Could not get hiscore skill: ", e);
        }
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(ColorScheme.DARKER_GRAY_COLOR);
        setBorder(new EmptyBorder(5, 5, 5, 5));
        setMaximumSize(new Dimension(1000, 200));

        // Create main content panel
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        headerPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        headerPanel.setBorder(BorderFactory.createEmptyBorder());

        // create components
        JLabel taskLabel = new JLabel(task.getTaskName());
        JLabel taskTier = new JLabel();
        taskTier.setIcon(new ImageIcon(getTierIcon(task.getTier())));
        taskTier.setToolTipText("+" + task.getTier());

        // Create detail panel that will be shown/hidden
        detailPanel = new JPanel();
        detailPanel.setLayout(new GridBagLayout()); // this will probably change
        detailPanel.setBorder(new EmptyBorder(10, 5, 5, 5));
        detailPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        JTextArea taskDescription = new JTextArea(task.getTaskDescription());
        taskDescription.setLineWrap(true);
        taskDescription.setWrapStyleWord(true);
        taskDescription.setEditable(false);
        taskDescription.setFocusable(false);
        taskDescription.setOpaque(false);
        taskDescription.setFont(FontManager.getRunescapeSmallFont());

        JLabel taskType = new JLabel(task.getType().name());
        taskType.setFont(FontManager.getRunescapeSmallFont());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.7;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(0, 0, 0, 10);
        detailPanel.add(taskDescription, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.3;
        gbc.insets = new Insets(0, 0, 0, 0);
        detailPanel.add(taskType, gbc);

        detailPanel.setVisible(false);

        // Add components to main content
        headerPanel.add(icon);
        headerPanel.add(taskLabel);
        headerPanel.add(taskTier);

        // Add mouse listener to the main content panel
        headerPanel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                toggleDetailPanel();
            }

            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                headerPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                headerPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
            }
        });

        // Make the cursor change to indicate clickable area
        headerPanel.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Add both panels to this container
        this.add(headerPanel);
        this.add(detailPanel);
    }

    private void toggleDetailPanel() {
        expanded = !expanded;
        detailPanel.setVisible(expanded);
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

    private BufferedImage getTierIcon(int tier) {
        BufferedImage icon = ImageUtil.loadImageResource(CARoadmapPlugin.class, TIER_ICON_MAP.get(tier));
        return ImageUtil.resizeImage(ImageUtil.resizeCanvas(icon, 25, 25), 20, 20);
    }
}
