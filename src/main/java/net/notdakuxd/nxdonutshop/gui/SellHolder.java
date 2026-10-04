package net.notdakuxd.nxdonutshop.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/** Inventory holder for the /sell GUI. Carries the configured sell-button slot. */
public class SellHolder implements InventoryHolder {
    private Inventory inventory;
    private final int sellSlot;

    public SellHolder() {
        this(-1);
    }

    public SellHolder(int sellSlot) {
        this.sellSlot = sellSlot;
    }

    public Inventory getInventory() {
        return this.inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    /** Slot of the sell button; negative means "use the default SellGUI slot". */
    public int getSellSlot() {
        return this.sellSlot;
    }
}
