package com.minidb.engine;

import java.util.Set;

/** AND/OR над двумя условиями. */
public class Logical extends Condition {
    private final Condition left, right;
    private final boolean isAnd;
    public Logical(Condition left, Condition right, boolean isAnd) {
        this.left = left; this.right = right; this.isAnd = isAnd;
    }

    @Override public boolean test(Row row) {
        return isAnd ? left.test(row) && right.test(row) : left.test(row) || right.test(row);
    }

    @Override public Set<Long> tryIndex(Table table) {
        Set<Long> l = left.tryIndex(table);
        Set<Long> r = right.tryIndex(table);
        if (isAnd) {
            if (l == null && r == null) return null;
            if (l == null) return r;
            if (r == null) return l;
            l.retainAll(r);
            return l;
        } else {
            if (l == null || r == null) return null;
            l.addAll(r);
            return l;
        }
    }
}