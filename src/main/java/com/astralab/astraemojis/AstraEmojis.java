package com.astralab.astraemojis;

import com.astralab.astraemojis.api.*;
import com.astralab.astraemojis.command.EmojiCommand;
import com.astralab.astraemojis.emoji.EmojiManager;
import com.astralab.astraemojis.listener.SurfaceListener;
import com.astralab.astraemojis.pack.ResourcePackManager;
import com.astralab.astraemojis.packet.PacketSurfaceBridge;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public final class AstraEmojis extends JavaPlugin {
    private static AstraEmojis instance;
    private EmojiManager emojiManager;
    private ResourcePackManager packManager;
    private PacketSurfaceBridge packetBridge;
    private SurfaceListener surfaceListener;

    @Override public void onEnable() {
        instance = this;
        saveDefaultConfig();
        saveIfMissing("emojis.yml");
        emojiManager = new EmojiManager(this);
        packManager = new ResourcePackManager(this, emojiManager);
        try {
            packManager.synchronizePngMappings();
            emojiManager.reload();
            if (getConfig().getBoolean("resource-pack.generate-on-startup", true)) packManager.build();
            packManager.startServer();
        } catch (IOException error) {
            getLogger().severe("Resource pack setup failed: " + error.getMessage());
        }

        surfaceListener = new SurfaceListener(this, emojiManager, packManager);
        getServer().getPluginManager().registerEvents(surfaceListener, this);
        getServer().getServicesManager().register(AstraEmojisService.class,
            new DefaultAstraEmojisService(emojiManager), this, ServicePriority.Normal);

        PluginCommand command = getCommand("emoji");
        if (command != null) {
            EmojiCommand executor = new EmojiCommand(this);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }
        registerPackets();
        getLogger().info("Loaded " + emojiManager.all().size() + " emojis.");
    }

    @Override public void onDisable() {
        if (packetBridge != null) packetBridge.close();
        if (packManager != null) packManager.close();
        getServer().getServicesManager().unregisterAll(this);
        instance = null;
    }

    public void reloadPlugin() {
        reloadConfig();
        if (packetBridge != null) packetBridge.close();
        try {
            packManager.synchronizePngMappings();
            emojiManager.reload();
            packManager.build();
            packManager.startServer();
        } catch (IOException error) {
            getLogger().severe("Reload/resource pack generation failed: " + error.getMessage());
        }
        registerPackets();
    }

    private void registerPackets() {
        if (!getConfig().getBoolean("packets.enabled", true)) return;
        if (!Bukkit.getPluginManager().isPluginEnabled("ProtocolLib")) {
            getLogger().warning("ProtocolLib is absent; packet-level tab/scoreboard/hologram/dialog interception is disabled.");
            return;
        }
        packetBridge = new PacketSurfaceBridge(this, emojiManager, surfaceListener);
        packetBridge.register();
    }

    private void saveIfMissing(String path) {
        if (!new File(getDataFolder(), path).isFile()) saveResource(path, false);
    }

    public static AstraEmojis instance() { return instance; }
    public EmojiManager emojiManager() { return emojiManager; }
    public ResourcePackManager packManager() { return packManager; }
}
