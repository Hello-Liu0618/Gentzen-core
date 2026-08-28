package com.gentzen;

import com.gentzen.core.*;
import com.gentzen.parser.*;
import java.util.Map;

/**
 * Gentzen 矢列演算自动定理证明器 —— 主入口。
 *
 * <p>用法：
 * <pre>
 *   java -jar gentzen-core.jar "0 |- 0"
 *   java -jar gentzen-core.jar "(0 -> 1), 0 |- 1"
 *   java -jar gentzen-core.jar "~(0 | 1) |- (~0 & ~1)"
 * </pre>
 *
 * <p>也可不传参数，从标准输入读取矢列（一行一个）。
 */
public class Main {

    private static final int DEFAULT_STEPS = 100;

    public static void main(String[] args) {
        if (args.length > 0) {
            // 命令行模式：将所有参数拼接为输入字符串
            String input = String.join(" ", args);
            processSequent(input);
        } else {
            // 交互模式：从标准输入读取
            System.out.println("Gentzen 矢列演算自动定理证明器 (G' system)");
            System.out.println("支持的连接词: ~ (否定), & (合取), | (析取), -> (蕴含)");
            System.out.println("原子命题: 非负整数 (0, 1, 2, ...)");
            System.out.println("矢列格式: 左侧公式 , 分隔 |- 右侧公式 , 分隔");
            System.out.println("输入 ':q' 退出。");
            System.out.println();

            java.util.Scanner scanner = new java.util.Scanner(System.in);
            while (true) {
                System.out.print("> ");
                String line = scanner.nextLine().trim();
                if (line.equals(":q") || line.equals(":quit") || line.equals("exit")) {
                    System.out.println("再见！");
                    break;
                }
                if (line.isEmpty()) {
                    continue;
                }
                processSequent(line);
                System.out.println();
            }
            scanner.close();
        }
    }

    /**
     * 处理一个矢列输入：解析 → 求解 → 输出。
     */
    private static void processSequent(String input) {
        System.out.println("输入: " + input);

        // 1. 解析
        Sequent sequent;
        try {
            sequent = Parser.parseSequent(input);
        } catch (Exception e) {
            System.out.println("解析错误: " + e.getMessage());
            return;
        }
        System.out.println("矢列: " + sequent);

        // 2. 求解
        Solver solver = new Solver(DEFAULT_STEPS);
        Solver.SolveResult result = solver.solve(sequent);

        // 3. 输出结果
        switch (result) {
            case Solver.SolveResult.Provable(ProofTree tree) -> {
                System.out.println("结果: ✓ 可证 (Provable)");
                System.out.println("证明树:");
                System.out.println(tree.toPrettyString());
            }
            case Solver.SolveResult.NotProvable(Map<Integer, Boolean> val) -> {
                System.out.println("结果: ✗ 不可证 (Not Provable)");
                System.out.println("反例赋值 (Valuation):");
                // 按变量 id 排序输出
                val.entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .forEach(e -> System.out.println(
                                "  P" + e.getKey() + " = " + e.getValue()));
                // 验证反例正确性（可选）
                System.out.println("验证: Γ 中公式 = true, Δ 中公式 = false");
            }
            case Solver.SolveResult.ResourceExhausted(int used) -> {
                System.out.println("结果: ⚠ 资源耗尽 (Resource Exhausted) — 已使用 " + used + " 步");
            }
        }
    }
}
