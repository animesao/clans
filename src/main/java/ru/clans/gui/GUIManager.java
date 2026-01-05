package ru.clans.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.configuration.file.FileConfiguration;
import ru.clans.ClanPlugin;
import ru.clans.managers.GUIConfigManager;
import ru.clans.managers.QuestManager;
import ru.clans.models.*;
import ru.clans.utils.ColorUtil;
import ru.clans.utils.ItemBuilder;
import ru.clans.utils.MessageUtil;

import java.util.*;

public class GUIManager {

    public static final String MAIN_MENU_TITLE = "§8Меню клана";
    public static final String MEMBERS_MENU_TITLE = "§8Участники клана";
    public static final String STORAGE_MENU_TITLE = "§8Хранилище клана";
    public static final String QUESTS_MENU_TITLE = "§8Задания клана";
    public static final String TOP_MENU_TITLE = "§8Топ кланов";
    public static final String SETTINGS_MENU_TITLE = "§8Настройки клана";
    public static final String AVAILABLE_QUESTS_TITLE = "§8Доступные задания";
    public static final String ADMIN_MENU_TITLE = "§4§lАдмин панель кланов";
    public static final String BANK_MENU_TITLE = "§8Казна клана";
    public static final String COLOR_MENU_TITLE = "§8Выбор цвета клана";
    public static final String CLAN_MANAGE_MENU_TITLE = "§8Управление кланом";
    public static final String ROLE_SETTINGS_MENU_TITLE = "§8Настройка ролей";
    public static final String PERMISSION_SETTINGS_MENU_TITLE = "§8Настройка прав";

    private final ClanPlugin plugin;
    private final Map<UUID, Integer> storagePage;
    private final Map<UUID, StorageViewMode> storageViewMode;

    public GUIManager(ClanPlugin plugin) {
        this.plugin = plugin;
        this.storagePage = new HashMap<>();
        this.storageViewMode = new HashMap<>();
    }

    private GUIConfigManager getConfig() {
        return plugin.getGUIConfigManager();
    }

    private ItemStack createConfiguredItem(String menu, String item, String defaultTexture, Map<String, String> placeholders) {
        GUIConfigManager config = getConfig();
        Material material = config.getItemMaterial(menu, item);
        String name = config.getItemName(menu, item);
        List<String> lore = config.getItemLore(menu, item);
        String skullTexture = config.getString(menu + ".items." + item + ".skull-texture");

        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            name = name.replace(entry.getKey(), entry.getValue());
            lore = replacePlaceholders(lore, entry.getKey(), entry.getValue());
        }

