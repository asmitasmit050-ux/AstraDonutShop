package com.astralab.astraemojis.api;

import com.astralab.astraemojis.AstraEmojis;
import com.astralab.astraemojis.emoji.Emoji;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.Optional;

/** Stable static facade for plugins that do not want to use Bukkit's service manager. */
public final class AstraEmojisAPI {
    private AstraEmojisAPI() { }

    public static Component parse(String text) {
        return instance().emojiManager().replace(text);
    }

    public static Component parse(Component component) {
        return instance().emojiManager().replace(component);
    }

    public static Component parse(String text, Player permissionSubject) {
        return instance().emojiManager().replace(text, permissionSubject);
    }

    public static Optional<Emoji> getEmoji(String name) {
        return instance().emojiManager().getEmoji(name);
    }

    private static AstraEmojis instance() {
        AstraEmojis plugin = AstraEmojis.instance();
        if (plugin == null || !plugin.isEnabled()) throw new IllegalStateException("AstraEmojis is not enabled");
        return plugin;
    }
}
