package com.itemretrievalwarning;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class ItemRetrievalWarningPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(ItemRetrievalWarningPlugin.class);
		RuneLite.main(args);
	}
}