package net.notdakuxd.nxdonutshop.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class QuickBuyHolder implements InventoryHolder {
   private Inventory inventory;

   public void setInventory(Inventory inventory) {
      this.inventory = inventory;
   }

   public Inventory getInventory() {
      return this.inventory;
   }
}
