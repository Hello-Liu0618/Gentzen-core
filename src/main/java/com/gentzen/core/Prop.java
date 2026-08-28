package com.gentzen.core;

import java.util.*;

/**
 * 命题逻辑公式的代数数据类型 (Algebraic Data Type)。
 * 使用 Java sealed interface + record 模拟 inductive types。
 *
 * <p>支持的构造子：
 * <ul>
 *   <li>Var(int id) — 原子命题（命题变量），id 为非负整数</li>
 *   <li>Not(Prop sub) — 否定 ¬</li>
 *   <li>And(Prop left, Prop right) — 合取 ∧</li>
 *   <li>Or(Prop left, Prop right) — 析取 ∨</li>
 *   <li>Impl(Prop left, Prop right) — 蕴含 →</li>
 * </ul>
 */
public sealed interface Prop {
    /** 原子命题（变量），id 为非负整数，如 0 表示 P₀ */
    record Var(int id) implements Prop {
        @Override
        public String toString() {
            return String.valueOf(id);
        }
    }

    /** 否定 ¬A */
    record Not(Prop sub) implements Prop {
        @Override
        public String toString() {
            return "~" + (sub instanceof Var || sub instanceof Not
                    ? sub.toString()
                    : "(" + sub + ")");
        }
    }

    /** 合取 A ∧ B */
    record And(Prop left, Prop right) implements Prop {
        @Override
        public String toString() {
            String l = left instanceof Var || left instanceof Not || left instanceof And
                    ? left.toString() : "(" + left + ")";
            String r = right instanceof Var || right instanceof Not || right instanceof And
                    ? right.toString() : "(" + right + ")";
            return l + " & " + r;
        }
    }

    /** 析取 A ∨ B */
    record Or(Prop left, Prop right) implements Prop {
        @Override
        public String toString() {
            String l = left instanceof Var || left instanceof Not || left instanceof And || left instanceof Or
                    ? left.toString() : "(" + left + ")";
            String r = right instanceof Var || right instanceof Not || right instanceof And || right instanceof Or
                    ? right.toString() : "(" + right + ")";
            return l + " | " + r;
        }
    }

    /** 蕴含 A → B（右结合） */
    record Impl(Prop left, Prop right) implements Prop {
        @Override
        public String toString() {
            // → 是右结合的，所以 left 若是 Impl 需要加括号
            // 如 (A -> B) -> C 不能写成 A -> B -> C
            String l = (left instanceof Var || left instanceof Not || left instanceof And
                    || left instanceof Or)
                    ? left.toString() : "(" + left + ")";
            // right 若是 Impl 不需要括号（右结合）
            String r = (right instanceof Var || right instanceof Not || right instanceof Impl)
                    ? right.toString() : "(" + right + ")";
            return l + " -> " + r;
        }
    }

    /**
     * 判断公式是否为原子命题（变量）。
     * 用于 axiom 检查——只有原子命题可以直接构成公理。
     */
    default boolean isAtomic() {
        return this instanceof Var;
    }

    /**
     * 递归收集公式中出现的所有命题变量 id。
     */
    default void collectVarIds(Set<Integer> out) {
        switch (this) {
            case Var(int id) -> out.add(id);
            case Not(Prop sub) -> sub.collectVarIds(out);
            case And(Prop l, Prop r) -> { l.collectVarIds(out); r.collectVarIds(out); }
            case Or(Prop l, Prop r)  -> { l.collectVarIds(out); r.collectVarIds(out); }
            case Impl(Prop l, Prop r) -> { l.collectVarIds(out); r.collectVarIds(out); }
        }
    }
}
