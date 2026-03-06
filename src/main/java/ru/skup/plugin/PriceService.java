package ru.skup.plugin;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public class PriceService {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final SkupPlugin plugin;
    private final Map<Material, Double> prices = new EnumMap<>(Material.class);
    private final Map<String, Double> permissionMultipliers = new HashMap<>();

    public PriceService(SkupPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        prices.clear();
        permissionMultipliers.clear();

        ConfigurationSection priceSection = plugin.getConfig().getConfigurationSection("prices");
        if (priceSection != null) {
            for (String key : priceSection.getKeys(false)) {
                Material material = Material.matchMaterial(key);
                if (material != null) {
                    prices.put(material, priceSection.getDouble(key, 0.0));
                }
            }
        }

        ConfigurationSection multiplierSection = plugin.getConfig().getConfigurationSection("permission-multipliers");
        if (multiplierSection != null) {
            for (String permission : multiplierSection.getKeys(false)) {
                permissionMultipliers.put(permission, multiplierSection.getDouble(permission, 1.0));
            }
        }
    }

    public boolean isSellable(Material material) {
        return prices.containsKey(material);
    }

    public double getBasePrice(Material material) {
        return prices.getOrDefault(material, 0.0);
    }

    public double resolvePlayerMultiplier(org.bukkit.entity.Player player) {
        double maxMultiplier = 1.0;
        for (Map.Entry<String, Double> entry : permissionMultipliers.entrySet()) {
            if (player.hasPermission(entry.getKey())) {
                maxMultiplier = Math.max(maxMultiplier, entry.getValue());
            }
        }

        if (isHappyHour()) {
            maxMultiplier *= plugin.getConfig().getDouble("happy-hour.multiplier", 1.0);
        }
        return maxMultiplier;
    }

    public int getDailyItemLimit() {
        return plugin.getConfig().getInt("limits.daily-items", 0);
    }

    public boolean isHappyHour() {
        if (!plugin.getConfig().getBoolean("happy-hour.enabled", false)) {
            return false;
        }

        String startRaw = plugin.getConfig().getString("happy-hour.start", "18:00");
        String endRaw = plugin.getConfig().getString("happy-hour.end", "19:00");

        try {
            LocalTime now = LocalTime.now();
            LocalTime start = LocalTime.parse(startRaw, TIME_FORMAT);
            LocalTime end = LocalTime.parse(endRaw, TIME_FORMAT);

            if (end.isBefore(start)) {
                return !now.isBefore(start) || !now.isAfter(end);
            }
            return !now.isBefore(start) && !now.isAfter(end);
        } catch (Exception ignored) {
            return false;
        }
    }

    public Map<Material, Double> getPrices() {
        return Collections.unmodifiableMap(prices);
    }
}
