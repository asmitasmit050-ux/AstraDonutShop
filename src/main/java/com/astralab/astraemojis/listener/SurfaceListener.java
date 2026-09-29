package com.astralab.astraemojis.listener;

import com.astralab.astraemojis.emoji.EmojiManager;
import com.astralab.astraemojis.pack.ResourcePackManager;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

/** Bukkit/Paper event coverage. PacketSurfaceBridge complements this for other plugins' outgoing UI. */
public final class SurfaceListener implements Listener {
    private final JavaPlugin plugin;
    private final EmojiManager emojis;
    private final ResourcePackManager packs;

    public SurfaceListener(JavaPlugin plugin, EmojiManager emojis, ResourcePackManager packs) {
        this.plugin = plugin;
        this.emojis = emojis;
        this.packs = packs;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        if (!enabled("chat")) return;
        event.message(emojis.replace(event.message(), event.getPlayer()));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSign(SignChangeEvent event) {
        if (!enabled("signs")) return;
        Player player = event.getPlayer();
        for (int i = 0; i < 4; i++) event.line(i, emojis.replace(event.line(i), player));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBook(PlayerEditBookEvent event) {
        if (!enabled("books")) return;
        BookMeta meta = event.getNewBookMeta();
        List<Component> pages = new ArrayList<>();
        for (Component page : meta.pages()) pages.add(emojis.replace(page, event.getPlayer()));
        meta.pages(pages);
        if (meta.hasTitle()) meta.title(emojis.replace(meta.title(), event.getPlayer()));
        event.setNewBookMeta(meta);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onAnvil(PrepareAnvilEvent event) {
        if (!enabled("items")) return;
        ItemStack result = event.getResult();
        if (result == null || result.getType() == Material.AIR) return;
        ItemStack edited = result.clone();
        transformItem(edited, event.getView().getPlayer() instanceof Player p ? p : null);
        event.setResult(edited);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (plugin.getConfig().getBoolean("resource-pack.enabled", false)
            && plugin.getConfig().getBoolean("resource-pack.send-on-join", false)) packs.send(event.getPlayer());
    }

    public void transformItem(ItemStack item, Player viewer) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        if (meta.hasDisplayName()) meta.displayName(emojis.replace(meta.displayName(), viewer));
        if (meta.hasLore() && meta.lore() != null) {
            List<Component> lore = new ArrayList<>();
            for (Component line : meta.lore()) lore.add(emojis.replace(line, viewer));
            meta.lore(lore);
        }
        item.setItemMeta(meta);
    }

    private boolean enabled(String name) {
        return plugin.getConfig().getBoolean("surfaces." + name, true);
    }
}
