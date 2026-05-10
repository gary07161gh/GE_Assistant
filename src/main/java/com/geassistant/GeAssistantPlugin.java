package com.geassistant;

import com.google.inject.Provides;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GrandExchangeOffer;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GrandExchangeOfferChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@PluginDescriptor(
	name = "GE Assistant"
)
public class GeAssistantPlugin extends Plugin
{
	private static final Logger log = LoggerFactory.getLogger(GeAssistantPlugin.class);
	private final GeWarningEvaluator evaluator = new GeWarningEvaluator();
	private final GeOfferInsightBuilder insightBuilder = new GeOfferInsightBuilder(evaluator);
	private final WikiPriceCache priceCache = new WikiPriceCache(new WikiLatestPriceClient(), Clock.systemUTC());
	private final GeOfferSnapshotReader snapshotReader = new GeOfferSnapshotReader();
	private final Map<Integer, OfferSnapshot> offers = new ConcurrentHashMap<>();
	private final Map<Integer, GeWarning> warnings = new ConcurrentHashMap<>();
	private final Map<Integer, GeOfferInsight> insights = new ConcurrentHashMap<>();
	private final AtomicBoolean refreshInFlight = new AtomicBoolean();
	private ExecutorService executor;

	@Inject
	private Client client;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private GeAssistantOverlay overlay;

	@Inject
	private GeAssistantConfig config;

	@Override
	protected void startUp()
	{
		executor = Executors.newSingleThreadExecutor(new DaemonThreadFactory());
		overlayManager.add(overlay);
		syncCurrentOffers();
		refreshPricesAsync();
		log.info("GE Assistant started");
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		offers.clear();
		warnings.clear();
		insights.clear();
		if (executor != null)
		{
			executor.shutdownNow();
			executor = null;
		}
	}

	@Subscribe
	public void onGrandExchangeOfferChanged(GrandExchangeOfferChanged event)
	{
		if (recordOffer(event.getSlot(), event.getOffer()))
		{
			refreshPricesAsync();
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (syncCurrentOffers() && !offers.isEmpty())
		{
			refreshPricesAsync();
		}
	}

	Collection<GeWarning> getWarnings()
	{
		return Collections.unmodifiableCollection(warnings.values());
	}

	private boolean syncCurrentOffers()
	{
		GrandExchangeOffer[] currentOffers = client.getGrandExchangeOffers();
		if (currentOffers == null)
		{
			boolean changed = !offers.isEmpty() || !warnings.isEmpty() || !insights.isEmpty();
			offers.clear();
			warnings.clear();
			insights.clear();
			return changed;
		}

		boolean changed = false;
		for (int slot = 0; slot < currentOffers.length; slot++)
		{
			changed |= recordOffer(slot, currentOffers[slot]);
		}

		return changed;
	}

	private boolean recordOffer(int slot, GrandExchangeOffer offer)
	{
		OfferSnapshot snapshot = snapshotReader.fromOffer(slot, offer);
		if (snapshot == null)
		{
			boolean removed = offers.remove(slot) != null;
			warnings.remove(slot);
			insights.remove(slot);
			if (removed)
			{
				log.debug("GE Assistant cleared offer slot {}", slot);
			}
			return removed;
		}

		OfferSnapshot previous = offers.put(slot, snapshot);
		evaluateOffer(snapshot);
		if (!snapshot.equals(previous))
		{
			log.info("GE Assistant saw {} slot {} item {} price {} qty {}",
				snapshot.getSide(), snapshot.getSlot(), snapshot.getItemId(), snapshot.getPrice(), snapshot.getTotalQuantity());
		}
		return !snapshot.equals(previous);
	}

	private void refreshPricesAsync()
	{
		if (!config.enableWikiPrices() || executor == null || !refreshInFlight.compareAndSet(false, true))
		{
			return;
		}

		try
		{
			executor.submit(() -> {
				try
				{
					boolean refreshed = priceCache.refreshIfNeeded(Duration.ofSeconds(Math.max(5, config.priceRefreshSeconds())));
					if (refreshed)
					{
						log.info("GE Assistant refreshed {} Wiki prices", priceCache.size());
						recomputeWarnings();
					}
					else if (priceCache.getLastError() != null)
					{
						log.warn("GE Assistant Wiki price refresh failed: {}", priceCache.getLastError());
					}
				}
				finally
				{
					refreshInFlight.set(false);
				}
			});
		}
		catch (RejectedExecutionException ex)
		{
			refreshInFlight.set(false);
		}
	}

	private void recomputeWarnings()
	{
		if (!config.enableWikiPrices())
		{
			warnings.clear();
			insights.clear();
			return;
		}

		for (OfferSnapshot offer : offers.values())
		{
			evaluateOffer(offer);
		}
	}

	private void evaluateOffer(OfferSnapshot offer)
	{
		if (!config.enableWikiPrices())
		{
			warnings.clear();
			insights.clear();
			return;
		}

		priceCache.get(offer.getItemId())
			.flatMap(price -> insightBuilder.build(
				offer,
				price,
				Math.max(0, config.warningThresholdPercent()),
				Math.max(0, config.taxPercent()),
				Math.max(0, config.taxCapGp())
			))
			.ifPresentOrElse(
				insight -> {
					insights.put(offer.getSlot(), insight);
					if (insight.hasWarning())
					{
						GeWarning warning = insight.getWarning();
						warnings.put(offer.getSlot(), warning);
						log.info("GE Assistant warning slot {} item {} offer {} ref {} pct {}",
							offer.getSlot(), offer.getItemId(), offer.getPrice(), warning.getReferencePrice(), warning.getBadgeText());
					}
					else
					{
						warnings.remove(offer.getSlot());
					}
				},
				() -> {
					warnings.remove(offer.getSlot());
					insights.remove(offer.getSlot());
				}
			);
	}

	Collection<GeOfferInsight> getInsights()
	{
		return Collections.unmodifiableCollection(insights.values());
	}

	int getOfferCount()
	{
		return offers.size();
	}

	int getPriceCount()
	{
		return priceCache.size();
	}

	boolean isRefreshInFlight()
	{
		return refreshInFlight.get();
	}

	String getPriceError()
	{
		return priceCache.getLastError();
	}

	Optional<GeOfferInsight> getSetupInsight(SetupOfferSnapshot offer)
	{
		if (!config.enableWikiPrices() || offer == null)
		{
			return Optional.empty();
		}
		return insightBuilder.build(
			offer,
			priceCache.get(offer.getItemId()).orElse(null),
			Math.max(0, config.warningThresholdPercent()),
			Math.max(0, config.taxPercent()),
			Math.max(0, config.taxCapGp()),
			Instant.now()
		);
	}

	@Provides
	GeAssistantConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(GeAssistantConfig.class);
	}

	private static final class DaemonThreadFactory implements ThreadFactory
	{
		@Override
		public Thread newThread(Runnable runnable)
		{
			Thread thread = new Thread(runnable, "ge-assistant-price-refresh");
			thread.setDaemon(true);
			return thread;
		}
	}
}
