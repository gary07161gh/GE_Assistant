package com.geassistant;

import java.util.Objects;

final class OfferSnapshot implements GeOfferInput
{
	private final int slot;
	private final int itemId;
	private final OfferSide side;
	private final int price;
	private final int totalQuantity;
	private final int quantityTraded;

	OfferSnapshot(int slot, int itemId, OfferSide side, int price, int totalQuantity, int quantityTraded)
	{
		this.slot = slot;
		this.itemId = itemId;
		this.side = side;
		this.price = price;
		this.totalQuantity = totalQuantity;
		this.quantityTraded = quantityTraded;
	}

	int getSlot()
	{
		return slot;
	}

	public int getItemId()
	{
		return itemId;
	}

	public OfferSide getSide()
	{
		return side;
	}

	public int getPrice()
	{
		return price;
	}

	public int getTotalQuantity()
	{
		return totalQuantity;
	}

	int getQuantityTraded()
	{
		return quantityTraded;
	}

	@Override
	public boolean equals(Object other)
	{
		if (this == other)
		{
			return true;
		}
		if (!(other instanceof OfferSnapshot))
		{
			return false;
		}
		OfferSnapshot that = (OfferSnapshot) other;
		return slot == that.slot
			&& itemId == that.itemId
			&& price == that.price
			&& totalQuantity == that.totalQuantity
			&& quantityTraded == that.quantityTraded
			&& side == that.side;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(slot, itemId, side, price, totalQuantity, quantityTraded);
	}
}
