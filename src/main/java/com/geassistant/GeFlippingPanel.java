package com.geassistant;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Locale;
import java.util.function.IntFunction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.PluginPanel;

final class GeFlippingPanel extends PluginPanel
{
	private static final Color BACKGROUND = new Color(35, 35, 35);
	private static final Color CARD = new Color(45, 45, 45);
	private static final Color TEXT = new Color(235, 235, 235);
	private static final Color MUTED = new Color(170, 170, 170);
	private static final Color WARNING = new Color(205, 54, 45);
	private static final Color FAVORABLE = new Color(70, 150, 85);

	private final JPanel content = new JPanel();
	private final IntFunction<String> itemNameResolver;
	private volatile String lastRenderKey = "";

	GeFlippingPanel()
	{
		this(itemId -> "Item " + itemId);
	}

	GeFlippingPanel(IntFunction<String> itemNameResolver)
	{
		setLayout(new BorderLayout());
		setBackground(BACKGROUND);
		this.itemNameResolver = itemNameResolver;
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		content.setBackground(BACKGROUND);
		content.setBorder(new EmptyBorder(10, 10, 10, 10));
		add(content, BorderLayout.NORTH);
		update(GeFlippingSummary.from(null, java.util.Optional.empty()));
	}

	void update(GeFlippingSummary summary)
	{
		String renderKey = renderKey(summary);
		if (renderKey.equals(lastRenderKey))
		{
			return;
		}
		lastRenderKey = renderKey;

		SwingUtilities.invokeLater(() -> {
			content.removeAll();
			content.add(title("GE Assistant"));
			content.add(Box.createRigidArea(new Dimension(0, 8)));
			content.add(setupCard(summary));
			content.add(Box.createRigidArea(new Dimension(0, 8)));
			content.add(activeCard(summary));
			content.add(Box.createRigidArea(new Dimension(0, 8)));
			content.add(bestCard(summary));
			content.revalidate();
			content.repaint();
		});
	}

	private Component title(String text)
	{
		JLabel label = new JLabel(text);
		label.setForeground(TEXT);
		label.setFont(label.getFont().deriveFont(Font.BOLD, 18f));
		label.setAlignmentX(Component.LEFT_ALIGNMENT);
		return label;
	}

	private JPanel setupCard(GeFlippingSummary summary)
	{
		JPanel panel = card("Setup Flip");
		if (!summary.getSetupInsight().isPresent())
		{
			addRow(panel, "Current", "No setup offer");
			return panel;
		}

		GeOfferInsight insight = summary.getSetupInsight().get();
		addRow(panel, "Item", itemLabel(insight));
		addRow(panel, "Action", setupAction(insight));
		addRow(panel, "Buy", formatGp(insight.getSuggestedBuyPrice()));
		addRow(panel, "Sell", formatGp(insight.getSuggestedSellPrice()));
		addRow(panel, "Profit", formatGp(insight.getSuggestedProfit()) + " after " + formatGp(insight.getSuggestedTax()) + " tax");
		addRow(panel, "ROI", formatPercent(insight.getSuggestedRoiPercent()));
		addRow(panel, "Score", insight.getOpportunityLabel() + " " + insight.getOpportunityScore());
		addRow(panel, "Volume", "5m " + formatNumber(insight.getFiveMinuteVolume()) + " / 1h " + formatNumber(insight.getHourlyVolume()));
		addRow(panel, "Trend", insight.getOpportunityTrendText());
		return panel;
	}

	private JPanel activeCard(GeFlippingSummary summary)
	{
		JPanel panel = card("Active Offers");
		addRow(panel, "Offers", Integer.toString(summary.getOfferCount()));
		addRow(panel, "Risky", Integer.toString(summary.getRiskyOfferCount()));
		addRow(panel, "Profit", formatGp(summary.getProjectedProfit()));
		addRow(panel, "Risk", formatGp(summary.getProjectedLoss()));
		addRow(panel, "Net", formatGp(summary.getNetProjectedProfit()));
		return panel;
	}

