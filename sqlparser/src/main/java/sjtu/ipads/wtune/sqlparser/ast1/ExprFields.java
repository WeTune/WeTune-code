package sjtu.ipads.wtune.sqlparser.ast1;

import sjtu.ipads.wtune.common.field.FieldKey;
import sjtu.ipads.wtune.sqlparser.ast1.constants.*;

import static sjtu.ipads.wtune.sqlparser.ast1.ExprKind.*;
import static sjtu.ipads.wtune.sqlparser.ast1.SqlKind.Expr;
import static sjtu.ipads.wtune.sqlparser.ast1.SqlNodeFields.Expr_Kind;


public interface ExprFields {
  // Variable
  FieldKey<VariableScope> Variable_Scope = Variable.field("Scope", VariableScope.class);
  FieldKey<String> Variable_Name = Variable.textField("Name");
  FieldKey<SqlNode> Variable_Assignment = Variable.nodeField("Assignment"); // Expr
  // Col Ref
  FieldKey<SqlNode> ColRef_ColName = ColRef.nodeField("Column"); // ColName
  // Func Call
  FieldKey<SqlNode> FuncCall_Name = FuncCall.nodeField("Name"); // Name2
  FieldKey<SqlNodes> FuncCall_Args = FuncCall.nodesField("Args"); // Expr
  // Collate
  FieldKey<SqlNode> Collate_Expr = Collate.nodeField("Expr"); // Expr
  FieldKey<SqlNode> Collate_Collation = Collate.nodeField("Collation"); // Symbol
  // Interval
  FieldKey<SqlNode> Interval_Expr = Interval.nodeField("Expr");// Expr
  FieldKey<IntervalUnit> Interval_Unit = Interval.field("Unit", IntervalUnit.class);
  // Symbol
  FieldKey<String> Symbol_Text = Symbol.textField("Text");
  // Literal
  FieldKey<LiteralKind> Literal_Kind = Literal.field("Kind", LiteralKind.class);
  FieldKey<Object> Literal_Value = Literal.field("Value", Object.class);
  FieldKey<String> Literal_Unit = Literal.textField("Unit");
  // Aggregate
  FieldKey<String> Aggregate_Name = Aggregate.textField("Name");
  FieldKey<Boolean> Aggregate_Distinct = Aggregate.boolField("Distinct");
  FieldKey<SqlNodes> Aggregate_Args = Aggregate.nodesField("Args"); // Expr
  FieldKey<String> Aggregate_WindowName = Aggregate.textField("WindowName");
  FieldKey<SqlNode> Aggregate_WindowSpec = Aggregate.nodeField("WindowSpec"); // WindowSpec
  FieldKey<SqlNode> Aggregate_Filter = Aggregate.nodeField("Filter"); // Expr
  FieldKey<SqlNodes> Aggregate_WithinGroupOrder = Aggregate.nodesField("WithinGroupOrder"); // Grouping
  FieldKey<SqlNodes> Aggregate_Order = Aggregate.nodesField("Order");
  FieldKey<String> Aggregate_Sep = Aggregate.textField("Sep");
  // Wildcard
  FieldKey<SqlNode> Wildcard_Table = Wildcard.nodeField("Table"); // TableName
  // Grouping
  FieldKey<SqlNodes> GroupingOp_Exprs = GroupingOp.nodesField("Exprs"); // Expr
  // Unary
  FieldKey<UnaryOpKind> Unary_Op = Unary.field("Op", UnaryOpKind.class);
  FieldKey<SqlNode> Unary_Expr = Unary.nodeField("Expr"); // Expr
  // Binary
  FieldKey<BinaryOpKind> Binary_Op = Binary.field("Op", BinaryOpKind.class);
  FieldKey<SqlNode> Binary_Left = Binary.nodeField("Left");
  FieldKey<SqlNode> Binary_Right = Binary.nodeField("Right");
  FieldKey<SubqueryOption> Binary_SubqueryOption =
      Binary.field("SubqueryOption", SubqueryOption.class);
  // Ternary
  FieldKey<TernaryOp> Ternary_Op = Ternary.field("Op", TernaryOp.class);
  FieldKey<SqlNode> Ternary_Left = Ternary.nodeField("Left"); // Expr
  FieldKey<SqlNode> Ternary_Middle = Ternary.nodeField("Middle"); // Expr
  FieldKey<SqlNode> Ternary_Right = Ternary.nodeField("Right"); // Expr
  // Tuple
  FieldKey<SqlNodes> Tuple_Exprs = Tuple.nodesField("Exprs"); // Expr
  FieldKey<Boolean> Tuple_AsRow = Tuple.boolField("AsRow");
  // Exists
  FieldKey<SqlNode> Exists_Subquery = Exists.nodeField("Subquery"); // QueryExpr
  // MatchAgainst
  FieldKey<SqlNodes> Match_Cols = Match.nodesField("Columns"); // ColRef
  FieldKey<SqlNode> Match_Expr = Match.nodeField("Expr"); // Expr
  FieldKey<MatchOption> Match_Option = Match.field("Option", MatchOption.class);
  // Cast
  FieldKey<SqlNode> Cast_Expr = Cast.nodeField("Expr"); // Expr
  FieldKey<SqlDataType> Cast_Type = Cast.field("Type", SqlDataType.class);
  FieldKey<Boolean> Cast_IsArray = Cast.boolField("IsArray");
  // Case
  FieldKey<SqlNode> Case_Cond = Case.nodeField("Condition"); // Expr
  FieldKey<SqlNodes> Case_Whens = Case.nodesField("When"); // When
  FieldKey<SqlNode> Case_Else = Case.nodeField("Else"); // Expr
  // When
  FieldKey<SqlNode> When_Cond = When.nodeField("Condition"); // Expr
  FieldKey<SqlNode> When_Expr = When.nodeField("Expr"); // Expr
  // ConvertUsing
  FieldKey<SqlNode> ConvertUsing_Expr = ConvertUsing.nodeField("Expr"); // Expr
  FieldKey<SqlNode> ConvertUsing_Charset = ConvertUsing.nodeField("Charset"); // Symbol
  // Default
  FieldKey<SqlNode> Default_Col = Default.nodeField("Col"); // Expr
  // Values
  FieldKey<SqlNode> Values_Expr = Values.nodeField("Expr"); // Expr
  // QueryExpr
  FieldKey<SqlNode> QueryExpr_Query = QueryExpr.nodeField("Query"); // Query
  // Indirection
  FieldKey<SqlNode> Indirection_Expr = Indirection.nodeField("Expr"); // Expr
  FieldKey<SqlNodes> Indirection_Comps = Indirection.nodesField("Comps"); // IndirectionComp
  // IndirectionComp
  FieldKey<Boolean> IndirectionComp_Subscript = IndirectionComp.boolField("Subscript");
  FieldKey<SqlNode> IndirectionComp_Start = IndirectionComp.nodeField("Start"); // Expr
  FieldKey<SqlNode> IndirectionComp_End = IndirectionComp.nodeField("End"); // Expr
  // Param
  FieldKey<Integer> Param_Number = Param.field("Number", Integer.class);
  FieldKey<Boolean> Param_ForceQuestion = Param.boolField("ForceQuestion");
  // ComparisonMod
  FieldKey<SubqueryOption> ComparisonMod_Option =
      ComparisonMod.field("Option", SubqueryOption.class);
  FieldKey<SqlNode> ComparisonMod_Expr = ComparisonMod.nodeField("Expr"); // Expr
  // Array
  FieldKey<SqlNodes> Array_Elements = Array.nodesField("Elements"); // Expr
  // TypeCoercion
  FieldKey<SqlDataType> TypeCoercion_Type = TypeCoercion.field("Type", SqlDataType.class);
  FieldKey<String> TypeCoercion_String = TypeCoercion.textField("RawType");
  // DataTimeOverlap
  FieldKey<SqlNode> DateTimeOverlap_LeftStart = DateTimeOverlap.nodeField("LeftStart"); // Expr
  FieldKey<SqlNode> DateTimeOverlap_LeftEnd = DateTimeOverlap.nodeField("LeftEnd"); // Expr
  FieldKey<SqlNode> DateTimeOverlap_RightStart = DateTimeOverlap.nodeField("RightStart"); // Expr
  FieldKey<SqlNode> DateTimeOverlap_RightEnd = DateTimeOverlap.nodeField("RightEnd"); // Expr

  static int getOperatorPrecedence(SqlNode node) {
    if (!Expr.isInstance(node)) return -1;
    final ExprKind exprKind = node.$(Expr_Kind);
    return switch (exprKind) {
      case Unary -> node.$(Unary_Op).precedence();
      case Binary -> node.$(Binary_Op).precedence();
      case Ternary -> node.$(Ternary_Op).precedence();
      case Case, When -> 5;
      case Collate -> 13;
      case Interval -> 14;
      default -> -1;
    };
  }
}
