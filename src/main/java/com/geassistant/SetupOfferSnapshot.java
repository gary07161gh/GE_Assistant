package com.geassistant;

final class SetupOfferSnapshot implements GeOfferInput
{
	private final int itemId;
	private final OfferSide side;
	private final int price;
	private final int totalQuantity;

	SetupOfferSnapshot(int itemId, OfferSide side, int price, int totalQuantity)
	{
		this.itemId = itemId;
		this.side = side;
		this.price = price;
		this.totalQuantity = totalQuantity;
	}

	@Override
	public int getItemId()
	{
		return itemId;
	}

	@Override
	public OfferSide getSide()
	{
		return side;
	}

	@Override
	public int getPrice()
	{
		return price;
	}

	@Override
	public int getTotalQuantity()
	{
		return totalQuantity;
	}
}
