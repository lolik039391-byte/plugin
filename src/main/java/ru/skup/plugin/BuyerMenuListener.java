package ru.skup.plugin;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;

public class BuyerMenuListener implements Listener {

    private final SkupPlugin plugin;
    private final PriceService priceService;
    private final PlayerStatsService statsService;

    public BuyerMenuListener(SkupPlugin plugin, PriceService priceService, PlayerStatsService statsService) {
        this.plugin = plugin;
        this.priceService = priceService;
        this.statsService = statsService;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        if (!BuyerCommand.MENU_TITLE.equals(event.getView().getTitle())) {
            return;
        }

        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        Inventory clickedInv = event.getClickedInventory();
        if (clickedInv == null || !clickedInv.equals(event.getView().getTopInventory())) {
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }

        if (clicked.getType() == Material.BARRIER) {
            player.closeInventory();
            return;
        }

        if (!priceService.isSellable(clicked.getType())) {
            return;
        }

        int requested = resolveRequested(event.getClick());
        int sold = sellMaterial(player, clicked.getType(), requested);

        if (sold <= 0) {
            player.sendMessage(color("&cНедостаточно предметов или достигнут дневной лимит."));
            return;
        }

        double earned = priceService.getBasePrice(clicked.getType()) * sold * priceService.resolvePlayerMultiplier(player);
        statsService.addSale(player.getUniqueId(), sold, earned);

        player.sendMessage(color("&aПродано: &f" + sold + "x " + BuyerMenuFactory.pretty(clicked.getType())
                + " &aза &6" + formatMoney(earned) + "$"));

        plugin.getBuyerCommand().openMenu(player);
    }

    private int resolveRequested(ClickType clickType) {
        if (clickType == ClickType.SHIFT_LEFT) {
            return 64;
        }
        if (clickType == ClickType.RIGHT) {
            return 16;
        }
        if (clickType == ClickType.DROP) {
            return Integer.MAX_VALUE;
        }
        return 1;
    }

    private int sellMaterial(Player player, Material material, int requested) {
        int limit = priceService.getDailyItemLimit();
        int soldToday = statsService.getTodaySoldItems(player.getUniqueId());
        int maxByLimit = limit <= 0 ? Integer.MAX_VALUE : Math.max(0, limit - soldToday);
        int toSell = Math.min(requested, maxByLimit);
        if (toSell <= 0) {
            return 0;
        }

        int remaining = toSell;
        int sold = 0;

        ItemStack[] contents = player.getInventory().getContents();
        for (ItemStack item : contents) {
            if (item == null || item.getType() != material || item.getAmount() <= 0) {
                continue;
            }

            int take = Math.min(item.getAmount(), remaining);
            item.setAmount(item.getAmount() - take);
            remaining -= take;
            sold += take;

            if (remaining <= 0) {
                break;
            }
        }

        return sold;
    }

    private String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    private String formatMoney(double value) {
        return String.format(Locale.US, "%.2f", value);
    }
}
