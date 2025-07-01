// Resume command class: ResumeCommand.java
package com.dekatater.artmapbrushes;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ResumeCommand implements CommandExecutor {
    private final ArtmapBrushes plugin;

    public ResumeCommand(ArtmapBrushes plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!plugin.isPaused()) {
            sender.sendMessage(ChatColor.YELLOW + "ArtmapBrushes is already running!");
        } else {
            plugin.setPaused(false);
            plugin.getLogger().info("NBT application resumed by: " + sender.getName());
            sender.sendMessage(ChatColor.GREEN + "ArtmapBrushes NBT application has been resumed!");
        }
        return true;
    }
}