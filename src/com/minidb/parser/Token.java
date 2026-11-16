package com.minidb.parser;
public class Token {
    public final TokenType type; public final String text;
    public Token(TokenType t, String s) { type = t; text = s; }
    @Override public String toString() { return type + "(" + text + ")"; }
}