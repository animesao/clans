package ru.clans;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import ru.clans.commands.ClanAdminCommand;
import ru.clans.commands.ClanCommand;
import ru.clans.commands.ClanChatCommand;
import ru.clans.gui.GUIManager;
import ru.clans.listeners.ChatListener;
import ru.clans.listeners.GUIListener;
import ru.clans.listeners.PlayerListener;
import ru.clans.managers.ClanManager;
import ru.clans.managers.ConfigManager;
import ru.clans.managers.DataManager;
import ru.clans.managers.GUIConfigManager;
import ru.clans.managers.LevelManager;
import ru.clans.managers.QuestManager;
import ru.clans.managers.VaultManager;
import ru.clans.placeholders.ClanPlaceholders;

public class ClanPlugin extends JavaPlugin {

    private static ClanPlugin instance;
    private ClanManager clanManager;
    private DataManager dataManager;
    private ConfigManager configManager;
    private GUIConfigManager guiConfigManager;
    private LevelManager levelManager;
    private QuestManager questManager;
    private GUIManager guiManager;
    private VaultManager vaultManager;
    private ChatListener chatListener;
    private boolean placeholderAPIEnabled = false;
    private boolean isReloading = false;

    @Override
    public void onEnable() {
        instance = this;
        isReloading = false;
        
        saveDefaultConfig();
        
        this.configManager = new ConfigManager(this);
        this.guiConfigManager = new GUIConfigManager(this);
        this.dataManager = new DataManager(this);
        this.levelManager = new LevelManager(this);
        this.questManager = new QuestManager(this);
        this.clanManager = new ClanManager(this);
        this.guiManager = new GUIManager(this);
        this.vaultManager = new VaultManager(this);
        
        this.chatListener = new ChatListener(this);
        
        dataManager.loadAllClans();
        
        getCommand("clan").setExecutor(new ClanCommand(this));
        getCommand("clanadmin").setExecutor(new ClanAdminCommand(this, guiManager));
        getCommand("cc").setExecutor(new ClanChatCommand(this));
        
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);
        getServer().getPluginManager().registerEvents(chatListener, this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new ClanPlaceholders(this).register();
            placeholderAPIEnabled = true;
            getLogger().info("PlaceholderAPI integration enabled!");
        }
        
        getLogger().info("Clans has been enabled!");
        getLogger().info("Loaded " + clanManager.getClans().size() + " clans");
        if (vaultManager.isEnabled()) {
            getLogger().info("Vault economy integration enabled!");
        }
    }

    @Override
    public void onDisable() {
        isReloading = true;
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            String title = player.getOpenInventory().getTitle();
            if (title != null && (
                title.equals(GUIManager.MAIN_MENU_TITLE) ||
                title.equals(GUIManager.MEMBERS_MENU_TITLE) ||
                title.equals(GUIManager.STORAGE_MENU_TITLE) ||
                title.equals(GUIManager.QUESTS_MENU_TITLE) ||
                title.equals(GUIManager.TOP_MENU_TITLE) ||
                title.equals(GUIManager.SETTINGS_MENU_TITLE) ||
                title.equals(GUIManager.AVAILABLE_QUESTS_TITLE) ||
                title.equals(GUIManager.ADMIN_MENU_TITLE) ||
                title.equals(GUIManager.BANK_MENU_TITLE)
            )) {
                player.closeInventory();
            }
        }
        
        if (dataManager != null) {
            dataManager.saveAllClans();
        }
        getLogger().info("Clans has been disabled!");
    }
    
    public boolean isReloading() {
        return isReloading;
    }

    public static ClanPlugin getInstance() {
        return instance;
    }

    public ClanManager getClanManager() {
        return clanManager;
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public GUIConfigManager getGUIConfigManager() {
        return guiConfigManager;
    }

    public LevelManager getLevelManager() {
        return levelManager;
    }

    public QuestManager getQuestManager() {
        return questManager;
    }

    public GUIManager getGUIManager() {
        return guiManager;
    }

    public VaultManager getVaultManager() {
        return vaultManager;
    }

    public ChatListener getChatListener() {
        return chatListener;
    }

    public boolean isPlaceholderAPIEnabled() {
        return placeholderAPIEnabled;
    }

    public boolean isVaultEnabled() {
        return vaultManager != null && vaultManager.isEnabled();
    }
}
