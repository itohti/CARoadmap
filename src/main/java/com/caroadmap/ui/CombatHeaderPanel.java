package com.caroadmap.ui;

import com.caroadmap.CombatSession;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

import javax.swing.*;
import java.awt.*;

public class CombatHeaderPanel extends JPanel
{
    private JLabel iconLabel = new JLabel();
    private final JLabel bossLabel = new JLabel();

    private final SpriteManager spriteManager;
    private String displayedBoss;

    public CombatHeaderPanel(SpriteManager spriteManager)
    {
        this.spriteManager = spriteManager;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        bossLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        bossLabel.setHorizontalAlignment(SwingConstants.CENTER);
        bossLabel.setFont(FontManager.getRunescapeBoldFont());

        add(iconLabel);
        add(Box.createVerticalStrut(4));
        add(bossLabel);
    }

    public void update(CombatSession session)
    {
        String bossName = session.getBossName();

        if (!bossName.equals(displayedBoss)) {
            displayedBoss = bossName;

            bossLabel.setText(bossName + " Combat Achievements");

            BossIconUtil.loadBossIcon(
                    bossName,
                    spriteManager,
                    iconLabel
            );
        }
    }
}
