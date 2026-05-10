package com.geassistant;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class GeAssistantPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(GeAssistantPlugin.class);
		RuneLite.main(args);
	}
}
