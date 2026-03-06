package ru.skup.plugin;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class BuyerCommand implements CommandExecutor, TabCompleter {

    public static final String MENU_TITLE = "§8§lСкупщик §7• §fПродажа";

    private final SkupPlugin plugin;
    private final PriceService priceService;
    private final PlayerStatsService statsService;

    public BuyerCommand(SkupPlugin plugin, PriceService priceService, PlayerStatsService statsService) {
        this.plugin = plugin;
        this.priceService = priceService;
        this.statsService = statsService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player) && (args.length == 0 || !args[0].equalsIgnoreCase("reload"))) {
            sender.sendMessage(color("&cЭта команда доступна только игроку."));
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("open")) {
            openMenu((Player) sender);
            return true;
        }

        Player player = sender instanceof Player ? (Player) sender : null;

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "prices":
                sendPrices(player);
                return true;
            case "stats":
                sendStats(player);
                return true;
            case "top":
                sendTop(player);
                return true;
            case "reload":
                if (!sender.hasPermission("buyer.admin")) {
                    sender.sendMessage(color("&cНедостаточно прав."));
                    return true;
                }
                plugin.reloadPlugin();
                sender.sendMessage(color("&aBuyer plugin перезагружен."));
                return true;
            default:
                sendHelp(sender);
                return true;
        }
    }

    public void openMenu(Player player) {
        player.openInventory(BuyerMenuFactory.build(player, priceService, statsService));
    }

    private void sendPrices(Player player) {
        player.sendMessage(color("&6==== Актуальные цены скупщика ===="));
        priceService.getPrices().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> player.sendMessage(color("&f" + BuyerMenuFactory.pretty(entry.getKey())
                        + " &7→ &a" + formatMoney(entry.getValue()) + "$")));

        if (priceService.isHappyHour()) {
            player.sendMessage(color("&dСчастливый час активен! Цены повышены."));
        }
    }

    private void sendStats(Player player) {
        int today = statsService.getTodaySoldItems(player.getUniqueId());
        int allItems = statsService.getLifetimeItems(player.getUniqueId());
        double allMoney = statsService.getLifetimeEarnings(player.getUniqueId());

        player.sendMessage(color("&6==== Твоя статистика скупки ===="));
        player.sendMessage(color("&fСегодня продано: &a" + today + " шт."));
        player.sendMessage(color("&fВсего продано: &a" + allItems + " шт."));
        player.sendMessage(color("&fВсего заработано: &6" + formatMoney(allMoney) + "$"));
        player.sendMessage(color("&fТекущий множитель: &bx" + formatMoney(priceService.resolvePlayerMultiplier(player))));
    }

    private void sendTop(Player player) {
        Map<String, Double> top = statsService.getTopEarners(5);
        player.sendMessage(color("&6==== Топ-5 по заработку ===="));
        if (top.isEmpty()) {
            player.sendMessage(color("&7Пока данных нет."));
            return;
        }

        int i = 1;
        for (Map.Entry<String, Double> entry : top.entrySet()) {
            player.sendMessage(color("&e" + i + ". &f" + entry.getKey() + " &7- &6" + formatMoney(entry.getValue()) + "$"));
            i++;
        }
    }

    private String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    private String formatMoney(double value) {
        return String.format(Locale.US, "%.2f", value);
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(color("&6==== Команды buyer ===="));
        sender.sendMessage(color("&e/buyer &7- открыть меню скупщика"));
        sender.sendMessage(color("&e/buyer prices &7- посмотреть цены"));
        sender.sendMessage(color("&e/buyer stats &7- личная статистика"));
        sender.sendMessage(color("&e/buyer top &7- топ игроков"));
        if (sender.hasPermission("buyer.admin")) {
            sender.sendMessage(color("&c/buyer reload &7- перезагрузить конфиг"));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>(Arrays.asList("open", "prices", "stats", "top"));
            if (sender.hasPermission("buyer.admin")) {
                options.add("reload");
            }
            String part = args[0].toLowerCase(Locale.ROOT);
            return options.stream().filter(opt -> opt.startsWith(part)).collect(Collectors.toList());
        }
        return List.of();
    }
}
