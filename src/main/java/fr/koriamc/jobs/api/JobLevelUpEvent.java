package fr.koriamc.jobs.api;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Un joueur passe un niveau dans un métier (après les récompenses).
 */
public final class JobLevelUpEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final String job;
    private final int level;

    public JobLevelUpEvent(Player player, String job, int level) {
        this.player = player;
        this.job = job;
        this.level = level;
    }

    public Player getPlayer() {
        return player;
    }

    /** Identifiant du métier (chasseur, pharmacien...). */
    public String getJob() {
        return job;
    }

    public int getLevel() {
        return level;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
