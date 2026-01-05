package ru.clans.listeners;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import ru.clans.managers.GUIConfigManager;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.configuration.file.FileConfiguration;
import ru.clans.ClanPlugin;
import ru.clans.gui.GUIManager;
import ru.clans.managers.QuestManager;
import ru.clans.models.Clan;
import ru.clans.models.ClanMember;
import ru.clans.models.ClanRank;
import ru.clans.models.ClanPermission;
import ru.clans.utils.MessageUtil;

import java.util.*;

public class GUIListener implements Listener {

    private final ClanPlugin plugin;

    public GUIListener(ClanPlugin plugin) {
        this.plugin = plugin;
    }

    private GUIManager getGuiManager() {
        return plugin.getGUIManager();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();

        try {
            if (plugin.isReloading()) {
                event.setCancelled(true);
                player.closeInventory();
                return;
            }
            
            if (title.equals(GUIManager.MAIN_MENU_TITLE)) {
                handleMainMenu(event, player);
            } else if (title.equals(GUIManager.MEMBERS_MENU_TITLE)) {
                handleMembersMenu(event, player);
            } else if (title.startsWith(GUIManager.STORAGE_MENU_TITLE)) {
                handleStorageMenu(event, player);
            } else if (title.equals(GUIManager.QUESTS_MENU_TITLE)) {
                handleQuestsMenu(event, player);
            } else if (title.equals(GUIManager.TOP_MENU_TITLE)) {
                handleTopMenu(event, player);
            } else if (title.equals(GUIManager.SETTINGS_MENU_TITLE)) {
                handleSettingsMenu(event, player);
            } else if (title.equals(GUIManager.AVAILABLE_QUESTS_TITLE)) {
                handleAvailableQuestsMenu(event, player);
            } else if (title.equals(GUIManager.ADMIN_MENU_TITLE)) {
                handleAdminMenu(event, player);
            } else if (title.equals(GUIManager.BANK_MENU_TITLE)) {
                handleBankMenu(event, player);
            } else if (title.equals(GUIManager.COLOR_MENU_TITLE)) {
                handleColorMenu(event, player);
            } else if (title.equals(GUIManager.CLAN_MANAGE_MENU_TITLE)) {
                handleClanManageMenu(event, player);
            } else if (title.equals(GUIManager.ROLE_SETTINGS_MENU_TITLE)) {
                handleRoleSettingsMenu(event, player);
            } else if (title.startsWith("§8Права: ")) {
                handlePermissionSettingsMenu(event, player);
            }
        } catch (IllegalStateException e) {
            event.setCancelled(true);
            player.closeInventory();
            MessageUtil.send(player, "&cПлагин перезагружается, попробуйте позже.");
        }
    }

    private void handleColorMenu(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
        if (clan == null) return;

        int slot = event.getRawSlot();
        FileConfiguration colorsConfig = plugin.getConfigManager().getColorsConfig();
        
        if (colorsConfig.contains("gui.back-button") && slot == colorsConfig.getInt("gui.back-button.slot")) {
            getGuiManager().openSettingsMenu(player, clan);
            return;
        }

        ConfigurationSection colors = colorsConfig.getConfigurationSection("colors");
        if (colors == null) return;

        for (String key : colors.getKeys(false)) {
            if (colors.getInt(key + ".slot") == slot) {
                int requiredLevel = colors.getInt(key + ".required-level", 1);
                if (clan.getLevel() < requiredLevel) {
                    MessageUtil.send(player, "&cЭтот цвет доступен с &f" + requiredLevel + " &cуровня клана!");
                    return;
                }
                String hex = colors.getString(key + ".hex");
                clan.setHexColor(hex);
                plugin.getDataManager().saveClan(clan);
                MessageUtil.send(player, "&aЦвет клана успешно изменен!");
                getGuiManager().openSettingsMenu(player, clan);
                return;
            }
        }
    }

