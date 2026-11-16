package com.minidb.engine;

import com.minidb.DbException;
import java.util.*;

/** Таблица: схема, строки, индексы, кэш PK. */
public class Table {
    private final String name;
    private final List<Column> columns = new ArrayList<>();
    private final List<Row> rows = new ArrayList<>();
    private final Map<String, HashIndex> indexes = new LinkedHashMap<>();
    private final Map<Object, Row> pkMap = new HashMap<>();
    private long nextRowid = 1;

    public Table(String name) { this.name = name; }
    public String getName() { return name; }
    public List<Column> getColumns() { return columns; }
    public List<Row> getRows() { return rows; }
    public Map<String, HashIndex> getIndexes() { return indexes; }
    public void addColumn(Column c) { columns.add(c); }

    public int columnIndex(String n) {
        for (int i = 0; i < columns.size(); i++)
            if (columns.get(i).getName().equalsIgnoreCase(n)) return i;
        return -1;
    }
    public Column getColumn(String n) {
        int i = columnIndex(n);
        if (i < 0) throw new DbException("Unknown column: " + n);
        return columns.get(i);
    }
    public Column getPrimaryKey() {
        for (Column c : columns) if (c.isPrimaryKey()) return c;
        return null;
    }
    public Row findRowByPk(Object pk) { return pkMap.get(pk); }
    public long allocateRowid() { return nextRowid++; }

    public void addRow(Row row) {
        rows.add(row);
        Column pk = getPrimaryKey();
        if (pk != null) pkMap.put(row.get(pk.getName()), row);
        for (HashIndex idx : indexes.values())
            idx.add(row.get(idx.getColumn()), row.getRowid());
    }
    public void rebuildAllIndexes() {
        pkMap.clear();
        Column pk = getPrimaryKey();
        if (pk != null) for (Row r : rows) pkMap.put(r.get(pk.getName()), r);
        for (HashIndex idx : indexes.values()) {
            idx.clear();
            for (Row r : rows) idx.add(r.get(idx.getColumn()), r.getRowid());
        }
    }
    public HashIndex getIndexForColumn(String column) {
        for (HashIndex idx : indexes.values())
            if (idx.getColumn().equalsIgnoreCase(column)) return idx;
        return null;
    }
    public HashIndex createIndex(String idxName, String column) {
        Column c = getColumn(column);
        if (getIndexForColumn(c.getName()) != null)
            throw new DbException("Index on column " + c.getName() + " already exists");
        if (indexes.containsKey(idxName))
            throw new DbException("Index name already exists: " + idxName);
        HashIndex idx = new HashIndex(idxName, name, c.getName());
        for (Row r : rows) idx.add(r.get(idx.getColumn()), r.getRowid());
        indexes.put(idxName, idx);
        return idx;
    }
}