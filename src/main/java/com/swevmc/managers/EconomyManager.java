package com.swevmc.managers;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;

import java.util.UUID;

public class EconomyManager {
    
    private final Economy economy;
    
    public EconomyManager(Economy economy) {
        this.economy = economy;
    }
    
    public boolean hasEnoughMoney(Player player, double amount) {
        return economy.getBalance(player) >= amount;
    }
    
    public boolean withdrawMoney(Player player, double amount) {
        if (!hasEnoughMoney(player, amount)) {
            return false;
        }
        
        return economy.withdrawPlayer(player, amount).transactionSuccess();
    }
    
    public boolean depositMoney(Player player, double amount) {
        return economy.depositPlayer(player, amount).transactionSuccess();
    }

    public boolean depositMoney(UUID playerUuid, double amount) {
        return economy.depositPlayer(Bukkit.getOfflinePlayer(playerUuid), amount).transactionSuccess();
    }
    
    public double getBalance(Player player) {
        return economy.getBalance(player);
    }
    
    public String formatMoney(double amount) {
        return economy.format(amount);
    }
    
    public Economy getEconomy() {
        return economy;
    }
}
