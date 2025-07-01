package com.dekatater.artmapbrushes;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UpdateChecker {
    private final ArtmapBrushes plugin;
    private final String currentVersion;
    private final String githubApiUrl;
    private String latestVersion = null;
    private String downloadUrl = null;
    private boolean hasUpdate = false;
    private long lastCheckTime = 0;
    private static final long CHECK_COOLDOWN = 3600000; // 1 hour cooldown

    public UpdateChecker(ArtmapBrushes plugin, String githubRepo) {
        this.plugin = plugin;
        this.currentVersion = plugin.getDescription().getVersion();
        this.githubApiUrl = "https://api.github.com/repos/" + githubRepo + "/releases/latest";
    }

    public CompletableFuture<Boolean> checkForUpdates() {
        return CompletableFuture.supplyAsync(() -> {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastCheckTime < CHECK_COOLDOWN && latestVersion != null) {
                return hasUpdate;
            }

            try {
                URL url = new URL(githubApiUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("User-Agent", "ArtmapBrushes-UpdateChecker");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                int responseCode = connection.getResponseCode();
                if (responseCode == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;

                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    return parseResponse(response.toString());
                } else {
                    plugin.getLogger().warning("Failed to check for updates. HTTP response code: " + responseCode);
                    return false;
                }
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to check for updates: " + e.getMessage());
                return false;
            } finally {
                lastCheckTime = currentTime;
            }
        });
    }

    private boolean parseResponse(String jsonResponse) {
        try {
            Pattern tagPattern = Pattern.compile("\"tag_name\"\\s*:\\s*\"([^\"]+)\"");
            Pattern urlPattern = Pattern.compile("\"browser_download_url\"\\s*:\\s*\"([^\"]+\\.jar)\"");

            Matcher tagMatcher = tagPattern.matcher(jsonResponse);
            Matcher urlMatcher = urlPattern.matcher(jsonResponse);

            if (tagMatcher.find()) {
                latestVersion = tagMatcher.group(1);
                
                if (urlMatcher.find()) {
                    downloadUrl = urlMatcher.group(1);
                }

                hasUpdate = isNewerVersion(latestVersion, currentVersion);
                
                if (hasUpdate) {
                    plugin.getLogger().info("Update available! Current: " + currentVersion + ", Latest: " + latestVersion);
                } else {
                    plugin.getLogger().info("Plugin is up to date. Current version: " + currentVersion);
                }
                
                return hasUpdate;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error parsing update response: " + e.getMessage());
        }
        return false;
    }

    private boolean isNewerVersion(String latest, String current) {
        try {
            String latestClean = cleanVersion(latest);
            String currentClean = cleanVersion(current);
            
            String[] latestParts = latestClean.split("\\.");
            String[] currentParts = currentClean.split("\\.");
            
            int maxLength = Math.max(latestParts.length, currentParts.length);
            
            for (int i = 0; i < maxLength; i++) {
                int latestPart = i < latestParts.length ? parseVersionPart(latestParts[i]) : 0;
                int currentPart = i < currentParts.length ? parseVersionPart(currentParts[i]) : 0;
                
                if (latestPart > currentPart) {
                    return true;
                } else if (latestPart < currentPart) {
                    return false;
                }
            }
            
            return false;
        } catch (Exception e) {
            plugin.getLogger().warning("Error comparing versions: " + e.getMessage());
            return false;
        }
    }

    private String cleanVersion(String version) {
        return version.replaceAll("(?i)(release|snapshot|beta|alpha|v)", "").trim();
    }

    private int parseVersionPart(String part) {
        try {
            return Integer.parseInt(part.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public void notifyUpdate(Player player) {
        if (hasUpdate && latestVersion != null) {
            player.sendMessage(ChatColor.YELLOW + "[ArtmapBrushes] " + ChatColor.GREEN + "Update available!");
            player.sendMessage(ChatColor.YELLOW + "Current: " + ChatColor.WHITE + currentVersion + 
                             ChatColor.YELLOW + " | Latest: " + ChatColor.WHITE + latestVersion);
            if (downloadUrl != null) {
                player.sendMessage(ChatColor.YELLOW + "Download: " + ChatColor.AQUA + downloadUrl);
            }
        }
    }

    public void notifyConsoleUpdate() {
        if (hasUpdate && latestVersion != null) {
            plugin.getLogger().info("====================================");
            plugin.getLogger().info("UPDATE AVAILABLE for ArtmapBrushes!");
            plugin.getLogger().info("Current version: " + currentVersion);
            plugin.getLogger().info("Latest version: " + latestVersion);
            if (downloadUrl != null) {
                plugin.getLogger().info("Download URL: " + downloadUrl);
            }
            plugin.getLogger().info("====================================");
        }
    }

    public void checkAndNotifyAsync() {
        new BukkitRunnable() {
            @Override
            public void run() {
                checkForUpdates().thenAccept(updateAvailable -> {
                    if (updateAvailable) {
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                notifyConsoleUpdate();
                                
                                for (Player player : Bukkit.getOnlinePlayers()) {
                                    if (player.hasPermission("artmapbrushes.admin") || player.isOp()) {
                                        notifyUpdate(player);
                                    }
                                }
                            }
                        }.runTask(plugin);
                    }
                });
            }
        }.runTaskAsynchronously(plugin);
    }

    public boolean hasUpdate() {
        return hasUpdate;
    }

    public String getLatestVersion() {
        return latestVersion;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }
}