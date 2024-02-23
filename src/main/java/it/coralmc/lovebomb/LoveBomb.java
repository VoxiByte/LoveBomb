package it.coralmc.lovebomb;

import com.vk2gpz.tokenenchant.api.EnchantHandler;
import com.vk2gpz.tokenenchant.api.InvalidTokenEnchantException;
import com.vk2gpz.tokenenchant.api.TokenEnchantAPI;
import it.coralmc.lovebomb.hook.AutoSellHook;
import me.jet315.prisonmines.JetsPrisonMines;
import me.jet315.prisonmines.JetsPrisonMinesAPI;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public final class LoveBomb extends EnchantHandler implements Listener {
    private static final int PICKUP_DELAY_INFINITE = Integer.MAX_VALUE;
    private static final int EXPLOSION_RADIUS = 3;
    private static final int EXPLOSION_PARTICLE_COUNT = 10;
    private static final Material BOMB_MATERIAL = Material.PAPER;
    private static final String BOMB_NAME = ChatColor.RED + "LOVE " + ChatColor.BOLD + "BOMB" + ChatColor.GRAY + " (Click destro)";

    private static JavaPlugin plugin;

    private final JetsPrisonMinesAPI jetsPrisonMinesAPI;
    private final Map<Player, Integer> brokenBlocks = new HashMap<>();
    private final ItemStack bombItem;

    private int bombThreshold;
    private int bombDelay;
    private int bombModelData;
    private List<String> blackListedWorlds;

    public static JavaPlugin getInstance() {
        return plugin;
    }

    public LoveBomb(final TokenEnchantAPI tokenEnchantAPI) throws InvalidTokenEnchantException {
        super(tokenEnchantAPI);
        plugin = tokenEnchantAPI;
        this.jetsPrisonMinesAPI = ((JetsPrisonMines) Bukkit.getPluginManager().getPlugin("JetsPrisonMines")).getAPI();
        this.bombItem = createBombItem();
        loadConfig();
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (!isEnchanted(player) || jetsPrisonMinesAPI.getMinesByLocation(player.getLocation()).isEmpty()) {
            return;
        }

        World world = event.getBlock().getWorld();
        if(this.blackListedWorlds.contains(world.getName())) {
            return;
        }

        incrementBrokenBlocks(player);

        if (getBrokenBlocks(player) >= bombThreshold) {
            launchBomb(player);
            resetBrokenBlocks(player);
        }
    }

    private void launchBomb(Player player) {
        Location eyeLocation = player.getEyeLocation();
        Item thrownBomb = player.getWorld().dropItem(eyeLocation, bombItem);
        thrownBomb.setVelocity(eyeLocation.getDirection());
        thrownBomb.setPickupDelay(PICKUP_DELAY_INFINITE);

        scheduleExplosion(player, thrownBomb);
    }

    private void scheduleExplosion(Player player, Item thrownBomb) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> explode(player, thrownBomb), bombDelay * 20L);
    }

    private void explode(Player player, Item thrownBomb) {
        Location explosionLocation = thrownBomb.getLocation();
        thrownBomb.remove();
        createHeartExplosion(explosionLocation.getWorld(), explosionLocation);
        breakBlocksAround(player, explosionLocation);
        player.playSound(explosionLocation, Sound.ENTITY_GENERIC_EXPLODE, 1, 1);
    }

    private void breakBlocksAround(Player player, Location center) {
        List<ItemStack> drops = collectDrops(center);
        drops.forEach(drop -> player.getInventory().addItem(drop));
        AutoSellHook.sellShopItems(player);
    }

    private List<ItemStack> collectDrops(Location center) {
        List<ItemStack> drops = new ArrayList<>();
        World world = center.getWorld();
        int startX = center.getBlockX() - EXPLOSION_RADIUS;
        int startY = center.getBlockY() - EXPLOSION_RADIUS;
        int startZ = center.getBlockZ() - EXPLOSION_RADIUS;
        int endX = center.getBlockX() + EXPLOSION_RADIUS;
        int endY = center.getBlockY() + EXPLOSION_RADIUS;
        int endZ = center.getBlockZ() + EXPLOSION_RADIUS;

        for (int x = startX; x <= endX; x++) {
            for (int y = startY; y <= endY; y++) {
                for (int z = startZ; z <= endZ; z++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (!jetsPrisonMinesAPI.getMinesByLocation(block.getLocation()).isEmpty() && block.getType() != Material.BROWN_MUSHROOM_BLOCK) {
                        drops.addAll(block.getDrops());
                        block.setType(Material.AIR);
                    }
                }
            }
        }
        return drops;
    }

    private void createHeartExplosion(World world, Location location) {
        world.spawnParticle(Particle.HEART, location, EXPLOSION_PARTICLE_COUNT, 0.5, 0.5, 0.5);
    }

    private ItemStack createBombItem() {
        ItemStack item = new ItemStack(BOMB_MATERIAL);
        ItemMeta meta = item.getItemMeta();
        meta.setCustomModelData(bombModelData);
        meta.setDisplayName(BOMB_NAME);
        item.setItemMeta(meta);
        return item;
    }

    private int getBrokenBlocks(Player player) {
        return brokenBlocks.getOrDefault(player, 0);
    }

    private void incrementBrokenBlocks(Player player) {
        brokenBlocks.put(player, getBrokenBlocks(player) + 1);
    }

    private void resetBrokenBlocks(Player player) {
        brokenBlocks.put(player, 0);
    }

    public boolean isEnchanted(final Player player) {
        return getCELevel(player) > 0;
    }

    @Override
    public void loadConfig() {
        super.loadConfig();
        bombThreshold = getConfig().getInt("Enchants.LoveBomb.bomb_threshold");
        bombDelay = getConfig().getInt("Enchants.LoveBomb.bomb_delay");
        bombModelData = getConfig().getInt("Enchants.LoveBomb.model_data");
        blackListedWorlds = getConfig().getStringList("Enchants.LoveBomb.blacklisted_worlds");
    }

    @Override
    public String getName() {
        return "LoveBomb";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }
}
