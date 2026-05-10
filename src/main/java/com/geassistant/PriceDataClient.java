package com.geassistant;

import java.io.IOException;

interface PriceDataClient
{
	String fetchLatestPrices() throws IOException;
}

