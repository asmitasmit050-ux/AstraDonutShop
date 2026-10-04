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

public class BedrockAmountGUI {
   public static void open(Player player, int slot, Material material, int amount) {
      int maxStack = material.getMaxStackSize();
      int safeAmount = Math.max(1, Math.min(amount, maxStack));
      BedrockAmountHolder holder = new BedrockAmountHolder(slot, material, safeAmount);
      Component title = Component.text("Choose Amount: " + safeAmount).decoration(TextDecoration.ITALIC, false);
      Inventory gui = Bukkit.createInventory(holder, 27, title);
      holder.setInventory(gui);
      populateGUI(gui, material, safeAmount, slot);
      player.openInventory(gui);
   }

   public static void updateInPlace(Inventory gui, BedrockAmountHolder holder, int newAmount) {
      int maxStack = holder.getMaterial().getMaxStackSize();
      int safeAmount = Math.max(1, Math.min(newAmount, maxStack));
      holder.setAmount(safeAmount);
      populateGUI(gui, holder.getMaterial(), safeAmount, holder.getSlot());
   }

   private static void populateGUI(Inventory gui, Material mat, int amount, int slot) {
      ShopManager shopManager = NxDonutShop.getInstance().getShopManager();
      double buyUnit = shopManager.getBuyPrice(mat);
      double sellUnit = shopManager.getSellPrice(mat);
      double totalBuy = buyUnit * amount;
      double totalSell = sellUnit * amount;
      int maxStack = mat.getMaxStackSize();
      ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
      ItemMeta paneMeta = pane.getItemMeta();
      if (paneMeta != null) {
         paneMeta.displayName(Component.empty());
         pane.setItemMeta(paneMeta);
      }

      for (int i = 0; i < 27; i++) {
         gui.setItem(i, pane);
      }

      ItemStack displayItem = new ItemStack(mat, amount);
      ItemMeta displayMeta = displayItem.getItemMeta();
      if (displayMeta != null) {
         for (ItemFlag flag : ItemFlag.values()) {
            displayMeta.addItemFlags(new ItemFlag[]{flag});
         }

         displayMeta.displayName(
            ((TranslatableComponent)Component.translatable(mat.translationKey()).color(NamedTextColor.WHITE)).decoration(TextDecoration.ITALIC, false)
         );
         List<Component> lore = new ArrayList();
         lore.add(
            ((TextComponent)((TextComponent)Component.text("Sell Worth: ").color(NamedTextColor.WHITE)).decoration(TextDecoration.ITALIC, false))
               .append(((TextComponent)Component.text("$ " + (int)sellUnit).color(NamedTextColor.GREEN)).decoration(TextDecoration.ITALIC, false))
         );
         lore.add(Component.empty());
         lore.add(
            ((TextComponent)((TextComponent)Component.text("Selected Amount: ").color(NamedTextColor.YELLOW)).decoration(TextDecoration.ITALIC, false))
               .append(((TextComponent)Component.text(amount + "x").color(NamedTextColor.WHITE)).decoration(TextDecoration.ITALIC, false))
         );
         lore.add(
            ((TextComponent)((TextComponent)Component.text("Total Buy: ").color(NamedTextColor.GRAY)).decoration(TextDecoration.ITALIC, false))
               .append(((TextComponent)Component.text("$" + (int)totalBuy).color(NamedTextColor.GREEN)).decoration(TextDecoration.ITALIC, false))
         );
         lore.add(
            ((TextComponent)((TextComponent)Component.text("Total Sell: ").color(NamedTextColor.GRAY)).decoration(TextDecoration.ITALIC, false))
               .append(((TextComponent)Component.text("$" + (int)totalSell).color(NamedTextColor.GOLD)).decoration(TextDecoration.ITALIC, false))
         );
         displayMeta.lore(lore);
         displayItem.setItemMeta(displayMeta);
      }

      gui.setItem(13, displayItem);
      gui.setItem(10, createPresetButton(Material.LIME_DYE, "1x", 1, amount == 1));
      if (maxStack >= 16) {
         gui.setItem(11, createPresetButton(Material.LIME_DYE, "16x", 16, amount == 16));
      }

      if (maxStack >= 32) {
         gui.setItem(12, createPresetButton(Material.LIME_DYE, "32x", 32, amount == 32));
      }

      gui.setItem(14, createPresetButton(Material.LIME_DYE, maxStack + "x (Max)", maxStack, amount == maxStack));
      ItemStack signBtn = new ItemStack(Material.WRITABLE_BOOK);
      ItemMeta signMeta = signBtn.getItemMeta();
      if (signMeta != null) {
         signMeta.displayName(((TextComponent)Component.text("⌨ Custom Amount").color(NamedTextColor.GOLD)).decoration(TextDecoration.ITALIC, false));
         List<Component> lore = new ArrayList();
         lore.add(((TextComponent)Component.text("Click and type amount in chat").color(NamedTextColor.GRAY)).decoration(TextDecoration.ITALIC, false));
         signMeta.lore(lore);
         signBtn.setItemMeta(signMeta);
      }

      gui.setItem(15, signBtn);
      ItemStack cancel = new ItemStack(Material.BARRIER);
      ItemMeta cancelMeta = cancel.getItemMeta();
      if (cancelMeta != null) {
         cancelMeta.displayName(((TextComponent)Component.text("✖ Cancel").color(NamedTextColor.RED)).decoration(TextDecoration.ITALIC, false));
         cancel.setItemMeta(cancelMeta);
      }

      gui.setItem(20, cancel);
      ItemStack confirm = new ItemStack(Material.EMERALD_BLOCK);
      ItemMeta confirmMeta = confirm.getItemMeta();
      if (confirmMeta != null) {
         confirmMeta.displayName(
            ((TextComponent)Component.text("✔ Add to Quick Buy (" + amount + "x)").color(NamedTextColor.GREEN)).decoration(TextDecoration.ITALIC, false)
         );
         List<Component> lore = new ArrayList();
         lore.add(((TextComponent)Component.text("Click to save to Slot " + (slot + 1)).color(NamedTextColor.GRAY)).decoration(TextDecoration.ITALIC, false));
         confirmMeta.lore(lore);
         confirm.setItemMeta(confirmMeta);
      }

      gui.setItem(24, confirm);
   }

   private static ItemStack createPresetButton(Material mat, String label, int count, boolean isSelected) {
      ItemStack item = new ItemStack(mat);
      ItemMeta meta = item.getItemMeta();
      if (meta != null) {
         meta.displayName(
            ((TextComponent)Component.text(label).color(isSelected ? NamedTextColor.GREEN : NamedTextColor.WHITE)).decoration(TextDecoration.ITALIC, false)
         );
         List<Component> lore = new ArrayList();
         if (isSelected) {
            lore.add(((TextComponent)Component.text("● Currently Selected").color(NamedTextColor.GREEN)).decoration(TextDecoration.ITALIC, false));
         } else {
            lore.add(((TextComponent)Component.text("Click to select " + label).color(NamedTextColor.GRAY)).decoration(TextDecoration.ITALIC, false));
         }

         meta.lore(lore);
         item.setItemMeta(meta);
      }

      return item;
   }
}
