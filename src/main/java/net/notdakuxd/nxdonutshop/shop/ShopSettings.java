package net.notdakuxd.nxdonutshop.shop;

import java.util.Arrays;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Central typed access to values from config.yml.
 *
 * New canonical keys are used (mirrored into the bundled config.yml):
 *   price-fluctuation.* - market price randomizer + configurable announcement
 *   sell-worth-lore.*   - toggleable "Sell Worth" lore on held shop items
 *   sell-gui.*          - fully configurable /sell gui menu
 *
 * For backwards compatibility the old keys (randomize, price-randomizer.*) are still
 * honored when the new keys are missing, then migrated into the new ones.
 */
public class ShopSettings {
    public static final List<String> DEFAULT_ANNOUNCEMENT = Arrays.asList(
        "&6&l========================================",
        "&e 📊 &lMARKET UPDATE: &aShop Prices Have Fluctuated!",
        "&7Market economy rates have shifted. Check &b/shop&7!",
        "&6&l========================================"
    );

    public static final String DEFAULT_SELL_WORTH_LINE = "&fSell Worth: &a$ {price}";
    public static final String DEFAULT_GUI_TITLE = "&8Sell Chest";
    public static final int DEFAULT_GUI_SIZE = 54;
    public static final int DEFAULT_GUI_SLOT = 53;
    public static final String DEFAULT_BUTTON_MATERIAL = "BARRIER";
    public static final String DEFAULT_BUTTON_NAME = "&c&lClick to Sell";
    public static final List<String> DEFAULT_BUTTON_LORE = List.of("&7Click to sell all items!");

    private final JavaPlugin plugin;

    private boolean fluctuationEnabled;
    private int fluctuationIntervalMinutes;
    private double minMultiplier;
    private double maxMultiplier;
    private boolean announcementEnabled;
    private List<String> announcement;

    private boolean sellWorthLoreEnabled;
    private String sellWorthLine;

    private String sellGuiTitle;
    private int sellGuiSize;
    private int sellGuiSlot;
    private Material sellGuiButtonMaterial;
    private String sellGuiButtonName;
    private List<String> sellGuiButtonLore;

    public ShopSettings(JavaPlugin plugin) {
        this.plugin = plugin;
        this.load();
    }

    public void load() {
        FileConfiguration config = this.plugin.getConfig();
        boolean changed = migrateLegacyKeys(config);
        if (changed) {
            this.plugin.saveConfig();
        }

        this.fluctuationEnabled = config.getBoolean("price-fluctuation.enabled", true);
        this.fluctuationIntervalMinutes = Math.max(1, config.getInt("price-fluctuation.interval-minutes", 15));
        this.minMultiplier = config.getDouble("price-fluctuation.min-multiplier", 0.70D);
        this.maxMultiplier = config.getDouble("price-fluctuation.max-multiplier", 1.40D);
        if (this.minMultiplier > this.maxMultiplier) {
            double tmp = this.minMultiplier;
            this.minMultiplier = this.maxMultiplier;
            this.maxMultiplier = tmp;
        }

        this.announcementEnabled = config.getBoolean("price-fluctuation.announce.enabled", true);
        List<String> lines = config.getStringList("price-fluctuation.announce.messages");
        if (lines == null || lines.isEmpty()) {
            lines = DEFAULT_ANNOUNCEMENT;
        }
        this.announcement = List.copyOf(lines);

        this.sellWorthLoreEnabled = config.getBoolean("sell-worth-lore.enabled", true);
        this.sellWorthLine = config.getString("sell-worth-lore.line", DEFAULT_SELL_WORTH_LINE);
        if (this.sellWorthLine == null || this.sellWorthLine.isBlank()) {
            this.sellWorthLine = DEFAULT_SELL_WORTH_LINE;
        }

        this.sellGuiTitle = config.getString("sell-gui.title", DEFAULT_GUI_TITLE);
        if (this.sellGuiTitle == null) {
            this.sellGuiTitle = DEFAULT_GUI_TITLE;
        }
        int size = config.getInt("sell-gui.size", DEFAULT_GUI_SIZE);
        if (size < 9 || size > 54 || size % 9 != 0) {
            this.plugin.getLogger().warning("[GGDonutShop] sell-gui.size '" + size + "' is invalid (must be a multiple of 9 between 9 and 54). Using " + DEFAULT_GUI_SIZE + ".");
            size = DEFAULT_GUI_SIZE;
        }
        this.sellGuiSize = size;

        int slot = config.getInt("sell-gui.sell-slot", size - 1);
        if (slot < 0 || slot >= size) {
            this.plugin.getLogger().warning("[GGDonutShop] sell-gui.sell-slot '" + slot + "' is outside the gui (0-" + (size - 1) + "). Using the last slot.");
            slot = size - 1;
        }
        this.sellGuiSlot = slot;

        String matName = config.getString("sell-gui.sell-button.material", DEFAULT_BUTTON_MATERIAL);
        Material mat = matName == null ? null : Material.matchMaterial(matName);
        if (mat == null || !mat.isItem() || mat.isAir()) {
            this.plugin.getLogger().warning("[GGDonutShop] sell-gui.sell-button.material is invalid. Using " + DEFAULT_BUTTON_MATERIAL + ".");
            mat = Material.matchMaterial(DEFAULT_BUTTON_MATERIAL);
        }
        this.sellGuiButtonMaterial = mat;
        this.sellGuiButtonName = config.getString("sell-gui.sell-button.name", DEFAULT_BUTTON_NAME);
        if (this.sellGuiButtonName == null) {
            this.sellGuiButtonName = DEFAULT_BUTTON_NAME;
        }
        List<String> lore = config.getStringList("sell-gui.sell-button.lore");
        this.sellGuiButtonLore = lore == null ? DEFAULT_BUTTON_LORE : List.copyOf(lore);
    }

