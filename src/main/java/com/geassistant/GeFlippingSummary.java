package com.geassistant;

import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;

final class GeFlippingSummary
{
	private final int offerCount;
	private final int riskyOfferCount;
	private final int projectedProfit;
	private final GeOfferInsight bestOpportunity;
	private final GeOfferInsight setupInsight;

	private GeFlippingSummary(int offerCount, int riskyOfferCount, int projectedProfit, GeOfferInsight bestOpportunity,
		GeOfferInsight setupInsight)
	{
		this.offerCount = offerCount;
		this.riskyOfferCount = riskyOfferCount;
		this.projectedProfit = projectedProfit;
		this.bestOpportunity = bestOpportunity;
		this.setupInsight = setupInsight;
	}

	static GeFlippingSummary from(Collection<GeOfferInsight> insights, Optional<GeOfferInsight> setupInsight)
	{
		int offerCount = 0;
		int riskyOfferCount = 0;
		int projectedProfit = 0;
		GeOfferInsight bestOpportunity = null;

		if (insights != null)
		{
			for (GeOfferInsight insight : insights)
			{
				if (insight == null)
				{
					continue;
				}

				offerCount++;
				if (insight.hasWarning())
				{
					riskyOfferCount++;
				}

				int profit = Math.max(0, insight.getOpportunityNetMargin()) * Math.max(0, insight.getOffer().getTotalQuantity());
				projectedProfit += profit;
				if (bestOpportunity == null || opportunityComparator().compare(insight, bestOpportunity) > 0)
				{
					bestOpportunity = insight;
				}
			}
		}

		return new GeFlippingSummary(offerCount, riskyOfferCount, projectedProfit, bestOpportunity, setupInsight.orElse(null));
	}

	int getOfferCount()
	{
		return offerCount;
	}

	int getRiskyOfferCount()
	{
		return riskyOfferCount;
	}

	int getProjectedProfit()
	{
		return projectedProfit;
	}

	Optional<GeOfferInsight> getBestOpportunity()
	{
		return Optional.ofNullable(bestOpportunity);
	}

	Optional<GeOfferInsight> getSetupInsight()
	{
		return Optional.ofNullable(setupInsight);
	}

	private static Comparator<GeOfferInsight> opportunityComparator()
	{
		return Comparator.comparingInt(GeOfferInsight::getOpportunityScore)
			.thenComparingInt(GeOfferInsight::getOpportunityNetMargin);
	}
}
