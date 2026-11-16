package com.minidb.engine;

import java.util.*;

/** Хеш-индекс: value → {rowid}. */
public class HashIndex {
    private final String name, tableName, column;
    private final Map<Object, Set<Long>> map = new HashMap<>();
    private final Set<Long> nulls = new HashSet<>();

    public HashIndex(String name, String tableName, String column) {
        this.name = name; this.tableName = tableName; this.column = column;
    }
    public void add(Object v, long rowid) {
        if (v == null) nulls.add(rowid);
        else map.computeIfAbsent(v, k -> new HashSet<>()).add(rowid);
    }
    public void remove(Object v, long rowid) {
        if (v == null) nulls.remove(rowid);
        else { Set<Long> s = map.get(v); if (s != null) { s.remove(rowid); if (s.isEmpty()) map.remove(v); } }
    }
    public Set<Long> lookup(Object v) {
        if (v == null) return new HashSet<>(nulls);
        Set<Long> s = map.get(v);
        return s == null ? new HashSet<>() : new HashSet<>(s);
    }
    public void clear() { map.clear(); nulls.clear(); }
    public String getName() { return name; }
    public String getTableName() { return tableName; }
    public String getColumn() { return column; }
    public Map<Object, Set<Long>> getMap() { return map; }
    public Set<Long> getNulls() { return nulls; }
}