package net.notdakuxd.nxdonutshop.listener;

import java.util.HashMap;
import java.util.Map.Entry;
import net.milkbowl.vault.economy.Economy;
import net.notdakuxd.nxdonutshop.NxDonutShop;
import net.notdakuxd.nxdonutshop.gui.BedrockShopGUI;
import net.notdakuxd.nxdonutshop.gui.BedrockShopHolder;
import net.notdakuxd.nxdonutshop.gui.QuickBuyHolder;
import net.notdakuxd.nxdonutshop.gui.SellGUI;
import net.notdakuxd.nxdonutshop.gui.SellHolder;
import net.notdakuxd.nxdonutshop.shop.ShopManager;
import net.notdakuxd.nxdonutshop.util.BedrockUtil;
import net.notdakuxd.nxdonutshop.util.SignPromptManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class ShopListener implements Listener {
   @EventHandler
   public void onInventoryClick(InventoryClickEvent event) {
      if (event.getWhoClicked() instanceof Player player) {
         if (event.getInventory().getHolder() instanceof SellHolder sellHolder) {
            int rawSlot = event.getRawSlot();
            Inventory inv = event.getInventory();
            int sellSlot = SellGUI.resolveSellSlot(sellHolder, inv.getSize());
            if (rawSlot == sellSlot) {
               event.setCancelled(true);
               ShopManager shopManager = NxDonutShop.getInstance().getShopManager();
               Economy econ = NxDonutShop.getInstance().getEconomy();
               double totalEarned = 0.0;
               int totalCount = 0;

               for (int i = 0; i < inv.getSize(); i++) {
                  if (i == sellSlot) {
                     continue;
                  }
                  ItemStack item = inv.getItem(i);
                  if (item != null && !item.getType().isAir()) {
                     Material mat = item.getType();
                     if (shopManager.isItemInShop(mat)) {
                        double unitSell = shopManager.getSellPrice(mat);
                        int amount = item.getAmount();
                        totalEarned += unitSell * amount;
                        totalCount += amount;
                        inv.setItem(i, null);
                     }
                  }
               }

               if (totalCount > 0) {
                  if (econ != null) {
                     econ.depositPlayer(player, totalEarned);
                  }

                  player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.2F);
                  player.playSound(player.getLocation(), Sound.BLOCK_CHEST_CLOSE, 1.0F, 1.0F);
                  player.sendMessage(
                     ChatColor.GREEN
                        + "✔ Successfully sold "
                        + ChatColor.YELLOW
                        + totalCount
                        + ChatColor.GREEN
                        + " items for "
                        + ChatColor.GOLD
                        + "$"
                        + (int)totalEarned
                        + ChatColor.GREEN
                        + "!"
                  );
               } else {
                  player.sendMessage(ChatColor.RED + "No sellable items found in the sell chest!");
                  player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
               }
            }
         } else {
            if (event.getInventory().getHolder() instanceof BedrockShopHolder bedrockHolder) {
               event.setCancelled(true);
               int rawSlot = event.getRawSlot();
               if (rawSlot < 0 || rawSlot >= 54) {
                  return;
               }

               ItemStack clicked = event.getCurrentItem();
               if (clicked == null || clicked.getType() == Material.GRAY_STAINED_GLASS_PANE || clicked.getType().isAir()) {
                  return;
               }

               if (rawSlot == 45 && clicked.getType() == Material.ARROW) {
                  BedrockShopGUI.updateInPlace(event.getInventory(), bedrockHolder, bedrockHolder.getPage() - 1);
                  player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7F, 1.2F);
                  return;
               }

               if (rawSlot == 48) {
                  int targetSlot = bedrockHolder.getSlot();
                  String[] searchSign = new String[]{"Search Item", "↓↓↓↓↓↓↓↓↓↓", "", ""};
                  SignPromptManager.openSignPrompt(player, searchSign, lines -> {
                     String query = "";
                     if (lines.length > 2 && lines[2] != null && !lines[2].isBlank()) {
                        query = lines[2].trim();
                     } else if (lines.length > 3 && lines[3] != null && !lines[3].isBlank()) {
                        query = lines[3].trim();
                     } else {
                        for (String line : lines) {
                           if (line != null && !line.isBlank()) {
                              String trimmed = line.trim();
                              if (!trimmed.startsWith("↓") && !trimmed.toLowerCase().contains("search") && !trimmed.toLowerCase().contains("item")) {
                                 query = trimmed;
                                 break;
                              }
                           }
                        }
                     }

                     BedrockShopGUI.open(player, targetSlot, query, 1);
                  });
                  return;
               }

               if (rawSlot == 50) {
                  NxDonutShop.getInstance().openQuickBuyGUI(player);
                  return;
               }

               if (rawSlot == 53 && clicked.getType() == Material.ARROW) {
                  BedrockShopGUI.updateInPlace(event.getInventory(), bedrockHolder, bedrockHolder.getPage() + 1);
                  player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7F, 1.2F);
                  return;
               }

               if (rawSlot >= 0 && rawSlot < 45) {
                  Material mat = clicked.getType();
                  int targetSlot = bedrockHolder.getSlot();
                  int maxStack = mat.getMaxStackSize();
                  String[] amountSign = new String[]{"Enter Amount", "↓↓↓↓↓↓↓↓↓↓", "", ""};
                  SignPromptManager.openSignPrompt(player, amountSign, lines -> {
                     int chosenAmount = maxStack;
                     String rawNumber = "";
                     if (lines.length > 2 && lines[2] != null && !lines[2].isBlank()) {
                        rawNumber = lines[2].trim();
                     } else if (lines.length > 3 && lines[3] != null && !lines[3].isBlank()) {
                        rawNumber = lines[3].trim();
                     } else {
                        for (String line : lines) {
                           if (line != null && !line.isBlank()) {
                              String trimmed = line.trim();
                              if (!trimmed.startsWith("↓") && !trimmed.toLowerCase().contains("enter") && !trimmed.toLowerCase().contains("amount")) {
                                 rawNumber = trimmed;
                                 break;
                              }
                           }
                        }
                     }

                     if (!rawNumber.isBlank()) {
                        try {
                           int parsed = Integer.parseInt(rawNumber);
                           if (parsed > 0) {
                              chosenAmount = Math.min(parsed, maxStack);
                           }
                        } catch (Exception var12x) {
                        }
                     }

                     ItemStack displayItem = NxDonutShop.getInstance().createQuickBuyItem(player, mat, chosenAmount);
                     NxDonutShop.getInstance().setQuickBuySlotItem(player, targetSlot, displayItem);
                     player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8F, 1.5F);
                     NxDonutShop.getInstance().openQuickBuyGUI(player);
                  });
               }
            }

            if (event.getInventory().getHolder() instanceof QuickBuyHolder) {
               event.setCancelled(true);
               int rawSlot = event.getRawSlot();
               if (rawSlot >= 0 && rawSlot < 54) {
                  ItemStack clicked = event.getCurrentItem();
                  if (clicked == null || clicked.getType().isAir()) {
                     return;
                  }

                  if (clicked.getType() == Material.GRAY_STAINED_GLASS_PANE) {
                     NxDonutShop.getInstance().openPaperDialogue(player, rawSlot);
                     return;
                  }

                  boolean isBedrock = BedrockUtil.isBedrockPlayer(player);
                  if (isBedrock) {
                     if (event.getClick() == ClickType.RIGHT
                        || event.getClick() == ClickType.DROP
                        || event.getClick() == ClickType.CONTROL_DROP
                        || event.getClick() == ClickType.MIDDLE) {
                        NxDonutShop.getInstance().openPaperDialogue(player, rawSlot);
                        return;
                     }
                  } else if (event.isShiftClick()) {
                     NxDonutShop.getInstance().openPaperDialogue(player, rawSlot);
                     return;
                  }

                  ShopManager shopManager = NxDonutShop.getInstance().getShopManager();
                  Economy econ = NxDonutShop.getInstance().getEconomy();
                  if (!shopManager.isItemInShop(clicked.getType())) {
                     player.sendMessage(ChatColor.RED + "This item is not configured in the shop.");
                     return;
                  }

                  int stackCount = clicked.getAmount();
                  double unitBuy = shopManager.getBuyPrice(clicked.getType());
                  double enchantTotalUnit = NxDonutShop.getInstance().getEnchantmentManager().getTotalEnchantmentPrice(clicked.getEnchantments());
                  double totalBuy = (unitBuy + enchantTotalUnit) * stackCount;
                  if (econ != null) {
                     if (!econ.has(player, totalBuy)) {
                        player.sendMessage(ChatColor.RED + "You need $" + (int)totalBuy + " to buy " + stackCount + "x " + clicked.getType().name() + "!");
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
                        return;
                     }

                     econ.withdrawPlayer(player, totalBuy);
                  }

                  ItemStack toAdd = new ItemStack(clicked.getType(), stackCount);
                  if (!clicked.getEnchantments().isEmpty()) {
                     for (Entry<Enchantment, Integer> entry : clicked.getEnchantments().entrySet()) {
                        toAdd.addUnsafeEnchantment((Enchantment)entry.getKey(), (Integer)entry.getValue());
                     }
                  }

                  HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(new ItemStack[]{toAdd});
                  if (leftover != null && !leftover.isEmpty()) {
                     for (ItemStack drop : leftover.values()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), drop);
                     }

                     player.sendMessage(ChatColor.YELLOW + "⚠ Inventory full! Purchased items dropped at your feet.");
                  }

                  player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0F, 1.2F);
                  player.sendMessage(ChatColor.GREEN + "✔ Purchased " + stackCount + "x " + clicked.getType().name() + " for $" + (int)totalBuy + "!");
                  // Refresh sell-worth lore so freshly bought items show the current sell price
                  org.bukkit.Bukkit.getScheduler()
                     .runTaskLater(NxDonutShop.getInstance(), () -> ItemLoreListener.formatPlayerInventory(player), 1L);
               }
            }
         }
      }
   }

   @EventHandler
   public void onInventoryClose(InventoryCloseEvent event) {
      if (event.getInventory().getHolder() instanceof SellHolder sellHolder) {
         if (!(event.getPlayer() instanceof Player player)) {
            return;
         }

         Inventory var9 = event.getInventory();
         int sellSlot = SellGUI.resolveSellSlot(sellHolder, var9.getSize());

         for (int i = 0; i < var9.getSize(); i++) {
            if (i == sellSlot) {
               continue;
            }
            ItemStack item = var9.getItem(i);
            if (item != null && !item.getType().isAir()) {
               HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(new ItemStack[]{item});

               for (ItemStack drop : leftover.values()) {
                  player.getWorld().dropItemNaturally(player.getLocation(), drop);
               }

               var9.setItem(i, null);
            }
         }

         player.playSound(player.getLocation(), Sound.BLOCK_CHEST_CLOSE, 1.0F, 1.0F);
      }
   }
}
