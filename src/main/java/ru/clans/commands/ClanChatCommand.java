package ru.clans.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.clans.ClanPlugin;
import ru.clans.models.Clan;
import ru.clans.models.ClanMember;
import ru.clans.models.ClanPermission;
import ru.clans.utils.MessageUtil;

public class ClanChatCommand implements CommandExecutor {

    private final ClanPlugin plugin;

    public ClanChatCommand(ClanPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Только для игроков!");
            return true;
        }

        Player player = (Player) sender;
        Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());

        if (clan == null) {
            MessageUtil.send(player, "&cВы не состоите в клане!");
            return true;
        }

        ClanMember member = clan.getMember(player.getUniqueId());
        if (member == null || !member.hasPermission(ClanPermission.CHAT)) {
            MessageUtil.send(player, "&cУ вас нет прав писать в клановый чат!");
            return true;
        }

        if (args.length == 0) {
            MessageUtil.send(player, "&cИспользуйте: /cc <сообщение>");
            return true;
        }

        String message = String.join(" ", args);
        MessageUtil.sendClanChat(clan, player, message);

        return true;
    }
}
