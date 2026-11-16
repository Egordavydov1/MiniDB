package com.minidb.storage;

import com.minidb.DbException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

/** Атомарная запись: write(tmp) → move(tmp → target). Гарантирует, что target не будет частично записан. */
public final class AtomicFiles {
    private AtomicFiles() {}

    public static void writeString(Path target, String content) {
        try {
            Path parent = target.toAbsolutePath().getParent();
            if (parent != null) Files.createDirectories(parent);
            Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
            Files.write(tmp, content.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            try {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new DbException("I/O error writing " + target, e);
        }
    }
    public static String readString(Path p) {
        try {
            return Files.exists(p) ? new String(Files.readAllBytes(p), StandardCharsets.UTF_8) : null;
        } catch (IOException e) { throw new DbException("I/O error reading " + p, e); }
    }
    public static void deleteIfExists(Path p) {
        try { Files.deleteIfExists(p); } catch (IOException ignored) {}
    }
}
