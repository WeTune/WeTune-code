package sjtu.ipads.wtune.sqlparser.plan.internal;

import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.sqlparser.util.ColumnRefCollector.gatherColumnRefs;

import gnu.trove.list.TIntList;
import java.util.ArrayList;
import java.util.List;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.plan.AttributeDef;
import sjtu.ipads.wtune.sqlparser.plan.AttributeDefBag;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.sqlparser.plan.SortNode;

public class SortNodeImpl extends PlanNodeBase implements SortNode {
  protected List<ASTNode> keys;
  protected TIntList attrsInKeys;

  private boolean isASTUpdated;

  protected SortNodeImpl(List<ASTNode> keys, TIntList attrsInKeys, boolean isASTUpdated) {
    this.keys = keys;
    this.attrsInKeys = attrsInKeys;
    this.isASTUpdated = isASTUpdated;
  }

  public static SortNode build(List<ASTNode> orderKeys) {
    return new SortNodeImpl(orderKeys, null, false);
  }

  @Override
  public AttributeDefBag definedAttributes() {
    return predecessors()[0].definedAttributes();
  }

  @Override
  public List<AttributeDef> usedAttributes() {
    final AttributeDefBag inputAttrs = predecessors()[0].definedAttributes();
    final List<AttributeDef> usedAttrs = new ArrayList<>(attrsInKeys.size());
    attrsInKeys.forEach(it -> usedAttrs.add(inputAttrs.get(it)));
    return usedAttrs;
  }

  @Override
  public void resolveUsed() {
    if (attrsInKeys != null) return;

    final AttributeDefBag inAttrs = predecessors()[0].definedAttributes();
    attrsInKeys = resolveUsed(gatherColumnRefs(keys), inAttrs);

    this.isASTUpdated = false;
  }

  @Override
  public List<ASTNode> orderKeys() {
    if (isASTUpdated) return listMap(ASTNode::deepCopy, keys);

    final List<ASTNode> keys = listMap(ASTNode::deepCopy, this.keys);
    updateColumnRefs(gatherColumnRefs(keys), attrsInKeys, predecessors()[0].definedAttributes());

    return this.keys = keys;
  }

  @Override
  protected PlanNode copy0() {
    return new SortNodeImpl(keys, attrsInKeys, isASTUpdated);
  }

  @Override
  public String toString() {
    final List<ASTNode> orderKeys = isASTUpdated ? this.keys : this.orderKeys();
    return "Sort<%s>".formatted(orderKeys);
  }
}
