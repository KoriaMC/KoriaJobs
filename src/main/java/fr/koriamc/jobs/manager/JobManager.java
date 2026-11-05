package fr.koriamc.jobs.manager;

import fr.koriamc.jobs.KoriaJobs;
import fr.koriamc.jobs.api.JobLevelUpEvent;
import fr.koriamc.jobs.hook.VaultHook;
import fr.koriamc.jobs.model.Job;
import fr.koriamc.jobs.util.Sounds;
import fr.koriamc.jobs.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * XP, niveaux et récompenses des métiers.
 */
public final class JobManager {

    private final KoriaJobs plugin;

    public JobManager(KoriaJobs plugin) {
        this.plugin = plugin;
    }

    public long getXp(UUID uuid, Job job) {
        return plugin.store().getXp(uuid, job.getId());
    }

    public int getLevel(UUID uuid, Job job) {
        return job.levelFor(getXp(uuid, job));
    }

    /**
     * Ajoute de l'XP dans un métier, donne les récompenses des niveaux atteints.
     *
     * @return les niveaux gagnés
     */
    public int addXp(Player player, Job job, long amount, boolean notify) {
        if (amount <= 0) {
            return 0;
        }
        double multiplier = plugin.settings().multiplier(player);
        amount = Math.max(1, Math.round(amount * multiplier));
        UUID uuid = player.getUniqueId();
        long before = getXp(uuid, job);
        int oldLevel = job.levelFor(before);
        long after = before + amount;
        plugin.store().setXp(uuid, player.getName(), job.getId(), after);
        int newLevel = job.levelFor(after);
        if (notify && plugin.settings().actionBar) {
            plugin.msg().send(player, "xp-gain", vars(job, after, "gain", Text.number(amount, 0)));
        }
        for (int level = oldLevel + 1; level <= newLevel; level++) {
            levelUp(player, job, level);
        }
        return newLevel - oldLevel;
    }

    /** Fixe l'XP totale (commande admin), sans donner les récompenses. */
    public void setXp(OfflinePlayer player, Job job, long xp) {
        plugin.store().setXp(player.getUniqueId(), player.getName(), job.getId(), xp);
    }

    private void levelUp(Player player, Job job, int level) {
        plugin.msg().send(player, "level-up", "metier", Text.color(job.getName()), "niveau", level,
                "max", job.getMaxLevel());
        for (String line : job.rewardTexts(level)) {
            plugin.msg().send(player, "level-up-reward", "recompense", Text.color(line));
        }
        for (Map.Entry<String, Integer> unlock : job.getUnlocks().entrySet()) {
            if (unlock.getValue() == level) {
                plugin.msg().send(player, "unlock", "deblocage", unlockName(unlock.getKey()));
            }
        }
        give(player, job, level);
        Sounds.play(player, plugin.settings().sound(level >= job.getMaxLevel() ? "niveau-max" : "niveau"));
        if (level >= job.getMaxLevel()) {
            plugin.msg().broadcast("max-announce", "joueur", player.getName(), "metier", Text.color(job.getName()));
        }
        Bukkit.getPluginManager().callEvent(new JobLevelUpEvent(player, job.getId(), level));
    }

