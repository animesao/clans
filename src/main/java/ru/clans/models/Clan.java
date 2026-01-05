package ru.clans.models;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;

public class Clan {

    private String id;
    private String name;
    private String tag;
    private String description;
    private UUID leader;
    private int level;
    private long experience;
    private long createdAt;
    private Map<UUID, ClanMember> members;
    private Location home;
    private List<ItemStack[]> storage;
    private int storageLevel;
    private Map<String, ClanQuest> activeQuests;
    private Map<String, Integer> completedQuests;
    private String hexColor;
    private boolean pvpEnabled;
    private int kills;
    private int deaths;
    private double bank;
    private Deque<BankTransaction> bankHistory;
    private static final int MAX_BANK_HISTORY = 50;
    private Map<String, ClanRankData> customRanks;

    public static class ClanRankData {
        private String name;
        private String prefix;
        private int priority;
        private Set<ClanPermission> permissions;

        public ClanRankData(String name, String prefix, int priority, Set<ClanPermission> permissions) {
            this.name = name;
            this.prefix = prefix;
            this.priority = priority;
            this.permissions = permissions;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getPrefix() { return prefix; }
        public void setPrefix(String prefix) { this.prefix = prefix; }
        public int getPriority() { return priority; }
        public Set<ClanPermission> getPermissions() { return permissions; }
    }

