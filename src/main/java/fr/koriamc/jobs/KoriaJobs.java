package fr.koriamc.jobs;

import fr.koriamc.jobs.command.JobsCommand;
import fr.koriamc.jobs.config.JobCatalog;
import fr.koriamc.jobs.config.JsonFile;
import fr.koriamc.jobs.config.Messages;
import fr.koriamc.jobs.config.Settings;
import fr.koriamc.jobs.gui.JobsMenu;
import fr.koriamc.jobs.hook.VaultHook;
import fr.koriamc.jobs.listener.JobListener;
import fr.koriamc.jobs.manager.JobManager;
import fr.koriamc.jobs.manager.PlayerStore;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * KoriaJobs : métiers chasseur, agriculteur, mineur, pharmacien et pêcheur.
 * <p>
 * Fichiers : config.json (réglages), jobs.json (métiers), messages.json, joueurs.json (données).
 */
public final class KoriaJobs extends JavaPlugin {

    private static KoriaJobs instance;

    private JsonFile configFile;
    private JsonFile jobsFile;
    private JsonFile messagesFile;

    private final Settings settings = new Settings();
    private final JobCatalog jobs = new JobCatalog();
    private final Messages messages = new Messages();
    private PlayerStore store;
    private JobManager manager;
    private JobsMenu menu;
    private VaultHook vault;
    private boolean vaultChecked;
    private BukkitTask saveTask;

    public static KoriaJobs get() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        configFile = new JsonFile(this, "config.json", "multiplicateurs", "cultures-modees");
        jobsFile = new JsonFile(this, "jobs.json", "*", "*.xp", "*.recompenses", "*.recompenses.*",
                "*.deblocages", "*.bonus", "*.bonus.*");
        messagesFile = new JsonFile(this, "messages.json", "deblocages", "noms");
        store = new PlayerStore(this);
        manager = new JobManager(this);
        menu = new JobsMenu(this);

        for (String problem : loadConfigs()) {
            getLogger().warning(problem);
        }
        store.load();

        Bukkit.getPluginManager().registerEvents(new JobListener(this), this);
        Bukkit.getPluginManager().registerEvents(menu, this);
        PluginCommand command = getCommand("jobs");
        if (command != null) {
            JobsCommand executor = new JobsCommand(this);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }
        scheduleSave();
        getLogger().info("KoriaJobs activé (" + jobs.all().size() + " métier(s)).");
    }

    @Override
    public void onDisable() {
        if (saveTask != null) {
            saveTask.cancel();
        }
        if (store != null) {
            store.save();
        }
        instance = null;
    }

    private void scheduleSave() {
        if (saveTask != null) {
            saveTask.cancel();
        }
        long ticks = settings.saveMinutes * 60L * 20L;
        saveTask = Bukkit.getScheduler().runTaskTimerAsynchronously(this, new Runnable() {
            @Override
            public void run() {
                store.save();
            }
        }, ticks, ticks);
    }

    private List<String> loadConfigs() {
        List<String> problems = new ArrayList<String>();
        for (JsonFile file : Arrays.asList(configFile, jobsFile, messagesFile)) {
            String error = file.load();
            if (error != null) {
                problems.add(file.getName() + " : " + error);
            }
        }
        settings.load(configFile.toConfiguration());
        messages.load(messagesFile.toConfiguration());
        // jobs.json sans valeurs du jar : un métier ou une récompense retiré reste retiré
        Configuration jobsConfig = jobsFile.toConfiguration();
        jobsConfig.setDefaults(new MemoryConfiguration());
        jobs.load(jobsConfig);
        problems.addAll(jobs.getProblems());
        return problems;
    }

    public List<String> reload() {
        List<String> problems = loadConfigs();
        scheduleSave();
        return problems;
    }

    public VaultHook economy() {
        if (vault == null && !vaultChecked && Bukkit.getPluginManager().getPlugin("Vault") != null) {
            vaultChecked = true;
            vault = VaultHook.create();
        }
        return vault;
    }

    public Settings settings() {
        return settings;
    }

    public JobCatalog jobs() {
        return jobs;
    }

    public Messages msg() {
        return messages;
    }

    public PlayerStore store() {
        return store;
    }

    public JobManager manager() {
        return manager;
    }

    public JobsMenu menu() {
        return menu;
    }
}
