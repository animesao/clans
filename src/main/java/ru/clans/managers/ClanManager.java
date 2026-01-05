package ru.clans.managers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import ru.clans.ClanPlugin;
import ru.clans.models.*;
import ru.clans.utils.MessageUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ClanManager {

    private final ClanPlugin plugin;
    private final Map<String, Clan> clans;
    private final Map<UUID, String> playerClans;
    private final Map<UUID, List<ClanInvite>> pendingInvites;
    private final Set<UUID> clanChatEnabled;

    public ClanManager(ClanPlugin plugin) {
        this.plugin = plugin;
        this.clans = new ConcurrentHashMap<>();
        this.playerClans = new ConcurrentHashMap<>();
        this.pendingInvites = new ConcurrentHashMap<>();
        this.clanChatEnabled = ConcurrentHashMap.newKeySet();
    }

    public Clan createClan(String name, String tag, Player leader) {
        String id = UUID.randomUUID().toString().substring(0, 8);
        Clan clan = new Clan(id, name, tag, leader.getUniqueId());
        
        ClanMember leaderMember = new ClanMember(leader.getUniqueId(), leader.getName(), ClanRank.LEADER);
        clan.addMember(leaderMember);
        
        clans.put(id, clan);
        playerClans.put(leader.getUniqueId(), id);
        
        plugin.getDataManager().saveClan(clan);
        
        String broadcastMsg = plugin.getConfigManager().getMessage("clan-created-broadcast");
        if (broadcastMsg != null && !broadcastMsg.isEmpty()) {
            MessageUtil.broadcast(broadcastMsg
                .replace("%player%", leader.getName())
                .replace("%clan%", name)
                .replace("%tag%", tag));
        }
        
        return clan;
    }

    public void disbandClan(Clan clan) {
        for (UUID memberUuid : clan.getMembers().keySet()) {
            playerClans.remove(memberUuid);
            Player player = Bukkit.getPlayer(memberUuid);
            if (player != null) {
                MessageUtil.send(player, "&cВаш клан &f" + clan.getName() + " &cбыл расформирован!");
            }
        }
        
        clans.remove(clan.getId());
        plugin.getDataManager().deleteClan(clan.getId());
        
        MessageUtil.broadcast("&cКлан &f" + clan.getName() + " &cбыл расформирован!");
    }

    public void addMember(Clan clan, Player player, ClanRank rank) {
        ClanMember member = new ClanMember(player.getUniqueId(), player.getName(), rank);
        clan.addMember(member);
        playerClans.put(player.getUniqueId(), clan.getId());
        
        plugin.getDataManager().saveClan(clan);
        
        MessageUtil.sendClanMessage(clan, "&aИгрок &f" + player.getName() + " &aприсоединился к клану!");
    }

    public void removeMember(Clan clan, UUID playerUuid) {
        ClanMember member = clan.getMember(playerUuid);
        if (member != null) {
            clan.removeMember(playerUuid);
            playerClans.remove(playerUuid);
            
            plugin.getDataManager().saveClan(clan);
            
            Player player = Bukkit.getPlayer(playerUuid);
            if (player != null) {
                MessageUtil.send(player, "&cВы были исключены из клана &f" + clan.getName());
            }
            
            MessageUtil.sendClanMessage(clan, "&cИгрок &f" + member.getName() + " &cпокинул клан!");
        }
    }

    public void leaveClan(Player player) {
        Clan clan = getPlayerClan(player.getUniqueId());
        if (clan == null) return;
        
        if (clan.isLeader(player.getUniqueId())) {
            MessageUtil.send(player, "&cЛидер не может покинуть клан! Передайте лидерство или расформируйте клан.");
            return;
        }
        
        ClanMember member = clan.getMember(player.getUniqueId());
        clan.removeMember(player.getUniqueId());
        playerClans.remove(player.getUniqueId());
        
        plugin.getDataManager().saveClan(clan);
        
        MessageUtil.send(player, "&cВы покинули клан &f" + clan.getName());
        MessageUtil.sendClanMessage(clan, "&cИгрок &f" + player.getName() + " &cпокинул клан!");
    }

    public void invitePlayer(Clan clan, Player inviter, Player target) {
        if (getPlayerClan(target.getUniqueId()) != null) {
            MessageUtil.send(inviter, "&cЭтот игрок уже состоит в клане!");
            return;
        }
        
        List<ClanInvite> invites = pendingInvites.computeIfAbsent(target.getUniqueId(), k -> new ArrayList<>());
        
        invites.removeIf(invite -> invite.getClanId().equals(clan.getId()));
        
        ClanInvite invite = new ClanInvite(clan.getId(), target.getUniqueId(), 
                                           inviter.getUniqueId(), plugin.getConfigManager().getInviteExpireTime());
        invites.add(invite);
        
        MessageUtil.send(inviter, "&aВы пригласили игрока &f" + target.getName() + " &aв клан!");
        MessageUtil.send(target, "&aВас пригласили в клан &f" + clan.getName() + " &7[" + clan.getTag() + "]");
        MessageUtil.send(target, "&aИспользуйте &f/clan accept " + clan.getTag() + " &aчтобы принять!");
    }

    public boolean acceptInvite(Player player, String clanTag) {
        List<ClanInvite> invites = pendingInvites.get(player.getUniqueId());
        if (invites == null || invites.isEmpty()) {
            MessageUtil.send(player, "&cУ вас нет приглашений в кланы!");
            return false;
        }
        
        invites.removeIf(ClanInvite::isExpired);
        
        Clan targetClan = null;
        ClanInvite targetInvite = null;
        
        for (ClanInvite invite : invites) {
            Clan clan = clans.get(invite.getClanId());
            if (clan != null && clan.getTag().equalsIgnoreCase(clanTag)) {
                targetClan = clan;
                targetInvite = invite;
                break;
            }
        }
        
        if (targetClan == null || targetInvite == null) {
            MessageUtil.send(player, "&cПриглашение не найдено или истекло!");
            return false;
        }
        
        int maxMembers = plugin.getConfigManager().getMaxMembersForLevel(targetClan.getLevel());
        if (targetClan.getMemberCount() >= maxMembers) {
            MessageUtil.send(player, "&cВ этом клане достигнут лимит участников!");
            return false;
        }
        
        invites.remove(targetInvite);
        addMember(targetClan, player, ClanRank.RECRUIT);
        
        return true;
    }

    public void promoteMember(Clan clan, UUID promoter, UUID target) {
        ClanMember member = clan.getMember(target);
        if (member == null) return;
        
        ClanMember promoterMember = clan.getMember(promoter);
        if (promoterMember == null) return;
        
        if (!promoterMember.getRank().isHigherThan(member.getRank())) {
            Player promoterPlayer = Bukkit.getPlayer(promoter);
            if (promoterPlayer != null) {
                MessageUtil.send(promoterPlayer, "&cВы не можете повысить этого игрока!");
            }
            return;
        }
        
        ClanRank nextRank = member.getRank().getNextRank();
        if (nextRank == ClanRank.LEADER) {
            nextRank = ClanRank.COLEADER;
        }
        
        member.setRank(nextRank);
        plugin.getDataManager().saveClan(clan);
        
        MessageUtil.sendClanMessage(clan, "&aИгрок &f" + member.getName() + " &aбыл повышен до &f" + nextRank.getDisplayName());
    }

    public void demoteMember(Clan clan, UUID demoter, UUID target) {
        ClanMember member = clan.getMember(target);
        if (member == null) return;
        
        ClanMember demoterMember = clan.getMember(demoter);
        if (demoterMember == null) return;
        
        if (!demoterMember.getRank().isHigherThan(member.getRank())) {
            Player demoterPlayer = Bukkit.getPlayer(demoter);
            if (demoterPlayer != null) {
                MessageUtil.send(demoterPlayer, "&cВы не можете понизить этого игрока!");
            }
            return;
        }
        
        ClanRank prevRank = member.getRank().getPreviousRank();
        member.setRank(prevRank);
        plugin.getDataManager().saveClan(clan);
        
        MessageUtil.sendClanMessage(clan, "&cИгрок &f" + member.getName() + " &cбыл понижен до &f" + prevRank.getDisplayName());
    }

    public void transferLeadership(Clan clan, UUID newLeader) {
        UUID oldLeader = clan.getLeader();
        
        ClanMember oldLeaderMember = clan.getMember(oldLeader);
        if (oldLeaderMember != null) {
            oldLeaderMember.setRank(ClanRank.COLEADER);
        }
        
        ClanMember newLeaderMember = clan.getMember(newLeader);
        if (newLeaderMember != null) {
            newLeaderMember.setRank(ClanRank.LEADER);
        }
        
        clan.setLeader(newLeader);
        plugin.getDataManager().saveClan(clan);
        
        MessageUtil.sendClanMessage(clan, "&aЛидерство клана передано игроку &f" + newLeaderMember.getName());
    }

    public Clan getClan(String id) {
        return clans.get(id);
    }

    public Clan getClanByTag(String tag) {
        for (Clan clan : clans.values()) {
            if (clan.getTag().equalsIgnoreCase(tag)) {
                return clan;
            }
        }
        return null;
    }

    public Clan getClanByName(String name) {
        for (Clan clan : clans.values()) {
            if (clan.getName().equalsIgnoreCase(name)) {
                return clan;
            }
        }
        return null;
    }

    public Clan getPlayerClan(UUID playerUuid) {
        String clanId = playerClans.get(playerUuid);
        if (clanId == null) return null;
        return clans.get(clanId);
    }

    public boolean isInClan(UUID playerUuid) {
        return playerClans.containsKey(playerUuid);
    }

    public boolean isNameTaken(String name) {
        return getClanByName(name) != null;
    }

    public boolean isTagTaken(String tag) {
        return getClanByTag(tag) != null;
    }

    public Collection<Clan> getClans() {
        return clans.values();
    }

    public List<Clan> getTopClans(int limit) {
        return clans.values().stream()
                .sorted((c1, c2) -> {
                    int levelCompare = Integer.compare(c2.getLevel(), c1.getLevel());
                    if (levelCompare != 0) return levelCompare;
                    return Long.compare(c2.getExperience(), c1.getExperience());
                })
                .limit(limit)
                .collect(Collectors.toList());
    }

    public List<Clan> getTopClansByKills(int limit) {
        return clans.values().stream()
                .sorted((c1, c2) -> Integer.compare(c2.getKills(), c1.getKills()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    public void toggleClanChat(UUID playerUuid) {
        if (clanChatEnabled.contains(playerUuid)) {
            clanChatEnabled.remove(playerUuid);
        } else {
            clanChatEnabled.add(playerUuid);
        }
    }

    public boolean isClanChatEnabled(UUID playerUuid) {
        return clanChatEnabled.contains(playerUuid);
    }

    public void loadClan(Clan clan) {
        clans.put(clan.getId(), clan);
        for (UUID memberUuid : clan.getMembers().keySet()) {
            playerClans.put(memberUuid, clan.getId());
        }
    }

    public void unloadClan(String clanId) {
        Clan clan = clans.remove(clanId);
        if (clan != null) {
            for (UUID memberUuid : clan.getMembers().keySet()) {
                playerClans.remove(memberUuid);
            }
        }
    }
}