    public Clan(String id, String name, String tag, UUID leader) {
        this.id = id;
        this.name = name;
        this.tag = tag;
        this.leader = leader;
        this.level = 1;
        this.experience = 0;
        this.createdAt = System.currentTimeMillis();
        this.members = new HashMap<>();
        this.storage = new ArrayList<>();
        this.storageLevel = 1;
        this.activeQuests = new HashMap<>();
        this.completedQuests = new HashMap<>();
        this.hexColor = "#FFFFFF";
        this.pvpEnabled = false;
        this.kills = 0;
        this.deaths = 0;
        this.description = "";
        this.bank = 0.0;
        this.bankHistory = new ConcurrentLinkedDeque<>();
        this.customRanks = new HashMap<>();
        
        // Инициализируем страницы согласно лимиту 1 уровня
        int initialPages = 1;
        try {
            initialPages = ru.clans.ClanPlugin.getInstance().getConfigManager().getStoragePageLimit(1);
        } catch (Exception ignored) {}
        
        for (int i = 0; i < initialPages; i++) {
            storage.add(new ItemStack[27]);
        }
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public UUID getLeader() { return leader; }
    public void setLeader(UUID leader) { this.leader = leader; }
    public int getLevel() { return level; }
    public void setLevel(int level) { 
        this.level = level; 
        updateStoragePages();
    }
    
    private void cleanLockedStorageSlots() {
        int unlockedSlots = ru.clans.ClanPlugin.getInstance().getConfigManager().getStorageSlotsForLevel(storageLevel);
        for (ItemStack[] page : storage) {
            for (int i = unlockedSlots; i < page.length; i++) {
                page[i] = null;
            }
        }
    }
    public long getExperience() { return experience; }
    public void setExperience(long experience) { this.experience = experience; }
    public void addExperience(long amount) { this.experience += amount; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public Map<UUID, ClanMember> getMembers() { return members; }
    public void setMembers(Map<UUID, ClanMember> members) { this.members = members; }
    public void addMember(ClanMember member) { 
        member.setClan(this);
        members.put(member.getUuid(), member); 
    }
    public void removeMember(UUID uuid) { members.remove(uuid); }
    public ClanMember getMember(UUID uuid) { return members.get(uuid); }
    public boolean isMember(UUID uuid) { return members.containsKey(uuid); }
    public boolean isLeader(UUID uuid) { return leader.equals(uuid); }
    public int getMemberCount() { return members.size(); }
    public Location getHome() { return home; }
    public void setHome(Location home) { this.home = home; }
    public boolean hasHome() { return home != null; }
    public List<ItemStack[]> getStorage() { return storage; }
    public void setStorage(List<ItemStack[]> storage) { this.storage = storage; }
    public int getStorageLevel() { return storageLevel; }
    public void setStorageLevel(int storageLevel) { 
        this.storageLevel = storageLevel;
        updateStoragePages();
    }
    
    public void updateStoragePages() {
        try {
            // Используем ConfigManager для получения лимита
            int maxPages = ru.clans.ClanPlugin.getInstance().getConfigManager().getStoragePageLimit(this.level);
            
            if (storage == null) storage = new ArrayList<>();
            
            // Если страниц в списке меньше, чем положено по уровню - добавляем новые
            while (storage.size() < maxPages) {
                storage.add(new ItemStack[27]);
            }
            
            // Мы НЕ удаляем лишние страницы (если уровень клана упал), 
            // чтобы не пропали вещи игроков. Они просто станут недоступны через GUI.
            
        } catch (Exception e) {
            if (storage == null) storage = new ArrayList<>();
            if (storage.isEmpty()) storage.add(new ItemStack[27]);
        }
        cleanLockedStorageSlots();
    }

    public void upgradeStorage() { 
        this.storageLevel++; 
        updateStoragePages();
    }
    public int getStoragePages() { return storage.size(); }
    public ItemStack[] getStoragePage(int page) {
        if (page >= 0 && page < storage.size()) return storage.get(page);
        return new ItemStack[27];
    }
    public void setStoragePage(int page, ItemStack[] items) {
        if (page >= 0 && page < storage.size()) storage.set(page, items);
    }
    public Map<String, ClanQuest> getActiveQuests() { return activeQuests; }
    public void setActiveQuests(Map<String, ClanQuest> activeQuests) { this.activeQuests = activeQuests; }
    public void addQuest(ClanQuest quest) { activeQuests.put(quest.getId(), quest); }
    public void removeQuest(String questId) { activeQuests.remove(questId); }
    public ClanQuest getQuest(String questId) { return activeQuests.get(questId); }
    public Map<String, Integer> getCompletedQuests() { return completedQuests; }
    public void setCompletedQuests(Map<String, Integer> completedQuests) { this.completedQuests = completedQuests; }
    public void completeQuest(String questId) {
        completedQuests.merge(questId, 1, Integer::sum);
        activeQuests.remove(questId);
    }
    public int getQuestCompletions(String questId) { return completedQuests.getOrDefault(questId, 0); }
    public String getHexColor() { return hexColor; }
    public void setHexColor(String hexColor) { this.hexColor = hexColor; }
    public boolean isPvpEnabled() { return pvpEnabled; }
    public void setPvpEnabled(boolean pvpEnabled) { this.pvpEnabled = pvpEnabled; }
    public int getKills() { return kills; }
    public void setKills(int kills) { this.kills = kills; }
    public void addKill() { this.kills++; }
    public int getDeaths() { return deaths; }
    public void setDeaths(int deaths) { this.deaths = deaths; }
    public void addDeath() { this.deaths++; }
    public double getKDR() {
        if (deaths == 0) return kills;
        return (double) kills / deaths;
    }
    public List<ClanMember> getOnlineMembers() {
        List<ClanMember> online = new ArrayList<>();
        for (ClanMember member : members.values()) {
            if (Bukkit.getPlayer(member.getUuid()) != null) online.add(member);
        }
        return online;
    }
    public List<ClanMember> getMembersByRank(ClanRank rank) {
        List<ClanMember> result = new ArrayList<>();
        for (ClanMember member : members.values()) {
            if (member.getRank() == rank) result.add(member);
        }
        return result;
    }
    public int getOnlineMemberCount() {
        int count = 0;
        for (ClanMember member : members.values()) {
            if (Bukkit.getPlayer(member.getUuid()) != null) count++;
        }
        return count;
    }
    public String getColoredTag() {
        if (hexColor == null || hexColor.isEmpty()) return "[" + tag + "]";
        return ru.clans.utils.ColorUtil.getHexColor(hexColor) + "[" + tag + "]";
    }
    public String getColoredName() {
        if (hexColor == null || hexColor.isEmpty()) return name;
        return ru.clans.utils.ColorUtil.getHexColor(hexColor) + name;
    }
    public void resetStats() {
        this.kills = 0;
        this.deaths = 0;
        this.experience = 0;
    }
    public void clearStorage() {
        storage.clear();
        for (int i = 0; i < storageLevel; i++) storage.add(new ItemStack[27]);
    }
    public double getBank() { return bank; }
    public void setBank(double bank) { this.bank = bank; }
    public void depositBank(double amount) { this.bank += amount; }
    public boolean withdrawBank(double amount) {
        if (this.bank >= amount) {
            this.bank -= amount;
            return true;
        }
        return false;
    }
    public void depositBank(double amount, UUID playerUuid, String playerName) {
        this.bank += amount;
        addBankTransaction(new BankTransaction(playerUuid, playerName, BankTransaction.TransactionType.DEPOSIT, amount));
    }
    public boolean withdrawBank(double amount, UUID playerUuid, String playerName) {
        if (this.bank >= amount) {
            this.bank -= amount;
            addBankTransaction(new BankTransaction(playerUuid, playerName, BankTransaction.TransactionType.WITHDRAW, amount));
            return true;
        }
        return false;
    }
    public void addBankTransaction(BankTransaction transaction) {
        if (bankHistory == null) bankHistory = new ConcurrentLinkedDeque<>();
        bankHistory.addFirst(transaction);
        while (bankHistory.size() > MAX_BANK_HISTORY) bankHistory.removeLast();
    }
    public Deque<BankTransaction> getBankHistory() {
        if (bankHistory == null) bankHistory = new ConcurrentLinkedDeque<>();
        return bankHistory;
    }
    public void setBankHistory(Deque<BankTransaction> history) { this.bankHistory = history; }
    public List<BankTransaction> getRecentTransactions(int count) {
        if (bankHistory == null) return new ArrayList<>();
        List<BankTransaction> recent = new ArrayList<>();
        int i = 0;
        for (BankTransaction transaction : bankHistory) {
            if (i >= count) break;
            recent.add(transaction);
            i++;
        }
        return recent;
    }
    public Map<String, ClanRankData> getCustomRanks() { return customRanks; }
    public void setCustomRanks(Map<String, ClanRankData> customRanks) { this.customRanks = customRanks; }

    public ClanRankData getRankData(String rankName) {
        if (!customRanks.containsKey(rankName)) {
            ClanRank rank = ClanRank.valueOf(rankName);
            customRanks.put(rankName, new ClanRankData(rank.getDisplayName(), rank.getPrefix(), rank.getPriority(), new HashSet<>()));
        }
        return customRanks.get(rankName);
    }
}
