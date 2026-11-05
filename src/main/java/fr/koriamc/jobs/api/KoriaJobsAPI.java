package fr.koriamc.jobs.api;

import fr.koriamc.jobs.KoriaJobs;
import fr.koriamc.jobs.model.Job;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Accès aux métiers pour les autres plugins (machines, quêtes...).
 * <p>
 * Exemple pour la machine médicale :
 * <pre>
 * if (KoriaJobsAPI.isUnlocked(player.getUniqueId(), "medicament_jaune")) { ... }
 * double vitesse = KoriaJobsAPI.getBonus(player.getUniqueId(), "pharmacien", "vitesse-machine", 1);
 * KoriaJobsAPI.reward(player, "pharmacien", "MEDICAMENT_JAUNE", 1);
 * </pre>
 */
public final class KoriaJobsAPI {

    private KoriaJobsAPI() {
    }

    private static KoriaJobs plugin() {
        KoriaJobs plugin = KoriaJobs.get();
        if (plugin == null) {
            throw new IllegalStateException("KoriaJobs n'est pas activé");
        }
        return plugin;
    }

    public static boolean isAvailable() {
        return KoriaJobs.get() != null;
    }

    /** Niveau du joueur dans un métier (0 si le métier n'existe pas). */
    public static int getLevel(UUID player, String job) {
        Job j = plugin().jobs().get(job);
        return j == null ? 0 : plugin().manager().getLevel(player, j);
    }

    /** XP totale du joueur dans un métier. */
    public static long getXp(UUID player, String job) {
        Job j = plugin().jobs().get(job);
        return j == null ? 0 : plugin().manager().getXp(player, j);
    }

    /** Ajoute de l'XP (récompenses des niveaux comprises). @return niveaux gagnés */
    public static int addXp(Player player, String job, long amount) {
        Job j = plugin().jobs().get(job);
        return j == null ? 0 : plugin().manager().addXp(player, j, amount, true);
    }

    /**
     * Donne l'XP prévue dans jobs.json pour une action (clé de la table "xp" du métier),
     * par exemple un médicament fabriqué par une machine.
     */
    public static int reward(Player player, String job, String key, int amount) {
        Job j = plugin().jobs().get(job);
        if (j == null) {
            return 0;
        }
        long xp = (long) j.xpFor(key) * Math.max(1, amount);
        return xp <= 0 ? 0 : plugin().manager().addXp(player, j, xp, true);
    }

    /** Déblocage atteint (medicament_jaune, machine_medicale...) ? Un déblocage inconnu est toujours accordé. */
    public static boolean isUnlocked(UUID player, String unlock) {
        return plugin().manager().isUnlocked(player, unlock);
    }

    /** Bonus du métier au niveau du joueur (vitesse-machine...), ou {@code def}. */
    public static double getBonus(UUID player, String job, String bonus, double def) {
        Job j = plugin().jobs().get(job);
        return j == null ? def : j.bonus(bonus.toLowerCase(java.util.Locale.ROOT), plugin().manager().getLevel(player, j), def);
    }
}
