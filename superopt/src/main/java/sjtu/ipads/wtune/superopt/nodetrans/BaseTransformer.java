package sjtu.ipads.wtune.superopt.nodetrans;

import com.microsoft.z3.Context;
import org.apache.calcite.jdbc.JavaTypeFactoryImpl;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.rel.type.RelDataTypeSystem;
import org.apache.calcite.rex.RexBuilder;
import org.apache.calcite.sql.type.SqlTypeName;
import sjtu.ipads.wtune.spes.AlgeNode.AlgeNode;
import sjtu.ipads.wtune.sqlparser.plan1.PlanContext;
import sjtu.ipads.wtune.sqlparser.plan1.PlanKind;
import sjtu.ipads.wtune.sqlparser.plan1.PlanNode;

abstract class BaseTransformer implements Transformer{
    static RelDataTypeFactory typeFactory = new JavaTypeFactoryImpl(RelDataTypeSystem.DEFAULT);
    static RexBuilder rexBuilder = new RexBuilder(typeFactory);

    PlanNode planNode;
    PlanContext planCtx;
    Context z3Context;

    @Override
    public void setFields(PlanNode planNode, PlanContext planCtx, Context z3Context) {
        this.planNode = planNode;
        this.planCtx = planCtx;
        this.z3Context = z3Context;
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
        return transformer.transform();
    }

    static RelDataType defaultIntType() {
        return typeFactory.createSqlType(SqlTypeName.INTEGER);
    }
}
