package fr.koriamc.jobs.gui;

import fr.koriamc.jobs.KoriaJobs;
import fr.koriamc.jobs.manager.JobManager;
import fr.koriamc.jobs.model.Job;
import fr.koriamc.jobs.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Menu /jobs : un objet par métier avec niveau, progression, prochaine récompense et déblocages.
 * Un clic affiche le détail du métier dans le chat.
 */
public final class JobsMenu implements Listener {

    private static final class Holder implements InventoryHolder {
        final Map<Integer, String> slots = new HashMap<Integer, String>();
        Inventory inventory;

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    private static final int[] SLOTS = {11, 12, 13, 14, 15, 20, 21, 22, 23, 24};

    private final KoriaJobs plugin;

    public JobsMenu(KoriaJobs plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Holder holder = new Holder();
        Inventory inv = Bukkit.createInventory(holder, 36, Text.truncate(plugin.msg().text("menu.titre"), 32));
        holder.inventory = inv;
        int i = 0;
        for (Job job : plugin.jobs().all()) {
            if (i >= SLOTS.length) {
                break;
            }
            inv.setItem(SLOTS[i], icon(player, job));
            holder.slots.put(SLOTS[i], job.getId());
            i++;
        }
        player.openInventory(inv);
    }

    @SuppressWarnings("deprecation")
    private ItemStack icon(Player player, Job job) {
        ItemStack item = JobManager.parseItem(job.getIcon() + ":1");
        if (item == null) {
            item = new ItemStack(Material.PAPER);
        }
        long xp = plugin.manager().getXp(player.getUniqueId(), job);
        int level = job.levelFor(xp);
        Object[] kv = plugin.manager().vars(job, xp);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(plugin.msg().text("menu.nom", kv));
        List<String> lore = new ArrayList<String>();
        for (String line : job.getDescription()) {
            lore.add(Text.color(line));
        }
        lore.addAll(plugin.msg().lines(level >= job.getMaxLevel() ? "menu.lore-max" : "menu.lore", kv));
        if (level < job.getMaxLevel()) {
            List<String> next = new ArrayList<String>(job.rewardTexts(level + 1));
            for (Map.Entry<String, Integer> u : job.getUnlocks().entrySet()) {
                if (u.getValue() == level + 1) {
                    next.add(plugin.manager().unlockName(u.getKey()));
                }
            }
            if (!next.isEmpty()) {
                lore.add(plugin.msg().text("menu.prochaine"));
                for (String line : next) {
                    lore.add(plugin.msg().text("menu.ligne", "texte", Text.color(line)));
                }
            }
        }
        lore.add(plugin.msg().text("menu.clic"));
        meta.setLore(lore);
        try {
            meta.addItemFlags(ItemFlag.values());
        } catch (Throwable ignored) {
            // API ancienne
        }
        item.setItemMeta(meta);
        return item;
    }

    /** Détail d'un métier dans le chat : table d'XP et récompenses. */
    public void details(Player player, Job job) {
        long xp = plugin.manager().getXp(player.getUniqueId(), job);
        plugin.msg().send(player, "details.entete", plugin.manager().vars(job, xp));
        List<Map.Entry<String, Integer>> table = new ArrayList<Map.Entry<String, Integer>>(job.getXpTable().entrySet());
        java.util.Collections.sort(table, new java.util.Comparator<Map.Entry<String, Integer>>() {
            @Override
            public int compare(Map.Entry<String, Integer> a, Map.Entry<String, Integer> b) {
                return a.getValue() - b.getValue();
            }
        });
        for (Map.Entry<String, Integer> e : table) {
            plugin.msg().send(player, "details.xp", "cle", label(e.getKey()), "xp", e.getValue());
        }
        for (int level = 1; level <= job.getMaxLevel(); level++) {
            List<String> texts = new ArrayList<String>(job.rewardTexts(level));
            for (Map.Entry<String, Integer> u : job.getUnlocks().entrySet()) {
                if (u.getValue() == level) {
                    texts.add(plugin.manager().unlockName(u.getKey()));
                }
            }
            if (!texts.isEmpty()) {
                plugin.msg().send(player, "details.niveau", "niveau", level, "xp", Text.number(job.threshold(level), 0),
                        "recompense", Text.color(Text.join(texts, ", ")));
            }
        }
    }

    private String label(String key) {
        String path = "noms." + key.toUpperCase(Locale.ROOT).replace(':', '_');
        return plugin.msg().has(path) ? plugin.msg().text(path) : key.toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof Holder)) {
            return;
        }
        event.setCancelled(true);
        Holder holder = (Holder) event.getInventory().getHolder();
        String id = holder.slots.get(event.getRawSlot());
        if (id == null || !(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        Job job = plugin.jobs().get(id);
        if (job != null) {
            player.closeInventory();
            details(player, job);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof Holder) {
            event.setCancelled(true);
        }
    }
}