    /**
     * Migrates legacy keys (from the original config.yml) into the new canonical ones.
     * Only fills keys that do not exist yet, so the admin's current behavior is preserved.
     *
     * @return true when something was written and the config should be saved
     */
    private boolean migrateLegacyKeys(FileConfiguration config) {
        boolean changed = false;

        if (!config.contains("price-fluctuation.enabled")) {
            boolean legacy = config.getBoolean("price-randomizer.enabled", config.getBoolean("randomize", true));
            config.set("price-fluctuation.enabled", legacy);
            changed = true;
        }
        if (!config.contains("price-fluctuation.interval-minutes")) {
            config.set("price-fluctuation.interval-minutes", Math.max(1, config.getInt("price-randomizer.interval-minutes", 15)));
            changed = true;
        }
        if (!config.contains("price-fluctuation.min-multiplier")) {
            config.set("price-fluctuation.min-multiplier", config.getDouble("price-randomizer.min-multiplier", 0.70D));
            changed = true;
        }
        if (!config.contains("price-fluctuation.max-multiplier")) {
            config.set("price-fluctuation.max-multiplier", config.getDouble("price-randomizer.max-multiplier", 1.40D));
            changed = true;
        }
        if (!config.contains("price-fluctuation.announce.enabled")) {
            config.set("price-fluctuation.announce.enabled", config.getBoolean("price-randomizer.broadcast-changes", true));
            changed = true;
        }
        if (!config.contains("price-fluctuation.announce.messages")) {
            config.set("price-fluctuation.announce.messages", DEFAULT_ANNOUNCEMENT);
            changed = true;
        }
        if (!config.contains("sell-worth-lore.enabled")) {
            config.set("sell-worth-lore.enabled", true);
            changed = true;
        }
        if (!config.contains("sell-worth-lore.line")) {
            config.set("sell-worth-lore.line", DEFAULT_SELL_WORTH_LINE);
            changed = true;
        }
        if (!config.contains("sell-gui.title")) {
            config.set("sell-gui.title", DEFAULT_GUI_TITLE);
            changed = true;
        }
        if (!config.contains("sell-gui.size")) {
            config.set("sell-gui.size", DEFAULT_GUI_SIZE);
            changed = true;
        }
        if (!config.contains("sell-gui.sell-slot")) {
            config.set("sell-gui.sell-slot", DEFAULT_GUI_SLOT);
            changed = true;
        }
        if (!config.contains("sell-gui.sell-button.material")) {
            config.set("sell-gui.sell-button.material", DEFAULT_BUTTON_MATERIAL);
            changed = true;
        }
        if (!config.contains("sell-gui.sell-button.name")) {
            config.set("sell-gui.sell-button.name", DEFAULT_BUTTON_NAME);
            changed = true;
        }
        if (!config.contains("sell-gui.sell-button.lore")) {
            config.set("sell-gui.sell-button.lore", DEFAULT_BUTTON_LORE);
            changed = true;
        }
        return changed;
    }

    public boolean isFluctuationEnabled() {
        return this.fluctuationEnabled;
    }

    public int getFluctuationIntervalMinutes() {
        return this.fluctuationIntervalMinutes;
    }

    public double getMinMultiplier() {
        return this.minMultiplier;
    }

    public double getMaxMultiplier() {
        return this.maxMultiplier;
    }

    public boolean isAnnouncementEnabled() {
        return this.announcementEnabled;
    }

    public List<String> getAnnouncement() {
        return this.announcement;
    }

    public boolean isSellWorthLoreEnabled() {
        return this.sellWorthLoreEnabled;
    }

    public String getSellWorthLine() {
        return this.sellWorthLine;
    }

    public String getSellGuiTitle() {
        return this.sellGuiTitle;
    }

    public int getSellGuiSize() {
        return this.sellGuiSize;
    }

    public int getSellGuiSlot() {
        return this.sellGuiSlot;
    }

    public Material getSellGuiButtonMaterial() {
        return this.sellGuiButtonMaterial;
    }

    public String getSellGuiButtonName() {
        return this.sellGuiButtonName;
    }

    public List<String> getSellGuiButtonLore() {
        return this.sellGuiButtonLore;
    }
}
