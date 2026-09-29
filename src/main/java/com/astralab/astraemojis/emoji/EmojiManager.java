package com.astralab.astraemojis.emoji;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The one parsing/mapping service used by listeners, packets, commands and the public API.
 * Parsing is deliberately context aware: fenced/inline code and URL tokens are copied verbatim.
 */
public final class EmojiManager {
    private static final Pattern TOKEN = Pattern.compile("(?is)(?<![\\w])((?:https?://|www\\.)\\S+)|`{1,3}[^`]*`{1,3}|(?<!\\\\):([a-z0-9_\\-]+):");
    private static final Pattern ESCAPED = Pattern.compile("\\\\(:[a-zA-Z0-9_-]+:)");
    private final JavaPlugin plugin;
    private volatile Map<String, Emoji> emojis = Map.of();
    private volatile Key font = Key.key("minecraft", "default");

    public EmojiManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "emojis.yml");
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        font = Key.key(plugin.getConfig().getString("resource-pack.font", "minecraft:default"));
        ConfigurationSection root = yaml.getConfigurationSection("emojis");
        Map<String, Emoji> loaded = new TreeMap<>();
        if (root != null) {
            for (String rawName : root.getKeys(false)) {
                String name = normalize(rawName);
                String raw = root.getString(rawName + ".char", "");
                String glyph = decodeGlyph(raw);
                String spritePath = root.getString(rawName + ".sprite",
                    root.getString(rawName + ".texture", ""));
                if (spritePath.isBlank()) spritePath = materialSprite(name);
                if (name.isEmpty() || (glyph.isEmpty() && spritePath.isBlank())) {
                    plugin.getLogger().warning("Ignoring emoji without a char or sprite: " + rawName);
                    continue;
                }
                Key sprite = spritePath.isBlank() ? null : Key.key(spritePath);
                String atlasPath = root.getString(rawName + ".atlas", inferAtlas(spritePath));
                loaded.put(name, new Emoji(name, glyph,
                    root.getString(rawName + ".permission", ""), font,
                    sprite == null ? null : Key.key(atlasPath), sprite));
            }
        }
        if (plugin.getConfig().getBoolean("auto-material-emojis", true)) addMaterialEmojis(loaded);
        emojis = Collections.unmodifiableMap(loaded);
    }

    /** Parse without a permission subject, intended for trusted/plugin-authored content. */
    public Component replace(String input) {
        return replace(input, ignored -> true);
    }

    /** Parse all textual leaves while preserving the original component tree and styles. */
    public Component replace(Component input) {
        return replace(input, ignored -> true);
    }

    public Component replace(String input, @Nullable Player player) {
        return replace(input, emoji -> allowed(player, emoji));
    }

    public Component replace(Component input, @Nullable Player player) {
        return replace(input, emoji -> allowed(player, emoji));
    }

    private Component replace(Component input, Predicate<Emoji> allowed) {
        List<Component> originalChildren = input.children();
        if (input instanceof TextComponent text) {
            Component parsed = replace(text.content(), allowed).style(input.style());
            if (originalChildren.isEmpty()) return parsed;
            List<Component> combined = new ArrayList<>(parsed.children());
            for (Component child : originalChildren) combined.add(replace(child, allowed));
            return parsed.children(combined);
        }
        if (originalChildren.isEmpty()) return input;
        List<Component> mapped = new ArrayList<>(originalChildren.size());
        for (Component child : originalChildren) mapped.add(replace(child, allowed));
        return input.children(mapped);
    }

    private Component replace(String input, Predicate<Emoji> allowed) {
        if (input == null || input.isEmpty()) return Component.empty();
        TextComponent.Builder out = Component.text();
        Matcher matcher = TOKEN.matcher(input);
        int end = 0;
        while (matcher.find()) {
            if (matcher.start() > end) out.append(Component.text(unescape(input.substring(end, matcher.start()))));
            String name = matcher.group(2);
            if (name == null) {
                out.append(Component.text(matcher.group())); // URL or code span
            } else {
                Emoji emoji = emojis.get(normalize(name));
                out.append(emoji != null && allowed.test(emoji) ? emoji.component() : Component.text(matcher.group()));
            }
            end = matcher.end();
        }
        if (end < input.length()) out.append(Component.text(unescape(input.substring(end))));
        return out.build();
    }

    /** Fallback for protocol fields that are still raw strings; sprite objects require component fields. */
    public String replaceSerialized(String json, @Nullable Player viewer) {
        if (json == null || json.indexOf(':') < 0) return json;
        Matcher matcher = Pattern.compile("(?<!\\\\\\\\):([a-zA-Z0-9_-]+):").matcher(json);
        StringBuffer out = new StringBuffer();
        while (matcher.find()) {
            Emoji emoji = emojis.get(normalize(matcher.group(1)));
            if (emoji != null && !emoji.isSprite() && allowed(viewer, emoji))
                matcher.appendReplacement(out, Matcher.quoteReplacement(emoji.glyph()));
            else matcher.appendReplacement(out, Matcher.quoteReplacement(matcher.group()));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    private void addMaterialEmojis(Map<String, Emoji> loaded) {
        List<Material> materials = Arrays.stream(Material.values())
            .filter(m -> !m.isLegacy() && (m.isItem() || m.isBlock()))
            .sorted(Comparator.comparing(Material::name)).toList();
        for (Material material : materials) {
            String name = material.name().toLowerCase(Locale.ROOT);
            if (loaded.containsKey(name)) continue;
            String spritePath = materialSprite(name);
            loaded.put(name, new Emoji(name, "", "astraemojis.use." + name, font,
                Key.key(inferAtlas(spritePath)), Key.key(spritePath)));
        }
    }

    private static String materialSprite(String name) {
        Material material = Material.matchMaterial(name);
        if (material == null || material.isLegacy()) return "";
        return "minecraft:" + (material.isBlock() ? "block/" : "item/") + name;
    }

    private static String inferAtlas(String spritePath) {
        return spritePath != null && spritePath.contains(":block/") ? "minecraft:blocks" : "minecraft:items";
    }

    public Optional<Emoji> getEmoji(String name) {
        return Optional.ofNullable(emojis.get(normalize(name)));
    }

    public Collection<Emoji> all() {
        return emojis.values();
    }

    public Set<String> names() {
        return emojis.keySet();
    }

    private boolean allowed(@Nullable Player player, Emoji emoji) {
        if (player == null) return true;
        if (!player.hasPermission("astraemojis.use")) return false;
        return emoji.permission().isBlank() || player.hasPermission(emoji.permission())
            || player.hasPermission("astraemojis.use." + emoji.name())
            || player.hasPermission("astraemojis.use.*");
    }

    private static String normalize(String name) {
        return name == null ? "" : name.toLowerCase(Locale.ROOT).replace(' ', '_').replace(':', ' ').trim();
    }

    private static String decodeGlyph(String raw) {
        if (raw == null) return "";
        String value = raw.trim();
        try {
            if (value.matches("(?i)U\\+[0-9a-f]{1,6}"))
                return Character.toString(Integer.parseInt(value.substring(2), 16));
            if (value.matches("(?i)0x[0-9a-f]{1,6}"))
                return Character.toString(Integer.parseInt(value.substring(2), 16));
        } catch (IllegalArgumentException ignored) { }
        return value;
    }

    private static String unescape(String value) {
        return ESCAPED.matcher(value).replaceAll("$1");
    }
}
