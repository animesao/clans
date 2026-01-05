package ru.clans.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import ru.clans.ClanPlugin;
import ru.clans.models.Clan;
import ru.clans.models.ClanMember;
import ru.clans.models.ClanPermission;
import ru.clans.utils.ColorUtil;
import ru.clans.utils.MessageUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ChatListener implements Listener {

    private final ClanPlugin plugin;
    private final Map<UUID, String> awaitingInput;

    public ChatListener(ClanPlugin plugin) {
        this.plugin = plugin;
        this.awaitingInput = new HashMap<>();
    }

    public void expectInput(UUID uuid, String type) {
        awaitingInput.put(uuid, type);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (awaitingInput.containsKey(uuid)) {
            String type = awaitingInput.remove(uuid);
            event.setCancelled(true);
            
            String message = event.getMessage();
            double amount;
            try {
                amount = Double.parseDouble(message);
                if (amount <= 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                MessageUtil.send(player, "&cНеверная сумма! Введите положительное число.");
                return;
            }

            Clan clan = plugin.getClanManager().getPlayerClan(uuid);
            if (clan == null) return;

            if (type.equals("bank_deposit")) {
                if (plugin.getVaultManager().hasEnough(player, amount)) {
                    plugin.getVaultManager().withdraw(player, amount);
                    clan.depositBank(amount, uuid, player.getName());
                    plugin.getDataManager().saveClan(clan);
                    MessageUtil.send(player, "&aВы положили &f" + MessageUtil.formatNumber((long)amount) + " &aв казну клана!");
                } else {
                    MessageUtil.send(player, "&cУ вас недостаточно денег!");
                }
            } else if (type.equals("bank_withdraw")) {
                if (!clan.getMember(uuid).hasPermission(ClanPermission.BANK_WITHDRAW)) {
                    MessageUtil.send(player, "&cУ вас недостаточно прав для снятия денег!");
                    return;
                }
                if (clan.withdrawBank(amount, uuid, player.getName())) {
                    plugin.getVaultManager().deposit(player, amount);
                    plugin.getDataManager().saveClan(clan);
                    MessageUtil.send(player, "&aВы сняли &f" + MessageUtil.formatNumber((long)amount) + " &aиз казны клана!");
                } else {
                    MessageUtil.send(player, "&cВ казне недостаточно денег!");
                }
            }
            return;
        }
        
        if (plugin.getClanManager().isClanChatEnabled(player.getUniqueId())) {
            Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
            if (clan != null) {
                ClanMember member = clan.getMember(player.getUniqueId());
                if (member != null && member.hasPermission(ClanPermission.CHAT)) {
                    event.setCancelled(true);
                    MessageUtil.sendClanChat(clan, player, event.getMessage());
                    return;
                }
            }
        }
        
        Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
        if (clan != null) {
            String hexColor = clan.getHexColor();
            String tag = ColorUtil.colorize("&7[" + ColorUtil.getHexColor(hexColor) + clan.getTag() + "&7] ");
            event.setFormat(tag + event.getFormat());
        }
    }
}
