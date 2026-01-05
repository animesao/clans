package ru.clans.managers;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import ru.clans.ClanPlugin;
import ru.clans.models.Clan;
import ru.clans.models.ClanQuest;
import ru.clans.models.QuestType;
import ru.clans.utils.MessageUtil;

import java.util.*;

public class QuestManager {

    private final ClanPlugin plugin;
    private final Map<String, QuestTemplate> questTemplates;

    public QuestManager(ClanPlugin plugin) {
        this.plugin = plugin;
        this.questTemplates = new HashMap<>();
        loadQuestTemplates();
    }

    private void loadQuestTemplates() {
        ConfigurationSection section = plugin.getConfigManager().getQuestsConfig().getConfigurationSection("quests");
        if (section == null) return;
        
        for (String key : section.getKeys(false)) {
            ConfigurationSection questSection = section.getConfigurationSection(key);
            if (questSection == null) continue;
            
            QuestTemplate template = new QuestTemplate(
                key,
                questSection.getString("name", "Задание"),
                questSection.getString("description", ""),
                QuestType.fromString(questSection.getString("type", "KILL_MOBS")),
                questSection.getInt("target", 100),
                questSection.getLong("experience", 1000),
                questSection.getLong("duration", 86400000),
                questSection.getInt("min-level", 1)
            );
            
            questTemplates.put(key, template);
        }
        
        if (questTemplates.isEmpty()) {
            createDefaultTemplates();
        }
        
        plugin.getLogger().info("Loaded " + questTemplates.size() + " quest templates");
    }

    private void createDefaultTemplates() {
        questTemplates.put("kill_mobs_easy", new QuestTemplate(
            "kill_mobs_easy", "Охотник на мобов", "Убейте 50 мобов",
            QuestType.KILL_MOBS, 50, 500, 86400000, 1
        ));
        
        questTemplates.put("kill_mobs_medium", new QuestTemplate(
            "kill_mobs_medium", "Опытный охотник", "Убейте 200 мобов",
            QuestType.KILL_MOBS, 200, 2000, 86400000, 5
        ));
        
        questTemplates.put("mine_ores", new QuestTemplate(
            "mine_ores", "Шахтёр", "Добудьте 100 руды",
            QuestType.MINE_ORES, 100, 1500, 86400000, 3
        ));
        
        questTemplates.put("kill_players", new QuestTemplate(
            "kill_players", "Воин", "Убейте 10 игроков",
            QuestType.KILL_PLAYERS, 10, 3000, 86400000, 10
        ));
        
        questTemplates.put("travel", new QuestTemplate(
            "travel", "Путешественник", "Пройдите 10000 блоков",
            QuestType.TRAVEL_DISTANCE, 10000, 1000, 86400000, 1
        ));
        
        questTemplates.put("fish", new QuestTemplate(
            "fish", "Рыбак", "Поймайте 50 рыб",
            QuestType.FISH, 50, 800, 86400000, 2
        ));
    }

    public ClanQuest startQuest(Clan clan, String templateId, Player startedBy) {
        QuestTemplate template = questTemplates.get(templateId);
        if (template == null) {
            MessageUtil.send(startedBy, "&cЗадание не найдено!");
            return null;
        }
        
        if (clan.getLevel() < template.minLevel) {
            MessageUtil.send(startedBy, "&cТребуется уровень клана: &f" + template.minLevel);
            return null;
        }
        
        if (clan.getActiveQuests().containsKey(templateId)) {
            MessageUtil.send(startedBy, "&cЭто задание уже активно!");
            return null;
        }
        
        ClanQuest quest = new ClanQuest(
            templateId,
            template.name,
            template.description,
            template.type,
            template.target,
            template.experience,
            template.duration
        );
        quest.setStartedBy(startedBy.getUniqueId());
        
        clan.addQuest(quest);
        plugin.getDataManager().saveClan(clan);
        
        MessageUtil.sendClanMessage(clan, "&aНовое задание: &f" + quest.getName());
        MessageUtil.sendClanMessage(clan, "&7" + quest.getDescription());
        
        return quest;
    }

    public void progressQuest(Clan clan, QuestType type, int amount) {
        List<ClanQuest> toComplete = new ArrayList<>();
        
        for (ClanQuest quest : clan.getActiveQuests().values()) {
            if (quest.getType() == type && !quest.isCompleted() && !quest.isExpired()) {
                quest.addProgress(amount);
                
                if (quest.isCompleted()) {
                    toComplete.add(quest);
                }
            }
        }
        
        for (ClanQuest quest : toComplete) {
            completeQuest(clan, quest);
        }
        
        if (!toComplete.isEmpty()) {
            plugin.getDataManager().saveClan(clan);
        }
    }

    private void completeQuest(Clan clan, ClanQuest quest) {
        clan.completeQuest(quest.getId());
        
        plugin.getLevelManager().addExperience(clan, quest.getExperienceReward(), "Quest: " + quest.getName());
        
        MessageUtil.sendClanMessage(clan, "&a&lЗадание выполнено: &f" + quest.getName());
        MessageUtil.sendClanMessage(clan, "&7Получено опыта: &a+" + MessageUtil.formatNumber(quest.getExperienceReward()));
    }

    public void checkExpiredQuests(Clan clan) {
        List<String> expired = new ArrayList<>();
        
        for (ClanQuest quest : clan.getActiveQuests().values()) {
            if (quest.isExpired() && !quest.isCompleted()) {
                expired.add(quest.getId());
            }
        }
        
        for (String questId : expired) {
            ClanQuest quest = clan.getQuest(questId);
            clan.removeQuest(questId);
            MessageUtil.sendClanMessage(clan, "&cЗадание &f" + quest.getName() + " &cистекло!");
        }
        
        if (!expired.isEmpty()) {
            plugin.getDataManager().saveClan(clan);
        }
    }

    public Collection<QuestTemplate> getAvailableQuests(Clan clan) {
        List<QuestTemplate> available = new ArrayList<>();
        
        for (QuestTemplate template : questTemplates.values()) {
            if (clan.getLevel() >= template.minLevel && !clan.getActiveQuests().containsKey(template.id)) {
                available.add(template);
            }
        }
        
        return available;
    }

    public QuestTemplate getTemplate(String id) {
        return questTemplates.get(id);
    }

    public Collection<QuestTemplate> getAllTemplates() {
        return questTemplates.values();
    }

    public static class QuestTemplate {
        public final String id;
        public final String name;
        public final String description;
        public final QuestType type;
        public final int target;
        public final long experience;
        public final long duration;
        public final int minLevel;

        public QuestTemplate(String id, String name, String description, QuestType type,
                           int target, long experience, long duration, int minLevel) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.type = type;
            this.target = target;
            this.experience = experience;
            this.duration = duration;
            this.minLevel = minLevel;
        }
    }
}
