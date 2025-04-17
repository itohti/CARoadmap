package com.caroadmap.ui;

import data.Task;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class DisplayTask extends JPanel {
    private final JPanel detailPanel;
    private boolean expanded = false;

    public DisplayTask(Task task) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(ColorScheme.DARKER_GRAY_COLOR);
        setBorder(new EmptyBorder(5, 5, 5, 5));
        setMaximumSize(new Dimension(1000, 200));

        // Create main content panel
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        headerPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        headerPanel.setBorder(BorderFactory.createEmptyBorder());

        // create components
        JLabel bossLabel = new JLabel(task.getBoss());
        JLabel taskLabel = new JLabel(task.getTaskName());
        JLabel taskTier = new JLabel("+" + task.getTier());

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
        headerPanel.add(bossLabel);
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
}
