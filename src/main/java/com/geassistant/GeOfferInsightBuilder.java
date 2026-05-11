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
		return build(offer, price, null, null, thresholdPercent, taxPercent, taxCapGp, Instant.now());
	}

	Optional<GeOfferInsight> build(GeOfferInput offer, PriceSnapshot price, double thresholdPercent, double taxPercent, int taxCapGp, Instant now)
	{
		return build(offer, price, null, null, thresholdPercent, taxPercent, taxCapGp, now);
	}

	Optional<GeOfferInsight> build(GeOfferInput offer, PriceSnapshot price, MarketSnapshot fiveMinute, MarketSnapshot hourly,
		double thresholdPercent, double taxPercent, int taxCapGp, Instant now)
	{
		if (offer == null || offer.getItemId() <= 0 || offer.getSide() == null)
		{
			return Optional.empty();
		}

		if (offer.getPrice() <= 0)
		{
			return Optional.of(unavailable(offer, price, fiveMinute, hourly, "Waiting for offer price", taxPercent, taxCapGp, now));
		}

		if (price == null)
		{
			return Optional.of(unavailable(offer, null, fiveMinute, hourly, "Waiting for Wiki price", taxPercent, taxCapGp, now));
		}

		Integer reference = offer.getSide() == OfferSide.BUY ? price.getLow() : price.getHigh();
		if (reference == null || reference <= 0)
		{
			return Optional.of(unavailable(offer, price, fiveMinute, hourly, "Waiting for Wiki price", taxPercent, taxCapGp, now));
		}

		double signedPercent = ((offer.getPrice() - reference) * 100.0) / reference;
		int rawSpread = rawSpread(price);
		int taxAdjustedMargin = taxAdjustedMargin(price, taxPercent, taxCapGp);
		GeWarning warning = evaluator.evaluate(offer, price, thresholdPercent, taxPercent, taxCapGp).orElse(null);
		GeInsightStatus status = status(offer, warning, signedPercent);
		OpportunityValues opportunity = opportunity(offer, price, fiveMinute, hourly, taxPercent, taxCapGp, now);
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
			PriceAgeFormatter.formatAge(price.getLowTime(), now),
			opportunity.label,
			opportunity.score,
			opportunity.netMargin,
			opportunity.roiPercent,
			opportunity.fiveMinuteVolume,
			opportunity.hourlyVolume,
			opportunity.trendText
		));
	}

	private GeOfferInsight unavailable(GeOfferInput offer, PriceSnapshot price, MarketSnapshot fiveMinute, MarketSnapshot hourly,
		String statusText, double taxPercent, int taxCapGp, Instant now)
	{
		OpportunityValues opportunity = opportunity(offer, price, fiveMinute, hourly, taxPercent, taxCapGp, now);
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
			price == null ? "n/a" : PriceAgeFormatter.formatAge(price.getLowTime(), now),
			opportunity.label,
			opportunity.score,
			opportunity.netMargin,
			opportunity.roiPercent,
			opportunity.fiveMinuteVolume,
			opportunity.hourlyVolume,
			opportunity.trendText
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

	private OpportunityValues opportunity(GeOfferInput offer, PriceSnapshot price, MarketSnapshot fiveMinute, MarketSnapshot hourly,
		double taxPercent, int taxCapGp, Instant now)
	{
		int fiveMinuteVolume = fiveMinute == null ? 0 : fiveMinute.getTotalVolume();
		int hourlyVolume = hourly == null ? 0 : hourly.getTotalVolume();
		String trendText = trendText(fiveMinute, hourly);

		if (offer == null || price == null || price.getHigh() == null || price.getLow() == null
			|| price.getHigh() <= 0 || price.getLow() <= 0)
		{
			return new OpportunityValues("Waiting for market data", 0, 0, 0.0, fiveMinuteVolume, hourlyVolume, trendText);
		}

		int buyPrice = offer.getSide() == OfferSide.BUY && offer.getPrice() > 0 ? offer.getPrice() : price.getLow();
		int sellPrice = offer.getSide() == OfferSide.SELL && offer.getPrice() > 0 ? offer.getPrice() : price.getHigh();
		int sellAfterTax = sellPriceAfterTax(sellPrice, taxPercent, taxCapGp);
		int netMargin = offer.getSide() == OfferSide.SELL ? sellAfterTax - price.getLow() : sellAfterTax - buyPrice;
		double roiPercent = buyPrice <= 0 ? 0.0 : (netMargin * 100.0) / buyPrice;

		if (netMargin < 0)
		{
			return new OpportunityValues("Avoid", 0, netMargin, roiPercent, fiveMinuteVolume, hourlyVolume, trendText);
		}

		int score = opportunityScore(netMargin, roiPercent, fiveMinuteVolume, hourlyVolume, price, fiveMinute, hourly, now);
		return new OpportunityValues(opportunityLabel(score), score, netMargin, roiPercent, fiveMinuteVolume, hourlyVolume, trendText);
	}

	private int opportunityScore(int netMargin, double roiPercent, int fiveMinuteVolume, int hourlyVolume, PriceSnapshot price,
		MarketSnapshot fiveMinute, MarketSnapshot hourly, Instant now)
	{
		double roiScore = Math.min(30.0, Math.max(0.0, roiPercent));
		double marginScore = Math.min(20.0, Math.log10(netMargin + 1.0) * 4.0);
		double liquidityScore = Math.min(20.0, Math.log10(fiveMinuteVolume + 1.0) * 5.0)
			+ Math.min(10.0, Math.log10(hourlyVolume + 1.0) * 2.0);
		double freshnessScore = freshnessScore(price, now);
		double trendScore = trendScore(fiveMinute, hourly);
		return (int) Math.round(Math.max(0.0, Math.min(100.0,
			roiScore + marginScore + liquidityScore + freshnessScore + trendScore)));
	}

	private double freshnessScore(PriceSnapshot price, Instant now)
	{
		if (price.getHighTime() == null || price.getLowTime() == null)
		{
			return 0.0;
		}

		long ageSeconds = Math.max(
			Math.abs(now.getEpochSecond() - price.getHighTime().getEpochSecond()),
			Math.abs(now.getEpochSecond() - price.getLowTime().getEpochSecond())
		);
		if (ageSeconds <= 300)
		{
			return 15.0;
		}
		if (ageSeconds <= 900)
		{
			return 11.0;
		}
		if (ageSeconds <= 3600)
		{
			return 7.0;
		}
		return 3.0;
	}

	private double trendScore(MarketSnapshot fiveMinute, MarketSnapshot hourly)
	{
		if (!hasAverages(fiveMinute) || !hasAverages(hourly))
		{
			return 5.0;
		}

		double fiveMinuteMid = midpoint(fiveMinute);
		double hourlyMid = midpoint(hourly);
		if (hourlyMid <= 0)
		{
			return 5.0;
		}

		double changePercent = Math.abs((fiveMinuteMid - hourlyMid) * 100.0 / hourlyMid);
		if (changePercent <= 2.0)
		{
			return 15.0;
		}
		if (changePercent <= 5.0)
		{
			return 10.0;
		}
		return 5.0;
	}

	private String trendText(MarketSnapshot fiveMinute, MarketSnapshot hourly)
	{
		if (!hasAverages(fiveMinute) || !hasAverages(hourly))
		{
			return "No trend data";
		}

		double fiveMinuteMid = midpoint(fiveMinute);
		double hourlyMid = midpoint(hourly);
		if (hourlyMid <= 0)
		{
			return "No trend data";
		}

		double changePercent = (fiveMinuteMid - hourlyMid) * 100.0 / hourlyMid;
		if (Math.abs(changePercent) <= 2.0)
		{
			return "Stable";
		}
		return changePercent > 0 ? "Rising" : "Falling";
	}

	private boolean hasAverages(MarketSnapshot market)
	{
		return market != null
			&& market.getAvgHighPrice() != null && market.getAvgHighPrice() > 0
			&& market.getAvgLowPrice() != null && market.getAvgLowPrice() > 0;
	}

	private double midpoint(MarketSnapshot market)
	{
		return (market.getAvgHighPrice() + market.getAvgLowPrice()) / 2.0;
	}

	private int sellPriceAfterTax(int sellPrice, double taxPercent, int taxCapGp)
	{
		int tax = (int) Math.floor(sellPrice * Math.max(0.0, taxPercent) / 100.0);
		if (taxCapGp > 0)
		{
			tax = Math.min(tax, taxCapGp);
		}
		return sellPrice - tax;
	}

	private String opportunityLabel(int score)
	{
		if (score >= 75)
		{
			return "Strong";
		}
		if (score >= 50)
		{
			return "Fair";
		}
		if (score >= 25)
		{
			return "Weak";
		}
		return "Avoid";
	}

	private static final class OpportunityValues
	{
		private final String label;
		private final int score;
		private final int netMargin;
		private final double roiPercent;
		private final int fiveMinuteVolume;
		private final int hourlyVolume;
		private final String trendText;

		private OpportunityValues(String label, int score, int netMargin, double roiPercent, int fiveMinuteVolume, int hourlyVolume,
			String trendText)
		{
			this.label = label;
			this.score = score;
			this.netMargin = netMargin;
			this.roiPercent = roiPercent;
			this.fiveMinuteVolume = fiveMinuteVolume;
			this.hourlyVolume = hourlyVolume;
			this.trendText = trendText;
		}
	}
}
