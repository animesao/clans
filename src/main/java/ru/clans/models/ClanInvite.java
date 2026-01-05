package ru.clans.models;

import java.util.UUID;

public class ClanInvite {

    private String clanId;
    private UUID invitedPlayer;
    private UUID invitedBy;
    private long createdAt;
    private long expiresAt;

    public ClanInvite(String clanId, UUID invitedPlayer, UUID invitedBy, long duration) {
        this.clanId = clanId;
        this.invitedPlayer = invitedPlayer;
        this.invitedBy = invitedBy;
        this.createdAt = System.currentTimeMillis();
        this.expiresAt = createdAt + duration;
    }

    public String getClanId() {
        return clanId;
    }

    public UUID getInvitedPlayer() {
        return invitedPlayer;
    }

    public UUID getInvitedBy() {
        return invitedBy;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expiresAt;
    }

    public long getRemainingTime() {
        return Math.max(0, expiresAt - System.currentTimeMillis());
    }
}
