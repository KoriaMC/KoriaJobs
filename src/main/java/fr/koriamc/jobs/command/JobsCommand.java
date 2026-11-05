package fr.koriamc.jobs.command;

import fr.koriamc.jobs.KoriaJobs;
import fr.koriamc.jobs.model.Job;
import fr.koriamc.jobs.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * /jobs [info &lt;métier&gt; | top &lt;métier&gt; | addxp | setxp | reload]
 */
public final class JobsCommand implements TabExecutor {

    private final KoriaJobs plugin;

    public JobsCommand(KoriaJobs plugin) {
        this.plugin = plugin;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String sub = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
        if (sub.isEmpty() || sub.equals("menu")) {
            if (!(sender instanceof Player)) {
                plugin.msg().send(sender, "commande.joueur-seulement");
                return true;
            }
            plugin.menu().open((Player) sender);
            return true;
        }
        if (sub.equals("info")) {
            Job job = args.length < 2 ? null : plugin.jobs().get(args[1]);
            if (!(sender instanceof Player) || job == null) {
                plugin.msg().send(sender, "commande.metier-inconnu", "metiers", jobList());
                return true;
            }
            plugin.menu().details((Player) sender, job);
            return true;
        }
        if (sub.equals("top")) {
            Job job = args.length < 2 ? null : plugin.jobs().get(args[1]);
            if (job == null) {
                plugin.msg().send(sender, "commande.metier-inconnu", "metiers", jobList());
                return true;
            }
            plugin.msg().send(sender, "top.entete", "metier", Text.color(job.getName()));
            int rank = 1;
            for (Object[] row : plugin.manager().top(job, 10)) {
                long xp = (Long) row[1];
                plugin.msg().send(sender, "top.ligne", "rang", rank++, "joueur", row[0], "niveau", job.levelFor(xp),
                        "xp", Text.number(xp, 0));
            }
            return true;
        }
        if (!sender.hasPermission("koriajobs.admin")) {
            plugin.msg().send(sender, "commande.permission");
            return true;
        }
        if (sub.equals("reload")) {
            List<String> problems = plugin.reload();
            plugin.msg().send(sender, "commande.recharge", "problemes", problems.size());
            for (String p : problems) {
                sender.sendMessage("§c- " + p);
            }
            return true;
        }
        if ((sub.equals("addxp") || sub.equals("setxp")) && args.length >= 4) {
            Job job = plugin.jobs().get(args[2]);
            Long amount = parseLong(args[3]);
            if (job == null || amount == null) {
                plugin.msg().send(sender, "commande.usage-admin");
                return true;
            }
            if (sub.equals("addxp")) {
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    plugin.msg().send(sender, "commande.joueur-hors-ligne", "joueur", args[1]);
                    return true;
                }
                plugin.manager().addXp(target, job, amount, true);
            } else {
                OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
                plugin.manager().setXp(target, job, amount);
            }
            plugin.msg().send(sender, "commande.xp-modifiee", "joueur", args[1], "metier", Text.color(job.getName()));
            return true;
        }
        plugin.msg().send(sender, "commande.aide");
        return true;
    }

    private static Long parseLong(String s) {
        try {
            return Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String jobList() {
        List<String> ids = new ArrayList<String>();
        for (Job job : plugin.jobs().all()) {
            ids.add(job.getId());
        }
        return Text.join(ids, ", ");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> ids = new ArrayList<String>();
        for (Job job : plugin.jobs().all()) {
            ids.add(job.getId());
        }
        if (args.length == 1) {
            List<String> subs = new ArrayList<String>(Arrays.asList("menu", "info", "top"));
            if (sender.hasPermission("koriajobs.admin")) {
                subs.addAll(Arrays.asList("addxp", "setxp", "reload"));
            }
            return Text.complete(args[0], subs);
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("info") || args[0].equalsIgnoreCase("top"))) {
            return Text.complete(args[1], ids);
        }
        if (args.length == 3 && (args[0].equalsIgnoreCase("addxp") || args[0].equalsIgnoreCase("setxp"))) {
            return Text.complete(args[2], ids);
        }
        return args.length == 2 ? null : Collections.<String>emptyList();
    }
}
