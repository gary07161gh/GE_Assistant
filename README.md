# GE Assistant

GE Assistant is a [RuneLite](https://runelite.net/) Plugin Hub plugin for active Grand Exchange flipping. It helps you make informed margin decisions by drawing warning badges over suspicious submitted Grand Exchange offer slots and showing cached OSRS Wiki high/low price context.

## Features

### Offer Warnings
- **Suspicious buy offers**: Warns when a buy offer is priced significantly above the latest Wiki low price (default >2% above low)
- **Suspicious sell offers**: Warns when a sell offer is priced significantly below the latest Wiki high price (default >2% below high)
- Visual badge overlays on GE offer slots with the percentage deviation from reference price
- Red border highlight around warned offer slots

### Margin Insights
- Hover over any warned offer slot to see a tooltip with:
  - Current high and low Wiki prices with age indicators
  - Raw spread (high - low)
  - Tax-adjusted margin (high - tax - low)
- Color-coded status indicators:
  - 🔴 **Risky** (red): Offer is significantly worse than reference
  - 🟢 **Favorable** (green): Buy offer at/below low or sell offer at/above high
  - 🔵 **Neutral** (blue): Offer near reference price

### Setup Panel
While setting up a new offer in the Grand Exchange interface, GE Assistant shows a panel with:
- Current high/low Wiki prices and their ages
- Tax-adjusted margin for the item
- Status indicator for the prospective offer
- Balanced opportunity score using margin, ROI, volume, price freshness, and short-term trend
- Suggested flip buy price, sell price, tax, expected profit, and ROI

### GE Tax Support
- Configurable tax percentage (default 2% to match OSRS mechanics)
- Configurable tax cap (default 5,000,000 gp)
- Tax is applied to sell-side margin calculations

### Opportunity Scoring
- Fetches OSRS Wiki 5-minute and 1-hour market data alongside latest prices
- Scores opportunities from 0-100 with Strong, Fair, Weak, and Avoid labels
- Shows score, ROI, net margin, volume, and trend context in the setup panel and hover tooltip

### Sidebar Flipping Info
- Adds a GE Assistant sidebar panel to RuneLite's toolbar
- Shows setup-offer flipping context while entering an offer
- Resolves item names for active and setup offers
- Summarizes active GE offers, risky offer count, projected profit, projected loss, net projection, and best profitable opportunity

## Configuration

Open RuneLite's configuration panel and navigate to **GE Assistant** to adjust:

| Setting | Default | Description |
|---------|---------|-------------|
| Warning threshold percent | 2% | Warn when a submitted offer is this far worse than the latest high/low price |
| Price refresh seconds | 60 | Minimum seconds between OSRS Wiki latest-price refreshes |
| Show slot badges | Enabled | Draw warning badges and borders over suspicious GE offer slots |
| Show margin tooltip | Enabled | Show high, low, spread, and tax-adjusted margin when hovering a warned offer |
| GE tax percent | 2% | Sell-side Grand Exchange tax percent used in margin calculations |
| GE tax cap | 5,000,000 gp | Maximum GP tax per sold item |
| Enable Wiki prices | Enabled | Fetch cached latest high/low prices from the OSRS Wiki real-time prices API |
| Show debug status | Disabled | Show offer, warning, and price-cache counts on the GE screen for troubleshooting |
| Show setup panel | Enabled | Show safe-price context while setting up a Grand Exchange offer |
| Show opportunity score | Enabled | Show balanced opportunity score, ROI, volume, and trend context |

## Installation

This plugin is available on the [RuneLite Plugin Hub](https://runelite.net/plugin-hub/):

1. Open RuneLite
2. Click the wrench icon to open the Configuration panel
3. Select **Plugin Hub** from the left sidebar
4. Search for "GE Assistant"
5. Click **Install**

### Manual Installation (Development)

```bash
git clone https://github.com/gary07161gh/GE_Assistant.git
cd GE_Assistant
./gradlew build
```

The built JAR will be in `build/libs/`. Add it to RuneLite via the "Open RuneLite dev tools" → "External plugins" panel.

## How It Works

1. **Offer Detection**: On each game tick, the plugin reads all 8 GE offer slots from the client state
2. **Price Fetching**: Latest prices and 5-minute/1-hour market data are fetched from the [OSRS Wiki Real-time Prices API](https://prices.runescape.wiki/api/v1/osrs) with configurable refresh intervals
3. **Warning Evaluation**: Each offer is compared against the latest Wiki reference price:
   - **Buy offers** compared against the Wiki **low** price (above threshold = risky)
   - **Sell offers** compared against the Wiki **high** price (below threshold = risky)
4. **Overlay Rendering**: Warning badges, borders, and tooltips are drawn on the GE interface

## API Credits

This plugin uses the [OSRS Wiki Real-time Prices API](https://oldschool.runescape.wiki/w/RuneScape:Real-time_Prices) to provide high/low price data.

## License

This project is licensed under the BSD 2-Clause License. See [LICENSE](LICENSE) for details.

## Author

**Gary0** - [GitHub](https://github.com/gary07161gh/GE_Assistant)
