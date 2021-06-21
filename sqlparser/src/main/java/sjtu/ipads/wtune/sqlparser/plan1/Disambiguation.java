package sjtu.ipads.wtune.sqlparser.plan1;

import static sjtu.ipads.wtune.sqlparser.plan.OperatorType.Input;
import static sjtu.ipads.wtune.sqlparser.plan.OperatorType.Proj;
import static sjtu.ipads.wtune.sqlparser.plan.OperatorType.Union;

import java.util.HashSet;
import java.util.Set;
import sjtu.ipads.wtune.sqlparser.plan.OperatorType;

// assign unique qualification to each Input and Proj
class Disambiguation {
  private final Set<String> knownNames = new HashSet<>();
  private final PlanNode plan;

  Disambiguation(PlanNode plan) {
    this.plan = plan;
  }

  public static void disambiguate(PlanNode node) {
    new Disambiguation(node).onNode(node);
  }

  private void onNode(PlanNode node) {
    for (PlanNode predecessor : node.predecessors()) onNode(predecessor);
    if (node.type() == Input) onInput((InputNode) node);
    else if (node.type() == Proj) onProj((ProjNode) node);
  }

  private void onProj(ProjNode proj) {
    final String knownName = proj.values().qualification();

    if (knownName == null)
      if (mustQualified(proj)) throw failed("subquery must be named " + proj);
      else return;

    if (knownNames.add(knownName)) return;

    final String newName = generateNewName(knownName);
    proj.values().setQualification(newName);
    knownNames.add(newName);
  }

  private void onInput(InputNode input) {
    final String knownName = input.values().qualification();
    if (knownName == null) throw failed("non-consensus qualification found in " + input);

    if (knownNames.add(knownName)) return;

    final String newName = generateNewName(knownName);
    input.values().setQualification(newName);
    knownNames.add(newName);
  }

  private String generateNewName(String baseName) {
    int i = 0;
    while (true) {
      final String newName = baseName + (i++);
      if (!knownNames.contains(newName)) return newName;
    }
  }

  private RuntimeException failed(String reason) {
    return new IllegalArgumentException("invalid plan [" + reason + "] " + plan);
  }

  private static boolean mustQualified(ProjNode proj) {
    PlanNode successor = proj.successor();
    while (successor != null) {
      final OperatorType succType = successor.type();
      if (succType == Union) return false;
      if (succType == Proj) return true;
      if (succType.isJoin()) return true;
      if (succType.isFilter()) return successor.predecessors()[0] == proj;
      successor = successor.successor();
    }
    return false;
  }
}
