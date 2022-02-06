package sjtu.ipads.wtune.superopt.runner;

import sjtu.ipads.wtune.common.utils.Args;
import sjtu.ipads.wtune.sql.ast.SqlNode;
import sjtu.ipads.wtune.superopt.substitution.Substitution;
import sjtu.ipads.wtune.superopt.substitution.SubstitutionBank;
import sjtu.ipads.wtune.superopt.uexpr.UExprSupport;
import sjtu.ipads.wtune.superopt.uexpr.UExprTranslationResult;

import java.io.IOException;
import java.nio.file.Path;

import static sjtu.ipads.wtune.sql.plan.PlanSupport.translateAsAst;
import static sjtu.ipads.wtune.superopt.logic.LogicSupport.*;
import static sjtu.ipads.wtune.superopt.runner.RunnerSupport.dataDir;
import static sjtu.ipads.wtune.superopt.substitution.SubstitutionSupport.loadBank;
import static sjtu.ipads.wtune.superopt.substitution.SubstitutionSupport.translateAsPlan;

public class RunRuleExample implements Runner {
  private Substitution rule;

  @Override
  public void prepare(String[] argStrings) throws Exception {
    final Args args = Args.parse(argStrings, 1);
    final String rules = args.getOptional("R", "rules", String.class, "prepared/rules.example.txt");
    final SubstitutionBank bank = loadBank(dataDir().resolve(rules));
    final int index = args.getOptional("I", "index", int.class, 0);
    for (Substitution rule : bank.rules()) {
      if (rule.id() == index) {
        this.rule = rule;
        break;
      }
    }
    if (this.rule == null) throw new IllegalArgumentException("No such rule: " + index);
  }

  @Override
  public void run() throws Exception {
    run(rule);
  }

  private static void run(Substitution rule) {
    System.out.println("1. Rule String");
    System.out.println("  " + rule);
    System.out.println();

    final var pair = translateAsPlan(rule);
    final SqlNode q0 = translateAsAst(pair.getLeft(), pair.getLeft().root(), true);
    final SqlNode q1 = translateAsAst(pair.getRight(), pair.getRight().root(), true);

    System.out.println("2. Example Query");
    System.out.println("  " + q0);
    System.out.println("  " + q1);
    System.out.println();

    final UExprTranslationResult uExprs = UExprSupport.translateToUExpr(rule);
    System.out.println("3. U-Expression");
    System.out.println("  [[q0]](" + uExprs.sourceOutVar() + ") := " + uExprs.sourceExpr());
    System.out.println("  [[q1]](" + uExprs.targetOutVar() + ") := " + uExprs.targetExpr());
    System.out.println();

    System.out.println("4. First-Order Formulas (Z3 Script)");
    setDumpFormulas(true);
    final int result = proveEq(uExprs);
    System.out.println(
        "==> Result: " + (result == EQ ? "UNSAT" : result == NEQ ? "SAT" : "UNKNOWN"));
  }
}
