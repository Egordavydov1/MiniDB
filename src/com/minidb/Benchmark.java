package com.minidb;

import com.minidb.engine.*;
import com.minidb.parser.Parser;
import java.nio.file.*;

/**
 * Мини-бенчмарк: 5000 строк, 100 брендов; сравнение SELECT WHERE brand = 'x'
 * с индексом и без.
 *
 * Запуск: java -cp out com.minidb.Benchmark
 */
public class Benchmark {
    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("minidb-bench");
        Database db = new Database(dir);
        db.load();

        QueryExecutor exec = new QueryExecutor(db);
        exec.setAutoSave(false);
        exec.execute(new Parser("CREATE TABLE cars (id INT PRIMARY KEY, brand VARCHAR, price DOUBLE);").parse());

        int N = 5000;
        System.out.println("Inserting " + N + " rows...");
        long t0 = System.currentTimeMillis();
        for (int i = 0; i < N; i++) {
            String sql = "INSERT INTO cars VALUES (" + i + ", 'brand" + (i % 100) + "', " + (i * 1.5) + ");";
            exec.execute(new Parser(sql).parse());
        }
        System.out.println("Insert time: " + (System.currentTimeMillis() - t0) + " ms");

        int Q = 1000;
        long total;

        System.out.println("\n-- Without index (1000 lookups by brand) --");
        t0 = System.nanoTime(); total = 0;
        for (int i = 0; i < Q; i++) {
            total += exec.execute(new Parser("SELECT * FROM cars WHERE brand = 'brand" + (i % 100) + "';").parse()).getRows().size();
        }
        long t1 = System.nanoTime();
        System.out.printf("Total: %.2f ms, rows returned: %d%n", (t1 - t0) / 1e6, total);

        exec.execute(new Parser("CREATE INDEX idx_brand ON cars (brand);").parse());

        System.out.println("\n-- With index (1000 lookups by brand) --");
        t0 = System.nanoTime(); total = 0;
        for (int i = 0; i < Q; i++) {
            total += exec.execute(new Parser("SELECT * FROM cars WHERE brand = 'brand" + (i % 100) + "';").parse()).getRows().size();
        }
        t1 = System.nanoTime();
        System.out.printf("Total: %.2f ms, rows returned: %d%n", (t1 - t0) / 1e6, total);
    }
}