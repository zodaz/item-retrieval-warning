package com.itemretrievalwarning;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Units;

@ConfigGroup("itemretrievalwarning")
public interface ItemRetrievalWarningConfig extends Config
{
    @ConfigItem(
	    position = 0,
	    keyName = "warningThreshold",
	    name = "Warning Threshold",
	    description = "Plugin will warn you if you have items stored in an item retrieval service and hp drops below this %"
    )
    @Units(Units.PERCENT)
    default int warningThreshold()
    {
	    return 50;
    }

    @ConfigItem(
	    position = 1,
	    keyName = "displayInfobox",
	    name = "Display Infobox",
	    description = "Display an infobox reminding you when you have items stored in an item retrieval service"
    )
    default boolean displayInfobox()
    {
	return true;
    }
}