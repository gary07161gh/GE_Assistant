package com.geassistant;

import com.google.inject.Provides;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
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
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
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
	private volatile GeOfferInsight setupSidebarInsight;
	private ExecutorService executor;
	private GeFlippingPanel flippingPanel;
	private NavigationButton navigationButton;

	@Inject
	private Client client;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private GeAssistantOverlay overlay;

	@Inject
	private GeAssistantConfig config;

	@Override
	protected void startUp()
	{
		executor = Executors.newSingleThreadExecutor(new DaemonThreadFactory());
		flippingPanel = new GeFlippingPanel();
		navigationButton = NavigationButton.builder()
			.tooltip("GE Assistant")
			.icon(createSidebarIcon())
			.priority(6)
			.panel(flippingPanel)
			.build();
		clientToolbar.addNavigation(navigationButton);
		overlayManager.add(overlay);
		syncCurrentOffers();
		refreshPricesAsync();
		updateSidebar();
		log.info("GE Assistant started");
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		offers.clear();
		warnings.clear();
		insights.clear();
		setupSidebarInsight = null;
		if (navigationButton != null)
		{
			clientToolbar.removeNavigation(navigationButton);
			navigationButton = null;
		}
		flippingPanel = null;
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
			updateSidebar();
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
			updateSidebar();
			return removed;
		}

		OfferSnapshot previous = offers.put(slot, snapshot);
		evaluateOffer(snapshot);
		if (!snapshot.equals(previous))
		{
			log.info("GE Assistant saw {} slot {} item {} price {} qty {}",
				snapshot.getSide(), snapshot.getSlot(), snapshot.getItemId(), snapshot.getPrice(), snapshot.getTotalQuantity());
		}
		updateSidebar();
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
			updateSidebar();
			return;
		}

		for (OfferSnapshot offer : offers.values())
		{
			evaluateOffer(offer);
		}
		updateSidebar();
	}

	private void evaluateOffer(OfferSnapshot offer)
	{
		if (!config.enableWikiPrices())
		{
			warnings.clear();
			insights.clear();
			updateSidebar();
			return;
		}

		priceCache.get(offer.getItemId())
			.flatMap(price -> insightBuilder.build(
				offer,
				price,
				priceCache.getFiveMinute(offer.getItemId()).orElse(null),
				priceCache.getHourly(offer.getItemId()).orElse(null),
				Math.max(0, config.warningThresholdPercent()),
				Math.max(0, config.taxPercent()),
				Math.max(0, config.taxCapGp()),
				Instant.now()
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
		updateSidebar();
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
			priceCache.getFiveMinute(offer.getItemId()).orElse(null),
			priceCache.getHourly(offer.getItemId()).orElse(null),
			Math.max(0, config.warningThresholdPercent()),
			Math.max(0, config.taxPercent()),
			Math.max(0, config.taxCapGp()),
			Instant.now()
		);
	}

	void updateSidebarSetupInsight(Optional<GeOfferInsight> insight)
	{
		setupSidebarInsight = insight.orElse(null);
		updateSidebar();
	}

	private void updateSidebar()
	{
		GeFlippingPanel panel = flippingPanel;
		if (panel == null)
		{
			return;
		}
		panel.update(GeFlippingSummary.from(getInsights(), Optional.ofNullable(setupSidebarInsight)));
	}

	private static BufferedImage createSidebarIcon()
	{
		BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		try
		{
			graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			graphics.setColor(new Color(28, 55, 68));
			graphics.fillRoundRect(1, 1, 14, 14, 4, 4);
			graphics.setColor(new Color(70, 150, 85));
			graphics.fillRect(4, 10, 8, 2);
			graphics.setColor(new Color(255, 218, 68));
			graphics.fillOval(4, 3, 8, 8);
			graphics.setColor(new Color(20, 18, 14));
			graphics.drawOval(4, 3, 8, 8);
		}
		finally
		{
			graphics.dispose();
		}
		return image;
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
