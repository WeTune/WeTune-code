package sjtu.ipads.wtune.superopt.nodetrans;

import com.microsoft.z3.Context;
import org.apache.calcite.jdbc.JavaTypeFactoryImpl;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.rel.type.RelDataTypeSystem;
import org.apache.calcite.rex.RexBuilder;
import org.apache.calcite.sql.SqlFunction;
import org.apache.calcite.sql.SqlFunctionCategory;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.type.OperandTypes;
import org.apache.calcite.sql.type.ReturnTypes;
import org.apache.calcite.sql.type.SqlTypeName;
import sjtu.ipads.wtune.spes.AlgeNode.AlgeNode;
import sjtu.ipads.wtune.sqlparser.plan.PlanContext;
import sjtu.ipads.wtune.sqlparser.plan.PlanKind;
import sjtu.ipads.wtune.sqlparser.plan.PlanNode;

import java.util.ArrayList;
import java.util.List;

abstract class BaseTransformer implements Transformer{
  static RelDataTypeFactory typeFactory = new JavaTypeFactoryImpl(RelDataTypeSystem.DEFAULT);
  static RexBuilder rexBuilder = new RexBuilder(typeFactory);

  static List<SqlFunction> registeredPredicates = new ArrayList<>();

  PlanNode planNode;
  PlanContext planCtx;
  Context z3Context;

  @Override
  public void setFields(PlanNode planNode, PlanContext planCtx, Context z3Context) {
    this.planNode = planNode;
    this.planCtx = planCtx;
    this.z3Context = z3Context;
  }

  @Override
  public void dropFields() {
    this.planNode = null;
    this.planCtx = null;
    this.z3Context = null;
  }

  static Transformer getTransformer(PlanKind kind) {
    return switch (kind) {
      case Input -> new InputTransformer();
      case Proj -> new ProjTransformer();
      case Filter -> new SimpleFilterTransformer();
      case Join -> new JoinTransformer();
      case SetOp -> new UnionTransformer();
      case InSub -> new InSubFilterTransformer();
      case Agg -> new AggTranformer();
      default -> throw new UnsupportedOperationException(
          "Unsupported operator type for AlgeNode: " + kind);
    };
  }

  static AlgeNode transformNode(PlanNode planNode, PlanContext planCtx, Context z3Context) {
    Transformer transformer = getTransformer(planNode.kind());
    transformer.setFields(planNode, planCtx, z3Context);
    AlgeNode node = transformer.transform();
    transformer.dropFields();
    return node;
  }

  static RelDataType defaultIntType() {
    return typeFactory.createSqlType(SqlTypeName.INTEGER);
  }

  static void resetEnv() {
    registeredPredicates.clear();
  }

  static SqlFunction getOrCreatePred(String predName) {
    for (SqlFunction existingPred : registeredPredicates) {
      if (existingPred.getName().equals(predName))
        return existingPred;
    }

    SqlFunction udfPred = createUninterpretedFunction(predName);
    registeredPredicates.add(udfPred);
    return udfPred;
  }

  private static SqlFunction createUninterpretedFunction(String funcName) {
    return new SqlFunction(
        funcName,
        SqlKind.OTHER_FUNCTION,
        ReturnTypes.BOOLEAN,
        null,
        OperandTypes.INTEGER,
        SqlFunctionCategory.USER_DEFINED_FUNCTION);
  }
}
