package com.gentzen.parser;

import com.gentzen.core.*;
import com.gentzen.parser.Lexer.Token;
import com.gentzen.parser.Lexer.TokenType;
import java.util.*;

/**
 * 递归下降解析器：将 Token 序列解析为 Sequent（矢列）。
 *
 * <p>文法（EBNF）：
 * <pre>
 * sequent     ::= formula_list TURNSTILE formula_list
 * formula_list ::= [ formula (COMMA formula)* ]
 * formula     ::= implication
 * implication ::= disjunction (IMPL implication)?
 * disjunction ::= conjunction (OR conjunction)*
 * conjunction ::= negation (AND negation)*
 * negation    ::= NOT negation | atom
 * atom        ::= VAR | LPAREN formula RPAREN
 * </pre>
 *
 * <p>优先级（从低到高）：→ , | , & , ~
 * <p>→ 是右结合的，其余二元运算符是左结合的。
 */
public class Parser {

    private final List<Token> tokens;
    private int pos;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
        this.pos = 0;
    }

    // ──────────────── 公开接口 ────────────────

    /**
     * 解析矢列字符串。例如 "0 -> 1, 0 |- 1"。
     */
    public static Sequent parseSequent(String input) {
        Lexer lexer = new Lexer(input);
        List<Token> tokens = lexer.tokenize();
        Parser parser = new Parser(tokens);
        Sequent seq = parser.parseSequent();
        // 确保已消费全部 Token
        if (parser.pos < tokens.size()) {
            throw new IllegalArgumentException(
                    "多余的 Token: " + tokens.get(parser.pos) + " (位置 " + parser.pos + ")");
        }
        return seq;
    }

    /**
     * 解析单个命题公式字符串。例如 "~(0 | 1)"。
     */
    public static Prop parseFormula(String input) {
        Lexer lexer = new Lexer(input);
        List<Token> tokens = lexer.tokenize();
        Parser parser = new Parser(tokens);
        Prop formula = parser.parseFormula();
        if (parser.pos < tokens.size()) {
            throw new IllegalArgumentException(
                    "多余的 Token: " + tokens.get(parser.pos));
        }
        return formula;
    }

    // ──────────────── 内部解析方法 ────────────────

    /** sequent ::= formula_list TURNSTILE formula_list */
    private Sequent parseSequent() {
        // 解析左侧公式列表（可以为空）
        List<Prop> left = parseFormulaList();
        // 必须遇到 |-
        if (!match(TokenType.TURNSTILE)) {
            throw new IllegalArgumentException("期望 '|-', 但遇到: " + current());
        }
        // 解析右侧公式列表（可以为空）
        List<Prop> right = parseFormulaList();
        return new Sequent(left, right);
    }

    /** formula_list ::= [ formula (COMMA formula)* ] */
    private List<Prop> parseFormulaList() {
        List<Prop> formulas = new ArrayList<>();
        // 检查是否直接遇到 |- 或 EOF（空列表）
        if (check(TokenType.TURNSTILE) || check(TokenType.EOF)) {
            return formulas;
        }
        formulas.add(parseFormula());
        while (match(TokenType.COMMA)) {
            // 逗号后面不允许直接是 |- 或 EOF
            if (check(TokenType.TURNSTILE) || check(TokenType.EOF)) {
                throw new IllegalArgumentException("逗号后缺少公式");
            }
            formulas.add(parseFormula());
        }
        return formulas;
    }

    /** formula ::= implication */
    private Prop parseFormula() {
        return parseImplication();
    }

    /** implication ::= disjunction (IMPL implication)? */
    private Prop parseImplication() {
        Prop left = parseDisjunction();
        if (match(TokenType.IMPL)) {
            Prop right = parseImplication();  // 右结合
            return new Prop.Impl(left, right);
        }
        return left;
    }

    /** disjunction ::= conjunction (OR conjunction)* */
    private Prop parseDisjunction() {
        Prop left = parseConjunction();
        while (match(TokenType.OR)) {
            Prop right = parseConjunction();
            left = new Prop.Or(left, right);
        }
        return left;
    }

    /** conjunction ::= negation (AND negation)* */
    private Prop parseConjunction() {
        Prop left = parseNegation();
        while (match(TokenType.AND)) {
            Prop right = parseNegation();
            left = new Prop.And(left, right);
        }
        return left;
    }

    /** negation ::= NOT negation | atom */
    private Prop parseNegation() {
        if (match(TokenType.NOT)) {
            Prop sub = parseNegation();  // 允许 ~~A
            return new Prop.Not(sub);
        }
        return parseAtom();
    }

    /** atom ::= VAR | LPAREN formula RPAREN */
    private Prop parseAtom() {
        if (match(TokenType.VAR)) {
            int id = previous().value();
            return new Prop.Var(id);
        }
        if (match(TokenType.LPAREN)) {
            Prop inner = parseFormula();
            if (!match(TokenType.RPAREN)) {
                throw new IllegalArgumentException("期望 ')', 但遇到: " + current());
            }
            return inner;
        }
        throw new IllegalArgumentException("期望变量或 '(', 但遇到: " + current());
    }

    // ──────────────── Token 辅助方法 ────────────────

    private Token current() {
        if (pos < tokens.size()) {
            return tokens.get(pos);
        }
        return new Token(TokenType.EOF);
    }

    private Token previous() {
        return tokens.get(pos - 1);
    }

    private boolean check(TokenType type) {
        return current().type() == type;
    }

    /** 如果当前 Token 匹配期望类型，消费并返回 true；否则返回 false。 */
    private boolean match(TokenType type) {
        if (check(type)) {
            pos++;
            return true;
        }
        return false;
    }
}
