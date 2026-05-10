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
		List<GeWarning> warnings = new ArrayList<>(plugin.getWarnings());
		if (warnings.isEmpty())
		{
			return null;
		}

		if (!config.showSlotBadges())
		{
			renderFallbackSummary(graphics, warnings);
			return null;
		}

		warnings.sort(Comparator.comparingInt(w -> w.getOffer().getSlot()));
		boolean renderedSlot = false;
		for (GeWarning warning : warnings)
		{
			Optional<Rectangle> bounds = slotLocator.findOfferSlotBounds(client, warning.getOffer().getSlot());
			if (bounds.isPresent())
			{
				renderSlotWarning(graphics, bounds.get(), warning);
				renderedSlot = true;
			}
		}

		if (!renderedSlot)
		{
			renderFallbackSummary(graphics, warnings);
		}

		return null;
	}

	private void renderSlotWarning(Graphics2D graphics, Rectangle bounds, GeWarning warning)
	{
		graphics.setStroke(new BasicStroke(2f));
		graphics.setColor(WARNING);
		graphics.drawRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 8, 8);

		String text = warning.getBadgeText();
		FontMetrics metrics = graphics.getFontMetrics();
		int width = metrics.stringWidth(text) + 10;
		int height = metrics.getHeight() + 2;
		int x = bounds.x + bounds.width - width - 4;
		int y = bounds.y + 4;

		graphics.setColor(WARNING_FILL);
		graphics.fillRoundRect(x, y, width, height, 6, 6);
		graphics.setColor(TEXT);
		graphics.drawString(text, x + 5, y + metrics.getAscent() + 1);

		if (config.showMarginTooltip() && mouseInside(bounds))
		{
			renderDetails(graphics, bounds, warning);
		}
	}

	private void renderDetails(Graphics2D graphics, Rectangle anchor, GeWarning warning)
	{
		List<String> lines = new ArrayList<>();
		lines.add(warning.getOffer().getSide() + " offer: " + formatGp(warning.getOffer().getPrice()));
		lines.add("Wiki high: " + formatNullableGp(warning.getPrice().getHigh()));
		lines.add("Wiki low: " + formatNullableGp(warning.getPrice().getLow()));
		lines.add("Spread: " + formatGp(warning.getRawSpread()));
		lines.add("Tax margin: " + formatGp(warning.getTaxAdjustedMargin()));

		FontMetrics metrics = graphics.getFontMetrics();
		int width = lines.stream().mapToInt(metrics::stringWidth).max().orElse(120) + 14;
		int height = lines.size() * metrics.getHeight() + 12;
		int x = anchor.x + anchor.width + 8;
		int y = Math.max(0, anchor.y);

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

	private void renderFallbackSummary(Graphics2D graphics, List<GeWarning> warnings)
	{
		Optional<Rectangle> geBounds = slotLocator.findGrandExchangeBounds(client);
		if (!geBounds.isPresent())
		{
			return;
		}

		Rectangle bounds = geBounds.get();
		String text = warnings.size() == 1
			? "GE Assistant: 1 suspicious offer"
			: "GE Assistant: " + warnings.size() + " suspicious offers";
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

