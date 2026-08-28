package com.gentzen.core;

import java.util.*;

/**
 * G' 矢列演算自动证明搜索器。
 *
 * <p>使用反向搜索（从目标矢列出发生成前提），递归应用 G' 系统规则。
 * 所有规则都是可逆的，因此不需要回溯——任意分解顺序都会找到证明（若存在）。
 *
 * <p>步数机制：每次规则应用消耗 1 步；步数耗尽时返回 ResourceExhausted。
 *
 * <p><b>Bonus: 反例生成</b> — 当矢列不可证时，返回一个赋值 (valuation)
 * 使得 Γ 中所有公式为 true，Δ 中所有公式为 false。
 */
public class Solver {

    /** 求解结果类型 */
    public sealed interface SolveResult {
        /** 可证——携带完整证明树 */
        record Provable(ProofTree tree) implements SolveResult {}

        /**
         * 不可证——携带反例赋值。
         * Map 中的键为命题变量 id，值为 true/false。
         * 该赋值使得 Γ 中所有公式为 true，Δ 中所有公式为 false。
         */
        record NotProvable(Map<Integer, Boolean> valuation) implements SolveResult {}

        /** 资源耗尽（步数用完） */
        record ResourceExhausted(int stepsUsed) implements SolveResult {}
    }

    private final int maxSteps;

    public Solver(int maxSteps) {
        if (maxSteps < 0) {
            throw new IllegalArgumentException("步数不能为负数");
        }
        this.maxSteps = maxSteps;
    }

    /**
     * 尝试证明一个矢列。
     *
     * @param sequent 目标矢列
     * @return 求解结果
     */
    public SolveResult solve(Sequent sequent) {
        SolveResult result = solveInternal(sequent, maxSteps);
        // Bonus: 丰富反例赋值，确保覆盖原始矢列中所有变量
        if (result instanceof SolveResult.NotProvable(Map<Integer, Boolean> leafVal)) {
            Map<Integer, Boolean> fullVal = new HashMap<>(leafVal);
            for (int id : sequent.allVarIds()) {
                fullVal.putIfAbsent(id, false); // 未出现的变量任意赋 false
            }
            return new SolveResult.NotProvable(Collections.unmodifiableMap(fullVal));
        }
        return result;
    }

    /**
     * 内部递归求解。
     *
     * @param seq  当前矢列
     * @param step 剩余步数
     * @return 求解结果
     */
    private SolveResult solveInternal(Sequent seq, int step) {
        // 1. 检查公理
        Integer axiomVar = findAxiomVar(seq);
        if (axiomVar != null) {
            return new SolveResult.Provable(new ProofTree.Ax(seq, axiomVar));
        }

        // 2. 步数耗尽
        if (step <= 0) {
            return new SolveResult.ResourceExhausted(maxSteps);
        }

        // 3. 优先左侧非原子公式
        int idx = seq.firstCompoundLeft();
        if (idx >= 0) {
            return applyLeftRule(seq, idx, step - 1);
        }

        // 4. 再试右侧非原子公式
        idx = seq.firstCompoundRight();
        if (idx >= 0) {
            return applyRightRule(seq, idx, step - 1);
        }

        // 5. 全是原子公式但不构成公理 → 不可证，生成反例
        return notProvableFromLeaf(seq);
    }

    // ──────────────── 反例生成 ────────────────

    /**
     * 从全部为原子公式的叶子矢列生成反例赋值。
     * 因为不是公理，左右两侧没有共同的原子命题，
     * 所以可以将左侧原子全部赋 true、右侧原子全部赋 false。
     * <p>
     * <b>正确性论证：</b>G' 系统规则都是可逆且保真的。
     * 叶子处的反例赋值通过规则反向传播，必然是根矢列的反例。
     */
    private SolveResult.NotProvable notProvableFromLeaf(Sequent leaf) {
        Map<Integer, Boolean> valuation = new HashMap<>();
        // 左侧原子为 true
        for (Prop p : leaf.left()) {
            if (p instanceof Prop.Var(int id)) {
                valuation.put(id, true);
            }
        }
        // 右侧原子为 false（左侧已赋值的除外）
        for (Prop p : leaf.right()) {
            if (p instanceof Prop.Var(int id) && !valuation.containsKey(id)) {
                valuation.put(id, false);
            }
        }
        return new SolveResult.NotProvable(valuation);
    }

    // ──────────────── 公理检测 ────────────────

    /**
     * 查找左右两侧共有的原子命题 id，用于构建 Ax 节点。
     * 返回第一个匹配的变量 id，若无则返回 null。
     */
    private Integer findAxiomVar(Sequent seq) {
        Set<Integer> leftAtomIds = new HashSet<>();
        for (Prop p : seq.left()) {
            if (p instanceof Prop.Var(int id)) {
                leftAtomIds.add(id);
            }
        }
        for (Prop p : seq.right()) {
            if (p instanceof Prop.Var(int id) && leftAtomIds.contains(id)) {
                return id;
            }
        }
        return null;
    }

    // ──────────────── 左侧规则 ────────────────

