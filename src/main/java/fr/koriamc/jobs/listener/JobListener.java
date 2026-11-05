package fr.koriamc.jobs.listener;

import fr.koriamc.jobs.KoriaJobs;
import fr.koriamc.jobs.model.Job;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.Locale;

/**
 * XP des métiers : mobs tués, cultures récoltées, blocs minés, pêche, fabrication.
 */
@SuppressWarnings("deprecation")
public final class JobListener implements Listener {

    private static final String PLACED = "koriajobs-pose";

    private final KoriaJobs plugin;

    public JobListener(KoriaJobs plugin) {
        this.plugin = plugin;
    }

    private boolean counts(Player player) {
        if (plugin.settings().disabledWorlds.contains(player.getWorld().getName().toLowerCase(Locale.ROOT))) {
            return false;
        }
        GameMode mode = player.getGameMode();
        return plugin.settings().creative || (mode != GameMode.CREATIVE && mode != GameMode.SPECTATOR);
    }

    private void reward(Player player, Job.Source source, int amount, String... keys) {
        if (!counts(player)) {
            return;
        }
        for (Job job : plugin.jobs().bySource(source)) {
            int xp = job.xpFor(keys) * Math.max(1, amount);
            if (xp > 0) {
                plugin.manager().addXp(player, job, xp, true);
            }
        }
    }

    // ================================================================ Mobs

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(EntityDeathEvent event) {
        LivingEntity victim = event.getEntity();
        Player killer = victim.getKiller();
        if (killer == null || victim instanceof Player) {
            return;
        }
        reward(killer, Job.Source.TUER, 1, customName(victim), victim.getType().name());
    }

    /** Nom d'un mob custom (KOALA) : nom affiché sans couleurs, en majuscules. */
    private static String customName(Entity entity) {
        String name = entity.getCustomName();
        if (name == null || name.isEmpty()) {
            return null;
        }
        return ChatColor.stripColor(name).trim().toUpperCase(Locale.ROOT).replace(' ', '_');
    }

    // ===================================================== Minage et récolte

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (plugin.settings().ignorePlaced) {
            event.getBlock().setMetadata(PLACED, new FixedMetadataValue(plugin, Boolean.TRUE));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        boolean placed = block.hasMetadata(PLACED);
        if (placed) {
            block.removeMetadata(PLACED, plugin);
        }
        Player player = event.getPlayer();
        String name = block.getType().name();
        String withData = name + ":" + block.getData();
        Boolean mature = mature(block.getType(), block.getData());
        if (mature == null && !(placed && plugin.settings().ignorePlaced)) {
            // bloc qui ne pousse pas par stades : minage (et melons, citrouilles, cannes pour l'agriculteur)
            reward(player, Job.Source.MINER, 1, withData, name);
            reward(player, Job.Source.RECOLTER, 1, withData, name);
        } else if (Boolean.TRUE.equals(mature)) {
            reward(player, Job.Source.RECOLTER, 1, withData, name);
        }
    }

    /** Culture mûre ? null si le bloc ne pousse pas par stades. */
    private Boolean mature(Material material, int data) {
        Integer max = plugin.settings().moddedCrops.get(material.name());
        if (max != null) {
            return data >= max;
        }
        switch (material) {
            case CROPS:
            case CARROT:
            case POTATO:
                return data >= 7;
            case NETHER_WARTS:
                return data >= 3;
            case COCOA:
                return (data >> 2) >= 2;
            default:
                return null;
        }
    }

    // ================================================================ Pêche

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH || !(event.getCaught() instanceof Item)) {
            return;
        }
        ItemStack stack = ((Item) event.getCaught()).getItemStack();
        String name = stack.getType().name();
        reward(event.getPlayer(), Job.Source.PECHER, stack.getAmount(), name + ":" + stack.getDurability(), name);
    }

    // ========================================================== Fabrication

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player) || event.getCurrentItem() == null) {
            return;
        }
        ItemStack result = event.getRecipe().getResult();
        int crafts = 1;
        if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            // shift-clic : autant de fois que l'ingrédient le plus rare le permet
            crafts = Integer.MAX_VALUE;
            for (ItemStack ingredient : event.getInventory().getMatrix()) {
                if (ingredient != null && ingredient.getType() != Material.AIR) {
                    crafts = Math.min(crafts, ingredient.getAmount());
                }
            }
            if (crafts == Integer.MAX_VALUE) {
                crafts = 1;
            }
        }
        String name = result.getType().name();
        reward((Player) event.getWhoClicked(), Job.Source.FABRIQUER, result.getAmount() * crafts,
                name + ":" + result.getDurability(), name);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        plugin.store().setName(event.getPlayer().getUniqueId(), event.getPlayer().getName());
    }
}
