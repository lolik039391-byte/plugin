package ru.skup.plugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public final class BuyerMenuFactory {

    private BuyerMenuFactory() {
    }

    public static Inventory build(Player player, PriceService priceService, PlayerStatsService statsService) {
        Inventory inv = Bukkit.createInventory(null, 54, BuyerCommand.MENU_TITLE);

        ItemStack filler = makeItem(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, filler);
        }

        List<Map.Entry<Material, Double>> priceEntries = priceService.getPrices().entrySet().stream()
                .sorted(Comparator.comparing(e -> e.getKey().name()))
                .limit(45)
                .collect(Collectors.toList());

        int slot = 0;
        double multiplier = priceService.resolvePlayerMultiplier(player);

        for (Map.Entry<Material, Double> entry : priceEntries) {
            Material material = entry.getKey();
            double basePrice = entry.getValue();
            double finalPrice = basePrice * multiplier;
            int amount = countMaterial(player, material);

            List<String> lore = new ArrayList<>();
            lore.add(color("&7Базовая цена: &a" + formatMoney(basePrice) + "$"));
            lore.add(color("&7Твоя цена: &6" + formatMoney(finalPrice) + "$"));
            lore.add(color("&7В инвентаре: &f" + amount + " шт."));
            lore.add(" ");
            lore.add(color("&aЛКМ &7- продать 1 шт."));
            lore.add(color("&aПКМ &7- продать 16 шт."));
            lore.add(color("&aSHIFT+ЛКМ &7- продать 64 шт."));
            lore.add(color("&6Q &7- продать всё этого типа"));

            inv.setItem(slot++, makeItem(material, "&f" + pretty(material), lore));
        }

        int soldToday = statsService.getTodaySoldItems(player.getUniqueId());
        int dailyLimit = priceService.getDailyItemLimit();
        String dailyInfo = dailyLimit <= 0 ? "∞" : soldToday + "/" + dailyLimit;

        inv.setItem(49, makeItem(Material.NETHER_STAR, "&eТвой профиль", List.of(
                color("&7Продано сегодня: &f" + dailyInfo),
                color("&7Всего продано: &a" + statsService.getLifetimeItems(player.getUniqueId())),
                color("&7Всего заработано: &6" + formatMoney(statsService.getLifetimeEarnings(player.getUniqueId())) + "$"),
                color("&7Текущий множитель: &bx" + formatMoney(multiplier)),
                priceService.isHappyHour() ? color("&dСчастливый час активен!") : color("&8Счастливый час неактивен")
        )));

        inv.setItem(45, makeItem(Material.BOOK, "&bПодсказка", List.of(
                color("&7Нажимай на предметы, чтобы быстро продавать."),
                color("&7Цены учитывают твои permissions и happy-hour.")
        )));

        inv.setItem(53, makeItem(Material.BARRIER, "&cЗакрыть", List.of(color("&7Нажми, чтобы закрыть меню."))));

        return inv;
    }

    public static String pretty(Material material) {
        return java.util.Arrays.stream(material.name().split("_"))
                .map(s -> s.substring(0, 1) + s.substring(1).toLowerCase(Locale.ROOT))
                .collect(Collectors.joining(" "));
    }

    private static int countMaterial(Player player, Material material) {
        int count = 0;
        for (ItemStack content : player.getInventory().getContents()) {
            if (content != null && content.getType() == material) {
                count += content.getAmount();
            }
        }
        return count;
    }

    private static ItemStack makeItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        meta.setDisplayName(color(name));
        meta.setLore(lore.stream().map(BuyerMenuFactory::color).collect(Collectors.toList()));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    private static String formatMoney(double value) {
        return String.format(Locale.US, "%.2f", value);
    }
}
