package com.geassistant;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

final class WikiLatestPriceClient implements PriceDataClient
{
	private static final URI LATEST_PRICES = URI.create("https://prices.runescape.wiki/api/v1/osrs/latest");
	private static final String USER_AGENT = "ge-assistant-runelite-plugin - https://github.com/Gary0/ge-assistant";

	private final HttpClient httpClient;

	WikiLatestPriceClient()
	{
		this(HttpClient.newHttpClient());
	}

	WikiLatestPriceClient(HttpClient httpClient)
	{
		this.httpClient = httpClient;
	}

	@Override
	public String fetchLatestPrices() throws IOException
	{
		HttpRequest request = HttpRequest.newBuilder(LATEST_PRICES)
			.header("User-Agent", USER_AGENT)
			.header("Accept", "application/json")
			.GET()
			.build();

		try
		{
			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() < 200 || response.statusCode() >= 300)
			{
				throw new IOException("OSRS Wiki price API returned HTTP " + response.statusCode());
			}
			return response.body();
		}
		catch (InterruptedException ex)
		{
			Thread.currentThread().interrupt();
			throw new IOException("Interrupted while fetching OSRS Wiki prices", ex);
		}
	}
}

