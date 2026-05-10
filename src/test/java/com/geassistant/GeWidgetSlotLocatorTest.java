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
	public void keepsBottomRowSyntheticSlotsSeparate()
	{
		Rectangle root = new Rectangle(18, 14, 477, 290);

		Rectangle slotFive = locator.syntheticOfferSlotBounds(root, 5).get();
		Rectangle slotSix = locator.syntheticOfferSlotBounds(root, 6).get();
		Rectangle slotSeven = locator.syntheticOfferSlotBounds(root, 7).get();

		assertFalse(slotFive.intersects(slotSix));
		assertFalse(slotSix.intersects(slotSeven));
		assertTrue(slotFive.contains(slotFive.x + slotFive.width / 2, slotFive.y + slotFive.height / 2));
		assertFalse(slotSix.contains(slotFive.x + slotFive.width / 2, slotFive.y + slotFive.height / 2));
	}

	@Test
	public void rejectsInvalidSyntheticSlots()
	{
		assertFalse(locator.syntheticOfferSlotBounds(new Rectangle(0, 0, 477, 290), -1).isPresent());
		assertFalse(locator.syntheticOfferSlotBounds(new Rectangle(0, 0, 477, 290), 8).isPresent());
		assertFalse(locator.syntheticOfferSlotBounds(null, 0).isPresent());
	}

	@Test
	public void anchorsBadgeToBottomRightOfSlot()
	{
		Rectangle badge = GeAssistantOverlay.badgeBounds(new Rectangle(100, 200, 112, 109), 38, 17);

		assertEquals(new Rectangle(170, 288, 38, 17), badge);
	}

	@Test
	public void detectsOverviewTitleWithoutMatchingSetupOfferTitle()
	{
		assertTrue(GeWidgetSlotLocator.isOverviewTitle("Grand Exchange"));
		assertFalse(GeWidgetSlotLocator.isOverviewTitle("Grand Exchange: Set up offer"));
	}

	@Test
	public void detectsOverviewInstruction()
	{
		assertTrue(GeWidgetSlotLocator.isOverviewInstruction("Select an offer slot to set up or view an offer."));
		assertFalse(GeWidgetSlotLocator.isOverviewInstruction("Buy offer"));
		assertFalse(GeWidgetSlotLocator.isOverviewInstruction(null));
	}
}
