package com.gentzen;

import com.gentzen.core.*;
import com.gentzen.parser.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;

/**
 * G' 矢列演算证明系统的单元测试。
 * 覆盖 PDF 中要求的全部 5 个测试用例以及一些额外边界情况。
 */
public class GentzenTest {

    private static final int DEFAULT_STEPS = 200;

    // ──────────────── Case 1: 公理测试 ────────────────

    @Test
    @DisplayName("Case 1: Axiom — P ⊢ P")
    public void testAxiom() {
        Sequent seq = Parser.parseSequent("0 |- 0");
        assertEquals("0 |- 0", seq.toString());

        Solver.SolveResult result = new Solver(DEFAULT_STEPS).solve(seq);
        assertInstanceOf(Solver.SolveResult.Provable.class, result);

        Solver.SolveResult.Provable provable = (Solver.SolveResult.Provable) result;
        assertInstanceOf(ProofTree.Ax.class, provable.tree());

        ProofTree.Ax ax = (ProofTree.Ax) provable.tree();
        assertEquals(0, ax.varId());
        System.out.println("Case 1 passed: Axiom");
    }

    // ──────────────── Case 2: Modus Ponens ────────────────

    @Test
    @DisplayName("Case 2: Modus Ponens — P→Q, P ⊢ Q")
    public void testModusPonens() {
        Sequent seq = Parser.parseSequent("(0 -> 1), 0 |- 1");
        assertEquals("0 -> 1, 0 |- 1", seq.toString());

        Solver.SolveResult result = new Solver(DEFAULT_STEPS).solve(seq);
        assertInstanceOf(Solver.SolveResult.Provable.class, result);

        Solver.SolveResult.Provable provable = (Solver.SolveResult.Provable) result;
        // 应该是 →L 规则
        assertInstanceOf(ProofTree.ImplyL.class, provable.tree());

        System.out.println("Case 2 passed: Modus Ponens");
        System.out.println(provable.tree().toPrettyString());
    }

    // ──────────────── Case 3: De Morgan's Law ────────────────

    @Test
    @DisplayName("Case 3: De Morgan — ¬(P∨Q) ⊢ ¬P ∧ ¬Q")
    public void testDeMorgan() {
        Sequent seq = Parser.parseSequent("~(0 | 1) |- (~0 & ~1)");
        assertEquals("~(0 | 1) |- ~0 & ~1", seq.toString());

        Solver.SolveResult result = new Solver(DEFAULT_STEPS).solve(seq);
        assertInstanceOf(Solver.SolveResult.Provable.class, result);

        System.out.println("Case 3 passed: De Morgan's Law");
        Solver.SolveResult.Provable provable = (Solver.SolveResult.Provable) result;
        System.out.println(provable.tree().toPrettyString());
    }

    // ──────────────── Case 4: Peirce's Law ────────────────

    @Test
    @DisplayName("Case 4: Peirce's Law — ⊢ ((P→Q)→P)→P")
    public void testPeirceLaw() {
        Sequent seq = Parser.parseSequent("|- (((0 -> 1) -> 0) -> 0)");

        Solver.SolveResult result = new Solver(DEFAULT_STEPS).solve(seq);
        assertInstanceOf(Solver.SolveResult.Provable.class, result);

        System.out.println("Case 4 passed: Peirce's Law");
        Solver.SolveResult.Provable provable = (Solver.SolveResult.Provable) result;
        System.out.println(provable.tree().toPrettyString());
    }

    // ──────────────── Case 5: 反例（不可证） ────────────────

    @Test
    @DisplayName("Case 5: Counter-example — P∨Q ⊢ P∧Q (不可证)")
    public void testCounterExample() {
        Sequent seq = Parser.parseSequent("(0 | 1) |- (0 & 1)");
        assertEquals("0 | 1 |- 0 & 1", seq.toString());

        Solver.SolveResult result = new Solver(DEFAULT_STEPS).solve(seq);
        assertInstanceOf(Solver.SolveResult.NotProvable.class, result);

        Solver.SolveResult.NotProvable notProvable =
                (Solver.SolveResult.NotProvable) result;
        Map<Integer, Boolean> val = notProvable.valuation();

        // 应包含 P0 和 P1
        assertTrue(val.containsKey(0), "应包含变量 P0");
        assertTrue(val.containsKey(1), "应包含变量 P1");

        // 验证反例正确性：Γ 中公式为 true (P0∨P1 = true)
        boolean leftEval = val.get(0) || val.get(1);
        assertTrue(leftEval, "Γ 中公式 P0∨P1 应为 true");

        // Δ 中公式为 false (P0∧P1 = false)
        boolean rightEval = val.get(0) && val.get(1);
        assertFalse(rightEval, "Δ 中公式 P0∧P1 应为 false");

        System.out.println("Case 5 passed: Unprovable with counter-example " + val);
    }

