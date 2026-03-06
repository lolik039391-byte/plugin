package ru.skup.plugin;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class SkupCommand implements CommandExecutor, TabCompleter {

    private final SkupPlugin plugin;
    private final PriceService priceService;
    private final PlayerStatsService statsService;

    public SkupCommand(SkupPlugin plugin, PriceService priceService, PlayerStatsService statsService) {
        this.plugin = plugin;
        this.priceService = priceService;
        this.statsService = statsService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player) && (args.length == 0 || !args[0].equalsIgnoreCase("reload"))) {
            sender.sendMessage(color("&cКоманда доступна только игроку."));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "sellhand":
                sellHand((Player) sender);
                return true;
            case "sellall":
                sellAll((Player) sender);
                return true;
            case "prices":
                sendPrices((Player) sender);
                return true;
            case "stats":
                sendStats((Player) sender);
                return true;
            case "top":
                sendTop((Player) sender);
                return true;
            case "reload":
                if (!sender.hasPermission("skup.admin")) {
                    sender.sendMessage(color("&cНедостаточно прав."));
                    return true;
                }
                plugin.reloadPlugin();
                sender.sendMessage(color("&aSkupPlugin перезагружен."));
                return true;
            default:
                sendHelp(sender);
                return true;
        }
    }

    private void sellHand(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR || item.getAmount() <= 0) {
            player.sendMessage(color("&eВ руке нет предмета для продажи."));
            return;
        }

        if (!priceService.isSellable(item.getType())) {
            player.sendMessage(color("&cЭтот предмет скупщик не принимает."));
            return;
        }

        int amount = item.getAmount();
        int allowed = getAllowedAmount(player, amount);
        if (allowed <= 0) {
            player.sendMessage(color("&cДостигнут дневной лимит продаж."));
            return;
        }

        double earned = calculateEarnings(player, item.getType(), allowed);
        player.getInventory().setItemInMainHand(allowed == amount ? null : new ItemStack(item.getType(), amount - allowed));
        statsService.addSale(player.getUniqueId(), allowed, earned);

        player.sendMessage(color("&aПродано: &f" + allowed + "x " + pretty(item.getType()) + " &aза &6" + formatMoney(earned) + "$"));
    }

    private void sellAll(Player player) {
        int sold = 0;
        double earned = 0.0;

        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType() == Material.AIR || item.getAmount() <= 0) {
                continue;
            }

            if (!priceService.isSellable(item.getType())) {
                continue;
            }

            int allowed = getAllowedAmount(player, item.getAmount());
            if (allowed <= 0) {
                break;
            }

            sold += allowed;
            earned += calculateEarnings(player, item.getType(), allowed);

            if (allowed == item.getAmount()) {
                item.setAmount(0);
            } else {
                item.setAmount(item.getAmount() - allowed);
            }
        }

        if (sold == 0) {
            player.sendMessage(color("&eНечего продавать или лимит исчерпан."));
            return;
        }

        statsService.addSale(player.getUniqueId(), sold, earned);
        player.sendMessage(color("&aПродано предметов: &f" + sold + " &aна сумму &6" + formatMoney(earned) + "$"));
    }

    private void sendPrices(Player player) {
        player.sendMessage(color("&6==== Актуальные цены скупщика ===="));
        priceService.getPrices().entrySet().stream()
                .sorted(Comparator.comparing(e -> e.getKey().name()))
                .forEach(entry -> player.sendMessage(color("&f" + pretty(entry.getKey()) + " &7→ &a" + formatMoney(entry.getValue()) + "$")));

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
        player.sendMessage(color("&6==== Топ-5 по заработку на скупщике ===="));
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

    private int getAllowedAmount(Player player, int requested) {
        int limit = priceService.getDailyItemLimit();
        if (limit <= 0) {
            return requested;
        }

        int soldToday = statsService.getTodaySoldItems(player.getUniqueId());
        int left = limit - soldToday;
        return Math.max(0, Math.min(left, requested));
    }

    private double calculateEarnings(Player player, Material material, int amount) {
        double basePrice = priceService.getBasePrice(material);
        double multiplier = priceService.resolvePlayerMultiplier(player);
        return basePrice * amount * multiplier;
    }

    private String pretty(Material material) {
        return Arrays.stream(material.name().split("_"))
                .map(s -> s.substring(0, 1) + s.substring(1).toLowerCase(Locale.ROOT))
                .collect(Collectors.joining(" "));
    }

    private String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    private String formatMoney(double value) {
        return String.format(Locale.US, "%.2f", value);
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(color("&6==== Команды скупщика ===="));
        sender.sendMessage(color("&e/skup sellhand &7- продать предмет из руки"));
        sender.sendMessage(color("&e/skup sellall &7- продать все подходящие предметы"));
        sender.sendMessage(color("&e/skup prices &7- посмотреть цены"));
        sender.sendMessage(color("&e/skup stats &7- личная статистика"));
        sender.sendMessage(color("&e/skup top &7- топ игроков"));
        if (sender.hasPermission("skup.admin")) {
            sender.sendMessage(color("&c/skup reload &7- перезагрузить конфиг"));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>(Arrays.asList("sellhand", "sellall", "prices", "stats", "top"));
            if (sender.hasPermission("skup.admin")) {
                options.add("reload");
            }
            String part = args[0].toLowerCase(Locale.ROOT);
            return options.stream().filter(opt -> opt.startsWith(part)).collect(Collectors.toList());
        }
        return List.of();
    }
}
