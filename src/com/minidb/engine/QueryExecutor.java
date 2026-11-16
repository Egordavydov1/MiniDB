package com.minidb.engine;

import com.minidb.DbException;
import com.minidb.parser.ast.Ast;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

/** Выполняет AST-узлы, применяет индексы, обеспечивает ограничения PK/FK. */
public class QueryExecutor {
    private final Database db;
    private boolean autoSave = true;

    public QueryExecutor(Database db) { this.db = db; }
    public void setAutoSave(boolean v) { this.autoSave = v; }

    public QueryResult execute(Ast.Statement s) {
        long t0 = System.currentTimeMillis();
        QueryResult r = doExecute(s);
        if (autoSave && isMutation(s)) db.save();
        r.setElapsedMs(System.currentTimeMillis() - t0);
        return r;
    }

    private boolean isMutation(Ast.Statement s) {
        return s instanceof Ast.Insert || s instanceof Ast.Update || s instanceof Ast.Delete
                || s instanceof Ast.CreateTable || s instanceof Ast.DropTable
                || s instanceof Ast.CreateIndex || s instanceof Ast.DropIndex;
    }

    private QueryResult doExecute(Ast.Statement s) {
        if (s instanceof Ast.Select) return select((Ast.Select) s);
        if (s instanceof Ast.Insert) return insert((Ast.Insert) s);
        if (s instanceof Ast.Update) return update((Ast.Update) s);
        if (s instanceof Ast.Delete) return delete((Ast.Delete) s);
        if (s instanceof Ast.CreateTable) return createTable((Ast.CreateTable) s);
        if (s instanceof Ast.DropTable) return dropTable((Ast.DropTable) s);
        if (s instanceof Ast.CreateIndex) return createIndex((Ast.CreateIndex) s);
        if (s instanceof Ast.DropIndex) return dropIndex((Ast.DropIndex) s);
        if (s instanceof Ast.ShowTables) return showTables();
        if (s instanceof Ast.Describe) return describe((Ast.Describe) s);
        throw new DbException("Unsupported statement: " + s.getClass().getSimpleName());
    }

    private QueryResult select(Ast.Select s) {
        Table t = db.getTable(s.table);
        List<Column> sel = s.columns == null ? t.getColumns() : resolveColumns(t, s.columns);

        List<Row> candidates;
        if (s.where != null) {
            Set<Long> ids = s.where.tryIndex(t);
            if (ids != null) {
                candidates = new ArrayList<>();
                for (Row r : t.getRows()) if (ids.contains(r.getRowid())) candidates.add(r);
            } else candidates = t.getRows();
        } else candidates = t.getRows();

        List<List<String>> out = new ArrayList<>();
        for (Row r : candidates) {
            if (s.where != null && !s.where.test(r)) continue;
            List<String> vals = new ArrayList<>();
            for (Column c : sel) vals.add(str(r.get(c.getName())));
            out.add(vals);
        }
        List<String> hdr = new ArrayList<>();
        for (Column c : sel) hdr.add(c.getName());
        QueryResult qr = new QueryResult();
        qr.setColumns(hdr); qr.setRows(out); qr.setMessage(out.size() + " row(s)");
        return qr;
    }

    private QueryResult insert(Ast.Insert ins) {
        Table t = db.getTable(ins.table);
        List<Column> target = ins.columns == null ? t.getColumns() : resolveColumns(t, ins.columns);
        Column pk = t.getPrimaryKey();
        List<Row> pending = new ArrayList<>();
        Set<Object> batchPk = new HashSet<>();

        for (List<Object> vals : ins.rows) {
            if (vals.size() != target.size())
                throw new DbException("Column count mismatch: expected " + target.size() + ", got " + vals.size());
            Row r = new Row(t.allocateRowid());
            for (Column c : t.getColumns()) r.put(c.getName(), null);
            for (int i = 0; i < target.size(); i++) {
                Column c = target.get(i);
                r.put(c.getName(), coerce(vals.get(i), c.getType()));
            }
            if (pk != null) {
                Object v = r.get(pk.getName());
                if (v == null) throw new DbException("Primary key cannot be NULL");
                if (t.findRowByPk(v) != null || !batchPk.add(v))
                    throw new DbException("Duplicate primary key: " + v);
            }
            for (Column c : t.getColumns()) {
                if (c.isForeignKey() && r.get(c.getName()) != null) {
                    Table ref = db.getTable(c.getRefTable());
                    if (ref.findRowByPk(r.get(c.getName())) == null)
                        throw new DbException("FK violation on " + c.getName() + "=" + r.get(c.getName()));
                }
            }
            pending.add(r);
        }
        for (Row r : pending) t.addRow(r);
        return QueryResult.message(pending.size() + " row(s) inserted");
    }

