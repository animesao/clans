package ru.clans.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import ru.clans.ClanPlugin;
import ru.clans.gui.GUIManager;
import ru.clans.models.Clan;
import ru.clans.models.ClanMember;
import ru.clans.models.ClanRank;
import ru.clans.utils.MessageUtil;

import java.util.*;

public class ClanAdminCommand implements CommandExecutor, TabCompleter {

    private final ClanPlugin plugin;
    private final GUIManager guiManager;

    public ClanAdminCommand(ClanPlugin plugin, GUIManager guiManager) {
        this.plugin = plugin;
        this.guiManager = guiManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("clan.admin")) {
            MessageUtil.send(sender, "&cУ вас нет прав на использование этой команды!");
            return true;
        }

        if (args.length == 0) {
            if (sender instanceof Player) {
                guiManager.openAdminMenu((Player) sender);
            } else {
                sendAdminHelp(sender);
            }
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "reload":
                handleReload(sender);
                break;
            case "disband":
                handleDisband(sender, args);
                break;
            case "setlevel":
                handleSetLevel(sender, args);
                break;
            case "addexp":
                handleAddExp(sender, args);
                break;
            case "setexp":
                handleSetExp(sender, args);
                break;
            case "setleader":
                handleSetLeader(sender, args);
                break;
            case "addmember":
                handleAddMember(sender, args);
                break;
            case "removemember":
                handleRemoveMember(sender, args);
                break;
            case "setrank":
                handleSetRank(sender, args);
                break;
            case "setstorage":
                handleSetStorage(sender, args);
                break;
            case "list":
                handleList(sender, args);
                break;
            case "info":
                handleInfo(sender, args);
                break;
            case "save":
                handleSave(sender);
                break;
            case "reset":
                handleReset(sender, args);
                break;
            case "help":
            default:
                sendAdminHelp(sender);
                break;
        }

