package it.coralmc.lovebomb.hook;

import it.coralmc.lovebomb.LoveBomb;
import me.clip.autosell.SellHandler;
import me.clip.autosell.objects.SellResponse;
import me.clip.autosell.objects.Shop;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class AutoSellHook {
    private static final JavaPlugin loveBomb = LoveBomb.getInstance();

    public static void sellShopItems(Player p) {
        Shop shop = SellHandler.getShop(p);
        if (shop == null) {
            return;
        }
        Bukkit.getScheduler().runTask(loveBomb, () -> {
            SellResponse sellResponse = SellHandler.sellInventory(p, shop);
            sellResponse.getSoldItems().forEach(p.getInventory()::removeItem);
        });
    }
}
