package net.notdakuxd.nxdonutshop.listener;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.notdakuxd.nxdonutshop.NxDonutShop;
import net.notdakuxd.nxdonutshop.gui.BedrockAmountHolder;
import net.notdakuxd.nxdonutshop.gui.BedrockShopHolder;
import net.notdakuxd.nxdonutshop.gui.ItemDialogueHolder;
import net.notdakuxd.nxdonutshop.gui.QuickBuyHolder;
import net.notdakuxd.nxdonutshop.gui.SellHolder;
import net.notdakuxd.nxdonutshop.shop.ShopManager;
import net.notdakuxd.nxdonutshop.shop.ShopSettings;
import net.notdakuxd.nxdonutshop.util.ColorUtil;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

/**
 * Maintains the "Sell Worth" lore line on shop items in player inventories.
 *
 * Fixes from the original implementation:
 *  - The line is tracked with a PersistentDataContainer marker, so it is never added twice.
 *  - Every existing "Sell Worth" line (any color format, including duplicates left behind by
 *    older builds) is collapsed back into exactly one line before the new one is written.
 *  - The line is refreshed when the market price changes (price fluctuation) and when the
 *    configured line format changes.
 *  - The whole feature can be toggled with `sell-worth-lore.enabled` in config.yml. When
 *    disabled, the lore line (and marker) is stripped from items again.
 */
public class ItemLoreListener implements Listener {
    /** Keyword detect used to recognize sell-worth lore lines, regardless of coloring. */
    private static final String LINE_KEYWORD = "sell worth";
    private static NamespacedKey markerKey;

    private static NamespacedKey markerKey() {
        if (markerKey == null) {
            markerKey = new NamespacedKey(NxDonutShop.getInstance(), "sell_worth_price");
        }
        return markerKey;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerJoin(PlayerJoinEvent event) {
        formatPlayerInventory(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player) {
            ItemStack stack = event.getItem().getItemStack();
            if (refreshItem(stack)) {
                event.getItem().setItemStack(stack);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        ItemStack result = event.getCurrentItem();
        if (result != null && !result.getType().isAir()) {
            refreshItem(result);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (event.getPlayer() instanceof Player player) {
            formatPlayerInventory(player);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        // Never touch the display items inside our own GUIs.
        if (isPluginInventory(event.getInventory()) || isPluginInventory(event.getClickedInventory())) {
            return;
        }

        ItemStack current = event.getCurrentItem();
        if (current != null && !current.getType().isAir()) {
            refreshItem(current);
        }

        ItemStack cursor = event.getCursor();
        if (cursor != null && !cursor.getType().isAir()) {
            refreshItem(cursor);
        }
    }

    private static boolean isPluginInventory(Inventory inventory) {
        if (inventory == null) {
            return false;
        }
        Object holder = inventory.getHolder();
        return holder instanceof SellHolder
            || holder instanceof QuickBuyHolder
            || holder instanceof BedrockShopHolder
            || holder instanceof BedrockAmountHolder
            || holder instanceof ItemDialogueHolder;
    }

    /**
     * Applies (or removes, when the feature is disabled) the sell-worth lore for every item
     * in the player's inventory. Backwards compatible alias of the original method name.
     */
    public static void formatPlayerInventory(Player player) {
        if (player == null) {
            return;
        }
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && !item.getType().isAir()) {
                refreshItem(item);
            }
        }
    }

    /**
     * Toggle-aware updater: adds/refreshes the sell-worth line when the feature is enabled,
     * and strips it (plus any duplicates and the marker) when it is disabled.
     *
     * @return true when the item was modified
     */
    public static boolean refreshItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }
        ShopSettings settings = NxDonutShop.getInstance().getSettings();
        if (settings != null && settings.isSellWorthLoreEnabled()) {
            return applyItemWorthLore(item, NxDonutShop.getInstance().getShopManager());
        }
        return removeItemWorthLore(item);
    }

    /**
     * Adds or refreshes the configured "Sell Worth" lore line. Idempotent: when the line is
     * already present and up to date nothing is re-written, so no duplicates can appear.
     *
     * @return true when the item was modified
     */
    public static boolean applyItemWorthLore(ItemStack item, ShopManager shopManager) {
        if (item == null || item.getType() == Material.AIR || shopManager == null) {
            return false;
        }
        Material mat = item.getType();
        if (!shopManager.isItemInShop(mat)) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }

        double sellPrice = shopManager.getSellPrice(mat);

        // Collapse every previous sell-worth line (any format / duplicates) into a clean base.
        List<Component> originalLore = meta.lore();
        List<Component> baseLore = originalLore == null ? new ArrayList<>() : new ArrayList<>(originalLore);
        baseLore.removeIf(ItemLoreListener::isSellWorthLine);

        Component worthLine = buildWorthLine(item, sellPrice);
        List<Component> targetLore = new ArrayList<>(baseLore);
        targetLore.add(0, worthLine);

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        Double markedPrice = pdc.get(markerKey(), PersistentDataType.DOUBLE);
        boolean upToDate = markedPrice != null
            && Math.abs(markedPrice - sellPrice) < 1.0E-6
            && targetLore.equals(originalLore);
        if (upToDate) {
            // Already applied and still accurate - do not touch the item again.
            return false;
        }

        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        meta.lore(targetLore);
        pdc.set(markerKey(), PersistentDataType.DOUBLE, sellPrice);
        item.setItemMeta(meta);
        return true;
    }

    /**
     * Removes all "Sell Worth" lore lines (plus the marker) from the item.
     *
     * @return true when the item was modified
     */
    public static boolean removeItemWorthLore(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }

        List<Component> originalLore = meta.lore();
        List<Component> baseLore = originalLore == null ? new ArrayList<>() : new ArrayList<>(originalLore);
        boolean removedLine = baseLore.removeIf(ItemLoreListener::isSellWorthLine);

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        boolean hadMarker = pdc.has(markerKey(), PersistentDataType.DOUBLE);
        if (!removedLine && !hadMarker) {
            return false;
        }

        meta.lore(baseLore.isEmpty() ? null : baseLore);
        pdc.remove(markerKey());
        item.setItemMeta(meta);
        return true;
    }

    /** Strips colors, then checks whether the line is a sell-worth line (legacy or new). */
    private static boolean isSellWorthLine(Component component) {
        return ColorUtil.plainText(component).toLowerCase(Locale.ROOT).contains(LINE_KEYWORD);
    }

    /** Builds the lore line from the configured template (supports hex codes and placeholders). */
    private static Component buildWorthLine(ItemStack item, double sellPrice) {
        String template = ShopSettings.DEFAULT_SELL_WORTH_LINE;
        ShopSettings settings = NxDonutShop.getInstance().getSettings();
        if (settings != null && settings.getSellWorthLine() != null && !settings.getSellWorthLine().isBlank()) {
            template = settings.getSellWorthLine();
        }
        String text = template
            .replace("{price}", String.valueOf((int) Math.round(sellPrice)))
            .replace("{total}", String.valueOf((int) Math.round(sellPrice * item.getAmount())))
            .replace("{amount}", String.valueOf(item.getAmount()))
            .replace("{material}", item.getType().name());
        return ColorUtil.toComponent(text);
    }
}
