package com.geassistant;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.runelite.api.Client;
import net.runelite.api.widgets.InterfaceID;
import net.runelite.api.widgets.Widget;

final class GeWidgetSlotLocator
{
	private static final int MAX_GE_SLOTS = 8;

	@SuppressWarnings("deprecation")
	Optional<Rectangle> findOfferSlotBounds(Client client, int slot)
	{
		if (slot < 0 || slot >= MAX_GE_SLOTS)
		{
			return Optional.empty();
		}

		Widget root = findGrandExchangeRoot(client);
		if (root == null)
		{
			return Optional.empty();
		}

		List<Rectangle> slots = new ArrayList<>();
		collectCandidateSlots(root, root.getBounds(), slots);
		slots.sort(Comparator.comparingInt((Rectangle r) -> r.y).thenComparingInt(r -> r.x));

		return slot < slots.size() ? Optional.of(slots.get(slot)) : Optional.empty();
	}

	@SuppressWarnings("deprecation")
	Optional<Rectangle> findGrandExchangeBounds(Client client)
	{
		Widget root = findGrandExchangeRoot(client);
		return root == null ? Optional.empty() : Optional.ofNullable(root.getBounds());
	}

	@SuppressWarnings("deprecation")
	private Widget findGrandExchangeRoot(Client client)
	{
		Widget[] roots = client.getWidgetRoots();
		if (roots == null || InterfaceID.GRAND_EXCHANGE < 0 || InterfaceID.GRAND_EXCHANGE >= roots.length)
		{
			return null;
		}

		Widget root = roots[InterfaceID.GRAND_EXCHANGE];
		if (root == null || root.isHidden())
		{
			return null;
		}
		return root;
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
}

