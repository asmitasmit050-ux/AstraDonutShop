package com.astralab.astraemojis.api;

import com.astralab.astraemojis.emoji.Emoji;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.Optional;

/** Bukkit ServicesManager contract, useful when consumers do not compile against the implementation. */
public interface AstraEmojisService {
    Component parse(String input);
    Component parse(Component input);
    Component parse(String input, Player player);
    Optional<Emoji> getEmoji(String name);
}
