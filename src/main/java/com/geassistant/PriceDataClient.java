package com.geassistant;

import java.io.IOException;

interface PriceDataClient
{
	String fetchLatestPrices() throws IOException;

	String fetchFiveMinutePrices() throws IOException;

	String fetchHourlyPrices() throws IOException;
}
