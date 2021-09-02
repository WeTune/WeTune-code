package sjtu.ipads.wtune.sqlparser.plan;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.schema.Schema;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import static sjtu.ipads.wtune.common.utils.FuncUtils.zipForEach;
import static sjtu.ipads.wtune.common.utils.TreeNode.copyTree;
import static sjtu.ipads.wtune.sqlparser.plan.ValueBag.locateValue;
import static sjtu.ipads.wtune.sqlparser.plan.ValueBag.locateValueRelaxed;

public interface PlanSupport {
  static ASTNode translateAsAst(PlanNode plan) {
    return AstTranslator.translate(plan);
  }

  static PlanNode buildPlan(ASTNode ast, Schema schema) {
    return PlanBuilder.buildPlan(ast, schema == null ? ast.context().schema() : schema);
  }

  static PlanNode assemblePlan(ASTNode ast, Schema schema) {
    if (schema == null) schema = ast.context().schema();

    final PlanNode plan = PlanBuilder.buildPlan(ast, schema);
    RefResolver.resolve(plan);
    return plan;
  }

  static PlanNode resolvePlan(PlanNode plan) {
    RefResolver.resolve(plan);
    return plan;
  }

  static PlanNode resolveSubqueryExpr(PlanNode plan) {
    RefResolver.resolveSubqueryExpr(plan);
    return plan;
  }

  static PlanNode disambiguate(PlanNode plan) {
    return new Disambiguation(plan).disambiguate();
  }

  static PlanNode wrapWildcardProj(PlanNode plan) {
    // wrap a fragment plan with a outer Proj
    final ProjNode proj = ProjNode.mkWildcard(plan.values());
    final PlanContext ctx = PlanContext.mk(plan.context().schema());

    proj.setContext(ctx);
    proj.setPredecessor(0, copyTree(plan, ctx));
    ctx.registerRefs(proj, proj.refs());
    ctx.registerValues(proj, proj.values());
    zipForEach(proj.refs(), plan.values(), ctx::setRef);

    return proj;
  }

  static boolean isDependentRef(Ref ref, PlanContext ctx) {
    final Value v = ctx.deRef(ref);
    if (v == null) throw new IllegalArgumentException("cannot resolve ref " + ref);

    final PlanNode vOwner = ctx.ownerOf(v);
    final PlanNode rOwner = ctx.ownerOf(ref);

    PlanNode path = vOwner;
    while (path != null) {
      if (path == rOwner) return false;
      path = path.successor();
    }

    return true;
  }

  static boolean isWildcardProj(ProjNode proj) {
    final PlanContext ctx = proj.context();
    final ValueBag inputs = proj.predecessors()[0].values();
    final ValueBag outputs = proj.values();
    if (inputs.size() != outputs.size()) return false;

    for (int i = 0, bound = inputs.size(); i < bound; i++) {
      final Value input = inputs.get(i);
      final Expr output = outputs.get(i).expr();
      if (output.isIdentity() && ctx.deRef(output.refs().get(0)) != input) return false;
    }

    return true;
  }

  static List<Value> bindValues(List<Value> values, PlanNode predecessor) {
    if (values.isEmpty()) return values;

    final List<Value> boundValues = new ArrayList<>(values.size());
    final PlanContext ctx = predecessor.context();
    final ValueBag lookup = predecessor.values();

    boolean changed = false;
    for (Value value : values) {
      final Value boundValue = locateValue(lookup, value, ctx);
      if (boundValue == null) throw new NoSuchElementException("cannot bind value: " + value);
      boundValues.add(boundValue);
      changed |= boundValue != value;
    }

    return changed ? boundValues : values;
  }

  static List<Value> bindValuesRelaxed(
      List<Value> values, PlanContext refCtx, PlanNode predecessor) {
    if (values.isEmpty() || refCtx == null) return values;

    final List<Value> boundValues = new ArrayList<>(values.size());
    final PlanContext lookupCtx = predecessor.context();
    final ValueBag lookup = predecessor.values();

    boolean changed = false;
    for (Value value : values) {
      Value src = lookupCtx.redirect(value);
      if (src == value) src = refCtx.sourceOf(value);

      final Value found;
      if ((found = locateValueRelaxed(lookup, src, lookupCtx)) == null)
        throw new NoSuchElementException("cannot bind value: " + value);
      boundValues.add(found);
      changed |= found != value;
    }

    return changed ? boundValues : values;
  }
}
