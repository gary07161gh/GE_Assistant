package com.geassistant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.awt.Rectangle;
import java.util.Optional;
import org.junit.Test;

public class GeWidgetSlotLocatorTest
{
	private final GeWidgetSlotLocator locator = new GeWidgetSlotLocator();

	@Test
	public void synthesizesEightSlotBoundsFromGrandExchangePanelBounds()
	{
		Rectangle root = new Rectangle(54, 74, 477, 290);

		Optional<Rectangle> first = locator.syntheticOfferSlotBounds(root, 0);
		Optional<Rectangle> second = locator.syntheticOfferSlotBounds(root, 1);
		Optional<Rectangle> fifth = locator.syntheticOfferSlotBounds(root, 4);

		assertTrue(first.isPresent());
		assertEquals(new Rectangle(61, 133, 112, 109), first.get());
		assertEquals(new Rectangle(177, 133, 112, 109), second.get());
		assertEquals(new Rectangle(61, 246, 112, 109), fifth.get());
	}

	@Test
	public void rejectsInvalidSyntheticSlots()
	{
		assertFalse(locator.syntheticOfferSlotBounds(new Rectangle(0, 0, 477, 290), -1).isPresent());
		assertFalse(locator.syntheticOfferSlotBounds(new Rectangle(0, 0, 477, 290), 8).isPresent());
		assertFalse(locator.syntheticOfferSlotBounds(null, 0).isPresent());
	}
}
