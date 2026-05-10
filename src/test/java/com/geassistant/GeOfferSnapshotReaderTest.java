package com.geassistant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import net.runelite.api.GrandExchangeOffer;
import net.runelite.api.GrandExchangeOfferState;
import org.junit.Test;

public class GeOfferSnapshotReaderTest
{
	private final GeOfferSnapshotReader reader = new GeOfferSnapshotReader();

	@Test
	public void readsBuyAndSellOffersFromCurrentClientState()
	{
		OfferSnapshot buy = reader.fromOffer(2, offer(GrandExchangeOfferState.BUYING, 1001, 5_000));
		OfferSnapshot sell = reader.fromOffer(3, offer(GrandExchangeOfferState.SELLING, 1002, 7_500));

		assertEquals(2, buy.getSlot());
		assertEquals(1001, buy.getItemId());
		assertEquals(OfferSide.BUY, buy.getSide());
		assertEquals(5_000, buy.getPrice());
		assertEquals(3, sell.getSlot());
		assertEquals(1002, sell.getItemId());
		assertEquals(OfferSide.SELL, sell.getSide());
		assertEquals(7_500, sell.getPrice());
	}

	@Test
	public void ignoresEmptyOrInvalidOffers()
	{
		assertNull(reader.fromOffer(0, null));
		assertNull(reader.fromOffer(0, offer(GrandExchangeOfferState.EMPTY, 1001, 5_000)));
		assertNull(reader.fromOffer(0, offer(GrandExchangeOfferState.BUYING, 0, 5_000)));
		assertNull(reader.fromOffer(0, offer(GrandExchangeOfferState.BUYING, 1001, 0)));
	}

	private GrandExchangeOffer offer(GrandExchangeOfferState state, int itemId, int price)
	{
		return new FakeOffer(state, itemId, price);
	}

	private static final class FakeOffer implements GrandExchangeOffer
	{
		private final GrandExchangeOfferState state;
		private final int itemId;
		private final int price;

		private FakeOffer(GrandExchangeOfferState state, int itemId, int price)
		{
			this.state = state;
			this.itemId = itemId;
			this.price = price;
		}

		@Override
		public int getQuantitySold()
		{
			return 1;
		}

		@Override
		public int getItemId()
		{
			return itemId;
		}

		@Override
		public int getTotalQuantity()
		{
			return 1;
		}

		@Override
		public int getPrice()
		{
			return price;
		}

		@Override
		public int getSpent()
		{
			return 0;
		}

		@Override
		public GrandExchangeOfferState getState()
		{
			return state;
		}
	}
}
