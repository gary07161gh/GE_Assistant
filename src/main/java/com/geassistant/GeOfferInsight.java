package com.geassistant;

import java.util.Locale;

final class GeOfferInsight
{
	private final GeOfferInput offer;
	private final PriceSnapshot price;
	private final GeWarning warning;
	private final double signedPercent;
	private final int rawSpread;
	private final int taxAdjustedMargin;
	private final GeInsightStatus status;
	private final String statusText;
	private final String highAgeText;
	private final String lowAgeText;

	GeOfferInsight(GeOfferInput offer, PriceSnapshot price, GeWarning warning, double signedPercent, int rawSpread, int taxAdjustedMargin)
	{
		this(offer, price, warning, signedPercent, rawSpread, taxAdjustedMargin, warning == null ? GeInsightStatus.NEUTRAL : GeInsightStatus.RISKY, "", "n/a", "n/a");
	}

	GeOfferInsight(GeOfferInput offer, PriceSnapshot price, GeWarning warning, double signedPercent, int rawSpread, int taxAdjustedMargin,
		GeInsightStatus status, String statusText, String highAgeText, String lowAgeText)
	{
		this.offer = offer;
		this.price = price;
		this.warning = warning;
		this.signedPercent = signedPercent;
		this.rawSpread = rawSpread;
		this.taxAdjustedMargin = taxAdjustedMargin;
		this.status = status;
		this.statusText = statusText;
		this.highAgeText = highAgeText;
		this.lowAgeText = lowAgeText;
	}

	GeOfferInput getOffer()
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

	GeInsightStatus getStatus()
	{
		return status;
	}

	String getStatusText()
	{
		return statusText;
	}

	String getHighAgeText()
	{
		return highAgeText;
	}

	String getLowAgeText()
	{
		return lowAgeText;
	}

	String getBadgeText()
	{
		return String.format(Locale.US, "%+.1f%%", signedPercent);
	}
}
