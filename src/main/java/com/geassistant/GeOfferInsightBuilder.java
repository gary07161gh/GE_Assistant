package com.geassistant;

import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

final class GeOfferInsightBuilder
{
	private final GeWarningEvaluator evaluator;

	GeOfferInsightBuilder(GeWarningEvaluator evaluator)
	{
		this.evaluator = evaluator;
	}

	Optional<GeOfferInsight> build(GeOfferInput offer, PriceSnapshot price, double thresholdPercent, double taxPercent, int taxCapGp)
	{
		return build(offer, price, thresholdPercent, taxPercent, taxCapGp, Instant.now());
	}

	Optional<GeOfferInsight> build(GeOfferInput offer, PriceSnapshot price, double thresholdPercent, double taxPercent, int taxCapGp, Instant now)
	{
		if (offer == null || offer.getItemId() <= 0 || offer.getSide() == null)
		{
			return Optional.empty();
		}

		if (offer.getPrice() <= 0)
		{
			return Optional.of(unavailable(offer, price, "Waiting for offer price", taxPercent, taxCapGp, now));
		}

		if (price == null)
		{
			return Optional.of(unavailable(offer, null, "Waiting for Wiki price", taxPercent, taxCapGp, now));
		}

		Integer reference = offer.getSide() == OfferSide.BUY ? price.getLow() : price.getHigh();
		if (reference == null || reference <= 0)
		{
			return Optional.of(unavailable(offer, price, "Waiting for Wiki price", taxPercent, taxCapGp, now));
		}

		double signedPercent = ((offer.getPrice() - reference) * 100.0) / reference;
		int rawSpread = rawSpread(price);
		int taxAdjustedMargin = taxAdjustedMargin(price, taxPercent, taxCapGp);
		GeWarning warning = evaluator.evaluate(offer, price, thresholdPercent, taxPercent, taxCapGp).orElse(null);
		GeInsightStatus status = status(offer, warning, signedPercent);
		return Optional.of(new GeOfferInsight(
			offer,
			price,
			warning,
			signedPercent,
			rawSpread,
			taxAdjustedMargin,
			status,
			statusText(offer, status, signedPercent),
			PriceAgeFormatter.formatAge(price.getHighTime(), now),
			PriceAgeFormatter.formatAge(price.getLowTime(), now)
		));
	}

	private GeOfferInsight unavailable(GeOfferInput offer, PriceSnapshot price, String statusText, double taxPercent, int taxCapGp, Instant now)
	{
		return new GeOfferInsight(
			offer,
			price,
			null,
			0,
			rawSpread(price),
			taxAdjustedMargin(price, taxPercent, taxCapGp),
			GeInsightStatus.UNAVAILABLE,
			statusText,
			price == null ? "n/a" : PriceAgeFormatter.formatAge(price.getHighTime(), now),
			price == null ? "n/a" : PriceAgeFormatter.formatAge(price.getLowTime(), now)
		);
	}

	private GeInsightStatus status(GeOfferInput offer, GeWarning warning, double signedPercent)
	{
		if (warning != null)
		{
			return GeInsightStatus.RISKY;
		}
		if (offer.getSide() == OfferSide.BUY && signedPercent <= 0
			|| offer.getSide() == OfferSide.SELL && signedPercent >= 0)
		{
			return GeInsightStatus.FAVORABLE;
		}
		return GeInsightStatus.NEUTRAL;
	}

	private String statusText(GeOfferInput offer, GeInsightStatus status, double signedPercent)
	{
		String side = offer.getSide() == OfferSide.BUY ? "buy" : "sell";
		String reference = offer.getSide() == OfferSide.BUY ? "low" : "high";
		String direction = signedPercent < 0 ? "under" : "over";
		String label = status == GeInsightStatus.RISKY ? "Risky"
			: status == GeInsightStatus.FAVORABLE ? "Favorable" : "Fair";
		return String.format(Locale.US, "%s %s: %+.1f%% %s %s", label, side, signedPercent, direction, reference);
	}

	private int rawSpread(PriceSnapshot price)
	{
		if (price == null || price.getHigh() == null || price.getLow() == null)
		{
			return 0;
		}
		return price.getHigh() - price.getLow();
	}

	private int taxAdjustedMargin(PriceSnapshot price, double taxPercent, int taxCapGp)
	{
		if (price == null || price.getHigh() == null || price.getLow() == null)
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