    /** Exécute les actions de récompense d'un niveau. */
    @SuppressWarnings("deprecation")
    private void give(Player player, Job job, int level) {
        Object[] kv = {"joueur", player.getName(), "player", player.getName(), "niveau", level,
                "metier", Text.color(job.getName())};
        for (String action : job.rewards(level)) {
            int colon = action.indexOf(':');
            String type = (colon < 0 ? action : action.substring(0, colon)).trim().toUpperCase(Locale.ROOT);
            String value = colon < 0 ? "" : Text.replace(action.substring(colon + 1), kv);
            try {
                if (type.equals("ARGENT")) {
                    VaultHook economy = plugin.economy();
                    if (economy == null) {
                        plugin.getLogger().warning("Récompense ARGENT non donnée à " + player.getName() + " : Vault absent.");
                    } else {
                        economy.deposit(player, Double.parseDouble(value.trim()));
                    }
                } else if (type.equals("ITEM")) {
                    ItemStack item = parseItem(value);
                    if (item == null) {
                        plugin.getLogger().warning("jobs.json : objet inconnu dans '" + action + "'");
                        continue;
                    }
                    for (ItemStack rest : player.getInventory().addItem(item).values()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), rest);
                    }
                } else if (type.equals("CONSOLE")) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), value.startsWith("/") ? value.substring(1) : value);
                } else if (type.equals("MESSAGE")) {
                    plugin.msg().sendLine(player, plugin.msg().format(value));
                } else if (type.equals("ANNONCE")) {
                    String line = plugin.msg().format(value);
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        plugin.msg().sendLine(p, line);
                    }
                } else {
                    plugin.getLogger().warning("jobs.json : action de récompense inconnue '" + action + "'");
                }
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("jobs.json : nombre invalide dans '" + action + "'");
            }
        }
    }

    /** "MATERIAU[:data]:quantite" */
    @SuppressWarnings("deprecation")
    public static ItemStack parseItem(String spec) {
        String[] parts = spec.trim().split(":");
        Material material = Material.matchMaterial(parts[0].toUpperCase(Locale.ROOT));
        if (material == null) {
            return null;
        }
        short data = 0;
        int amount = 1;
        if (parts.length == 2) {
            amount = Integer.parseInt(parts[1]);
        } else if (parts.length >= 3) {
            data = Short.parseShort(parts[1]);
            amount = Integer.parseInt(parts[2]);
        }
        return new ItemStack(material, Math.max(1, amount), data);
    }

    // ============================================================ Déblocages

    /** Le joueur a-t-il le niveau requis pour ce déblocage (medicament_jaune...) ? */
    public boolean isUnlocked(UUID uuid, String unlock) {
        String key = unlock.toLowerCase(Locale.ROOT);
        for (Job job : plugin.jobs().all()) {
            Integer required = job.getUnlocks().get(key);
            if (required != null) {
                return getLevel(uuid, job) >= required;
            }
        }
        return true;
    }

    public String unlockName(String key) {
        String path = "deblocages." + key;
        return plugin.msg().has(path) ? plugin.msg().text(path) : key;
    }

    // ============================================================ Affichage

    public Object[] vars(Job job, long xp, Object... extra) {
        int level = job.levelFor(xp);
        boolean max = level >= job.getMaxLevel();
        long from = job.threshold(level);
        long to = max ? from : job.threshold(level + 1);
        int percent = max ? 100 : (int) Math.floor((xp - from) * 100.0 / Math.max(1, to - from));
        Object[] base = {
                "metier", Text.color(job.getName()),
                "niveau", level,
                "max", job.getMaxLevel(),
                "xp", Text.number(xp, 0),
                "requis", Text.number(to, 0),
                "restant", Text.number(Math.max(0, to - xp), 0),
                "pourcentage", percent,
                "barre", bar(percent)
        };
        Object[] all = new Object[base.length + extra.length];
        System.arraycopy(extra, 0, all, 0, extra.length);
        System.arraycopy(base, 0, all, extra.length, base.length);
        return all;
    }

    public String bar(int percent) {
        int length = 20;
        int filled = (int) Math.round(length * Math.max(0, Math.min(100, percent)) / 100.0);
        StringBuilder sb = new StringBuilder("§a");
        for (int i = 0; i < length; i++) {
            if (i == filled) {
                sb.append("§8");
            }
            sb.append('|');
        }
        return sb.toString();
    }

    /** Classement d'un métier : [nom, xp] triés par XP décroissante. */
    public List<Object[]> top(Job job, int limit) {
        List<Object[]> list = plugin.store().snapshot(job.getId());
        Collections.sort(list, new Comparator<Object[]>() {
            @Override
            public int compare(Object[] a, Object[] b) {
                return Long.compare((Long) b[2], (Long) a[2]);
            }
        });
        List<Object[]> out = new ArrayList<Object[]>();
        for (int i = 0; i < Math.min(limit, list.size()); i++) {
            out.add(new Object[]{list.get(i)[1], list.get(i)[2]});
        }
        return out;
    }
}
