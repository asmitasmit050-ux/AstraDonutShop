package net.notdakuxd.nxdonutshop.util;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TextComponent.Builder;
import net.kyori.adventure.text.format.TextDecoration;

public class SpriteComponentReplacer {
   private static final Map<String, String> SHORTCODES = new HashMap();
   private static final Pattern PATTERN = Pattern.compile("<sprite:([^>]+)>|:[a-zA-Z0-9_]+:");

   public static Component replaceShortcodes(String input) {
      if (input != null && !input.isEmpty()) {
         Builder builder = Component.text();
         Matcher matcher = PATTERN.matcher(input);

         int lastEnd;
         for (lastEnd = 0; matcher.find(); lastEnd = matcher.end()) {
            if (matcher.start() > lastEnd) {
               builder.append(Component.text(input.substring(lastEnd, matcher.start())));
            }

            String match = matcher.group();
            if (SHORTCODES.containsKey(match)) {
               String unicodeGlyph = (String)SHORTCODES.get(match);
               Component sprite = ((TextComponent)Component.text(unicodeGlyph).font(Key.key("minecraft", "default"))).decoration(TextDecoration.ITALIC, false);
               builder.append(sprite);
            } else if (match.startsWith("<sprite:")) {
               String spritePath = matcher.group(1);
               Component sprite = ((TextComponent)Component.text("\ue001").font(Key.key("minecraft", spritePath))).decoration(TextDecoration.ITALIC, false);
               builder.append(sprite);
            } else {
               builder.append(Component.text(match));
            }
         }

         if (lastEnd < input.length()) {
            builder.append(Component.text(input.substring(lastEnd)));
         }

         return builder.build();
      } else {
         return Component.empty();
      }
   }

   static {
      SHORTCODES.put(":yt:", "\ue001");
      SHORTCODES.put(":india:", "\ue002");
      SHORTCODES.put(":star:", "\ue003");
      SHORTCODES.put(":check:", "\ue004");
      SHORTCODES.put(":heart:", "\ue005");
      SHORTCODES.put(":donut:", "\ue006");
   }
}
