package sjtu.ipads.wtune.sql.plan;

import sjtu.ipads.wtune.sql.ast.SqlContext;
import sjtu.ipads.wtune.sql.ast.SqlNode;
import sjtu.ipads.wtune.sql.ast.SqlNodes;

import java.util.List;

import static sjtu.ipads.wtune.sql.SqlSupport.copyAst;
import static sjtu.ipads.wtune.sql.ast.ExprFields.ColRef_ColName;
import static sjtu.ipads.wtune.sql.ast.SqlNodeFields.ColName_Col;
import static sjtu.ipads.wtune.sql.ast.SqlNodeFields.ColName_Table;
import static sjtu.ipads.wtune.sql.plan.PlanSupport.PLACEHOLDER_NAME;
import static sjtu.ipads.wtune.sql.support.locator.LocatorSupport.gatherColRefs;

public class ExpressionImpl implements Expression {
  private final SqlNode template;
  private final List<SqlNode> internalRefs;
  private final List<SqlNode> colRefs;

  ExpressionImpl(SqlNode ast) {
    final SqlContext tempCtx = SqlContext.mk(8);
    this.template = copyAst(ast, tempCtx);
    this.internalRefs = SqlNodes.mk(tempCtx, gatherColRefs(template));
    this.colRefs = SqlNodes.mk(ast.context(), gatherColRefs(ast));
    // To make a template, we extract all the col-refs, and replace the
    // interpolate "#.#" to the original position.
    // e.g., "t.x > 10" becomes "#.# > 10"
    for (SqlNode colRef : internalRefs) putPlaceholder(colRef);
  }

  ExpressionImpl(SqlNode ast, List<SqlNode> colRefs) {
    this.template = ast;
    this.colRefs = colRefs;
    this.internalRefs = colRefs;
  }

  @Override
  public SqlNode template() {
    return template;
  }

  @Override
  public List<SqlNode> colRefs() {
    return colRefs;
  }

  @Override
  public List<SqlNode> internalRefs() {
    return internalRefs;
  }

  @Override
  public SqlNode interpolate(SqlContext ctx, Values values) {
    if (internalRefs.isEmpty() && (values == null || values.isEmpty()))
      return copyAst(template, ctx);

    if (values == null || internalRefs.size() != values.size())
      throw new PlanException("mismatched # of values during interpolation");

    for (int i = 0, bound = internalRefs.size(); i < bound; i++) {
      final SqlNode colName = internalRefs.get(i).$(ColRef_ColName);
      final Value value = values.get(i);
      colName.$(ColName_Table, value.qualification());
      colName.$(ColName_Col, value.name());
    }

    final SqlNode newAst = copyAst(template, ctx);
    for (SqlNode colRef : internalRefs) putPlaceholder(colRef);
    return newAst;
  }

  @Override
  public String toString() {
    return template.toString();
  }

  private void putPlaceholder(SqlNode colRef) {
    final SqlNode colName = colRef.$(ColRef_ColName);
    colName.$(ColName_Table, PLACEHOLDER_NAME);
    colName.$(ColName_Col, PLACEHOLDER_NAME);
  }
}
