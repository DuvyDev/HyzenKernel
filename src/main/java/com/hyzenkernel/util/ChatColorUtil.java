package com.hyzenkernel.util;

import com.hypixel.hytale.server.core.Message;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility for converting Minecraft-style color codes (&0-&f, &l, &o, etc.)
 * to Hytale's Message API format.
 *
 * Hytale doesn't use section sign (§) color codes like Minecraft.
 * Instead, it uses Message.raw("text").color("#RRGGBB") for colors.
 */
public class ChatColorUtil {

    // Minecraft color code to hex color mapping
    private static final Map<Character, String> COLOR_MAP = new HashMap<>();

    static {
        COLOR_MAP.put('0', "#000000"); // Black
        COLOR_MAP.put('1', "#0000AA"); // Dark Blue
        COLOR_MAP.put('2', "#00AA00"); // Dark Green
        COLOR_MAP.put('3', "#00AAAA"); // Dark Aqua
        COLOR_MAP.put('4', "#AA0000"); // Dark Red
        COLOR_MAP.put('5', "#AA00AA"); // Dark Purple
        COLOR_MAP.put('6', "#FFAA00"); // Gold
        COLOR_MAP.put('7', "#AAAAAA"); // Gray
        COLOR_MAP.put('8', "#555555"); // Dark Gray
        COLOR_MAP.put('9', "#5555FF"); // Blue
        COLOR_MAP.put('a', "#55FF55"); // Green
        COLOR_MAP.put('b', "#55FFFF"); // Aqua
        COLOR_MAP.put('c', "#FF5555"); // Red
        COLOR_MAP.put('d', "#FF55FF"); // Light Purple
        COLOR_MAP.put('e', "#FFFF55"); // Yellow
        COLOR_MAP.put('f', "#FFFFFF"); // White
    }

    /**
     * Converts a string with Minecraft-style color codes (&0-&f) to a Hytale Message.
     *
     * @param text The text with & color codes
     * @return A properly formatted Hytale Message
     */
    public static Message format(String text) {
        if (text == null || text.isEmpty()) {
            return Message.raw("");
        }

        List<Message> segments = new ArrayList<>();
        StringBuilder currentText = new StringBuilder();
        String currentColor = null;
        boolean currentBold = false;
        boolean currentItalic = false;

        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);

            // Check for color code
            if (c == '&' && i + 1 < text.length()) {
                char code = Character.toLowerCase(text.charAt(i + 1));

                // Check if it's a valid color code
                if (COLOR_MAP.containsKey(code)) {
                    // Save current segment if we have text
                    if (currentText.length() > 0) {
                        segments.add(createSegment(currentText.toString(), currentColor, currentBold, currentItalic));
                        currentText = new StringBuilder();
                    }
                    currentColor = COLOR_MAP.get(code);
                    // Reset formatting on color change (like Minecraft)
                    currentBold = false;
                    currentItalic = false;
                    i += 2;
                    continue;
                }
                // Check for formatting codes
                else if (code == 'l') { // Bold
                    if (currentText.length() > 0) {
                        segments.add(createSegment(currentText.toString(), currentColor, currentBold, currentItalic));
                        currentText = new StringBuilder();
                    }
                    currentBold = true;
                    i += 2;
                    continue;
                }
                else if (code == 'o') { // Italic
                    if (currentText.length() > 0) {
                        segments.add(createSegment(currentText.toString(), currentColor, currentBold, currentItalic));
                        currentText = new StringBuilder();
                    }
                    currentItalic = true;
                    i += 2;
                    continue;
                }
                else if (code == 'r') { // Reset
                    if (currentText.length() > 0) {
                        segments.add(createSegment(currentText.toString(), currentColor, currentBold, currentItalic));
                        currentText = new StringBuilder();
                    }
                    currentColor = null;
                    currentBold = false;
                    currentItalic = false;
                    i += 2;
                    continue;
                }
                // Unsupported codes (k=obfuscated, m=strikethrough, n=underline) - skip them
                else if (code == 'k' || code == 'm' || code == 'n') {
                    i += 2;
                    continue;
                }
            }

            // Regular character
            currentText.append(c);
            i++;
        }

        // Add remaining text
        if (currentText.length() > 0) {
            segments.add(createSegment(currentText.toString(), currentColor, currentBold, currentItalic));
        }

        // Join all segments
        if (segments.isEmpty()) {
            return Message.raw("");
        } else if (segments.size() == 1) {
            return segments.get(0);
        } else {
            return Message.join(segments.toArray(new Message[0]));
        }
    }

    /**
     * Creates a Message segment with the given formatting.
     */
    private static Message createSegment(String text, String color, boolean bold, boolean italic) {
        Message msg = Message.raw(text);
        if (color != null) {
            msg = msg.color(color);
        }
        if (bold) {
            msg = msg.bold(true);
        }
        if (italic) {
            msg = msg.italic(true);
        }
        return msg;
    }

    /**
     * Convenience method to send a colored message to a player.
     */
    public static void sendMessage(com.hypixel.hytale.server.core.entity.entities.Player player, String message) {
        player.sendMessage(format(message));
    }
}
