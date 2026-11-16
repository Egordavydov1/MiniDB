package com.minidb.cli;

import com.minidb.DbException;
import com.minidb.engine.Database;
import com.minidb.engine.QueryExecutor;
import com.minidb.engine.QueryResult;
import com.minidb.parser.Parser;
import com.minidb.parser.ast.Ast;
import java.io.*;
import java.util.List;

/** Консольный REPL. Позволяет вводить SQL до ';', многострочно. */
public class Console {
    private final QueryExecutor exec;
    public Console(Database db) { this.exec = new QueryExecutor(db); }

    public void run() {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("MiniDB v1.0. HELP for commands, EXIT to quit.");
        StringBuilder buf = new StringBuilder();
        try {
            while (true) {
                System.out.print(buf.length() == 0 ? "minidb> " : "     -> ");
                String line = br.readLine();
                if (line == null) break;
                String tr = line.trim();
                if (buf.length() == 0) {
                    if (tr.equalsIgnoreCase("exit") || tr.equalsIgnoreCase("quit") || tr.equals("\\q")) break;
                    if (tr.equalsIgnoreCase("help") || tr.equals("\\h")) { help(); continue; }
                    if (tr.isEmpty()) continue;
                }
                buf.append(line).append('\n');
                if (tr.endsWith(";")) {
                    String sql = buf.toString();
                    buf.setLength(0);
                    handle(sql);
                }
            }
        } catch (IOException e) { System.err.println("I/O: " + e.getMessage()); }
        System.out.println("Bye.");
    }

    private void handle(String sql) {
        try {
            Ast.Statement st = new Parser(sql).parse();
            QueryResult r = exec.execute(st);
            if (r.isTabular()) printTable(r);
            if (r.getMessage() != null && !r.getMessage().isEmpty())
                System.out.println("OK: " + r.getMessage() + " (" + r.getElapsedMs() + " ms)");
        } catch (DbException | IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    private void printTable(QueryResult r) {
        List<String> cols = r.getColumns();
        List<List<String>> rows = r.getRows();
        int[] w = new int[cols.size()];
        for (int i = 0; i < cols.size(); i++) w[i] = cols.get(i).length();
        for (List<String> row : rows)
            for (int i = 0; i < cols.size(); i++)
                w[i] = Math.max(w[i], (i < row.size() ? row.get(i) : "").length());
        StringBuilder sep = new StringBuilder("+");
        for (int width : w) { for (int k = 0; k < width + 2; k++) sep.append('-'); sep.append('+'); }
        System.out.println(sep);
        System.out.print("|");
        for (int i = 0; i < cols.size(); i++) System.out.printf(" %-" + w[i] + "s |", cols.get(i));
        System.out.println();
        System.out.println(sep);
        for (List<String> row : rows) {
            System.out.print("|");
            for (int i = 0; i < cols.size(); i++)
                System.out.printf(" %-" + w[i] + "s |", i < row.size() ? row.get(i) : "");
            System.out.println();
        }
        System.out.println(sep);
    }

    private void help() {
        System.out.println(
                "DDL:  CREATE TABLE t (id INT PRIMARY KEY, name VARCHAR, ...);\n" +
                        "      DROP TABLE t;  CREATE INDEX idx ON t(col);  DROP INDEX idx ON t;\n" +
                        "DML:  INSERT INTO t VALUES (...), (...);\n" +
                        "      INSERT INTO t (a,b) VALUES (1,'x');\n" +
                        "      UPDATE t SET a = 1 WHERE id = 2;\n" +
                        "      DELETE FROM t WHERE id = 3;\n" +
                        "DQL:  SELECT * FROM t [WHERE cond AND/OR cond];\n" +
                        "      SELECT a,b FROM t WHERE a > 5;\n" +
                        "META: SHOW TABLES;  DESCRIBE t;  HELP;  EXIT;");
    }
}