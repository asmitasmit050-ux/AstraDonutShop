package net.notdakuxd.nxdonutshop.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class BedrockShopHolder implements InventoryHolder {
   private Inventory inventory;
   private final int slot;
   private final String query;
   private int page;

   public BedrockShopHolder(int slot, String query, int page) {
      this.slot = slot;
      this.query = query;
      this.page = page;
   }

   public Inventory getInventory() {
      return this.inventory;
   }

   public void setInventory(Inventory inventory) {
      this.inventory = inventory;
   }

   public int getSlot() {
      return this.slot;
   }

   public String getQuery() {
      return this.query;
   }

   public int getPage() {
      return this.page;
   }

   public void setPage(int page) {
      this.page = page;
   }
}
