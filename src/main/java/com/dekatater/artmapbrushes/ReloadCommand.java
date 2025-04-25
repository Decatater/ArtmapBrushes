// Reload command class: ReloadCommand.java
package com.dekatater.artmapbrushes;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ReloadCommand implements CommandExecutor {
    private final ArtmapBrushes plugin;

    public ReloadCommand(ArtmapBrushes plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        plugin.getLogger().info("Reload command executed by: " + sender.getName());
        plugin.loadConfig();
        sender.sendMessage(ChatColor.GREEN + "ArtmapBrushes configuration reloaded successfully!");
        return true;
    }
}