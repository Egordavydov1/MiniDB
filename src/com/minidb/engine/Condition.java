package com.minidb.engine;

import java.util.Set;

/** Дерево условий. tryIndex возвращает набор rowid-кандидатов или null, если индекс применить нельзя. */
public abstract class Condition {
    public abstract boolean test(Row row);
    public abstract Set<Long> tryIndex(Table table);
}
