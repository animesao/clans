package ru.clans.managers;

import com.google.gson.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import ru.clans.ClanPlugin;
import ru.clans.models.*;

import java.io.*;
import java.util.*;

public class DataManager {

    private final ClanPlugin plugin;
    private final File clansFolder;
    private final Gson gson;

    public DataManager(ClanPlugin plugin) {
        this.plugin = plugin;
        this.clansFolder = new File(plugin.getDataFolder(), "clans");
        
        if (!clansFolder.exists()) {
            clansFolder.mkdirs();
        }
        
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();
    }

    public void saveAllClans() {
        for (Clan clan : plugin.getClanManager().getClans()) {
            saveClan(clan);
        }
        plugin.getLogger().info("Saved " + plugin.getClanManager().getClans().size() + " clans");
    }

    public void loadAllClans() {
        File[] files = clansFolder.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) return;
        
        int loaded = 0;
        for (File file : files) {
            try {
                Clan clan = loadClan(file);
                if (clan != null) {
                    plugin.getClanManager().loadClan(clan);
                    loaded++;
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load clan from " + file.getName() + ": " + e.getMessage());
            }
        }
        plugin.getLogger().info("Loaded " + loaded + " clans");
    }

    public void saveClan(Clan clan) {
        File file = new File(clansFolder, clan.getId() + ".json");
        
        try (Writer writer = new FileWriter(file)) {
            JsonObject json = new JsonObject();
            
            json.addProperty("id", clan.getId());
            json.addProperty("name", clan.getName());
            json.addProperty("tag", clan.getTag());
            json.addProperty("description", clan.getDescription());
            json.addProperty("leader", clan.getLeader().toString());
            json.addProperty("level", clan.getLevel());
            json.addProperty("experience", clan.getExperience());
            json.addProperty("createdAt", clan.getCreatedAt());
            json.addProperty("hexColor", clan.getHexColor());
            json.addProperty("pvpEnabled", clan.isPvpEnabled());
            json.addProperty("kills", clan.getKills());
            json.addProperty("deaths", clan.getDeaths());
            json.addProperty("storageLevel", clan.getStorageLevel());
            json.addProperty("bank", clan.getBank());
            
            JsonObject customRanks = new JsonObject();
            for (Map.Entry<String, Clan.ClanRankData> entry : clan.getCustomRanks().entrySet()) {
                JsonObject rankJson = new JsonObject();
                Clan.ClanRankData rankData = entry.getValue();
                rankJson.addProperty("name", rankData.getName());
                rankJson.addProperty("prefix", rankData.getPrefix());
                rankJson.addProperty("priority", rankData.getPriority());
                
                JsonArray perms = new JsonArray();
                for (ClanPermission perm : rankData.getPermissions()) {
                    perms.add(perm.name());
                }
                rankJson.add("permissions", perms);
                customRanks.add(entry.getKey(), rankJson);
            }
            json.add("customRanks", customRanks);
            
            if (clan.hasHome()) {
                json.add("home", serializeLocation(clan.getHome()));
            }
            
            JsonArray membersArray = new JsonArray();
            for (ClanMember member : clan.getMembers().values()) {
                membersArray.add(serializeMember(member));
            }
            json.add("members", membersArray);
            
            JsonArray storageArray = new JsonArray();
            for (ItemStack[] page : clan.getStorage()) {
                storageArray.add(serializeInventory(page));
            }
            json.add("storage", storageArray);
            
            JsonArray questsArray = new JsonArray();
            for (ClanQuest quest : clan.getActiveQuests().values()) {
                questsArray.add(serializeQuest(quest));
            }
            json.add("activeQuests", questsArray);
            
            JsonObject completedQuests = new JsonObject();
            for (Map.Entry<String, Integer> entry : clan.getCompletedQuests().entrySet()) {
                completedQuests.addProperty(entry.getKey(), entry.getValue());
            }
            json.add("completedQuests", completedQuests);
            
            gson.toJson(json, writer);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save clan " + clan.getName() + ": " + e.getMessage());
        }
    }

