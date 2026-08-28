package com.gentzen.core;

/**
 * 证明树——G' 矢列演算的证明数据结构。
 *
 * <p>每个节点携带当前矢列以及作为主公式（被规则作用的公式）的索引。
 * 对于 Ax 节点，还携带匹配的原子命题 id。
 *
 * <p>对应 G' 系统规则（不含 Cut）：
 * <ul>
 *   <li>Ax — 公理</li>
 *   <li>NotL, NotR, AndL, OrR, ImplyR — 单分支规则</li>
 *   <li>AndR, OrL, ImplyL — 双分支规则</li>
 * </ul>
 */
public sealed interface ProofTree {

    /** 公理节点：存在原子命题 varId 同时在左右两侧出现 */
    record Ax(Sequent sequent, int varId) implements ProofTree {}

    /** ¬L: Γ, ¬A ⊢ Δ  →  Γ ⊢ A, Δ */
    record NotL(Sequent sequent, int formulaIndex, ProofTree child) implements ProofTree {}

    /** ¬R: Γ ⊢ ¬A, Δ  →  Γ, A ⊢ Δ */
    record NotR(Sequent sequent, int formulaIndex, ProofTree child) implements ProofTree {}

    /** ∧L: Γ, A∧B ⊢ Δ  →  Γ, A, B ⊢ Δ */
    record AndL(Sequent sequent, int formulaIndex, ProofTree child) implements ProofTree {}

    /** ∨R: Γ ⊢ A∨B, Δ  →  Γ ⊢ A, B, Δ */
    record OrR(Sequent sequent, int formulaIndex, ProofTree child) implements ProofTree {}

    /** →R: Γ ⊢ A→B, Δ  →  Γ, A ⊢ B, Δ */
    record ImplyR(Sequent sequent, int formulaIndex, ProofTree child) implements ProofTree {}

    /** ∧R: Γ ⊢ A∧B, Δ  →  Γ ⊢ A, Δ  和  Γ ⊢ B, Δ */
    record AndR(Sequent sequent, int formulaIndex, ProofTree left, ProofTree right) implements ProofTree {}

    /** ∨L: Γ, A∨B ⊢ Δ  →  Γ, A ⊢ Δ  和  Γ, B ⊢ Δ */
    record OrL(Sequent sequent, int formulaIndex, ProofTree left, ProofTree right) implements ProofTree {}

    /** →L: Γ, A→B ⊢ Δ  →  Γ ⊢ A, Δ  和  Γ, B ⊢ Δ */
    record ImplyL(Sequent sequent, int formulaIndex, ProofTree left, ProofTree right) implements ProofTree {}

    // ──────────────── 格式化输出 ────────────────

    /**
     * 将证明树格式化为可读的缩进文本。
     */
    default String toPrettyString() {
        StringBuilder sb = new StringBuilder();
        formatTree(sb, "", true);
        return sb.toString();
    }

    /** 递归格式化证明树 */
    private void formatTree(StringBuilder sb, String indent, boolean last) {
        // 根据节点类型获取规则名称和子节点
        switch (this) {
            case Ax(Sequent seq, int varId) -> {
                sb.append(indent).append("(Ax on ").append(varId).append(")  ")
                        .append(seq).append("\n");
            }
            case NotL(Sequent seq, int idx, ProofTree child) -> {
                sb.append(indent).append("(¬L[").append(idx).append("])  ")
                        .append(seq).append("\n");
                child.formatTree(sb, indent + "  ", true);
            }
            case NotR(Sequent seq, int idx, ProofTree child) -> {
                sb.append(indent).append("(¬R[").append(idx).append("])  ")
                        .append(seq).append("\n");
                child.formatTree(sb, indent + "  ", true);
            }
            case AndL(Sequent seq, int idx, ProofTree child) -> {
                sb.append(indent).append("(∧L[").append(idx).append("])  ")
                        .append(seq).append("\n");
                child.formatTree(sb, indent + "  ", true);
            }
            case OrR(Sequent seq, int idx, ProofTree child) -> {
                sb.append(indent).append("(∨R[").append(idx).append("])  ")
                        .append(seq).append("\n");
                child.formatTree(sb, indent + "  ", true);
            }
            case ImplyR(Sequent seq, int idx, ProofTree child) -> {
                sb.append(indent).append("(→R[").append(idx).append("])  ")
                        .append(seq).append("\n");
                child.formatTree(sb, indent + "  ", true);
            }
            case AndR(Sequent seq, int idx, ProofTree left, ProofTree right) -> {
                sb.append(indent).append("(∧R[").append(idx).append("])  ")
                        .append(seq).append("\n");
                sb.append(indent).append("├─ left:\n");
                left.formatTree(sb, indent + "│  ", false);
                sb.append(indent).append("└─ right:\n");
                right.formatTree(sb, indent + "   ", true);
            }
            case OrL(Sequent seq, int idx, ProofTree left, ProofTree right) -> {
                sb.append(indent).append("(∨L[").append(idx).append("])  ")
                        .append(seq).append("\n");
                sb.append(indent).append("├─ left:\n");
                left.formatTree(sb, indent + "│  ", false);
                sb.append(indent).append("└─ right:\n");
                right.formatTree(sb, indent + "   ", true);
            }
            case ImplyL(Sequent seq, int idx, ProofTree left, ProofTree right) -> {
                sb.append(indent).append("(→L[").append(idx).append("])  ")
                        .append(seq).append("\n");
                sb.append(indent).append("├─ left:\n");
                left.formatTree(sb, indent + "│  ", false);
                sb.append(indent).append("└─ right:\n");
                right.formatTree(sb, indent + "   ", true);
            }
        }
    }
}
