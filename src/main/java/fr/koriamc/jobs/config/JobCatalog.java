package fr.koriamc.jobs.config;

import fr.koriamc.jobs.model.Job;
import fr.koriamc.jobs.util.Items;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Les métiers de jobs.json, dans l'ordre du fichier.
 */
public final class JobCatalog {

    private final Map<String, Job> jobs = new LinkedHashMap<String, Job>();
    private final List<String> problems = new ArrayList<String>();

    public void load(ConfigurationSection file) {
        jobs.clear();
        problems.clear();
        for (String id : file.getKeys(false)) {
            ConfigurationSection s = file.getConfigurationSection(id);
            if (s == null || !s.getBoolean("actif", true)) {
                continue;
            }
            Job job = read(id.toLowerCase(Locale.ROOT), s);
            if (job != null) {
                jobs.put(job.getId(), job);
            }
        }
    }

    private Job read(String id, ConfigurationSection s) {
        String where = "jobs.json : " + id;
        Job.Source source = Job.Source.parse(s.getString("source"));
        if (source == null) {
            problems.add(where + ".source invalide : TUER, RECOLTER, MINER, PECHER ou FABRIQUER.");
            return null;
        }
        List<Long> thresholds = new ArrayList<Long>();
        long previous = 0;
        for (Object value : s.getList("niveaux", Collections.emptyList())) {
            long n;
            try {
                n = Long.parseLong(String.valueOf(value).trim());
            } catch (NumberFormatException e) {
                problems.add(where + ".niveaux : '" + value + "' n'est pas un nombre.");
                continue;
            }
            if (n <= previous) {
                problems.add(where + ".niveaux : les paliers d'XP doivent augmenter (" + n + ").");
                continue;
            }
            thresholds.add(n);
            previous = n;
        }
        if (thresholds.isEmpty()) {
            problems.add(where + ".niveaux est vide, métier ignoré.");
            return null;
        }
        Map<String, Integer> xp = new HashMap<String, Integer>();
        ConfigurationSection xpSection = s.getConfigurationSection("xp");
        if (xpSection != null) {
            for (String key : xpSection.getKeys(false)) {
                xp.put(key.trim().toUpperCase(Locale.ROOT), xpSection.getInt(key));
            }
        }
        Map<Integer, List<String>> rewards = new HashMap<Integer, List<String>>();
        Map<Integer, List<String>> texts = new HashMap<Integer, List<String>>();
        ConfigurationSection r = s.getConfigurationSection("recompenses");
        if (r != null) {
            for (String key : r.getKeys(false)) {
                int level = parseLevel(key);
                if (level < 1 || level > thresholds.size()) {
                    problems.add(where + ".recompenses." + key + " : niveau invalide (1 à " + thresholds.size() + ").");
                    continue;
                }
                ConfigurationSection l = r.getConfigurationSection(key);
                if (l != null) {
                    rewards.put(level, Items.stringList(l, "actions"));
                    texts.put(level, Items.stringList(l, "texte"));
                } else {
                    rewards.put(level, Items.stringList(r, key));
                }
            }
        }
        Map<String, Integer> unlocks = new LinkedHashMap<String, Integer>();
        ConfigurationSection u = s.getConfigurationSection("deblocages");
        if (u != null) {
            for (String key : u.getKeys(false)) {
                unlocks.put(key.trim().toLowerCase(Locale.ROOT), Math.max(0, u.getInt(key)));
            }
        }
        Map<String, TreeMap<Integer, Double>> bonuses = new HashMap<String, TreeMap<Integer, Double>>();
        ConfigurationSection b = s.getConfigurationSection("bonus");
        if (b != null) {
            for (String key : b.getKeys(false)) {
                ConfigurationSection steps = b.getConfigurationSection(key);
                if (steps == null) {
                    continue;
                }
                TreeMap<Integer, Double> map = new TreeMap<Integer, Double>();
                for (String level : steps.getKeys(false)) {
                    int l = parseLevel(level);
                    if (l >= 0) {
                        map.put(l, steps.getDouble(level));
                    }
                }
                bonuses.put(key.trim().toLowerCase(Locale.ROOT), map);
            }
        }
        return new Job(id, s.getString("nom", id), s.getString("icone", "PAPER"), source,
                Items.stringList(s, "description"), thresholds, xp, rewards, texts, unlocks, bonuses);
    }

    private static int parseLevel(String key) {
        try {
            return Integer.parseInt(key.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public Job get(String id) {
        return id == null ? null : jobs.get(id.toLowerCase(Locale.ROOT));
    }

    public Collection<Job> all() {
        return Collections.unmodifiableCollection(jobs.values());
    }

    /** Métiers qui gagnent de l'XP avec cette source. */
    public List<Job> bySource(Job.Source source) {
        List<Job> out = new ArrayList<Job>();
        for (Job job : jobs.values()) {
            if (job.getSource() == source) {
                out.add(job);
            }
        }
        return out;
    }

    public List<String> getProblems() {
        return problems;
    }
}
