package ru.clans.placeholders;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import ru.clans.ClanPlugin;
import ru.clans.models.Clan;
import ru.clans.models.ClanMember;

import java.util.ArrayList;
import java.util.List;
import java.util.Collection;

public class ClanPlaceholders extends PlaceholderExpansion {

    private final ClanPlugin plugin;

    public ClanPlaceholders(ClanPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "clan";
    }

    @Override
    public @NotNull String getAuthor() {
        return "animesao";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) return "";

        Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
        
        // Если игрока нет в менеджере кланов, значит он не в клане (даже если данные в модели остались)
        if (clan != null && !plugin.getClanManager().isInClan(player.getUniqueId())) {
            clan = null;
        }

        ClanMember member = clan != null ? clan.getMember(player.getUniqueId()) : null;

        switch (params.toLowerCase()) {
            case "has_clan":
                return clan != null ? "true" : "false";
            
            case "name":
                return clan != null ? clan.getName() : "";
            
            case "tag":
                return clan != null ? clan.getTag() : "";
            
            case "tag_formatted":
                return clan != null ? "[" + clan.getTag() + "]" : "";
            
            case "tag_colored":
                return clan != null ? clan.getColoredTag() : "";
            
            case "name_colored":
                return clan != null ? clan.getColoredName() : "";
            
            case "level":
                return clan != null ? String.valueOf(clan.getLevel()) : "0";
            
            case "level_colored":
                if (clan == null) return "0";
                String color = plugin.getConfigManager().getLevelColor(clan.getLevel());
                return ru.clans.utils.ColorUtil.getHexColor(color).toString() + clan.getLevel();

            case "level_prefix":
                return clan != null ? ru.clans.utils.ColorUtil.colorize(plugin.getConfigManager().getLevelPrefix(clan.getLevel())) : "";

            case "level_suffix":
                return clan != null ? ru.clans.utils.ColorUtil.colorize(plugin.getConfigManager().getLevelSuffix(clan.getLevel())) : "";

            case "level_full":
                if (clan == null) return "";
                int lvl = clan.getLevel();
                String p = plugin.getConfigManager().getLevelPrefix(lvl);
                String s = plugin.getConfigManager().getLevelSuffix(lvl);
                String c = plugin.getConfigManager().getLevelColor(lvl);
                return ru.clans.utils.ColorUtil.colorize(p + ru.clans.utils.ColorUtil.getHexColor(c) + lvl + s);
            
            case "experience":
                return clan != null ? String.valueOf(clan.getExperience()) : "0";
            
            case "balance":
            case "treasury":
            case "bank_balance":
                return clan != null ? String.valueOf((long)clan.getBank()) : "0";

            case "top_position_level":
                if (clan != null) {
                    List<Clan> topClans = plugin.getClanManager().getTopClans(100);
                    for (int i = 0; i < topClans.size(); i++) {
                        if (topClans.get(i).getId().equals(clan.getId())) return String.valueOf(i + 1);
                    }
                }
                return "0";

            case "top_position_treasury":
                if (clan != null) {
                    List<Clan> clansList = new ArrayList<>(plugin.getClanManager().getClans());
                    clansList.sort((c1, c2) -> Double.compare(c2.getBank(), c1.getBank()));
                    for (int i = 0; i < clansList.size(); i++) {
                        if (clansList.get(i).getId().equals(clan.getId())) return String.valueOf(i + 1);
                    }
                }
                return "0";

            case "top_position_kills":
                if (clan != null) {
                    List<Clan> clansList = new ArrayList<>(plugin.getClanManager().getClans());
                    clansList.sort((c1, c2) -> Integer.compare(c2.getKills(), c1.getKills()));
                    for (int i = 0; i < clansList.size(); i++) {
                        if (clansList.get(i).getId().equals(clan.getId())) return String.valueOf(i + 1);
                    }
                }
                return "0";
            
            case "experience_formatted":
                return clan != null ? formatNumber(clan.getExperience()) : "0";
            
            case "members":
            case "member_count":
                return clan != null ? String.valueOf(clan.getMemberCount()) : "0";
            
            case "max_members":
                return clan != null ? String.valueOf(plugin.getConfigManager().getMaxMembersForLevel(clan.getLevel())) : "0";
            
            case "kills":
                return clan != null ? String.valueOf(clan.getKills()) : "0";
            
            case "deaths":
                return clan != null ? String.valueOf(clan.getDeaths()) : "0";
            
            case "kdr":
                return clan != null ? String.format("%.2f", clan.getKDR()) : "0.00";
            
            case "rank":
                return member != null ? member.getRank().getDisplayName() : "";
            
            case "rank_prefix":
                return member != null ? member.getRank().getPrefix() : "";
            
            case "player_kills":
                return member != null ? String.valueOf(member.getKills()) : "0";
            
            case "player_deaths":
                return member != null ? String.valueOf(member.getDeaths()) : "0";
            
            case "player_kdr":
                return member != null ? String.format("%.2f", member.getKDR()) : "0.00";
            
            case "player_contribution":
                return member != null ? String.valueOf(member.getContribution()) : "0";
            
            case "color":
            case "hex_color":
                return clan != null ? clan.getHexColor() : "#FFFFFF";
            
            case "leader":
                if (clan != null) {
                    ClanMember leader = clan.getMember(clan.getLeader());
                    return leader != null ? leader.getName() : "";
                }
                return "";
            
            case "is_leader":
                return clan != null && clan.isLeader(player.getUniqueId()) ? "true" : "false";
            
            case "pvp_enabled":
                return clan != null ? (clan.isPvpEnabled() ? "true" : "false") : "false";
            
            case "has_home":
                return clan != null ? (clan.hasHome() ? "true" : "false") : "false";
            
            case "storage_level":
                return clan != null ? String.valueOf(clan.getStorageLevel()) : "0";
            
            case "bank":
            case "bank_balance_val":
                return clan != null ? String.valueOf((long)clan.getBank()) : "0";
            
            case "bank_formatted":
                return clan != null ? formatNumber((long)clan.getBank()) : "0";
            
            case "online_members":
                return clan != null ? String.valueOf(clan.getOnlineMemberCount()) : "0";
            
            case "position":
            case "rank_position":
                if (clan != null) {
                    List<Clan> topClans = plugin.getClanManager().getTopClans(100);
                    for (int i = 0; i < topClans.size(); i++) {
                        if (topClans.get(i).getId().equals(clan.getId())) {
                            return String.valueOf(i + 1);
                        }
                    }
                }
                return "0";
            
            default:
                if (params.startsWith("top_")) {
                    return handleTopPlaceholder(params);
                }
                return null;
        }
    }

    private String handleTopPlaceholder(String params) {
        String[] parts = params.split("_");
        if (parts.length < 3) return null;

        int position;
        try {
            position = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            return null;
        }

        if (position < 1 || position > 100) return null;

        List<Clan> topClans = plugin.getClanManager().getTopClans(position);
        if (topClans.size() < position) return "";

        Clan clan = topClans.get(position - 1);
        String type = parts[2].toLowerCase();

        switch (type) {
            case "name":
                return clan.getName();
            case "tag":
                return clan.getTag();
            case "level":
                return String.valueOf(clan.getLevel());
            case "experience":
                return String.valueOf(clan.getExperience());
            case "members":
                return String.valueOf(clan.getMemberCount());
            case "kills":
                return String.valueOf(clan.getKills());
            case "kdr":
                return String.format("%.2f", clan.getKDR());
            case "leader":
                ClanMember leader = clan.getMember(clan.getLeader());
                return leader != null ? leader.getName() : "";
            default:
                return null;
        }
    }

    private String formatNumber(long number) {
        if (number >= 1000000) {
            return String.format("%.1fM", number / 1000000.0);
        } else if (number >= 1000) {
            return String.format("%.1fK", number / 1000.0);
        }
        return String.valueOf(number);
    }
}
