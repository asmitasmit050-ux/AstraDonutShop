package net.notdakuxd.nxdonutshop.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/**
 * Color translation helper with Hex Code support.
 *
 * Supported formats in any configurable text (announcements, GUI titles, lore lines, ...):
 *   - Legacy color/format codes: &a &b &c ... &f, &l &o &n &m &k &r
 *   - Hex colors: <&#RRGGBB> e.g. &#FFAA00
 *   - Bungee/legacy hex: &x&F&F&A&A&0&0
 *   - Raw section signs (§) are passed through untouched.
 */
public final class ColorUtil {
    private static final char COLOR_CHAR = '§';

    /** Matches &#RRGGBB */
    private static final Pattern HEX_AMP_PATTERN = Pattern.compile("&#([0-9a-fA-F]{6})");
    /** Matches &x&F&F&A&A&0&0 */
    private static final Pattern HEX_BUNGEE_PATTERN = Pattern.compile("&x(&[0-9a-fA-F]){6}");
    /** Matches §-color/format codes (used for stripping). */
    private static final Pattern STRIP_PATTERN = Pattern.compile("(?i)§([0-9a-fk-orx])");

    private ColorUtil() {
    }

    /**
     * Translates &-codes and hex codes into legacy § sequences. The returned string keeps
     * hex colors in the §x§R§R§G§G§B§B form which modern clients render correctly.
     */
    public static String colorize(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }

        // &#RRGGBB -> §x§R§R§G§G§B§B
        Matcher hexMatcher = HEX_AMP_PATTERN.matcher(input);
        StringBuilder sb = new StringBuilder(input.length() + 16);
        while (hexMatcher.find()) {
            String hex = hexMatcher.group(1);
            StringBuilder repl = new StringBuilder("§x");
            for (char c : hex.toCharArray()) {
                repl.append('§').append(c);
            }
            hexMatcher.appendReplacement(sb, Matcher.quoteReplacement(repl.toString()));
        }
        hexMatcher.appendTail(sb);

        // &x&F&F&A&A&0&0 -> §x§F§F§A§A§0§0
        String out = sb.toString();
        Matcher bungeeMatcher = HEX_BUNGEE_PATTERN.matcher(out);
        StringBuilder sb2 = new StringBuilder(out.length());
        while (bungeeMatcher.find()) {
            String code = bungeeMatcher.group().replace("&", "").substring(1); // strip leading 'x'
            StringBuilder repl = new StringBuilder("§x");
            for (char c : code.toCharArray()) {
                repl.append('§').append(c);
            }
            bungeeMatcher.appendReplacement(sb2, Matcher.quoteReplacement(repl.toString()));
        }
        bungeeMatcher.appendTail(sb2);

        // Standard &0-&9 &a-&f &k-&o &r codes
        char[] chars = sb2.toString().toCharArray();
        StringBuilder result = new StringBuilder(chars.length);
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if (c == '&' && i + 1 < chars.length && isColorCode(chars[i + 1])) {
                result.append(COLOR_CHAR).append(Character.toLowerCase(chars[i + 1]));
                i++;
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    private static boolean isColorCode(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F')
            || (c >= 'k' && c <= 'o') || (c >= 'K' && c <= 'O') || c == 'r' || c == 'R';
    }

    /**
     * Converts a configurable string (with & and hex codes) into an Adventure {@link Component}.
     * Italic is explicitly disabled so lore/title lines do not render slanted by default.
     */
    public static Component toComponent(String input) {
        return LegacyComponentSerializer.legacySection()
            .deserialize(colorize(input))
            .decoration(TextDecoration.ITALIC, false);
    }

    /** Same as {@link #toComponent(String)} but keeps the default italic state untouched. */
    public static Component toComponentKeepingItalic(String input) {
        return LegacyComponentSerializer.legacySection().deserialize(colorize(input));
    }

    /** Converts a list of configurable strings into Components. */
    public static List<Component> toComponents(List<String> inputs) {
        List<Component> out = new ArrayList<>();
        if (inputs != null) {
            for (String line : inputs) {
                out.add(toComponent(line));
            }
        }
        return out;
    }

    /**
     * Returns a plain text view of a component (colors serialized to § codes, then stripped).
     * Useful for text-based lookups (e.g. detecting an existing "Sell Worth" lore line).
     */
    public static String plainText(Component component) {
        if (component == null) {
            return "";
        }
        return stripColors(LegacyComponentSerializer.legacySection().serialize(component));
    }

    /** Removes all § color/format codes (including hex §x sequences) from a string. */
    public static String stripColors(String input) {
        if (input == null) {
            return "";
        }
        // Strip §x§R§R§G§G§B§B hex sequences first, then the simple codes.
        String out = input.replaceAll("(?i)§x(§[0-9a-f]){6}", "");
        return STRIP_PATTERN.matcher(out).replaceAll("");
    }
}
