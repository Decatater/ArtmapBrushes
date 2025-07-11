// Item listener class: ItemListener.java
package com.dekatater.artmapbrushes;

import de.tr7zw.nbtapi.NBT;
import de.tr7zw.nbtapi.NBTCompound;
import de.tr7zw.nbtapi.NBTItem;
import de.tr7zw.nbtapi.NBTList;
import de.tr7zw.nbtapi.iface.ReadWriteItemNBT;
import de.tr7zw.nbtapi.iface.ReadWriteNBT;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

public class ItemListener implements Listener {
    private final ArtmapBrushes plugin;
    private final Map<String, AtomicLong> playerRateLimits = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> playerInventoryProcessing = new ConcurrentHashMap<>();
    private volatile boolean worldSaving = false;
    private volatile long lastSaveDetection = 0;

    public ItemListener(ArtmapBrushes plugin) {
        this.plugin = plugin;
        startSaveDetectionTask();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getCurrentItem() != null && event.getWhoClicked() instanceof Player) {
            Player player = (Player) event.getWhoClicked();
            if (canProcessForPlayer(player.getUniqueId().toString(), plugin.getInventoryRateLimitMs())) {
                updateItemTexture(event.getCurrentItem());
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onAnvilPrepare(PrepareAnvilEvent event) {
        if (event.getResult() != null) {
            updateItemTexture(event.getResult());
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onItemCraft(PrepareItemCraftEvent event) {
        if (event.getInventory().getResult() != null) {
            updateItemTexture(event.getInventory().getResult());
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Process inventory asynchronously with delays to prevent lag
        new BukkitRunnable() {
            private int index = 0;
            private final ItemStack[] contents = player.getInventory().getContents();
            
            @Override
            public void run() {
                if (plugin.isPaused() || worldSaving) {
                    cancel();
                    return;
                }
                
                int processed = 0;
                while (index < contents.length && processed < plugin.getItemsPerTick()) {
                    ItemStack item = contents[index];
                    if (item != null) {
                        updateItemTexture(item);
                        processed++;
                    }
                    index++;
                }
                
                if (index >= contents.length) {
                    cancel();
                    
                    // Check for updates and notify if admin
                    if (plugin.isUpdateCheckerEnabled() && plugin.shouldNotifyOnJoin() && 
                        (player.hasPermission("artmapbrushes.admin") || player.isOp())) {
                        plugin.getUpdateChecker().checkForUpdates().thenAccept(hasUpdate -> {
                            if (hasUpdate) {
                                new BukkitRunnable() {
                                    @Override
                                    public void run() {
                                        plugin.getUpdateChecker().notifyUpdate(player);
                                    }
                                }.runTask(plugin);
                            }
                        });
                    }
                }
            }
        }.runTaskTimer(plugin, plugin.getJoinProcessingDelayTicks(), plugin.getJoinProcessingIntervalTicks());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItem(event.getNewSlot());

        if (item != null && canProcessForPlayer(player.getUniqueId().toString(), plugin.getRateLimitMs())) {
            updateItemTexture(item);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onItemDrop(PlayerDropItemEvent event) {
        updateItemTexture(event.getItemDrop().getItemStack());
    }

    private void updateItemTexture(ItemStack item) {
        // Skip processing if plugin is paused, world is saving, or item is invalid
        if (plugin.isPaused() || worldSaving || item == null || !item.hasItemMeta()) {
            return;
        }

        Material material = item.getType();
        ItemMeta meta = item.getItemMeta();
        boolean shouldHaveModelData = false;
        String correctModelData = null;

        // Check if this item's material is used in any brush mappings
        boolean materialUsedInBrushes = false;
        for (ArtmapBrushes.BrushMapping mapping : plugin.getBrushMappings().values()) {
            if (mapping.getMaterial() == material) {
                materialUsedInBrushes = true;
                break;
            }
        }

        // If material isn't used in brushes, no need to process
        if (!materialUsedInBrushes) {
            return;
        }

        // Check if the item has a display name
        if (!meta.hasDisplayName()) {
            // Remove model data if it exists since it has no name
            removeCustomModelData(item);
            return;
        }

        String displayName = meta.getDisplayName();

        // Check against all brush mappings to see if this should have model data
        for (Map.Entry<String, ArtmapBrushes.BrushMapping> entry : plugin.getBrushMappings().entrySet()) {
            ArtmapBrushes.BrushMapping mapping = entry.getValue();

            // Skip if material doesn't match
            if (material != mapping.getMaterial()) {
                continue;
            }

            String itemName = displayName;
            String mappingName = mapping.getName();

            // Remove color codes if configured to ignore them
            if (mapping.shouldIgnoreColor()) {
                itemName = ChatColor.stripColor(itemName);
                mappingName = ChatColor.stripColor(mappingName);
            }
            
            // Remove special characters if configured to ignore them
            if (mapping.shouldIgnoreSpecialChars()) {
                itemName = itemName.replaceAll("[^a-zA-Z0-9\\s]", "");
                mappingName = mappingName.replaceAll("[^a-zA-Z0-9\\s]", "");
            }

            // Check if name matches (respecting case sensitivity setting)
            boolean nameMatches;
            if (mapping.isCaseSensitive()) {
                nameMatches = itemName.equals(mappingName);
            } else {
                nameMatches = itemName.equalsIgnoreCase(mappingName);
            }

            if (nameMatches) {
                shouldHaveModelData = true;
                correctModelData = mapping.getCustomModelData();
                break;
            }
        }

        // Handle the model data based on whether it should have it or not
        if (shouldHaveModelData) {
            applyCustomModelData(item, correctModelData);
        } else {
            removeCustomModelData(item);
        }
    }

    private void applyCustomModelData(ItemStack item, String modelDataValue) {
        try {
            // Check if the item already has this value
            final boolean[] alreadyHasCorrectData = {false};

            de.tr7zw.nbtapi.NBT.getComponents(item, componentsNbt -> {
                if (componentsNbt.hasTag("minecraft:custom_model_data")) {
                    de.tr7zw.nbtapi.NBTCompound cmdNbt = (NBTCompound) componentsNbt.getCompound("minecraft:custom_model_data");

                    if (cmdNbt.hasTag("strings")) {
                        de.tr7zw.nbtapi.NBTList<String> strings = cmdNbt.getStringList("strings");
                        for (int i = 0; i < strings.size(); i++) {
                            if (modelDataValue.equals(strings.get(i))) {
                                alreadyHasCorrectData[0] = true;
                                break;
                            }
                        }
                    }
                }
            });

            // If it already has the correct value, nothing to do
            if (alreadyHasCorrectData[0]) {
                return;
            }

            // If it has a different value, we need to remove it first
            removeCustomModelData(item);

            // Add the correct model data
            de.tr7zw.nbtapi.NBT.modifyComponents(item, componentsNbt -> {
                de.tr7zw.nbtapi.NBTCompound cmdNbt =
                        (de.tr7zw.nbtapi.NBTCompound) componentsNbt.getOrCreateCompound("minecraft:custom_model_data");

                de.tr7zw.nbtapi.NBTList<String> stringsList = cmdNbt.getStringList("strings");
                stringsList.add(modelDataValue);
            });


        } catch (Exception e) {
            plugin.getLogger().severe("Error applying custom model data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void removeCustomModelData(ItemStack item) {
        try {
            // Check if it has any model data to remove
            final boolean[] hasModelData = {false};

            de.tr7zw.nbtapi.NBT.getComponents(item, componentsNbt -> {
                if (componentsNbt.hasTag("minecraft:custom_model_data")) {
                    hasModelData[0] = true;
                }
            });

            // If it has model data, remove it
            if (hasModelData[0]) {
                de.tr7zw.nbtapi.NBT.modifyComponents(item, componentsNbt -> {
                    componentsNbt.removeKey("minecraft:custom_model_data");
                });


            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error removing custom model data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean canProcessForPlayer(String playerId, long rateLimitMs) {
        AtomicLong lastProcessTime = playerRateLimits.computeIfAbsent(playerId, k -> new AtomicLong(0));
        long currentTime = System.currentTimeMillis();
        long lastTime = lastProcessTime.get();
        
        if (currentTime - lastTime >= rateLimitMs) {
            lastProcessTime.set(currentTime);
            return true;
        }
        return false;
    }

    private void startSaveDetectionTask() {
        new BukkitRunnable() {
            private int lastPlayerCount = 0;
            private double lastTPS = 20.0;
            
            @Override
            public void run() {
                try {
                    // Detect potential world saves by monitoring TPS drops and player count
                    int currentPlayerCount = Bukkit.getOnlinePlayers().size();
                    double currentTPS = getCurrentTPS();
                    
                    // Detect save conditions: significant TPS drop or server lag
                    boolean potentialSave = false;
                    if (currentTPS < 15.0 && lastTPS >= 18.0) {
                        potentialSave = true;
                    }
                    
                    // Also check for server thread blocking (common during saves)
                    long startTime = System.nanoTime();
                    Thread.yield();
                    long endTime = System.nanoTime();
                    if ((endTime - startTime) > 50_000_000) { // 50ms+ delay indicates blocking
                        potentialSave = true;
                    }
                    
                    if (potentialSave) {
                        worldSaving = true;
                        lastSaveDetection = System.currentTimeMillis();
                        plugin.getLogger().info("World save detected - pausing NBT operations");
                    } else if (worldSaving && (System.currentTimeMillis() - lastSaveDetection) > plugin.getSaveDetectionCooldownMs()) {
                        worldSaving = false;
                        plugin.getLogger().info("World save completed - resuming NBT operations");
                    }
                    
                    lastPlayerCount = currentPlayerCount;
                    lastTPS = currentTPS;
                    
                } catch (Exception e) {
                    plugin.getLogger().warning("Error in save detection: " + e.getMessage());
                }
            }
        }.runTaskTimer(plugin, 20L, 20L); // Run every second
    }
    
    private double getCurrentTPS() {
        try {
            Object server = Bukkit.getServer().getClass().getMethod("getServer").invoke(Bukkit.getServer());
            Object[] recentTps = (Object[]) server.getClass().getField("recentTps").get(server);
            return (Double) recentTps[0];
        } catch (Exception e) {
            return 20.0; // Default to 20 TPS if detection fails
        }
    }
}