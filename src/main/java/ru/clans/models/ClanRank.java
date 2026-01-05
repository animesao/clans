package ru.clans.models;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public enum ClanRank {

    RECRUIT("Новичок", "&7[Н]", 0, 
        ClanPermission.CHAT, 
        ClanPermission.HOME,
        ClanPermission.STORAGE_VIEW,
        ClanPermission.STORAGE_DEPOSIT),
    
    MEMBER("Участник", "&a[У]", 1, 
        ClanPermission.CHAT, 
        ClanPermission.HOME, 
        ClanPermission.STORAGE_VIEW,
        ClanPermission.STORAGE_DEPOSIT,
        ClanPermission.STORAGE_WITHDRAW,
        ClanPermission.BANK_DEPOSIT),
    
    OFFICER("Офицер", "&e[О]", 2, 
        ClanPermission.CHAT, 
        ClanPermission.HOME, 
        ClanPermission.STORAGE_VIEW, 
        ClanPermission.STORAGE_MODIFY,
        ClanPermission.STORAGE_DEPOSIT,
        ClanPermission.STORAGE_WITHDRAW,
        ClanPermission.BANK_DEPOSIT,
        ClanPermission.INVITE),
    
    ELDER("Старейшина", "&6[С]", 3, 
        ClanPermission.CHAT, 
        ClanPermission.HOME, 
        ClanPermission.STORAGE_VIEW, 
        ClanPermission.STORAGE_MODIFY,
        ClanPermission.STORAGE_DEPOSIT,
        ClanPermission.STORAGE_WITHDRAW,
        ClanPermission.BANK_DEPOSIT,
        ClanPermission.BANK_WITHDRAW,
        ClanPermission.INVITE, 
        ClanPermission.KICK,
        ClanPermission.SET_HOME),
    
    COLEADER("Со-Лидер", "&b[СЛ]", 4, 
        ClanPermission.CHAT, 
        ClanPermission.HOME, 
        ClanPermission.STORAGE_VIEW, 
        ClanPermission.STORAGE_MODIFY,
        ClanPermission.STORAGE_DEPOSIT,
        ClanPermission.STORAGE_WITHDRAW,
        ClanPermission.BANK_DEPOSIT,
        ClanPermission.BANK_WITHDRAW,
        ClanPermission.INVITE, 
        ClanPermission.KICK,
        ClanPermission.SET_HOME, 
        ClanPermission.PROMOTE,
        ClanPermission.DEMOTE,
        ClanPermission.MANAGE_QUESTS),
    
    LEADER("Лидер", "&c[Л]", 5, 
        ClanPermission.values());

    private final String displayName;
    private final String prefix;
    private final int priority;
    private final Set<ClanPermission> permissions;

    ClanRank(String displayName, String prefix, int priority, ClanPermission... permissions) {
        this.displayName = displayName;
        this.prefix = prefix;
        this.priority = priority;
        this.permissions = new HashSet<>(Arrays.asList(permissions));
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getPrefix() {
        return prefix;
    }

    public int getPriority() {
        return priority;
    }

    public Set<ClanPermission> getPermissions() {
        return permissions;
    }

    public boolean hasPermission(ClanPermission permission) {
        return permissions.contains(permission);
    }

    public boolean isHigherThan(ClanRank other) {
        return this.priority > other.priority;
    }

    public boolean isHigherOrEqual(ClanRank other) {
        return this.priority >= other.priority;
    }

    public ClanRank getNextRank() {
        ClanRank[] ranks = values();
        for (int i = 0; i < ranks.length - 1; i++) {
            if (ranks[i] == this) {
                return ranks[i + 1];
            }
        }
        return this;
    }

    public ClanRank getPreviousRank() {
        ClanRank[] ranks = values();
        for (int i = 1; i < ranks.length; i++) {
            if (ranks[i] == this) {
                return ranks[i - 1];
            }
        }
        return this;
    }

    public static ClanRank fromString(String name) {
        try {
            return valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return RECRUIT;
        }
    }
}