    public Clan loadClan(File file) throws IOException {
        try (Reader reader = new FileReader(file)) {
            JsonParser parser = new JsonParser();
            JsonObject json = parser.parse(reader).getAsJsonObject();
            
            String id = json.get("id").getAsString();
            String name = json.get("name").getAsString();
            String tag = json.get("tag").getAsString();
            UUID leader = UUID.fromString(json.get("leader").getAsString());
            
            Clan clan = new Clan(id, name, tag, leader);
            
            clan.setDescription(getStringOrDefault(json, "description", ""));
            clan.setLevel(json.get("level").getAsInt());
            clan.setExperience(json.get("experience").getAsLong());
            clan.setCreatedAt(json.get("createdAt").getAsLong());
            clan.setHexColor(getStringOrDefault(json, "hexColor", "#FFFFFF"));
            clan.setPvpEnabled(getBooleanOrDefault(json, "pvpEnabled", false));
            clan.setKills(getIntOrDefault(json, "kills", 0));
            clan.setDeaths(getIntOrDefault(json, "deaths", 0));
            clan.setStorageLevel(getIntOrDefault(json, "storageLevel", 1));
            clan.setBank(getDoubleOrDefault(json, "bank", 0.0));
            
            // Ensure storage pages are initialized based on current level after loading
            clan.updateStoragePages();
            
            if (json.has("customRanks")) {
                Map<String, Clan.ClanRankData> customRanks = new HashMap<>();
                JsonObject ranksJson = json.getAsJsonObject("customRanks");
                for (Map.Entry<String, JsonElement> entry : ranksJson.entrySet()) {
                    JsonObject rankJson = entry.getValue().getAsJsonObject();
                    String rName = rankJson.get("name").getAsString();
                    String rPrefix = rankJson.get("prefix").getAsString();
                    int rPriority = rankJson.get("priority").getAsInt();
                    
                    Set<ClanPermission> perms = new HashSet<>();
                    if (rankJson.has("permissions")) {
                        JsonArray permsArray = rankJson.getAsJsonArray("permissions");
                        for (JsonElement pElem : permsArray) {
                            try {
                                perms.add(ClanPermission.valueOf(pElem.getAsString()));
                            } catch (IllegalArgumentException ignored) {}
                        }
                    }
                    customRanks.put(entry.getKey(), new Clan.ClanRankData(rName, rPrefix, rPriority, perms));
                }
                clan.setCustomRanks(customRanks);
            }
            
            if (json.has("home") && !json.get("home").isJsonNull()) {
                clan.setHome(deserializeLocation(json.getAsJsonObject("home")));
            }
            
            clan.getMembers().clear();
            JsonArray membersArray = json.getAsJsonArray("members");
            for (JsonElement element : membersArray) {
                ClanMember member = deserializeMember(element.getAsJsonObject());
                clan.addMember(member);
            }
            
            if (json.has("storage")) {
                List<ItemStack[]> storage = new ArrayList<>();
                JsonArray storageArray = json.getAsJsonArray("storage");
                for (JsonElement element : storageArray) {
                    storage.add(deserializeInventory(element.getAsString()));
                }
                clan.setStorage(storage);
            }
            
            if (json.has("activeQuests")) {
                JsonArray questsArray = json.getAsJsonArray("activeQuests");
                for (JsonElement element : questsArray) {
                    ClanQuest quest = deserializeQuest(element.getAsJsonObject());
                    clan.addQuest(quest);
                }
            }
            
            if (json.has("completedQuests")) {
                Map<String, Integer> completedQuests = new HashMap<>();
                JsonObject completed = json.getAsJsonObject("completedQuests");
                for (Map.Entry<String, JsonElement> entry : completed.entrySet()) {
                    completedQuests.put(entry.getKey(), entry.getValue().getAsInt());
                }
                clan.setCompletedQuests(completedQuests);
            }
            
            return clan;
        }
    }

    public void deleteClan(String clanId) {
        File file = new File(clansFolder, clanId + ".json");
        if (file.exists()) {
            file.delete();
        }
    }

    private JsonObject serializeLocation(Location loc) {
        JsonObject json = new JsonObject();
        json.addProperty("world", loc.getWorld().getName());
        json.addProperty("x", loc.getX());
        json.addProperty("y", loc.getY());
        json.addProperty("z", loc.getZ());
        json.addProperty("yaw", loc.getYaw());
        json.addProperty("pitch", loc.getPitch());
        return json;
    }

    private Location deserializeLocation(JsonObject json) {
        World world = Bukkit.getWorld(json.get("world").getAsString());
        if (world == null) return null;
        
        double x = json.get("x").getAsDouble();
        double y = json.get("y").getAsDouble();
        double z = json.get("z").getAsDouble();
        float yaw = json.get("yaw").getAsFloat();
        float pitch = json.get("pitch").getAsFloat();
        
        return new Location(world, x, y, z, yaw, pitch);
    }

