package com.astralab.astraemojis.emoji;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.object.ObjectContents;

/** Immutable emoji definition. Sprite-backed entries need no resource pack. */
public record Emoji(String name, String glyph, String permission, Key font, Key atlas, Key sprite) {
    public String shortcode() {
        return ":" + name + ":";
    }

    public boolean isSprite() {
        return sprite != null;
    }

    public Component component() {
        return isSprite()
            ? Component.object(ObjectContents.sprite(atlas, sprite))
            : Component.text(glyph).font(font);
    }
}
