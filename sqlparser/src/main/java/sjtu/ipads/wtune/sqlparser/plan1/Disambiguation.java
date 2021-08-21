package sjtu.ipads.wtune.sqlparser.plan1;

import sjtu.ipads.wtune.sqlparser.plan.OperatorType;

import java.util.HashSet;
import java.util.Set;

import static sjtu.ipads.wtune.sqlparser.plan.OperatorType.*;

// assign unique qualification to each Input and Proj
class Disambiguation {
  private final Set<String> knownNames = new HashSet<>();
  private final PlanNode plan;

  Disambiguation(PlanNode plan) {
    this.plan = plan;
  }

  public PlanNode disambiguate() {
    onNode(plan);
    return plan;
  }

  private void onNode(PlanNode node) {
    for (PlanNode predecessor : node.predecessors()) onNode(predecessor);
    if (node.kind() == INPUT) onInput((InputNode) node);
    else if (node.kind() == PROJ) onProj((ProjNode) node);
  }

  private void onProj(ProjNode proj) {
    final String knownName = proj.values().qualification();

    if (knownName == null && !mustBeQualified(proj)) return;
    if (knownName != null && knownNames.add(knownName)) return;

    final String newName = generateNewName(knownName);
    knownNames.add(newName);

    proj.values().setQualification(newName);
  }

  private void onInput(InputNode input) {
    final String knownName = input.values().qualification();
    if (knownName == null) throw failed("non-consensus qualification found in " + input);

    if (knownNames.add(knownName)) return;

    final String newName = generateNewName(knownName);
    knownNames.add(newName);

    input.values().setQualification(newName);
  }

  private String generateNewName(String baseName) {
    baseName = baseName == null ? "sub" : baseName;
    int i = 0;
    while (true) {
      final String newName = baseName + '_' + (i++);
      if (!knownNames.contains(newName)) return newName;
    }
  }

  private RuntimeException failed(String reason) {
    return new IllegalArgumentException("invalid plan [" + reason + "] " + plan);
  }

  private static boolean mustBeQualified(ProjNode proj) {
    PlanNode successor = proj.successor();
    while (successor != null) {
      final OperatorType succType = successor.kind();
      if (succType == UNION) return false;
      if (succType == PROJ) return true;
      if (succType.isJoin()) return true;
      if (succType.isFilter()) return successor.predecessors()[0] == proj;
      successor = successor.successor();
    }
    return false;
  }
}
