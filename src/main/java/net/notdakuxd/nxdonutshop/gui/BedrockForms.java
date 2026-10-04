package net.notdakuxd.nxdonutshop.gui;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.milkbowl.vault.economy.Economy;
import net.notdakuxd.nxdonutshop.NxDonutShop;
import net.notdakuxd.nxdonutshop.listener.ItemLoreListener;
import net.notdakuxd.nxdonutshop.shop.ShopManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class BedrockForms {
   public static boolean hasFormsSupport() {
      try {
         Class.forName("org.geysermc.floodgate.api.FloodgateApi");
         Class.forName("org.geysermc.cumulus.form.SimpleForm");
         return true;
      } catch (Throwable ignored) {
         return false;
      }
   }

   public static void openQuickBuyForm(Player player) {
      if (!hasFormsSupport()) {
         NxDonutShop.getInstance().openQuickBuyGUI(player);
      } else {
         try {
            ShopManager shopManager = NxDonutShop.getInstance().getShopManager();
            Map<Integer, ItemStack> savedItems = NxDonutShop.getInstance().getPlayerShopData(player.getUniqueId());
            Class<?> simpleFormClass = Class.forName("org.geysermc.cumulus.form.SimpleForm");
            Object builder = simpleFormClass.getMethod("builder", new Class[0]).invoke(null, new Object[0]);
            builder.getClass().getMethod("title", new Class[]{String.class}).invoke(builder, new Object[]{"§6§lQuick Buy Shop"});
            builder.getClass()
               .getMethod("content", new Class[]{String.class})
               .invoke(builder, new Object[]{"§7Tap an item to Buy/Sell or tap an empty slot to configure:\n"});

            for (int i = 0; i < 27; i++) {
               int slot = i;
               if (savedItems != null && savedItems.containsKey(slot)) {
                  ItemStack item = (ItemStack)savedItems.get(slot);
                  Material mat = item.getType();
                  int amount = item.getAmount();
                  double buyUnit = shopManager.getBuyPrice(mat);
                  double sellUnit = shopManager.getSellPrice(mat);
                  String btnText = "§f"
                     + formatName(mat.name())
                     + " §7("
                     + amount
                     + "x)\n§aBuy: $"
                     + (int)(buyUnit * amount)
                     + " §e| Sell: $"
                     + (int)(sellUnit * amount);
                  builder.getClass().getMethod("button", new Class[]{String.class}).invoke(builder, new Object[]{btnText});
               } else {
                  builder.getClass()
                     .getMethod("button", new Class[]{String.class})
                     .invoke(builder, new Object[]{"§8[Slot " + (slot + 1) + "] §7Empty (Tap to add item)"});
               }
            }

            Consumer<Object> handler = responseObj -> {
               try {
                  int clickedButton = (Integer)responseObj.getClass().getMethod("clickedButtonId", new Class[0]).invoke(responseObj, new Object[0]);
                  if (clickedButton >= 0 && clickedButton < 27) {
                     handleSlotClick(player, clickedButton);
                  }
               } catch (Throwable var3x) {
               }
            };
            builder.getClass().getMethod("validResultHandler", new Class[]{Consumer.class}).invoke(builder, new Object[]{handler});
            Object form = builder.getClass().getMethod("build", new Class[0]).invoke(builder, new Object[0]);
            sendForm(player, form);
         } catch (Throwable ex) {
            NxDonutShop.getInstance().openQuickBuyGUI(player);
         }
      }
   }

   private static void handleSlotClick(Player player, int slot) {
      ShopManager shopManager = NxDonutShop.getInstance().getShopManager();
      Map<Integer, ItemStack> savedItems = NxDonutShop.getInstance().getPlayerShopData(player.getUniqueId());
      if (savedItems != null && savedItems.containsKey(slot)) {
         ItemStack item = (ItemStack)savedItems.get(slot);
         Material mat = item.getType();
         int amount = item.getAmount();
         double buyUnit = shopManager.getBuyPrice(mat);
         double sellUnit = shopManager.getSellPrice(mat);
         double totalBuy = buyUnit * amount;
         double totalSell = sellUnit * amount;

         try {
            Class<?> simpleFormClass = Class.forName("org.geysermc.cumulus.form.SimpleForm");
            Object builder = simpleFormClass.getMethod("builder", new Class[0]).invoke(null, new Object[0]);
            builder.getClass().getMethod("title", new Class[]{String.class}).invoke(builder, new Object[]{"§6" + formatName(mat.name())});
            builder.getClass()
               .getMethod("content", new Class[]{String.class})
               .invoke(
                  builder,
                  new Object[]{
                     "§fItem: §e"
                        + formatName(mat.name())
                        + " §7(x"
                        + amount
                        + ")\n§fBuy Price: §a$"
                        + (int)totalBuy
                        + "\n§fSell Price: §6$"
                        + (int)totalSell
                        + "\n\n§7Choose an action below:"
                  }
               );
            builder.getClass()
               .getMethod("button", new Class[]{String.class})
               .invoke(builder, new Object[]{"§a§l✔ BUY (" + amount + "x for $" + (int)totalBuy + ")"});
            builder.getClass()
               .getMethod("button", new Class[]{String.class})
               .invoke(builder, new Object[]{"§6§l$ SELL (" + amount + "x for $" + (int)totalSell + ")"});
            builder.getClass().getMethod("button", new Class[]{String.class}).invoke(builder, new Object[]{"§e§l\ud83d\udd04 CHANGE ITEM"});
            builder.getClass().getMethod("button", new Class[]{String.class}).invoke(builder, new Object[]{"§c§l✖ REMOVE FROM SLOT"});
            builder.getClass().getMethod("button", new Class[]{String.class}).invoke(builder, new Object[]{"§7◀ Back to Quick Buy"});
            Consumer<Object> handler = responseObj -> {
               try {
                  int action = (Integer)responseObj.getClass().getMethod("clickedButtonId", new Class[0]).invoke(responseObj, new Object[0]);
                  Economy econ = NxDonutShop.getInstance().getEconomy();
                  if (action == 0) {
                     if (econ != null && !econ.has(player, totalBuy)) {
                        player.sendMessage(ChatColor.RED + "You need $" + (int)totalBuy + " to buy this!");
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
                        openQuickBuyForm(player);
                        return;
                     }

                     if (econ != null) {
                        econ.withdrawPlayer(player, totalBuy);
                     }

                     ItemStack stack = new ItemStack(mat, amount);
                     ItemLoreListener.refreshItem(stack);
                     player.getInventory().addItem(new ItemStack[]{stack});
                     player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0F, 1.2F);
                     player.sendMessage(ChatColor.GREEN + "✔ Purchased " + amount + "x " + formatName(mat.name()) + " for $" + (int)totalBuy + "!");
                     openQuickBuyForm(player);
                  } else if (action == 1) {
                     if (!player.getInventory().containsAtLeast(new ItemStack(mat), amount)) {
                        player.sendMessage(ChatColor.RED + "You don't have " + amount + "x " + formatName(mat.name()) + " in your inventory!");
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
                        openQuickBuyForm(player);
                        return;
                     }

                     player.getInventory().removeItem(new ItemStack[]{new ItemStack(mat, amount)});
                     if (econ != null) {
                        econ.depositPlayer(player, totalSell);
                     }

                     player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.2F);
                     player.sendMessage(ChatColor.GOLD + "✔ Sold " + amount + "x " + formatName(mat.name()) + " for $" + (int)totalSell + "!");
                     openQuickBuyForm(player);
                  } else if (action == 2) {
                     openItemSelectForm(player, slot, null, 1);
                  } else if (action == 3) {
                     NxDonutShop.getInstance().removeQuickBuySlotItem(player, slot);
                     player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0F, 1.0F);
                     openQuickBuyForm(player);
                  } else {
                     openQuickBuyForm(player);
                  }
               } catch (Throwable var13x) {
               }
            };
            builder.getClass().getMethod("validResultHandler", new Class[]{Consumer.class}).invoke(builder, new Object[]{handler});
            Object form = builder.getClass().getMethod("build", new Class[0]).invoke(builder, new Object[0]);
            sendForm(player, form);
         } catch (Throwable ex) {
            openQuickBuyForm(player);
         }
      } else {
         openItemSelectForm(player, slot, null, 1);
      }
   }

   public static void openItemSelectForm(Player player, int slot, String query, int page) {
      if (!hasFormsSupport()) {
         BedrockShopGUI.open(player, slot, query, page);
      } else {
         try {
            ShopManager shopManager = NxDonutShop.getInstance().getShopManager();
            List<Material> materials = shopManager.getAvailableMaterials(query);
            int itemsPerPage = 30;
            int totalItems = materials.size();
            int totalPages = Math.max(1, (int)Math.ceil((double)totalItems / itemsPerPage));
            int currentPage = Math.max(1, Math.min(page, totalPages));
            int startIndex = (currentPage - 1) * itemsPerPage;
            int endIndex = Math.min(startIndex + itemsPerPage, totalItems);
            Class<?> simpleFormClass = Class.forName("org.geysermc.cumulus.form.SimpleForm");
            Object builder = simpleFormClass.getMethod("builder", new Class[0]).invoke(null, new Object[0]);
            builder.getClass()
               .getMethod("title", new Class[]{String.class})
               .invoke(builder, new Object[]{"§6Choose Item §7(Page " + currentPage + "/" + totalPages + ")"});
            builder.getClass()
               .getMethod("content", new Class[]{String.class})
               .invoke(builder, new Object[]{"§7Tap an item to select it for your Quick Buy:\n"});
            if (currentPage > 1) {
               builder.getClass().getMethod("button", new Class[]{String.class}).invoke(builder, new Object[]{"§e◀ Previous Page (" + (currentPage - 1) + ")"});
            }

            if (currentPage < totalPages) {
               builder.getClass().getMethod("button", new Class[]{String.class}).invoke(builder, new Object[]{"§aNext Page ▶ (" + (currentPage + 1) + ")"});
            }

            builder.getClass().getMethod("button", new Class[]{String.class}).invoke(builder, new Object[]{"§c✖ Back to Quick Buy"});
            int navCount = (currentPage > 1 ? 1 : 0) + (currentPage < totalPages ? 1 : 0) + 1;

            for (int i = startIndex; i < endIndex; i++) {
               Material mat = (Material)materials.get(i);
               double buy = shopManager.getBuyPrice(mat);
               double sell = shopManager.getSellPrice(mat);
               builder.getClass()
                  .getMethod("button", new Class[]{String.class})
                  .invoke(builder, new Object[]{"§f" + formatName(mat.name()) + "\n§aBuy: $" + (int)buy + " §e| Sell: $" + (int)sell});
            }

            Consumer<Object> handler = responseObj -> {
               try {
                  int clicked = (Integer)responseObj.getClass().getMethod("clickedButtonId", new Class[0]).invoke(responseObj, new Object[0]);
                  int idx = 0;
                  if (currentPage > 1) {
                     if (clicked == idx) {
                        openItemSelectForm(player, slot, query, currentPage - 1);
                        return;
                     }

                     idx++;
                  }

                  if (currentPage < totalPages) {
                     if (clicked == idx) {
                        openItemSelectForm(player, slot, query, currentPage + 1);
                        return;
                     }

                     idx++;
                  }

                  if (clicked == idx) {
                     openQuickBuyForm(player);
                     return;
                  }

                  idx++;
                  int itemIndex = startIndex + (clicked - navCount);
                  if (itemIndex >= startIndex && itemIndex < endIndex) {
                     Material chosen = (Material)materials.get(itemIndex);
                     ItemStack displayItem = NxDonutShop.getInstance().createQuickBuyItem(chosen, chosen.getMaxStackSize());
                     NxDonutShop.getInstance().setQuickBuySlotItem(player, slot, displayItem);
                     player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8F, 1.5F);
                     player.sendMessage(ChatColor.GREEN + "✔ Added " + formatName(chosen.name()) + " to Quick Buy Slot " + (slot + 1) + "!");
                     openQuickBuyForm(player);
                  }
               } catch (Throwable var15x) {
               }
            };
            builder.getClass().getMethod("validResultHandler", new Class[]{Consumer.class}).invoke(builder, new Object[]{handler});
            Object form = builder.getClass().getMethod("build", new Class[0]).invoke(builder, new Object[0]);
            sendForm(player, form);
         } catch (Throwable ex) {
            BedrockShopGUI.open(player, slot, query, page);
         }
      }
   }

   private static void sendForm(Player player, Object form) {
      try {
         Class<?> floodgateApiClass = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
         Object api = floodgateApiClass.getMethod("getInstance", new Class[0]).invoke(null, new Object[0]);
         Method sendFormMethod = api.getClass().getMethod("sendForm", new Class[]{UUID.class, Class.forName("org.geysermc.cumulus.form.Form")});
         sendFormMethod.invoke(api, new Object[]{player.getUniqueId(), form});
      } catch (Throwable var5) {
      }
   }

   private static String formatName(String raw) {
      if (raw == null) {
         return "";
      }

      String[] parts = raw.toLowerCase().split("_");
      StringBuilder sb = new StringBuilder();

      for (String p : parts) {
         if (!p.isEmpty()) {
            if (sb.length() > 0) {
               sb.append(" ");
            }

            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
         }
      }

      return sb.toString();
   }
}
