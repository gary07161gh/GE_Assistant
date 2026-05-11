package com.geassistant;

import static org.junit.Assert.assertEquals;

import java.time.Instant;
import org.junit.Test;

public class GeFlippingPanelTest
{
	private final GeOfferInsightBuilder builder = new GeOfferInsightBuilder(new GeWarningEvaluator());
	private final Instant now = Instant.parse("2026-05-11T12:00:00Z");

	@Test
	public void resolvesItemNameForSidebarRows()
	{
		GeFlippingPanel panel = new GeFlippingPanel(itemId -> "Dragon bones");

		assertEquals("Dragon bones", panel.itemLabel(insight()));
	}

	@Test
	public void formatsSetupActionFromSuggestedFlipPrices()
	{
		GeFlippingPanel panel = new GeFlippingPanel(itemId -> "Dragon bones");

		assertEquals("Buy 100 gp / sell 140 gp", panel.setupAction(insight()));
	}

	private GeOfferInsight insight()
	{
		SetupOfferSnapshot offer = new SetupOfferSnapshot(536, OfferSide.BUY, 100, 1);
		return builder.build(
			offer,
			new PriceSnapshot(536, 140, now.minusSeconds(60), 100, now.minusSeconds(120)),
			new MarketSnapshot(536, 140, 1_500, 100, 1_500),
			new MarketSnapshot(536, 138, 15_000, 101, 15_000),
			2.0,
			2.0,
			5_000_000,
			now
		).get();
	}
}
