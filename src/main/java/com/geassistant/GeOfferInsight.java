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
	private final String opportunityLabel;
	private final int opportunityScore;
	private final int opportunityNetMargin;
	private final double opportunityRoiPercent;
	private final int fiveMinuteVolume;
	private final int hourlyVolume;
	private final String opportunityTrendText;

	GeOfferInsight(GeOfferInput offer, PriceSnapshot price, GeWarning warning, double signedPercent, int rawSpread, int taxAdjustedMargin)
	{
		this(offer, price, warning, signedPercent, rawSpread, taxAdjustedMargin, warning == null ? GeInsightStatus.NEUTRAL : GeInsightStatus.RISKY, "", "n/a", "n/a");
	}

	GeOfferInsight(GeOfferInput offer, PriceSnapshot price, GeWarning warning, double signedPercent, int rawSpread, int taxAdjustedMargin,
		GeInsightStatus status, String statusText, String highAgeText, String lowAgeText)
	{
		this(offer, price, warning, signedPercent, rawSpread, taxAdjustedMargin, status, statusText, highAgeText, lowAgeText,
			"Waiting for market data", 0, 0, 0.0, 0, 0, "No trend data");
	}

	GeOfferInsight(GeOfferInput offer, PriceSnapshot price, GeWarning warning, double signedPercent, int rawSpread, int taxAdjustedMargin,
		GeInsightStatus status, String statusText, String highAgeText, String lowAgeText, String opportunityLabel,
		int opportunityScore, int opportunityNetMargin, double opportunityRoiPercent, int fiveMinuteVolume, int hourlyVolume,
		String opportunityTrendText)
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
		this.opportunityLabel = opportunityLabel;
		this.opportunityScore = opportunityScore;
		this.opportunityNetMargin = opportunityNetMargin;
		this.opportunityRoiPercent = opportunityRoiPercent;
		this.fiveMinuteVolume = fiveMinuteVolume;
		this.hourlyVolume = hourlyVolume;
		this.opportunityTrendText = opportunityTrendText;
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

	String getOpportunityLabel()
	{
		return opportunityLabel;
	}

	int getOpportunityScore()
	{
		return opportunityScore;
	}

	int getOpportunityNetMargin()
	{
		return opportunityNetMargin;
	}

	double getOpportunityRoiPercent()
	{
		return opportunityRoiPercent;
	}

	int getFiveMinuteVolume()
	{
		return fiveMinuteVolume;
	}

	int getHourlyVolume()
	{
		return hourlyVolume;
	}

	String getOpportunityTrendText()
	{
		return opportunityTrendText;
	}

	String getBadgeText()
	{
		return String.format(Locale.US, "%+.1f%%", signedPercent);
	}
}
