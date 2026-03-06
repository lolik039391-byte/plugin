package ru.skup.plugin;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class SkupPlugin extends JavaPlugin {

    private PriceService priceService;
    private PlayerStatsService statsService;
    private BuyerCommand buyerCommand;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.priceService = new PriceService(this);
        this.statsService = new PlayerStatsService(this);
        this.buyerCommand = new BuyerCommand(this, priceService, statsService);

        PluginCommand command = getCommand("buyer");
        if (command != null) {
            command.setExecutor(buyerCommand);
            command.setTabCompleter(buyerCommand);
        } else {
            getLogger().severe("Команда /buyer не найдена в plugin.yml");
        }

        getServer().getPluginManager().registerEvents(new BuyerMenuListener(this, priceService, statsService), this);
        getLogger().info("BuyerPlugin включен. /buyer готов к работе!");
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

    public BuyerCommand getBuyerCommand() {
        return buyerCommand;
    }
}
