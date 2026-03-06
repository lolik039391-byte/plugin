package ru.skup.plugin;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class SkupPlugin extends JavaPlugin {

    private PriceService priceService;
    private PlayerStatsService statsService;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.priceService = new PriceService(this);
        this.statsService = new PlayerStatsService(this);

        PluginCommand command = getCommand("skup");
        if (command != null) {
            SkupCommand skupCommand = new SkupCommand(this, priceService, statsService);
            command.setExecutor(skupCommand);
            command.setTabCompleter(skupCommand);
        } else {
            getLogger().severe("Команда /skup не найдена в plugin.yml");
        }

        getLogger().info("SkupPlugin включен. Готов к скупке предметов!");
    }

    @Override
    public void onDisable() {
        if (statsService != null) {
            statsService.flush();
        }
    }

    public void reloadPlugin() {
        reloadConfig();
        this.priceService.reload();
        this.statsService.reload();
    }
}
