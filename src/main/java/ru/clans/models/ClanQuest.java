package ru.clans.models;

import java.util.UUID;

public class ClanQuest {

    private String id;
    private String name;
    private String description;
    private QuestType type;
    private int targetAmount;
    private int currentProgress;
    private long experienceReward;
    private long startedAt;
    private long expiresAt;
    private UUID startedBy;
    private boolean completed;

    public ClanQuest(String id, String name, String description, QuestType type, 
                     int targetAmount, long experienceReward, long durationMillis) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.type = type;
        this.targetAmount = targetAmount;
        this.currentProgress = 0;
        this.experienceReward = experienceReward;
        this.startedAt = System.currentTimeMillis();
        this.expiresAt = startedAt + durationMillis;
        this.completed = false;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public QuestType getType() {
        return type;
    }

    public void setType(QuestType type) {
        this.type = type;
    }

    public int getTargetAmount() {
        return targetAmount;
    }

    public void setTargetAmount(int targetAmount) {
        this.targetAmount = targetAmount;
    }

    public int getCurrentProgress() {
        return currentProgress;
    }

    public void setCurrentProgress(int currentProgress) {
        this.currentProgress = currentProgress;
    }

    public void addProgress(int amount) {
        this.currentProgress += amount;
        if (this.currentProgress >= targetAmount) {
            this.currentProgress = targetAmount;
            this.completed = true;
        }
    }

    public long getExperienceReward() {
        return experienceReward;
    }

    public void setExperienceReward(long experienceReward) {
        this.experienceReward = experienceReward;
    }

    public long getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(long startedAt) {
        this.startedAt = startedAt;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(long expiresAt) {
        this.expiresAt = expiresAt;
    }

    public UUID getStartedBy() {
        return startedBy;
    }

    public void setStartedBy(UUID startedBy) {
        this.startedBy = startedBy;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expiresAt;
    }

    public double getProgressPercent() {
        return (double) currentProgress / targetAmount * 100;
    }

    public long getRemainingTime() {
        return Math.max(0, expiresAt - System.currentTimeMillis());
    }

    public String getFormattedRemainingTime() {
        long remaining = getRemainingTime();
        long hours = remaining / (1000 * 60 * 60);
        long minutes = (remaining % (1000 * 60 * 60)) / (1000 * 60);
        return String.format("%dч %dмин", hours, minutes);
    }
}
