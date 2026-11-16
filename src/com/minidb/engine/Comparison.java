package com.minidb.engine;

import java.time.LocalDate;
import java.util.Set;

/** Листовое сравнение: column OP value. */
public class Comparison extends Condition {
    private final String column;
    private final Operator op;
    private final Object value;

    public Comparison(String column, Operator op, Object value) {
        this.column = column; this.op = op; this.value = value;
    }

    @Override
    public boolean test(Row row) {
        Object v = row.get(column);
        if (v == null && value == null) return op == Operator.EQ;
        if (v == null || value == null) return op == Operator.NEQ;
        int cmp = compare(v, value);
        switch (op) {
            case EQ: return cmp == 0;
            case NEQ: return cmp != 0;
            case LT: return cmp < 0;
            case LTE: return cmp <= 0;
            case GT: return cmp > 0;
            case GTE: return cmp >= 0;
            default: return false;
        }
    }

    @Override
    public Set<Long> tryIndex(Table table) {
        if (op != Operator.EQ) return null;
        HashIndex idx = table.getIndexForColumn(column);
        if (idx == null) return null;
        Column c = table.getColumn(column);
        return idx.lookup(coerce(value, c.getType()));
    }

    private static Object coerce(Object v, DataType t) {
        if (v == null) return null;
        switch (t) {
            case INT: return v instanceof Number ? ((Number) v).longValue() : Long.parseLong(v.toString());
            case DOUBLE: return v instanceof Number ? ((Number) v).doubleValue() : Double.parseDouble(v.toString());
            case BOOLEAN: return v instanceof Boolean ? v : Boolean.parseBoolean(v.toString());
            case DATE: return v instanceof LocalDate ? v : LocalDate.parse(v.toString());
            case STRING: return v.toString();
            default: return v;
        }
    }

    @SuppressWarnings({"unchecked","rawtypes"})
    private static int compare(Object a, Object b) {
        if (a instanceof Number && b instanceof Number)
            return Double.compare(((Number) a).doubleValue(), ((Number) b).doubleValue());
        if (a instanceof LocalDate && b instanceof String)
            return ((LocalDate) a).compareTo(LocalDate.parse((String) b));
        if (a instanceof Boolean && b instanceof String)
            return Boolean.compare((Boolean) a, Boolean.parseBoolean((String) b));
        if (a instanceof Comparable && b.getClass().equals(a.getClass()))
            return ((Comparable) a).compareTo(b);
        return String.valueOf(a).compareTo(String.valueOf(b));
    }
}