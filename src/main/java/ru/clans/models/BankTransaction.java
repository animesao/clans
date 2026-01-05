package ru.clans.models;

import java.util.UUID;

public class BankTransaction {

    public enum TransactionType {
        DEPOSIT("Внесение"),
        WITHDRAW("Снятие");

        private final String displayName;

        TransactionType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private UUID playerUuid;
    private String playerName;
    private TransactionType type;
    private double amount;
    private long timestamp;

    public BankTransaction(UUID playerUuid, String playerName, TransactionType type, double amount) {
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.type = type;
        this.amount = amount;
        this.timestamp = System.currentTimeMillis();
    }

    public BankTransaction(UUID playerUuid, String playerName, TransactionType type, double amount, long timestamp) {
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.type = type;
        this.amount = amount;
        this.timestamp = timestamp;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public String getPlayerName() {
        return playerName;
    }

    public TransactionType getType() {
        return type;
    }

    public double getAmount() {
        return amount;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getFormattedTime() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm");
        return sdf.format(new java.util.Date(timestamp));
    }
}
