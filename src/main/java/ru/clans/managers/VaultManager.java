package ru.clans.managers;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import ru.clans.ClanPlugin;

public class VaultManager {

    private final ClanPlugin plugin;
    private Economy economy;
    private boolean vaultEnabled;

    public VaultManager(ClanPlugin plugin) {
        this.plugin = plugin;
        this.vaultEnabled = setupEconomy();
        
        if (vaultEnabled) {
            plugin.getLogger().info("Vault economy integration enabled!");
        } else {
            plugin.getLogger().warning("Vault not found! Economy features disabled.");
        }
    }

    private boolean setupEconomy() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        
        economy = rsp.getProvider();
        return economy != null;
    }

    public boolean isEnabled() {
        return vaultEnabled && economy != null;
    }

    public Economy getEconomy() {
        return economy;
    }

    public double getBalance(OfflinePlayer player) {
        if (!isEnabled()) return 0;
        return economy.getBalance(player);
    }

    public boolean hasEnough(OfflinePlayer player, double amount) {
        if (!isEnabled()) return false;
        return economy.has(player, amount);
    }

    public boolean withdraw(OfflinePlayer player, double amount) {
        if (!isEnabled()) return false;
        EconomyResponse response = economy.withdrawPlayer(player, amount);
        return response.transactionSuccess();
    }

    public boolean deposit(OfflinePlayer player, double amount) {
        if (!isEnabled()) return false;
        EconomyResponse response = economy.depositPlayer(player, amount);
        return response.transactionSuccess();
    }

    public String format(double amount) {
        if (!isEnabled()) return String.format("%.2f", amount);
        return economy.format(amount);
    }

    public String getCurrencyName() {
        if (!isEnabled()) return "$";
        return economy.currencyNamePlural();
    }

    public boolean transferToClanBank(Player player, ru.clans.models.Clan clan, double amount) {
        if (!isEnabled()) return false;
        if (!hasEnough(player, amount)) return false;
        
        if (withdraw(player, amount)) {
            clan.depositBank(amount, player.getUniqueId(), player.getName());
            plugin.getDataManager().saveClan(clan);
            return true;
        }
        return false;
    }

    public boolean withdrawFromClanBank(Player player, ru.clans.models.Clan clan, double amount) {
        if (!isEnabled()) return false;
        if (clan.getBank() < amount) return false;
        
        if (clan.withdrawBank(amount, player.getUniqueId(), player.getName())) {
            deposit(player, amount);
            plugin.getDataManager().saveClan(clan);
            return true;
        }
        return false;
    }

    public double applyWithdrawTax(double amount) {
        double tax = plugin.getConfigManager().getWithdrawTax();
        if (tax > 0) {
            return amount * (1 - tax / 100);
        }
        return amount;
    }

    public double getWithdrawTax() {
        return plugin.getConfigManager().getWithdrawTax();
    }
}
