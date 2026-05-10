package com.geassistant;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

final class GeAssistantOverlay extends Overlay
{
	private static final Color WARNING = new Color(205, 54, 45);
	private static final Color WARNING_FILL = new Color(205, 54, 45, 190);
	private static final Color INFO = new Color(64, 140, 190);
	private static final Color INFO_FILL = new Color(28, 55, 68, 205);
	private static final Color FAVORABLE = new Color(70, 150, 85);
	private static final Color FAVORABLE_FILL = new Color(31, 75, 45, 205);
	private static final Color PANEL = new Color(20, 18, 14, 230);
	private static final Color TEXT = Color.WHITE;

	private final Client client;
	private final GeAssistantPlugin plugin;
	private final GeAssistantConfig config;
	private final GeWidgetSlotLocator slotLocator;

	@Inject
	GeAssistantOverlay(Client client, GeAssistantPlugin plugin, GeAssistantConfig config)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		this.slotLocator = new GeWidgetSlotLocator();
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		List<GeOfferInsight> insights = new ArrayList<>(plugin.getInsights());
		if (config.showDebugStatus())
		{
			renderDebugStatus(graphics);
		}

		if (insights.isEmpty())
		{
			return null;
		}

		if (!config.showSlotBadges())
		{
			renderFallbackSummary(graphics, insights);
			return null;
		}

		insights.sort(Comparator.comparingInt(w -> w.getOffer().getSlot()));
		boolean renderedSlot = false;
		for (GeOfferInsight insight : insights)
		{
			Optional<Rectangle> bounds = slotLocator.findOfferSlotBounds(client, insight.getOffer().getSlot());
			if (bounds.isPresent())
			{
				renderSlotInsight(graphics, bounds.get(), insight);
				renderedSlot = true;
			}
		}

		if (!renderedSlot)
		{
			renderFallbackSummary(graphics, insights);
		}

		return null;
	}

	private void renderSlotInsight(Graphics2D graphics, Rectangle bounds, GeOfferInsight insight)
	{
		boolean warning = insight.hasWarning();
		Color accent = warning ? WARNING : insight.getSignedPercent() <= 0 && insight.getOffer().getSide() == OfferSide.BUY
			|| insight.getSignedPercent() >= 0 && insight.getOffer().getSide() == OfferSide.SELL ? FAVORABLE : INFO;
		Color fill = warning ? WARNING_FILL : accent == FAVORABLE ? FAVORABLE_FILL : INFO_FILL;

		if (warning)
		{
			graphics.setStroke(new BasicStroke(2f));
			graphics.setColor(WARNING);
			graphics.drawRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 8, 8);
		}

		String text = insight.getBadgeText();
		FontMetrics metrics = graphics.getFontMetrics();
		int width = metrics.stringWidth(text) + 10;
		int height = metrics.getHeight() + 2;
		int x = bounds.x + bounds.width - width - 4;
		int y = bounds.y + 4;

		graphics.setColor(fill);
		graphics.fillRoundRect(x, y, width, height, 6, 6);
		graphics.setColor(TEXT);
		graphics.drawString(text, x + 5, y + metrics.getAscent() + 1);

		if (config.showMarginTooltip() && mouseInside(bounds))
		{
			renderDetails(graphics, bounds, insight, accent);
		}
	}

	private void renderDetails(Graphics2D graphics, Rectangle anchor, GeOfferInsight insight, Color accent)
	{
		List<String> lines = new ArrayList<>();
		lines.add(insight.getOffer().getSide() + " offer: " + formatGp(insight.getOffer().getPrice()));
		lines.add("Wiki high: " + formatNullableGp(insight.getPrice().getHigh()));
		lines.add("Wiki low: " + formatNullableGp(insight.getPrice().getLow()));
		lines.add("Spread: " + formatGp(insight.getRawSpread()));
		lines.add("Tax margin: " + formatGp(insight.getTaxAdjustedMargin()));

		FontMetrics metrics = graphics.getFontMetrics();
		int width = lines.stream().mapToInt(metrics::stringWidth).max().orElse(120) + 14;
		int height = lines.size() * metrics.getHeight() + 12;
		int x = anchor.x + anchor.width + 8;
		int y = Math.max(0, anchor.y);

		graphics.setColor(PANEL);
		graphics.fillRoundRect(x, y, width, height, 8, 8);
		graphics.setColor(accent);
		graphics.drawRoundRect(x, y, width, height, 8, 8);
		graphics.setColor(TEXT);
		for (int i = 0; i < lines.size(); i++)
		{
			graphics.drawString(lines.get(i), x + 7, y + 7 + metrics.getAscent() + (i * metrics.getHeight()));
		}
	}

	private void renderFallbackSummary(Graphics2D graphics, List<GeOfferInsight> insights)
	{
		Optional<Rectangle> geBounds = slotLocator.findGrandExchangeBounds(client);
		Rectangle bounds = geBounds.orElse(new Rectangle(8, 8, 500, 350));
		long warningCount = insights.stream().filter(GeOfferInsight::hasWarning).count();
		String text = warningCount == 0
			? "GE Assistant: " + insights.size() + " offers"
			: "GE Assistant: " + warningCount + " suspicious";
		FontMetrics metrics = graphics.getFontMetrics();
		int width = metrics.stringWidth(text) + 14;
		int height = metrics.getHeight() + 8;
		int x = bounds.x + bounds.width - width - 8;
		int y = bounds.y + 8;

		graphics.setColor(WARNING_FILL);
		graphics.fillRoundRect(x, y, width, height, 8, 8);
		graphics.setColor(TEXT);
		graphics.drawString(text, x + 7, y + 5 + metrics.getAscent());
	}

	private void renderDebugStatus(Graphics2D graphics)
	{
		Optional<Rectangle> geBounds = slotLocator.findGrandExchangeBounds(client);
		Rectangle bounds = geBounds.orElse(new Rectangle(8, 8, 500, 350));
		List<String> lines = new ArrayList<>();
		lines.add("GE Assistant debug");
		lines.add("offers: " + plugin.getOfferCount() + " warnings: " + plugin.getWarnings().size());
		lines.add("prices: " + plugin.getPriceCount() + " refresh: " + (plugin.isRefreshInFlight() ? "running" : "idle"));
		if (plugin.getPriceError() != null)
		{
			lines.add("price error: " + plugin.getPriceError());
		}

		FontMetrics metrics = graphics.getFontMetrics();
		int width = lines.stream().mapToInt(metrics::stringWidth).max().orElse(120) + 14;
		int height = lines.size() * metrics.getHeight() + 12;
		int x = bounds.x + 8;
		int y = bounds.y + 8;

		graphics.setColor(PANEL);
		graphics.fillRoundRect(x, y, width, height, 8, 8);
		graphics.setColor(WARNING);
		graphics.drawRoundRect(x, y, width, height, 8, 8);
		graphics.setColor(TEXT);
		for (int i = 0; i < lines.size(); i++)
		{
			graphics.drawString(lines.get(i), x + 7, y + 7 + metrics.getAscent() + (i * metrics.getHeight()));
		}
	}

	private boolean mouseInside(Rectangle bounds)
	{
		Point mouse = client.getMouseCanvasPosition();
		return mouse != null && bounds.contains(mouse.getX(), mouse.getY());
	}

	private String formatGp(int value)
	{
		return String.format("%,d gp", value);
	}

	private String formatNullableGp(Integer value)
	{
		return value == null ? "n/a" : formatGp(value);
	}
}