	private JPanel bestCard(GeFlippingSummary summary)
	{
		JPanel panel = card("Best Active");
		if (!summary.getBestOpportunity().isPresent())
		{
			addRow(panel, "Current", summary.getOfferCount() == 0 ? "No active offers" : "No profitable offers");
			return panel;
		}

		GeOfferInsight insight = summary.getBestOpportunity().get();
		addRow(panel, "Item", itemLabel(insight));
		addRow(panel, "Side", insight.getOffer().getSide().toString());
		addRow(panel, "Price", formatGp(insight.getOffer().getPrice()));
		addRow(panel, "Profit", formatGp(insight.getOpportunityNetMargin()) + " each");
		addRow(panel, "ROI", formatPercent(insight.getOpportunityRoiPercent()));
		addRow(panel, "Score", insight.getOpportunityLabel() + " " + insight.getOpportunityScore());
		return panel;
	}

	private JPanel card(String heading)
	{
		JPanel panel = new JPanel(new GridBagLayout());
		panel.setBackground(CARD);
		panel.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(new Color(60, 60, 60)),
			new EmptyBorder(8, 8, 8, 8)
		));
		panel.setAlignmentX(Component.LEFT_ALIGNMENT);
		panel.setMaximumSize(new Dimension(PluginPanel.PANEL_WIDTH - 20, Integer.MAX_VALUE));

		JLabel title = new JLabel(heading);
		title.setForeground(TEXT);
		title.setFont(title.getFont().deriveFont(Font.BOLD));
		GridBagConstraints constraints = constraints(0, 0);
		constraints.gridwidth = 2;
		constraints.insets = new Insets(0, 0, 6, 0);
		panel.add(title, constraints);
		return panel;
	}

	private void addRow(JPanel panel, String label, String value)
	{
		int row = panel.getComponentCount();
		JLabel labelComponent = new JLabel(label);
		labelComponent.setForeground(MUTED);
		JLabel valueComponent = new JLabel(value);
		valueComponent.setForeground(value.startsWith("-") ? WARNING : TEXT);
		if ("Profit".equals(label) || "Net".equals(label))
		{
			valueComponent.setForeground(value.startsWith("-") ? WARNING : FAVORABLE);
		}
		if ("Risk".equals(label) && !"0 gp".equals(value))
		{
			valueComponent.setForeground(WARNING);
		}

		panel.add(labelComponent, constraints(0, row));
		panel.add(valueComponent, constraints(1, row));
	}

	private GridBagConstraints constraints(int x, int y)
	{
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = x;
		constraints.gridy = y;
		constraints.anchor = GridBagConstraints.WEST;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.weightx = x == 0 ? 0.0 : 1.0;
		constraints.insets = new Insets(2, 0, 2, x == 0 ? 8 : 0);
		return constraints;
	}

	private String formatGp(int value)
	{
		return String.format(Locale.US, "%,d gp", value);
	}

	private String formatNumber(int value)
	{
		return String.format(Locale.US, "%,d", value);
	}

	private String formatPercent(double value)
	{
		return String.format(Locale.US, "%.1f%%", value);
	}

	private String renderKey(GeFlippingSummary summary)
	{
		return summary.getOfferCount()
			+ "|" + summary.getRiskyOfferCount()
			+ "|" + summary.getProjectedProfit()
			+ "|" + summary.getProjectedLoss()
			+ "|" + insightKey(summary.getSetupInsight().orElse(null))
			+ "|" + insightKey(summary.getBestOpportunity().orElse(null));
	}

	private String insightKey(GeOfferInsight insight)
	{
		if (insight == null)
		{
			return "none";
		}
		return insight.getOffer().getItemId()
			+ ":" + insight.getOffer().getSide()
			+ ":" + insight.getOffer().getPrice()
			+ ":" + insight.getOffer().getTotalQuantity()
			+ ":" + insight.getOpportunityScore()
			+ ":" + insight.getOpportunityNetMargin()
			+ ":" + insight.getSuggestedBuyPrice()
			+ ":" + insight.getSuggestedSellPrice()
			+ ":" + insight.getSuggestedProfit();
	}

	String itemLabel(GeOfferInsight insight)
	{
		if (insight == null)
		{
			return "Unknown item";
		}
		String name = itemNameResolver.apply(insight.getOffer().getItemId());
		return name == null || name.trim().isEmpty() ? "Item " + insight.getOffer().getItemId() : name;
	}

	String setupAction(GeOfferInsight insight)
	{
		if (insight == null || insight.getSuggestedBuyPrice() <= 0 || insight.getSuggestedSellPrice() <= 0)
		{
			return "Waiting for prices";
		}
		return "Buy " + formatGp(insight.getSuggestedBuyPrice()) + " / sell " + formatGp(insight.getSuggestedSellPrice());
	}
}