    private SolveResult applyLeftRule(Sequent seq, int idx, int step) {
        Prop p = seq.left().get(idx);

        return switch (p) {
            case Prop.Not(Prop sub) -> {
                // ¬L: Γ, ¬A ⊢ Δ  →  Γ ⊢ A, Δ
                Sequent subgoal = seq.removeLeft(idx).addRight(sub);
                SolveResult r = solveInternal(subgoal, step);
                yield mapSingle(r, tree -> new ProofTree.NotL(seq, idx, tree));
            }
            case Prop.And(Prop l, Prop r) -> {
                // ∧L: Γ, A∧B ⊢ Δ  →  Γ, A, B ⊢ Δ
                Sequent subgoal = seq.replaceLeft(idx, List.of(l, r));
                SolveResult res = solveInternal(subgoal, step);
                yield mapSingle(res, tree -> new ProofTree.AndL(seq, idx, tree));
            }
            case Prop.Or(Prop l, Prop r) -> {
                // ∨L: Γ, A∨B ⊢ Δ  →  (Γ, A ⊢ Δ) 和 (Γ, B ⊢ Δ)
                yield applyDoubleRule(seq, idx, step,
                        seq.replaceLeft(idx, List.of(l)),   // left branch: Γ, A ⊢ Δ
                        seq.replaceLeft(idx, List.of(r)),   // right branch: Γ, B ⊢ Δ
                        (leftTree, rightTree) -> new ProofTree.OrL(seq, idx, leftTree, rightTree));
            }
            case Prop.Impl(Prop l, Prop r) -> {
                // →L: Γ, A→B ⊢ Δ  →  (Γ ⊢ A, Δ) 和 (Γ, B ⊢ Δ)
                yield applyDoubleRule(seq, idx, step,
                        seq.removeLeft(idx).addRight(l),    // left branch: Γ ⊢ A, Δ
                        seq.replaceLeft(idx, List.of(r)),   // right branch: Γ, B ⊢ Δ
                        (leftTree, rightTree) -> new ProofTree.ImplyL(seq, idx, leftTree, rightTree));
            }
            default -> notProvableFromLeaf(seq); // 原子公式（不应到达这里）
        };
    }

    // ──────────────── 右侧规则 ────────────────

    private SolveResult applyRightRule(Sequent seq, int idx, int step) {
        Prop p = seq.right().get(idx);

        return switch (p) {
            case Prop.Not(Prop sub) -> {
                // ¬R: Γ ⊢ ¬A, Δ  →  Γ, A ⊢ Δ
                Sequent subgoal = seq.removeRight(idx).addLeft(sub);
                SolveResult r = solveInternal(subgoal, step);
                yield mapSingle(r, tree -> new ProofTree.NotR(seq, idx, tree));
            }
            case Prop.And(Prop l, Prop r) -> {
                // ∧R: Γ ⊢ A∧B, Δ  →  (Γ ⊢ A, Δ) 和 (Γ ⊢ B, Δ)
                yield applyDoubleRule(seq, idx, step,
                        seq.replaceRight(idx, List.of(l)),   // left branch: Γ ⊢ A, Δ
                        seq.replaceRight(idx, List.of(r)),   // right branch: Γ ⊢ B, Δ
                        (leftTree, rightTree) -> new ProofTree.AndR(seq, idx, leftTree, rightTree));
            }
            case Prop.Or(Prop l, Prop r) -> {
                // ∨R: Γ ⊢ A∨B, Δ  →  Γ ⊢ A, B, Δ
                Sequent subgoal = seq.replaceRight(idx, List.of(l, r));
                SolveResult res = solveInternal(subgoal, step);
                yield mapSingle(res, tree -> new ProofTree.OrR(seq, idx, tree));
            }
            case Prop.Impl(Prop l, Prop r) -> {
                // →R: Γ ⊢ A→B, Δ  →  Γ, A ⊢ B, Δ
                Sequent subgoal = seq.removeRight(idx).addLeft(l).addRight(r);
                SolveResult res = solveInternal(subgoal, step);
                yield mapSingle(res, tree -> new ProofTree.ImplyR(seq, idx, tree));
            }
            default -> notProvableFromLeaf(seq); // 原子公式（不应到达这里）
        };
    }

    // ──────────────── 双分支规则 ────────────────

    @FunctionalInterface
    private interface DoubleBranchBuilder {
        ProofTree build(ProofTree left, ProofTree right);
    }

    /**
     * 处理双分支规则：两个子目标都必须可证。
     * 按顺序尝试（先左后右），任一失败则返回失败（含反例）。
     */
    private SolveResult applyDoubleRule(Sequent seq, int idx, int step,
                                        Sequent leftSubgoal, Sequent rightSubgoal,
                                        DoubleBranchBuilder builder) {
        SolveResult leftResult = solveInternal(leftSubgoal, step);
        if (!(leftResult instanceof SolveResult.Provable(ProofTree leftTree))) {
            return leftResult; // 传播失败/资源耗尽/反例
        }

        SolveResult rightResult = solveInternal(rightSubgoal, step);
        if (!(rightResult instanceof SolveResult.Provable(ProofTree rightTree))) {
            return rightResult;
        }

        return new SolveResult.Provable(builder.build(leftTree, rightTree));
    }

    // ──────────────── 辅助方法 ────────────────

    @FunctionalInterface
    private interface SingleBranchBuilder {
        ProofTree build(ProofTree child);
    }

    /** 将子目标结果映射为单分支规则结果 */
    private SolveResult mapSingle(SolveResult result, SingleBranchBuilder builder) {
        if (result instanceof SolveResult.Provable(ProofTree child)) {
            return new SolveResult.Provable(builder.build(child));
        }
        return result;
    }

    // ──────────────── 便捷静态方法 ────────────────

    /**
     * 使用默认步数（100）求解矢列。
     */
    public static SolveResult solveDefault(Sequent sequent) {
        return new Solver(100).solve(sequent);
    }
}
