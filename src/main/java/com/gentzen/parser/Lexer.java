package com.gentzen.parser;

import java.util.*;

/**
 * 词法分析器：将输入字符串转换为 Token 列表。
 *
 * <p>Token 类型：
 * <ul>
 *   <li>VAR — 非负整数（原子命题）</li>
 *   <li>NOT — ~</li>
 *   <li>AND — &</li>
 *   <li>OR  — |</li>
 *   <li>IMPL — -></li>
 *   <li>COMMA — ,</li>
 *   <li>TURNSTILE — |-</li>
 *   <li>LPAREN — (</li>
 *   <li>RPAREN — )</li>
 * </ul>
 */
public class Lexer {

    public enum TokenType {
        VAR,        // 非负整数
        NOT,        // ~
        AND,        // &
        OR,         // |
        IMPL,       // ->
        COMMA,      // ,
        TURNSTILE,  // |-
        LPAREN,     // (
        RPAREN,     // )
        EOF
    }

    public record Token(TokenType type, int value) {
        public Token(TokenType type) {
            this(type, -1);
        }

        @Override
        public String toString() {
            if (type == TokenType.VAR) {
                return "VAR(" + value + ")";
            }
            return type.name();
        }
    }

    private final String input;
    private int pos;

    public Lexer(String input) {
        this.input = input;
        this.pos = 0;
    }

    /**
     * 将整个输入转换为 Token 列表（不含 EOF）。
     */
    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();
        Token t;
        while ((t = nextToken()).type() != TokenType.EOF) {
            tokens.add(t);
        }
        return tokens;
    }

    /**
     * 获取下一个 Token。
     */
    public Token nextToken() {
        skipWhitespace();
        if (pos >= input.length()) {
            return new Token(TokenType.EOF);
        }

        char c = input.charAt(pos);

        // 非负整数
        if (Character.isDigit(c)) {
            int start = pos;
            while (pos < input.length() && Character.isDigit(input.charAt(pos))) {
                pos++;
            }
            int value = Integer.parseInt(input.substring(start, pos));
            return new Token(TokenType.VAR, value);
        }

        // 双字符操作符: ->
        if (c == '-' && pos + 1 < input.length() && input.charAt(pos + 1) == '>') {
            pos += 2;
            return new Token(TokenType.IMPL);
        }

        // 双字符操作符: |-
        if (c == '|' && pos + 1 < input.length() && input.charAt(pos + 1) == '-') {
            pos += 2;
            return new Token(TokenType.TURNSTILE);
        }

        // 单字符 Token
        pos++;
        return switch (c) {
            case '~' -> new Token(TokenType.NOT);
            case '&' -> new Token(TokenType.AND);
            case '|' -> new Token(TokenType.OR);
            case ',' -> new Token(TokenType.COMMA);
            case '(' -> new Token(TokenType.LPAREN);
            case ')' -> new Token(TokenType.RPAREN);
            default -> throw new IllegalArgumentException(
                    "非法字符 '" + c + "' 在位置 " + (pos - 1));
        };
    }

    private void skipWhitespace() {
        while (pos < input.length() && Character.isWhitespace(input.charAt(pos))) {
            pos++;
        }
    }
}
