package ru.clans.models;

public enum ClanPermission {

    CHAT("Чат клана"),
    HOME("Телепортация на базу"),
    SET_HOME("Установка базы"),
    INVITE("Приглашение игроков"),
    KICK("Исключение игроков"),
    PROMOTE("Повышение игроков"),
    DEMOTE("Понижение игроков"),
    STORAGE_VIEW("Просмотр хранилища"),
    STORAGE_MODIFY("Изменение хранилища"),
    STORAGE_DEPOSIT("Положить в хранилище"),
    STORAGE_WITHDRAW("Забрать из хранилища"),
    MANAGE_QUESTS("Управление заданиями"),
    UPGRADE_STORAGE("Улучшение хранилища"),
    CHANGE_SETTINGS("Изменение настроек"),
    DISBAND("Расформирование клана"),
    BANK_DEPOSIT("Положить в казну"),
    BANK_WITHDRAW("Забрать из казны");

    private final String displayName;
    private final String description;

    ClanPermission(String displayName) {
        this.displayName = displayName;
        this.description = "";
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return displayName;
    }
}
