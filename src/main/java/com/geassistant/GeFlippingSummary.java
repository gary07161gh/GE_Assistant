package com.geassistant;

import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;

final class GeFlippingSummary
{
	private final int offerCount;
	private final int riskyOfferCount;
	private final int projectedProfit;
	private final int projectedLoss;
	private final GeOfferInsight bestOpportunity;
	private final GeOfferInsight setupInsight;

	private GeFlippingSummary(int offerCount, int riskyOfferCount, int projectedProfit, int projectedLoss, GeOfferInsight bestOpportunity,
		GeOfferInsight setupInsight)
	{
		this.offerCount = offerCount;
		this.riskyOfferCount = riskyOfferCount;
		this.projectedProfit = projectedProfit;
		this.projectedLoss = projectedLoss;
		this.bestOpportunity = bestOpportunity;
		this.setupInsight = setupInsight;
	}

	static GeFlippingSummary from(Collection<GeOfferInsight> insights, Optional<GeOfferInsight> setupInsight)
	{
		int offerCount = 0;
		int riskyOfferCount = 0;
		int projectedProfit = 0;
		int projectedLoss = 0;
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

				int projected = insight.getOpportunityNetMargin() * Math.max(0, insight.getOffer().getTotalQuantity());
				if (projected >= 0)
				{
					projectedProfit += projected;
				}
				else
				{
					projectedLoss += Math.abs(projected);
				}

				if (insight.getOpportunityNetMargin() > 0
					&& (bestOpportunity == null || opportunityComparator().compare(insight, bestOpportunity) > 0))
				{
					bestOpportunity = insight;
				}
			}
		}

		return new GeFlippingSummary(offerCount, riskyOfferCount, projectedProfit, projectedLoss, bestOpportunity, setupInsight.orElse(null));
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

	int getProjectedLoss()
	{
		return projectedLoss;
	}

	int getNetProjectedProfit()
	{
		return projectedProfit - projectedLoss;
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
