package com.caroadmap.ui;

import com.caroadmap.data.Task;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.hiscore.HiscoreSkill;
import net.runelite.client.util.ImageUtil;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Map;

import static net.runelite.api.SpriteID.TAB_COMBAT;

@Slf4j
public class BossIconUtil {
    private static final Map<String, HiscoreSkill> HISCORE_ALIASES = Map.ofEntries(
            Map.entry("theatre of blood", HiscoreSkill.THEATRE_OF_BLOOD),
            Map.entry("theatre of blood entry mode", HiscoreSkill.THEATRE_OF_BLOOD),
            Map.entry("theatre of blood hard mode", HiscoreSkill.THEATRE_OF_BLOOD),

            Map.entry("chambers of xeric", HiscoreSkill.CHAMBERS_OF_XERIC),
            Map.entry("chambers of xeric challenge mode", HiscoreSkill.CHAMBERS_OF_XERIC),

            Map.entry("tombs of amascut", HiscoreSkill.TOMBS_OF_AMASCUT),
            Map.entry("tombs of amascut expert mode", HiscoreSkill.TOMBS_OF_AMASCUT_EXPERT),

            Map.entry("barrows", HiscoreSkill.BARROWS_CHESTS),
            Map.entry("moons of peril", HiscoreSkill.LUNAR_CHESTS),
            Map.entry("crystalline hunllef", HiscoreSkill.THE_GAUNTLET),
            Map.entry("corrupted hunllef", HiscoreSkill.THE_CORRUPTED_GAUNTLET),
            Map.entry("fortis colosseum", HiscoreSkill.SOL_HEREDIT),
            Map.entry("tzhaar ket raks challenges", HiscoreSkill.TZTOK_JAD),
            Map.entry("royal titans", HiscoreSkill.THE_ROYAL_TITANS),
            Map.entry("the nightmare", HiscoreSkill.NIGHTMARE)
    );


    protected static JLabel createBossIcon(String bossName, SpriteManager spriteManager)
    {
        JLabel icon = new JLabel();

        HiscoreSkill skill = getBossSkill(bossName);

        if (skill == null)
        {
            log.debug("Could not find {} hiscore sprite", bossName);
        }

        spriteManager.getSpriteAsync(
                skill == null ? TAB_COMBAT : skill.getSpriteId(),
                0,
                sprite -> SwingUtilities.invokeLater(() ->
                {
                    BufferedImage scaled =
                            ImageUtil.resizeImage(
                                    ImageUtil.resizeCanvas(sprite, 25, 25),
                                    20,
                                    20);

                    icon.setIcon(new ImageIcon(scaled));
                }));

        return icon;
    }

    public static void loadBossIcon(
            String bossName,
            SpriteManager spriteManager,
            JLabel label)
    {
        HiscoreSkill skill = getBossSkill(bossName);

        if (skill == null)
        {
            log.debug("Could not find {} hiscore sprite", bossName);
        }

        spriteManager.getSpriteAsync(
                skill == null ? TAB_COMBAT : skill.getSpriteId(),
                0,
                sprite -> SwingUtilities.invokeLater(() ->
                {
                    BufferedImage scaled =
                            ImageUtil.resizeImage(
                                    ImageUtil.resizeCanvas(sprite, 25, 25),
                                    20,
                                    20);

                    label.setIcon(new ImageIcon(scaled));
                }));
    }

    private static HiscoreSkill getBossSkill(String boss) {
        String normalized = normalizeBossName(boss);

        // Exact alias first
        HiscoreSkill alias = HISCORE_ALIASES.get(normalized);
        if (alias != null) {
            return alias;
        }

        // Try automatic enum lookup
        try {
            return HiscoreSkill.valueOf(
                    normalized
                            .toUpperCase()
                            .replace(" ", "_")
            );
        }
        catch (IllegalArgumentException ignored) {
        }

        // Try prepending THE_
        try {
            return HiscoreSkill.valueOf(
                    "THE_" + normalized
                            .toUpperCase()
                            .replace(" ", "_")
            );
        }
        catch (IllegalArgumentException ignored) {
        }

        // Try stripping suffixes
        String[] parts = normalized.split(" ");

        for (int i = parts.length - 1; i > 0; i--) {
            String candidate = String.join("_", Arrays.copyOf(parts, i))
                    .toUpperCase();

            try {
                return HiscoreSkill.valueOf(candidate);
            }
            catch (IllegalArgumentException ignored) {
            }

            try {
                return HiscoreSkill.valueOf("THE_" + candidate);
            }
            catch (IllegalArgumentException ignored) {
            }
        }

        return null;
    }

    private static String normalizeBossName(String metric) {
        return metric
                .toLowerCase()
                .replace("_", " ")
                .replace(":", " ")
                .replace("-", " ")
                .replace("'", "")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
