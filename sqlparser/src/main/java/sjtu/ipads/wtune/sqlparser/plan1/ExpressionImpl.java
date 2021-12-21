package sjtu.ipads.wtune.sqlparser.plan1;

import sjtu.ipads.wtune.sqlparser.ast1.SqlContext;
import sjtu.ipads.wtune.sqlparser.ast1.SqlNode;

import java.util.List;

import static sjtu.ipads.wtune.sqlparser.SqlSupport.copyAst;
import static sjtu.ipads.wtune.sqlparser.ast1.ExprFields.ColRef_ColName;
import static sjtu.ipads.wtune.sqlparser.ast1.SqlNodeFields.ColName_Col;
import static sjtu.ipads.wtune.sqlparser.ast1.SqlNodeFields.ColName_Table;
import static sjtu.ipads.wtune.sqlparser.plan1.PlanSupport.PLACEHOLDER_NAME;
import static sjtu.ipads.wtune.sqlparser.util.ColRefGatherer.gatherColRefs;

public class ExpressionImpl implements Expression {
  private final SqlNode template;
  private final List<SqlNode> colRefs;

  ExpressionImpl(SqlNode ast) {
    this.template = ast;

    // To make a template, we extract all the col-refs, and replace the
    // interpolate "#.#" to the original position.
    // e.g., "t.x > 10" becomes "#.# > 10"
    final List<SqlNode> colRefs = gatherColRefs(ast);
    final SqlContext tempCtx = SqlContext.mk(colRefs.size() * 2);
    for (int i = 0, bound = colRefs.size(); i < bound; i++) {
      final SqlNode colRef = colRefs.get(i);
      colRefs.set(i, copyAst(colRef, tempCtx));

      final SqlNode colName = colRef.$(ColRef_ColName);
      colName.$(ColName_Table, PLACEHOLDER_NAME);
      colName.$(ColName_Col, PLACEHOLDER_NAME);
    }

    this.colRefs = colRefs;
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
  public SqlNode interpolate(SqlContext ctx, Values values) {
    final SqlNode newAst = copyAst(template, ctx);
    final List<SqlNode> colRefs = gatherColRefs(newAst);

    if (colRefs.size() != values.size())
      throw new PlanException("mismatched # of values during interpolation");

    for (int i = 0, bound = colRefs.size(); i < bound; i++) {
      final SqlNode colName = colRefs.get(i).$(ColRef_ColName);
      final Value value = values.get(i);
      colName.$(ColName_Table, value.qualification());
      colName.$(ColName_Col, value.name());
    }

    return newAst;
  }

  @Override
  public String toString() {
    return template.toString();
  }
}
