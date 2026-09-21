package com.caroadmap;

import com.caroadmap.data.RewardTier;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("CARoadmap")
public interface CARoadmapConfig extends Config
{
	@ConfigItem(
		keyName = "targetRewardTier",
		name = "Target reward tier",
		description = "The Combat Achievement reward tier you're working towards. "
			+ "Recommendations aim to close the point gap to this tier's threshold."
	)
	default RewardTier targetRewardTier()
	{
		return RewardTier.GRANDMASTER;
	}
}
