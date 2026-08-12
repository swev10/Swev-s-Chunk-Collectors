package com.swevmc.commands;

import com.swevmc.SwevsChunkCollector;
import com.swevmc.models.ChunkCollector;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ChunkCollectorCommand implements CommandExecutor, TabCompleter {

    private final SwevsChunkCollector plugin;

    public ChunkCollectorCommand(SwevsChunkCollector plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("chunkcollector.admin")) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() +
                    plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                handleReload(sender);
                break;
            case "help":
                sendHelp(sender);
                break;
            case "info":
                handleInfo(sender);
                break;
            case "give":
                handleGive(sender, args);
                break;
            default:
                sendHelp(sender);
                break;
        }

        return true;
    }

    private void handleReload(CommandSender sender) {
        try {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + "§eReloading SwevsChunkCollector...");

            plugin.getConfigManager().reloadConfig();
            plugin.getEconomyPriceManager().reloadPrices();
            plugin.getChunkCollectorManager().restartTasks();

            int updatedCount = 0;
            for (ChunkCollector collector : plugin.getChunkCollectorManager().getCollectors()) {
                if (collector.isActive()) {
                    collector.updateHologram();
                    updatedCount++;
                }
            }

            sender.sendMessage(plugin.getConfigManager().getPrefix() + "§aReload complete! Updated " + updatedCount
                    + " active collectors.");

        } catch (Exception e) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + "§cError during reload: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleInfo(CommandSender sender) {
        int totalCollectors = plugin.getChunkCollectorManager().getCollectors().size();
        sender.sendMessage(plugin.getConfigManager().getPrefix() + "§6Chunk Collector Info:");
        sender.sendMessage("§7Total Collectors: §a" + totalCollectors);
        sender.sendMessage("§7Collection Speed: §a" + plugin.getConfigManager().getCollectionSpeed() + " ticks");
        sender.sendMessage("§7Max Collectors per Player: §a" + plugin.getConfigManager().getMaxCollectorsPerPlayer());
        sender.sendMessage(
                "§7Default Charge Time: §a" + plugin.getConfigManager().getDefaultChargeMinutes() + " minutes");
        sender.sendMessage(
                "§7Recharge Cost: §a$" + plugin.getConfigManager().getRechargeCostPerMinute() + " per minute");
    }

    private void handleGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("chunkcollector.give")) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() +
                    plugin.getConfigManager().getMessage("no-permission"));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() +
                    "§cUsage: /chunkcollector give <player> [amount]");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() +
                    "§cPlayer not found!");
            return;
        }

        int amount = 1;
        if (args.length > 2) {
            try {
                amount = Integer.parseInt(args[2]);
                if (amount <= 0) {
                    sender.sendMessage(plugin.getConfigManager().getPrefix() +
                            plugin.getConfigManager().getMessage("invalid-amount"));
                    return;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(plugin.getConfigManager().getPrefix() +
                        plugin.getConfigManager().getMessage("invalid-amount"));
                return;
            }
        }

        int remaining = amount;
        int maxStackSize = plugin.getCollectorItemFactory().getMaterial().getMaxStackSize();
        while (remaining > 0) {
            int stackSize = Math.min(remaining, maxStackSize);
            ItemStack stack = plugin.getCollectorItemFactory().create(stackSize);
            target.getInventory().addItem(stack).values()
                    .forEach(leftover -> target.getWorld().dropItemNaturally(target.getLocation(), leftover));
            remaining -= stackSize;
        }

        sender.sendMessage(plugin.getConfigManager().getPrefix()
                + plugin.getConfigManager().getMessage("collector-given", "${amount}", String.valueOf(amount)));
        target.sendMessage(plugin.getConfigManager().getPrefix()
                + plugin.getConfigManager().getMessage("collector-received", "${amount}", String.valueOf(amount),
                        "${player}", sender.getName()));
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(plugin.getConfigManager().getPrefix() + "§6Chunk Collector Commands:");
        sender.sendMessage("§7/cc reload §8- §7Reload the configuration");
        sender.sendMessage("§7/cc info §8- §7Show plugin information");
        sender.sendMessage("§7/cc help §8- §7Show this help message");
        sender.sendMessage("§7/cc give <player> [amount] §8- §7Give chunk collectors to a player");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subcommands = Arrays.asList("reload", "help", "info", "give");
            return subcommands.stream()
                    .filter(sub -> sub.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            if (sender.hasPermission("chunkcollector.give")) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            if (sender.hasPermission("chunkcollector.give")) {
                return Arrays.asList("1", "2", "3", "4", "5", "10", "16", "32", "64");
            }
        }

        return completions;
    }
}
