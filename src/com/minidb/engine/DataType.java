package com.minidb.engine;

import com.minidb.DbException;

/** Поддерживаемые типы данных колонок. */
public enum DataType {
    INT, DOUBLE, STRING, DATE, BOOLEAN;

    public static DataType fromSql(String s) {
        switch (s.toUpperCase()) {
            case "INT": case "INTEGER": return INT;
            case "DOUBLE": case "FLOAT": case "REAL": return DOUBLE;
            case "VARCHAR": case "STRING": case "TEXT": case "CHAR": return STRING;
            case "DATE": return DATE;
            case "BOOLEAN": case "BOOL": return BOOLEAN;
            default: throw new DbException("Unknown data type: " + s);
        }
    }
}