        return true;
    }

    private void handleReload(CommandSender sender) {
        plugin.getConfigManager().reloadConfigs();
        plugin.getGUIConfigManager().reloadConfig();
        MessageUtil.send(sender, "&aКонфигурация успешно перезагружена!");
    }

    private void handleDisband(CommandSender sender, String[] args) {
        if (args.length < 2) {
            MessageUtil.send(sender, "&cИспользуйте: /clanadmin disband <клан>");
            return;
        }

        Clan clan = findClan(args[1]);
        if (clan == null) {
            MessageUtil.send(sender, "&cКлан не найден!");
            return;
        }

        String clanName = clan.getName();
        plugin.getClanManager().disbandClan(clan);
        MessageUtil.send(sender, "&aКлан &f" + clanName + " &aбыл расформирован!");
    }

    private void handleSetLevel(CommandSender sender, String[] args) {
        if (args.length < 3) {
            MessageUtil.send(sender, "&cИспользуйте: /clanadmin setlevel <клан> <уровень>");
            return;
        }

        Clan clan = findClan(args[1]);
        if (clan == null) {
            MessageUtil.send(sender, "&cКлан не найден!");
            return;
        }

        int level;
        try {
            level = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            MessageUtil.send(sender, "&cНеверный формат уровня!");
            return;
        }

        if (level < 1 || level > 50) {
            MessageUtil.send(sender, "&cУровень должен быть от 1 до 50!");
            return;
        }

        int oldLevel = clan.getLevel();
        clan.setLevel(level);
        
        if (level > oldLevel) {
            int storageUpgrades = 0;
            for (int i = oldLevel + 1; i <= level; i++) {
                Map<String, Object> rewards = plugin.getConfigManager().getLevelRewards(i);
                if (rewards.containsKey("storage") && (Boolean) rewards.get("storage")) {
                    if (clan.getStorageLevel() < plugin.getConfigManager().getMaxStorageLevel()) {
                        clan.upgradeStorage();
                        storageUpgrades++;
                    }
                }
            }
            if (storageUpgrades > 0) {
                MessageUtil.send(sender, "&7Хранилище улучшено до уровня &f" + clan.getStorageLevel());
            }
        }
        
        plugin.getDataManager().saveClan(clan);
        MessageUtil.send(sender, "&aУровень клана &f" + clan.getName() + " &aустановлен на &f" + level);
    }

    private void handleAddExp(CommandSender sender, String[] args) {
        if (args.length < 3) {
            MessageUtil.send(sender, "&cИспользуйте: /clanadmin addexp <клан> <количество>");
            return;
        }

        Clan clan = findClan(args[1]);
        if (clan == null) {
            MessageUtil.send(sender, "&cКлан не найден!");
            return;
        }

        long exp;
        try {
            exp = Long.parseLong(args[2]);
        } catch (NumberFormatException e) {
            MessageUtil.send(sender, "&cНеверный формат опыта!");
            return;
        }

        plugin.getLevelManager().addExperience(clan, exp, "Admin command");
        MessageUtil.send(sender, "&aДобавлено &f" + exp + " &aопыта клану &f" + clan.getName());
    }

    private void handleSetExp(CommandSender sender, String[] args) {
        if (args.length < 3) {
            MessageUtil.send(sender, "&cИспользуйте: /clanadmin setexp <клан> <количество>");
            return;
        }

        Clan clan = findClan(args[1]);
        if (clan == null) {
            MessageUtil.send(sender, "&cКлан не найден!");
            return;
        }

        long exp;
        try {
            exp = Long.parseLong(args[2]);
        } catch (NumberFormatException e) {
            MessageUtil.send(sender, "&cНеверный формат опыта!");
            return;
        }

        clan.setExperience(exp);
        plugin.getDataManager().saveClan(clan);
        MessageUtil.send(sender, "&aОпыт клана &f" + clan.getName() + " &aустановлен на &f" + exp);
    }

    private void handleSetLeader(CommandSender sender, String[] args) {
        if (args.length < 3) {
            MessageUtil.send(sender, "&cИспользуйте: /clanadmin setleader <клан> <игрок>");
            return;
        }

        Clan clan = findClan(args[1]);
        if (clan == null) {
            MessageUtil.send(sender, "&cКлан не найден!");
            return;
        }

        ClanMember member = findMember(clan, args[2]);
        if (member == null) {
            MessageUtil.send(sender, "&cИгрок не найден в клане!");
            return;
        }

        plugin.getClanManager().transferLeadership(clan, member.getUuid());
        MessageUtil.send(sender, "&aЛидер клана &f" + clan.getName() + " &aизменён на &f" + member.getName());
    }

    private void handleAddMember(CommandSender sender, String[] args) {
        if (args.length < 3) {
            MessageUtil.send(sender, "&cИспользуйте: /clanadmin addmember <клан> <игрок>");
            return;
        }

        Clan clan = findClan(args[1]);
        if (clan == null) {
            MessageUtil.send(sender, "&cКлан не найден!");
            return;
        }

        Player target = Bukkit.getPlayer(args[2]);
        if (target == null) {
            MessageUtil.send(sender, "&cИгрок не найден или не в сети!");
            return;
        }

        if (plugin.getClanManager().isInClan(target.getUniqueId())) {
            MessageUtil.send(sender, "&cИгрок уже состоит в клане!");
            return;
        }

        plugin.getClanManager().addMember(clan, target, ClanRank.RECRUIT);
        MessageUtil.send(sender, "&aИгрок &f" + target.getName() + " &aдобавлен в клан &f" + clan.getName());
    }

    private void handleRemoveMember(CommandSender sender, String[] args) {
        if (args.length < 3) {
            MessageUtil.send(sender, "&cИспользуйте: /clanadmin removemember <клан> <игрок>");
            return;
        }

        Clan clan = findClan(args[1]);
        if (clan == null) {
            MessageUtil.send(sender, "&cКлан не найден!");
            return;
        }

        ClanMember member = findMember(clan, args[2]);
        if (member == null) {
            MessageUtil.send(sender, "&cИгрок не найден в клане!");
            return;
        }

        if (clan.isLeader(member.getUuid())) {
            MessageUtil.send(sender, "&cНельзя удалить лидера! Сначала передайте лидерство.");
            return;
        }

        plugin.getClanManager().removeMember(clan, member.getUuid());
        MessageUtil.send(sender, "&aИгрок &f" + member.getName() + " &aудалён из клана &f" + clan.getName());
    }

    private void handleSetRank(CommandSender sender, String[] args) {
        if (args.length < 4) {
            MessageUtil.send(sender, "&cИспользуйте: /clanadmin setrank <клан> <игрок> <ранг>");
            MessageUtil.send(sender, "&7Ранги: RECRUIT, MEMBER, OFFICER, ELDER, COLEADER, LEADER");
            return;
        }

        Clan clan = findClan(args[1]);
        if (clan == null) {
            MessageUtil.send(sender, "&cКлан не найден!");
            return;
        }

        ClanMember member = findMember(clan, args[2]);
        if (member == null) {
            MessageUtil.send(sender, "&cИгрок не найден в клане!");
            return;
        }

        ClanRank rank;
        try {
            rank = ClanRank.valueOf(args[3].toUpperCase());
        } catch (IllegalArgumentException e) {
            MessageUtil.send(sender, "&cНеверный ранг! Доступные: RECRUIT, MEMBER, OFFICER, ELDER, COLEADER, LEADER");
            return;
        }

        member.setRank(rank);
        if (rank == ClanRank.LEADER) {
            clan.setLeader(member.getUuid());
        }
        plugin.getDataManager().saveClan(clan);
        MessageUtil.send(sender, "&aРанг игрока &f" + member.getName() + " &aизменён на &f" + rank.getDisplayName());
    }

    private void handleSetStorage(CommandSender sender, String[] args) {
        if (args.length < 3) {
            MessageUtil.send(sender, "&cИспользуйте: /clanadmin setstorage <клан> <уровень>");
            return;
        }

        Clan clan = findClan(args[1]);
        if (clan == null) {
            MessageUtil.send(sender, "&cКлан не найден!");
            return;
        }

        int level;
        try {
            level = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            MessageUtil.send(sender, "&cНеверный формат уровня!");
            return;
        }

        if (level < 1 || level > 5) {
            MessageUtil.send(sender, "&cУровень хранилища должен быть от 1 до 5!");
            return;
        }

        clan.setStorageLevel(level);
        plugin.getDataManager().saveClan(clan);
        MessageUtil.send(sender, "&aУровень хранилища клана &f" + clan.getName() + " &aустановлен на &f" + level);
    }

    private void handleList(CommandSender sender, String[] args) {
        int page = 1;
        if (args.length > 1) {
            try {
                page = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                page = 1;
            }
        }

        List<Clan> clans = new ArrayList<>(plugin.getClanManager().getClans());
        int perPage = 10;
        int totalPages = (int) Math.ceil((double) clans.size() / perPage);
        page = Math.max(1, Math.min(page, totalPages));

        MessageUtil.sendRaw(sender, "");
        MessageUtil.sendRaw(sender, "&6&lСписок кланов &7(стр. " + page + "/" + totalPages + ")");
        
        int start = (page - 1) * perPage;
        int end = Math.min(start + perPage, clans.size());

        for (int i = start; i < end; i++) {
            Clan clan = clans.get(i);
            MessageUtil.sendRaw(sender, "&7" + (i + 1) + ". &f" + clan.getName() + " &7[" + clan.getTag() + "] " +
                    "&7- Ур: &f" + clan.getLevel() + " &7| Уч: &f" + clan.getMemberCount());
        }
        MessageUtil.sendRaw(sender, "");
    }

    private void handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            MessageUtil.send(sender, "&cИспользуйте: /clanadmin info <клан>");
            return;
        }

        Clan clan = findClan(args[1]);
        if (clan == null) {
            MessageUtil.send(sender, "&cКлан не найден!");
            return;
        }

        MessageUtil.sendRaw(sender, "");
        MessageUtil.sendRaw(sender, "&6&l" + clan.getName() + " &7[" + clan.getTag() + "]");
        MessageUtil.sendRaw(sender, "&7ID: &f" + clan.getId());
        MessageUtil.sendRaw(sender, "&7Уровень: &f" + clan.getLevel() + " &7| Опыт: &f" + clan.getExperience());
        MessageUtil.sendRaw(sender, "&7Участников: &f" + clan.getMemberCount());
        MessageUtil.sendRaw(sender, "&7Убийств: &a" + clan.getKills() + " &7| Смертей: &c" + clan.getDeaths());
        MessageUtil.sendRaw(sender, "&7K/D: &f" + String.format("%.2f", clan.getKDR()));
        MessageUtil.sendRaw(sender, "&7Хранилище: &fУровень " + clan.getStorageLevel());
        MessageUtil.sendRaw(sender, "&7PVP: " + (clan.isPvpEnabled() ? "&aВкл" : "&cВыкл"));
        MessageUtil.sendRaw(sender, "&7Цвет: &f" + clan.getHexColor());
        
        ClanMember leader = clan.getMember(clan.getLeader());
        MessageUtil.sendRaw(sender, "&7Лидер: &f" + (leader != null ? leader.getName() : "N/A"));
        MessageUtil.sendRaw(sender, "");
        
        MessageUtil.sendRaw(sender, "&7Участники:");
        for (ClanMember member : clan.getMembers().values()) {
            boolean online = Bukkit.getPlayer(member.getUuid()) != null;
            MessageUtil.sendRaw(sender, "  &7- " + (online ? "&a" : "&c") + member.getName() + 
                    " &7(" + member.getRank().getDisplayName() + "&7)");
        }
        MessageUtil.sendRaw(sender, "");
    }

    private void handleSave(CommandSender sender) {
        plugin.getDataManager().saveAllClans();
        MessageUtil.send(sender, "&aВсе данные кланов сохранены!");
    }

    private void handleReset(CommandSender sender, String[] args) {
        if (args.length < 2) {
            MessageUtil.send(sender, "&cИспользуйте: /clanadmin reset <клан> <stats|quests|storage>");
            return;
        }

        Clan clan = findClan(args[1]);
        if (clan == null) {
            MessageUtil.send(sender, "&cКлан не найден!");
            return;
        }

        if (args.length < 3) {
            MessageUtil.send(sender, "&cУкажите что сбросить: stats, quests, storage");
            return;
        }

        switch (args[2].toLowerCase()) {
            case "stats":
                clan.resetStats();
                for (ClanMember member : clan.getMembers().values()) {
                    member.resetStats();
                }
                MessageUtil.send(sender, "&aСтатистика клана &f" + clan.getName() + " &aсброшена!");
                break;
            case "quests":
                clan.getActiveQuests().clear();
                MessageUtil.send(sender, "&aЗадания клана &f" + clan.getName() + " &aсброшены!");
                break;
            case "storage":
                clan.clearStorage();
                MessageUtil.send(sender, "&aХранилище клана &f" + clan.getName() + " &aочищено!");
                break;
            default:
                MessageUtil.send(sender, "&cНеверный параметр! Доступные: stats, quests, storage");
                return;
        }

        plugin.getDataManager().saveClan(clan);
    }

    private Clan findClan(String query) {
        Clan clan = plugin.getClanManager().getClanByTag(query);
        if (clan == null) {
            clan = plugin.getClanManager().getClanByName(query);
        }
        return clan;
    }

    private ClanMember findMember(Clan clan, String name) {
        for (ClanMember member : clan.getMembers().values()) {
            if (member.getName().equalsIgnoreCase(name)) {
                return member;
            }
        }
        return null;
    }

    private void sendAdminHelp(CommandSender sender) {
        MessageUtil.sendRaw(sender, "");
        MessageUtil.sendRaw(sender, "&c&lАдминистрирование кланов:");
        MessageUtil.sendRaw(sender, "&c/clanadmin reload &7- Перезагрузить конфиг");
        MessageUtil.sendRaw(sender, "&c/clanadmin list [страница] &7- Список кланов");
        MessageUtil.sendRaw(sender, "&c/clanadmin info <клан> &7- Информация о клане");
        MessageUtil.sendRaw(sender, "&c/clanadmin disband <клан> &7- Расформировать клан");
        MessageUtil.sendRaw(sender, "&c/clanadmin setlevel <клан> <уровень> &7- Установить уровень");
        MessageUtil.sendRaw(sender, "&c/clanadmin addexp <клан> <кол-во> &7- Добавить опыт");
        MessageUtil.sendRaw(sender, "&c/clanadmin setexp <клан> <кол-во> &7- Установить опыт");
        MessageUtil.sendRaw(sender, "&c/clanadmin setleader <клан> <игрок> &7- Сменить лидера");
        MessageUtil.sendRaw(sender, "&c/clanadmin addmember <клан> <игрок> &7- Добавить участника");
        MessageUtil.sendRaw(sender, "&c/clanadmin removemember <клан> <игрок> &7- Удалить участника");
        MessageUtil.sendRaw(sender, "&c/clanadmin setrank <клан> <игрок> <ранг> &7- Изменить ранг");
        MessageUtil.sendRaw(sender, "&c/clanadmin setstorage <клан> <уровень> &7- Уровень хранилища");
        MessageUtil.sendRaw(sender, "&c/clanadmin reset <клан> <stats|quests|storage> &7- Сброс");
        MessageUtil.sendRaw(sender, "&c/clanadmin save &7- Сохранить все данные");
        MessageUtil.sendRaw(sender, "");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("clan.admin")) return Collections.emptyList();

        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subCommands = Arrays.asList(
                    "reload", "list", "info", "disband", "setlevel", "addexp", "setexp",
                    "setleader", "addmember", "removemember", "setrank", "setstorage",
                    "reset", "save", "help"
            );
            for (String sub : subCommands) {
                if (sub.startsWith(args[0].toLowerCase())) {
                    completions.add(sub);
                }
            }
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (Arrays.asList("info", "disband", "setlevel", "addexp", "setexp", 
                    "setleader", "addmember", "removemember", "setrank", "setstorage", "reset").contains(sub)) {
                for (Clan clan : plugin.getClanManager().getClans()) {
                    if (clan.getTag().toLowerCase().startsWith(args[1].toLowerCase()) ||
                            clan.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(clan.getTag());
                    }
                }
            }
        } else if (args.length == 3) {
            String sub = args[0].toLowerCase();
            if (Arrays.asList("setleader", "removemember", "setrank", "addmember").contains(sub)) {
                if (sub.equals("addmember")) {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        if (p.getName().toLowerCase().startsWith(args[2].toLowerCase())) {
                            completions.add(p.getName());
                        }
                    }
                } else {
                    Clan clan = findClan(args[1]);
                    if (clan != null) {
                        for (ClanMember member : clan.getMembers().values()) {
                            if (member.getName().toLowerCase().startsWith(args[2].toLowerCase())) {
                                completions.add(member.getName());
                            }
                        }
                    }
                }
            } else if (sub.equals("reset")) {
                for (String option : Arrays.asList("stats", "quests", "storage")) {
                    if (option.startsWith(args[2].toLowerCase())) {
                        completions.add(option);
                    }
                }
            }
        } else if (args.length == 4) {
            if (args[0].equalsIgnoreCase("setrank")) {
                for (ClanRank rank : ClanRank.values()) {
                    if (rank.name().toLowerCase().startsWith(args[3].toLowerCase())) {
                        completions.add(rank.name());
                    }
                }
            }
        }

        return completions;
    }
}
