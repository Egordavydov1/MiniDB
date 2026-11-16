package com.minidb.parser;

import com.minidb.DbException;
import com.minidb.engine.*;
import com.minidb.parser.ast.Ast;
import java.util.*;

/** Рекурсивный спуск по SQL-подмножеству. */
public class Parser {
    private final List<Token> t; private int p;
    public Parser(String sql) { t = new Lexer(sql).tokenize(); }

    public Ast.Statement parse() {
        switch (peek().type) {
            case INSERT: return parseInsert();
            case SELECT: return parseSelect();
            case UPDATE: return parseUpdate();
            case DELETE: return parseDelete();
            case CREATE: return parseCreate();
            case DROP: return parseDrop();
            case SHOW: return parseShow();
            case DESCRIBE: return parseDescribe();
            default: throw new DbException("Unexpected token: " + peek());
        }
    }

    private Ast.Statement parseInsert() {
        expect(TokenType.INSERT); expect(TokenType.INTO);
        String table = id();
        List<String> cols = null;
        if (check(TokenType.LPAREN)) {
            expect(TokenType.LPAREN); cols = new ArrayList<>();
            cols.add(id());
            while (check(TokenType.COMMA)) { expect(TokenType.COMMA); cols.add(id()); }
            expect(TokenType.RPAREN);
        }
        expect(TokenType.VALUES);
        List<List<Object>> rows = new ArrayList<>();
        rows.add(tuple());
        while (check(TokenType.COMMA)) { expect(TokenType.COMMA); rows.add(tuple()); }
        semi();
        return new Ast.Insert(table, cols, rows);
    }

    private List<Object> tuple() {
        expect(TokenType.LPAREN);
        List<Object> v = new ArrayList<>();
        v.add(literal());
        while (check(TokenType.COMMA)) { expect(TokenType.COMMA); v.add(literal()); }
        expect(TokenType.RPAREN);
        return v;
    }

    private Object literal() {
        if (check(TokenType.MINUS)) {
            expect(TokenType.MINUS);
            Token n = expect(TokenType.NUMBER);
            // Исправлено: добавлены скобки вокруг отрицательных значений и убраны лишние приведения типов
            return n.text.contains(".") ? (-Double.parseDouble(n.text)) : (-Long.parseLong(n.text));
        }
        Token tok = advance();
        switch (tok.type) {
            case NUMBER:
                // Здесь тоже убрали (Object), Java автоматически упакует в Double или Long
                return tok.text.contains(".") ? Double.parseDouble(tok.text) : Long.parseLong(tok.text);
            case STRING: return tok.text;
            case TRUE: return Boolean.TRUE;
            case FALSE: return Boolean.FALSE;
            case NULL: return null;
            default: throw new DbException("Expected literal, got: " + tok);
        }
    }

    private Ast.Statement parseSelect() {
        expect(TokenType.SELECT);
        List<String> cols;
        if (check(TokenType.STAR)) { expect(TokenType.STAR); cols = null; }
        else { cols = new ArrayList<>(); cols.add(id());
            while (check(TokenType.COMMA)) { expect(TokenType.COMMA); cols.add(id()); } }
        expect(TokenType.FROM);
        String table = id();
        Condition where = null;
        if (check(TokenType.WHERE)) { expect(TokenType.WHERE); where = or(); }
        semi();
        return new Ast.Select(table, cols, where);
    }

    private Ast.Statement parseUpdate() {
        expect(TokenType.UPDATE);
        String table = id(); expect(TokenType.SET);
        Map<String, Object> set = new LinkedHashMap<>();
        do {
            String c = id(); expect(TokenType.OP_EQ); set.put(c, literal());
        } while (check(TokenType.COMMA) && consume(TokenType.COMMA));
        Condition where = null;
        if (check(TokenType.WHERE)) { expect(TokenType.WHERE); where = or(); }
        semi();
        return new Ast.Update(table, set, where);
    }

    private Ast.Statement parseDelete() {
        expect(TokenType.DELETE); expect(TokenType.FROM);
        String table = id();
        Condition where = null;
        if (check(TokenType.WHERE)) { expect(TokenType.WHERE); where = or(); }
        semi();
        return new Ast.Delete(table, where);
    }

