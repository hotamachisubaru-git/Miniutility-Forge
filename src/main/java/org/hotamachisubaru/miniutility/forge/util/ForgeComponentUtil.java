package org.hotamachisubaru.miniutility.forge.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

public final class ForgeComponentUtil {
    private ForgeComponentUtil() {
    }

    public static Component fromLegacy(String text) {
        if (text == null || text.isEmpty()) {
            return Component.literal("");
        }

        MutableComponent root = Component.literal("");
        StringBuilder segment = new StringBuilder();
        Style style = Style.EMPTY;

        for (int i = 0; i < text.length(); i++) {
            char current = text.charAt(i);
            if (current == '§' && i + 1 < text.length()) {
                appendSegment(root, segment, style);
                style = applyLegacyCode(style, Character.toLowerCase(text.charAt(++i)));
                continue;
            }

            segment.append(current);
        }

        appendSegment(root, segment, style);
        return root;
    }

    public static String ampersandToSection(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        StringBuilder converted = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char current = text.charAt(i);
            if (current == '&' && i + 1 < text.length() && isLegacyCode(text.charAt(i + 1))) {
                converted.append('§');
                converted.append(Character.toLowerCase(text.charAt(++i)));
            } else {
                converted.append(current);
            }
        }
        return converted.toString();
    }

    private static void appendSegment(MutableComponent root, StringBuilder segment, Style style) {
        if (segment.isEmpty()) {
            return;
        }

        root.append(Component.literal(segment.toString()).setStyle(style));
        segment.setLength(0);
    }

    private static Style applyLegacyCode(Style current, char code) {
        return switch (code) {
            case '0' -> Style.EMPTY.withColor(ChatFormatting.BLACK);
            case '1' -> Style.EMPTY.withColor(ChatFormatting.DARK_BLUE);
            case '2' -> Style.EMPTY.withColor(ChatFormatting.DARK_GREEN);
            case '3' -> Style.EMPTY.withColor(ChatFormatting.DARK_AQUA);
            case '4' -> Style.EMPTY.withColor(ChatFormatting.DARK_RED);
            case '5' -> Style.EMPTY.withColor(ChatFormatting.DARK_PURPLE);
            case '6' -> Style.EMPTY.withColor(ChatFormatting.GOLD);
            case '7' -> Style.EMPTY.withColor(ChatFormatting.GRAY);
            case '8' -> Style.EMPTY.withColor(ChatFormatting.DARK_GRAY);
            case '9' -> Style.EMPTY.withColor(ChatFormatting.BLUE);
            case 'a' -> Style.EMPTY.withColor(ChatFormatting.GREEN);
            case 'b' -> Style.EMPTY.withColor(ChatFormatting.AQUA);
            case 'c' -> Style.EMPTY.withColor(ChatFormatting.RED);
            case 'd' -> Style.EMPTY.withColor(ChatFormatting.LIGHT_PURPLE);
            case 'e' -> Style.EMPTY.withColor(ChatFormatting.YELLOW);
            case 'f' -> Style.EMPTY.withColor(ChatFormatting.WHITE);
            case 'k' -> current.withObfuscated(true);
            case 'l' -> current.withBold(true);
            case 'm' -> current.withStrikethrough(true);
            case 'n' -> current.withUnderlined(true);
            case 'o' -> current.withItalic(true);
            case 'r' -> Style.EMPTY;
            default -> current;
        };
    }

    private static boolean isLegacyCode(char code) {
        char normalized = Character.toLowerCase(code);
        return (normalized >= '0' && normalized <= '9')
                || (normalized >= 'a' && normalized <= 'f')
                || normalized == 'k'
                || normalized == 'l'
                || normalized == 'm'
                || normalized == 'n'
                || normalized == 'o'
                || normalized == 'r';
    }
}
