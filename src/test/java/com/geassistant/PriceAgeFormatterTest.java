package com.geassistant;

import static org.junit.Assert.assertEquals;

import java.time.Instant;
import org.junit.Test;

public class PriceAgeFormatterTest
{
	private final Instant now = Instant.parse("2026-05-11T12:00:00Z");

	@Test
	public void formatsSecondsMinutesAndHours()
	{
		assertEquals("45s", PriceAgeFormatter.formatAge(now.minusSeconds(45), now));
		assertEquals("6m", PriceAgeFormatter.formatAge(now.minusSeconds(360), now));
		assertEquals("2h", PriceAgeFormatter.formatAge(now.minusSeconds(7_200), now));
	}

	@Test
	public void formatsMissingTimestampAsUnavailable()
	{
		assertEquals("n/a", PriceAgeFormatter.formatAge(null, now));
	}

	@Test
	public void formatsFutureTimestampAsFresh()
	{
		assertEquals("0s", PriceAgeFormatter.formatAge(now.plusSeconds(30), now));
	}
}
