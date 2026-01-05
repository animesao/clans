package ru.clans.utils;

import net.md_5.bungee.api.ChatColor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColorUtil {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern GRADIENT_PATTERN = Pattern.compile("<gradient:([A-Fa-f0-9]{6}):([A-Fa-f0-9]{6})>(.*?)</gradient>");

    public static String colorize(String message) {
        if (message == null) return "";
        
        message = applyGradient(message);
        message = applyHexColors(message);
        message = ChatColor.translateAlternateColorCodes('&', message);
        
        return message;
    }

    public static String applyHexColors(String message) {
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer();
        
        while (matcher.find()) {
            String hex = matcher.group(1);
            ChatColor color = ChatColor.of("#" + hex);
            matcher.appendReplacement(buffer, color.toString());
        }
        
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    public static String applyGradient(String message) {
        Matcher matcher = GRADIENT_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer();
        
        while (matcher.find()) {
            String startHex = matcher.group(1);
            String endHex = matcher.group(2);
            String text = matcher.group(3);
            String gradientText = createGradient(text, startHex, endHex);
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(gradientText));
        }
        
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    public static String createGradient(String text, String startHex, String endHex) {
        int[] startRGB = hexToRGB(startHex);
        int[] endRGB = hexToRGB(endHex);
        
        StringBuilder result = new StringBuilder();
        int length = text.length();
        
        for (int i = 0; i < length; i++) {
            double ratio = (double) i / (length - 1);
            int r = (int) (startRGB[0] + ratio * (endRGB[0] - startRGB[0]));
            int g = (int) (startRGB[1] + ratio * (endRGB[1] - startRGB[1]));
            int b = (int) (startRGB[2] + ratio * (endRGB[2] - startRGB[2]));
            
            ChatColor color = ChatColor.of(String.format("#%02X%02X%02X", r, g, b));
            result.append(color).append(text.charAt(i));
        }
        
        return result.toString();
    }

    public static int[] hexToRGB(String hex) {
        hex = hex.replace("#", "");
        return new int[] {
            Integer.parseInt(hex.substring(0, 2), 16),
            Integer.parseInt(hex.substring(2, 4), 16),
            Integer.parseInt(hex.substring(4, 6), 16)
        };
    }

    public static String rgbToHex(int r, int g, int b) {
        return String.format("#%02X%02X%02X", r, g, b);
    }

    public static boolean isValidHex(String hex) {
        if (hex == null) return false;
        hex = hex.replace("#", "");
        return hex.matches("[A-Fa-f0-9]{6}");
    }

    public static String stripColor(String message) {
        return ChatColor.stripColor(colorize(message));
    }

    public static ChatColor getHexColor(String hex) {
        if (!isValidHex(hex)) {
            return ChatColor.WHITE;
        }
        return ChatColor.of(hex.startsWith("#") ? hex : "#" + hex);
    }
}
