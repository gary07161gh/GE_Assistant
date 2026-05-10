package com.geassistant;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("geassistant")
public interface GeAssistantConfig extends Config
{
	@ConfigItem(
		keyName = "warningThresholdPercent",
		name = "Warning threshold percent",
		description = "Warn when a submitted offer is this far worse than the latest high or low price.",
		position = 0
	)
	default int warningThresholdPercent()
	{
		return 2;
	}

	@ConfigItem(
		keyName = "priceRefreshSeconds",
		name = "Price refresh seconds",
		description = "Minimum seconds between OSRS Wiki latest-price refreshes.",
		position = 1
	)
	default int priceRefreshSeconds()
	{
		return 60;
	}

	@ConfigItem(
		keyName = "showSlotBadges",
		name = "Show slot badges",
		description = "Draw warning badges and borders over suspicious GE offer slots.",
		position = 2
	)
	default boolean showSlotBadges()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showMarginTooltip",
		name = "Show margin tooltip",
		description = "Show high, low, spread, and tax-adjusted margin when hovering a warned offer.",
		position = 3
	)
	default boolean showMarginTooltip()
	{
		return true;
	}

	@ConfigItem(
		keyName = "taxPercent",
		name = "GE tax percent",
		description = "Sell-side Grand Exchange tax percent used in margin calculations.",
		position = 4
	)
	default int taxPercent()
	{
		return 2;
	}

	@ConfigItem(
		keyName = "taxCapGp",
		name = "GE tax cap",
		description = "Maximum GP tax per sold item used in margin calculations.",
		position = 5
	)
	default int taxCapGp()
	{
		return 5_000_000;
	}

	@ConfigItem(
		keyName = "enableWikiPrices",
		name = "Enable Wiki prices",
		description = "Fetch cached latest high and low prices from the OSRS Wiki real-time prices API.",
		position = 6
	)
	default boolean enableWikiPrices()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showDebugStatus",
		name = "Show debug status",
		description = "Show GE Assistant offer, warning, and price-cache counts on the GE screen.",
		position = 7
	)
	default boolean showDebugStatus()
	{
		return false;
	}
}
