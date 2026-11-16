package com.minidb.engine;

/** Описание колонки: имя, тип, признаки PK/FK. */
public class Column {
    private final String name;
    private final DataType type;
    private final boolean primaryKey;
    private final String refTable;
    private final String refColumn;

    public Column(String name, DataType type, boolean primaryKey, String refTable, String refColumn) {
        this.name = name;
        this.type = type;
        this.primaryKey = primaryKey;
        this.refTable = refTable;
        this.refColumn = refColumn;
    }
    public String getName() { return name; }
    public DataType getType() { return type; }
    public boolean isPrimaryKey() { return primaryKey; }
    public boolean isForeignKey() { return refTable != null; }
    public String getRefTable() { return refTable; }
    public String getRefColumn() { return refColumn; }
}