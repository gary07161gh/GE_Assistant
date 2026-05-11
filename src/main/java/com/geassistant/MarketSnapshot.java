package com.geassistant;

final class MarketSnapshot
{
	private final int itemId;
	private final Integer avgHighPrice;
	private final Integer highPriceVolume;
	private final Integer avgLowPrice;
	private final Integer lowPriceVolume;

	MarketSnapshot(int itemId, Integer avgHighPrice, Integer highPriceVolume, Integer avgLowPrice, Integer lowPriceVolume)
	{
		this.itemId = itemId;
		this.avgHighPrice = avgHighPrice;
		this.highPriceVolume = highPriceVolume;
		this.avgLowPrice = avgLowPrice;
		this.lowPriceVolume = lowPriceVolume;
	}

	int getItemId()
	{
		return itemId;
	}

	Integer getAvgHighPrice()
	{
		return avgHighPrice;
	}

	Integer getHighPriceVolume()
	{
		return highPriceVolume;
	}

	Integer getAvgLowPrice()
	{
		return avgLowPrice;
	}

	Integer getLowPriceVolume()
	{
		return lowPriceVolume;
	}

	int getTotalVolume()
	{
		return Math.max(0, highPriceVolume == null ? 0 : highPriceVolume)
			+ Math.max(0, lowPriceVolume == null ? 0 : lowPriceVolume);
	}
}
