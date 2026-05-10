package com.geassistant;

import net.runelite.api.GrandExchangeOffer;
import net.runelite.api.GrandExchangeOfferState;

final class GeOfferSnapshotReader
{
	OfferSnapshot fromOffer(int slot, GrandExchangeOffer offer)
	{
		if (offer == null || offer.getItemId() <= 0 || offer.getPrice() <= 0)
		{
			return null;
		}

		GrandExchangeOfferState state = offer.getState();
		OfferSide side;
		if (state == GrandExchangeOfferState.BUYING || state == GrandExchangeOfferState.BOUGHT || state == GrandExchangeOfferState.CANCELLED_BUY)
		{
			side = OfferSide.BUY;
		}
		else if (state == GrandExchangeOfferState.SELLING || state == GrandExchangeOfferState.SOLD || state == GrandExchangeOfferState.CANCELLED_SELL)
		{
			side = OfferSide.SELL;
		}
		else
		{
			return null;
		}

		return new OfferSnapshot(slot, offer.getItemId(), side, offer.getPrice(), offer.getTotalQuantity(), offer.getQuantitySold());
	}
}
