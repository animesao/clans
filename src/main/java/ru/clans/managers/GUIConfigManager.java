package ru.clans.managers;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import ru.clans.ClanPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GUIConfigManager {

    private final ClanPlugin plugin;
    private FileConfiguration guiConfig;
    private File guiFile;

    public GUIConfigManager(ClanPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        guiFile = new File(plugin.getDataFolder(), "gui.yml");
        if (!guiFile.exists()) {
            plugin.saveResource("gui.yml", false);
        }
        guiConfig = YamlConfiguration.loadConfiguration(guiFile);
    }

    public void reloadConfig() {
        loadConfig();
    }

    public String getMenuTitle(String menu) {
        return guiConfig.getString(menu + ".title", "&8Меню");
    }

    public int getMenuSize(String menu) {
        return guiConfig.getInt(menu + ".size", 54);
    }

    public boolean shouldFillBorder(String menu) {
        return guiConfig.getBoolean(menu + ".fill-border", true);
    }

    public Material getBorderMaterial(String menu) {
        String materialName = guiConfig.getString(menu + ".border-item.material", "BLACK_STAINED_GLASS_PANE");
        try {
            return Material.valueOf(materialName);
        } catch (IllegalArgumentException e) {
            return Material.BLACK_STAINED_GLASS_PANE;
        }
    }

    public String getBorderName(String menu) {
        return guiConfig.getString(menu + ".border-item.name", " ");
    }

    public int getItemSlot(String menu, String item) {
        return guiConfig.getInt(menu + ".items." + item + ".slot", 0);
    }

    public Material getItemMaterial(String menu, String item) {
        String materialName = guiConfig.getString(menu + ".items." + item + ".material", "STONE");
        try {
            return Material.valueOf(materialName);
        } catch (IllegalArgumentException e) {
            return Material.STONE;
        }
    }

    public String getItemName(String menu, String item) {
        return guiConfig.getString(menu + ".items." + item + ".name", "&fItem");
    }

    public List<String> getItemLore(String menu, String item) {
        return guiConfig.getStringList(menu + ".items." + item + ".lore");
    }

    public String getSkullOwner(String menu, String item) {
        return guiConfig.getString(menu + ".items." + item + ".skull-owner", null);
    }

    public int getBackButtonSlot(String menu) {
        return guiConfig.getInt(menu + ".back-button.slot", 49);
    }

    public Material getBackButtonMaterial(String menu) {
        String materialName = guiConfig.getString(menu + ".back-button.material", "ARROW");
        try {
            return Material.valueOf(materialName);
        } catch (IllegalArgumentException e) {
            return Material.ARROW;
        }
    }

    public String getBackButtonName(String menu) {
        return guiConfig.getString(menu + ".back-button.name", "&7Назад");
    }

    public String getMemberItemName(String menu) {
        return guiConfig.getString(menu + ".member-item.name", "&e%member_name%");
    }

    public List<String> getMemberItemLore(String menu) {
        return guiConfig.getStringList(menu + ".member-item.lore");
    }

    public boolean isMemberOnlineGlow(String menu) {
        return guiConfig.getBoolean(menu + ".member-item.online-glow", true);
    }

    public String getActiveQuestName() {
        return guiConfig.getString("quests-menu.active-quest-item.name", "&a%quest_name%");
    }

    public List<String> getActiveQuestLore() {
        return guiConfig.getStringList("quests-menu.active-quest-item.lore");
    }

    public String getAvailableQuestName() {
        return guiConfig.getString("available-quests-menu.quest-item.name", "&#FFD700%quest_name%");
    }

    public List<String> getAvailableQuestLore() {
        return guiConfig.getStringList("available-quests-menu.quest-item.lore");
    }

    public String getClanItemName() {
        return guiConfig.getString("top-menu.clan-item.name", "&6#%position% %clan_name%");
    }

    public List<String> getClanItemLore() {
        return guiConfig.getStringList("top-menu.clan-item.lore");
    }

    public Material getTopPositionMaterial(int position) {
        String key = "top-menu.position-materials." + position;
        String materialName = guiConfig.getString(key, null);
        if (materialName == null) {
            materialName = guiConfig.getString("top-menu.position-materials.default", "COAL_BLOCK");
        }
        try {
            return Material.valueOf(materialName);
        } catch (IllegalArgumentException e) {
            return Material.COAL_BLOCK;
        }
    }

    public Material getPvpMaterialEnabled() {
        String materialName = guiConfig.getString("settings-menu.items.pvp-toggle.material-enabled", "DIAMOND_SWORD");
        try {
            return Material.valueOf(materialName);
        } catch (IllegalArgumentException e) {
            return Material.DIAMOND_SWORD;
        }
    }

    public Material getPvpMaterialDisabled() {
        String materialName = guiConfig.getString("settings-menu.items.pvp-toggle.material-disabled", "WOODEN_SWORD");
        try {
            return Material.valueOf(materialName);
        } catch (IllegalArgumentException e) {
            return Material.WOODEN_SWORD;
        }
    }

    public String getPvpNameEnabled() {
        return guiConfig.getString("settings-menu.items.pvp-toggle.name-enabled", "&aPVP в клане: &aВКЛЮЧЕН");
    }

    public String getPvpNameDisabled() {
        return guiConfig.getString("settings-menu.items.pvp-toggle.name-disabled", "&cPVP в клане: &cВЫКЛЮЧЕН");
    }

    public int getNavigationSlot(String menu, String button) {
        return guiConfig.getInt(menu + ".navigation." + button + ".slot", 0);
    }

    public Material getNavigationMaterial(String menu, String button) {
        String materialName = guiConfig.getString(menu + ".navigation." + button + ".material", "ARROW");
        try {
            return Material.valueOf(materialName);
        } catch (IllegalArgumentException e) {
            return Material.ARROW;
        }
    }

    public String getNavigationName(String menu, String button) {
        return guiConfig.getString(menu + ".navigation." + button + ".name", "&7Навигация");
    }

    public ConfigurationSection getMenuItems(String menu) {
        return guiConfig.getConfigurationSection(menu + ".items");
    }

    public String getItemSkullTexture(ConfigurationSection section) {
        return section.getString("skull-texture");
    }

    public String getItemCommand(String menu, String item) {
        return guiConfig.getString(menu + ".items." + item + ".command");
    }

    public List<String> getItemLore(ConfigurationSection section) {
        return section.getStringList("lore");
    }

    public String getItemName(ConfigurationSection section) {
        return section.getString("name", "&fItem");
    }

    public Material getItemMaterial(ConfigurationSection section) {
        String materialName = section.getString("material", "STONE");
        try {
            return Material.valueOf(materialName);
        } catch (IllegalArgumentException e) {
            return Material.STONE;
        }
    }

    public int getItemSlot(ConfigurationSection section) {
        return section.getInt("slot", 0);
    }

    public int getItemAmount(String menu, String item) {
        return guiConfig.getInt(menu + ".items." + item + ".amount", 1);
    }

    public String getItemNameAlt(String menu, String item, String suffix) {
        return guiConfig.getString(menu + ".items." + item + ".name-" + suffix, null);
    }

    public List<String> getItemLoreAlt(String menu, String item, String suffix) {
        String path = menu + ".items." + item + ".lore-" + suffix;
        if (guiConfig.contains(path)) {
            return guiConfig.getStringList(path);
        }
        return null;
    }

    public String getInfoItemName(String menu) {
        return guiConfig.getString(menu + ".info-item.name", "&fИнформация");
    }

    public List<String> getInfoItemLore(String menu) {
        return guiConfig.getStringList(menu + ".info-item.lore");
    }

    public Material getInfoItemMaterial(String menu) {
        String materialName = guiConfig.getString(menu + ".info-item.material", "BOOK");
        try {
            return Material.valueOf(materialName);
        } catch (IllegalArgumentException e) {
            return Material.BOOK;
        }
    }

    public int getInfoItemSlot(String menu) {
        return guiConfig.getInt(menu + ".info-item.slot", 4);
    }

    public String getInfoItemSkullTexture(String menu) {
        return guiConfig.getString(menu + ".info-item.skull-texture", null);
    }

    public ConfigurationSection getDepositButtons() {
        return guiConfig.getConfigurationSection("bank-menu.deposit-buttons");
    }

    public ConfigurationSection getWithdrawButtons() {
        return guiConfig.getConfigurationSection("bank-menu.withdraw-buttons");
    }

    public int getButtonSlot(String menu, String buttonType, String amount) {
        return guiConfig.getInt(menu + "." + buttonType + "." + amount + ".slot", 0);
    }

    public Material getButtonMaterial(String menu, String buttonType, String amount) {
        String materialName = guiConfig.getString(menu + "." + buttonType + "." + amount + ".material", "STONE");
        try {
            return Material.valueOf(materialName);
        } catch (IllegalArgumentException e) {
            return Material.STONE;
        }
    }

    public int getButtonAmount(String menu, String buttonType, String amount) {
        return guiConfig.getInt(menu + "." + buttonType + "." + amount + ".amount", 1);
    }

    public String getButtonName(String menu, String buttonType, String amount) {
        return guiConfig.getString(menu + "." + buttonType + "." + amount + ".name", "&fКнопка");
    }

    public List<String> getButtonLore(String menu, String buttonType, String amount) {
        return guiConfig.getStringList(menu + "." + buttonType + "." + amount + ".lore");
    }

    public String getString(String path) {
        return guiConfig.getString(path);
    }

    public String getString(String path, String defaultValue) {
        return guiConfig.getString(path, defaultValue);
    }

    public List<String> getStringList(String path) {
        return guiConfig.getStringList(path);
    }

    public int getInt(String path, int defaultValue) {
        return guiConfig.getInt(path, defaultValue);
    }

    public boolean contains(String path) {
        return guiConfig.contains(path);
    }
}
