package com.pumpkings.pkcrates.presentation.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.format.TextDecoration;

/**
 * Central text parsing utility for PkCrates.
 *
 * <h3>Supported formats</h3>
 * <ul>
 *   <li><strong>MiniMessage</strong> — {@code <green>}, {@code <gradient:#ff0000:#00ff00>}, {@code <bold>}, etc.</li>
 *   <li><strong>Legacy section sign (§)</strong> — Converted to {@code &} equivalents then fed to MiniMessage.</li>
 *   <li><strong>Ampersand codes (&amp;)</strong> — Converted to their named MiniMessage counterparts before parsing,
 *       so mixed content like {@code &aHello <gold>world</gold>} is handled correctly.</li>
 * </ul>
 *
 * <h3>Why a single pipeline?</h3>
 * <p>The previous implementation branched on whether the string contained {@code &}:
 * if it did, the whole string was handed to {@code LegacyComponentSerializer}, which
 * does not understand MiniMessage tags and prints them as literal text.
 * This class always parses with MiniMessage after converting legacy codes first.</p>
 */
public class TextUtil {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    /**
     * Parses the given text into a {@link Component}.
     *
     * <ol>
     *   <li>Replaces {@code §} with {@code &} for uniformity.</li>
     *   <li>Converts ampersand color/format codes to their MiniMessage tag equivalents.</li>
     *   <li>Parses the result with {@link MiniMessage}.</li>
     *   <li>Wraps the result to disable the default italic decoration (useful inside GUIs).</li>
     * </ol>
     *
     * @param text Raw text to parse. May be {@code null} or empty.
     * @return The parsed {@link Component}, never {@code null}.
     */
    public static Component parse(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }

        // 1. Normalize section signs to ampersands
        String normalized = text.replace("§", "&");

        // 1.5 Convert hex colors &#RRGGBB to MiniMessage format <#RRGGBB>
        normalized = normalized.replaceAll("&#([a-fA-F0-9]{6})", "<#$1>");

        // 2. Convert &<code> → MiniMessage tags so MiniMessage can handle everything in one pass
        normalized = legacyToMiniMessage(normalized);

        // 3. Parse with MiniMessage
        Component result = MINI_MESSAGE.deserialize(normalized);

        // 4. Strip the default italic that inventory titles/lore get in vanilla
        return Component.empty().decoration(TextDecoration.ITALIC, false).append(result);
    }

    // -------------------------------------------------------------------------
    // Internal helper — maps &X codes to MiniMessage tag equivalents
    // -------------------------------------------------------------------------

    /**
     * Replaces {@code &X} legacy codes with their MiniMessage tag equivalents.
     *
     * <p>Only single-char codes are handled (colours + formatting).
     * Hex codes in the form {@code &x&r&r&g&g&b&b} are NOT converted because
     * they are rarely used and MiniMessage's {@code <#rrggbb>} syntax is preferred.</p>
     *
     * @param input The string potentially containing {@code &X} sequences.
     * @return The string with all recognised {@code &X} replaced by MiniMessage tags.
     */
    private static String legacyToMiniMessage(String input) {
        if (!input.contains("&")) return input;

        StringBuilder sb = new StringBuilder(input.length() + 16);
        int len = input.length();
        int i = 0;

        while (i < len) {
            char c = input.charAt(i);

            if (c == '&' && i + 1 < len) {
                char code = Character.toLowerCase(input.charAt(i + 1));
                String tag = codeToTag(code);
                if (tag != null) {
                    sb.append(tag);
                    i += 2;
                    continue;
                }
            }

            sb.append(c);
            i++;
        }

        return sb.toString();
    }

    /** Maps a single legacy colour/format char to its MiniMessage equivalent, or {@code null} if unknown. */
    private static String codeToTag(char code) {
        return switch (code) {
            case '0' -> "<black>";
            case '1' -> "<dark_blue>";
            case '2' -> "<dark_green>";
            case '3' -> "<dark_aqua>";
            case '4' -> "<dark_red>";
            case '5' -> "<dark_purple>";
            case '6' -> "<gold>";
            case '7' -> "<gray>";
            case '8' -> "<dark_gray>";
            case '9' -> "<blue>";
            case 'a' -> "<green>";
            case 'b' -> "<aqua>";
            case 'c' -> "<red>";
            case 'd' -> "<light_purple>";
            case 'e' -> "<yellow>";
            case 'f' -> "<white>";
            case 'k' -> "<obfuscated>";
            case 'l' -> "<bold>";
            case 'm' -> "<strikethrough>";
            case 'n' -> "<underlined>";
            case 'o' -> "<italic>";
            case 'r' -> "<reset>";
            default  -> null;
        };
    }
}
