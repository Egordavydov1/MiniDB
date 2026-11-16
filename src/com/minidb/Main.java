package com.minidb;

import com.minidb.cli.Console;
import com.minidb.engine.Database;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Точка входа. Аргумент: каталог данных (по умолчанию data/). */
public class Main {
    public static void main(String[] args) {
        Path dir = Paths.get(args.length > 0 ? args[0] : "data");
        Database db = new Database(dir);
        try { db.load(); }
        catch (RuntimeException e) { System.err.println("Failed to load: " + e.getMessage()); }
        new Console(db).run();
    }
}
