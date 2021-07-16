package sjtu.ipads.wtune.sqlparser.plan.internal;

import static java.lang.System.identityHashCode;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;
import static sjtu.ipads.wtune.sqlparser.plan.AttributeDef.fromExpr;
import static sjtu.ipads.wtune.sqlparser.plan.AttributeDefBag.makeBag;
import static sjtu.ipads.wtune.sqlparser.util.ColumnRefCollector.gatherColumnRefs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.plan.AttributeDef;
import sjtu.ipads.wtune.sqlparser.plan.AttributeDefBag;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;
import sjtu.ipads.wtune.sqlparser.plan.ProjNode;

public class ProjNodeImpl extends PlanNodeBase implements ProjNode {
  private AttributeDefBag definedAttrs;
  private List<ASTNode> selections;
  private List<AttributeDef> usedAttrs;

  private boolean isForcedUnique;
  private boolean isWildcard;

  private boolean isSelectionUpdated;

  protected ProjNodeImpl(
      List<ASTNode> selections,
      List<AttributeDef> definedAttrs,
      List<AttributeDef> usedAttrs,
      boolean isForcedUnique,
      boolean isWildcard) {
    this.selections = selections;
    this.definedAttrs = makeBag(definedAttrs);
    this.usedAttrs = usedAttrs;

    this.isForcedUnique = isForcedUnique;
    this.isWildcard = isWildcard;

    this.isSelectionUpdated = false;
  }

  public static ProjNode build(String qualification, List<ASTNode> selections) {
    return new ProjNodeImpl(
        selections, makeAttributes(qualification, selections), null, false, false);
  }

  public static ProjNode build(List<AttributeDef> definedAttrs) {
    return new ProjNodeImpl(null, coerceIntoDerivedAttribute(definedAttrs), null, false, false);
  }

  public static ProjNode buildWildcard(List<AttributeDef> usedAttrs) {
    final int key = identityHashCode(new Object());
    final List<AttributeDef> definedAttrs = new ArrayList<>(usedAttrs.size());
    for (AttributeDef used : usedAttrs) {
      final AttributeDef definedAttr = fromExpr(key, null, used.name(), used.makeColumnRef());
      definedAttr.setReferences(Collections.singletonList(used));
      definedAttrs.add(definedAttr);
    }
    return new ProjNodeImpl(null, definedAttrs, usedAttrs, false, true);
  }

  private static AttributeDefBag coerceIntoDerivedAttribute(List<AttributeDef> attrs) {
    final int key = identityHashCode(new Object());

    final List<AttributeDef> coerced = new ArrayList<>(attrs.size());
    for (AttributeDef attr : attrs) {
      if (attr instanceof DerivedAttributeDef) coerced.add(attr);
      else {
        final ASTNode colRef = attr.makeColumnRef();
        final String name = attr.name();
        final String qualification = attr.qualification();
        final int id = key * 31 + colRef.hashCode();
        coerced.add(fromExpr(id, qualification, name, colRef));
      }
    }

    return makeBag(coerced);
  }

  @Override
  public void setWildcard(boolean wildcard) {
    this.isWildcard = wildcard;
  }

  @Override
  public void setForcedUnique(boolean forcedUnique) {
    this.isForcedUnique = forcedUnique;
  }

  @Override
  public void setQualification(String qualification) {
    this.definedAttrs = definedAttrs.copyWithQualification(qualification);
  }

  @Override
  public boolean isWildcard() {
    return isWildcard;
  }

  @Override
  public boolean isForcedUnique() {
    return isForcedUnique;
  }

  @Override
  public List<ASTNode> selections() {
    if (isSelectionUpdated) return listMap(selections, ASTNode::deepCopy);

    final List<ASTNode> selections = listMap(definedAttributes(), AttributeDef::makeSelectItem);
    updateColumnRefs(gatherColumnRefs(selections), usedAttributes());

    this.selections = selections;
    this.isSelectionUpdated = true;

    return selections;
  }

  @Override
  public List<AttributeDef> usedAttributes() {
    return usedAttrs;
  }

  @Override
  public AttributeDefBag definedAttributes() {
    return definedAttrs;
  }

  @Override
  public void resolveUsed() {
    final AttributeDefBag inAttrs = predecessors()[0].definedAttributes();
    final List<AttributeDef> outAttrs = definedAttributes();

    final List<AttributeDef> usedAttrs = new ArrayList<>(outAttrs.size());
    final List<AttributeDef> newDefined = new ArrayList<>(outAttrs.size());

    for (AttributeDef outAttr : outAttrs) {
      assert outAttr instanceof DerivedAttributeDef;

      final List<AttributeDef> references;
      final AttributeDef newOutAttr;

      if (outAttr.references() == null) {
        final ASTNode expr = ((DerivedAttributeDef) outAttr).expr();
        references = listMap(gatherColumnRefs(expr), inAttrs::lookup);
        newOutAttr = outAttr;

      } else {
        references = listMap(outAttr.references(), inAttrs::lookup);
        newOutAttr = outAttr.copy();
      }

      usedAttrs.addAll(references);
      newOutAttr.setReferences(references);
      newDefined.add(newOutAttr);
    }

    this.definedAttrs = makeBag(newDefined);
    this.usedAttrs = usedAttrs;
    this.isSelectionUpdated = false;

    // update isWildcard
    if (!isWildcard && successor() != null) isWildcard = definedAttributes().covers(inAttrs);
  }

  @Override
  protected PlanNode copy0() {
    return new ProjNodeImpl(selections, definedAttrs, usedAttrs, isForcedUnique, isWildcard);
  }

  @Override
  public String toString() {
    final List<ASTNode> selections = isSelectionUpdated ? this.selections : this.selections();
    return "Proj%s<%s>".formatted(isForcedUnique ? "'" : "", selections);
  }
}