    private Ast.Statement parseCreate() {
        expect(TokenType.CREATE);
        if (check(TokenType.TABLE)) return createTable();
        if (check(TokenType.INDEX)) return createIndex();
        throw new DbException("Expected TABLE or INDEX");
    }

    private Ast.Statement createTable() {
        expect(TokenType.TABLE);
        String name = id();
        expect(TokenType.LPAREN);
        List<Column> cols = new ArrayList<>();
        do {
            String cn = id();
            DataType ty = dataType();
            boolean pk = false; String rt = null, rc = null;
            while (true) {
                if (check(TokenType.PRIMARY)) { expect(TokenType.PRIMARY); expect(TokenType.KEY); pk = true; }
                else if (check(TokenType.REFERENCES)) {
                    expect(TokenType.REFERENCES); rt = id();
                    if (check(TokenType.LPAREN)) { expect(TokenType.LPAREN); rc = id(); expect(TokenType.RPAREN); }
                    else rc = "id";
                } else break;
            }
            cols.add(new Column(cn, ty, pk, rt, rc));
        } while (check(TokenType.COMMA) && consume(TokenType.COMMA));
        expect(TokenType.RPAREN);
        semi();
        return new Ast.CreateTable(name, cols);
    }

    private DataType dataType() {
        Token tok = advance();
        switch (tok.type) {
            case INT: return DataType.INT;
            case DOUBLE: return DataType.DOUBLE;
            case VARCHAR: return DataType.STRING;
            case DATE: return DataType.DATE;
            case BOOLEAN_TYPE: return DataType.BOOLEAN;
            default: throw new DbException("Expected type, got: " + tok);
        }
    }

    private Ast.Statement createIndex() {
        expect(TokenType.INDEX);
        String n = id(); expect(TokenType.ON);
        String t = id(); expect(TokenType.LPAREN); String c = id(); expect(TokenType.RPAREN);
        semi();
        return new Ast.CreateIndex(n, t, c);
    }

    private Ast.Statement parseDrop() {
        expect(TokenType.DROP);
        if (check(TokenType.TABLE)) { expect(TokenType.TABLE); String n = id(); semi(); return new Ast.DropTable(n); }
        if (check(TokenType.INDEX)) {
            expect(TokenType.INDEX); String n = id(); String t = null;
            if (check(TokenType.ON)) { expect(TokenType.ON); t = id(); }
            semi(); return new Ast.DropIndex(n, t);
        }
        throw new DbException("Expected TABLE or INDEX");
    }

    private Ast.Statement parseShow() {
        expect(TokenType.SHOW);
        if (!check(TokenType.SEMICOLON) && !check(TokenType.EOF)) advance();
        semi(); return new Ast.ShowTables();
    }

    private Ast.Statement parseDescribe() {
        expect(TokenType.DESCRIBE); String n = id(); semi();
        return new Ast.Describe(n);
    }

    private Condition or() {
        Condition l = and();
        while (check(TokenType.OR)) { expect(TokenType.OR); l = new Logical(l, and(), false); }
        return l;
    }
    private Condition and() {
        Condition l = cmp();
        while (check(TokenType.AND)) { expect(TokenType.AND); l = new Logical(l, cmp(), true); }
        return l;
    }
    private Condition cmp() {
        String c = id(); Token tok = advance();
        Operator op;
        switch (tok.type) {
            case OP_EQ: op = Operator.EQ; break;
            case OP_NEQ: op = Operator.NEQ; break;
            case OP_LT: op = Operator.LT; break;
            case OP_LTE: op = Operator.LTE; break;
            case OP_GT: op = Operator.GT; break;
            case OP_GTE: op = Operator.GTE; break;
            default: throw new DbException("Expected op, got: " + tok);
        }
        return new Comparison(c, op, literal());
    }

    private Token peek() { return t.get(p); }
    private Token advance() { return t.get(p++); }
    private boolean check(TokenType tt) { return peek().type == tt; }
    private boolean consume(TokenType tt) { if (check(tt)) { advance(); return true; } return false; }
    private Token expect(TokenType tt) {
        if (!check(tt)) throw new DbException("Expected " + tt + ", got " + peek());
        return advance();
    }
    private String id() {
        Token tok = advance();
        if (tok.type == TokenType.IDENTIFIER) return tok.text;
        throw new DbException("Expected identifier, got: " + tok);
    }
    private void semi() { if (check(TokenType.SEMICOLON)) advance(); }
}