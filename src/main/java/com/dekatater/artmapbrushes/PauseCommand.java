// Pause command class: PauseCommand.java
package com.dekatater.artmapbrushes;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class PauseCommand implements CommandExecutor {
    private final ArtmapBrushes plugin;

    public PauseCommand(ArtmapBrushes plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (plugin.isPaused()) {
            sender.sendMessage(ChatColor.YELLOW + "ArtmapBrushes is already paused!");
        } else {
            plugin.setPaused(true);
            plugin.getLogger().info("NBT application paused by: " + sender.getName());
            sender.sendMessage(ChatColor.GREEN + "ArtmapBrushes NBT application has been paused!");
        }
        return true;
    }
}