    private void handleSettingsMenu(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
        if (clan == null) return;

        int slot = event.getRawSlot();
        if (slot == 40 && clan.isLeader(player.getUniqueId())) {
            getGuiManager().openRoleSettingsMenu(player, clan);
        } else if (slot == 38 && clan.isLeader(player.getUniqueId())) {
            getGuiManager().openClanManageMenu(player, clan);
        } else if (slot == 49) {
            getGuiManager().openMainMenu(player);
        } else if (slot == 20) {
            clan.setPvpEnabled(!clan.isPvpEnabled());
            plugin.getDataManager().saveClan(clan);
            MessageUtil.sendClanMessage(clan, clan.isPvpEnabled() ? "&aPVP внутри клана включен" : "&cPVP внутри клана выключен");
            getGuiManager().openSettingsMenu(player, clan);
        } else if (slot == 22) {
            getGuiManager().openColorMenu(player, clan);
        } else if (slot == 24) {
            if (plugin.getLevelManager().canUpgradeStorage(clan)) {
                int cost = plugin.getLevelManager().getStorageUpgradeCost(clan.getStorageLevel() + 1);
                if (clan.getExperience() >= cost) {
                    clan.setExperience(clan.getExperience() - cost);
                    clan.upgradeStorage();
                    plugin.getDataManager().saveClan(clan);
                    MessageUtil.sendClanMessage(clan, "&aХранилище улучшено до уровня &f" + clan.getStorageLevel());
                    getGuiManager().openSettingsMenu(player, clan);
                } else {
                    MessageUtil.send(player, "&cНедостаточно опыта клана!");
                }
            }
        } else if (slot == 30) {
            if (clan.isLeader(player.getUniqueId())) {
                if (event.getClick().isRightClick()) {
                    clan.setHome(null);
                    plugin.getDataManager().saveClan(clan);
                    MessageUtil.send(player, "&aТочка дома клана удалена!");
                } else {
                    clan.setHome(player.getLocation());
                    plugin.getDataManager().saveClan(clan);
                    MessageUtil.send(player, "&aТочка дома клана успешно установлена!");
                }
                getGuiManager().openSettingsMenu(player, clan);
            } else {
                MessageUtil.send(player, "&cТолько лидер может управлять домом клана!");
            }
        }
    }

    private void handleClanManageMenu(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
        if (clan == null) return;
        
        int slot = event.getRawSlot();
        switch (slot) {
            case 10: getGuiManager().openRoleSettingsMenu(player, clan); break;
            case 12: getGuiManager().openRoleSettingsMenu(player, clan); break;
            case 14: getGuiManager().openRoleSettingsMenu(player, clan); break;
            case 16:
                player.closeInventory();
                plugin.getClanManager().disbandClan(clan);
                MessageUtil.send(player, "&cВаш клан был расформирован.");
                break;
            case 22: getGuiManager().openSettingsMenu(player, clan); break;
        }
    }

    private void handleRoleSettingsMenu(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
        if (clan == null) return;
        
        int slot = event.getRawSlot();
        if (slot == 49) {
            getGuiManager().openSettingsMenu(player, clan);
            return;
        }

        if (slot == 45) {
            player.closeInventory();
            plugin.getClanManager().disbandClan(clan);
            MessageUtil.send(player, "&cВаш клан был расформирован.");
            return;
        }

        int[] slots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};
        List<ClanRank> ranks = new ArrayList<>(Arrays.asList(ClanRank.values()));
        ranks.sort((r1, r2) -> Integer.compare(r2.getPriority(), r1.getPriority()));

