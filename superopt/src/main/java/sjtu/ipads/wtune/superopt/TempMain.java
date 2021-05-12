package sjtu.ipads.wtune.superopt;

import static sjtu.ipads.wtune.sqlparser.plan.OperatorType.Proj;
import static sjtu.ipads.wtune.stmt.support.Workflow.normalize;
import static sjtu.ipads.wtune.superopt.optimizer.support.UniquenessInference.inferUniqueness;

import java.util.HashSet;
import java.util.Set;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.constants.ConstraintType;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.sqlparser.plan.ToPlanTranslator;
import sjtu.ipads.wtune.sqlparser.schema.Constraint;
import sjtu.ipads.wtune.sqlparser.schema.Table;
import sjtu.ipads.wtune.stmt.App;
import sjtu.ipads.wtune.stmt.Statement;

public class TempMain {
  private static void checkNonSetTable() {
    int totalTables = 0;
    final Set<String> nonSetTables = new HashSet<>();
    for (App app : App.all()) {
      outer:
      for (Table table : app.schema("base").tables()) {
        ++totalTables;
        for (Constraint constraint : table.constraints()) {
          if (constraint.type() == ConstraintType.PRIMARY) continue outer;
          if (constraint.type() == ConstraintType.UNIQUE) continue outer;
        }
        nonSetTables.add(app.name() + "." + table.name());
      }
    }

    System.out.println(totalTables + "/" + nonSetTables.size());
    nonSetTables.forEach(System.out::println);
  }

  private static void checkNonSetStatement() {
    int total = 0, nonSet = 0, partialNonSet = 0, unsupported = 0;
    for (Statement statement : Statement.findAll()) {
      final ASTNode ast = statement.parsed();
      ast.context().setSchema(statement.app().schema("base", true));
      normalize(ast);

      final PlanNode plan = ToPlanTranslator.toPlan(ast);
      if (plan == null) continue;

      final int result = EffectiveSetChecker.check(plan);
      if (result == 0) ++partialNonSet;
      else if (result == -1) ++nonSet;
      ++total;
      System.out.println(total + "/" + nonSet + "/" + partialNonSet + "/" + unsupported);
    }

    System.out.println(total + "/" + nonSet + "/" + partialNonSet + "/" + unsupported);
  }

  public static void main(String[] args) {
    checkNonSetStatement();
    //    checkNonSetTable();
  }

  private static class EffectiveSetChecker {
    private int projCount, nonSetProjCount;

    public static int check(PlanNode node) {
      final EffectiveSetChecker checker = new EffectiveSetChecker();
      checker.check0(node);
      if (checker.nonSetProjCount == 0) return 1;
      if (checker.projCount > checker.nonSetProjCount) return 0;
      else return -1;
    }

    private void check0(PlanNode node) {
      if (node.type() == Proj) {
        ++projCount;
        if (!inferUniqueness(node)) ++nonSetProjCount;
      }
      for (PlanNode predecessor : node.predecessors()) {
        check0(predecessor);
      }
    }
  }
}
