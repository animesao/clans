package ru.clans.models;

import org.bukkit.Material;

public enum QuestType {

    KILL_MOBS("Убийство мобов", Material.DIAMOND_SWORD),
    KILL_PLAYERS("Убийство игроков", Material.IRON_SWORD),
    MINE_BLOCKS("Добыча блоков", Material.DIAMOND_PICKAXE),
    MINE_ORES("Добыча руды", Material.DIAMOND_ORE),
    CRAFT_ITEMS("Крафт предметов", Material.CRAFTING_TABLE),
    PLACE_BLOCKS("Размещение блоков", Material.GRASS_BLOCK),
    FISH("Рыбалка", Material.FISHING_ROD),
    BREED_ANIMALS("Разведение животных", Material.WHEAT),
    ENCHANT_ITEMS("Зачарование предметов", Material.ENCHANTING_TABLE),
    TRADE_VILLAGERS("Торговля с жителями", Material.EMERALD),
    TRAVEL_DISTANCE("Пройти расстояние", Material.LEATHER_BOOTS),
    COLLECT_EXPERIENCE("Собрать опыт", Material.EXPERIENCE_BOTTLE);

    private final String displayName;
    private final Material icon;

    QuestType(String displayName, Material icon) {
        this.displayName = displayName;
        this.icon = icon;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Material getIcon() {
        return icon;
    }

    public static QuestType fromString(String name) {
        try {
            return valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return KILL_MOBS;
        }
    }
}
