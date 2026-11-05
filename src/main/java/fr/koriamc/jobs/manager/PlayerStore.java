package fr.koriamc.jobs.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * XP des joueurs dans chaque métier, enregistrée dans joueurs.json (dossier du plugin).
 */
public final class PlayerStore {

    /** Données d'un joueur. */
    public static final class Entry {
        public String nom = "";
        public Map<String, Long> xp = new LinkedHashMap<String, Long>();
    }

    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final Type TYPE = new TypeToken<Map<String, Entry>>() {
    }.getType();

    private final JavaPlugin plugin;
    private final File file;
    private final Gson gson = new GsonBuilder().create();
    private Map<String, Entry> data = new HashMap<String, Entry>();
    private boolean dirty;

    public PlayerStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "joueurs.json");
    }

    public void load() {
        data = new HashMap<String, Entry>();
        if (!file.isFile()) {
            return;
        }
        Reader reader = null;
        try {
            reader = new InputStreamReader(new FileInputStream(file), UTF8);
            Map<String, Entry> read = gson.fromJson(reader, TYPE);
            if (read != null) {
                data.putAll(read);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Lecture de joueurs.json impossible", e);
        } finally {
            close(reader);
        }
    }

    /** Écrit le fichier s'il a changé (fichier temporaire puis renommage). */
    public void save() {
        String json;
        synchronized (this) {
            if (!dirty) {
                return;
            }
            json = gson.toJson(data, TYPE);
            dirty = false;
        }
        File tmp = new File(file.getParentFile(), file.getName() + ".tmp");
        Writer writer = null;
        try {
            file.getParentFile().mkdirs();
            writer = new OutputStreamWriter(new FileOutputStream(tmp), UTF8);
            writer.write(json);
            writer.close();
            writer = null;
            if (file.exists() && !file.delete()) {
                plugin.getLogger().warning("Impossible de remplacer joueurs.json");
            }
            if (!tmp.renameTo(file)) {
                plugin.getLogger().warning("Impossible de renommer " + tmp.getName());
            }
        } catch (Exception e) {
            dirty = true;
            plugin.getLogger().log(Level.SEVERE, "Écriture de joueurs.json impossible", e);
        } finally {
            close(writer);
        }
    }

    public synchronized Entry get(UUID uuid) {
        Entry entry = data.get(uuid.toString());
        if (entry == null) {
            entry = new Entry();
            data.put(uuid.toString(), entry);
        }
        if (entry.xp == null) {
            entry.xp = new LinkedHashMap<String, Long>();
        }
        return entry;
    }

    public synchronized Entry find(UUID uuid) {
        return data.get(uuid.toString());
    }

    public synchronized long getXp(UUID uuid, String job) {
        Entry entry = data.get(uuid.toString());
        Long value = entry == null || entry.xp == null ? null : entry.xp.get(job);
        return value == null ? 0 : value;
    }

    public synchronized void setXp(UUID uuid, String name, String job, long xp) {
        Entry entry = get(uuid);
        if (name != null) {
            entry.nom = name;
        }
        entry.xp.put(job, Math.max(0, xp));
        dirty = true;
    }

    public synchronized void setName(UUID uuid, String name) {
        Entry entry = get(uuid);
        if (!name.equals(entry.nom)) {
            entry.nom = name;
            dirty = true;
        }
    }

    /** Copie (UUID, nom, XP) des joueurs ayant de l'XP dans ce métier. */
    public synchronized List<Object[]> snapshot(String job) {
        List<Object[]> out = new ArrayList<Object[]>();
        for (Map.Entry<String, Entry> e : data.entrySet()) {
            Long xp = e.getValue().xp == null ? null : e.getValue().xp.get(job);
            if (xp != null && xp > 0) {
                out.add(new Object[]{e.getKey(), e.getValue().nom, xp});
            }
        }
        return out;
    }

    private static void close(java.io.Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (Exception ignored) {
                // rien
            }
        }
    }
}
