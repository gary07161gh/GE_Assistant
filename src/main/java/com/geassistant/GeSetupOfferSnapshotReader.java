package com.geassistant;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.runelite.api.Client;
import net.runelite.api.widgets.Widget;

final class GeSetupOfferSnapshotReader
{
	private static final Pattern ITEM_ID = Pattern.compile("(?i)item\\s*id\\s*:\\s*(\\d+)");
	private static final Pattern PRICE = Pattern.compile("(?i)price[^:]*:\\s*([\\d,]+)");
	private static final Pattern QUANTITY = Pattern.compile("(?i)quantity\\s*:\\s*([\\d,]+)");

	Optional<SetupOfferSnapshot> fromClient(Client client)
	{
		if (client == null)
		{
			return Optional.empty();
		}

		List<String> texts = new ArrayList<>();
		Widget[] roots = client.getWidgetRoots();
		if (roots != null)
		{
			for (Widget root : roots)
			{
				collectTexts(root, texts);
			}
		}
		return fromWidgetTexts(texts);
	}

	Optional<SetupOfferSnapshot> fromWidgetTexts(Collection<String> texts)
	{
		if (texts == null || texts.isEmpty())
		{
			return Optional.empty();
		}

		OfferSide side = null;
		Integer itemId = null;
		int price = 0;
		int quantity = 0;

		for (String text : texts)
		{
			if (text == null)
			{
				continue;
			}

			String normalized = stripTags(text).trim();
			String lower = normalized.toLowerCase(Locale.ROOT);
			if (lower.contains("buy offer"))
			{
				side = OfferSide.BUY;
			}
			else if (lower.contains("sell offer"))
			{
				side = OfferSide.SELL;
			}

			itemId = firstPresent(itemId, matchNumber(ITEM_ID, normalized));
			Integer parsedPrice = matchNumber(PRICE, normalized);
			if (parsedPrice != null)
			{
				price = parsedPrice;
			}
			Integer parsedQuantity = matchNumber(QUANTITY, normalized);
			if (parsedQuantity != null)
			{
				quantity = parsedQuantity;
			}
		}

		if (side == null || itemId == null || itemId <= 0)
		{
			return Optional.empty();
		}
		return Optional.of(new SetupOfferSnapshot(itemId, side, Math.max(0, price), Math.max(0, quantity)));
	}

	private void collectTexts(Widget widget, List<String> texts)
	{
		if (widget == null || widget.isHidden())
		{
			return;
		}

		if (widget.getText() != null && !widget.getText().trim().isEmpty())
		{
			texts.add(widget.getText());
		}
		String name = readString(widget, "getName");
		if (name != null && !name.trim().isEmpty())
		{
			texts.add("Item: " + name);
		}
		Integer itemId = readInt(widget, "getItemId");
		if (itemId != null && itemId > 0)
		{
			texts.add("Item id: " + itemId);
		}

		collectTexts(widget.getStaticChildren(), texts);
		collectTexts(widget.getDynamicChildren(), texts);
		collectTexts(widget.getNestedChildren(), texts);
	}

	private void collectTexts(Widget[] widgets, List<String> texts)
	{
		if (widgets == null)
		{
			return;
		}
		for (Widget widget : widgets)
		{
			collectTexts(widget, texts);
		}
	}

	private Integer firstPresent(Integer current, Integer candidate)
	{
		return current != null ? current : candidate;
	}

	private Integer matchNumber(Pattern pattern, String text)
	{
		Matcher matcher = pattern.matcher(text);
		if (!matcher.find())
		{
			return null;
		}
		return Integer.parseInt(matcher.group(1).replace(",", ""));
	}

	private String stripTags(String text)
	{
		return text.replaceAll("<[^>]+>", "");
	}

	private String readString(Widget widget, String methodName)
	{
		try
		{
			Object value = widget.getClass().getMethod(methodName).invoke(widget);
			return value instanceof String ? (String) value : null;
		}
		catch (ReflectiveOperationException ex)
		{
			return null;
		}
	}

	private Integer readInt(Widget widget, String methodName)
	{
		try
		{
			Object value = widget.getClass().getMethod(methodName).invoke(widget);
			return value instanceof Number ? ((Number) value).intValue() : null;
		}
		catch (ReflectiveOperationException ex)
		{
			return null;
		}
	}
}
