package com.minidb.storage;

import com.minidb.DbException;
import com.minidb.engine.*;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/** Файловый слой: *.schema, *.data, <table>.<index>.idx. */
public class StorageManager {
    private final Path dataDir;

    public StorageManager(Path dataDir) {
        this.dataDir = dataDir;
        try { Files.createDirectories(dataDir); }
        catch (IOException e) { throw new DbException("Cannot create dir " + dataDir, e); }
    }

    public List<Table> loadAllTables() {
        List<Table> out = new ArrayList<>();
        if (!Files.exists(dataDir)) return out;
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dataDir, "*.schema")) {
            for (Path p : ds) {
                String fn = p.getFileName().toString();
                out.add(loadTable(fn.substring(0, fn.length() - 7)));
            }
        } catch (IOException e) { throw new DbException("Cannot list tables", e); }
        return out;
    }

    public Table loadTable(String name) {
        String schemaText = AtomicFiles.readString(dataDir.resolve(name + ".schema"));
        if (schemaText == null) throw new DbException("Schema not found for " + name);

        Table t = new Table(name);
        for (String line : schemaText.split("\n")) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            String[] parts = line.split("\\|", -1);
            if (parts.length < 2) continue;
            boolean pk = false; String refT = null, refC = null;
            for (int i = 2; i < parts.length; i++) {
                String f = parts[i].trim();
                if (f.equals("PK")) pk = true;
                else if (f.startsWith("REF:")) {
                    String[] r = f.substring(4).split("\\.");
                    refT = r[0]; refC = r.length > 1 ? r[1] : "id";
                }
            }
            t.addColumn(new Column(parts[0], DataType.fromSql(parts[1]), pk, refT, refC));
        }

        String dataText = AtomicFiles.readString(dataDir.resolve(name + ".data"));
        if (dataText != null) for (String line : dataText.split("\n")) {
            if (line.isEmpty()) continue;
            String[] parts = line.split("\t", -1);
            Row r = new Row(t.allocateRowid());
            for (int i = 0; i < t.getColumns().size(); i++) {
                Column c = t.getColumns().get(i);
                String raw = i < parts.length ? parts[i] : "\\N";
                r.put(c.getName(), decode(raw, c.getType()));
            }
            t.addRow(r);   // сразу попадёт в pkMap
        }

        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dataDir, name + ".*.idx")) {
            for (Path p : ds) {
                String fn = p.getFileName().toString();
                String idxName = fn.substring(name.length() + 1, fn.length() - 4);
                loadIndex(t, idxName, p);
            }
        } catch (IOException ignored) {}
        return t;
    }

    private void loadIndex(Table t, String idxName, Path p) {
        try {
            List<String> lines = Files.readAllLines(p);
            if (lines.isEmpty() || !lines.get(0).startsWith("#")) return;
            String[] h = lines.get(0).substring(1).trim().split("\\|");
            if (h.length < 3) return;
            Column c = t.getColumn(h[2]);
            HashIndex idx = new HashIndex(idxName, t.getName(), c.getName());
            for (int i = 1; i < lines.size(); i++) {
                String[] kv = lines.get(i).split("\t", 2);
                if (kv.length < 2) continue;
                Object v = decode(kv[0], c.getType());
                for (String id : kv[1].split(",")) if (!id.isEmpty()) idx.add(v, Long.parseLong(id));
            }
            t.getIndexes().put(idxName, idx);
        } catch (Exception e) {
            System.err.println("Warning: index " + p + " is corrupt, will be rebuilt: " + e.getMessage());
        }
    }

    public void saveAll(Collection<Table> tables) { for (Table t : tables) saveTable(t); }

    public void saveTable(Table t) {
        StringBuilder sb = new StringBuilder();
        for (Column c : t.getColumns()) {
            sb.append(c.getName()).append('|').append(c.getType().name());
            if (c.isPrimaryKey()) sb.append("|PK");
            if (c.isForeignKey()) sb.append("|REF:").append(c.getRefTable()).append('.').append(c.getRefColumn());
            sb.append('\n');
        }
        AtomicFiles.writeString(dataDir.resolve(t.getName() + ".schema"), sb.toString());

        StringBuilder db = new StringBuilder();
        for (Row r : t.getRows()) {
            for (int i = 0; i < t.getColumns().size(); i++) {
                if (i > 0) db.append('\t');
                db.append(encode(r.get(t.getColumns().get(i).getName())));
            }
            db.append('\n');
        }
        AtomicFiles.writeString(dataDir.resolve(t.getName() + ".data"), db.toString());

        for (HashIndex idx : t.getIndexes().values()) {
            StringBuilder ib = new StringBuilder();
            ib.append('#').append(idx.getName()).append('|').append(t.getName()).append('|').append(idx.getColumn()).append('\n');
            for (Map.Entry<Object, Set<Long>> e : idx.getMap().entrySet()) {
                ib.append(encode(e.getKey())).append('\t')
                        .append(e.getValue().stream().map(String::valueOf).sorted().collect(Collectors.joining(",")))
                        .append('\n');
            }
            for (Long id : idx.getNulls()) ib.append("\\N\t").append(id).append('\n');
            AtomicFiles.writeString(dataDir.resolve(t.getName() + "." + idx.getName() + ".idx"), ib.toString());
        }
    }

    public void deleteTable(String name) {
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dataDir, name + ".*")) {
            for (Path p : ds) Files.deleteIfExists(p);
        } catch (IOException ignored) {}
    }

    public void deleteIndexFile(String table, String index) {
        AtomicFiles.deleteIfExists(dataDir.resolve(table + "." + index + ".idx"));
    }

    private static String encode(Object v) {
        if (v == null) return "\\N";
        if (v instanceof Boolean) return ((Boolean) v) ? "true" : "false";
        if (v instanceof LocalDate) return v.toString();
        StringBuilder sb = new StringBuilder();
        for (char c : v.toString().toCharArray()) {
            switch (c) {
                case '\\': sb.append("\\\\"); break;
                case '\t': sb.append("\\t"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }

    private static Object decode(String s, DataType type) {
        if (s.equals("\\N")) return null;
        switch (type) {
            case INT: return Long.parseLong(s);
            case DOUBLE: return Double.parseDouble(s);
            case BOOLEAN: return Boolean.parseBoolean(s);
            case DATE: return LocalDate.parse(s);
            case STRING: {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < s.length(); i++) {
                    char c = s.charAt(i);
                    if (c == '\\' && i + 1 < s.length()) {
                        char n = s.charAt(++i);
                        switch (n) {
                            case 't': sb.append('\t'); break;
                            case 'n': sb.append('\n'); break;
                            case 'r': sb.append('\r'); break;
                            case '\\': sb.append('\\'); break;
                            default: sb.append(n);
                        }
                    } else sb.append(c);
                }
                return sb.toString();
            }
            default: return s;
        }
    }
}
