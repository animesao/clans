package ru.clans.managers;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import ru.clans.ClanPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ConfigManager {

    private final ClanPlugin plugin;
    private FileConfiguration config;
    private FileConfiguration levelsConfig;
    private FileConfiguration questsConfig;
    private FileConfiguration messagesConfig;
    private FileConfiguration colorsConfig;
    private FileConfiguration rolesConfig;

    public ConfigManager(ClanPlugin plugin) {
        this.plugin = plugin;
        loadConfigs();
    }

    private void loadConfigs() {
        plugin.saveDefaultConfig();
        config = plugin.getConfig();
        
        levelsConfig = loadOrCreate("levels.yml");
        questsConfig = loadOrCreate("quests.yml");
        messagesConfig = loadOrCreate("messages.yml");
        colorsConfig = loadOrCreate("colors.yml");
        rolesConfig = loadOrCreate("roles.yml");
    }

    private FileConfiguration loadOrCreate(String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) {
            plugin.saveResource(fileName, false);
        }
        return YamlConfiguration.loadConfiguration(file);
    }

    public void reloadConfigs() {
        plugin.reloadConfig();
        config = plugin.getConfig();
        levelsConfig = loadOrCreate("levels.yml");
        questsConfig = loadOrCreate("quests.yml");
        messagesConfig = loadOrCreate("messages.yml");
        colorsConfig = loadOrCreate("colors.yml");
        rolesConfig = loadOrCreate("roles.yml");
    }

    public FileConfiguration getRolesConfig() {
        return rolesConfig;
    }

    public FileConfiguration getColorsConfig() {
        if (colorsConfig == null) {
            colorsConfig = loadOrCreate("colors.yml");
        }
        return colorsConfig;
    }

    public int getMaxClanNameLength() {
        return config.getInt("clan.max-name-length", 16);
    }

    public int getMinClanNameLength() {
        return config.getInt("clan.min-name-length", 3);
    }

    public int getMaxTagLength() {
        return config.getInt("clan.max-tag-length", 5);
    }

    public int getMinTagLength() {
        return config.getInt("clan.min-tag-length", 2);
    }

    public int getBaseMemberLimit() {
        return config.getInt("clan.base-member-limit", 10);
    }

    public int getMembersPerLevel() {
        return config.getInt("clan.members-per-level", 2);
    }

    public long getInviteExpireTime() {
        return config.getLong("clan.invite-expire-time", 300) * 1000;
    }

    public int getHomeCooldown() {
        return config.getInt("clan.home-cooldown", 30);
    }

    public int getMaxStorageLevel() {
        return config.getInt("storage.upgrades.max-level", 5);
    }

    public int getStorageSlotsPerLevel() {
        return config.getInt("storage.upgrades.slots-per-level", 9);
    }

    public int getStorageSlotsForLevel(int level) {
        int slots = level * getStorageSlotsPerLevel();
        return Math.min(slots, 27);
    }

    public int getStoragePageLimit(int clanLevel) {
        org.bukkit.configuration.ConfigurationSection section = config.getConfigurationSection("storage.page-limits");
        if (section == null) return 1;

        int limit = section.getInt("default", 1);
        
        // Ищем максимально подходящий уровень в конфиге
        // Например, если в конфиге есть 1, 5, 10, а у клана 7 уровень - вернет значение для 5.
        int bestLevel = -1;
        for (String key : section.getKeys(false)) {
            if (key.equalsIgnoreCase("default")) continue;
            try {
                int lvl = Integer.parseInt(key);
                if (clanLevel >= lvl && lvl > bestLevel) {
                    bestLevel = lvl;
                    limit = section.getInt(key);
                }
            } catch (NumberFormatException ignored) {}
        }
        
        return Math.max(1, limit);
    }

    public String getStorageMessage(String key) {
        return config.getString("storage.messages." + key, "");
    }

    public int getStorageUpgradeCost(int level) {
        return levelsConfig.getInt("storage-upgrades." + level + ".cost", level * 10000);
    }

    public int getStorageUpgradeRequiredLevel(int level) {
        return levelsConfig.getInt("storage-upgrades." + level + ".required-level", level * 2);
    }

    public long getExperienceForLevel(int level) {
        return levelsConfig.getLong("levels." + level + ".experience", level * 1000L);
    }

    public int getMaxMembersForLevel(int level) {
        return getBaseMemberLimit() + (level - 1) * getMembersPerLevel();
    }

    public Map<String, Object> getLevelRewards(int level) {
        Map<String, Object> rewards = new HashMap<>();
        if (levelsConfig.contains("levels." + level + ".rewards")) {
            rewards.put("members", levelsConfig.getInt("levels." + level + ".rewards.members", 0));
            rewards.put("storage", levelsConfig.getBoolean("levels." + level + ".rewards.storage-upgrade", false));
        }
        return rewards;
    }

    public int getMaxLevel() {
        return levelsConfig.getInt("max-level", 50);
    }

    public String getLevelColor(int level) {
        return levelsConfig.getString("levels." + level + ".color", "#FFFFFF");
    }

    public String getLevelPrefix(int level) {
        return levelsConfig.getString("levels." + level + ".prefix", "");
    }

    public String getLevelSuffix(int level) {
        return levelsConfig.getString("levels." + level + ".suffix", "");
    }

    public FileConfiguration getLevelsConfig() {
        return levelsConfig;
    }

    public FileConfiguration getQuestsConfig() {
        return questsConfig;
    }

    public FileConfiguration getMessagesConfig() {
        return messagesConfig;
    }

    public String getMessage(String key) {
        String path = "messages." + key;
        if (!messagesConfig.contains(path)) {
            return "&cСообщение не найдено: " + key;
        }

        // Если значение является булевым и оно false - отключаем сообщение
        if (messagesConfig.isBoolean(path) && !messagesConfig.getBoolean(path)) {
            return null;
        }

        // Если это список (старый формат [сообщение, статус])
        if (messagesConfig.isList(path)) {
            java.util.List<?> list = messagesConfig.getList(path);
            if (list.size() >= 2) {
                Object msgObj = list.get(0);
                Object statusObj = list.get(1);
                
                if (statusObj instanceof Boolean && !((Boolean) statusObj)) {
                    return null;
                }
                return String.valueOf(msgObj);
            }
        }

        return messagesConfig.getString(path);
    }

    public boolean isAllowedWorld(String worldName) {
        if (!config.getBoolean("world-restrictions.enabled", false)) {
            return true;
        }
        return config.getStringList("world-restrictions.allowed-worlds").contains(worldName);
    }

    public boolean isBankEnabled() {
        return config.getBoolean("bank.enabled", true);
    }

    public double getWithdrawTax() {
        return config.getDouble("bank.withdraw-tax", 0);
    }

    public double getMinDeposit() {
        return config.getDouble("bank.min-deposit", 10);
    }

    public double getMinWithdraw() {
        return config.getDouble("bank.min-withdraw", 10);
    }

    public double getCreationCost() {
        return config.getDouble("clan.creation-cost", 5000);
    }

    public boolean isCreationCostEnabled() {
        return config.getBoolean("clan.creation-cost-enabled", true);
    }
}