        for (int i = 0; i < slots.length && i < ranks.size(); i++) {
            if (slots[i] == slot) {
                ClanRank rank = ranks.get(i);
                if (event.getClick().isShiftClick()) {
                    getGuiManager().openPermissionSettingsMenu(player, clan, rank);
                } else if (event.getClick().isLeftClick()) {
                    player.closeInventory();
                    MessageUtil.send(player, "&eВведите новое название для роли &f" + rank.name() + " &eв чат:");
                    // Awaiting input logic
                } else if (event.getClick().isRightClick()) {
                    player.closeInventory();
                    MessageUtil.send(player, "&eВведите новый префикс для роли &f" + rank.name() + " &eв чат:");
                    // Awaiting input logic
                }
                return;
            }
        }
    }

    private void handlePermissionSettingsMenu(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
        if (clan == null) return;

        String title = event.getView().getTitle();
        String rankNameInTitle = title.substring("§8Права: ".length());
        
        ClanRank targetRank = null;
        for (ClanRank rank : ClanRank.values()) {
            String currentRankName = rank.getDisplayName();
            Map<String, Clan.ClanRankData> customRanks = clan.getCustomRanks();
            if (customRanks != null && customRanks.containsKey(rank.name())) {
                currentRankName = customRanks.get(rank.name()).getName();
            }
            if (currentRankName.equals(rankNameInTitle)) {
                targetRank = rank;
                break;
            }
        }
        
        if (targetRank == null) return;
        int slot = event.getRawSlot();
        if (slot == 49) {
            getGuiManager().openRoleSettingsMenu(player, clan);
            return;
        }

        int currentSlot = 10;
        for (ClanPermission perm : ClanPermission.values()) {
            if (currentSlot > 43) break;
            if (currentSlot % 9 == 0 || currentSlot % 9 == 8) { currentSlot++; }
            
            if (currentSlot == slot) {
                Clan.ClanRankData data = clan.getRankData(targetRank.name());
                if (data.getPermissions().contains(perm)) {
                    data.getPermissions().remove(perm);
                } else {
                    data.getPermissions().add(perm);
                }
                plugin.getDataManager().saveClan(clan);
                getGuiManager().openPermissionSettingsMenu(player, clan, targetRank);
                return;
            }
            currentSlot++;
            if (currentSlot % 9 == 8) currentSlot += 2;
        }
    }

    private void handleMainMenu(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
        if (clan == null) { player.closeInventory(); return; }
        int slot = event.getRawSlot();
        GUIConfigManager config = plugin.getGUIConfigManager();
        ConfigurationSection itemsSection = config.getMenuItems("main-menu");
        if (itemsSection != null) {
            for (String key : itemsSection.getKeys(false)) {
                ConfigurationSection itemConfig = itemsSection.getConfigurationSection(key);
                if (itemConfig != null && config.getItemSlot(itemConfig) == slot) {
                    switch (key) {
                        case "members": getGuiManager().openMembersMenu(player, clan); return;
                        case "storage": getGuiManager().openStorageMenu(player, clan, 0); return;
                        case "quests": getGuiManager().openQuestsMenu(player, clan); return;
                        case "home":
                            if (clan.hasHome()) {
                                player.closeInventory();
                                ClanMember member = clan.getMember(player.getUniqueId());
                                if (member != null && member.hasPermission(ClanPermission.HOME)) {
                                    player.teleport(clan.getHome());
                                    MessageUtil.send(player, "&aВы телепортированы на базу клана!");
                                } else MessageUtil.send(player, "&cУ вас нет прав телепортироваться на базу!");
                            }
                            return;
                        case "bank": getGuiManager().openBankMenu(player, clan); return;
                        case "top": getGuiManager().openTopMenu(player); return;
                        case "settings":
                            ClanMember member = clan.getMember(player.getUniqueId());
                            if (member != null && member.hasPermission(ClanPermission.CHANGE_SETTINGS)) getGuiManager().openSettingsMenu(player, clan);
                            return;
                        case "close": player.closeInventory(); return;
                    }
                }
            }
        }
    }

    private void handleMembersMenu(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        if (event.getRawSlot() == 49) getGuiManager().openMainMenu(player);
    }

    private void handleStorageMenu(InventoryClickEvent event, Player player) {
        Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
        if (clan == null) { event.setCancelled(true); player.closeInventory(); return; }
        int slot = event.getRawSlot();
        int page = getGuiManager().getStoragePage(player.getUniqueId());
        
        if (slot == 45) {
            event.setCancelled(true);
            if (page <= 0) return;
            saveStorageContents(event.getInventory(), clan, page);
            getGuiManager().openStorageMenu(player, clan, page - 1);
        } else if (slot == 53) {
            event.setCancelled(true);
            int maxPages = plugin.getConfigManager().getStoragePageLimit(clan.getLevel());
            if (page + 1 >= maxPages) {
                String msg = plugin.getConfigManager().getStorageMessage("limit-reached")
                        .replace("%limit%", String.valueOf(maxPages))
                        .replace("%level%", String.valueOf(clan.getLevel()));
                MessageUtil.send(player, msg);
                return;
            }
            saveStorageContents(event.getInventory(), clan, page);
            getGuiManager().openStorageMenu(player, clan, page + 1);
        } else if (slot == 49) {
            event.setCancelled(true);
            saveStorageContents(event.getInventory(), clan, page);
            getGuiManager().openMainMenu(player);
        } else if ((slot >= 0 && slot < 9) || (slot >= 36 && slot < 54)) {
            event.setCancelled(true);
        } else {
            // Проверка на заблокированный слот
            int itemSlot = slot - 9;
            if (itemSlot >= 0 && itemSlot < 27) {
                int unlockedSlots = plugin.getConfigManager().getStorageSlotsForLevel(clan.getStorageLevel());
                if (itemSlot >= unlockedSlots) {
                    event.setCancelled(true);
                    MessageUtil.send(player, "&cЭтот слот заблокирован!");
                }
            }
        }
    }

    private void saveStorageContents(Inventory inv, Clan clan, int page) {
        ItemStack[] items = new ItemStack[27];
        int unlockedSlots = plugin.getConfigManager().getStorageSlotsForLevel(clan.getStorageLevel());
        for (int i = 0; i < 27; i++) {
            ItemStack item = inv.getItem(i + 9);
            if (item != null && item.getType() != Material.AIR && i < unlockedSlots) items[i] = item;
        }
        clan.setStoragePage(page, items);
        plugin.getDataManager().saveClan(clan);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        String title = event.getView().getTitle();
        if (title.startsWith(GUIManager.STORAGE_MENU_TITLE)) {
            Clan clan = plugin.getClanManager().getPlayerClan(event.getPlayer().getUniqueId());
            if (clan != null) saveStorageContents(event.getInventory(), clan, getGuiManager().getStoragePage(event.getPlayer().getUniqueId()));
        }
    }

    private void handleQuestsMenu(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
        if (clan == null) { player.closeInventory(); return; }
        if (event.getRawSlot() == 49) getGuiManager().openMainMenu(player);
        else if (event.getRawSlot() == 40) {
            ClanMember member = clan.getMember(player.getUniqueId());
            if (member != null && member.hasPermission(ClanPermission.MANAGE_QUESTS)) getGuiManager().openAvailableQuestsMenu(player, clan);
        }
    }

    private void handleAvailableQuestsMenu(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
        if (clan == null) { player.closeInventory(); return; }
        
        int slot = event.getRawSlot();
        if (slot == 49) {
            getGuiManager().openQuestsMenu(player, clan);
            return;
        }

        List<QuestManager.QuestTemplate> available = new ArrayList<>(plugin.getQuestManager().getAvailableQuests(clan));
        int questIndex = 0;
        int currentSlot = 10;
        
        while (questIndex < available.size() && currentSlot <= 43) {
            if (currentSlot % 9 == 0 || currentSlot % 9 == 8) {
                currentSlot++;
                continue;
            }
            
            if (currentSlot == slot) {
                QuestManager.QuestTemplate template = available.get(questIndex);
                plugin.getQuestManager().startQuest(clan, template.id, player);
                getGuiManager().openQuestsMenu(player, clan);
                return;
            }
            
            questIndex++;
            currentSlot++;
        }
    }

    private void handleTopMenu(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        if (event.getRawSlot() == 49) getGuiManager().openMainMenu(player);
    }

    private void handleAdminMenu(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        if (event.getRawSlot() == 40) {
            player.closeInventory();
            plugin.getConfigManager().reloadConfigs();
            plugin.getGUIConfigManager().reloadConfig();
            MessageUtil.send(player, "&aКонфигурация успешно перезагружена!");
        }
    }

    private void handleBankMenu(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
        if (clan == null) { player.closeInventory(); return; }
        ClanMember member = clan.getMember(player.getUniqueId());
        if (member == null) { player.closeInventory(); return; }
        
        int slot = event.getRawSlot();
        if (slot == 40) { getGuiManager().openMainMenu(player); return; }
        
        if (slot == 21) {
            if (!member.hasPermission(ClanPermission.BANK_DEPOSIT)) {
                MessageUtil.send(player, "&cУ вас нет прав на пополнение казны!");
                return;
            }
            player.closeInventory();
            MessageUtil.send(player, "&eВведите сумму для пополнения казны:");
            plugin.getChatListener().expectInput(player.getUniqueId(), "bank_deposit");
        } else if (slot == 23) {
            if (!member.hasPermission(ClanPermission.BANK_WITHDRAW)) {
                MessageUtil.send(player, "&cУ вас нет прав на снятие денег из казны!");
                return;
            }
            player.closeInventory();
            MessageUtil.send(player, "&eВведите сумму для снятия из казны:");
            plugin.getChatListener().expectInput(player.getUniqueId(), "bank_withdraw");
        }
    }
}
