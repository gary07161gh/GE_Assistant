package com.geassistant;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import net.runelite.api.Client;
import net.runelite.api.widgets.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;

final class GeWidgetSlotLocator
{
	private static final int MAX_GE_SLOTS = 8;
	private static final int GRID_COLUMNS = 4;
	private static final int GRID_ROWS = 2;
	private static final int GRID_LEFT_INSET = 7;
	private static final int GRID_TOP_INSET = 59;
	private static final int GRID_RIGHT_INSET = 7;
	private static final int GRID_BOTTOM_INSET = 9;
	private static final int GRID_COLUMN_GAP = 4;
	private static final int GRID_ROW_GAP = 4;

	@SuppressWarnings("deprecation")
	boolean isOfferOverviewOpen(Client client)
	{
		RootCandidate root = findGrandExchangeRoot(client);
		if (root == null)
		{
			return false;
		}

		return hasWidgetText(root.widget, GeWidgetSlotLocator::isOverviewInstruction)
			|| hasWidgetText(root.widget, GeWidgetSlotLocator::isOverviewTitle);
	}

	@SuppressWarnings("deprecation")
	Optional<Rectangle> findOfferSlotBounds(Client client, int slot)
	{
		if (slot < 0 || slot >= MAX_GE_SLOTS)
		{
			return Optional.empty();
		}

		RootCandidate root = findGrandExchangeRoot(client);
		if (root == null)
		{
			return Optional.empty();
		}

		List<Rectangle> slots = new ArrayList<>();
		Rectangle rootBounds = root.widget.getBounds();
		collectCandidateSlots(root.widget, rootBounds, slots);
		slots.sort(Comparator.comparingInt((Rectangle r) -> r.y).thenComparingInt(r -> r.x));

		if (slots.size() >= MAX_GE_SLOTS)
		{
			return Optional.of(slots.get(slot));
		}

		return syntheticOfferSlotBounds(rootBounds, slot);
	}

	@SuppressWarnings("deprecation")
	Optional<Rectangle> findGrandExchangeBounds(Client client)
	{
		RootCandidate root = findGrandExchangeRoot(client);
		return root == null ? Optional.empty() : Optional.ofNullable(root.widget.getBounds());
	}

	@SuppressWarnings("deprecation")
	String describeGrandExchangeRoot(Client client)
	{
		RootCandidate root = findGrandExchangeRoot(client);
		if (root == null)
		{
			return "GE widget: missing";
		}

		Rectangle bounds = root.widget.getBounds();
		List<Rectangle> slots = new ArrayList<>();
		collectCandidateSlots(root.widget, bounds, slots);
		return "GE widget: " + root.source + " " + format(bounds) + " candidates:" + slots.size()
			+ " overview:" + isOfferOverviewRoot(root.widget);
	}

	@SuppressWarnings("deprecation")
	private RootCandidate findGrandExchangeRoot(Client client)
	{
		RootCandidate widgetInfoRoot = widgetInfoRoot(client, WidgetInfo.GRAND_EXCHANGE_WINDOW_CONTAINER, "window");
		if (widgetInfoRoot != null)
		{
			return widgetInfoRoot;
		}

		widgetInfoRoot = widgetInfoRoot(client, WidgetInfo.GRAND_EXCHANGE_OFFER_CONTAINER, "offer-container");
		if (widgetInfoRoot != null)
		{
			return widgetInfoRoot;
		}

		widgetInfoRoot = widgetInfoRoot(client, WidgetInfo.GRAND_EXCHANGE_OFFER_TEXT, "offer-text");
		if (widgetInfoRoot != null)
		{
			return widgetInfoRoot;
		}

		Widget[] roots = client.getWidgetRoots();
		if (roots == null || InterfaceID.GRAND_EXCHANGE < 0 || InterfaceID.GRAND_EXCHANGE >= roots.length)
		{
			return scanForGrandExchangeTitle(roots);
		}

		Widget root = roots[InterfaceID.GRAND_EXCHANGE];
		if (isUsable(root))
		{
			return new RootCandidate(root, "interface-root");
		}

		return scanForGrandExchangeTitle(roots);
	}

	private RootCandidate widgetInfoRoot(Client client, WidgetInfo widgetInfo, String source)
	{
		Widget widget = client.getWidget(widgetInfo);
		if (!isUsable(widget))
		{
			return null;
		}

		Widget panel = bestPanelAncestor(widget);
		return panel == null ? null : new RootCandidate(panel, source);
	}

	private RootCandidate scanForGrandExchangeTitle(Widget[] roots)
	{
		if (roots == null)
		{
			return null;
		}

		for (Widget root : roots)
		{
			Widget title = findWidgetWithText(root, "Grand Exchange");
			if (title != null)
			{
				Widget panel = bestPanelAncestor(title);
				if (panel != null)
				{
					return new RootCandidate(panel, "title-scan");
				}
			}
		}

		return null;
	}

	private Widget findWidgetWithText(Widget widget, String text)
	{
		if (!isUsable(widget))
		{
			return null;
		}

		if (text.equals(widget.getText()))
		{
			return widget;
		}

		Widget found = findWidgetWithText(widget.getStaticChildren(), text);
		if (found != null)
		{
			return found;
		}
		found = findWidgetWithText(widget.getDynamicChildren(), text);
		if (found != null)
		{
			return found;
		}
		return findWidgetWithText(widget.getNestedChildren(), text);
	}