    private QueryResult update(Ast.Update u) {
        Table t = db.getTable(u.table);
        List<Row> affected = new ArrayList<>();
        for (Row r : t.getRows()) if (u.where == null || u.where.test(r)) affected.add(r);

        List<Map<String, Object>> snapshot = new ArrayList<>();
        for (Row r : affected) snapshot.add(new LinkedHashMap<>(r.getValues()));

        try {
            for (Row r : affected)
                for (Map.Entry<String, Object> e : u.set.entrySet()) {
                    Column c = t.getColumn(e.getKey());
                    r.put(c.getName(), coerce(e.getValue(), c.getType()));
                }
            Column pk = t.getPrimaryKey();
            if (pk != null) {
                Set<Object> seen = new HashSet<>();
                for (Row r : t.getRows()) {
                    Object v = r.get(pk.getName());
                    if (v == null) continue;
                    if (!seen.add(v)) throw new DbException("Duplicate PK after update: " + v);
                }
            }
        } catch (RuntimeException ex) {
            for (int i = 0; i < affected.size(); i++) {
                affected.get(i).getValues().clear();
                affected.get(i).getValues().putAll(snapshot.get(i));
            }
            throw ex;
        }
        if (!affected.isEmpty()) t.rebuildAllIndexes();
        return QueryResult.message(affected.size() + " row(s) updated");
    }

    private QueryResult delete(Ast.Delete d) {
        Table t = db.getTable(d.table);
        Set<Long> ids = new HashSet<>();
        for (Row r : t.getRows()) if (d.where == null || d.where.test(r)) ids.add(r.getRowid());
        if (!ids.isEmpty()) {
            t.getRows().removeIf(r -> ids.contains(r.getRowid()));
            t.rebuildAllIndexes();
        }
        return QueryResult.message(ids.size() + " row(s) deleted");
    }

    private QueryResult createTable(Ast.CreateTable c) {
        Table t = new Table(c.name);
        Set<String> names = new HashSet<>();
        for (Column col : c.columns) {
            if (!names.add(col.getName().toLowerCase()))
                throw new DbException("Duplicate column: " + col.getName());
            t.addColumn(col);
        }
        db.addTable(t);
        return QueryResult.message("Table " + c.name + " created");
    }

    private QueryResult dropTable(Ast.DropTable d) {
        db.dropTable(d.name);
        return QueryResult.message("Table " + d.name + " dropped");
    }

    private QueryResult createIndex(Ast.CreateIndex c) {
        Table t = db.getTable(c.table);
        t.createIndex(c.name, c.column);
        return QueryResult.message("Index " + c.name + " created on " + c.table + "(" + c.column + ")");
    }

    private QueryResult dropIndex(Ast.DropIndex d) {
        Table t = db.getTable(d.table);
        if (t.getIndexes().remove(d.name) == null)
            throw new DbException("Index not found: " + d.name);
        db.getStorage().deleteIndexFile(t.getName(), d.name);
        return QueryResult.message("Index " + d.name + " dropped");
    }

    private QueryResult showTables() {
        QueryResult qr = new QueryResult();
        qr.setColumns(List.of("table"));
        List<List<String>> rows = new ArrayList<>();
        for (Table t : db.getTables()) rows.add(List.of(t.getName()));
        qr.setRows(rows); qr.setMessage(rows.size() + " table(s)");
        return qr;
    }

    private QueryResult describe(Ast.Describe d) {
        Table t = db.getTable(d.table);
        QueryResult qr = new QueryResult();
        qr.setColumns(List.of("column", "type", "pk", "references"));
        List<List<String>> rows = new ArrayList<>();
        for (Column c : t.getColumns())
            rows.add(List.of(c.getName(), c.getType().name(),
                    String.valueOf(c.isPrimaryKey()),
                    c.isForeignKey() ? c.getRefTable() + "." + c.getRefColumn() : ""));
        qr.setRows(rows);
        return qr;
    }

    private List<Column> resolveColumns(Table t, List<String> names) {
        List<Column> out = new ArrayList<>();
        for (String n : names) out.add(t.getColumn(n));
        return out;
    }

    private static Object coerce(Object v, DataType type) {
        if (v == null) return null;
        switch (type) {
            case INT: return v instanceof Number ? ((Number) v).longValue() : Long.parseLong(v.toString());
            case DOUBLE: return v instanceof Number ? ((Number) v).doubleValue() : Double.parseDouble(v.toString());
            case BOOLEAN: return v instanceof Boolean ? v : Boolean.parseBoolean(v.toString());
            case DATE:
                if (v instanceof LocalDate) return v;
                try { return LocalDate.parse(v.toString()); }
                catch (DateTimeParseException e) { throw new DbException("Invalid date: " + v + " (YYYY-MM-DD)"); }
            case STRING: return v.toString();
            default: return v;
        }
    }
    private static String str(Object v) { return v == null ? "NULL" : v.toString(); }
}
