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
    
    // Performance settings
    private long rateLimitMs = 50;
    private long inventoryRateLimitMs = 100;
    private long saveDetectionCooldownMs = 5000;
    private int itemsPerTick = 5;
    private long joinProcessingDelayTicks = 20;
    private long joinProcessingIntervalTicks = 2;
    
    // Update checker
    private UpdateChecker updateChecker;
    private boolean updateCheckerEnabled = true;
    private String githubRepo = "dekatater/ArtmapBrushes";
    private boolean notifyOnJoin = true;
    private boolean checkOnStartup = true;

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
        
        // Initialize update checker if enabled
        if (updateCheckerEnabled) {
            updateChecker = new UpdateChecker(this, githubRepo);
            if (checkOnStartup) {
                getLogger().info("Checking for updates...");
                updateChecker.checkAndNotifyAsync();
            }
        }

        // Register commands
        getCommand("artmapbrushesreload").setExecutor(new ReloadCommand(this));
        getCommand("artmapbrushespause").setExecutor(new PauseCommand(this));
        getCommand("artmapbrushesresume").setExecutor(new ResumeCommand(this));
        getCommand("artmapbrushesupdate").setExecutor(new UpdateCommand(this));

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
        
        // Load performance settings
        loadPerformanceSettings();
        
        // Load update checker settings
        loadUpdateCheckerSettings();

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
    
    private void loadPerformanceSettings() {
        rateLimitMs = config.getLong("performance.rate_limit_ms", 50);
        inventoryRateLimitMs = config.getLong("performance.inventory_rate_limit_ms", 100);
        saveDetectionCooldownMs = config.getLong("performance.save_detection_cooldown_ms", 5000);
        itemsPerTick = config.getInt("performance.items_per_tick", 5);
        joinProcessingDelayTicks = config.getLong("performance.join_processing_delay_ticks", 20);
        joinProcessingIntervalTicks = config.getLong("performance.join_processing_interval_ticks", 2);
        
        getLogger().info("Performance settings loaded: rate_limit=" + rateLimitMs + "ms, inventory_rate_limit=" + inventoryRateLimitMs + "ms");
    }
    
    private void loadUpdateCheckerSettings() {
        updateCheckerEnabled = config.getBoolean("update_checker.enabled", true);
        githubRepo = config.getString("update_checker.github_repo", "dekatater/ArtmapBrushes");
        notifyOnJoin = config.getBoolean("update_checker.notify_on_join", true);
        checkOnStartup = config.getBoolean("update_checker.check_on_startup", true);
        
        getLogger().info("Update checker settings loaded: enabled=" + updateCheckerEnabled + ", repo=" + githubRepo);
    }
    
    // Getters for performance settings
    public long getRateLimitMs() { return rateLimitMs; }
    public long getInventoryRateLimitMs() { return inventoryRateLimitMs; }
    public long getSaveDetectionCooldownMs() { return saveDetectionCooldownMs; }
    public int getItemsPerTick() { return itemsPerTick; }
    public long getJoinProcessingDelayTicks() { return joinProcessingDelayTicks; }
    public long getJoinProcessingIntervalTicks() { return joinProcessingIntervalTicks; }
    
    // Update checker getters
    public UpdateChecker getUpdateChecker() { return updateChecker; }
    public boolean isUpdateCheckerEnabled() { return updateCheckerEnabled; }
    public boolean shouldNotifyOnJoin() { return notifyOnJoin; }

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