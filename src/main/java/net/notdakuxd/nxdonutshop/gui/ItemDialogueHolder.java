package net.notdakuxd.nxdonutshop.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class ItemDialogueHolder implements InventoryHolder {
   private final int targetSlot;
   private final int page;
   private final String query;
   private Inventory inventory;

   public ItemDialogueHolder(int targetSlot, int page, String query) {
      this.targetSlot = targetSlot;
      this.page = page;
      this.query = query;
   }

   public int getTargetSlot() {
      return this.targetSlot;
   }

   public int getPage() {
      return this.page;
   }

   public String getQuery() {
      return this.query;
   }

   public void setInventory(Inventory inventory) {
      this.inventory = inventory;
   }

   public Inventory getInventory() {
      return this.inventory;
   }
}
