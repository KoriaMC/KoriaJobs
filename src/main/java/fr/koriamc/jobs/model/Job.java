package fr.koriamc.jobs.model;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Un métier de jobs.json : façon de gagner de l'XP, paliers de niveaux, récompenses et déblocages.
 */
public final class Job {

    /** Façon de gagner de l'XP. */
    public enum Source {
        /** Mobs tués (clé : type d'entité, ou nom du mob custom comme KOALA). */
        TUER,
        /** Cultures mûres récoltées (clé : matériau du bloc). */
        RECOLTER,
        /** Blocs minés, hors blocs posés (clé : matériau, ou matériau:data). */
        MINER,
        /** Prises à la pêche (clé : matériau, matériau:data). */
        PECHER,
        /** Objets fabriqués : établi, machines (clé : matériau du résultat). */
        FABRIQUER;

        public static Source parse(String value) {
            if (value == null) {
                return null;
            }
            try {
                return valueOf(value.trim().toUpperCase(Locale.ROOT).replace('É', 'E'));
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

    private final String id;
    private final String name;
    private final String icon;
    private final Source source;
    private final List<String> description;
    /** XP totale à atteindre pour chaque niveau (index 0 = niveau 1). */
    private final List<Long> thresholds;
    private final Map<String, Integer> xp;
    private final Map<Integer, List<String>> rewards;
    private final Map<Integer, List<String>> rewardTexts;
    /** Déblocage (medicament_jaune...) -> niveau requis. */
    private final Map<String, Integer> unlocks;
    /** Bonus (vitesse-machine...) -> niveau -> valeur. */
    private final Map<String, TreeMap<Integer, Double>> bonuses;

    public Job(String id, String name, String icon, Source source, List<String> description, List<Long> thresholds,
               Map<String, Integer> xp, Map<Integer, List<String>> rewards, Map<Integer, List<String>> rewardTexts,
               Map<String, Integer> unlocks, Map<String, TreeMap<Integer, Double>> bonuses) {
        this.id = id;
        this.name = name;
        this.icon = icon;
        this.source = source;
        this.description = Collections.unmodifiableList(description);
        this.thresholds = Collections.unmodifiableList(thresholds);
        this.xp = Collections.unmodifiableMap(xp);
        this.rewards = Collections.unmodifiableMap(rewards);
        this.rewardTexts = Collections.unmodifiableMap(rewardTexts);
        this.unlocks = Collections.unmodifiableMap(unlocks);
        this.bonuses = Collections.unmodifiableMap(bonuses);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getIcon() {
        return icon;
    }

    public Source getSource() {
        return source;
    }

    public List<String> getDescription() {
        return description;
    }

    public int getMaxLevel() {
        return thresholds.size();
    }

    /** XP totale nécessaire pour atteindre {@code level} (0 pour le niveau 0). */
    public long threshold(int level) {
        if (level <= 0) {
            return 0;
        }
        return thresholds.get(Math.min(level, thresholds.size()) - 1);
    }

    /** Niveau correspondant à une XP totale. */
    public int levelFor(long totalXp) {
        int level = 0;
        while (level < thresholds.size() && totalXp >= thresholds.get(level)) {
            level++;
        }
        return level;
    }

    /** XP d'une action : clés essayées dans l'ordre (ex. "RAW_FISH:2" puis "RAW_FISH"), puis "*". */
    public int xpFor(String... keys) {
        for (String key : keys) {
            if (key == null) {
                continue;
            }
            Integer value = xp.get(key.toUpperCase(Locale.ROOT));
            if (value != null) {
                return Math.max(0, value);
            }
        }
        Integer any = xp.get("*");
        return any == null ? 0 : Math.max(0, any);
    }

    public Map<String, Integer> getXpTable() {
        return xp;
    }

    /** Actions de récompense d'un niveau (ARGENT:n, ITEM:..., CONSOLE:..., MESSAGE:..., ANNONCE:...). */
    public List<String> rewards(int level) {
        List<String> list = rewards.get(level);
        return list == null ? Collections.<String>emptyList() : list;
    }

    /** Lignes affichées pour la récompense d'un niveau. */
    public List<String> rewardTexts(int level) {
        List<String> list = rewardTexts.get(level);
        return list == null ? Collections.<String>emptyList() : list;
    }

    public Map<String, Integer> getUnlocks() {
        return unlocks;
    }

    /** Valeur d'un bonus au niveau donné (le plus haut palier atteint), ou {@code def}. */
    public double bonus(String key, int level, double def) {
        TreeMap<Integer, Double> steps = bonuses.get(key);
        if (steps == null) {
            return def;
        }
        Map.Entry<Integer, Double> entry = steps.floorEntry(level);
        return entry == null ? def : entry.getValue();
    }
}
