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

    @Override
    public void onEnable() {
        getLogger().info("ArtmapBrushes is starting up...");

        // Create config if it doesn't exist
        saveDefaultConfig();
        configFile = new File(getDataFolder(), "config.yml");

        // Debug info
        getLogger().info("Config file path: " + configFile.getAbsolutePath());
        getLogger().info("Config file exists: " + configFile.exists());

        // Load the config
        loadConfig();

        // Register the item listener
        getServer().getPluginManager().registerEvents(new ItemListener(this), this);
        getLogger().info("Registered ItemListener");

        // Register reload command
        getCommand("artmapbrushesreload").setExecutor(new ReloadCommand(this));
        getLogger().info("Registered reload command");

        getLogger().info("ArtmapBrushes has been enabled! Loaded " + brushMappings.size() + " brush mappings");

        // Print all loaded brushes for debugging
        for (Map.Entry<String, BrushMapping> entry : brushMappings.entrySet()) {
            BrushMapping mapping = entry.getValue();
            getLogger().info("Brush: " + entry.getKey() +
                    " | Material: " + mapping.getMaterial() +
                    " | Name: '" + mapping.getName() + "'" +
                    " | CustomModelData: " + mapping.getCustomModelData());
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("ArtmapBrushes has been disabled!");
    }

    public void loadConfig() {
        getLogger().info("Loading configuration...");

        // Create default config if it doesn't exist
        if (!configFile.exists()) {
            getLogger().info("Config file not found, creating default config...");
            saveResource("config.yml", false);
        }

        // Load config
        config = YamlConfiguration.loadConfiguration(configFile);

        // Debug configuration content
        getLogger().info("Raw config: " + config.saveToString());

        // Clear existing mappings
        brushMappings.clear();

        // Load mappings from config
        if (config.contains("brushes")) {
            getLogger().info("Found 'brushes' section in config");
            Set<String> brushes = config.getConfigurationSection("brushes").getKeys(false);

            getLogger().info("Found " + brushes.size() + " brush entries in config");

            for (String brushSection : brushes) {
                getLogger().info("Processing brush: " + brushSection);

                String itemType = config.getString("brushes." + brushSection + ".type");
                String itemName = config.getString("brushes." + brushSection + ".name");
                boolean caseSensitive = config.getBoolean("brushes." + brushSection + ".case_sensitive", false);
                boolean ignoreColor = config.getBoolean("brushes." + brushSection + ".ignore_color", true);
                String customModelData = config.getString("brushes." + brushSection + ".custom_model_data");

                getLogger().info("Brush values - Type: " + itemType + ", Name: '" + itemName +
                        "', Case Sensitive: " + caseSensitive + ", Ignore Color: " +
                        ignoreColor + ", CustomModelData: " + customModelData);

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
                        customModelData
                );

                brushMappings.put(brushSection, mapping);
                getLogger().info("Successfully added brush '" + brushSection + "' with material " +
                        material + " and custom model data " + customModelData);
            }
        } else {
            getLogger().warning("No 'brushes' section found in config file!");
        }

        getLogger().info("Loaded " + brushMappings.size() + " brush mappings");
    }

    public Map<String, BrushMapping> getBrushMappings() {
        return brushMappings;
    }

    // Inner class to store brush mapping information
    public static class BrushMapping {
        private final Material material;
        private final String name;
        private final boolean caseSensitive;
        private final boolean ignoreColor;
        private final String customModelData;

        public BrushMapping(Material material, String name, boolean caseSensitive, boolean ignoreColor, String customModelData) {
            this.material = material;
            this.name = name;
            this.caseSensitive = caseSensitive;
            this.ignoreColor = ignoreColor;
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

        public String getCustomModelData() {
            return customModelData;
        }
    }
}