package net.notdakuxd.nxdonutshop.gui;

import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class BedrockAmountHolder implements InventoryHolder {
   private Inventory inventory;
   private final int slot;
   private final Material material;
   private int amount;

   public BedrockAmountHolder(int slot, Material material, int amount) {
      this.slot = slot;
      this.material = material;
      this.amount = amount;
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

   public Material getMaterial() {
      return this.material;
   }

   public int getAmount() {
      return this.amount;
   }

   public void setAmount(int amount) {
      this.amount = amount;
   }
}
