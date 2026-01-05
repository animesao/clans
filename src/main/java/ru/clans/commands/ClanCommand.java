package ru.clans.commands;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import ru.clans.ClanPlugin;
import ru.clans.gui.GUIManager;
import ru.clans.models.*;
import ru.clans.models.BankTransaction;
import ru.clans.utils.ColorUtil;
import ru.clans.utils.MessageUtil;

import java.util.*;
import java.util.stream.Collectors;

public class ClanCommand implements CommandExecutor, TabCompleter {

    private final ClanPlugin plugin;
    private final Map<UUID, Long> homeCooldowns;
    private final Map<UUID, Long> confirmDisband;

    public ClanCommand(ClanPlugin plugin) {
        this.plugin = plugin;
        this.homeCooldowns = new HashMap<>();
        this.confirmDisband = new HashMap<>();
    }

    private GUIManager getGuiManager() {
        return plugin.getGUIManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Только для игроков!");
            return true;
        }

        Player player = (Player) sender;

        try {
            if (plugin.isReloading()) {
                MessageUtil.send(player, "&cПлагин перезагружается, подождите...");
                return true;
            }

            if (args.length == 0) {
                Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
                if (clan != null) {
                    getGuiManager().openMainMenu(player);
                } else {
                    sendHelp(player);
                }
                return true;
            }

            String subCommand = args[0].toLowerCase();
            processCommand(player, subCommand, args);
        } catch (IllegalStateException e) {
            MessageUtil.send(player, "&cПлагин перезагружается, попробуйте позже.");
        }
        return true;
    }

    private void processCommand(Player player, String subCommand, String[] args) {

        switch (subCommand) {
            case "create":
                handleCreate(player, args);
                break;
            case "disband":
                handleDisband(player);
                break;
            case "invite":
                handleInvite(player, args);
                break;
            case "accept":
                handleAccept(player, args);
                break;
            case "leave":
                handleLeave(player);
                break;
            case "kick":
                handleKick(player, args);
                break;
            case "promote":
                handlePromote(player, args);
                break;
            case "demote":
                handleDemote(player, args);
                break;
            case "leader":
                handleLeader(player, args);
                break;
            case "home":
                handleHome(player);
                break;
            case "sethome":
                handleSetHome(player);
                break;
            case "delhome":
            case "removehome":
                handleDelHome(player);
                break;
            case "chat":
            case "c":
                handleChatToggle(player);
                break;
            case "info":
                handleInfo(player, args);
                break;
            case "top":
                getGuiManager().openTopMenu(player);
                break;
            case "menu":
                getGuiManager().openMainMenu(player);
                break;
            case "storage":
                handleStorage(player);
                break;
            case "color":
                handleColor(player, args);
                break;
            case "pvp":
                handlePvpToggle(player);
                break;
            case "bank":
                handleBank(player, args);
                break;
            case "admin":
                if (player.hasPermission("clan.admin")) {
                    getGuiManager().openAdminMenu(player);
                } else {
                    MessageUtil.send(player, "&cУ вас нет прав на эту команду!");
                }
                break;
            case "help":
            default:
                sendHelp(player);
                break;
        }
    }

    private void handleCreate(Player player, String[] args) {
        String usageMsg = plugin.getConfigManager().getMessage("usage.create");
        if (args.length < 3) {
            if (usageMsg != null) {
                MessageUtil.send(player, usageMsg);
            }
            return;
        }

        String alreadyInMsg = plugin.getConfigManager().getMessage("errors.already-in-clan");
        if (plugin.getClanManager().getPlayerClan(player.getUniqueId()) != null) {
            if (alreadyInMsg != null) {
                MessageUtil.send(player, alreadyInMsg);
            }
            return;
        }

        // Check clan creation cost
        double creationCost = plugin.getConfig().getDouble("clan.creation-cost", 0.0);
        if (creationCost > 0 && plugin.isVaultEnabled()) {
            if (!plugin.getVaultManager().hasEnough(player, creationCost)) {
                String fundsMsg = plugin.getConfigManager().getMessage("errors.insufficient-funds");
                if (fundsMsg != null) {
                    MessageUtil.send(player, fundsMsg.replace("%amount%", String.format("%.2f", creationCost)));
                }
                return;
            }
        }

        String name = args[1];
        String tag = args[2].toUpperCase();

        int minName = plugin.getConfigManager().getMinClanNameLength();
        int maxName = plugin.getConfigManager().getMaxClanNameLength();
        int minTag = plugin.getConfigManager().getMinTagLength();
        int maxTag = plugin.getConfigManager().getMaxTagLength();

        if (name.length() < minName || name.length() > maxName) {
            MessageUtil.send(player, "&cНазвание должно быть от " + minName + " до " + maxName + " символов!");
            return;
        }

        if (tag.length() < minTag || tag.length() > maxTag) {
            MessageUtil.send(player, "&cТег должен быть от " + minTag + " до " + maxTag + " символов!");
            return;
        }

        if (!name.matches("[a-zA-Zа-яА-Я0-9_]+")) {
            MessageUtil.send(player, "&cНазвание содержит недопустимые символы!");
            return;
        }

        if (!tag.matches("[a-zA-Zа-яА-Я0-9]+")) {
            MessageUtil.send(player, "&cТег содержит недопустимые символы!");
            return;
        }

        if (plugin.getClanManager().isNameTaken(name)) {
            MessageUtil.send(player, "&cКлан с таким названием уже существует!");
            return;
        }

        if (plugin.getClanManager().isTagTaken(tag)) {
            MessageUtil.send(player, "&cКлан с таким тегом уже существует!");
            return;
        }

        Clan clan = plugin.getClanManager().createClan(name, tag, player);
        if (clan == null) return; // createClan handles its own errors or nulls

        String msg = plugin.getConfigManager().getMessage("clan.created");
        if (msg != null && !msg.isEmpty()) {
            // Deduct creation cost
            if (creationCost > 0 && plugin.isVaultEnabled()) {
                plugin.getVaultManager().withdraw(player, creationCost);
                MessageUtil.send(player, "&aСписано &f" + String.format("%.2f", creationCost) + " &aза создание клана");
            }

            MessageUtil.send(player, msg
                    .replace("%clan%", name)
                    .replace("%tag%", tag));
        }
    }

    private void handleDisband(Player player) {
        Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
        if (clan == null) {
            MessageUtil.send(player, "&cВы не состоите в клане!");
            return;
        }

        if (!clan.isLeader(player.getUniqueId())) {
            MessageUtil.send(player, "&cТолько лидер может расформировать клан!");
            return;
        }

        Long lastConfirm = confirmDisband.get(player.getUniqueId());
        if (lastConfirm == null || System.currentTimeMillis() - lastConfirm > 30000) {
            confirmDisband.put(player.getUniqueId(), System.currentTimeMillis());
            MessageUtil.send(player, "&cВы уверены? Введите команду ещё раз в течение 30 секунд для подтверждения!");
            return;
        }

        confirmDisband.remove(player.getUniqueId());
        plugin.getClanManager().disbandClan(clan);
    }

    private void handleInvite(Player player, String[] args) {
        Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
        if (clan == null) {
            MessageUtil.send(player, "&cВы не состоите в клане!");
            return;
        }

        ClanMember member = clan.getMember(player.getUniqueId());
        if (!member.hasPermission(ClanPermission.INVITE)) {
            MessageUtil.send(player, "&cУ вас нет прав приглашать игроков!");
            return;
        }

        if (args.length < 2) {
            MessageUtil.send(player, "&cИспользуйте: /clan invite <игрок>");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            MessageUtil.send(player, "&cИгрок не найден или не в сети!");
            return;
        }

        if (target.getUniqueId().equals(player.getUniqueId())) {
            MessageUtil.send(player, "&cВы не можете пригласить себя!");
            return;
        }

        int maxMembers = plugin.getConfigManager().getMaxMembersForLevel(clan.getLevel());
        if (clan.getMemberCount() >= maxMembers) {
            MessageUtil.send(player, "&cВ клане достигнут лимит участников! Повысьте уровень клана.");
            return;
        }

        plugin.getClanManager().invitePlayer(clan, player, target);
    }

    private void handleAccept(Player player, String[] args) {
        if (plugin.getClanManager().isInClan(player.getUniqueId())) {
            MessageUtil.send(player, "&cВы уже состоите в клане!");
            return;
        }

        if (args.length < 2) {
            MessageUtil.send(player, "&cИспользуйте: /clan accept <тег клана>");
            return;
        }

        plugin.getClanManager().acceptInvite(player, args[1]);
    }

    private void handleLeave(Player player) {
        plugin.getClanManager().leaveClan(player);
    }

    private void handleKick(Player player, String[] args) {
        Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
        if (clan == null) {
            MessageUtil.send(player, "&cВы не состоите в клане!");
            return;
        }

        ClanMember member = clan.getMember(player.getUniqueId());
        if (!member.hasPermission(ClanPermission.KICK)) {
            MessageUtil.send(player, "&cУ вас нет прав исключать игроков!");
            return;
        }

        if (args.length < 2) {
            MessageUtil.send(player, "&cИспользуйте: /clan kick <игрок>");
            return;
        }

        ClanMember target = null;
        for (ClanMember m : clan.getMembers().values()) {
            if (m.getName().equalsIgnoreCase(args[1])) {
                target = m;
                break;
            }
        }

        if (target == null) {
            MessageUtil.send(player, "&cИгрок не найден в клане!");
            return;
        }

        if (target.getUuid().equals(player.getUniqueId())) {
            MessageUtil.send(player, "&cВы не можете исключить себя!");
            return;
        }

        if (target.getRank().isHigherOrEqual(member.getRank())) {
            MessageUtil.send(player, "&cВы не можете исключить игрока с таким же или выше рангом!");
            return;
        }

        plugin.getClanManager().removeMember(clan, target.getUuid());
        MessageUtil.send(player, "&aИгрок &f" + target.getName() + " &aбыл исключен из клана!");
    }

    private void handlePromote(Player player, String[] args) {
        Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
        if (clan == null) {
            MessageUtil.send(player, "&cВы не состоите в клане!");
            return;
        }

        ClanMember member = clan.getMember(player.getUniqueId());
        if (!member.hasPermission(ClanPermission.PROMOTE)) {
            MessageUtil.send(player, "&cУ вас нет прав повышать игроков!");
            return;
        }

        if (args.length < 2) {
            MessageUtil.send(player, "&cИспользуйте: /clan promote <игрок>");
            return;
        }

        ClanMember target = null;
        for (ClanMember m : clan.getMembers().values()) {
            if (m.getName().equalsIgnoreCase(args[1])) {
                target = m;
                break;
            }
        }

        if (target == null) {
            MessageUtil.send(player, "&cИгрок не найден в клане!");
            return;
        }

        plugin.getClanManager().promoteMember(clan, player.getUniqueId(), target.getUuid());
    }

    private void handleDemote(Player player, String[] args) {
        Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
        if (clan == null) {
            MessageUtil.send(player, "&cВы не состоите в клане!");
            return;
        }

        ClanMember member = clan.getMember(player.getUniqueId());
        if (!member.hasPermission(ClanPermission.DEMOTE)) {
            MessageUtil.send(player, "&cУ вас нет прав понижать игроков!");
            return;
        }

        if (args.length < 2) {
            MessageUtil.send(player, "&cИспользуйте: /clan demote <игрок>");
            return;
        }

        ClanMember target = null;
        for (ClanMember m : clan.getMembers().values()) {
            if (m.getName().equalsIgnoreCase(args[1])) {
                target = m;
                break;
            }
        }

        if (target == null) {
            MessageUtil.send(player, "&cИгрок не найден в клане!");
            return;
        }

        plugin.getClanManager().demoteMember(clan, player.getUniqueId(), target.getUuid());
    }

    private void handleLeader(Player player, String[] args) {
        Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
        if (clan == null) {
            MessageUtil.send(player, "&cВы не состоите в клане!");
            return;
        }

        if (!clan.isLeader(player.getUniqueId())) {
            MessageUtil.send(player, "&cТолько лидер может передать лидерство!");
            return;
        }

        if (args.length < 2) {
            MessageUtil.send(player, "&cИспользуйте: /clan leader <игрок>");
            return;
        }

        ClanMember target = null;
        for (ClanMember m : clan.getMembers().values()) {
            if (m.getName().equalsIgnoreCase(args[1])) {
                target = m;
                break;
            }
        }

        if (target == null) {
            MessageUtil.send(player, "&cИгрок не найден в клане!");
            return;
        }

        if (target.getUuid().equals(player.getUniqueId())) {
            MessageUtil.send(player, "&cВы уже являетесь лидером!");
            return;
        }

        plugin.getClanManager().transferLeadership(clan, target.getUuid());
    }

    private void handleHome(Player player) {
        Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
        if (clan == null) {
            MessageUtil.send(player, "&cВы не состоите в клане!");
            return;
        }

        ClanMember member = clan.getMember(player.getUniqueId());
        if (!member.hasPermission(ClanPermission.HOME)) {
            MessageUtil.send(player, "&cУ вас нет прав телепортироваться на базу!");
            return;
        }

        if (!clan.hasHome()) {
            MessageUtil.send(player, "&cДом клана не установлен!");
            return;
        }

        int cooldown = plugin.getConfigManager().getHomeCooldown();
        Long lastUse = homeCooldowns.get(player.getUniqueId());
        if (lastUse != null) {
            long remaining = (cooldown * 1000) - (System.currentTimeMillis() - lastUse);
            if (remaining > 0) {
                MessageUtil.send(player, "&cПодождите ещё &f" + (remaining / 1000) + " &cсекунд!");
                return;
            }
        }

        homeCooldowns.put(player.getUniqueId(), System.currentTimeMillis());
        player.teleport(clan.getHome());
        MessageUtil.send(player, "&aВы телепортированы на базу клана!");
    }

    private void handleSetHome(Player player) {
        Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
        if (clan == null) {
            MessageUtil.send(player, "&cВы не состоите в клане!");
            return;
        }

        ClanMember member = clan.getMember(player.getUniqueId());
        if (!member.hasPermission(ClanPermission.SET_HOME)) {
            MessageUtil.send(player, "&cУ вас нет прав устанавливать базу!");
            return;
        }

        clan.setHome(player.getLocation());
        plugin.getDataManager().saveClan(clan);
        MessageUtil.sendClanMessage(clan, "&aБаза клана была установлена игроком &f" + player.getName());
    }

    private void handleDelHome(Player player) {
        Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
        if (clan == null) {
            MessageUtil.send(player, "&cВы не состоите в клане!");
            return;
        }

        ClanMember member = clan.getMember(player.getUniqueId());
        if (!member.hasPermission(ClanPermission.SET_HOME)) {
            MessageUtil.send(player, "&cУ вас нет прав удалять базу!");
            return;
        }

        if (!clan.hasHome()) {
            MessageUtil.send(player, "&cБаза клана не установлена!");
            return;
        }

        clan.setHome(null);
        plugin.getDataManager().saveClan(clan);
        MessageUtil.sendClanMessage(clan, "&cБаза клана была удалена игроком &f" + player.getName());
    }

    private void handleChatToggle(Player player) {
        if (!plugin.getClanManager().isInClan(player.getUniqueId())) {
            MessageUtil.send(player, "&cВы не состоите в клане!");
            return;
        }

        plugin.getClanManager().toggleClanChat(player.getUniqueId());
        boolean enabled = plugin.getClanManager().isClanChatEnabled(player.getUniqueId());
        MessageUtil.send(player, enabled ? "&aКлановый чат включен" : "&cКлановый чат выключен");
    }

    private void handleInfo(Player player, String[] args) {
        Clan clan;
        if (args.length > 1) {
            clan = plugin.getClanManager().getClanByTag(args[1]);
            if (clan == null) {
                clan = plugin.getClanManager().getClanByName(args[1]);
            }
        } else {
            clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
        }

        if (clan == null) {
            MessageUtil.send(player, "&cКлан не найден!");
            return;
        }

        String hexColor = clan.getHexColor();
        MessageUtil.sendRaw(player, "");
        MessageUtil.sendRaw(player, ColorUtil.getHexColor(hexColor) + "&l" + clan.getName() + " &7[" + clan.getTag() + "]");
        MessageUtil.sendRaw(player, "&7Уровень: &f" + clan.getLevel() + " &8| &7Опыт: &f" + MessageUtil.formatNumber(clan.getExperience()));
        MessageUtil.sendRaw(player, "&7Участников: &f" + clan.getMemberCount() + "/" + plugin.getConfigManager().getMaxMembersForLevel(clan.getLevel()));
        MessageUtil.sendRaw(player, "&7Убийств: &a" + clan.getKills() + " &8| &7Смертей: &c" + clan.getDeaths() + " &8| &7K/D: &f" + String.format("%.2f", clan.getKDR()));

        ClanMember leader = clan.getMember(clan.getLeader());
        if (leader != null) {
            MessageUtil.sendRaw(player, "&7Лидер: &f" + leader.getName());
        }
        MessageUtil.sendRaw(player, "");
    }

    private void handleStorage(Player player) {
        Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
        if (clan == null) {
            MessageUtil.send(player, "&cВы не состоите в клане!");
            return;
        }

        getGuiManager().openStorageMenu(player, clan, 0);
    }

    private void handleColor(Player player, String[] args) {
        Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
        if (clan == null) {
            MessageUtil.send(player, "&cВы не состоите в клане!");
            return;
        }

        ClanMember member = clan.getMember(player.getUniqueId());
        if (!member.hasPermission(ClanPermission.CHANGE_SETTINGS)) {
            MessageUtil.send(player, "&cУ вас нет прав менять настройки!");
            return;
        }

        if (args.length < 2) {
            MessageUtil.send(player, "&cИспользуйте: /clan color <HEX>");
            MessageUtil.send(player, "&7Пример: /clan color #FF5733");
            return;
        }

        String hex = args[1];
        if (!hex.startsWith("#")) {
            hex = "#" + hex;
        }

        if (!ColorUtil.isValidHex(hex)) {
            MessageUtil.send(player, "&cНеверный HEX цвет! Пример: #FF5733");
            return;
        }

        clan.setHexColor(hex);
        plugin.getDataManager().saveClan(clan);
        MessageUtil.sendClanMessage(clan, "&aЦвет клана изменен на " + ColorUtil.getHexColor(hex) + hex);
    }

    private void handlePvpToggle(Player player) {
        Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
        if (clan == null) {
            MessageUtil.send(player, "&cВы не состоите в клане!");
            return;
        }

        ClanMember member = clan.getMember(player.getUniqueId());
        if (!member.hasPermission(ClanPermission.CHANGE_SETTINGS)) {
            MessageUtil.send(player, "&cУ вас нет прав менять настройки!");
            return;
        }

        clan.setPvpEnabled(!clan.isPvpEnabled());
        plugin.getDataManager().saveClan(clan);
        MessageUtil.sendClanMessage(clan, clan.isPvpEnabled() ? "&aPVP внутри клана включен" : "&cPVP внутри клана выключен");
    }

    private void handleBank(Player player, String[] args) {
        Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
        if (clan == null) {
            MessageUtil.send(player, "&cВы не состоите в клане!");
            return;
        }

        if (!plugin.isVaultEnabled()) {
            MessageUtil.send(player, "&cЭкономика недоступна!");
            return;
        }

        if (args.length < 2) {
            getGuiManager().openBankMenu(player, clan);
            return;
        }

        String action = args[1].toLowerCase();
        ClanMember member = clan.getMember(player.getUniqueId());

        switch (action) {
            case "deposit":
            case "вложить":
                if (!member.hasPermission(ClanPermission.BANK_DEPOSIT)) {
                    MessageUtil.send(player, "&cУ вас нет прав вносить деньги в казну!");
                    return;
                }
                if (args.length < 3) {
                    MessageUtil.send(player, "&cИспользуйте: /clan bank deposit <сумма>");
                    return;
                }
                handleBankDeposit(player, clan, args[2]);
                break;

            case "withdraw":
            case "снять":
                if (!member.hasPermission(ClanPermission.BANK_WITHDRAW)) {
                    MessageUtil.send(player, "&cУ вас нет прав снимать деньги из казны!");
                    return;
                }
                if (args.length < 3) {
                    MessageUtil.send(player, "&cИспользуйте: /clan bank withdraw <сумма>");
                    return;
                }
                handleBankWithdraw(player, clan, args[2]);
                break;

            case "history":
            case "история":
                handleBankHistory(player, clan);
                break;

            case "balance":
            case "баланс":
                String balance = plugin.getVaultManager().format(clan.getBank());
                MessageUtil.send(player, "&7Баланс казны клана: &a" + balance);
                break;

            default:
                getGuiManager().openBankMenu(player, clan);
                break;
        }
    }

    private void handleBankDeposit(Player player, Clan clan, String amountStr) {
        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            MessageUtil.send(player, "&cНеверная сумма!");
            return;
        }

        double minDeposit = plugin.getConfigManager().getMinDeposit();
        if (amount < minDeposit) {
            MessageUtil.send(player, "&cМинимальная сумма для внесения: &f" + plugin.getVaultManager().format(minDeposit));
            return;
        }

        if (plugin.getVaultManager().transferToClanBank(player, clan, amount)) {
            String formatted = plugin.getVaultManager().format(amount);
            MessageUtil.send(player, "&aВы внесли &f" + formatted + " &aв казну клана!");
            MessageUtil.sendClanMessage(clan, "&a" + player.getName() + " &7внес &a" + formatted + " &7в казну");
        } else {
            MessageUtil.send(player, "&cНедостаточно средств!");
        }
    }

    private void handleBankWithdraw(Player player, Clan clan, String amountStr) {
        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            MessageUtil.send(player, "&cНеверная сумма!");
            return;
        }

        if (amount <= 0) {
            MessageUtil.send(player, "&cСумма должна быть больше нуля!");
            return;
        }

        double minWithdraw = plugin.getConfigManager().getMinWithdraw();
        if (amount < minWithdraw) {
            MessageUtil.send(player, "&cМинимальная сумма для снятия: &f" + plugin.getVaultManager().format(minWithdraw));
            return;
        }

        // Check if clan has enough money
        if (clan.getBank() < amount) {
            MessageUtil.send(player, "&cВ казне недостаточно средств! Баланс: &f" + plugin.getVaultManager().format(clan.getBank()));
            return;
        }

        double tax = plugin.getVaultManager().getWithdrawTax();
        if (tax > 0) {
            double actualAmount = plugin.getVaultManager().applyWithdrawTax(amount);
            if (plugin.getVaultManager().withdrawFromClanBank(player, clan, amount)) {
                String formatted = plugin.getVaultManager().format(actualAmount);
                MessageUtil.send(player, "&aВы сняли &f" + formatted + " &aиз казны клана! (Налог: " + tax + "%)");
                MessageUtil.sendClanMessage(clan, "&c" + player.getName() + " &7снял &c" + plugin.getVaultManager().format(actualAmount) + " &7из казны");
            } else {
                MessageUtil.send(player, "&cОшибка при снятии средств!");
            }
        } else {
            if (plugin.getVaultManager().withdrawFromClanBank(player, clan, amount)) {
                String formatted = plugin.getVaultManager().format(amount);
                MessageUtil.send(player, "&aВы сняли &f" + formatted + " &aиз казны клана!");
                MessageUtil.sendClanMessage(clan, "&c" + player.getName() + " &7снял &c" + formatted + " &7из казны");
            } else {
                MessageUtil.send(player, "&cОшибка при снятии средств!");
            }
        }
    }

    private void handleBankHistory(Player player, Clan clan) {
        java.util.List<BankTransaction> history = clan.getRecentTransactions(10);

        if (history.isEmpty()) {
            MessageUtil.send(player, "&7История транзакций пуста");
            return;
        }

        MessageUtil.sendRaw(player, "");
        MessageUtil.sendRaw(player, "&6&lИстория транзакций казны:");
        MessageUtil.sendRaw(player, "&8━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        for (BankTransaction transaction : history) {
            String type = transaction.getType() == BankTransaction.TransactionType.DEPOSIT ? "&a⬇" : "&c⬆";
            String amount = plugin.getVaultManager().format(transaction.getAmount());
            MessageUtil.sendRaw(player, type + " &f" + transaction.getPlayerName() + " &8| &f" + amount + " &8| &7" + transaction.getFormattedTime());
        }
        MessageUtil.sendRaw(player, "");
    }

    private void sendHelp(Player player) {
        MessageUtil.sendRaw(player, "");
        MessageUtil.sendRaw(player, "&6&lКоманды клана:");
        MessageUtil.sendRaw(player, "&e/clan create <название> <тег> &7- Создать клан");
        MessageUtil.sendRaw(player, "&e/clan menu &7- Открыть меню клана");
        MessageUtil.sendRaw(player, "&e/clan info [клан] &7- Информация о клане");
        MessageUtil.sendRaw(player, "&e/clan invite <игрок> &7- Пригласить игрока");
        MessageUtil.sendRaw(player, "&e/clan accept <тег> &7- Принять приглашение");
        MessageUtil.sendRaw(player, "&e/clan leave &7- Покинуть клан");
        MessageUtil.sendRaw(player, "&e/clan home &7- Телепортация на базу");
        MessageUtil.sendRaw(player, "&e/clan sethome &7- Установить базу");
        MessageUtil.sendRaw(player, "&e/clan delhome &7- Удалить базу");
        MessageUtil.sendRaw(player, "&e/clan chat &7- Переключить клановый чат");
        MessageUtil.sendRaw(player, "&e/clan top &7- Топ кланов");
        MessageUtil.sendRaw(player, "&e/clan storage &7- Хранилище клана");
        MessageUtil.sendRaw(player, "&e/clan color <HEX> &7- Изменить цвет клана");
        MessageUtil.sendRaw(player, "&e/clan bank &7- Открыть казну клана");
        MessageUtil.sendRaw(player, "&e/clan bank deposit <сумма> &7- Внести деньги");
        MessageUtil.sendRaw(player, "&e/clan bank withdraw <сумма> &7- Снять деньги");
        MessageUtil.sendRaw(player, "&e/clan bank history &7- История транзакций");
        MessageUtil.sendRaw(player, "");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subCommands = Arrays.asList(
                "create", "disband", "invite", "accept", "leave", "kick",
                "promote", "demote", "leader", "home", "sethome", "delhome", "chat",
                "info", "top", "menu", "storage", "color", "pvp", "bank", "help"
            );
            for (String sub : subCommands) {
                if (sub.startsWith(args[0].toLowerCase())) {
                    completions.add(sub);
                }
            }
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("bank")) {
                List<String> bankActions = Arrays.asList("deposit", "withdraw", "history", "balance");
                for (String action : bankActions) {
                    if (action.startsWith(args[1].toLowerCase())) {
                        completions.add(action);
                    }
                }
            } else if (sub.equals("invite")) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(p.getName());
                    }
                }
            } else if (sub.equals("kick") || sub.equals("promote") || sub.equals("demote") || sub.equals("leader")) {
                if (sender instanceof Player) {
                    Clan clan = plugin.getClanManager().getPlayerClan(((Player) sender).getUniqueId());
                    if (clan != null) {
                        for (ClanMember m : clan.getMembers().values()) {
                            if (m.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                                completions.add(m.getName());
                            }
                        }
                    }
                }
            } else if (sub.equals("accept") || sub.equals("info")) {
                for (Clan clan : plugin.getClanManager().getClans()) {
                    if (clan.getTag().toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(clan.getTag());
                    }
                }
            }
        }

        return completions;
    }
}