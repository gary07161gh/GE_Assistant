package com.geassistant;

import java.util.Optional;

final class GeOfferInsightBuilder
{
	private final GeWarningEvaluator evaluator;

	GeOfferInsightBuilder(GeWarningEvaluator evaluator)
	{
		this.evaluator = evaluator;
	}

	Optional<GeOfferInsight> build(OfferSnapshot offer, PriceSnapshot price, double thresholdPercent, double taxPercent, int taxCapGp)
	{
		if (offer == null || price == null || offer.getPrice() <= 0)
		{
			return Optional.empty();
		}

		Integer reference = offer.getSide() == OfferSide.BUY ? price.getLow() : price.getHigh();
		if (reference == null || reference <= 0)
		{
			return Optional.empty();
		}

		double signedPercent = ((offer.getPrice() - reference) * 100.0) / reference;
		int rawSpread = rawSpread(price);
		int taxAdjustedMargin = taxAdjustedMargin(price, taxPercent, taxCapGp);
		GeWarning warning = evaluator.evaluate(offer, price, thresholdPercent, taxPercent, taxCapGp).orElse(null);
		return Optional.of(new GeOfferInsight(offer, price, warning, signedPercent, rawSpread, taxAdjustedMargin));
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
