package com.astralab.astraemojis.packet;

import com.astralab.astraemojis.emoji.EmojiManager;
import com.astralab.astraemojis.listener.SurfaceListener;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.*;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * Version-tolerant ProtocolLib output bridge. Packet constants are discovered by name so one jar
 * can run across the 1.21 protocol additions (including DIALOG packets when ProtocolLib exposes them).
 */
public final class PacketSurfaceBridge implements AutoCloseable {
    private final JavaPlugin plugin;
    private final EmojiManager emojis;
    private final SurfaceListener surfaces;
    private PacketListener listener;

    public PacketSurfaceBridge(JavaPlugin plugin, EmojiManager emojis, SurfaceListener surfaces) {
        this.plugin = plugin;
        this.emojis = emojis;
        this.surfaces = surfaces;
    }

    public void register() {
        Set<String> names = new HashSet<>();
        if (on("tab")) Collections.addAll(names, "PLAYER_LIST_HEADER_FOOTER", "PLAYER_INFO");
        if (on("scoreboard")) Collections.addAll(names, "SCOREBOARD_OBJECTIVE", "SCOREBOARD_TEAM", "SCOREBOARD_SCORE", "SCOREBOARD_DISPLAY_OBJECTIVE", "RESET_SCORE");
        if (on("holograms")) Collections.addAll(names, "ENTITY_METADATA", "SPAWN_ENTITY");
        if (on("menus")) Collections.addAll(names, "OPEN_WINDOW", "SET_SLOT", "WINDOW_ITEMS");
        if (on("dialogs")) Collections.addAll(names, "DIALOG", "SHOW_DIALOG");
        if (on("boss-bars")) names.add("BOSS");
        List<PacketType> types = findPacketTypes(names);
        if (types.isEmpty()) {
            plugin.getLogger().warning("ProtocolLib is installed but no compatible output packet types were found.");
            return;
        }
        listener = new PacketAdapter(plugin, ListenerPriority.NORMAL, types.toArray(PacketType[]::new)) {
            @Override public void onPacketSending(PacketEvent event) { transform(event); }
        };
        ProtocolLibrary.getProtocolManager().addPacketListener(listener);
        plugin.getLogger().info("ProtocolLib bridge enabled for " + types.size() + " packet types.");
    }

    private void transform(PacketEvent event) {
        Player viewer = event.getPlayer();
        PacketContainer packet = event.getPacket();
        try {
            var chats = packet.getChatComponents();
            for (int i = 0; i < chats.size(); i++) {
                WrappedChatComponent value = chats.readSafely(i);
                if (value != null) chats.writeSafely(i,
                    WrappedChatComponent.fromJson(emojis.replaceSerialized(value.getJson(), viewer)));
            }
            var strings = packet.getStrings();
            for (int i = 0; i < strings.size(); i++) {
                String value = strings.readSafely(i);
                if (value != null) strings.writeSafely(i, emojis.replaceSerialized(value, viewer));
            }
            if (on("items") || on("menus")) {
                var itemModifier = packet.getItemModifier();
                for (int i = 0; i < itemModifier.size(); i++) {
                    ItemStack item = itemModifier.readSafely(i);
                    if (item != null) {
                        item = item.clone();
                        surfaces.transformItem(item, viewer);
                        itemModifier.writeSafely(i, item);
                    }
                }
                var arrays = packet.getItemArrayModifier();
                for (int i = 0; i < arrays.size(); i++) {
                    ItemStack[] values = arrays.readSafely(i);
                    if (values == null) continue;
                    values = values.clone();
                    for (int j = 0; j < values.length; j++) if (values[j] != null) {
                        values[j] = values[j].clone();
                        surfaces.transformItem(values[j], viewer);
                    }
                    arrays.writeSafely(i, values);
                }
                var lists = packet.getItemListModifier();
                for (int i = 0; i < lists.size(); i++) {
                    List<ItemStack> original = lists.readSafely(i);
                    if (original == null) continue;
                    List<ItemStack> changed = new ArrayList<>(original.size());
                    for (ItemStack item : original) {
                        if (item == null) changed.add(null);
                        else {
                            ItemStack copy = item.clone();
                            surfaces.transformItem(copy, viewer);
                            changed.add(copy);
                        }
                    }
                    lists.writeSafely(i, changed);
                }
            }
        } catch (Throwable error) {
            // A packet field layout can change between ProtocolLib snapshots; never cancel the packet.
            if (plugin.getConfig().getBoolean("debug", false))
                plugin.getLogger().warning("Could not inspect " + event.getPacketType() + ": " + error.getMessage());
        }
    }

    private static List<PacketType> findPacketTypes(Set<String> wanted) {
        List<PacketType> result = new ArrayList<>();
        for (Field field : PacketType.Play.Server.class.getFields()) {
            if (!Modifier.isStatic(field.getModifiers()) || !wanted.contains(field.getName())) continue;
            try {
                Object value = field.get(null);
                if (value instanceof PacketType type && type.isSupported()) result.add(type);
            } catch (ReflectiveOperationException ignored) { }
        }
        return result;
    }

    private boolean on(String surface) {
        return plugin.getConfig().getBoolean("surfaces." + surface, true);
    }

    @Override public void close() {
        if (listener != null) ProtocolLibrary.getProtocolManager().removePacketListener(listener);
        listener = null;
    }
}
