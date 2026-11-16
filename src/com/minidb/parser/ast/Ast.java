package com.minidb.parser.ast;

import com.minidb.engine.Column;
import com.minidb.engine.Condition;
import java.util.List;
import java.util.Map;

/** Все AST-узлы. Сгруппированы в одном файле для компактности. */
public final class Ast {
    private Ast() {}
    public interface Statement {}

    public static class Insert implements Statement {
        public final String table;
        public final List<String> columns;      // null = все
        public final List<List<Object>> rows;
        public Insert(String t, List<String> c, List<List<Object>> r) { table = t; columns = c; rows = r; }
    }
    public static class Select implements Statement {
        public final String table;
        public final List<String> columns;      // null = *
        public final Condition where;
        public Select(String t, List<String> c, Condition w) { table = t; columns = c; where = w; }
    }
    public static class Update implements Statement {
        public final String table;
        public final Map<String, Object> set;
        public final Condition where;
        public Update(String t, Map<String, Object> s, Condition w) { table = t; set = s; where = w; }
    }
    public static class Delete implements Statement {
        public final String table; public final Condition where;
        public Delete(String t, Condition w) { table = t; where = w; }
    }
    public static class CreateTable implements Statement {
        public final String name; public final List<Column> columns;
        public CreateTable(String n, List<Column> c) { name = n; columns = c; }
    }
    public static class DropTable implements Statement {
        public final String name; public DropTable(String n) { name = n; }
    }
    public static class CreateIndex implements Statement {
        public final String name, table, column;
        public CreateIndex(String n, String t, String c) { name = n; table = t; column = c; }
    }
    public static class DropIndex implements Statement {
        public final String name, table;
        public DropIndex(String n, String t) { name = n; table = t; }
    }
    public static class ShowTables implements Statement {}
    public static class Describe implements Statement {
        public final String table; public Describe(String t) { table = t; }
    }
}