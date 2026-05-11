package com.geassistant;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

final class WikiPriceCache
{
	private final PriceDataClient client;
	private final Clock clock;
	private volatile Map<Integer, PriceSnapshot> prices = Collections.emptyMap();
	private volatile Map<Integer, MarketSnapshot> fiveMinutePrices = Collections.emptyMap();
	private volatile Map<Integer, MarketSnapshot> hourlyPrices = Collections.emptyMap();
	private volatile Instant lastRefresh;
	private volatile String lastError;

	WikiPriceCache(PriceDataClient client, Clock clock)
	{
		this.client = client;
		this.clock = clock;
	}

	synchronized boolean refreshIfNeeded(Duration refreshInterval)
	{
		Instant now = clock.instant();
		if (lastRefresh != null && now.isBefore(lastRefresh.plus(refreshInterval)))
		{
			return false;
		}

		try
		{
			Map<Integer, PriceSnapshot> parsed = parseLatest(client.fetchLatestPrices());
			prices = parsed;
			lastRefresh = now;
			lastError = null;
			refreshMarketData();
			return true;
		}
		catch (IOException | RuntimeException ex)
		{
			lastError = ex.getClass().getSimpleName() + ": " + ex.getMessage();
			return false;
		}
	}

	Optional<PriceSnapshot> get(int itemId)
	{
		return Optional.ofNullable(prices.get(itemId));
	}

	Optional<MarketSnapshot> getFiveMinute(int itemId)
	{
		return Optional.ofNullable(fiveMinutePrices.get(itemId));
	}

	Optional<MarketSnapshot> getHourly(int itemId)
	{
		return Optional.ofNullable(hourlyPrices.get(itemId));
	}

	int size()
	{
		return prices.size();
	}

	String getLastError()
	{
		return lastError;
	}

	private void refreshMarketData()
	{
		try
		{
			fiveMinutePrices = parseMarket(client.fetchFiveMinutePrices());
		}
		catch (IOException | RuntimeException ex)
		{
			fiveMinutePrices = Collections.emptyMap();
			lastError = "5m market data: " + ex.getClass().getSimpleName() + ": " + ex.getMessage();
		}

		try
		{
			hourlyPrices = parseMarket(client.fetchHourlyPrices());
		}
		catch (IOException | RuntimeException ex)
		{
			hourlyPrices = Collections.emptyMap();
			String message = "1h market data: " + ex.getClass().getSimpleName() + ": " + ex.getMessage();
			lastError = lastError == null ? message : lastError + "; " + message;
		}
	}

	private Map<Integer, PriceSnapshot> parseLatest(String body)
	{
		JsonObject root = JsonParser.parseString(body).getAsJsonObject();
		JsonObject data = root.getAsJsonObject("data");
		if (data == null)
		{
			throw new IllegalArgumentException("Missing data object");
		}

		Map<Integer, PriceSnapshot> parsed = new HashMap<>();
		for (Map.Entry<String, JsonElement> entry : data.entrySet())
		{
			int itemId = Integer.parseInt(entry.getKey());
			JsonObject item = entry.getValue().getAsJsonObject();
			parsed.put(itemId, new PriceSnapshot(
				itemId,
				intOrNull(item, "high"),
				instantOrNull(item, "highTime"),
				intOrNull(item, "low"),
				instantOrNull(item, "lowTime")
			));
		}
		return parsed;
	}

	private Map<Integer, MarketSnapshot> parseMarket(String body)
	{
		JsonObject root = JsonParser.parseString(body).getAsJsonObject();
		JsonObject data = root.getAsJsonObject("data");
		if (data == null)
		{
			throw new IllegalArgumentException("Missing data object");
		}

		Map<Integer, MarketSnapshot> parsed = new HashMap<>();
		for (Map.Entry<String, JsonElement> entry : data.entrySet())
		{
			int itemId = Integer.parseInt(entry.getKey());
			JsonObject item = entry.getValue().getAsJsonObject();
			parsed.put(itemId, new MarketSnapshot(
				itemId,
				intOrNull(item, "avgHighPrice"),
				intOrNull(item, "highPriceVolume"),
				intOrNull(item, "avgLowPrice"),
				intOrNull(item, "lowPriceVolume")
			));
		}
		return parsed;
	}

	private Integer intOrNull(JsonObject object, String name)
	{
		JsonElement element = object.get(name);
		return element == null || element.isJsonNull() ? null : element.getAsInt();
	}

	private Instant instantOrNull(JsonObject object, String name)
	{
		Integer epochSeconds = intOrNull(object, name);
		return epochSeconds == null ? null : Instant.ofEpochSecond(epochSeconds);
	}
}
