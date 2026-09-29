package com.astralab.astraemojis.pack;

import com.astralab.astraemojis.emoji.Emoji;
import com.astralab.astraemojis.emoji.EmojiManager;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class ResourcePackManager implements AutoCloseable {
    private final JavaPlugin plugin;
    private final EmojiManager emojis;
    private final File images;
    private final File pack;
    private HttpServer server;
    private byte[] sha1 = new byte[20];

    public ResourcePackManager(JavaPlugin plugin, EmojiManager emojis) {
        this.plugin = plugin;
        this.emojis = emojis;
        this.images = new File(plugin.getDataFolder(), "emojis");
        this.pack = new File(plugin.getDataFolder(), "generated/AstraEmojis-pack.zip");
    }

    /** Add YAML definitions for previously unseen PNG names, retaining stable code points. */
    public void synchronizePngMappings() throws IOException {
        images.mkdirs();
        File yamlFile = new File(plugin.getDataFolder(), "emojis.yml");
        org.bukkit.configuration.file.YamlConfiguration yaml = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(yamlFile);
        org.bukkit.configuration.ConfigurationSection root = yaml.getConfigurationSection("emojis");
        if (root == null) root = yaml.createSection("emojis");
        Set<Integer> used = new HashSet<>();
        for (String key : root.getKeys(false)) {
            String value = root.getString(key + ".char", "");
            if (value.matches("(?i)U\\+[0-9a-f]{1,6}")) used.add(Integer.parseInt(value.substring(2), 16));
            else if (!value.isEmpty()) value.codePoints().forEach(used::add);
        }
        int next = 0xE000;
        File[] pngs = images.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".png"));
        if (pngs == null) return;
        Arrays.sort(pngs, Comparator.comparing(File::getName));
        for (File png : pngs) {
            String name = png.getName().substring(0, png.getName().length() - 4).toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_-]", "_");
            if (root.contains(name)) continue;
            while (used.contains(next) && next <= 0xF8FF) next++;
            if (next > 0xF8FF) throw new IOException("BMP private-use character range exhausted");
            root.set(name + ".char", String.format("U+%04X", next));
            root.set(name + ".permission", "astraemojis.use." + name);
            used.add(next++);
        }
        yaml.save(yamlFile);
    }

    public synchronized void build() throws IOException {
        int format = plugin.getConfig().getInt("resource-pack.pack-format", 63);
        int ascent = plugin.getConfig().getInt("resource-pack.ascent", 8);
        int height = plugin.getConfig().getInt("resource-pack.height", 9);
        String namespace = plugin.getConfig().getString("resource-pack.namespace", "astraemojis");
        Path staging = Files.createTempDirectory(plugin.getDataFolder().toPath(), "pack-");
        try {
            Files.writeString(staging.resolve("pack.mcmeta"), "{\"pack\":{\"pack_format\":" + format +
                ",\"description\":\"AstraEmojis generated resource pack\"}}", StandardCharsets.UTF_8);
            Path fontDir = staging.resolve("assets/minecraft/font");
            Path textureDir = staging.resolve("assets/" + namespace + "/textures/emoji");
            Files.createDirectories(fontDir);
            Files.createDirectories(textureDir);
            List<String> providers = new ArrayList<>();
            providers.add("{\"type\":\"reference\",\"id\":\"minecraft:include/default\"}");
            for (Emoji emoji : emojis.all()) {
                Path source = images.toPath().resolve(emoji.name() + ".png");
                String texture = emoji.texture();
                if (Files.isRegularFile(source)) {
                    Path target = textureDir.resolve(emoji.name() + ".png");
                    Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                    texture = namespace + ":emoji/" + emoji.name();
                }
                if (texture == null || texture.isBlank()) continue;
                providers.add("{\"type\":\"bitmap\",\"file\":\"" + texture +
                    ".png\",\"ascent\":" + ascent + ",\"height\":" + height + ",\"chars\":[\"" +
                    jsonEscape(emoji.glyph()) + "\"]}");
            }
            Files.writeString(fontDir.resolve("default.json"), "{\"providers\":[" + String.join(",", providers) + "]}");
            pack.getParentFile().mkdirs();
            try (ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(pack)))) {
                try (var paths = Files.walk(staging)) {
                    for (Path path : paths.filter(Files::isRegularFile).toList()) {
                        zip.putNextEntry(new ZipEntry(staging.relativize(path).toString().replace(File.separatorChar, '/')));
                        Files.copy(path, zip);
                        zip.closeEntry();
                    }
                }
            }
            sha1 = MessageDigest.getInstance("SHA-1").digest(Files.readAllBytes(pack.toPath()));
            plugin.getLogger().info("Generated " + pack.getPath() + " with " + (providers.size() - 1) + " texture(s).");
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IOException(impossible);
        } finally {
            deleteTree(staging);
        }
    }

    public synchronized void startServer() throws IOException {
        close();
        if (!plugin.getConfig().getBoolean("resource-pack.embedded-server.enabled", true)) return;
        String bind = plugin.getConfig().getString("resource-pack.embedded-server.bind", "0.0.0.0");
        int port = plugin.getConfig().getInt("resource-pack.embedded-server.port", 8127);
        server = HttpServer.create(new InetSocketAddress(bind, port), 0);
        server.createContext("/AstraEmojis-pack.zip", this::servePack);
        server.setExecutor(Executors.newCachedThreadPool(r -> {
            Thread thread = new Thread(r, "AstraEmojis-PackServer");
            thread.setDaemon(true);
            return thread;
        }));
        server.start();
    }

    public void send(Player player) {
        if (!pack.isFile()) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>The emoji pack has not been generated.</red>"));
            return;
        }
        String configured = plugin.getConfig().getString("resource-pack.public-url", "").trim();
        String url = configured;
        if (url.isBlank()) {
            String host = Bukkit.getIp();
            if (host == null || host.isBlank() || host.equals("0.0.0.0")) host = "localhost";
            url = "http://" + host + ":" + plugin.getConfig().getInt("resource-pack.embedded-server.port", 8127) + "/AstraEmojis-pack.zip";
            plugin.getLogger().warning("No resource-pack.public-url configured; inferred URL " + url + " may not be reachable by remote players.");
        }
        String prompt = plugin.getConfig().getString("resource-pack.prompt", "Emoji resource pack");
        player.setResourcePack(url, sha1, MiniMessage.miniMessage().deserialize(prompt),
            plugin.getConfig().getBoolean("resource-pack.required", false));
    }

    private void servePack(HttpExchange exchange) throws IOException {
        if (!pack.isFile() || !exchange.getRequestMethod().equals("GET")) {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
            return;
        }
        exchange.getResponseHeaders().set("Content-Type", "application/zip");
        exchange.getResponseHeaders().set("Cache-Control", "no-cache");
        exchange.sendResponseHeaders(200, pack.length());
        try (OutputStream out = exchange.getResponseBody()) { Files.copy(pack.toPath(), out); }
    }

    public File packFile() { return pack; }
    @Override public synchronized void close() { if (server != null) { server.stop(0); server = null; } }

    private static String jsonEscape(String text) {
        StringBuilder out = new StringBuilder();
        text.codePoints().forEach(cp -> {
            if (cp <= 0xFFFF) out.append(String.format("\\u%04x", cp));
            else {
                char[] pair = Character.toChars(cp);
                out.append(String.format("\\u%04x\\u%04x", (int) pair[0], (int) pair[1]));
            }
        });
        return out.toString();
    }

    private static void deleteTree(Path path) {
        try (var stream = Files.walk(path)) {
            stream.sorted(Comparator.reverseOrder()).forEach(p -> { try { Files.deleteIfExists(p); } catch (IOException ignored) { } });
        } catch (IOException ignored) { }
    }
}
