package com.minidb.engine;

import com.minidb.DbException;
import com.minidb.storage.StorageManager;
import java.nio.file.Path;
import java.util.*;

/** Контейнер таблиц + связка со StorageManager. */
public class Database {
    private final StorageManager storage;
    private final Map<String, Table> tables = new LinkedHashMap<>();

    public Database(Path dataDir) { this.storage = new StorageManager(dataDir); }

    public void load() {
        tables.clear();
        for (Table t : storage.loadAllTables())
            tables.put(t.getName().toLowerCase(), t);
    }
    public void save() { storage.saveAll(tables.values()); }
    public Collection<Table> getTables() { return tables.values(); }
    public Table getTable(String n) {
        Table t = tables.get(n.toLowerCase());
        if (t == null) throw new DbException("Table not found: " + n);
        return t;
    }
    public boolean hasTable(String n) { return tables.containsKey(n.toLowerCase()); }
    public void addTable(Table t) {
        if (hasTable(t.getName())) throw new DbException("Table already exists: " + t.getName());
        tables.put(t.getName().toLowerCase(), t);
    }
    public void dropTable(String name) {
        if (tables.remove(name.toLowerCase()) == null)
            throw new DbException("Table not found: " + name);
        storage.deleteTable(name);
    }
    public void persistTable(Table t) { storage.saveTable(t); }
    public StorageManager getStorage() { return storage; }
}