        if (material == Material.PLAYER_HEAD && skullTexture != null) {
            return ItemBuilder.createCustomHead(skullTexture, name, lore.toArray(new String[0]));
        } else if (material == Material.PLAYER_HEAD && defaultTexture != null) {
            return ItemBuilder.createCustomHead(defaultTexture, name, lore.toArray(new String[0]));
        } else {
            return new ItemBuilder(material).setName(name).setLore(lore).build();
        }
    }

    private List<String> replacePlaceholders(List<String> list, String placeholder, String value) {
        List<String> result = new ArrayList<>();
        for (String s : list) {
            result.add(s.replace(placeholder, value));
        }
        return result;
    }

    public void openMainMenu(Player player) {
        Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());
        if (clan == null) {
            MessageUtil.send(player, "&cВы не состоите в клане!");
            return;
        }

        GUIConfigManager config = getConfig();
        int size = config.getMenuSize("main-menu");
        String title = config.getMenuTitle("main-menu");
        Inventory inv = Bukkit.createInventory(null, size, ColorUtil.colorize(title));
        
        if (config.shouldFillBorder("main-menu")) {
            fillBeautifulBorder(inv, clan.getHexColor());
        }
        
        ClanMember member = clan.getMember(player.getUniqueId());
        String hexColor = clan.getHexColor();
        
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%clan_name%", clan.getName());
        placeholders.put("%clan_tag%", clan.getTag());
        placeholders.put("%clan_level%", String.valueOf(clan.getLevel()));
        placeholders.put("%clan_experience%", MessageUtil.formatNumber(clan.getExperience()));
        placeholders.put("%clan_members%", String.valueOf(clan.getMemberCount()));
        placeholders.put("%clan_max_members%", String.valueOf(plugin.getConfigManager().getMaxMembersForLevel(clan.getLevel())));
        placeholders.put("%clan_kills%", String.valueOf(clan.getKills()));
        placeholders.put("%clan_deaths%", String.valueOf(clan.getDeaths()));
        placeholders.put("%clan_kdr%", String.format("%.2f", clan.getKDR()));
        placeholders.put("%clan_storage_level%", String.valueOf(clan.getStorageLevel()));
        placeholders.put("%clan_storage_pages%", String.valueOf(clan.getStoragePages()));
        placeholders.put("%clan_active_quests%", String.valueOf(clan.getActiveQuests().size()));
        placeholders.put("%clan_completed_quests%", String.valueOf(clan.getCompletedQuests().values().stream().mapToInt(Integer::intValue).sum()));
        placeholders.put("%clan_bank%", MessageUtil.formatNumber((long)clan.getBank()));
        placeholders.put("%clan_online%", String.valueOf(clan.getOnlineMemberCount()));
        placeholders.put("%home_status%", clan.hasHome() ? "&aУстановлен" : "&cНе установлен");
        placeholders.put("%player_name%", player.getName());
        
        if (clan.hasHome()) {
            placeholders.put("%home_world%", clan.getHome().getWorld().getName());
            placeholders.put("%home_x%", String.valueOf((int)clan.getHome().getX()));
            placeholders.put("%home_y%", String.valueOf((int)clan.getHome().getY()));
            placeholders.put("%home_z%", String.valueOf((int)clan.getHome().getZ()));
        } else {
            placeholders.put("%home_world%", "Не установлен");
            placeholders.put("%home_x%", "0");
            placeholders.put("%home_y%", "0");
            placeholders.put("%home_z%", "0");
        }

        ConfigurationSection itemsSection = config.getMenuItems("main-menu");
        if (itemsSection != null) {
            for (String key : itemsSection.getKeys(false)) {
                ConfigurationSection itemConfig = itemsSection.getConfigurationSection(key);
                if (itemConfig == null) continue;
                if (key.equals("settings") && !member.hasPermission(ClanPermission.CHANGE_SETTINGS)) continue;
                
                int slot = config.getItemSlot(itemConfig);
                Material material = config.getItemMaterial(itemConfig);
                String name = config.getItemName(itemConfig);
                List<String> lore = config.getItemLore(itemConfig);
                String skullTexture = config.getItemSkullTexture(itemConfig);
                String skullOwner = itemConfig.getString("skull-owner");
                
                if (key.equals("home") && !clan.hasHome()) {
                    name = itemConfig.getString("name-no-home", name);
                    lore = itemConfig.getStringList("lore-no-home");
                    if (lore.isEmpty()) lore = config.getItemLore(itemConfig);
                }

                for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                    name = name.replace(entry.getKey(), entry.getValue());
                    lore = replacePlaceholders(lore, entry.getKey(), entry.getValue());
                }

                ItemStack itemStack;
                if (material == Material.PLAYER_HEAD) {
                    if (skullOwner != null) {
                        itemStack = new ItemBuilder(Material.PLAYER_HEAD)
                            .setSkullOwner(Bukkit.getOfflinePlayer(skullOwner.replace("%player_name%", player.getName())))
                            .setName(name)
                            .setLore(lore)
                            .build();
                    } else if (skullTexture != null) {
                        itemStack = ItemBuilder.createCustomHead(skullTexture, name, lore.toArray(new String[0]));
                    } else {
                        itemStack = new ItemBuilder(material).setName(name).setLore(lore).build();
                    }
                } else {
                    itemStack = new ItemBuilder(material).setName(name).setLore(lore).build();
                }
                
                if (key.equals("info")) {
                    ItemMeta meta = itemStack.getItemMeta();
                    List<String> currentLore = meta.getLore();
                    if (currentLore == null) currentLore = new ArrayList<>();
                    currentLore.add("");
                    currentLore.add(ColorUtil.colorize("&7Прогресс уровня:"));
                    
                    int lvl = clan.getLevel();
                    String lvlColor = plugin.getConfigManager().getLevelColor(lvl);
                    String progressBar = MessageUtil.createProgressBar(plugin.getLevelManager().getProgressPercent(clan), 20);
                    
                    currentLore.add(ColorUtil.colorize(progressBar));
                    currentLore.add(ColorUtil.colorize("&7До следующего уровня: " + ColorUtil.getHexColor(hexColor) + MessageUtil.formatNumber((long) plugin.getLevelManager().getExperienceToNextLevel(clan)) + " &7опыта"));
                    meta.setLore(currentLore);
                    itemStack.setItemMeta(meta);
                }
                inv.setItem(slot, itemStack);
            }
        }
        player.openInventory(inv);
    }

    public void openMembersMenu(Player player, Clan clan) {
        Inventory inv = Bukkit.createInventory(null, 54, MEMBERS_MENU_TITLE);
        fillBorder(inv);
        List<ClanMember> members = new ArrayList<>(clan.getMembers().values());
        members.sort((m1, m2) -> Integer.compare(m2.getRank().getPriority(), m1.getRank().getPriority()));
        int slot = 10;
        for (ClanMember member : members) {
            if (slot > 43) break;
            if (slot % 9 == 0 || slot % 9 == 8) { slot++; continue; }
            boolean online = Bukkit.getPlayer(member.getUuid()) != null;
            ItemStack head = new ItemBuilder(Material.PLAYER_HEAD)
                    .setSkullOwner(Bukkit.getOfflinePlayer(member.getUuid()))
                    .setName((online ? "&a" : "&7") + member.getName())
                    .setLore("&7Ранг: &f" + member.getRank().getDisplayName(), "&7Вклад: &f" + member.getContribution(), "&7Убийств: &a" + member.getKills(), "&7Смертей: &c" + member.getDeaths(), "&7K/D: &f" + String.format("%.2f", member.getKDR()), "", "&7Присоединился: &f" + formatDate(member.getJoinedAt()), "&7Был онлайн: &f" + (online ? "Сейчас" : MessageUtil.formatTime(System.currentTimeMillis() - member.getLastOnline()) + " назад"))
                    .build();
            inv.setItem(slot, head);
            slot++;
        }
        inv.setItem(49, new ItemBuilder(Material.ARROW).setName("&7Назад").build());
        player.openInventory(inv);
    }

    public void openStorageMenu(Player player, Clan clan, int page) {
        ClanMember member = clan.getMember(player.getUniqueId());
        if (member == null || !member.hasPermission(ClanPermission.STORAGE_VIEW)) { MessageUtil.send(player, "&cУ вас нет доступа к хранилищу!"); return; }
        
        // Убеждаемся что страницы инициализированы
        clan.updateStoragePages();
        
        storagePage.put(player.getUniqueId(), page);
        boolean canDeposit = member.hasPermission(ClanPermission.STORAGE_DEPOSIT);
        boolean canWithdraw = member.hasPermission(ClanPermission.STORAGE_WITHDRAW);
        if (canDeposit && canWithdraw) storageViewMode.put(player.getUniqueId(), StorageViewMode.EDIT);
        else if (canDeposit) storageViewMode.put(player.getUniqueId(), StorageViewMode.DEPOSIT_ONLY);
        else if (canWithdraw) storageViewMode.put(player.getUniqueId(), StorageViewMode.WITHDRAW_ONLY);
        else storageViewMode.put(player.getUniqueId(), StorageViewMode.VIEW);
        
        int maxPages = plugin.getConfigManager().getStoragePageLimit(clan.getLevel());
        // Дополнительная проверка: GUI не должен показывать лимит меньше чем реально создано страниц
        maxPages = Math.max(maxPages, clan.getStoragePages());
        
        Inventory inv = Bukkit.createInventory(null, 54, STORAGE_MENU_TITLE + " §7(Стр. " + (page + 1) + "/" + maxPages + ")");
        int unlockedSlots = plugin.getConfigManager().getStorageSlotsForLevel(clan.getStorageLevel());
        ItemStack[] items = clan.getStoragePage(page);
        for (int i = 0; i < 27; i++) {
            if (i < unlockedSlots) {
                if (items[i] != null) inv.setItem(i + 9, items[i]);
            } else inv.setItem(i + 9, new ItemBuilder(Material.BARRIER).setName("&c✖ Слот заблокирован").setLore("", "&7Этот слот недоступен.", "&7Улучшите хранилище клана,", "&7чтобы разблокировать!", "", "&7Текущий уровень: &f" + clan.getStorageLevel(), "&7Открыто слотов: &f" + unlockedSlots + "/27").build());
        }
        for (int i = 0; i < 9; i++) inv.setItem(i, ItemBuilder.createFiller());
        for (int i = 36; i < 45; i++) inv.setItem(i, ItemBuilder.createFiller());
        for (int i = 45; i < 54; i++) inv.setItem(i, ItemBuilder.createFiller());
        if (page > 0) inv.setItem(45, new ItemBuilder(Material.ARROW).setName("&#FFD700◀ Предыдущая страница").setLore("&7Нажмите чтобы перейти", "&7на предыдущую страницу").build());
        if (page < maxPages - 1) inv.setItem(53, new ItemBuilder(Material.ARROW).setName("&#FFD700Следующая страница ▶").setLore("&7Нажмите чтобы перейти", "&7на следующую страницу").build());
        inv.setItem(49, new ItemBuilder(Material.BARRIER).setName("&c✖ Назад в меню").build());
        String modeText;
        StorageViewMode mode = storageViewMode.get(player.getUniqueId());
        switch (mode) {
            case EDIT: modeText = "&a✔ Полный доступ"; break;
            case DEPOSIT_ONLY: modeText = "&e⬇ Только положить"; break;
            case WITHDRAW_ONLY: modeText = "&e⬆ Только забрать"; break;
            default: modeText = "&c✖ Только просмотр";
        }
        inv.setItem(4, new ItemBuilder(Material.ENDER_CHEST).setName("&#9B59B6✦ Хранилище клана ✦").setLore("", "&7┃ Уровень: &#FFD700" + clan.getStorageLevel(), "&7┃ Страница: &#FFD700" + (page + 1) + "&7/&#FFD700" + maxPages, "&7┃ Открыто слотов: &#FFD700" + unlockedSlots + "&7/27", "&7┃ Ваш режим: " + modeText, "", "&8▪ &7Кликните на предмет чтобы взять", "&8▪ &7Положите предмет из инвентаря").addGlow().build());
        
        player.openInventory(inv);
    }

    public void openQuestsMenu(Player player, Clan clan) {
        plugin.getQuestManager().checkExpiredQuests(clan);
        Inventory inv = Bukkit.createInventory(null, 54, QUESTS_MENU_TITLE);
        fillBorder(inv);
        inv.setItem(4, new ItemBuilder(Material.BOOK).setName("&#3498DBЗадания клана").setLore("&7Активных заданий: &f" + clan.getActiveQuests().size(), "&7Всего выполнено: &f" + clan.getCompletedQuests().values().stream().mapToInt(Integer::intValue).sum()).build());
        int slot = 19;
        for (ClanQuest quest : clan.getActiveQuests().values()) {
            if (slot > 34) break;
            if (slot % 9 == 0 || slot % 9 == 8) { slot++; continue; }
            Material icon = quest.getType().getIcon();
            String status = quest.isCompleted() ? "&a&lВЫПОЛНЕНО" : "&eВ процессе";
            inv.setItem(slot, new ItemBuilder(icon).setName("&#FFD700" + quest.getName()).setLore("&7" + quest.getDescription(), "", "&7Прогресс: &f" + quest.getCurrentProgress() + "/" + quest.getTargetAmount(), "&7" + MessageUtil.createProgressBar(quest.getProgressPercent(), 15), "", "&7Награда: &a" + MessageUtil.formatNumber(quest.getExperienceReward()) + " опыта", "&7Осталось: &f" + quest.getFormattedRemainingTime(), "", status).build());
            slot++;
        }
        ClanMember member = clan.getMember(player.getUniqueId());
        if (member != null && member.hasPermission(ClanPermission.MANAGE_QUESTS)) inv.setItem(40, new ItemBuilder(Material.EMERALD).setName("&#2ECC71Взять новое задание").setLore("&7Нажмите для выбора", "&7нового задания").build());
        inv.setItem(49, new ItemBuilder(Material.ARROW).setName("&7Назад").build());
        player.openInventory(inv);
    }

    public void openAvailableQuestsMenu(Player player, Clan clan) {
        Collection<QuestManager.QuestTemplate> available = plugin.getQuestManager().getAvailableQuests(clan);
        Inventory inv = Bukkit.createInventory(null, 54, AVAILABLE_QUESTS_TITLE);
        fillBorder(inv);
        int slot = 10;
        for (QuestManager.QuestTemplate template : available) {
            if (slot > 43) break;
            if (slot % 9 == 0 || slot % 9 == 8) { slot++; continue; }
            inv.setItem(slot, new ItemBuilder(template.type.getIcon()).setName("&#FFD700" + template.name).setLore("&7" + template.description, "", "&7Тип: &f" + template.type.getDisplayName(), "&7Цель: &f" + template.target, "&7Награда: &a" + MessageUtil.formatNumber(template.experience) + " опыта", "&7Время: &f" + MessageUtil.formatTime(template.duration), "&7Мин. уровень: &f" + template.minLevel, "", "&eНажмите для принятия").build());
            slot++;
        }
        inv.setItem(49, new ItemBuilder(Material.ARROW).setName("&7Назад").build());
        player.openInventory(inv);
    }

    public void openTopMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, TOP_MENU_TITLE);
        fillBorder(inv);
        List<Clan> topClans = plugin.getClanManager().getTopClans(10);
        int slot = 10;
        int position = 1;
        for (Clan clan : topClans) {
            if (slot > 43) break;
            if (slot % 9 == 0 || slot % 9 == 8) { slot++; continue; }
            Material material = position == 1 ? Material.GOLD_BLOCK : position == 2 ? Material.IRON_BLOCK : position == 3 ? Material.EMERALD_BLOCK : Material.COAL_BLOCK;
            String hexColor = clan.getHexColor();
            inv.setItem(slot, new ItemBuilder(material).setName("&6#" + position + " " + ColorUtil.getHexColor(hexColor) + clan.getName() + " &7[" + clan.getTag() + "]").setLore("&7Уровень: &f" + clan.getLevel(), "&7Опыт: &f" + MessageUtil.formatNumber(clan.getExperience()), "&7Участников: &f" + clan.getMemberCount(), "&7Убийств: &a" + clan.getKills(), "&7K/D: &f" + String.format("%.2f", clan.getKDR())).build());
            slot++; position++;
        }
        inv.setItem(49, new ItemBuilder(Material.ARROW).setName("&7Назад").build());
        player.openInventory(inv);
    }

    public void openColorMenu(Player player, Clan clan) {
        FileConfiguration colorsConfig = plugin.getConfigManager().getColorsConfig();
        ConfigurationSection colors = colorsConfig.getConfigurationSection("colors");
        ConfigurationSection items = colorsConfig.getConfigurationSection("items");
        if (colors == null) return;
        
        int size = colorsConfig.getInt("gui.size", 36);
        String title = colorsConfig.getString("gui.title", COLOR_MENU_TITLE);
        Inventory inv = Bukkit.createInventory(null, size, ColorUtil.colorize(title));
        
        if (colorsConfig.getBoolean("gui.fill-border", true)) {
            fillBeautifulBorder(inv, clan.getHexColor());
        }

        // Загрузка декоративных или дополнительных предметов из секции items
        if (items != null) {
            for (String key : items.getKeys(false)) {
                ConfigurationSection itemSection = items.getConfigurationSection(key);
                if (itemSection == null) continue;
                
                int slot = itemSection.getInt("slot", -1);
                if (slot < 0 || slot >= size) continue;
                
                Material material = Material.valueOf(itemSection.getString("material", "STONE"));
                String name = itemSection.getString("name", " ");
                List<String> lore = itemSection.getStringList("lore");
                
                inv.setItem(slot, new ItemBuilder(material)
                        .setName(ColorUtil.colorize(name))
                        .setLore(lore)
                        .build());
            }
        }
        
        for (String key : colors.getKeys(false)) {
            ConfigurationSection colorSection = colors.getConfigurationSection(key);
            String name = colorSection.getString("name");
            String hex = colorSection.getString("hex");
            Material material = Material.valueOf(colorSection.getString("material", "WHITE_WOOL"));
            int slot = colorSection.getInt("slot");
            inv.setItem(slot, new ItemBuilder(material).setName(ColorUtil.colorize(name)).setLore("", ColorUtil.colorize("&7Кликните, чтобы выбрать этот цвет"), ColorUtil.colorize("&7HEX: &f" + hex)).build());
        }
        
        if (colorsConfig.contains("gui.back-button")) {
            int backSlot = colorsConfig.getInt("gui.back-button.slot", 31);
            Material backMat = Material.valueOf(colorsConfig.getString("gui.back-button.material", "ARROW"));
            String backName = colorsConfig.getString("gui.back-button.name", "&7Назад");
            inv.setItem(backSlot, new ItemBuilder(backMat).setName(ColorUtil.colorize(backName)).build());
        }
        
        player.openInventory(inv);
    }

    public void openSettingsMenu(Player player, Clan clan) {
        ClanMember member = clan.getMember(player.getUniqueId());
        if (member == null || !member.hasPermission(ClanPermission.CHANGE_SETTINGS)) { MessageUtil.send(player, "&cУ вас нет доступа к настройкам!"); return; }
        
        GUIConfigManager config = getConfig();
        int size = config.getMenuSize("settings-menu");
        String title = config.getMenuTitle("settings-menu");
        Inventory inv = Bukkit.createInventory(null, size, ColorUtil.colorize(title));
        
        if (config.shouldFillBorder("settings-menu")) {
            fillBeautifulBorder(inv, clan.getHexColor());
        }

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%clan_name%", clan.getName());
        placeholders.put("%clan_tag%", clan.getTag());
        placeholders.put("%clan_color_preview%", ColorUtil.getHexColor(clan.getHexColor()) + "■");
        placeholders.put("%clan_storage_level%", String.valueOf(clan.getStorageLevel()));
        placeholders.put("%upgrade_cost%", String.valueOf(plugin.getLevelManager().getStorageUpgradeCost(clan.getStorageLevel() + 1)));
        placeholders.put("%pvp_status%", clan.isPvpEnabled() ? "&aВключено" : "&cВыключено");
        placeholders.put("%home_status%", clan.hasHome() ? "&aУстановлен" : "&cНе установлен");
        placeholders.put("%player_name%", player.getName());

        ConfigurationSection itemsSection = config.getMenuItems("settings-menu");
        if (itemsSection != null) {
            for (String key : itemsSection.getKeys(false)) {
                if (key.equals("clan-manage") && member.getRank().getPriority() < 100) continue;
                
                ConfigurationSection itemConfig = itemsSection.getConfigurationSection(key);
                if (itemConfig == null) continue;
                
                int slot = config.getItemSlot(itemConfig);
                Material material = config.getItemMaterial(itemConfig);
                String name = config.getItemName(itemConfig);
                List<String> lore = config.getItemLore(itemConfig);
                String skullTexture = config.getItemSkullTexture(itemConfig);
                String skullOwner = itemConfig.getString("skull-owner");

                for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                    name = name.replace(entry.getKey(), entry.getValue());
                    lore = replacePlaceholders(lore, entry.getKey(), entry.getValue());
                }

                ItemStack itemStack;
                if (material == Material.PLAYER_HEAD) {
                    if (skullOwner != null) {
                        itemStack = new ItemBuilder(Material.PLAYER_HEAD)
                            .setSkullOwner(Bukkit.getOfflinePlayer(skullOwner.replace("%player_name%", player.getName())))
                            .setName(name)
                            .setLore(lore)
                            .build();
                    } else if (skullTexture != null) {
                        itemStack = ItemBuilder.createCustomHead(skullTexture, name, lore.toArray(new String[0]));
                    } else {
                        itemStack = new ItemBuilder(material).setName(name).setLore(lore).build();
                    }
                } else {
                    itemStack = new ItemBuilder(material).setName(name).setLore(lore).build();
                }
                inv.setItem(slot, itemStack);
            }
        }
        player.openInventory(inv);
    }

    public void openBankMenu(Player player, Clan clan) {
        if (!plugin.isVaultEnabled()) { MessageUtil.send(player, "&cЭкономика недоступна!"); return; }
        Inventory inv = Bukkit.createInventory(null, 45, BANK_MENU_TITLE);
        fillBeautifulBorder(inv, clan.getHexColor());
        inv.setItem(4, new ItemBuilder(Material.PLAYER_HEAD).setSkullOwner(player).setName("&#2ECC71$ Казна клана").setLore("", "&8▸ &7Баланс казны: &#2ECC71" + MessageUtil.formatNumber((long)clan.getBank()), "&8▸ &7Ваш баланс: &#FFD700" + MessageUtil.formatNumber((long)plugin.getVaultManager().getBalance(player)), "").build());
        
        inv.setItem(21, new ItemBuilder(Material.LIME_STAINED_GLASS_PANE).setName("&aПоложить в казну").setLore("&7Нажмите, чтобы положить деньги", "&7в общую казну клана.").build());
        inv.setItem(23, new ItemBuilder(Material.RED_STAINED_GLASS_PANE).setName("&cСнять из казны").setLore("&7Нажмите, чтобы забрать деньги", "&7из общей казны клана.").build());
        
        inv.setItem(40, new ItemBuilder(Material.ARROW).setName("&7◀ Назад").build());
        player.openInventory(inv);
    }

    public void openAdminMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, ADMIN_MENU_TITLE);
        fillBorder(inv);
        inv.setItem(20, new ItemBuilder(Material.BOOK).setName("&eСписок кланов").setLore("&7Всего кланов: &f" + plugin.getClanManager().getClans().size()).build());
        inv.setItem(40, new ItemBuilder(Material.REPEATER).setName("&cПерезагрузить конфиг").setLore("&7Нажмите для перезагрузки").build());
        player.openInventory(inv);
    }

    private void fillBorder(Inventory inv) {
        ItemStack filler = ItemBuilder.createFiller();
        for (int i = 0; i < 9; i++) inv.setItem(i, filler);
        for (int i = inv.getSize() - 9; i < inv.getSize(); i++) inv.setItem(i, filler);
        for (int i = 0; i < inv.getSize(); i += 9) inv.setItem(i, filler);
        for (int i = 8; i < inv.getSize(); i += 9) inv.setItem(i, filler);
    }

    public void openClanManageMenu(Player player, Clan clan) {
        Inventory inv = Bukkit.createInventory(null, 27, CLAN_MANAGE_MENU_TITLE);
        fillBeautifulBorder(inv, clan.getHexColor());
        
        inv.setItem(10, new ItemBuilder(Material.NAME_TAG).setName("&eНастройка ролей").setLore("&7Измените названия и префиксы", "&7ролей вашего клана", "", "&b▶ Нажмите для открытия").build());
        inv.setItem(12, new ItemBuilder(Material.PAPER).setName("&eНастройка прав").setLore("&7Управляйте доступом участников", "&7к различным функциям", "", "&b▶ Нажмите для открытия").build());
        inv.setItem(14, new ItemBuilder(Material.PLAYER_HEAD).setSkullOwner(Bukkit.getOfflinePlayer(player.getUniqueId())).setName("&eУправление ролями").setLore("&7Полное управление иерархией", "&7и ролями клана", "", "&b▶ Нажмите для открытия").build());
        inv.setItem(16, new ItemBuilder(Material.TNT).setName("&c&lРасформировать клан").setLore("&cВНИМАНИЕ! Это действие", "&cневозможно отменить!", "", "&eНажмите для расформирования").build());
        
        inv.setItem(22, new ItemBuilder(Material.ARROW).setName("&7Назад").build());
        player.openInventory(inv);
    }

    public void openRoleSettingsMenu(Player player, Clan clan) {
        Inventory inv = Bukkit.createInventory(null, 54, ROLE_SETTINGS_MENU_TITLE);
        fillBeautifulBorder(inv, clan.getHexColor());
        
        List<ClanRank> ranks = new ArrayList<>(Arrays.asList(ClanRank.values()));
        ranks.sort((r1, r2) -> Integer.compare(r2.getPriority(), r1.getPriority()));
        
        int[] slots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};
        for (int i = 0; i < ranks.size() && i < slots.length; i++) {
            ClanRank rank = ranks.get(i);
            String rankName = rank.getDisplayName();
            String rankPrefix = rank.getPrefix();
            
            Map<String, Clan.ClanRankData> customRanks = clan.getCustomRanks();
            if (customRanks != null && customRanks.containsKey(rank.name())) {
                Clan.ClanRankData data = customRanks.get(rank.name());
                rankName = data.getName();
                rankPrefix = data.getPrefix();
            }

            inv.setItem(slots[i], new ItemBuilder(Material.NAME_TAG)
                .setName("&eРоль: " + rankName)
                .setLore(
                    "&7Приоритет: &f" + rank.getPriority(),
                    "&7Префикс: &f" + (rankPrefix.isEmpty() ? "&cНет" : rankPrefix),
                    "",
                    "&eЛКМ &7- Изменить название",
                    "&eПКМ &7- Изменить префикс",
                    "&eShift+ЛКМ &7- Настроить права"
                ).build());
        }
        
        inv.setItem(45, new ItemBuilder(Material.TNT).setName("&c&lРасформировать клан").setLore("&cВНИМАНИЕ! Это действие", "&cневозможно отменить!", "", "&eНажмите для расформирования").build());
        inv.setItem(49, new ItemBuilder(Material.ARROW).setName("&7Назад").build());
        player.openInventory(inv);
    }

    public void openPermissionSettingsMenu(Player player, Clan clan, ClanRank rank) {
        String rankName = rank.getDisplayName();
        Map<String, Clan.ClanRankData> customRanks = clan.getCustomRanks();
        if (customRanks != null && customRanks.containsKey(rank.name())) {
            rankName = customRanks.get(rank.name()).getName();
        }

        Inventory inv = Bukkit.createInventory(null, 54, "§8Права: " + rankName);
        fillBeautifulBorder(inv, clan.getHexColor());
        
        Clan.ClanRankData rankData = clan.getRankData(rank.name());
        Set<ClanPermission> activePerms = rankData.getPermissions();

        int slot = 10;
        for (ClanPermission perm : ClanPermission.values()) {
            if (slot > 43) break;
            if (slot % 9 == 0 || slot % 9 == 8) { slot++; }
            
            boolean has = activePerms.contains(perm);
            Material mat = has ? Material.LIME_DYE : Material.GRAY_DYE;
            String status = has ? "&aВключено" : "&cВыключено";
            
            inv.setItem(slot, new ItemBuilder(mat)
                .setName("&e" + perm.getDisplayName())
                .setLore(
                    "&7" + perm.getDescription(),
                    "",
                    "&7Статус: " + status,
                    "",
                    "&eНажмите для переключения"
                ).build());
            slot++;
            if (slot % 9 == 8) slot += 2;
        }
        
        inv.setItem(49, new ItemBuilder(Material.ARROW).setName("&7Назад").build());
        player.openInventory(inv);
    }

    private void fillBeautifulBorder(Inventory inv, String hexColor) {
        Material[] panes = getColoredPanes(hexColor);
        for (int i = 0; i < 9; i++) inv.setItem(i, new ItemBuilder(panes[i % panes.length]).setName(" ").build());
        for (int i = inv.getSize() - 9; i < inv.getSize(); i++) inv.setItem(i, new ItemBuilder(panes[i % panes.length]).setName(" ").build());
        for (int i = 0; i < inv.getSize(); i += 9) inv.setItem(i, new ItemBuilder(panes[(i / 9) % panes.length]).setName(" ").build());
        for (int i = 8; i < inv.getSize(); i += 9) inv.setItem(i, new ItemBuilder(panes[(i / 9) % panes.length]).setName(" ").build());
    }

    private Material[] getColoredPanes(String hexColor) {
        if (hexColor == null || !hexColor.startsWith("#") || hexColor.length() < 7) {
            return new Material[]{Material.BLACK_STAINED_GLASS_PANE, Material.GRAY_STAINED_GLASS_PANE};
        }
        try {
            int r = Integer.valueOf(hexColor.substring(1, 3), 16);
            int g = Integer.valueOf(hexColor.substring(3, 5), 16);
            int b = Integer.valueOf(hexColor.substring(5, 7), 16);
            
            if (r > 200 && g < 100 && b < 100) return new Material[]{Material.RED_STAINED_GLASS_PANE, Material.ORANGE_STAINED_GLASS_PANE};
            if (g > 200 && r < 100 && b < 100) return new Material[]{Material.LIME_STAINED_GLASS_PANE, Material.GREEN_STAINED_GLASS_PANE};
            if (b > 200 && r < 100 && g < 100) return new Material[]{Material.BLUE_STAINED_GLASS_PANE, Material.CYAN_STAINED_GLASS_PANE};
        } catch (Exception ignored) {}
        return new Material[]{Material.BLACK_STAINED_GLASS_PANE, Material.GRAY_STAINED_GLASS_PANE};
    }

    private String formatDate(long timestamp) { return new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm").format(new Date(timestamp)); }
    public int getStoragePage(UUID uuid) { return storagePage.getOrDefault(uuid, 0); }
    public StorageViewMode getStorageViewMode(UUID uuid) { return storageViewMode.getOrDefault(uuid, StorageViewMode.VIEW); }
    public enum StorageViewMode { VIEW, EDIT, DEPOSIT_ONLY, WITHDRAW_ONLY }
}
