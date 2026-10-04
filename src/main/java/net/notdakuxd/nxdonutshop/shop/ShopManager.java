package net.notdakuxd.nxdonutshop.shop;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import net.notdakuxd.nxdonutshop.NxDonutShop;
import net.notdakuxd.nxdonutshop.listener.ItemLoreListener;
import net.notdakuxd.nxdonutshop.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class ShopManager {
    private final JavaPlugin plugin;
    private final Map<Material, ShopItem> shopItems = new LinkedHashMap<>();
    private final Map<Material, Double> priceMultipliers = new HashMap<>();
    private File configFile;
    private FileConfiguration config;

    public ShopManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.loadConfig();
    }

    public void loadConfig() {
        if (!this.plugin.getDataFolder().exists()) {
            this.plugin.getDataFolder().mkdirs();
        }

        this.configFile = new File(this.plugin.getDataFolder(), "shop.yml");
        if (!this.configFile.exists()) {
            this.plugin.saveResource("shop.yml", false);
        }

        this.config = YamlConfiguration.loadConfiguration(this.configFile);
        this.shopItems.clear();
        ConfigurationSection section = this.config.getConfigurationSection("items");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                try {
                    Material mat = Material.matchMaterial(key);
                    if (mat != null && isSupportedItem(mat)) {
                        double buy = section.getDouble(key + ".buy", 20.0);
                        double sell = section.getDouble(key + ".sell", 5.0);
                        this.shopItems.put(mat, new ShopItem(mat, buy, sell));
                        this.priceMultipliers.putIfAbsent(mat, 1.0);
                    }
                } catch (Exception ignored) {
                }
            }
        }
    }

    private ShopSettings settings() {
        return NxDonutShop.getInstance() != null ? NxDonutShop.getInstance().getSettings() : null;
    }

    /**
     * Fluctuates all market prices within the configured multiplier range, then (optionally)
     * broadcasts the fully configurable announcement from config.yml. Does nothing when the
     * feature is disabled (`price-fluctuation.enabled: false`).
     */
    public void randomizePrices() {
        ShopSettings settings = settings();
        if (this.shopItems.isEmpty() || settings == null || !settings.isFluctuationEnabled()) {
            return;
        }

        ThreadLocalRandom rand = ThreadLocalRandom.current();
        double min = settings.getMinMultiplier();
        double max = settings.getMaxMultiplier();
        for (Material mat : this.shopItems.keySet()) {
            double mult = min + rand.nextDouble() * (max - min);
            double roundedMult = Math.round(mult * 100.0) / 100.0;
            this.priceMultipliers.put(mat, roundedMult);
        }

        if (settings.isAnnouncementEnabled()) {
            for (String line : settings.getAnnouncement()) {
                String text = line
                    .replace("{min}", String.valueOf(min))
                    .replace("{max}", String.valueOf(max))
                    .replace("{interval}", String.valueOf(settings.getFluctuationIntervalMinutes()))
                    .replace("{min-multiplier}", String.valueOf(min))
                    .replace("{max-multiplier}", String.valueOf(max))
                    .replace("{interval-minutes}", String.valueOf(settings.getFluctuationIntervalMinutes()));
                Bukkit.broadcastMessage(ColorUtil.colorize(text));
            }
        }

        // Refresh the Sell Worth lines of everyone online (respects the lore toggle).
        for (Player p : Bukkit.getOnlinePlayers()) {
            ItemLoreListener.formatPlayerInventory(p);
        }
    }

    public static boolean isSupportedItem(Material mat) {
        if (mat != null && mat.isItem() && !mat.isAir()) {
            String name = mat.name();
            return !name.startsWith("LEGACY_")
                && !name.equals("DRAGON_EGG")
                && !name.startsWith("INFESTED_")
                && !name.startsWith("POTTED_")
                && !name.startsWith("ATTACHED_")
                && !name.startsWith("WALL_")
                && !name.endsWith("_WALL_HANGING_SIGN")
                && !name.endsWith("_WALL_SIGN")
                && !name.endsWith("_WALL_FAN")
                && !name.endsWith("_WALL_TORCH")
                && !name.endsWith("_CANDLE_CAKE");
        } else {
            return false;
        }
    }

    public boolean isItemInShop(Material material) {
        return this.shopItems.containsKey(material);
    }

    public double getMultiplier(Material material) {
        return this.priceMultipliers.getOrDefault(material, 1.0);
    }

    public double getBuyPrice(Material material) {
        ShopItem item = this.shopItems.get(material);
        if (item == null) {
            return 20.0;
        }
        double mult = this.priceMultipliers.getOrDefault(material, 1.0);
        return Math.max(1.0, Math.round(item.baseBuyPrice() * mult));
    }

    public double getSellPrice(Material material) {
        ShopItem item = this.shopItems.get(material);
        if (item == null) {
            return 5.0;
        }
        double mult = this.priceMultipliers.getOrDefault(material, 1.0);
        return Math.max(1.0, Math.round(item.baseSellPrice() * mult));
    }

    public List<Material> getAvailableMaterials(String query) {
        List<Material> list = new ArrayList<>(this.shopItems.keySet());
        list.sort(Comparator.comparing(Enum::name));
        if (query != null && !query.trim().isEmpty()) {
            String lower = query.toLowerCase().trim().replace('_', ' ');
            return list.stream().filter(m -> m.name().toLowerCase().replace('_', ' ').contains(lower)).collect(Collectors.toList());
        } else {
            return list;
        }
    }

    public Map<Material, ShopItem> getShopItems() {
        return this.shopItems;
    }

    /** Whether price fluctuation is enabled (reads the live settings). */
    public boolean isRandomizerEnabled() {
        ShopSettings settings = settings();
        return settings == null || settings.isFluctuationEnabled();
    }

    /** Currently configured fluctuation interval in minutes (reads the live settings). */
    public int getRandomizerIntervalMinutes() {
        ShopSettings settings = settings();
        return settings != null ? settings.getFluctuationIntervalMinutes() : 15;
    }

    public record ShopItem(Material material, double baseBuyPrice, double baseSellPrice) {
    }
}
