package com.minidb.engine;

import java.util.LinkedHashMap;
import java.util.Map;

/** Одна запись таблицы. Содержит внутренний rowid (для индексов) и значения по колонкам. */
public class Row {
    private final long rowid;
    private final LinkedHashMap<String, Object> values = new LinkedHashMap<>();
    public Row(long rowid) { this.rowid = rowid; }
    public long getRowid() { return rowid; }
    public Object get(String col) { return values.get(col); }
    public void put(String col, Object value) { values.put(col, value); }
    public Map<String, Object> getValues() { return values; }
}