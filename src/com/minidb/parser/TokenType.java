package com.minidb.parser;

public enum TokenType {
    IDENTIFIER, NUMBER, STRING,
    SELECT, INSERT, INTO, VALUES, FROM, WHERE, UPDATE, SET, DELETE,
    CREATE, TABLE, INDEX, ON, DROP, AND, OR, PRIMARY, KEY, REFERENCES,
    SHOW, DESCRIBE, NULL, TRUE, FALSE,
    INT, DOUBLE, VARCHAR, DATE, BOOLEAN_TYPE,
    COMMA, LPAREN, RPAREN, SEMICOLON, STAR, MINUS,
    OP_EQ, OP_NEQ, OP_LT, OP_LTE, OP_GT, OP_GTE, EOF;

    public static TokenType keywordFor(String w) {
        switch (w.toUpperCase()) {
            case "SELECT": return SELECT; case "INSERT": return INSERT; case "INTO": return INTO;
            case "VALUES": return VALUES; case "FROM": return FROM; case "WHERE": return WHERE;
            case "UPDATE": return UPDATE; case "SET": return SET; case "DELETE": return DELETE;
            case "CREATE": return CREATE; case "TABLE": return TABLE; case "INDEX": return INDEX;
            case "ON": return ON; case "DROP": return DROP; case "AND": return AND; case "OR": return OR;
            case "PRIMARY": return PRIMARY; case "KEY": return KEY; case "REFERENCES": return REFERENCES;
            case "SHOW": return SHOW; case "DESCRIBE": return DESCRIBE;
            case "NULL": return NULL; case "TRUE": return TRUE; case "FALSE": return FALSE;
            case "INT": case "INTEGER": return INT;
            case "DOUBLE": case "FLOAT": case "REAL": return DOUBLE;
            case "VARCHAR": case "STRING": case "TEXT": case "CHAR": return VARCHAR;
            case "DATE": return DATE;
            case "BOOLEAN": case "BOOL": return BOOLEAN_TYPE;
            default: return null;
        }
    }
}