package com.astralab.astraemojis.command;

import com.astralab.astraemojis.AstraEmojis;
import com.astralab.astraemojis.emoji.Emoji;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public final class EmojiCommand implements TabExecutor {
    private final AstraEmojis plugin;
    public EmojiCommand(AstraEmojis plugin) { this.plugin = plugin; }

    @Override public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                                       @NotNull String label, @NotNull String[] args) {
        String sub = args.length == 0 ? "list" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "list" -> list(sender, args.length > 1 ? parsePage(args[1]) : 1);
            case "reload" -> {
                if (!admin(sender)) return true;
                plugin.reloadPlugin();
                sender.sendMessage(Component.text("AstraEmojis reloaded.", NamedTextColor.GREEN));
            }
            case "pack" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(Component.text("This command must be used by a player.", NamedTextColor.RED));
                } else plugin.packManager().send(player);
            }
            case "give" -> {
                if (!admin(sender)) return true;
                if (!(sender instanceof Player player) || args.length < 2) {
                    sender.sendMessage(Component.text("Usage: /emoji give <name>", NamedTextColor.RED));
                    return true;
                }
                Optional<Emoji> found = plugin.emojiManager().getEmoji(args[1]);
                if (found.isEmpty()) {
                    sender.sendMessage(Component.text("Unknown emoji: " + args[1], NamedTextColor.RED));
                    return true;
                }
                Emoji emoji = found.get();
                ItemStack token = new ItemStack(Material.PAPER);
                ItemMeta meta = token.getItemMeta();
                meta.displayName(Component.text("Emoji: ").append(emoji.component()).append(Component.text(" " + emoji.shortcode())));
                meta.lore(List.of(Component.text("Copy this code into chat:", NamedTextColor.GRAY),
                    Component.text(emoji.shortcode(), NamedTextColor.YELLOW)
                        .clickEvent(ClickEvent.copyToClipboard(emoji.shortcode()))));
                token.setItemMeta(meta);
                player.getInventory().addItem(token).values().forEach(left -> player.getWorld().dropItemNaturally(player.getLocation(), left));
            }
            default -> sender.sendMessage(Component.text("Usage: /emoji <list|reload|give|pack>", NamedTextColor.YELLOW));
        }
        return true;
    }

    private void list(CommandSender sender, int requestedPage) {
        List<Emoji> available = plugin.emojiManager().all().stream().filter(emoji ->
            !(sender instanceof Player p) || p.hasPermission("astraemojis.use.*")
                || emoji.permission().isBlank() || p.hasPermission(emoji.permission())).toList();
        if (available.isEmpty()) {
            sender.sendMessage(Component.text("No emojis are available to you.", NamedTextColor.GRAY));
            return;
        }
        int pageSize = 25;
        int pages = Math.max(1, (available.size() + pageSize - 1) / pageSize);
        int page = Math.max(1, Math.min(requestedPage, pages));
        Component line = Component.text("Available emojis (" + page + "/" + pages + "): ", NamedTextColor.GOLD);
        for (Emoji emoji : available.subList((page - 1) * pageSize, Math.min(page * pageSize, available.size()))) {
            Component entry = Component.text(" " + emoji.shortcode(), NamedTextColor.YELLOW)
                .hoverEvent(HoverEvent.showText(Component.text("Click to copy")))
                .clickEvent(ClickEvent.copyToClipboard(emoji.shortcode()));
            line = line.append(emoji.component()).append(entry);
        }
        sender.sendMessage(line);
    }

    private static int parsePage(String value) {
        try { return Integer.parseInt(value); }
        catch (NumberFormatException ignored) { return 1; }
    }

    private boolean admin(CommandSender sender) {
        if (sender.hasPermission("astraemojis.admin")) return true;
        sender.sendMessage(Component.text("You do not have permission.", NamedTextColor.RED));
        return false;
    }

    @Override public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                 @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) return match(args[0], sender.hasPermission("astraemojis.admin")
            ? List.of("list", "reload", "give", "pack") : List.of("list", "pack"));
        if (args.length == 2 && args[0].equalsIgnoreCase("give")) return match(args[1], plugin.emojiManager().names());
        return List.of();
    }

    private static List<String> match(String prefix, Collection<String> choices) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        return choices.stream().filter(s -> s.startsWith(lower)).sorted().toList();
    }
}
