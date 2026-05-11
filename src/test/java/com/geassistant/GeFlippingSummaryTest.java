package com.geassistant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import org.junit.Test;

public class GeFlippingSummaryTest
{
	private final GeOfferInsightBuilder builder = new GeOfferInsightBuilder(new GeWarningEvaluator());
	private final Instant now = Instant.parse("2026-05-11T12:00:00Z");

	@Test
	public void summarizesActiveOffersAndBestOpportunity()
	{
		GeOfferInsight strongBuy = insight(OfferSide.BUY, 100, 10, price(140, 100));
		GeOfferInsight riskyBuy = insight(OfferSide.BUY, 140, 5, price(120, 100));
		GeOfferInsight setup = insight(OfferSide.SELL, 140, 1, price(140, 100));

		GeFlippingSummary summary = GeFlippingSummary.from(Arrays.asList(strongBuy, riskyBuy), Optional.of(setup));

		assertEquals(2, summary.getOfferCount());
		assertEquals(1, summary.getRiskyOfferCount());
		assertEquals(380, summary.getProjectedProfit());
		assertSame(strongBuy, summary.getBestOpportunity().get());
		assertSame(setup, summary.getSetupInsight().get());
	}

	private GeOfferInsight insight(OfferSide side, int offerPrice, int quantity, PriceSnapshot price)
	{
		SetupOfferSnapshot offer = new SetupOfferSnapshot(536, side, offerPrice, quantity);
		return builder.build(
			offer,
			price,
			new MarketSnapshot(536, 140, 1_500, 100, 1_500),
			new MarketSnapshot(536, 138, 15_000, 101, 15_000),
			2.0,
			2.0,
			5_000_000,
			now
		).get();
	}

	private PriceSnapshot price(Integer high, Integer low)
	{
		return new PriceSnapshot(
			536,
			high,
			high == null ? null : now.minusSeconds(65),
			low,
			low == null ? null : now.minusSeconds(360)
		);
	}
}
