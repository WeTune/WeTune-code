package sjtu.ipads.wtune.superopt.internal;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.constants.NodeType;
import sjtu.ipads.wtune.sqlparser.plan.*;

import java.util.*;

import static sjtu.ipads.wtune.common.utils.Commons.coalesce;
import static sjtu.ipads.wtune.common.utils.Commons.listJoin;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.SELECT_ITEM_EXPR;
import static sjtu.ipads.wtune.sqlparser.plan.OperatorType.Proj;

public class PlanNormalizer {
  public static void normalize(PlanNode node) {
    final PlanNode successor = node.successor();
    final PlanNode[] predecessors = node.predecessors();
    for (PlanNode predecessor : predecessors) normalize(predecessor);

    if (node.type() == Proj
        && ((ProjNode) node).isWildcard()
        && successor != null
        && successor.type().isJoin()
        && !predecessors[0].type().isFilter()) {
      successor.replacePredecessor(node, predecessors[0]);
      return;
    }

    if (node.type().isJoin()) {
      if (predecessors[0].type().isFilter()) insertProj(node, predecessors[0]);
      if (predecessors[1].type().isFilter()) insertProj(node, predecessors[1]);
      rectifyQualification(node);
    }

    node.resolveUsed();
  }

  private static void insertProj(PlanNode successor, PlanNode predecessor) {
    final List<ASTNode> exprs =
        listMap(PlanNormalizer::makeSelectItem, predecessor.definedAttributes());
    final ProjNode proj = ProjNode.make(null, exprs);
    successor.replacePredecessor(predecessor, proj);
    proj.setWildcard(true);
    proj.setPredecessor(0, predecessor);
    proj.resolveUsed();
  }

  private static ASTNode makeSelectItem(AttributeDef def) {
    final ASTNode expr = def.toColumnRef();
    final ASTNode item = ASTNode.node(NodeType.SELECT_ITEM);
    item.set(SELECT_ITEM_EXPR, expr);
    return item;
  }

  private static void rectifyQualification(PlanNode node) {
    if (!node.type().isJoin()) return;

    final PlanNode left = node.predecessors()[0], right = node.predecessors()[1];
    assert right.type() == Proj || right.type() == OperatorType.Input;

    final Map<String, PlanNode> qualified = new HashMap<>();
    final Set<PlanNode> unqualified = Collections.newSetFromMap(new IdentityHashMap<>());
    for (AttributeDef attr : listJoin(left.definedAttributes(), right.definedAttributes())) {
      final String qualification = attr.qualification();
      final PlanNode definer = attr.definer();
      if (qualification == null
          || qualified.compute(qualification, (s, n) -> coalesce(n, definer)) != definer)
        unqualified.add(definer);
    }

    for (PlanNode n : unqualified) {
      final String qualification = makeQualification(qualified.keySet());
      setQualification(n, qualification);
      qualified.put(qualification, n);
    }
  }

  private static String makeQualification(Set<String> existing) {
    int i = 0;
    while (true) {
      final String qualification = "sub" + i;
      if (!existing.contains(qualification)) return qualification;
      ++i;
    }
  }

  private static void setQualification(PlanNode node, String qualification) {
    assert node.type() == Proj || node.type() == OperatorType.Input;
    if (node.type() == OperatorType.Input) ((InputNode) node).setAlias(qualification);
    node.definedAttributes().forEach(it -> it.setQualification(qualification));
  }
}
