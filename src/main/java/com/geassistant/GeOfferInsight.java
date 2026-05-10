package com.geassistant;

import java.util.Locale;

final class GeOfferInsight
{
	private final OfferSnapshot offer;
	private final PriceSnapshot price;
	private final GeWarning warning;
	private final double signedPercent;
	private final int rawSpread;
	private final int taxAdjustedMargin;

	GeOfferInsight(OfferSnapshot offer, PriceSnapshot price, GeWarning warning, double signedPercent, int rawSpread, int taxAdjustedMargin)
	{
		this.offer = offer;
		this.price = price;
		this.warning = warning;
		this.signedPercent = signedPercent;
		this.rawSpread = rawSpread;
		this.taxAdjustedMargin = taxAdjustedMargin;
	}

	OfferSnapshot getOffer()
	{
		return offer;
	}

	PriceSnapshot getPrice()
	{
		return price;
	}

	GeWarning getWarning()
	{
		return warning;
	}

	boolean hasWarning()
	{
		return warning != null;
	}

	double getSignedPercent()
	{
		return signedPercent;
	}

	int getRawSpread()
	{
		return rawSpread;
	}

	int getTaxAdjustedMargin()
	{
		return taxAdjustedMargin;
	}

	String getBadgeText()
	{
		return String.format(Locale.US, "%+.1f%%", signedPercent);
	}
}