	private Widget findWidgetWithText(Widget[] children, String text)
	{
		if (children == null)
		{
			return null;
		}

		for (Widget child : children)
		{
			Widget found = findWidgetWithText(child, text);
			if (found != null)
			{
				return found;
			}
		}
		return null;
	}

	private boolean hasWidgetText(Widget widget, Predicate<String> matcher)
	{
		if (!isUsable(widget))
		{
			return false;
		}

		String widgetText = widget.getText();
		if (widgetText != null && matcher.test(widgetText))
		{
			return true;
		}

		return hasWidgetText(widget.getStaticChildren(), matcher)
			|| hasWidgetText(widget.getDynamicChildren(), matcher)
			|| hasWidgetText(widget.getNestedChildren(), matcher);
	}

	private boolean hasWidgetText(Widget[] children, Predicate<String> matcher)
	{
		if (children == null)
		{
			return false;
		}

		for (Widget child : children)
		{
			if (hasWidgetText(child, matcher))
			{
				return true;
			}
		}
		return false;
	}

	private boolean isOfferOverviewRoot(Widget root)
	{
		return hasWidgetText(root, GeWidgetSlotLocator::isOverviewInstruction)
			|| hasWidgetText(root, GeWidgetSlotLocator::isOverviewTitle);
	}

	static boolean isOverviewTitle(String text)
	{
		return "Grand Exchange".equals(text);
	}

	static boolean isOverviewInstruction(String text)
	{
		return text != null && text.contains("Select an offer slot");
	}

	private Widget bestPanelAncestor(Widget widget)
	{
		Widget best = null;
		for (Widget current = widget; current != null; current = current.getParent())
		{
			if (!isUsable(current))
			{
				continue;
			}

			Rectangle bounds = current.getBounds();
			if (isLikelyGePanel(bounds))
			{
				best = current;
			}
		}
		return best;
	}

	private boolean isUsable(Widget widget)
	{
		return widget != null && !widget.isHidden() && widget.getBounds() != null;
	}

	private boolean isLikelyGePanel(Rectangle bounds)
	{
		return bounds != null
			&& bounds.width >= 430
			&& bounds.width <= 560
			&& bounds.height >= 250
			&& bounds.height <= 340;
	}

	private void collectCandidateSlots(Widget widget, Rectangle rootBounds, List<Rectangle> slots)
	{
		if (widget == null || widget.isHidden())
		{
			return;
		}

		Rectangle bounds = widget.getBounds();
		if (isPlausibleOfferSlot(bounds, rootBounds) && slots.stream().noneMatch(bounds::equals))
		{
			slots.add(bounds);
		}

		collectChildren(widget.getStaticChildren(), rootBounds, slots);
		collectChildren(widget.getDynamicChildren(), rootBounds, slots);
		collectChildren(widget.getNestedChildren(), rootBounds, slots);
	}

	private void collectChildren(Widget[] children, Rectangle rootBounds, List<Rectangle> slots)
	{
		if (children == null)
		{
			return;
		}

		for (Widget child : children)
		{
			collectCandidateSlots(child, rootBounds, slots);
		}
	}

	private boolean isPlausibleOfferSlot(Rectangle bounds, Rectangle rootBounds)
	{
		if (bounds == null || rootBounds == null || !rootBounds.contains(bounds))
		{
			return false;
		}

		return bounds.width >= 90
			&& bounds.width <= 260
			&& bounds.height >= 45
			&& bounds.height <= 150
			&& bounds.width * bounds.height < rootBounds.width * rootBounds.height / 3;
	}

	Optional<Rectangle> syntheticOfferSlotBounds(Rectangle rootBounds, int slot)
	{
		if (rootBounds == null || slot < 0 || slot >= MAX_GE_SLOTS)
		{
			return Optional.empty();
		}

		int usableWidth = rootBounds.width - GRID_LEFT_INSET - GRID_RIGHT_INSET - ((GRID_COLUMNS - 1) * GRID_COLUMN_GAP);
		int usableHeight = rootBounds.height - GRID_TOP_INSET - GRID_BOTTOM_INSET - ((GRID_ROWS - 1) * GRID_ROW_GAP);
		if (usableWidth <= 0 || usableHeight <= 0)
		{
			return Optional.empty();
		}

		int slotWidth = usableWidth / GRID_COLUMNS;
		int slotHeight = usableHeight / GRID_ROWS;
		int column = slot % GRID_COLUMNS;
		int row = slot / GRID_COLUMNS;
		return Optional.of(new Rectangle(
			rootBounds.x + GRID_LEFT_INSET + (column * (slotWidth + GRID_COLUMN_GAP)),
			rootBounds.y + GRID_TOP_INSET + (row * (slotHeight + GRID_ROW_GAP)),
			slotWidth,
			slotHeight
		));
	}

	private String format(Rectangle bounds)
	{
		return bounds == null
			? "no-bounds"
			: bounds.x + "," + bounds.y + " " + bounds.width + "x" + bounds.height;
	}

	private static final class RootCandidate
	{
		private final Widget widget;
		private final String source;

		private RootCandidate(Widget widget, String source)
		{
			this.widget = widget;
			this.source = source;
		}
	}
}
