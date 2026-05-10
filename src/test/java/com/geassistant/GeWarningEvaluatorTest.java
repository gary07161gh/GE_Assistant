package com.geassistant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.util.Optional;
import org.junit.Test;

public class GeWarningEvaluatorTest
{
	private final GeWarningEvaluator evaluator = new GeWarningEvaluator();
	private final Instant now = Instant.parse("2026-05-10T20:00:00Z");

	@Test
	public void warnsWhenBuyOfferIsMoreThanThresholdAboveLatestLow()
	{
		OfferSnapshot offer = new OfferSnapshot(1, 536, OfferSide.BUY, 103, 1_000, 0);
		PriceSnapshot price = new PriceSnapshot(536, 108, now, 100, now);

		Optional<GeWarning> warning = evaluator.evaluate(offer, price, 2.0, 2.0, 5_000_000);

		assertTrue(warning.isPresent());
		assertEquals("+3.0%", warning.get().getBadgeText());
		assertEquals(3.0, warning.get().getAdversePercent(), 0.001);
	}

	@Test
	public void doesNotWarnWhenBuyOfferIsAtThreshold()
	{
		OfferSnapshot offer = new OfferSnapshot(1, 536, OfferSide.BUY, 102, 1_000, 0);
		PriceSnapshot price = new PriceSnapshot(536, 108, now, 100, now);

		assertFalse(evaluator.evaluate(offer, price, 2.0, 2.0, 5_000_000).isPresent());
	}

	@Test
	public void warnsWhenSellOfferIsMoreThanThresholdBelowLatestHigh()
	{
		OfferSnapshot offer = new OfferSnapshot(2, 536, OfferSide.SELL, 96, 1_000, 0);
		PriceSnapshot price = new PriceSnapshot(536, 100, now, 90, now);

		Optional<GeWarning> warning = evaluator.evaluate(offer, price, 2.0, 2.0, 5_000_000);

		assertTrue(warning.isPresent());
		assertEquals("-4.0%", warning.get().getBadgeText());
		assertEquals(4.0, warning.get().getAdversePercent(), 0.001);
	}

	@Test
	public void ignoresMissingReferencePrices()
	{
		OfferSnapshot buyOffer = new OfferSnapshot(1, 536, OfferSide.BUY, 103, 1_000, 0);
		OfferSnapshot sellOffer = new OfferSnapshot(2, 536, OfferSide.SELL, 96, 1_000, 0);
		PriceSnapshot noLow = new PriceSnapshot(536, 100, now, null, null);
		PriceSnapshot noHigh = new PriceSnapshot(536, null, null, 90, now);

		assertFalse(evaluator.evaluate(buyOffer, noLow, 2.0, 2.0, 5_000_000).isPresent());
		assertFalse(evaluator.evaluate(sellOffer, noHigh, 2.0, 2.0, 5_000_000).isPresent());
	}

	@Test
	public void calculatesTaxAdjustedMargin()
	{
		OfferSnapshot offer = new OfferSnapshot(1, 536, OfferSide.BUY, 1_021, 1_000, 0);
		PriceSnapshot price = new PriceSnapshot(536, 1_100, now, 1_000, now);

		GeWarning warning = evaluator.evaluate(offer, price, 2.0, 2.0, 5_000_000).get();

		assertEquals(100, warning.getRawSpread());
		assertEquals(78, warning.getTaxAdjustedMargin());
	}
}
