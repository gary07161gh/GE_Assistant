package com.geassistant;

import com.google.inject.Provides;
import java.time.Clock;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
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

@PluginDescriptor(
	name = "GE Assistant"
)
public class GeAssistantPlugin extends Plugin
{
	private final GeWarningEvaluator evaluator = new GeWarningEvaluator();
	private final WikiPriceCache priceCache = new WikiPriceCache(new WikiLatestPriceClient(), Clock.systemUTC());
	private final GeOfferSnapshotReader snapshotReader = new GeOfferSnapshotReader();
	private final Map<Integer, OfferSnapshot> offers = new ConcurrentHashMap<>();
	private final Map<Integer, GeWarning> warnings = new ConcurrentHashMap<>();
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
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		offers.clear();
		warnings.clear();
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
			boolean changed = !offers.isEmpty() || !warnings.isEmpty();
			offers.clear();
			warnings.clear();
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
			return removed;
		}

		OfferSnapshot previous = offers.put(slot, snapshot);
		evaluateOffer(snapshot);
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
						recomputeWarnings();
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
			return;
		}

		priceCache.get(offer.getItemId())
			.flatMap(price -> evaluator.evaluate(
				offer,
				price,
				Math.max(0, config.warningThresholdPercent()),
				Math.max(0, config.taxPercent()),
				Math.max(0, config.taxCapGp())
			))
			.ifPresentOrElse(
				warning -> warnings.put(offer.getSlot(), warning),
				() -> warnings.remove(offer.getSlot())
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
