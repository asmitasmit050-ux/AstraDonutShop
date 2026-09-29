package com.astralab.astraemojis.emoji;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;

/** Immutable emoji definition. */
public record Emoji(String name, String glyph, String permission, Key font, String texture) {
    public String shortcode() {
        return ":" + name + ":";
    }

    public Component component() {
        return Component.text(glyph).font(font);
    }
}
