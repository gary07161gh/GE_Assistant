package com.geassistant;

import java.util.Optional;

final class GeWarningEvaluator
{
	Optional<GeWarning> evaluate(OfferSnapshot offer, PriceSnapshot price, double thresholdPercent, double taxPercent, int taxCapGp)
	{
		if (offer == null || price == null || offer.getPrice() <= 0)
		{
			return Optional.empty();
		}

		int rawSpread = rawSpread(price);
		int taxAdjustedMargin = taxAdjustedMargin(price, taxPercent, taxCapGp);

		if (offer.getSide() == OfferSide.BUY)
		{
			Integer low = price.getLow();
			if (low == null || low <= 0)
			{
				return Optional.empty();
			}

			double percent = ((offer.getPrice() - low) * 100.0) / low;
			return percent > thresholdPercent
				? Optional.of(new GeWarning(offer, price, low, percent, rawSpread, taxAdjustedMargin))
				: Optional.empty();
		}

		Integer high = price.getHigh();
		if (high == null || high <= 0)
		{
			return Optional.empty();
		}

		double percent = ((offer.getPrice() - high) * 100.0) / high;
		return percent < -thresholdPercent
			? Optional.of(new GeWarning(offer, price, high, percent, rawSpread, taxAdjustedMargin))
			: Optional.empty();
	}

	private int rawSpread(PriceSnapshot price)
	{
		if (price.getHigh() == null || price.getLow() == null)
		{
			return 0;
		}
		return price.getHigh() - price.getLow();
	}

	private int taxAdjustedMargin(PriceSnapshot price, double taxPercent, int taxCapGp)
	{
		if (price.getHigh() == null || price.getLow() == null)
		{
			return 0;
		}

		int tax = (int) Math.floor(price.getHigh() * Math.max(0.0, taxPercent) / 100.0);
		if (taxCapGp > 0)
		{
			tax = Math.min(tax, taxCapGp);
		}
		return price.getHigh() - tax - price.getLow();
	}
}

