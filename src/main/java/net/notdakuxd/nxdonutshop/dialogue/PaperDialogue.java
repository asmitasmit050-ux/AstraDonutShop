package net.notdakuxd.nxdonutshop.dialogue;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.DialogBase.DialogAfterAction;
import io.papermc.paper.registry.data.dialog.DialogRegistryEntry.Builder;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.body.ItemDialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.TextDialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import io.papermc.paper.registry.data.dialog.type.MultiActionType;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.event.ClickCallback.Options;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.notdakuxd.nxdonutshop.NxDonutShop;
import net.notdakuxd.nxdonutshop.shop.EnchantmentManager;
import net.notdakuxd.nxdonutshop.shop.ShopManager;
import net.notdakuxd.nxdonutshop.util.NxSpriteResolver;
import net.notdakuxd.nxdonutshop.util.SkinRestorerHeadResolver;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class PaperDialogue {
   public static final int COLUMNS = 4;
   public static final int ENCHANT_COLUMNS = 6;
   public static final int ITEMS_PER_PAGE = 48;
   public static final int BUTTON_WIDTH = 125;
   public static final int ENCHANT_NAME_WIDTH = 140;
   public static final int ENCHANT_LVL_WIDTH = 38;
   private static final Options CLICK_OPTIONS = (Options)Options.builder().uses(-1).lifetime(Duration.ofDays(7L)).build();

   public static Component getLivePlayerHead(Player player) {
      return SkinRestorerHeadResolver.resolvePlayerHeadComponent(player);
   }

   public static String formatCompactBalance(double balance) {
      if (balance < 1000.0) {
         return String.format("%,d", new Object[]{(int)balance});
      } else if (balance < 1000000.0) {
         double val = balance / 1000.0;
         return val == (long)val
            ? String.format("%dk", new Object[]{(long)val})
            : String.format(Locale.US, "%.1fk", new Object[]{val}).replaceAll("\\.0k$", "k");
      } else if (balance < 1.0E9) {
         double val = balance / 1000000.0;
         return val == (long)val
            ? String.format("%dM", new Object[]{(long)val})
            : String.format(Locale.US, "%.1fM", new Object[]{val}).replaceAll("\\.0M$", "M");
      } else {
         double val = balance / 1.0E9;
         return val == (long)val
            ? String.format("%dB", new Object[]{(long)val})
            : String.format(Locale.US, "%.1fB", new Object[]{val}).replaceAll("\\.0B$", "B");
      }
   }

   public static Component buildHeaderBalance(Player player) {
      double balance = NxDonutShop.getInstance().getEconomy() != null ? NxDonutShop.getInstance().getEconomy().getBalance(player) : 0.0;
      Component headComponent = getLivePlayerHead(player)
         .color(NamedTextColor.WHITE)
         .decoration(TextDecoration.BOLD, false)
         .decoration(TextDecoration.ITALIC, false);
      return ((TextComponent)((TextComponent)((TextComponent)((TextComponent)((TextComponent)((TextComponent)((TextComponent)Component.text("   |   ")
                              .color(NamedTextColor.DARK_GRAY))
                           .decoration(TextDecoration.BOLD, false))
                        .decoration(TextDecoration.ITALIC, false))
                     .append(Component.empty().color(NamedTextColor.WHITE)))
                  .append(headComponent))
               .append(Component.space()))
            .append(
               ((TextComponent)((TextComponent)Component.text("Balance: ").color(NamedTextColor.WHITE)).decoration(TextDecoration.BOLD, false))
                  .decoration(TextDecoration.ITALIC, false)
            ))
         .append(
            ((TextComponent)((TextComponent)Component.text("$" + formatCompactBalance(balance)).color(NamedTextColor.GREEN))
                  .decoration(TextDecoration.BOLD, false))
               .decoration(TextDecoration.ITALIC, false)
         );
   }

   public static void openItemDialogue(Player player, int slot) {
      openItemDialogue(player, slot, null, 1);
   }

   public static void openItemDialogue(Player player, int slot, String query) {
      openItemDialogue(player, slot, query, 1);
   }

   public static void openItemDialogue(Player player, int slot, String query, int page) {
      if (player != null && player.isOnline()) {
         ShopManager shopManager = NxDonutShop.getInstance().getShopManager();
         List<Material> materials = shopManager.getAvailableMaterials(query);
         int totalItems = materials.size();
         int totalPages = Math.max(1, (int)Math.ceil(totalItems / 48.0));
         int currentPage = Math.max(1, Math.min(page, totalPages));
         int startIndex = (currentPage - 1) * 48;
         int endIndex = Math.min(startIndex + 48, totalItems);
         List<ActionButton> buttons = new ArrayList();
         String safeQuery = query != null && !query.isBlank() ? query.trim() : "";
         Component spyglassSprite = NxSpriteResolver.renderItemSprite(Material.SPYGLASS);
         Component searchLabel = spyglassSprite.append(Component.space())
            .append(
               ((TextComponent)((TextComponent)Component.text("Search").color(NamedTextColor.WHITE)).decoration(TextDecoration.BOLD, false))
                  .decoration(TextDecoration.ITALIC, false)
            );
         ActionButton searchBtn = ActionButton.create(
            searchLabel, Component.text("Click to search item typed in search box"), 125, DialogAction.customClick((view, audience) -> {
               if (audience instanceof Player p) {
                  String inputQuery = view.getText("search");
                  openItemDialogue(p, slot, inputQuery != null ? inputQuery.trim() : "", 1);
               }
            }, CLICK_OPTIONS)
         );
         buttons.add(searchBtn);
         if (currentPage > 1) {
            ActionButton prevBtn = ActionButton.create(
               ((TextComponent)((TextComponent)Component.text("◀ Prev Page").color(NamedTextColor.YELLOW)).decoration(TextDecoration.BOLD, false))
                  .decoration(TextDecoration.ITALIC, false),
               Component.text("Go to Page " + (currentPage - 1)),
               125,
               DialogAction.customClick((view, audience) -> {
                  if (audience instanceof Player p) {
                     openItemDialogue(p, slot, safeQuery, currentPage - 1);
                  }
               }, CLICK_OPTIONS)
            );
            buttons.add(prevBtn);
         } else if (!safeQuery.isEmpty()) {
            ActionButton resetBtn = ActionButton.create(
               ((TextComponent)((TextComponent)Component.text("Clear Search").color(NamedTextColor.GRAY)).decoration(TextDecoration.BOLD, false))
                  .decoration(TextDecoration.ITALIC, false),
               Component.text("Show all items"),
               125,
               DialogAction.customClick((view, audience) -> {
                  if (audience instanceof Player p) {
                     openItemDialogue(p, slot, "", 1);
                  }
               }, CLICK_OPTIONS)
            );
            buttons.add(resetBtn);
         } else {
            ActionButton startBtn = ActionButton.create(
               ((TextComponent)((TextComponent)Component.text("First Page").color(NamedTextColor.DARK_GRAY)).decoration(TextDecoration.BOLD, false))
                  .decoration(TextDecoration.ITALIC, false),
               Component.text("You are on Page 1"),
               125,
               DialogAction.customClick((view, audience) -> {}, CLICK_OPTIONS)
            );
            buttons.add(startBtn);
         }

         Component paperSprite = NxSpriteResolver.renderItemSprite(Material.PAPER);
         Component pageIndicatorLabel = paperSprite.append(Component.space())
            .append(
               ((TextComponent)((TextComponent)Component.text("Page " + currentPage + "/" + totalPages).color(NamedTextColor.WHITE))
                     .decoration(TextDecoration.BOLD, false))
                  .decoration(TextDecoration.ITALIC, false)
            );
         ActionButton pageIndicator = ActionButton.create(
            pageIndicatorLabel, Component.text("Click to open direct page list (1-" + totalPages + ")"), 125, DialogAction.customClick((view, audience) -> {
               if (audience instanceof Player p) {
                  openPageSelectDialogue(p, slot, safeQuery);
               }
            }, CLICK_OPTIONS)
         );
         buttons.add(pageIndicator);
         if (currentPage < totalPages) {
            ActionButton nextBtn = ActionButton.create(
               ((TextComponent)((TextComponent)Component.text("Next Page ▶").color(NamedTextColor.GREEN)).decoration(TextDecoration.BOLD, false))
                  .decoration(TextDecoration.ITALIC, false),
               Component.text("Go to Page " + (currentPage + 1)),
               125,
               DialogAction.customClick((view, audience) -> {
                  if (audience instanceof Player p) {
                     openItemDialogue(p, slot, safeQuery, currentPage + 1);
                  }
               }, CLICK_OPTIONS)
            );
            buttons.add(nextBtn);
         } else {
            ActionButton endBtn = ActionButton.create(
               ((TextComponent)((TextComponent)Component.text("Last Page").color(NamedTextColor.DARK_GRAY)).decoration(TextDecoration.BOLD, false))
                  .decoration(TextDecoration.ITALIC, false),
               Component.text("You are on the last page"),
               125,
               DialogAction.customClick((view, audience) -> {}, CLICK_OPTIONS)
            );
            buttons.add(endBtn);
         }

         for (int i = startIndex; i < endIndex; i++) {
            Material mat = (Material)materials.get(i);
            double buy = shopManager.getBuyPrice(mat);
            Component itemSprite = NxSpriteResolver.renderItemSprite(mat);
            Component label = itemSprite.append(Component.space())
               .append(
                  ((TranslatableComponent)((TranslatableComponent)Component.translatable(mat.translationKey()).color(NamedTextColor.WHITE))
                        .decoration(TextDecoration.BOLD, false))
                     .decoration(TextDecoration.ITALIC, false)
               );
            Component tooltip = ((TranslatableComponent)((TranslatableComponent)((TranslatableComponent)((TranslatableComponent)((TranslatableComponent)Component.translatable(
                                 mat.translationKey()
                              )
                              .color(NamedTextColor.WHITE))
                           .decoration(TextDecoration.BOLD, false))
                        .decoration(TextDecoration.ITALIC, false))
                     .append(Component.newline()))
                  .append(
                     ((TextComponent)((TextComponent)Component.text("Worth: ").color(NamedTextColor.WHITE)).decoration(TextDecoration.BOLD, false))
                        .decoration(TextDecoration.ITALIC, false)
                  ))
               .append(
                  ((TextComponent)((TextComponent)Component.text("$ " + (int)buy).color(NamedTextColor.GREEN)).decoration(TextDecoration.BOLD, false))
                     .decoration(TextDecoration.ITALIC, false)
               );
            Material itemMat = mat;
            ActionButton itemBtn = ActionButton.create(label, tooltip, 125, DialogAction.customClick((view, audience) -> {
               if (audience instanceof Player p) {
                  if (NxDonutShop.getInstance().getEnchantmentManager().isEnchantable(itemMat)) {
                     openEnchantmentDialogue(p, itemMat, slot, new LinkedHashMap());
                  } else {
                     openAmountDialogue(p, itemMat, slot);
                  }
               }
            }, CLICK_OPTIONS));
            buttons.add(itemBtn);
         }

         Component barrierSprite = NxSpriteResolver.renderItemSprite(Material.BARRIER);
         Component closeLabel = barrierSprite.append(Component.space())
            .append(
               ((TextComponent)((TextComponent)Component.text("Back").color(NamedTextColor.WHITE)).decoration(TextDecoration.BOLD, false))
                  .decoration(TextDecoration.ITALIC, false)
            );
         ActionButton cancel = ActionButton.create(closeLabel, Component.text("Return to Quick Buy"), 110, DialogAction.customClick((view, audience) -> {
            if (audience instanceof Player p) {
               NxDonutShop.getInstance().openQuickBuyGUI(p);
            }
         }, CLICK_OPTIONS));
         TextDialogInput searchInput = DialogInput.text("search", Component.text("Search Item")).width(240).initial(safeQuery).build();
         MultiActionType multiAction = DialogType.multiAction(buttons, cancel, 4);
         String titleSuffix = safeQuery.isEmpty() ? "Page " + currentPage + "/" + totalPages : "Search: '" + safeQuery + "' (" + totalItems + " found)";
         Component dialogTitle = ((TextComponent)((TextComponent)((TextComponent)Component.text("Choose Item (" + titleSuffix + ")")
                     .color(NamedTextColor.WHITE))
                  .decoration(TextDecoration.BOLD, false))
               .decoration(TextDecoration.ITALIC, false))
            .append(buildHeaderBalance(player));
         DialogBase base = DialogBase.builder(dialogTitle)
            .inputs(List.of(searchInput))
            .canCloseWithEscape(true)
            .pause(false)
            .afterAction(DialogAfterAction.NONE)
            .build();
         Dialog dialog = Dialog.create(factory -> ((Builder)factory.empty()).base(base).type(multiAction));
         player.showDialog(dialog);
      }
   }

   public static void openEnchantmentDialogue(Player player, Material material, int slot, Map<Enchantment, Integer> selectedEnchants) {
      if (player != null && player.isOnline()) {
         EnchantmentManager enchManager = NxDonutShop.getInstance().getEnchantmentManager();
         List<Enchantment> applicable = enchManager.getApplicableEnchantments(material);
         if (selectedEnchants == null) {
            selectedEnchants = new LinkedHashMap();
         }

         Map<Enchantment, Integer> currentSelected = new LinkedHashMap(selectedEnchants);
         ItemStack previewItem = new ItemStack(material);
         EnchantmentManager.applyEnchantments(previewItem, currentSelected);
         ItemDialogBody itemBody = DialogBody.item(previewItem).showTooltip(true).build();
         List<ActionButton> buttons = new ArrayList();

         for (Enchantment ench : applicable) {
            int maxLevel = Math.max(1, Math.min(ench.getMaxLevel(), 5));
            int currentLevel = (Integer)currentSelected.getOrDefault(ench, 0);
            boolean hasConflict = false;

            for (Enchantment selected : currentSelected.keySet()) {
               if (selected != ench && (ench.conflictsWith(selected) || selected.conflictsWith(ench))) {
                  hasConflict = true;
                  break;
               }
            }

            NamedTextColor nameColor;
            if (hasConflict) {
               nameColor = NamedTextColor.RED;
            } else {
               nameColor = NamedTextColor.WHITE;
            }

            String displayName = EnchantmentManager.formatEnchantmentName(ench);
            if (maxLevel > 1) {
               displayName = displayName + (currentLevel > 0 ? " " + EnchantmentManager.toRoman(currentLevel) : " I");
            }

            Component nameLabel = ((TextComponent)((TextComponent)Component.text(displayName).color(nameColor)).decoration(TextDecoration.BOLD, false))
               .decoration(TextDecoration.ITALIC, false);
            Enchantment thisEnch = ench;
            int targetLvlOnNameClick = currentLevel == 0 ? 1 : 0;
            ActionButton nameBtn = ActionButton.create(
               nameLabel,
               Component.text(EnchantmentManager.formatEnchantmentName(ench) + " (Click to toggle)"),
               140,
               DialogAction.customClick(
                  (view, audience) -> {
                     if (audience instanceof Player p) {
                        Map<Enchantment, Integer> newEnchants = new LinkedHashMap(currentSelected);
                        if (targetLvlOnNameClick == 0) {
                           newEnchants.remove(thisEnch);
                        } else {
                           newEnchants.entrySet()
                              .removeIf(entry -> ((Enchantment)entry.getKey()).conflictsWith(thisEnch) || thisEnch.conflictsWith((Enchantment)entry.getKey()));
                           newEnchants.put(thisEnch, targetLvlOnNameClick);
                        }

                        openEnchantmentDialogue(p, material, slot, newEnchants);
                     }
                  },
                  CLICK_OPTIONS
               )
            );
            buttons.add(nameBtn);

            for (int lvl = 1; lvl <= 5; lvl++) {
               if (lvl <= maxLevel) {
                  NamedTextColor lvlColor;
                  if (currentLevel == lvl) {
                     lvlColor = NamedTextColor.GREEN;
                  } else if (hasConflict) {
                     lvlColor = NamedTextColor.RED;
                  } else {
                     lvlColor = NamedTextColor.WHITE;
                  }

                  int targetLvl = currentLevel == lvl ? 0 : lvl;
                  ActionButton lvlBtn = ActionButton.create(
                     ((TextComponent)((TextComponent)Component.text(EnchantmentManager.toRoman(lvl)).color(lvlColor)).decoration(TextDecoration.BOLD, false))
                        .decoration(TextDecoration.ITALIC, false),
                     Component.text("Toggle Level " + lvl),
                     38,
                     DialogAction.customClick(
                        (view, audience) -> {
                           if (audience instanceof Player p) {
                              Map<Enchantment, Integer> newEnchants = new LinkedHashMap(currentSelected);
                              if (targetLvl == 0) {
                                 newEnchants.remove(thisEnch);
                              } else {
                                 newEnchants.entrySet()
                                    .removeIf(
                                       entry -> ((Enchantment)entry.getKey()).conflictsWith(thisEnch) || thisEnch.conflictsWith((Enchantment)entry.getKey())
                                    );
                                 newEnchants.put(thisEnch, targetLvl);
                              }

                              openEnchantmentDialogue(p, material, slot, newEnchants);
                           }
                        },
                        CLICK_OPTIONS
                     )
                  );
                  buttons.add(lvlBtn);
               } else {
                  ActionButton emptyBtn = ActionButton.create(
                     ((TextComponent)((TextComponent)Component.text("-").color(NamedTextColor.DARK_GRAY)).decoration(TextDecoration.BOLD, false))
                        .decoration(TextDecoration.ITALIC, false),
                     null,
                     38,
                     DialogAction.customClick((view, audience) -> {}, CLICK_OPTIONS)
                  );
                  buttons.add(emptyBtn);
               }
            }
         }

         Component continueLabel = ((TextComponent)((TextComponent)((TextComponent)Component.text("✔ Continue ").color(NamedTextColor.GREEN))
                  .decoration(TextDecoration.BOLD, false))
               .decoration(TextDecoration.ITALIC, false))
            .append(
               ((TextComponent)((TextComponent)Component.text("->").color(NamedTextColor.WHITE)).decoration(TextDecoration.BOLD, false))
                  .decoration(TextDecoration.ITALIC, false)
            );
         ActionButton continueBtn = ActionButton.create(
            continueLabel, Component.text("Proceed to choose amount and buy"), 260, DialogAction.customClick((view, audience) -> {
               if (audience instanceof Player p) {
                  openAmountDialogue(p, material, slot, currentSelected);
               }
            }, CLICK_OPTIONS)
         );
         MultiActionType multiAction = DialogType.multiAction(buttons, continueBtn, 6);
         Component dialogTitle = ((TextComponent)((TextComponent)Component.text("Choose Enchantments ⚠").color(NamedTextColor.WHITE))
               .decoration(TextDecoration.BOLD, false))
            .append(buildHeaderBalance(player));
         DialogBase base = DialogBase.builder(dialogTitle)
            .body(List.of(itemBody))
            .canCloseWithEscape(true)
            .pause(false)
            .afterAction(DialogAfterAction.NONE)
            .build();
         Dialog dialog = Dialog.create(factory -> ((Builder)factory.empty()).base(base).type(multiAction));
         player.showDialog(dialog);
      }
   }

   public static void openPageSelectDialogue(Player player, int slot, String query) {
      if (player != null && player.isOnline()) {
         ShopManager shopManager = NxDonutShop.getInstance().getShopManager();
         List<Material> materials = shopManager.getAvailableMaterials(query);
         int totalItems = materials.size();
         int totalPages = Math.max(1, (int)Math.ceil(totalItems / 48.0));
         String safeQuery = query != null && !query.isBlank() ? query.trim() : "";
         List<ActionButton> buttons = new ArrayList();
         Component spyglassSprite = NxSpriteResolver.renderItemSprite(Material.SPYGLASS);
         Component jumpLabel = spyglassSprite.append(Component.space())
            .append(((TextComponent)Component.text("Jump").color(NamedTextColor.WHITE)).decoration(TextDecoration.BOLD, false));
         ActionButton jumpBtn = ActionButton.create(
            jumpLabel, Component.text("Go to the page number typed in the box"), 125, DialogAction.customClick((view, audience) -> {
               if (audience instanceof Player p) {
                  String rawPage = view.getText("page_num");
                  int targetPage = 1;
                  if (rawPage != null && !rawPage.isBlank()) {
                     try {
                        targetPage = Math.max(1, Math.min(Integer.parseInt(rawPage.trim()), totalPages));
                     } catch (Exception var9x) {
                     }
                  }

                  openItemDialogue(p, slot, safeQuery, targetPage);
               }
            }, CLICK_OPTIONS)
         );
         buttons.add(jumpBtn);
         Component paperSprite = NxSpriteResolver.renderItemSprite(Material.PAPER);

         for (int p = 1; p <= totalPages; p++) {
            int pageNum = p;
            Component pageLabel = paperSprite.append(Component.space())
               .append(((TextComponent)Component.text("Page " + pageNum).color(NamedTextColor.WHITE)).decoration(TextDecoration.BOLD, false));
            ActionButton pageBtn = ActionButton.create(
               pageLabel, Component.text("Click to open Page " + pageNum), 125, DialogAction.customClick((view, audience) -> {
                  if (audience instanceof Player playerAudience) {
                     openItemDialogue(playerAudience, slot, safeQuery, pageNum);
                  }
               }, CLICK_OPTIONS)
            );
            buttons.add(pageBtn);
         }

         Component barrierSprite = NxSpriteResolver.renderItemSprite(Material.BARRIER);
         Component returnLabel = barrierSprite.append(Component.space())
            .append(((TextComponent)Component.text("Back").color(NamedTextColor.WHITE)).decoration(TextDecoration.BOLD, false));
         ActionButton cancel = ActionButton.create(returnLabel, Component.text("Return to Shop"), 110, DialogAction.customClick((view, audience) -> {
            if (audience instanceof Player p) {
               openItemDialogue(p, slot, safeQuery, 1);
            }
         }, CLICK_OPTIONS));
         TextDialogInput pageInput = DialogInput.text("page_num", Component.text("Page Number (1-" + totalPages + ")"))
            .width(240)
            .initial("1")
            .maxLength(3)
            .build();
         MultiActionType multiAction = DialogType.multiAction(buttons, cancel, 4);
         Component dialogTitle = ((TextComponent)((TextComponent)Component.text("Select Page (Total: " + totalPages + " Pages)").color(NamedTextColor.WHITE))
               .decoration(TextDecoration.BOLD, false))
            .append(buildHeaderBalance(player));
         DialogBase base = DialogBase.builder(dialogTitle)
            .inputs(List.of(pageInput))
            .canCloseWithEscape(true)
            .pause(false)
            .afterAction(DialogAfterAction.NONE)
            .build();
         Dialog dialog = Dialog.create(factory -> ((Builder)factory.empty()).base(base).type(multiAction));
         player.showDialog(dialog);
      }
   }

   public static void openAmountDialogue(Player player, Material material, int slot) {
      openAmountDialogue(player, material, slot, null);
   }

   public static void openAmountDialogue(Player player, Material material, int slot, Map<Enchantment, Integer> enchants) {
      if (player != null && player.isOnline()) {
         int maxStack = material.getMaxStackSize();
         ItemStack itemStack = new ItemStack(material);
         EnchantmentManager.applyEnchantments(itemStack, enchants);
         Map<Enchantment, Integer> currentEnchants = (Map<Enchantment, Integer>)(enchants != null ? new LinkedHashMap(enchants) : Collections.emptyMap());
         ItemDialogBody itemBody = DialogBody.item(itemStack).showTooltip(true).build();
         TextDialogInput amountInput = DialogInput.text("amount", Component.text("Amount")).width(240).initial(String.valueOf(maxStack)).maxLength(4).build();
         ActionButton cancelBtn = ActionButton.create(
            ((TextComponent)Component.text("Cancel!").color(NamedTextColor.RED)).decoration(TextDecoration.BOLD, false),
            null,
            110,
            DialogAction.customClick((view, audience) -> {
               if (audience instanceof Player p) {
                  if (currentEnchants.isEmpty() && !NxDonutShop.getInstance().getEnchantmentManager().isEnchantable(material)) {
                     openItemDialogue(p, slot, "", 1);
                  } else {
                     openEnchantmentDialogue(p, material, slot, currentEnchants);
                  }
               }
            }, CLICK_OPTIONS)
         );
         ActionButton addBtn = ActionButton.create(
            ((TextComponent)Component.text("Add to Quick Buy").color(NamedTextColor.WHITE)).decoration(TextDecoration.BOLD, false),
            null,
            140,
            DialogAction.customClick((view, audience) -> {
               if (audience instanceof Player p) {
                  String rawAmount = view.getText("amount");
                  int amount = material.getMaxStackSize();
                  if (rawAmount != null && !rawAmount.isBlank()) {
                     try {
                        amount = Math.max(1, Math.min(Integer.parseInt(rawAmount.trim()), material.getMaxStackSize()));
                     } catch (Exception var9x) {
                     }
                  }

                  ItemStack displayItem = NxDonutShop.getInstance().createQuickBuyItem(p, material, amount, currentEnchants);
                  NxDonutShop.getInstance().setQuickBuySlotItem(p, slot, displayItem);
                  p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8F, 1.5F);
                  NxDonutShop.getInstance().openQuickBuyGUI(p);
               }
            }, CLICK_OPTIONS)
         );
         MultiActionType type = DialogType.multiAction(List.of(cancelBtn, addBtn), null, 2);
         Component dialogTitle = ((TextComponent)((TextComponent)Component.text("How many to buy? ⚠").color(NamedTextColor.WHITE))
               .decoration(TextDecoration.BOLD, false))
            .append(buildHeaderBalance(player));
         DialogBase base = DialogBase.builder(dialogTitle)
            .body(List.of(itemBody))
            .inputs(List.of(amountInput))
            .canCloseWithEscape(true)
            .pause(false)
            .afterAction(DialogAfterAction.NONE)
            .build();
         Dialog dialog = Dialog.create(factory -> ((Builder)factory.empty()).base(base).type(type));
         player.showDialog(dialog);
      }
   }
}
