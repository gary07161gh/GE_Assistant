package com.geassistant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.util.Optional;
import org.junit.Test;

public class GeOfferInsightBuilderTest
{
	private final GeOfferInsightBuilder builder = new GeOfferInsightBuilder(new GeWarningEvaluator());
	private final Instant now = Instant.parse("2026-05-11T12:00:00Z");

	@Test
	public void classifiesBuyOfferAboveThresholdAsRisky()
	{
		GeOfferInsight insight = build(OfferSide.BUY, 103, price(108, 100)).get();

		assertEquals(GeInsightStatus.RISKY, insight.getStatus());
		assertEquals("Risky buy: +3.0% over low", insight.getStatusText());
		assertTrue(insight.hasWarning());
	}

	@Test
	public void classifiesBuyOfferAtThresholdAsNeutral()
	{
		GeOfferInsight insight = build(OfferSide.BUY, 102, price(108, 100)).get();

		assertEquals(GeInsightStatus.NEUTRAL, insight.getStatus());
		assertEquals("Fair buy: +2.0% over low", insight.getStatusText());
		assertFalse(insight.hasWarning());
	}

	@Test
	public void classifiesBuyOfferAtOrBelowLowAsFavorable()
	{
		GeOfferInsight insight = build(OfferSide.BUY, 99, price(108, 100)).get();

		assertEquals(GeInsightStatus.FAVORABLE, insight.getStatus());
		assertEquals("Favorable buy: -1.0% under low", insight.getStatusText());
	}

	@Test
	public void classifiesSellOfferBelowThresholdAsRisky()
	{
		GeOfferInsight insight = build(OfferSide.SELL, 96, price(100, 90)).get();

		assertEquals(GeInsightStatus.RISKY, insight.getStatus());
		assertEquals("Risky sell: -4.0% under high", insight.getStatusText());
		assertTrue(insight.hasWarning());
	}

	@Test
	public void classifiesMissingReferencePriceAsUnavailable()
	{
		GeOfferInsight insight = build(OfferSide.SELL, 96, new PriceSnapshot(536, null, null, 90, now)).get();

		assertEquals(GeInsightStatus.UNAVAILABLE, insight.getStatus());
		assertEquals("Waiting for Wiki price", insight.getStatusText());
		assertFalse(insight.hasWarning());
	}

	@Test
	public void classifiesMissingTypedPriceAsUnavailable()
	{
		GeOfferInsight insight = build(OfferSide.BUY, 0, price(108, 100)).get();

		assertEquals(GeInsightStatus.UNAVAILABLE, insight.getStatus());
		assertEquals("Waiting for offer price", insight.getStatusText());
		assertFalse(insight.hasWarning());
	}

	@Test
	public void scoresStrongBuyOpportunityWithOfferAdjustedMargin()
	{
		GeOfferInsight insight = build(
			OfferSide.BUY,
			100,
			price(140, 100),
			market(140, 1_500, 100, 1_500),
			market(138, 15_000, 101, 15_000)
		).get();

		assertEquals("Strong", insight.getOpportunityLabel());
		assertTrue(insight.getOpportunityScore() >= 75);
		assertEquals(38, insight.getOpportunityNetMargin());
		assertEquals(38.0, insight.getOpportunityRoiPercent(), 0.001);
		assertEquals(3_000, insight.getFiveMinuteVolume());
		assertEquals(30_000, insight.getHourlyVolume());
	}

	@Test
	public void scoresAvoidWhenOfferAdjustedMarginIsNegative()
	{
		GeOfferInsight insight = build(
			OfferSide.BUY,
			140,
			price(120, 100),
			market(120, 900, 100, 900),
			market(121, 9_000, 101, 9_000)
		).get();

		assertEquals("Avoid", insight.getOpportunityLabel());
		assertEquals(0, insight.getOpportunityScore());
		assertEquals(-22, insight.getOpportunityNetMargin());
	}

	@Test
	public void scoresSellOpportunityAgainstLatestLowPrice()
	{
		GeOfferInsight insight = build(
			OfferSide.SELL,
			112,
			price(140, 100),
			market(140, 1_500, 100, 1_500),
			market(138, 15_000, 101, 15_000)
		).get();

		assertEquals("Fair", insight.getOpportunityLabel());
		assertTrue(insight.getOpportunityScore() >= 50);
		assertEquals(10, insight.getOpportunityNetMargin());
		assertEquals(10.0, insight.getOpportunityRoiPercent(), 0.001);
	}

	@Test
	public void marksOpportunityUnavailableWhenMarketPricesAreIncomplete()
	{
		GeOfferInsight insight = build(
			OfferSide.BUY,
			100,
			new PriceSnapshot(536, null, null, 100, now),
			null,
			null
		).get();

		assertEquals("Waiting for market data", insight.getOpportunityLabel());
		assertEquals(0, insight.getOpportunityScore());
		assertEquals(0, insight.getOpportunityNetMargin());
	}

	private Optional<GeOfferInsight> build(OfferSide side, int offerPrice, PriceSnapshot price)
	{
		SetupOfferSnapshot offer = new SetupOfferSnapshot(536, side, offerPrice, 1);
		return builder.build(offer, price, 2.0, 2.0, 5_000_000, now);
	}

	private Optional<GeOfferInsight> build(OfferSide side, int offerPrice, PriceSnapshot price, MarketSnapshot fiveMinute, MarketSnapshot hourly)
	{
		SetupOfferSnapshot offer = new SetupOfferSnapshot(536, side, offerPrice, 1);
		return builder.build(offer, price, fiveMinute, hourly, 2.0, 2.0, 5_000_000, now);
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

	private MarketSnapshot market(Integer avgHighPrice, Integer highPriceVolume, Integer avgLowPrice, Integer lowPriceVolume)
	{
		return new MarketSnapshot(536, avgHighPrice, highPriceVolume, avgLowPrice, lowPriceVolume);
	}
}
