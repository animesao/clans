package ru.clans.utils;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.clans.ClanPlugin;
import ru.clans.models.Clan;
import ru.clans.models.ClanMember;

public class MessageUtil {

    private static final String PREFIX = "&8[&#FF6B6BК&#FF8E53л&#FFB347а&#E2C044н&#C4D93Fы&8] &7";

    public static void send(CommandSender sender, String message) {
        if (message == null || message.isEmpty()) return;
        sender.sendMessage(ColorUtil.colorize(PREFIX + message));
    }

    public static void sendRaw(CommandSender sender, String message) {
        if (message == null || message.isEmpty()) return;
        sender.sendMessage(ColorUtil.colorize(message));
    }

    public static void sendClanMessage(Clan clan, String message) {
        if (message == null || message.isEmpty()) return;
        String formatted = ColorUtil.colorize(PREFIX + message);
        for (ClanMember member : clan.getMembers().values()) {
            Player player = Bukkit.getPlayer(member.getUuid());
            if (player != null && player.isOnline()) {
                player.sendMessage(formatted);
            }
        }
    }

    public static void sendClanChat(Clan clan, Player sender, String message) {
        String hexColor = clan.getHexColor();
        String format = ColorUtil.colorize(
            "&8[" + ColorUtil.getHexColor(hexColor) + clan.getTag() + "&8] " +
            "&7" + sender.getName() + "&8: &f" + message
        );
        
        for (ClanMember member : clan.getMembers().values()) {
            Player player = Bukkit.getPlayer(member.getUuid());
            if (player != null && player.isOnline()) {
                player.sendMessage(format);
            }
        }
    }

    public static void broadcast(String message) {
        if (message == null || message.isEmpty()) return;
        String formatted = ColorUtil.colorize(PREFIX + message);
        Bukkit.broadcastMessage(formatted);
    }

    public static String formatTime(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return days + "д " + (hours % 24) + "ч";
        } else if (hours > 0) {
            return hours + "ч " + (minutes % 60) + "мин";
        } else if (minutes > 0) {
            return minutes + "мин " + (seconds % 60) + "сек";
        } else {
            return seconds + "сек";
        }
    }

    public static String formatNumber(long number) {
        if (number >= 1_000_000) {
            return String.format("%.1fM", number / 1_000_000.0);
        } else if (number >= 1_000) {
            return String.format("%.1fK", number / 1_000.0);
        }
        return String.valueOf(number);
    }

    public static String createProgressBar(double percent, int length) {
        int filled = (int) (length * (percent / 100));
        int empty = length - filled;
        
        StringBuilder bar = new StringBuilder();
        bar.append("&#00FF00");
        for (int i = 0; i < filled; i++) {
            bar.append("▌");
        }
        bar.append("&7");
        for (int i = 0; i < empty; i++) {
            bar.append("▌");
        }
        
        return ColorUtil.colorize(bar.toString());
    }
}
