package com.geassistant;

import java.util.Locale;

final class GeWarning
{
	private final OfferSnapshot offer;
	private final PriceSnapshot price;
	private final int referencePrice;
	private final double signedPercent;
	private final int rawSpread;
	private final int taxAdjustedMargin;

	GeWarning(OfferSnapshot offer, PriceSnapshot price, int referencePrice, double signedPercent, int rawSpread, int taxAdjustedMargin)
	{
		this.offer = offer;
		this.price = price;
		this.referencePrice = referencePrice;
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

	int getReferencePrice()
	{
		return referencePrice;
	}

	double getSignedPercent()
	{
		return signedPercent;
	}

	double getAdversePercent()
	{
		return Math.abs(signedPercent);
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

