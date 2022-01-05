package sjtu.ipads.wtune.sql.ast;

import sjtu.ipads.wtune.common.field.FieldKey;
import sjtu.ipads.wtune.sql.ast.constants.*;

import java.util.EnumSet;
import java.util.List;

import static sjtu.ipads.wtune.sql.ast.SqlKind.*;

public interface SqlNodeFields { // also serves as a marker interface
  //// TableName
  FieldKey<String> TableName_Schema = TableName.textField("Schema");
  FieldKey<String> TableName_Table = TableName.textField("Table");

  //// ColumnName
  FieldKey<String> ColName_Schema = ColName.textField("Schema");
  FieldKey<String> ColName_Table = ColName.textField("Table");
  FieldKey<String> ColName_Col = ColName.textField("Column");

  //// CommonName2
  FieldKey<String> Name2_0 = Name2.textField("Part0");
  FieldKey<String> Name2_1 = Name2.textField("Part1");

  //// CommonName3
  FieldKey<String> Name3_0 = Name3.textField("Part0");
  FieldKey<String> Name3_1 = Name3.textField("Part1");
  FieldKey<String> Name3_2 = Name3.textField("Part2");

  //// CreateTable
  FieldKey<SqlNode> CreateTable_Name = CreateTable.nodeField("Name"); // TableName
  FieldKey<SqlNodes> CreateTable_Cols = CreateTable.nodesField("Columns"); // ColDef
  FieldKey<SqlNodes> CreateTable_Cons = CreateTable.nodesField("Constraints"); // IndexDef
  FieldKey<String> CreateTable_Engine = CreateTable.textField("Engine");

  //// ColumnDef
  FieldKey<SqlNode> ColDef_Name = ColDef.nodeField("Name"); // ColName
  FieldKey<String> ColDef_RawType = ColDef.textField("TypeRaw");
  FieldKey<SqlDataType> ColDef_DataType = ColDef.field("DataType", SqlDataType.class);
  FieldKey<EnumSet<ConstraintKind>> ColDef_Cons = ColDef.field("Constraint", EnumSet.class);
  FieldKey<SqlNode> ColDef_Ref = ColDef.nodeField("References"); // References
  FieldKey<Boolean> ColDef_Generated = ColDef.boolField("Genearted");
  FieldKey<Boolean> ColDef_Default = ColDef.boolField("Default");
  FieldKey<Boolean> ColDef_AutoInc = ColDef.boolField("AutoInc");

  //// References
  FieldKey<SqlNode> Reference_Table = Reference.nodeField("Table"); // TableName
  FieldKey<SqlNodes> Reference_Cols = Reference.nodesField("Columns"); // ColName

  //// IndexDef
  FieldKey<String> IndexDef_Name = IndexDef.textField("Name");
  FieldKey<SqlNode> IndexDef_Table = IndexDef.nodeField("Table"); // TableName
  FieldKey<IndexKind> IndexDef_Kind = IndexDef.field("Kind", IndexKind.class);
  FieldKey<ConstraintKind> IndexDef_Cons = IndexDef.field("Constraint", ConstraintKind.class);
  FieldKey<SqlNodes> IndexDef_Keys = IndexDef.nodesField("Keys"); // KeyPart
  FieldKey<SqlNode> IndexDef_Refs = IndexDef.nodeField("References"); // References

  //// KeyPart
  FieldKey<String> KeyPart_Col = KeyPart.textField("Column");
  FieldKey<Integer> KeyPart_Len = KeyPart.field("Length", Integer.class);
  FieldKey<SqlNode> KeyPart_Expr = KeyPart.nodeField("Expr"); // Expr
  FieldKey<KeyDirection> KeyPart_Direction = KeyPart.field("Direction", KeyDirection.class);

  //// Union
  FieldKey<SqlNode> SetOp_Left = SetOp.nodeField("Left"); // Query
  FieldKey<SqlNode> SetOp_Right = SetOp.nodeField("Right"); // Query
  FieldKey<SetOpKind> SetOp_Kind = SetOp.field("Kind", SetOpKind.class);
  FieldKey<SetOpOption> SetOp_Option = SetOp.field("Option", SetOpOption.class);

  //// Query
  FieldKey<SqlNode> Query_Body = Query.nodeField("Body"); // QuerySpec
  FieldKey<SqlNodes> Query_OrderBy = Query.nodesField("OrderBy"); // OrderItem
  FieldKey<SqlNode> Query_Limit = Query.nodeField("Limit"); // Offset
  FieldKey<SqlNode> Query_Offset = Query.nodeField("Offset"); // Offset

