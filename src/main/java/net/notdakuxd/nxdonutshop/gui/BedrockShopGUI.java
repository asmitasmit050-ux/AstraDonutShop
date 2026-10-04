package net.notdakuxd.nxdonutshop.gui;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.notdakuxd.nxdonutshop.NxDonutShop;
import net.notdakuxd.nxdonutshop.shop.ShopManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class BedrockShopGUI {
   public static final int ITEMS_PER_PAGE = 45;

   public static void open(Player player, int slot, String query, int page) {
      ShopManager shopManager = NxDonutShop.getInstance().getShopManager();
      List<Material> materials = shopManager.getAvailableMaterials(query);
      int totalItems = materials.size();
      int totalPages = Math.max(1, (int)Math.ceil(totalItems / 45.0));
      int currentPage = Math.max(1, Math.min(page, totalPages));
      BedrockShopHolder holder = new BedrockShopHolder(slot, query, currentPage);
      String titleText = query != null && !query.isBlank()
         ? "Search: " + query + " (" + totalItems + ")"
         : "Choose Item (" + currentPage + "/" + totalPages + ")";
      Component title = Component.text(titleText).decoration(TextDecoration.ITALIC, false);
      Inventory gui = Bukkit.createInventory(holder, 54, title);
      holder.setInventory(gui);
      populateGUI(gui, shopManager, materials, currentPage, totalPages, query);
      player.openInventory(gui);
   }

   public static void updateInPlace(Inventory gui, BedrockShopHolder holder, int newPage) {
      ShopManager shopManager = NxDonutShop.getInstance().getShopManager();
      List<Material> materials = shopManager.getAvailableMaterials(holder.getQuery());
      int totalItems = materials.size();
      int totalPages = Math.max(1, (int)Math.ceil(totalItems / 45.0));
      int currentPage = Math.max(1, Math.min(newPage, totalPages));
      holder.setPage(currentPage);
      populateGUI(gui, shopManager, materials, currentPage, totalPages, holder.getQuery());
   }

   private static void populateGUI(Inventory gui, ShopManager shopManager, List<Material> materials, int currentPage, int totalPages, String query) {
      int totalItems = materials.size();
      int startIndex = (currentPage - 1) * 45;
      int endIndex = Math.min(startIndex + 45, totalItems);

      for (int i = 0; i < 45; i++) {
         int itemIndex = startIndex + i;
         if (itemIndex >= endIndex) {
            gui.setItem(i, null);
         } else {
            Material mat = (Material)materials.get(itemIndex);
            double buy = shopManager.getBuyPrice(mat);
            double sell = shopManager.getSellPrice(mat);
            ItemStack item = new ItemStack(mat, 1);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
               for (ItemFlag flag : ItemFlag.values()) {
                  meta.addItemFlags(new ItemFlag[]{flag});
               }

               meta.displayName(
                  ((TranslatableComponent)Component.translatable(mat.translationKey()).color(NamedTextColor.WHITE)).decoration(TextDecoration.ITALIC, false)
               );
               List<Component> lore = new ArrayList();
               lore.add(
                  ((TextComponent)((TextComponent)Component.text("Sell Worth: ").color(NamedTextColor.WHITE)).decoration(TextDecoration.ITALIC, false))
                     .append(((TextComponent)Component.text("$ " + (int)sell).color(NamedTextColor.GREEN)).decoration(TextDecoration.ITALIC, false))
               );
               lore.add(Component.empty());
               lore.add(
                  ((TextComponent)((TextComponent)Component.text("▪ Buy: ").color(NamedTextColor.GRAY)).decoration(TextDecoration.ITALIC, false))
                     .append(((TextComponent)Component.text("$" + (int)buy).color(NamedTextColor.GREEN)).decoration(TextDecoration.ITALIC, false))
               );
               lore.add(((TextComponent)Component.text("Click to choose amount on sign").color(NamedTextColor.YELLOW)).decoration(TextDecoration.ITALIC, false));
               meta.lore(lore);
               item.setItemMeta(meta);
            }

            gui.setItem(i, item);
         }
      }

      ItemStack grayPane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
      ItemMeta paneMeta = grayPane.getItemMeta();
      if (paneMeta != null) {
         paneMeta.displayName(Component.empty());
         grayPane.setItemMeta(paneMeta);
      }

      for (int s = 45; s < 54; s++) {
         gui.setItem(s, grayPane);
      }

      if (currentPage > 1) {
         ItemStack prev = new ItemStack(Material.ARROW);
         ItemMeta meta = prev.getItemMeta();
         if (meta != null) {
            meta.displayName(
               ((TextComponent)Component.text("◀ Prev Page (" + (currentPage - 1) + "/" + totalPages + ")").color(NamedTextColor.YELLOW))
                  .decoration(TextDecoration.ITALIC, false)
            );
            prev.setItemMeta(meta);
         }

         gui.setItem(45, prev);
      }

      ItemStack searchBtn = new ItemStack(Material.OAK_SIGN);
      ItemMeta searchMeta = searchBtn.getItemMeta();
      if (searchMeta != null) {
         searchMeta.displayName(((TextComponent)Component.text("\ud83d\udd0d Search Item").color(NamedTextColor.GOLD)).decoration(TextDecoration.ITALIC, false));
         List<Component> lore = new ArrayList();
         lore.add(((TextComponent)Component.text("Click to search on sign board").color(NamedTextColor.GRAY)).decoration(TextDecoration.ITALIC, false));
         if (query != null && !query.isBlank()) {
            lore.add(
               ((TextComponent)Component.text("Current: " + query + " (Click to reset)").color(NamedTextColor.AQUA)).decoration(TextDecoration.ITALIC, false)
            );
         }

         searchMeta.lore(lore);
         searchBtn.setItemMeta(searchMeta);
      }

      gui.setItem(48, searchBtn);
      ItemStack pageIndicator = new ItemStack(Material.PAPER);
      ItemMeta pageMeta = pageIndicator.getItemMeta();
      if (pageMeta != null) {
         pageMeta.displayName(
            ((TextComponent)Component.text("Page " + currentPage + " of " + totalPages).color(NamedTextColor.WHITE)).decoration(TextDecoration.ITALIC, false)
         );
         pageIndicator.setItemMeta(pageMeta);
      }

      gui.setItem(49, pageIndicator);
      ItemStack closeItem = new ItemStack(Material.BARRIER);
      ItemMeta closeMeta = closeItem.getItemMeta();
      if (closeMeta != null) {
         closeMeta.displayName(((TextComponent)Component.text("Close").color(NamedTextColor.WHITE)).decoration(TextDecoration.ITALIC, false));
         closeItem.setItemMeta(closeMeta);
      }

      gui.setItem(50, closeItem);
      if (currentPage < totalPages) {
         ItemStack next = new ItemStack(Material.ARROW);
         ItemMeta meta = next.getItemMeta();
         if (meta != null) {
            meta.displayName(
               ((TextComponent)Component.text("Next Page ▶ (" + (currentPage + 1) + "/" + totalPages + ")").color(NamedTextColor.GREEN))
                  .decoration(TextDecoration.ITALIC, false)
            );
            next.setItemMeta(meta);
         }

         gui.setItem(53, next);
      }
   }
}
