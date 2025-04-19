package com.caroadmap;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("CARoadmap")
public interface CARoadmapConfig extends Config
{
	@ConfigItem(
			keyName = "apiKey",
			name = "API Key",
			description = "Paste the API key from caroadmap.io here"
	)
	default String apiKey() {
		return "";
	}

    @ConfigItem(
            keyName = "apiKey",
            name = "",
            description = ""
    )
    void setApiKey(String key);
}
