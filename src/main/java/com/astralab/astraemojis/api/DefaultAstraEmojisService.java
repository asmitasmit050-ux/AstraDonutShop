package com.astralab.astraemojis.api;

import com.astralab.astraemojis.emoji.Emoji;
import com.astralab.astraemojis.emoji.EmojiManager;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.Optional;

public final class DefaultAstraEmojisService implements AstraEmojisService {
    private final EmojiManager manager;
    public DefaultAstraEmojisService(EmojiManager manager) { this.manager = manager; }
    public Component parse(String input) { return manager.replace(input); }
    public Component parse(Component input) { return manager.replace(input); }
    public Component parse(String input, Player player) { return manager.replace(input, player); }
    public Optional<Emoji> getEmoji(String name) { return manager.getEmoji(name); }
}
