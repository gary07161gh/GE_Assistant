package com.geassistant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import org.junit.Test;

public class WikiPriceCacheTest
{
	@Test
	public void refreshesAllLatestPricesOnceAndReusesCacheUntilExpired() throws Exception
	{
		FakePriceDataClient client = new FakePriceDataClient("{\"data\":{\"536\":{\"high\":108,\"highTime\":1770000100,\"low\":100,\"lowTime\":1770000000}}}");
		MutableClock clock = new MutableClock(Instant.parse("2026-05-10T20:00:00Z"));
		WikiPriceCache cache = new WikiPriceCache(client, clock);

		assertTrue(cache.refreshIfNeeded(Duration.ofSeconds(60)));
		assertFalse(cache.refreshIfNeeded(Duration.ofSeconds(60)));

		assertEquals(1, client.calls);
		assertEquals(Integer.valueOf(108), cache.get(536).get().getHigh());
		assertEquals(Integer.valueOf(100), cache.get(536).get().getLow());
	}

	@Test
	public void refreshesAgainAfterIntervalExpires() throws Exception
	{
		FakePriceDataClient client = new FakePriceDataClient("{\"data\":{}}");
		MutableClock clock = new MutableClock(Instant.parse("2026-05-10T20:00:00Z"));
		WikiPriceCache cache = new WikiPriceCache(client, clock);

		cache.refreshIfNeeded(Duration.ofSeconds(60));
		clock.now = clock.now.plusSeconds(61);
		cache.refreshIfNeeded(Duration.ofSeconds(60));

		assertEquals(2, client.calls);
	}

	@Test
	public void keepsExistingCacheWhenRefreshResponseIsMalformed() throws Exception
	{
		FakePriceDataClient client = new FakePriceDataClient(
			"{\"data\":{\"536\":{\"high\":108,\"highTime\":1770000100,\"low\":100,\"lowTime\":1770000000}}}",
			"not-json"
		);
		MutableClock clock = new MutableClock(Instant.parse("2026-05-10T20:00:00Z"));
		WikiPriceCache cache = new WikiPriceCache(client, clock);

		cache.refreshIfNeeded(Duration.ofSeconds(60));
		clock.now = clock.now.plusSeconds(61);

		assertFalse(cache.refreshIfNeeded(Duration.ofSeconds(60)));
		assertEquals(Integer.valueOf(108), cache.get(536).get().getHigh());
	}

	private static final class FakePriceDataClient implements PriceDataClient
	{
		private final String[] responses;
		private int calls;

		private FakePriceDataClient(String... responses)
		{
			this.responses = responses;
		}

		@Override
		public String fetchLatestPrices() throws IOException
		{
			String response = responses[Math.min(calls, responses.length - 1)];
			calls++;
			return response;
		}
	}

	private static final class MutableClock extends Clock
	{
		private Instant now;

		private MutableClock(Instant now)
		{
			this.now = now;
		}

		@Override
		public ZoneOffset getZone()
		{
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(java.time.ZoneId zone)
		{
			return this;
		}

		@Override
		public Instant instant()
		{
			return now;
		}
	}
}

