package com.caroadmap;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("CARoadmap")
public interface CARoadmapConfig extends Config
{
	@ConfigItem(
		keyName = "taskPreference",
		name = "Task Preference",
		description = "Type the task type from the most preferred to least preferred"
	)
	default String taskPreference()
	{
		return "kill count, mechanical, restriction, perfection, stamina, speed";
	}
}
