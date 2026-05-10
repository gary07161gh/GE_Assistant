package com.geassistant;

import java.time.Duration;
import java.time.Instant;

final class PriceAgeFormatter
{
	private PriceAgeFormatter()
	{
	}

	static String formatAge(Instant timestamp, Instant now)
	{
		if (timestamp == null || now == null)
		{
			return "n/a";
		}

		long seconds = Math.max(0, Duration.between(timestamp, now).getSeconds());
		if (seconds < 60)
		{
			return seconds + "s";
		}

		long minutes = seconds / 60;
		if (minutes < 60)
		{
			return minutes + "m";
		}

		return (minutes / 60) + "h";
	}
}