    private JsonObject serializeMember(ClanMember member) {
        JsonObject json = new JsonObject();
        json.addProperty("uuid", member.getUuid().toString());
        json.addProperty("name", member.getName());
        json.addProperty("rank", member.getRank().name());
        json.addProperty("joinedAt", member.getJoinedAt());
        json.addProperty("lastOnline", member.getLastOnline());
        json.addProperty("contribution", member.getContribution());
        json.addProperty("kills", member.getKills());
        json.addProperty("deaths", member.getDeaths());
        return json;
    }

    private ClanMember deserializeMember(JsonObject json) {
        UUID uuid = UUID.fromString(json.get("uuid").getAsString());
        String name = json.get("name").getAsString();
        ClanRank rank = ClanRank.fromString(json.get("rank").getAsString());
        
        ClanMember member = new ClanMember(uuid, name, rank);
        member.setJoinedAt(json.get("joinedAt").getAsLong());
        member.setLastOnline(json.get("lastOnline").getAsLong());
        member.setContribution(getIntOrDefault(json, "contribution", 0));
        member.setKills(getIntOrDefault(json, "kills", 0));
        member.setDeaths(getIntOrDefault(json, "deaths", 0));
        
        return member;
    }

    private JsonObject serializeQuest(ClanQuest quest) {
        JsonObject json = new JsonObject();
        json.addProperty("id", quest.getId());
        json.addProperty("name", quest.getName());
        json.addProperty("description", quest.getDescription());
        json.addProperty("type", quest.getType().name());
        json.addProperty("targetAmount", quest.getTargetAmount());
        json.addProperty("currentProgress", quest.getCurrentProgress());
        json.addProperty("experienceReward", quest.getExperienceReward());
        json.addProperty("startedAt", quest.getStartedAt());
        json.addProperty("expiresAt", quest.getExpiresAt());
        if (quest.getStartedBy() != null) {
            json.addProperty("startedBy", quest.getStartedBy().toString());
        }
        json.addProperty("completed", quest.isCompleted());
        return json;
    }

    private ClanQuest deserializeQuest(JsonObject json) {
        String id = json.get("id").getAsString();
        String name = json.get("name").getAsString();
        String description = json.get("description").getAsString();
        QuestType type = QuestType.fromString(json.get("type").getAsString());
        int targetAmount = json.get("targetAmount").getAsInt();
        long experienceReward = json.get("experienceReward").getAsLong();
        
        ClanQuest quest = new ClanQuest(id, name, description, type, targetAmount, experienceReward, 0);
        quest.setCurrentProgress(json.get("currentProgress").getAsInt());
        quest.setStartedAt(json.get("startedAt").getAsLong());
        quest.setExpiresAt(json.get("expiresAt").getAsLong());
        quest.setCompleted(json.get("completed").getAsBoolean());
        
        if (json.has("startedBy") && !json.get("startedBy").isJsonNull()) {
            quest.setStartedBy(UUID.fromString(json.get("startedBy").getAsString()));
        }
        
        return quest;
    }

    private String serializeInventory(ItemStack[] items) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
            
            dataOutput.writeInt(items.length);
            for (ItemStack item : items) {
                dataOutput.writeObject(item);
            }
            dataOutput.close();
            
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (IOException e) {
            return "";
        }
    }

    private ItemStack[] deserializeInventory(String data) {
        if (data == null || data.isEmpty()) {
            return new ItemStack[27];
        }
        
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(data));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            
            int size = dataInput.readInt();
            ItemStack[] items = new ItemStack[size];
            
            for (int i = 0; i < size; i++) {
                items[i] = (ItemStack) dataInput.readObject();
            }
            dataInput.close();
            
            return items;
        } catch (Exception e) {
            return new ItemStack[27];
        }
    }

    private String getStringOrDefault(JsonObject json, String key, String defaultValue) {
        return json.has(key) && !json.get(key).isJsonNull() ? json.get(key).getAsString() : defaultValue;
    }

    private int getIntOrDefault(JsonObject json, String key, int defaultValue) {
        return json.has(key) ? json.get(key).getAsInt() : defaultValue;
    }

    private boolean getBooleanOrDefault(JsonObject json, String key, boolean defaultValue) {
        return json.has(key) ? json.get(key).getAsBoolean() : defaultValue;
    }

    private double getDoubleOrDefault(JsonObject json, String key, double defaultValue) {
        return json.has(key) ? json.get(key).getAsDouble() : defaultValue;
    }
}
