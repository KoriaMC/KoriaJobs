package fr.koriamc.jobs.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Réglages de config.json.
 */
public final class Settings {

    public boolean actionBar;
    public boolean ignorePlaced;
    public boolean creative;
    public int saveMinutes;
    public final Set<String> disabledWorlds = new HashSet<String>();
    /** Permission -> multiplicateur d'XP (le plus grand s'applique). */
    public final Map<String, Double> multipliers = new LinkedHashMap<String, Double>();
    /** Culture modée -> data de la culture mûre. */
    public final Map<String, Integer> moddedCrops = new HashMap<String, Integer>();
    private final Map<String, String> sounds = new HashMap<String, String>();

    public void load(ConfigurationSection c) {
        actionBar = c.getBoolean("affichage.barre-action", true);
        ignorePlaced = c.getBoolean("anti-abus.ignorer-blocs-poses", true);
        creative = c.getBoolean("anti-abus.xp-en-creatif", false);
        saveMinutes = Math.max(1, c.getInt("sauvegarde-auto-minutes", 5));
        disabledWorlds.clear();
        for (String w : c.getStringList("anti-abus.mondes-desactives")) {
            disabledWorlds.add(w.toLowerCase(Locale.ROOT));
        }
        multipliers.clear();
        ConfigurationSection m = c.getConfigurationSection("multiplicateurs");
        if (m != null) {
            for (String perm : m.getKeys(false)) {
                multipliers.put(perm, m.getDouble(perm, 1));
            }
        }
        moddedCrops.clear();
        ConfigurationSection crops = c.getConfigurationSection("cultures-modees");
        if (crops != null) {
            for (String crop : crops.getKeys(false)) {
                moddedCrops.put(crop.trim().toUpperCase(Locale.ROOT), crops.getInt(crop, 7));
            }
        }
        sounds.clear();
        ConfigurationSection s = c.getConfigurationSection("sons");
        if (s != null) {
            for (String key : s.getKeys(false)) {
                sounds.put(key, s.getString(key, ""));
            }
        }
    }

    public String sound(String key) {
        String value = sounds.get(key);
        return value == null ? "" : value;
    }

    /** Multiplicateur d'XP du joueur (boosts par permission). */
    public double multiplier(Player player) {
        double best = 1;
        for (Map.Entry<String, Double> e : multipliers.entrySet()) {
            if (e.getValue() > best && player.hasPermission(e.getKey())) {
                best = e.getValue();
            }
        }
        return best;
    }
}
