package com.minidb.engine;

import java.util.ArrayList;
import java.util.List;

/** Результат выполнения запроса — табличный или текстовый. */
public class QueryResult {
    private List<String> columns = new ArrayList<>();
    private List<List<String>> rows = new ArrayList<>();
    private String message = "";
    private long elapsedMs;

    public static QueryResult message(String m) {
        QueryResult r = new QueryResult();
        r.setMessage(m);
        return r;
    }
    public boolean isTabular() { return !columns.isEmpty(); }
    public List<String> getColumns() { return columns; }
    public void setColumns(List<String> c) { this.columns = c; }
    public List<List<String>> getRows() { return rows; }
    public void setRows(List<List<String>> r) { this.rows = r; }
    public String getMessage() { return message; }
    public void setMessage(String m) { this.message = m; }
    public long getElapsedMs() { return elapsedMs; }
    public void setElapsedMs(long ms) { this.elapsedMs = ms; }
}