  //// QuerySpec
  FieldKey<Boolean> QuerySpec_Distinct = QuerySpec.boolField("Distinct");
  FieldKey<SqlNodes> QuerySpec_DistinctOn = QuerySpec.nodesField("DistinctOn"); // Expr
  FieldKey<SqlNodes> QuerySpec_SelectItems = QuerySpec.nodesField("SelectItem"); // SelectItem
  FieldKey<SqlNode> QuerySpec_From = QuerySpec.nodeField("From"); // TableSource
  FieldKey<SqlNode> QuerySpec_Where = QuerySpec.nodeField("Where"); // Expr
  FieldKey<SqlNodes> QuerySpec_GroupBy = QuerySpec.nodesField("GroupBy"); // GroupItem
  FieldKey<OLAPOption> QuerySpec_OlapOption = QuerySpec.field("OlapOption", OLAPOption.class);
  FieldKey<SqlNode> QuerySpec_Having = QuerySpec.nodeField("Having"); // Expr
  FieldKey<SqlNodes> QuerySpec_Windows = QuerySpec.nodesField("Windows"); // WindowSpec

  //// SelectItem
  FieldKey<SqlNode> SelectItem_Expr = SelectItem.nodeField("Expr"); // Expr
  FieldKey<String> SelectItem_Alias = SelectItem.textField("Alias");

  //// OrderItem
  FieldKey<SqlNode> OrderItem_Expr = OrderItem.nodeField("Expr"); // Expr
  FieldKey<KeyDirection> OrderItem_Direction = OrderItem.field("Direction", KeyDirection.class);

  //// GroupItem
  FieldKey<SqlNode> GroupItem_Expr = GroupItem.nodeField("Expr"); // Expr

  //// WindowSpec
  FieldKey<String> WindowSpec_Alias = WindowSpec.textField("Alias");
  FieldKey<String> WindowSpec_Name = WindowSpec.textField("Name");
  FieldKey<SqlNodes> WindowSpec_Part = WindowSpec.nodesField("Partition"); // WindowSpec
  FieldKey<SqlNodes> WindowSpec_Order = WindowSpec.nodesField("Order"); // OrderItem
  FieldKey<SqlNode> WindowSpec_Frame = WindowSpec.nodeField("Frame"); // WindowFrame

  //// WindowFrame
  FieldKey<WindowUnit> WindowFrame_Unit = WindowFrame.field("Unit", WindowUnit.class);
  FieldKey<SqlNode> WindowFrame_Start = WindowFrame.nodeField("Start"); // FrameBound
  FieldKey<SqlNode> WindowFrame_End = WindowFrame.nodeField("End"); // FrameBound
  FieldKey<WindowExclusion> WindowFrame_Exclusion =
      WindowFrame.field("Exclusion", WindowExclusion.class);

  //// FrameBound
  FieldKey<SqlNode> FrameBound_Expr = FrameBound.nodeField("Expr"); // Expr
  FieldKey<FrameBoundDirection> FrameBound_Direction =
      FrameBound.field("Direction", FrameBoundDirection.class);

  //// IndexHint
  FieldKey<IndexHintType> IndexHint_Kind = IndexHint.field("Kind", IndexHintType.class);
  FieldKey<IndexHintTarget> IndexHint_Target = IndexHint.field("Target", IndexHintTarget.class);
  FieldKey<List<String>> IndexHint_Names = IndexHint.field("Names", List.class);

  //// Statement
  FieldKey<StmtType> Statement_Kind = Statement.field("Kind", StmtType.class);
  FieldKey<SqlNode> Statement_Body = Statement.nodeField("Body"); // Any

  //// AlterSequence
  FieldKey<SqlNode> AlterSeq_Name = AlterSeq.nodeField("Name"); // Name2
  FieldKey<String> AlterSeq_Op = AlterSeq.textField("Op");
  FieldKey<Object> AlterSeq_Payload = AlterSeq.field("Payload", Object.class);

  //// AlterTable
  FieldKey<SqlNode> AlterTable_Name = AlterTable.nodeField("Name");
  FieldKey<SqlNodes> AlterTable_Actions = AlterTable.nodesField("Actions");

  //// AlterTableAction
  FieldKey<String> AlterTableAction_Name = AlterTableAction.textField("Name");
  FieldKey<Object> AlterTableAction_Payload = AlterTableAction.field("Payload", Object.class);

  //// Expr
  FieldKey<ExprKind> Expr_Kind = Expr.field("Kind", ExprKind.class);
  // for named argument in PG
  FieldKey<String> Expr_ArgName = Expr.textField("ArgName");
  FieldKey<Boolean> Expr_FuncArgVariadic = Expr.boolField("Variadic");

  //// TableSource
  FieldKey<TableSourceKind> TableSource_Kind = TableSource.field("Kind", TableSourceKind.class);
}
