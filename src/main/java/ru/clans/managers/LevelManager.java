package ru.clans.managers;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import ru.clans.ClanPlugin;
import ru.clans.models.Clan;
import ru.clans.models.ClanMember;
import ru.clans.utils.MessageUtil;

import java.util.Map;

public class LevelManager {

    private final ClanPlugin plugin;

    public LevelManager(ClanPlugin plugin) {
        this.plugin = plugin;
    }

    public void addExperience(Clan clan, long amount, String reason) {
        clan.addExperience(amount);
        
        checkLevelUp(clan);
        
        plugin.getDataManager().saveClan(clan);
    }

    public void checkLevelUp(Clan clan) {
        int maxLevel = plugin.getConfigManager().getMaxLevel();
        
        while (clan.getLevel() < maxLevel) {
            long requiredExp = getRequiredExperience(clan.getLevel() + 1);
            
            if (clan.getExperience() >= requiredExp) {
                levelUp(clan);
            } else {
                break;
            }
        }
    }

    private void levelUp(Clan clan) {
        int newLevel = clan.getLevel() + 1;
        clan.setLevel(newLevel);
        
        Map<String, Object> rewards = plugin.getConfigManager().getLevelRewards(newLevel);
        applyRewards(clan, rewards);
        
        for (ClanMember member : clan.getMembers().values()) {
            Player player = org.bukkit.Bukkit.getPlayer(member.getUuid());
            if (player != null && player.isOnline()) {
                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                
                MessageUtil.sendRaw(player, "");
                MessageUtil.sendRaw(player, "&6&l★ &e&lКЛАН ПОЛУЧИЛ НОВЫЙ УРОВЕНЬ! &6&l★");
                MessageUtil.sendRaw(player, "&7Ваш клан &f" + clan.getName() + " &7достиг уровня &a" + newLevel + "&7!");
                MessageUtil.sendRaw(player, "");
            }
        }
        
        MessageUtil.broadcast("&6Клан &f" + clan.getName() + " &6достиг &a" + newLevel + " &6уровня!");
    }

    private void applyRewards(Clan clan, Map<String, Object> rewards) {
        if (rewards.containsKey("storage") && (Boolean) rewards.get("storage")) {
            if (clan.getStorageLevel() < plugin.getConfigManager().getMaxStorageLevel()) {
                clan.upgradeStorage();
                MessageUtil.sendClanMessage(clan, "&aХранилище клана было улучшено до уровня &f" + clan.getStorageLevel());
            }
        }
    }

    public long getRequiredExperience(int level) {
        return plugin.getConfigManager().getExperienceForLevel(level);
    }

    public long getExperienceToNextLevel(Clan clan) {
        if (clan.getLevel() >= plugin.getConfigManager().getMaxLevel()) {
            return 0;
        }
        return getRequiredExperience(clan.getLevel() + 1) - clan.getExperience();
    }

    public double getProgressPercent(Clan clan) {
        if (clan.getLevel() >= plugin.getConfigManager().getMaxLevel()) {
            return 100.0;
        }
        
        long currentLevelExp = clan.getLevel() > 1 ? getRequiredExperience(clan.getLevel()) : 0;
        long nextLevelExp = getRequiredExperience(clan.getLevel() + 1);
        long currentExp = clan.getExperience();
        
        long expInLevel = currentExp - currentLevelExp;
        long expNeeded = nextLevelExp - currentLevelExp;
        
        return (double) expInLevel / expNeeded * 100;
    }

    public int getMaxMembersForLevel(int level) {
        return plugin.getConfigManager().getMaxMembersForLevel(level);
    }

    public boolean canUpgradeStorage(Clan clan) {
        int currentLevel = clan.getStorageLevel();
        int maxLevel = plugin.getConfigManager().getMaxStorageLevel();
        
        if (currentLevel >= maxLevel) {
            return false;
        }
        
        int requiredClanLevel = plugin.getConfigManager().getStorageUpgradeRequiredLevel(currentLevel + 1);
        return clan.getLevel() >= requiredClanLevel;
    }

    public int getStorageUpgradeCost(int level) {
        return plugin.getConfigManager().getStorageUpgradeCost(level);
    }
}
