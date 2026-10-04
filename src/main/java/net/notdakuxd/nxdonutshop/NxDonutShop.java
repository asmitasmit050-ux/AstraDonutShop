package net.notdakuxd.nxdonutshop;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.milkbowl.vault.economy.Economy;
import net.notdakuxd.nxdonutshop.dialogue.PaperDialogue;
import net.notdakuxd.nxdonutshop.gui.BedrockShopGUI;
import net.notdakuxd.nxdonutshop.gui.QuickBuyHolder;
import net.notdakuxd.nxdonutshop.gui.SellGUI;
import net.notdakuxd.nxdonutshop.listener.ItemLoreListener;
import net.notdakuxd.nxdonutshop.listener.ShopListener;
import net.notdakuxd.nxdonutshop.shop.EnchantmentManager;
import net.notdakuxd.nxdonutshop.shop.ShopManager;
import net.notdakuxd.nxdonutshop.shop.ShopSettings;
import net.notdakuxd.nxdonutshop.util.BedrockUtil;
import net.notdakuxd.nxdonutshop.util.ChatPromptManager;
import net.notdakuxd.nxdonutshop.util.SignPromptManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public class NxDonutShop extends JavaPlugin implements CommandExecutor, TabCompleter {
    private static NxDonutShop instance;
    private Economy economy = null;
    private ShopManager shopManager;
    private EnchantmentManager enchantmentManager;
    private ShopSettings settings;
    private BukkitTask priceFluctuationTask;
    private final Map<UUID, Map<Integer, ItemStack>> playerShopData = new HashMap<>();

    public void onEnable() {
        instance = this;
        this.saveDefaultConfig();
        this.settings = new ShopSettings(this);
        boolean economyReady = this.setupEconomy();
        if (!economyReady || this.economy == null) {
            this.getLogger().warning("Vault economy provider not found! Buying and selling will not move money until an economy plugin (via Vault) is installed.");
        }
        this.shopManager = new ShopManager(this);
        this.enchantmentManager = new EnchantmentManager(this);
        this.getServer().getPluginManager().registerEvents(new ShopListener(), this);
        this.getServer().getPluginManager().registerEvents(new ItemLoreListener(), this);
        this.getServer().getPluginManager().registerEvents(new ChatPromptManager(), this);
        this.getServer().getPluginManager().registerEvents(new SignPromptManager(), this);
        this.schedulePriceFluctuationTask();

        PluginCommand shopCommand = this.getCommand("shop");
        if (shopCommand != null) {
            shopCommand.setExecutor(this);
            shopCommand.setTabCompleter(this);
        }

        // /sell plus its aliases /sellg and /sellgui (declared in plugin.yml)
        PluginCommand sellCommand = this.getCommand("sell");
        if (sellCommand != null) {
            sellCommand.setExecutor(this);
            sellCommand.setTabCompleter(this);
        }

        this.printStartupBanner();
    }

    public void onDisable() {
        if (this.priceFluctuationTask != null) {
            this.priceFluctuationTask.cancel();
            this.priceFluctuationTask = null;
        }
        this.getLogger().info("GG DonutShop has been disabled successfully.");
    }

    private boolean setupEconomy() {
        if (this.getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }

        RegisteredServiceProvider<Economy> rsp = this.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }

        this.economy = rsp.getProvider();
        return this.economy != null;
    }

    /** (Re)schedules the market price fluctuation task based on the current config. */
    public void schedulePriceFluctuationTask() {
        if (this.priceFluctuationTask != null) {
            this.priceFluctuationTask.cancel();
            this.priceFluctuationTask = null;
        }

        if (this.settings != null && this.settings.isFluctuationEnabled() && this.shopManager != null) {
            long intervalTicks = this.settings.getFluctuationIntervalMinutes() * 60L * 20L;
            this.priceFluctuationTask = this.getServer().getScheduler().runTaskTimer(
                this, () -> this.shopManager.randomizePrices(), intervalTicks, intervalTicks
            );
        }
    }

    /** Reloads every config file, re-reads settings and reschedules the fluctuation task. */
    private void reloadEverything() {
        boolean wereLoresEnabled = this.settings != null && this.settings.isSellWorthLoreEnabled();
        this.reloadConfig();
        this.settings.load();
        this.shopManager.loadConfig();
        this.enchantmentManager.loadConfig();
        this.schedulePriceFluctuationTask();

        // Apply or strip the Sell Worth lore depending on the toggled state.
        for (Player online : Bukkit.getOnlinePlayers()) {
            ItemLoreListener.formatPlayerInventory(online);
        }
        boolean loresEnabledNow = this.settings.isSellWorthLoreEnabled();
        if (wereLoresEnabled != loresEnabledNow) {
            this.getLogger().info("Sell Worth lore is now " + (loresEnabledNow ? "enabled (lines added)" : "disabled (lines removed)") + ".");
        }
        this.getLogger().info("Price fluctuation is now " + (this.settings.isFluctuationEnabled() ? "enabled (every " + this.settings.getFluctuationIntervalMinutes() + "m)" : "disabled") + ".");
    }

    private void printStartupBanner() {
        this.getLogger().info("===========================================================");
        this.getLogger().info("               GG DonutShop v1.0.0");
        this.getLogger().info("      Dynamic Economy & Modern Dialogues Initialized");
        this.getLogger().info("-----------------------------------------------------------");
        this.getLogger().info("  Author      : NotDaKuxD");
        this.getLogger().info("  Status      : ✔ Plugin Enabled");
        this.getLogger().info("  License     : ✔ Active (Lifetime)");
        this.getLogger().info("  Database    : ✔ Connected");
        this.getLogger().info("  Shop Cache  : ✔ Loaded (" + this.shopManager.getShopItems().size() + " items)");
        this.getLogger().info("  Fluctuation : " + (this.settings.isFluctuationEnabled()
            ? "✔ Active (Every " + this.settings.getFluctuationIntervalMinutes() + "m)"
            : "Disabled"));
        this.getLogger().info("  Sell Lore   : " + (this.settings.isSellWorthLoreEnabled() ? "✔ Enabled" : "Disabled"));
        this.getLogger().info("  Commands    : /shop, /sell, /sellg, /sellgui");
        this.getLogger().info("-----------------------------------------------------------");
        this.getLogger().info("     Thank you for using GG DonutShop ❤");
        this.getLogger().info("===========================================================");
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // /sell, /sellg, /sellgui all map to the registered "sell" command.
        boolean isSellCommand = "sell".equalsIgnoreCase(command.getName());

        if (!(sender instanceof Player player)) {
            if (!isSellCommand && args.length >= 1 && "reload".equalsIgnoreCase(args[0])) {
                this.reloadEverything();
                sender.sendMessage(ChatColor.GREEN + "[GG DonutShop] Successfully reloaded config.yml, shop.yml, and enchantment.yml!");
                return true;
            }
            sender.sendMessage(ChatColor.RED + "Only players can open GUI! Use '/shop reload' from console.");
            return true;
        }

        if (isSellCommand) {
            SellGUI.open(player);
            return true;
        }

        if (args.length >= 1 && "sell".equalsIgnoreCase(args[0])) {
            SellGUI.open(player);
            return true;
        }

        if (args.length >= 1 && "reload".equalsIgnoreCase(args[0])) {
            if (!player.hasPermission("ggdonutshop.admin") && !player.hasPermission("nxdonutshop.admin") && !player.isOp()) {
                player.sendMessage(ChatColor.RED + "You don't have permission to reload the shop!");
                return true;
            }
            this.reloadEverything();
            player.sendMessage(ChatColor.GREEN + "[GG DonutShop] Successfully reloaded config.yml, shop.yml, and enchantment.yml! (" + this.shopManager.getShopItems().size() + " items loaded)");
            return true;
        }

        if (args.length >= 1 && "randomize".equalsIgnoreCase(args[0])) {
            if (!player.hasPermission("ggdonutshop.admin") && !player.hasPermission("nxdonutshop.admin") && !player.isOp()) {
                player.sendMessage(ChatColor.RED + "You don't have permission to randomize shop prices!");
                return true;
            }
            if (this.settings != null && !this.settings.isFluctuationEnabled()) {
                player.sendMessage(ChatColor.YELLOW + "[GG DonutShop] Price fluctuation is disabled in config.yml (price-fluctuation.enabled: false).");
                return true;
            }
            this.shopManager.randomizePrices();
            player.sendMessage(ChatColor.GREEN + "[GG DonutShop] Manually triggered price randomizer!");
            return true;
        }

        if (args.length >= 2 && "pageselect".equalsIgnoreCase(args[0])) {
            try {
                int slot = Integer.parseInt(args[1]);
                String query = args.length >= 3 ? this.joinArgs(args, 2) : "";
                if (BedrockUtil.isBedrockPlayer(player)) {
                    BedrockShopGUI.open(player, slot, query, 1);
                } else {
                    PaperDialogue.openPageSelectDialogue(player, slot, query);
                }
                return true;
            } catch (Exception ignored) {
            }
        }

        if (args.length >= 3 && "pick".equalsIgnoreCase(args[0])) {
            try {
                Material mat = Material.matchMaterial(args[1]);
                int slot = Integer.parseInt(args[2]);
                if (mat != null && slot >= 0 && slot < 54) {
                    if (BedrockUtil.isBedrockPlayer(player)) {
                        BedrockShopGUI.open(player, slot, null, 1);
                    } else if (this.enchantmentManager.isEnchantable(mat)) {
                        PaperDialogue.openEnchantmentDialogue(player, mat, slot, new LinkedHashMap<>());
                    } else {
                        PaperDialogue.openAmountDialogue(player, mat, slot);
                    }
                    return true;
                }
            } catch (Exception ignored) {
            }
        }

        if (args.length >= 3 && "enchant".equalsIgnoreCase(args[0])) {
            try {
                Material mat = Material.matchMaterial(args[1]);
                int slot = Integer.parseInt(args[2]);
                String encoded = args.length >= 4 ? this.joinArgs(args, 3) : "";
                Map<Enchantment, Integer> enchants = EnchantmentManager.decodeEnchantments(encoded);
                PaperDialogue.openEnchantmentDialogue(player, mat, slot, enchants);
                return true;
            } catch (Exception ignored) {
            }
        }

        if (args.length >= 5 && "toggleench".equalsIgnoreCase(args[0])) {
            try {
                Material mat = Material.matchMaterial(args[1]);
                int slot = Integer.parseInt(args[2]);
                String enchName = args[3];
                int level = Integer.parseInt(args[4]);
                String encoded = args.length >= 6 ? this.joinArgs(args, 5) : "";
                Map<Enchantment, Integer> enchants = EnchantmentManager.decodeEnchantments(encoded);
                Enchantment targetEnch = EnchantmentManager.matchEnchantment(enchName);
                if (targetEnch != null) {
                    if (level <= 0) {
                        enchants.remove(targetEnch);
                    } else {
                        List<Enchantment> toRemove = new ArrayList<>();
                        for (Enchantment e : enchants.keySet()) {
                            if (e != targetEnch && (e.conflictsWith(targetEnch) || targetEnch.conflictsWith(e))) {
                                toRemove.add(e);
                            }
                        }
                        for (Enchantment e : toRemove) {
                            enchants.remove(e);
                        }
                        enchants.put(targetEnch, level);
                    }
                }
                PaperDialogue.openEnchantmentDialogue(player, mat, slot, enchants);
                return true;
            } catch (Exception ignored) {
            }
        }

        if (args.length >= 3 && "continueench".equalsIgnoreCase(args[0])) {
            try {
                Material mat = Material.matchMaterial(args[1]);
                int slot = Integer.parseInt(args[2]);
                String encoded = args.length >= 4 ? this.joinArgs(args, 3) : "";
                Map<Enchantment, Integer> enchants = EnchantmentManager.decodeEnchantments(encoded);
                PaperDialogue.openAmountDialogue(player, mat, slot, enchants);
                return true;
            } catch (Exception ignored) {
            }
        }

        if (args.length >= 4 && "confirm".equalsIgnoreCase(args[0])) {
            try {
                Material mat = Material.matchMaterial(args[1]);
                int slot = Integer.parseInt(args[2]);
                int amount = Math.max(1, Math.min(Integer.parseInt(args[3]), mat.getMaxStackSize()));
                String encoded = args.length >= 5 ? this.joinArgs(args, 4) : "";
                Map<Enchantment, Integer> enchants = EnchantmentManager.decodeEnchantments(encoded);
                if (mat != null && slot >= 0 && slot < 54) {
                    ItemStack displayItem = this.createQuickBuyItem(player, mat, amount, enchants);
                    this.setQuickBuySlotItem(player, slot, displayItem);
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8F, 1.5F);
                    this.openQuickBuyGUI(player);
                    return true;
                }
            } catch (Exception ignored) {
            }
        }

        if (args.length >= 3 && "page".equalsIgnoreCase(args[0])) {
            try {
                int page = Integer.parseInt(args[1]);
                int slot = Integer.parseInt(args[2]);
                String query = args.length >= 4 ? this.joinArgs(args, 3) : "";
                if (BedrockUtil.isBedrockPlayer(player)) {
                    BedrockShopGUI.open(player, slot, query, page);
                } else {
                    PaperDialogue.openItemDialogue(player, slot, query, page);
                }
                return true;
            } catch (Exception ignored) {
            }
        }

        if (args.length >= 2 && "search".equalsIgnoreCase(args[0])) {
            String query = args[1];
            int slot = 0;
            if (args.length >= 3) {
                try {
                    slot = Integer.parseInt(args[2]);
                } catch (Exception ignored) {
                }
            }
            if (BedrockUtil.isBedrockPlayer(player)) {
                BedrockShopGUI.open(player, slot, query, 1);
            } else {
                PaperDialogue.openItemDialogue(player, slot, query, 1);
            }
            return true;
        }

        if (args.length >= 2 && "select".equalsIgnoreCase(args[0])) {
            try {
                int slot = Integer.parseInt(args[1]);
                if (BedrockUtil.isBedrockPlayer(player)) {
                    BedrockShopGUI.open(player, slot, null, 1);
                } else {
                    PaperDialogue.openItemDialogue(player, slot, null, 1);
                }
                return true;
            } catch (Exception ignored) {
            }
        }

        this.openQuickBuyGUI(player);
        return true;
    }

    private String joinArgs(String[] args, int start) {
        if (args.length <= start) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = start; i < args.length; i++) {
            if (i > start) {
                sb.append(" ");
            }
            sb.append(args[i]);
        }
        return sb.toString();
    }

    public void openQuickBuyGUI(Player player) {
        QuickBuyHolder holder = new QuickBuyHolder();
        Component title = Component.text("Quick Buy").decoration(TextDecoration.ITALIC, false);
        Inventory gui = Bukkit.createInventory(holder, 54, title);
        holder.setInventory(gui);
        ItemStack emptyItem = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = emptyItem.getItemMeta();
        if (meta != null) {
            for (ItemFlag flag : ItemFlag.values()) {
                meta.addItemFlags(flag);
            }
            meta.displayName(Component.text("Empty Slot").color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Click to choose an item").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);
            emptyItem.setItemMeta(meta);
        }

        Map<Integer, ItemStack> savedItems = this.playerShopData.getOrDefault(player.getUniqueId(), new HashMap<>());
        for (int i = 0; i < 54; i++) {
            if (savedItems.containsKey(i)) {
                ItemStack saved = savedItems.get(i);
                ItemStack refreshed = this.createQuickBuyItem(player, saved.getType(), saved.getAmount(), saved.getEnchantments());
                gui.setItem(i, refreshed);
            } else {
                gui.setItem(i, emptyItem);
            }
        }

        player.openInventory(gui);
    }

    public ItemStack createQuickBuyItem(Player player, Material mat, int amount, Map<Enchantment, Integer> enchants) {
        ItemStack item = new ItemStack(mat, amount);
        if (enchants != null && !enchants.isEmpty()) {
            EnchantmentManager.applyEnchantments(item, enchants);
        }

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            double buyUnit = this.shopManager.getBuyPrice(mat);
            double sellUnit = this.shopManager.getSellPrice(mat);
            double enchantTotalUnit = this.enchantmentManager != null && enchants != null ? this.enchantmentManager.getTotalEnchantmentPrice(enchants) : 0.0;
            double unitTotalBuy = buyUnit + enchantTotalUnit;
            double totalBuy = unitTotalBuy * amount;
            boolean isBedrock = player != null && BedrockUtil.isBedrockPlayer(player);

            for (ItemFlag flag : ItemFlag.values()) {
                meta.addItemFlags(flag);
            }

            meta.displayName(Component.translatable(mat.translationKey()).color(NamedTextColor.WHITE)
                .decoration(TextDecoration.ITALIC, false)
                .decoration(TextDecoration.BOLD, false));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Sell Worth: ").color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)
                .append(Component.text("$ " + (int) sellUnit).color(NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false)));
            if (enchants != null && !enchants.isEmpty()) {
                lore.add(Component.empty());
                lore.add(Component.text("Enchantments:").color(NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
                for (Entry<Enchantment, Integer> e : enchants.entrySet()) {
                    lore.add(Component.text(" ▪ " + EnchantmentManager.formatEnchantmentName(e.getKey()) + " " + EnchantmentManager.toRoman(e.getValue()))
                        .color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
                }
            }

            lore.add(Component.empty());
            if (isBedrock) {
                lore.add(Component.text("▪ Tap / Move: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                    .append(Component.text("Buy " + amount + "x ").color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false))
                    .append(Component.text("($" + (int) totalBuy + ")").color(NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false)));
                lore.add(Component.empty());
                lore.add(Component.text("Right-Click / Drop to change item").color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
            } else {
                lore.add(Component.text("▪ Left-Click: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                    .append(Component.text("Buy " + amount + "x ").color(NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false))
                    .append(Component.text("($" + (int) totalBuy + ")").color(NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false)));
                lore.add(Component.empty());
                lore.add(Component.text("Shift-Click to change item").color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
            }

            meta.lore(lore);
            item.setItemMeta(meta);
        }

        return item;
    }

    public ItemStack createQuickBuyItem(Player player, Material mat, int amount) {
        return this.createQuickBuyItem(player, mat, amount, null);
    }

    public ItemStack createQuickBuyItem(Material mat, int amount) {
        return this.createQuickBuyItem(null, mat, amount, null);
    }

    public void openPaperDialogue(Player player, int slot) {
        if (BedrockUtil.isBedrockPlayer(player)) {
            BedrockShopGUI.open(player, slot, null, 1);
        } else {
            PaperDialogue.openItemDialogue(player, slot, null, 1);
        }
    }

    public Map<Integer, ItemStack> getPlayerShopData(UUID uuid) {
        return this.playerShopData.get(uuid);
    }

    public void setQuickBuySlotItem(Player player, int slot, ItemStack item) {
        Map<Integer, ItemStack> savedItems = this.playerShopData.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>());
        savedItems.put(slot, item.clone());
    }

    public void removeQuickBuySlotItem(Player player, int slot) {
        Map<Integer, ItemStack> savedItems = this.playerShopData.get(player.getUniqueId());
        if (savedItems != null) {
            savedItems.remove(slot);
        }
    }

    public static NxDonutShop getInstance() {
        return instance;
    }

    public Economy getEconomy() {
        return this.economy;
    }

    public ShopManager getShopManager() {
        return this.shopManager;
    }

    public EnchantmentManager getEnchantmentManager() {
        return this.enchantmentManager;
    }

    public ShopSettings getSettings() {
        return this.settings;
    }

    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if ("shop".equalsIgnoreCase(command.getName()) && args.length == 1) {
            List<String> completions = new ArrayList<>();
            if (sender.hasPermission("ggdonutshop.admin") || sender.hasPermission("nxdonutshop.admin") || sender.isOp()) {
                completions.add("reload");
                completions.add("randomize");
            }
            completions.add("sell");
            return completions.stream().filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
