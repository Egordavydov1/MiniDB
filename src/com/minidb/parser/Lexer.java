package com.minidb.parser;

import com.minidb.DbException;
import java.util.*;

/** Лексер SQL-подмножества. Поддерживает '...'-строки, --комментарии, отрицательные числа. */
public class Lexer {
    private final String src; private int pos;
    public Lexer(String src) { this.src = src; }

    public List<Token> tokenize() {
        List<Token> out = new ArrayList<>();
        while (true) {
            skipSpace();
            if (pos >= src.length()) { out.add(new Token(TokenType.EOF, "")); return out; }
            char c = src.charAt(pos);
            if (Character.isDigit(c)) out.add(readNumber());
            else if (c == '\'') out.add(readString());
            else if (Character.isLetter(c) || c == '_') out.add(readWord());
            else out.add(readSym());
        }
    }

    private void skipSpace() {
        while (pos < src.length()) {
            char c = src.charAt(pos);
            if (Character.isWhitespace(c)) { pos++; continue; }
            if (c == '-' && pos + 1 < src.length() && src.charAt(pos + 1) == '-') {
                while (pos < src.length() && src.charAt(pos) != '\n') pos++;
                continue;
            }
            break;
        }
    }

    private Token readNumber() {
        int st = pos;
        while (pos < src.length() && Character.isDigit(src.charAt(pos))) pos++;
        if (pos < src.length() && src.charAt(pos) == '.') {
            pos++;
            while (pos < src.length() && Character.isDigit(src.charAt(pos))) pos++;
        }
        return new Token(TokenType.NUMBER, src.substring(st, pos));
    }

    private Token readString() {
        pos++;
        StringBuilder sb = new StringBuilder();
        while (pos < src.length()) {
            char c = src.charAt(pos++);
            if (c == '\'') {
                if (pos < src.length() && src.charAt(pos) == '\'') { sb.append('\''); pos++; }
                else break;
            } else if (c == '\\' && pos < src.length()) {
                char n = src.charAt(pos++);
                switch (n) {
                    case 'n': sb.append('\n'); break;
                    case 't': sb.append('\t'); break;
                    case 'r': sb.append('\r'); break;
                    case '\\': sb.append('\\'); break;
                    case '\'': sb.append('\''); break;
                    default: sb.append(n);
                }
            } else sb.append(c);
        }
        return new Token(TokenType.STRING, sb.toString());
    }

    private Token readWord() {
        int st = pos;
        while (pos < src.length() && (Character.isLetterOrDigit(src.charAt(pos)) || src.charAt(pos) == '_')) pos++;
        String w = src.substring(st, pos);
        TokenType kw = TokenType.keywordFor(w);
        return new Token(kw == null ? TokenType.IDENTIFIER : kw, w);
    }

    private Token readSym() {
        char c = src.charAt(pos);
        switch (c) {
            case ',': pos++; return new Token(TokenType.COMMA, ",");
            case '(': pos++; return new Token(TokenType.LPAREN, "(");
            case ')': pos++; return new Token(TokenType.RPAREN, ")");
            case ';': pos++; return new Token(TokenType.SEMICOLON, ";");
            case '*': pos++; return new Token(TokenType.STAR, "*");
            case '-': pos++; return new Token(TokenType.MINUS, "-");
            case '=': pos++; return new Token(TokenType.OP_EQ, "=");
            case '<':
                if (pos + 1 < src.length() && src.charAt(pos + 1) == '=') { pos += 2; return new Token(TokenType.OP_LTE, "<="); }
                if (pos + 1 < src.length() && src.charAt(pos + 1) == '>') { pos += 2; return new Token(TokenType.OP_NEQ, "<>"); }
                pos++; return new Token(TokenType.OP_LT, "<");
            case '>':
                if (pos + 1 < src.length() && src.charAt(pos + 1) == '=') { pos += 2; return new Token(TokenType.OP_GTE, ">="); }
                pos++; return new Token(TokenType.OP_GT, ">");
            case '!':
                if (pos + 1 < src.length() && src.charAt(pos + 1) == '=') { pos += 2; return new Token(TokenType.OP_NEQ, "!="); }
                throw new DbException("Unexpected character: !");
            default: throw new DbException("Unexpected character: " + c);
        }
    }
}