// Main plugin class: ArtmapBrushes.java
package com.dekatater.artmapbrushes;

import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ArtmapBrushes extends JavaPlugin {
    private Map<String, BrushMapping> brushMappings = new HashMap<>();
    private File configFile;
    private FileConfiguration config;
    private boolean paused = false;

    @Override
    public void onEnable() {
        getLogger().info("ArtmapBrushes is starting up...");

        // Create config if it doesn't exist
        saveDefaultConfig();
        configFile = new File(getDataFolder(), "config.yml");

        // Load the config
        loadConfig();

        // Register the item listener
        getServer().getPluginManager().registerEvents(new ItemListener(this), this);

        // Register commands
        getCommand("artmapbrushesreload").setExecutor(new ReloadCommand(this));
        getCommand("artmapbrushespause").setExecutor(new PauseCommand(this));
        getCommand("artmapbrushesresume").setExecutor(new ResumeCommand(this));

        getLogger().info("ArtmapBrushes has been enabled! Loaded " + brushMappings.size() + " brush mappings");
    }

    @Override
    public void onDisable() {
        getLogger().info("ArtmapBrushes has been disabled!");
    }

    public void loadConfig() {
        // Create default config if it doesn't exist
        if (!configFile.exists()) {
            saveResource("config.yml", false);
        }

        // Load config
        config = YamlConfiguration.loadConfiguration(configFile);

        // Clear existing mappings
        brushMappings.clear();

        // Load mappings from config
        if (config.contains("brushes")) {
            Set<String> brushes = config.getConfigurationSection("brushes").getKeys(false);

            for (String brushSection : brushes) {
                String itemType = config.getString("brushes." + brushSection + ".type");
                String itemName = config.getString("brushes." + brushSection + ".name");
                boolean caseSensitive = config.getBoolean("brushes." + brushSection + ".case_sensitive", false);
                boolean ignoreColor = config.getBoolean("brushes." + brushSection + ".ignore_color", true);
                boolean ignoreSpecialChars = config.getBoolean("brushes." + brushSection + ".ignore_special_chars", false);
                String customModelData = config.getString("brushes." + brushSection + ".custom_model_data");

                Material material = Material.getMaterial(itemType);
                if (material == null) {
                    getLogger().warning("Invalid material type: " + itemType + " for brush: " + brushSection);
                    continue;
                }

                BrushMapping mapping = new BrushMapping(
                        material,
                        itemName,
                        caseSensitive,
                        ignoreColor,
                        ignoreSpecialChars,
                        customModelData
                );

                brushMappings.put(brushSection, mapping);
            }
        } else {
            getLogger().warning("No 'brushes' section found in config file!");
        }
    }

    public Map<String, BrushMapping> getBrushMappings() {
        return brushMappings;
    }
    
    public boolean isPaused() {
        return paused;
    }
    
    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    // Inner class to store brush mapping information
    public static class BrushMapping {
        private final Material material;
        private final String name;
        private final boolean caseSensitive;
        private final boolean ignoreColor;
        private final boolean ignoreSpecialChars;
        private final String customModelData;

        public BrushMapping(Material material, String name, boolean caseSensitive, boolean ignoreColor, boolean ignoreSpecialChars, String customModelData) {
            this.material = material;
            this.name = name;
            this.caseSensitive = caseSensitive;
            this.ignoreColor = ignoreColor;
            this.ignoreSpecialChars = ignoreSpecialChars;
            this.customModelData = customModelData;
        }

        public Material getMaterial() {
            return material;
        }

        public String getName() {
            return name;
        }

        public boolean isCaseSensitive() {
            return caseSensitive;
        }

        public boolean shouldIgnoreColor() {
            return ignoreColor;
        }

        public boolean shouldIgnoreSpecialChars() {
            return ignoreSpecialChars;
        }

        public String getCustomModelData() {
            return customModelData;
        }
    }
}