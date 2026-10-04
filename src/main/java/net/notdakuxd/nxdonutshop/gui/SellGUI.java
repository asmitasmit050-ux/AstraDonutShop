package net.notdakuxd.nxdonutshop.gui;

import net.notdakuxd.nxdonutshop.NxDonutShop;
import net.notdakuxd.nxdonutshop.shop.ShopSettings;
import net.notdakuxd.nxdonutshop.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * The /sell (/sellg, /sellgui) chest menu.
 *
 * Fully configurable through the `sell-gui` section of config.yml:
 *   title, size, sell-slot and the sell button (material, name, lore).
 * All text supports legacy & codes and &#RRGGBB hex colors.
 */
public final class SellGUI {
    /** Fallback slot used when the holder does not carry an explicit slot. */
    public static final int DEFAULT_SELL_SLOT = ShopSettings.DEFAULT_GUI_SLOT;

    private SellGUI() {
    }

    public static void open(Player player) {
        ShopSettings settings = NxDonutShop.getInstance().getSettings();
        int size = settings != null ? settings.getSellGuiSize() : ShopSettings.DEFAULT_GUI_SIZE;
        int sellSlot = settings != null ? settings.getSellGuiSlot() : DEFAULT_SELL_SLOT;
        String title = settings != null ? settings.getSellGuiTitle() : ShopSettings.DEFAULT_GUI_TITLE;

        SellHolder holder = new SellHolder(sellSlot);
        Inventory gui = Bukkit.createInventory(holder, size, ColorUtil.toComponent(title));
        holder.setInventory(gui);

        ItemStack sellButton = new ItemStack(
            settings != null ? settings.getSellGuiButtonMaterial() : org.bukkit.Material.BARRIER
        );
        ItemMeta meta = sellButton.getItemMeta();
        if (meta != null) {
            for (ItemFlag flag : ItemFlag.values()) {
                meta.addItemFlags(flag);
            }
            meta.displayName(ColorUtil.toComponent(
                settings != null ? settings.getSellGuiButtonName() : ShopSettings.DEFAULT_BUTTON_NAME
            ));
            meta.lore(ColorUtil.toComponents(
                settings != null ? settings.getSellGuiButtonLore() : ShopSettings.DEFAULT_BUTTON_LORE
            ));
            sellButton.setItemMeta(meta);
        }

        gui.setItem(sellSlot, sellButton);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
        player.openInventory(gui);
    }

    /**
     * Resolves the effective sell-button slot for a sell GUI, preferring the slot stored in
     * the holder and falling back to the current settings/default.
     */
    public static int resolveSellSlot(SellHolder holder, int guiSize) {
        int slot = holder != null ? holder.getSellSlot() : -1;
        if (slot < 0) {
            ShopSettings settings = NxDonutShop.getInstance().getSettings();
            slot = settings != null ? settings.getSellGuiSlot() : DEFAULT_SELL_SLOT;
        }
        if (slot < 0 || slot >= guiSize) {
            slot = guiSize - 1;
        }
        return slot;
    }
}
