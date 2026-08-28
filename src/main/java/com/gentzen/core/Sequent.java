package com.gentzen.core;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 矢列 Γ ⊢ Δ，其中 Γ 为前提公式列表（左侧），Δ 为结论公式列表（右侧）。
 * 两个列表均为不可变列表。
 *
 * <p>G' 系统的公理规则要求：存在同一个原子命题同时出现在 Γ 和 Δ 中。
 */
public record Sequent(List<Prop> left, List<Prop> right) {

    public Sequent(List<Prop> left, List<Prop> right) {
        this.left = Collections.unmodifiableList(new ArrayList<>(left));
        this.right = Collections.unmodifiableList(new ArrayList<>(right));
    }

    /**
     * 判断当前矢列是否为公理。
     * G' 系统公理：存在某个原子命题 A 同时直接出现在 Γ 和 Δ 中。
     * 注意：必须是直接的原子命题 (Prop.Var)，不能递归地从复合公式中提取。
     */
    public boolean isAxiom() {
        Set<Integer> leftAtomIds = new HashSet<>();
        for (Prop p : left) {
            if (p instanceof Prop.Var(int id)) {
                leftAtomIds.add(id);
            }
        }
        for (Prop p : right) {
            if (p instanceof Prop.Var(int id) && leftAtomIds.contains(id)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 返回左侧第一个非原子公式的索引，若全为原子公式则返回 -1。
     */
    public int firstCompoundLeft() {
        for (int i = 0; i < left.size(); i++) {
            if (!left.get(i).isAtomic()) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 返回右侧第一个非原子公式的索引，若全为原子公式则返回 -1。
     */
    public int firstCompoundRight() {
        for (int i = 0; i < right.size(); i++) {
            if (!right.get(i).isAtomic()) {
                return i;
            }
        }
        return -1;
    }

    // ──────────────── 规则应用辅助方法 ────────────────

    /**
     * 用新公式替换左侧指定位置的公式，返回新矢列。
     */
    public Sequent replaceLeft(int index, List<Prop> newFormulas) {
        List<Prop> newLeft = new ArrayList<>(left);
        newLeft.remove(index);
        newLeft.addAll(index, newFormulas);
        return new Sequent(newLeft, this.right);
    }

    /**
     * 删除左侧指定位置的公式，返回新矢列。
     */
    public Sequent removeLeft(int index) {
        List<Prop> newLeft = new ArrayList<>(left);
        newLeft.remove(index);
        return new Sequent(newLeft, this.right);
    }

    /**
     * 用新公式替换右侧指定位置的公式，返回新矢列。
     */
    public Sequent replaceRight(int index, List<Prop> newFormulas) {
        List<Prop> newRight = new ArrayList<>(right);
        newRight.remove(index);
        newRight.addAll(index, newFormulas);
        return new Sequent(this.left, newRight);
    }

    /**
     * 删除右侧指定位置的公式，返回新矢列。
     */
    public Sequent removeRight(int index) {
        List<Prop> newRight = new ArrayList<>(right);
        newRight.remove(index);
        return new Sequent(this.left, newRight);
    }

    /**
     * 在左侧末尾添加公式，返回新矢列。
     */
    public Sequent addLeft(Prop p) {
        List<Prop> newLeft = new ArrayList<>(left);
        newLeft.add(p);
        return new Sequent(newLeft, this.right);
    }

    /**
     * 在右侧末尾添加公式，返回新矢列。
     */
    public Sequent addRight(Prop p) {
        List<Prop> newRight = new ArrayList<>(right);
        newRight.add(p);
        return new Sequent(this.left, newRight);
    }

    /**
     * 收集矢列中出现的所有命题变量 id。
     */
    public Set<Integer> allVarIds() {
        Set<Integer> ids = new HashSet<>();
        for (Prop p : left)  p.collectVarIds(ids);
        for (Prop p : right) p.collectVarIds(ids);
        return ids;
    }

    @Override
    public String toString() {
        String lhs = left.stream().map(Prop::toString).collect(Collectors.joining(", "));
        String rhs = right.stream().map(Prop::toString).collect(Collectors.joining(", "));
        if (lhs.isEmpty()) {
            return "|- " + rhs;
        }
        if (rhs.isEmpty()) {
            return lhs + " |-";
        }
        return lhs + " |- " + rhs;
    }

    /**
     * 用新左右列表创建一个新矢列（保留不可变性）。
     */
    public static Sequent of(List<Prop> left, List<Prop> right) {
        return new Sequent(left, right);
    }
}
