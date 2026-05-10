package com.geassistant;

import java.time.Instant;

final class PriceSnapshot
{
	private final int itemId;
	private final Integer high;
	private final Instant highTime;
	private final Integer low;
	private final Instant lowTime;

	PriceSnapshot(int itemId, Integer high, Instant highTime, Integer low, Instant lowTime)
	{
		this.itemId = itemId;
		this.high = high;
		this.highTime = highTime;
		this.low = low;
		this.lowTime = lowTime;
	}

	int getItemId()
	{
		return itemId;
	}

	Integer getHigh()
	{
		return high;
	}

	Instant getHighTime()
	{
		return highTime;
	}

	Integer getLow()
	{
		return low;
	}

	Instant getLowTime()
	{
		return lowTime;
	}
}

