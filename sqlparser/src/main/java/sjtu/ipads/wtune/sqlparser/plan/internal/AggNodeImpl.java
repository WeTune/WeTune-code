package sjtu.ipads.wtune.sqlparser.plan.internal;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.AGGREGATE_DISTINCT;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.SELECT_ITEM_EXPR;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind.AGGREGATE;
import static sjtu.ipads.wtune.sqlparser.util.ColumnRefCollector.gatherColumnRefs;

import gnu.trove.list.TIntList;
import java.util.ArrayList;
import java.util.List;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.plan.AggNode;
import sjtu.ipads.wtune.sqlparser.plan.AttributeDef;
import sjtu.ipads.wtune.sqlparser.plan.AttributeDefBag;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.sqlparser.plan.ProjNode;

public class AggNodeImpl extends PlanNodeBase implements AggNode {
  private final AttributeDefBag definedAttrs;

  private List<ASTNode> groups;
  private List<ASTNode> selections;
  private ASTNode having;

  private TIntList attrsInGroups;
  private TIntList attrsInSelections;
  private TIntList attrsInHaving;

  private boolean isASTUpdated;

  private AggNodeImpl(
      List<ASTNode> selections,
      List<ASTNode> groups,
      ASTNode having,
      AttributeDefBag definedAttrs,
      TIntList attrsInSelections,
      TIntList attrsInGroups,
      TIntList attrsInHaving,
      boolean isASTUpdated) {
    this.selections = selections;
    this.groups = groups;
    this.having = having;

    this.definedAttrs = definedAttrs;

    this.attrsInSelections = attrsInSelections;
    this.attrsInGroups = attrsInGroups;
    this.attrsInHaving = attrsInHaving;

    this.isASTUpdated = isASTUpdated;
  }

  public static AggNode build(
      String qualification, List<ASTNode> selections, List<ASTNode> groups, ASTNode having) {
    return new AggNodeImpl(
        selections,
        groups,
        having,
        makeAttributes(qualification, selections),
        null,
        null,
        null,
        false);
  }

  private void updateAST() {
    this.selections = updateSelections();
    this.groups = updateGroups();
    this.having = updateHaving();
    this.isASTUpdated = true;
  }

  private List<ASTNode> updateSelections() {
    final List<ASTNode> selections = listMap(ASTNode::deepCopy, this.selections);
    final List<ASTNode> colRefs = gatherColumnRefs(selections);

    updateColumnRefs(colRefs, attrsInSelections, predecessors()[0].definedAttributes(), true);

    final boolean forcedUnique = ((ProjNode) predecessors()[0]).isForcedUnique();
    for (ASTNode agg : selections) {
      final ASTNode expr = agg.get(SELECT_ITEM_EXPR);
      if (AGGREGATE.isInstance(expr)) expr.set(AGGREGATE_DISTINCT, forcedUnique);
    }
    return selections;
  }

  private List<ASTNode> updateGroups() {
    if (groups == null) return null;
    final List<ASTNode> groups = listMap(ASTNode::deepCopy, this.groups);
    updateColumnRefs(
        gatherColumnRefs(groups), attrsInGroups, predecessors()[0].definedAttributes(), true);
    return groups;
  }

  private ASTNode updateHaving() {
    if (having == null) return null;
    final ASTNode having = this.having.deepCopy();
    updateColumnRefs(
        gatherColumnRefs(having), attrsInHaving, predecessors()[0].definedAttributes(), true);
    return having;
  }

  @Override
  public List<ASTNode> selections() {
    if (isASTUpdated) return listMap(ASTNode::deepCopy, selections);

    updateAST();
    return selections;
  }

  @Override
  public List<ASTNode> groups() {
    if (groups == null) return null;
    if (isASTUpdated) return listMap(ASTNode::deepCopy, groups);
    updateAST();
    return groups;
  }

  @Override
  public ASTNode having() {
    if (having == null) return null;
    if (isASTUpdated) return having.deepCopy();
    updateAST();
    return having;
  }

  @Override
  public AttributeDefBag definedAttributes() {
    return definedAttrs;
  }

  @Override
  public List<AttributeDef> usedAttributes() {
    final List<AttributeDef> used = new ArrayList<>(attrsInSelections.size());
    final List<AttributeDef> inputAttrs = predecessors()[0].definedAttributes();
    attrsInSelections.forEach(it -> used.add(inputAttrs.get(it)));
    if (attrsInGroups != null) attrsInGroups.forEach(it -> used.add(inputAttrs.get(it)));
    if (attrsInHaving != null) attrsInHaving.forEach(it -> used.add(inputAttrs.get(it)));
    return used;
  }

  @Override
  public void resolveUsed() {
    // `input` must contains all attributes used in groupKeys and groupItems,
    // and its output relation must keep unchanged even after substitution.
    // Thus, to handle attribute displace, the used attribute are recorded by index.
    //
    // Example:
    // SQL: SELECT COUNT(b.id) FROM a JOIN b ON a.ref = b.id
    // Plan: Agg<[0]>(Proj<b.id>(InnerJoin<a.ref=b.id>(Input<a>, Input<b>)))
    // Plan_opt: Agg<[0]>(Proj<a.ref>(Input<a>))
    // SQL_opt: SELECT COUNT(a.ref) FROM a JOIN b ON a.ref = b.id

    this.isASTUpdated = false;
    if (attrsInSelections != null) return;

    final AttributeDefBag inAttrs = predecessors()[0].definedAttributes();
    attrsInSelections = resolveUsed(selections, inAttrs);
    if (groups != null) attrsInGroups = resolveUsed(groups, inAttrs);
    if (having != null) attrsInHaving = resolveUsed(singletonList(having), inAttrs);

    this.definedAttrs.forEach(it -> it.setReferences(emptyList()));
  }

  @Override
  protected PlanNode copy0() {
    return new AggNodeImpl(
        selections,
        groups,
        having,
        definedAttrs,
        attrsInSelections,
        attrsInGroups,
        attrsInHaving,
        isASTUpdated);
  }

  @Override
  public String toString() {
    if (!isASTUpdated) updateAST();
    return "Agg<%s %s %s>".formatted(selections, groups, having);
  }
}
