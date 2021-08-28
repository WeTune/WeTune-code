package sjtu.ipads.wtune.sqlparser.plan;

import java.util.HashSet;
import java.util.List;
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
    if (node.kind() == INPUT) {
      qualifyInput((InputNode) node);
    } else if (node.kind() == PROJ) {
      qualifyProj((ProjNode) node);
      distinguishAttrs(node.values());
    }
  }

  private void qualifyProj(ProjNode proj) {
    final String knownName = proj.values().qualification();

    if (knownName == null && !mustBeQualified(proj)) return;
    if (knownName != null && knownNames.add(knownName)) return;

    final String newName = generateNewName(knownName, knownNames);
    knownNames.add(newName);

    proj.values().setQualification(newName);
  }

  private void qualifyInput(InputNode input) {
    final String knownName = input.values().qualification();
    if (knownName == null) throw failed("non-consensus qualification found in " + input);

    if (knownNames.add(knownName)) return;

    final String newName = generateNewName(knownName, knownNames);
    knownNames.add(newName);

    input.values().setQualification(newName);
  }

  private void distinguishAttrs(List<Value> values) {
    final Set<String> knownNames = new HashSet<>(values.size());
    for (Value value : values) {
      if (!knownNames.add(value.name())) {
        final String newName = generateNewName(value.name(), knownNames);
        value.setName(newName);
        knownNames.add(newName);
      }
    }
  }

  private String generateNewName(String baseName, Set<String> knownNames) {
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
