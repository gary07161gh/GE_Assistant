package com.geassistant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.Map;
import net.runelite.api.Client;
import org.junit.Test;

public class GeAssistantPluginStateTest
{
	@Test
	public void clearsInsightsWhenWikiPricesAreDisabled() throws Exception
	{
		GeAssistantPlugin plugin = new GeAssistantPlugin();
		setField(plugin, "config", disabledWikiConfig());
		addInsight(plugin);

		invoke(plugin, "recomputeWarnings");

		assertTrue(plugin.getWarnings().isEmpty());
		assertTrue(plugin.getInsights().isEmpty());
	}

	@Test
	public void clearsInsightsWhenClientHasNoGrandExchangeOffers() throws Exception
	{
		GeAssistantPlugin plugin = new GeAssistantPlugin();
		setField(plugin, "client", clientWithNoGrandExchangeOffers());
		addOffer(plugin);
		addInsight(plugin);

		boolean changed = (boolean) invoke(plugin, "syncCurrentOffers");

		assertTrue(changed);
		assertEquals(0, plugin.getOfferCount());
		assertTrue(plugin.getWarnings().isEmpty());
		assertTrue(plugin.getInsights().isEmpty());
	}

	@SuppressWarnings("unchecked")
	private void addOffer(GeAssistantPlugin plugin) throws Exception
	{
		Map<Integer, OfferSnapshot> offers = (Map<Integer, OfferSnapshot>) getField(plugin, "offers");
		offers.put(0, offer());
	}

	@SuppressWarnings("unchecked")
	private void addInsight(GeAssistantPlugin plugin) throws Exception
	{
		Map<Integer, GeOfferInsight> insights = (Map<Integer, GeOfferInsight>) getField(plugin, "insights");
		PriceSnapshot price = price();
		GeWarning warning = new GeWarning(offer(), price, 100, 3.0, 10, 8);
		insights.put(0, new GeOfferInsight(offer(), price, warning, 3.0, 10, 8));
	}

	private OfferSnapshot offer()
	{
		return new OfferSnapshot(0, 536, OfferSide.BUY, 103, 1, 0);
	}

	private PriceSnapshot price()
	{
		Instant now = Instant.parse("2026-05-10T20:00:00Z");
		return new PriceSnapshot(536, 110, now, 100, now);
	}

	private GeAssistantConfig disabledWikiConfig()
	{
		return (GeAssistantConfig) Proxy.newProxyInstance(
			GeAssistantConfig.class.getClassLoader(),
			new Class<?>[] { GeAssistantConfig.class },
			(proxy, method, args) -> "enableWikiPrices".equals(method.getName()) ? false : method.getDefaultValue()
		);
	}

	private Client clientWithNoGrandExchangeOffers()
	{
		return (Client) Proxy.newProxyInstance(
			Client.class.getClassLoader(),
			new Class<?>[] { Client.class },
			(proxy, method, args) -> defaultValue(method.getReturnType())
		);
	}

	private Object defaultValue(Class<?> type)
	{
		if (!type.isPrimitive())
		{
			return null;
		}
		if (type == boolean.class)
		{
			return false;
		}
		if (type == void.class)
		{
			return null;
		}
		return 0;
	}

	private Object getField(GeAssistantPlugin plugin, String name) throws Exception
	{
		Field field = GeAssistantPlugin.class.getDeclaredField(name);
		field.setAccessible(true);
		return field.get(plugin);
	}

	private void setField(GeAssistantPlugin plugin, String name, Object value) throws Exception
	{
		Field field = GeAssistantPlugin.class.getDeclaredField(name);
		field.setAccessible(true);
		field.set(plugin, value);
	}

	private Object invoke(GeAssistantPlugin plugin, String name) throws Exception
	{
		Method method = GeAssistantPlugin.class.getDeclaredMethod(name);
		method.setAccessible(true);
		return method.invoke(plugin);
	}
}
