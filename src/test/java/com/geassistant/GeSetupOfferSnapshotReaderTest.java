package com.geassistant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Optional;
import org.junit.Test;

public class GeSetupOfferSnapshotReaderTest
{
	private final GeSetupOfferSnapshotReader reader = new GeSetupOfferSnapshotReader();

	@Test
	public void returnsEmptyWhenNoItemIsSelected()
	{
		Optional<SetupOfferSnapshot> snapshot = reader.fromWidgetTexts(Arrays.asList(
			"Grand Exchange: Set up offer",
			"Buy offer",
			"Price per item: 1,670 coins"
		));

		assertFalse(snapshot.isPresent());
	}

	@Test
	public void readsSelectedBuyItemWithPriceAndQuantity()
	{
		Optional<SetupOfferSnapshot> snapshot = reader.fromWidgetTexts(Arrays.asList(
			"Grand Exchange: Set up offer",
			"Buy offer",
			"Item: Nature rune",
			"Item id: 561",
			"Price per item: 167 gp",
			"Quantity: 1,000"
		));

		assertTrue(snapshot.isPresent());
		assertEquals(561, snapshot.get().getItemId());
		assertEquals(OfferSide.BUY, snapshot.get().getSide());
		assertEquals(167, snapshot.get().getPrice());
		assertEquals(1_000, snapshot.get().getTotalQuantity());
	}

	@Test
	public void readsSelectedItemWithoutTypedPriceAsUnavailableInput()
	{
		Optional<SetupOfferSnapshot> snapshot = reader.fromWidgetTexts(Arrays.asList(
			"Grand Exchange: Set up offer",
			"Sell offer",
			"Item: Nature rune",
			"Item id: 561",
			"Quantity: 250"
		));

		assertTrue(snapshot.isPresent());
		assertEquals(OfferSide.SELL, snapshot.get().getSide());
		assertEquals(0, snapshot.get().getPrice());
		assertEquals(250, snapshot.get().getTotalQuantity());
	}

	@Test
	public void returnsEmptyWhenSideIsUnreadable()
	{
		Optional<SetupOfferSnapshot> snapshot = reader.fromWidgetTexts(Arrays.asList(
			"Grand Exchange: Set up offer",
			"Item: Nature rune",
			"Item id: 561",
			"Price per item: 167 gp"
		));

		assertFalse(snapshot.isPresent());
	}
}
