# AstraDonutShop (GGDonutShop)

Modern donut-style shop plugin for **Paper 1.21.11** with dynamic market prices, a configurable
sell GUI, hex color support and full source code.

> The compiled, fixed plugin is **`GGDonutShop.jar`** in this repository.
> Drop it into `plugins/` (together with **Vault**) and restart.

## What was fixed / added in `1.21.11-26.3`

- **Sell Worth lore duplication fixed.** The `Sell Worth: $ X` lore line is tracked with a
  persistent data marker. It is never added twice, old duplicate lines are automatically
  cleaned up, and it updates itself when market prices change.
- **Toggleable lore.** New `sell-worth-lore` section in `config.yml`:
  - `enabled: true/false` — when set to `false` (or `/shop reload` toggles it) the lore line
    is **removed from all items** again; when re-enabled it is added back.
  - `line:` — fully configurable lore text (placeholders: `{price}`, `{total}`, `{amount}`, `{material}`).
- **Price fluctuation switch.** `price-fluctuation.enabled: true/false` in `config.yml` turns the
  market price randomizer on or off. The task is now also correctly (re)scheduled on `/shop reload`.
- **Configurable fluctuation announcement.** The market update broadcast is a configurable,
  hex-capable message list: `price-fluctuation.announce.messages`. It can be silenced with
  `price-fluctuation.announce.enabled: false`.
- **Configurable sell GUI.** New `sell-gui` section: menu `title`, `size`, `sell-slot` and the
  sell button (`material`, `name`, `lore`) are all editable.
- **Commands:** `/sell`, `/sellg` and `/sellgui` all open the sell GUI (aliases declared in
  `plugin.yml`).
- **Hex code support** everywhere configurable text is used: `&#RRGGBB`, `&x&R&R&G&G&B&B` and
  classic `&`-codes.
- **Bogus hard dependency removed.** `plugin.yml` previously hard-depended on `GGPlugins`
  (never actually used by the code), which prevented the plugin from enabling on servers
  without it. It is now a soft dependency.

All changes are fully backward compatible: existing `config.yml` files that still have the old
`randomize` / `price-randomizer.*` keys keep working — those values are read once and migrated
into the new sections automatically.

## Configuration (`config.yml`)

```yaml
price-fluctuation:
  enabled: true            # false = prices stay at the shop.yml base prices
  interval-minutes: 15
  min-multiplier: 0.70
  max-multiplier: 1.40
  announce:
    enabled: true
    messages:
      - "&6&l========================================"
      - "&e 📊 &lMARKET UPDATE: &aShop Prices Have Fluctuated!"
      - "&7Market economy rates have shifted. Check &b/shop&7!"
      - "&6&l========================================"

sell-worth-lore:
  enabled: true            # false = strip the lore from all items
  line: "&fSell Worth: &a$ {price}"

sell-gui:
  title: "&8Sell Chest"
  size: 54                 # multiple of 9 (9-54)
  sell-slot: 53
  sell-button:
    material: BARRIER
    name: "&c&lClick to Sell"
    lore:
      - "&7Click to sell all items!"
```

Shop prices themselves still live in `shop.yml`, enchantment prices in `enchantment.yml`.

## Building from source

Requires JDK 21+ and Maven:

```bash
mvn package
# output: target/GGDonutShop-1.21.11-26.3.jar
```

The only dependencies are `paper-api` and `VaultAPI` (both `provided` scope).

## Plugin layout

| Path | Purpose |
|------|---------|
| `net.notdakuxd.nxdonutshop.NxDonutShop` | main class, commands, fluctuation scheduler |
| `...shop.ShopManager` | shop.yml items + price fluctuation |
| `...shop.ShopSettings` | typed access to config.yml (with legacy migration) |
| `...shop.EnchantmentManager` | enchantment.yml pricing |
| `...listener.ItemLoreListener` | idempotent Sell-Worth lore handling |
| `...listener.ShopListener` | GUI click/sell handling |
| `...gui.SellGUI` / `SellHolder` | configurable sell menu |
| `...dialogue.PaperDialogue` | Paper dialog-based shop UI |
| `...util.ColorUtil` | hex code translation |
