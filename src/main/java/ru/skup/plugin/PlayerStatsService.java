package ru.skup.plugin;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerStatsService {

    private final SkupPlugin plugin;
    private final File file;
    private FileConfiguration config;

    public PlayerStatsService(SkupPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "player-stats.yml");
        reload();
    }

    public void reload() {
        if (!file.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Не удалось создать player-stats.yml: " + e.getMessage());
            }
        }
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public void flush() {
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Не удалось сохранить player-stats.yml: " + e.getMessage());
        }
    }

    private String key(UUID uuid, String path) {
        return "players." + uuid + "." + path;
    }

    public int getTodaySoldItems(UUID uuid) {
        String day = LocalDate.now().toString();
        String storedDay = config.getString(key(uuid, "day"), day);
        if (!day.equals(storedDay)) {
            resetDaily(uuid);
            return 0;
        }
        return config.getInt(key(uuid, "daily-items"), 0);
    }

    public void addSale(UUID uuid, int items, double earned) {
        String day = LocalDate.now().toString();
        if (!day.equals(config.getString(key(uuid, "day"), day))) {
            resetDaily(uuid);
        }

        config.set(key(uuid, "day"), day);
        config.set(key(uuid, "daily-items"), getTodaySoldItems(uuid) + items);
        config.set(key(uuid, "lifetime-items"), config.getInt(key(uuid, "lifetime-items"), 0) + items);
        config.set(key(uuid, "lifetime-earnings"), config.getDouble(key(uuid, "lifetime-earnings"), 0) + earned);
    }

    public int getLifetimeItems(UUID uuid) {
        return config.getInt(key(uuid, "lifetime-items"), 0);
    }

    public double getLifetimeEarnings(UUID uuid) {
        return config.getDouble(key(uuid, "lifetime-earnings"), 0.0);
    }

    public Map<String, Double> getTopEarners(int limit) {
        Map<String, Double> result = new HashMap<>();
        if (config.getConfigurationSection("players") == null) {
            return result;
        }

        config.getConfigurationSection("players").getKeys(false).stream()
                .limit(500)
                .forEach(uuidRaw -> {
                    double value = config.getDouble("players." + uuidRaw + ".lifetime-earnings", 0.0);
                    try {
                        String name = Bukkit.getOfflinePlayer(UUID.fromString(uuidRaw)).getName();
                        result.put(name == null ? uuidRaw : name, value);
                    } catch (IllegalArgumentException ignored) {
                        result.put(uuidRaw, value);
                    }
                });

        return result.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(limit)
                .collect(HashMap::new, (map, entry) -> map.put(entry.getKey(), entry.getValue()), HashMap::putAll);
    }

    private void resetDaily(UUID uuid) {
        config.set(key(uuid, "day"), LocalDate.now().toString());
        config.set(key(uuid, "daily-items"), 0);
    }
}
