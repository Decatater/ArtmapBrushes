package com.dekatater.artmapbrushes;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class UpdateCommand implements CommandExecutor {
    private final ArtmapBrushes plugin;

    public UpdateCommand(ArtmapBrushes plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("artmapbrushes.admin") && !sender.isOp()) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        if (!plugin.isUpdateCheckerEnabled()) {
            sender.sendMessage(ChatColor.YELLOW + "[ArtmapBrushes] " + ChatColor.RED + "Update checker is disabled in config.");
            return true;
        }

        sender.sendMessage(ChatColor.YELLOW + "[ArtmapBrushes] " + ChatColor.GREEN + "Checking for updates...");

        plugin.getUpdateChecker().checkForUpdates().thenAccept(hasUpdate -> {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (hasUpdate) {
                    String currentVersion = plugin.getDescription().getVersion();
                    String latestVersion = plugin.getUpdateChecker().getLatestVersion();
                    String downloadUrl = plugin.getUpdateChecker().getDownloadUrl();

                    sender.sendMessage(ChatColor.YELLOW + "[ArtmapBrushes] " + ChatColor.GREEN + "Update available!");
                    sender.sendMessage(ChatColor.YELLOW + "Current: " + ChatColor.WHITE + currentVersion + 
                                     ChatColor.YELLOW + " | Latest: " + ChatColor.WHITE + latestVersion);
                    
                    if (downloadUrl != null) {
                        sender.sendMessage(ChatColor.YELLOW + "Download: " + ChatColor.AQUA + downloadUrl);
                    }
                    
                    if (sender instanceof Player) {
                        Player player = (Player) sender;
                        player.sendMessage(ChatColor.YELLOW + "Click the link above to download the latest version.");
                    }
                } else {
                    sender.sendMessage(ChatColor.YELLOW + "[ArtmapBrushes] " + ChatColor.GREEN + "Plugin is up to date!");
                    sender.sendMessage(ChatColor.YELLOW + "Current version: " + ChatColor.WHITE + plugin.getDescription().getVersion());
                }
            });
        }).exceptionally(throwable -> {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                sender.sendMessage(ChatColor.YELLOW + "[ArtmapBrushes] " + ChatColor.RED + "Failed to check for updates.");
                sender.sendMessage(ChatColor.RED + "Error: " + throwable.getMessage());
            });
            return null;
        });

        return true;
    }
}