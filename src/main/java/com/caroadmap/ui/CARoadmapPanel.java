package com.caroadmap.ui;

import lombok.extern.slf4j.Slf4j;

import net.runelite.client.ui.PluginPanel;

import javax.inject.Inject;
import javax.swing.*;
import java.awt.*;

@Slf4j
public class CARoadmapPanel extends PluginPanel{
    @Inject
    public CARoadmapPanel() {
        super(false);
        setLayout(new BorderLayout());
    }
}
