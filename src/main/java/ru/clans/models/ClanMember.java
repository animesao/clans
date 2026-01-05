package ru.clans.models;

import java.util.UUID;

public class ClanMember {

    private UUID uuid;
    private String name;
    private ClanRank rank;
    private long joinedAt;
    private long lastOnline;
    private int contribution;
    private int kills;
    private int deaths;

    private Clan clan;

    public ClanMember(UUID uuid, String name, ClanRank rank) {
        this.uuid = uuid;
        this.name = name;
        this.rank = rank;
        this.joinedAt = System.currentTimeMillis();
        this.lastOnline = System.currentTimeMillis();
        this.contribution = 0;
        this.kills = 0;
        this.deaths = 0;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ClanRank getRank() {
        return rank;
    }

    public void setRank(ClanRank rank) {
        this.rank = rank;
    }

    public long getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(long joinedAt) {
        this.joinedAt = joinedAt;
    }

    public long getLastOnline() {
        return lastOnline;
    }

    public void setLastOnline(long lastOnline) {
        this.lastOnline = lastOnline;
    }

    public int getContribution() {
        return contribution;
    }

    public void setContribution(int contribution) {
        this.contribution = contribution;
    }

    public void addContribution(int amount) {
        this.contribution += amount;
    }

    public int getKills() {
        return kills;
    }

    public void setKills(int kills) {
        this.kills = kills;
    }

    public void addKill() {
        this.kills++;
    }

    public int getDeaths() {
        return deaths;
    }

    public void setDeaths(int deaths) {
        this.deaths = deaths;
    }

    public void addDeath() {
        this.deaths++;
    }

    public double getKDR() {
        if (deaths == 0) return kills;
        return (double) kills / deaths;
    }

    public boolean hasPermission(ClanPermission permission) {
        if (clan == null) return false;
        Clan.ClanRankData customRank = clan.getCustomRanks().get(rank.name());
        if (customRank != null) {
            return customRank.getPermissions().contains(permission);
        }
        return rank.hasPermission(permission);
    }

    public Clan getClan() { return clan; }
    public void setClan(Clan clan) { this.clan = clan; }

    public void resetStats() {
        this.kills = 0;
        this.deaths = 0;
        this.contribution = 0;
    }
}