    // ──────────────── 解析器附加测试 ────────────────

    @Test
    @DisplayName("Parser: 解析嵌套否定")
    public void testParseDoubleNegation() {
        Prop p = Parser.parseFormula("~~0");
        assertEquals("~~0", p.toString());
        assertInstanceOf(Prop.Not.class, p);
        Prop.Not not = (Prop.Not) p;
        assertInstanceOf(Prop.Not.class, not.sub());
    }

    @Test
    @DisplayName("Parser: 解析复杂公式")
    public void testParseComplex() {
        Prop p = Parser.parseFormula("(0 & 1) | (~2 -> 3)");
        assertInstanceOf(Prop.Or.class, p);
        System.out.println("Parsed: " + p);
    }

    @Test
    @DisplayName("Parser: 解析带空格的矢列")
    public void testParseWithSpaces() {
        Sequent seq = Parser.parseSequent("  0   |-   0  ");
        assertEquals("0 |- 0", seq.toString());
    }

    @Test
    @DisplayName("Parser: 解析空左侧矢列")
    public void testParseEmptyLeft() {
        Sequent seq = Parser.parseSequent("|- 0");
        assertTrue(seq.left().isEmpty());
        assertEquals(1, seq.right().size());
    }

    @Test
    @DisplayName("Parser: 解析空右侧矢列")
    public void testParseEmptyRight() {
        Sequent seq = Parser.parseSequent("0 |-");
        assertEquals(1, seq.left().size());
        assertTrue(seq.right().isEmpty());
    }

    @Test
    @DisplayName("Parser: 逗号分隔多个公式")
    public void testParseMultipleFormulas() {
        Sequent seq = Parser.parseSequent("0, 1, 2 |- 3, 4");
        assertEquals(3, seq.left().size());
        assertEquals(2, seq.right().size());
    }

    // ──────────────── 步数限制测试 ────────────────

    @Test
    @DisplayName("Solver: 步数耗尽时返回 ResourceExhausted")
    public void testStepExhaustion() {
        Sequent seq = Parser.parseSequent("~(0 | 1) |- (~0 & ~1)");
        // 给 1 步——肯定不够完成 De Morgan 证明
        Solver.SolveResult result = new Solver(1).solve(seq);
        assertInstanceOf(Solver.SolveResult.ResourceExhausted.class, result);
        System.out.println("Step exhaustion test passed");
    }

    // ──────────────── 额外逻辑测试 ────────────────

    @Test
    @DisplayName("Solver: 排中律 P ∨ ¬P (Law of Excluded Middle)")
    public void testExcludedMiddle() {
        Sequent seq = Parser.parseSequent("|- 0 | ~0");
        Solver.SolveResult result = new Solver(DEFAULT_STEPS).solve(seq);
        assertInstanceOf(Solver.SolveResult.Provable.class, result);
        System.out.println("Excluded Middle: Provable ✓");
    }

    @Test
    @DisplayName("Solver: 矛盾蕴涵任意命题 P∧¬P ⊢ Q")
    public void testContradiction() {
        Sequent seq = Parser.parseSequent("0 & ~0 |- 1");
        Solver.SolveResult result = new Solver(DEFAULT_STEPS).solve(seq);
        assertInstanceOf(Solver.SolveResult.Provable.class, result);
        System.out.println("Contradiction implies anything: Provable ✓");
    }

    @Test
    @DisplayName("Solver: 传递律 (P→Q), (Q→R) ⊢ P→R")
    public void testTransitivity() {
        Sequent seq = Parser.parseSequent("(0 -> 1), (1 -> 2) |- 0 -> 2");
        Solver.SolveResult result = new Solver(DEFAULT_STEPS).solve(seq);
        assertInstanceOf(Solver.SolveResult.Provable.class, result);
        System.out.println("Transitivity: Provable ✓");
    }
}
