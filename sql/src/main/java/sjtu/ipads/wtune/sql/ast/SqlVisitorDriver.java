package sjtu.ipads.wtune.sql.ast;


import sjtu.ipads.wtune.common.field.FieldKey;

import static sjtu.ipads.wtune.sql.ast.ExprFields.*;
import static sjtu.ipads.wtune.sql.ast.SqlKind.Expr;
import static sjtu.ipads.wtune.sql.ast.SqlKind.TableSource;
import static sjtu.ipads.wtune.sql.ast.SqlNodeFields.*;
import static sjtu.ipads.wtune.sql.ast.TableSourceFields.*;

interface SqlVisitorDriver {
  static boolean enter(SqlNode n, SqlVisitor v) {
    if (n == null) return false;
    if (!v.enter(n)) return false;

    return switch (n.kind()) {
      case Expr -> enterExpr(n, v);
      case TableSource -> enterTableSource(n, v);
      case TableName -> v.enterTableName(n);
      case ColName -> v.enterColumnName(n);
      case CreateTable -> v.enterCreateTable(n);
      case ColDef -> v.enterColumnDef(n);
      case Reference -> v.enterReferences(n);
      case IndexDef -> v.enterIndexDef(n);
      case KeyPart -> v.enterKeyPart(n);
      case WindowSpec -> v.enterWindowSpec(n);
      case WindowFrame -> v.enterWindowFrame(n);
      case FrameBound -> v.enterFrameBound(n);
      case GroupItem -> v.enterGroupItem(n);
      case OrderItem -> v.enterOrderItem(n);
      case SelectItem -> v.enterSelectItem(n);
      case IndexHint -> v.enterIndexHint(n);
      case QuerySpec -> v.enterQuerySpec(n);
      case Query -> v.enterQuery(n);
      case SetOp -> v.enterSetOp(n);
      case Name2 -> v.enterName2(n);
      case Name3 -> v.enterCommonName(n);
      default -> false;
    };

  }

  static void visitChildren(SqlNode n, SqlVisitor v) {
    switch (n.kind()) {
      case Expr -> visitExprChildren(n, v);
      case TableSource -> visitTableSourceChildren(n, v);
      case CreateTable -> {
        safeVisitChild(CreateTable_Name, n, v);
        safeVisitList(CreateTable_Cols, n, v);
        safeVisitList(CreateTable_Cons, n, v);
      }
      case ColDef -> {
        safeVisitChild(ColDef_Name, n, v);
        safeVisitChild(ColDef_Ref, n, v);
      }
      case Reference -> {
        safeVisitChild(Reference_Table, n, v);
        safeVisitList(Reference_Cols, n, v);
      }
      case IndexDef -> {
        safeVisitList(IndexDef_Keys, n, v);
        safeVisitChild(IndexDef_Refs, n, v);
      }
      case WindowSpec -> {
        safeVisitList(WindowSpec_Part, n, v);
        safeVisitList(WindowSpec_Order, n, v);
        safeVisitChild(WindowSpec_Frame, n, v);
      }
      case WindowFrame -> {
        safeVisitChild(WindowFrame_Start, n, v);
        safeVisitChild(WindowFrame_End, n, v);
      }
      case FrameBound -> safeVisitChild(FrameBound_Expr, n, v);
      case OrderItem -> safeVisitChild(OrderItem_Expr, n, v);
      case GroupItem -> safeVisitChild(GroupItem_Expr, n, v);
      case SelectItem -> safeVisitChild(SelectItem_Expr, n, v);
      case QuerySpec -> {
        safeVisitList(QuerySpec_DistinctOn, n, v);
        safeVisitList(QuerySpec_SelectItems, n, v);
        safeVisitChild(QuerySpec_From, n, v);
        safeVisitChild(QuerySpec_Where, n, v);
        safeVisitList(QuerySpec_GroupBy, n, v);
        safeVisitChild(QuerySpec_Having, n, v);
        safeVisitList(QuerySpec_Windows, n, v);
      }
      case Query -> {
        safeVisitChild(Query_Body, n, v);
        safeVisitList(Query_OrderBy, n, v);
        safeVisitChild(Query_Offset, n, v);
        safeVisitChild(Query_Limit, n, v);
      }
      case SetOp -> {
        safeVisitChild(SetOp_Left, n, v);
        safeVisitChild(SetOp_Right, n, v);
      }
    }
  }

  static void leave(SqlNode n, SqlVisitor v) {
    if (n == null) return;
    switch (n.kind()) {
      case Invalid:
        break;

      case Expr:
        leaveExpr(n, v);
        break;

      case TableSource:
        leaveTableSource(n, v);

      case TableName:
        v.leaveTableName(n);
        break;

      case ColName:
        v.leaveColumnName(n);
        break;

      case Name2:
        v.leaveName2(n);
        break;

      case Name3:
        v.leaveCommonName(n);
        return;

      case CreateTable:
        v.leaveCreateTable(n);
        break;

      case ColDef:
        v.leaveColumnDef(n);
        break;

      case Reference:
        v.leaveReferences(n);
        break;

      case IndexDef:
        v.leaveIndexDef(n);
        break;

      case KeyPart:
        v.leaveKeyPart(n);
        break;

      case WindowSpec:
        v.leaveWindowSpec(n);
        break;

      case WindowFrame:
        v.leaveWindowFrame(n);
        break;

      case FrameBound:
        v.leaveFrameBound(n);
        break;

      case OrderItem:
        v.leaveOrderItem(n);
        break;

      case GroupItem:
        v.leaveGroupItem(n);
        break;

      case SelectItem:
        v.leaveSelectItem(n);
        break;

      case IndexHint:
        v.leaveIndexHint(n);
        break;

      case QuerySpec:
        v.leaveQuerySpec(n);
        break;

      case Query:
        v.leaveQuery(n);
        break;

      case SetOp:
        v.leaveSetOp(n);
        break;
    }

    v.leave(n);
  }

  private static void safeAccept(SqlNode n, SqlVisitor v) {
    if (n != null) n.accept(v);
  }

  private static void safeVisitChild(FieldKey<SqlNode> key, SqlNode n, SqlVisitor v) {
    final SqlNode child = n.$(key);
    if (v.enterChild(n, key, child)) safeAccept(child, v);
    v.leaveChild(n, key, child);
  }

  private static void safeVisitList(FieldKey<SqlNodes> key, SqlNode n, SqlVisitor v) {
    final SqlNodes children = n.$(key);
    if (v.enterChildren(n, key, children))
      if (children != null) for (SqlNode child : children) safeAccept(child, v);
    v.leaveChildren(n, key, children);
  }

  private static boolean enterExpr(SqlNode n, SqlVisitor v) {
    assert Expr.isInstance(n);

    switch (n.$(Expr_Kind)) {
      case Variable:
        return v.enterVariable(n);
      case ColRef:
        return v.enterColumnRef(n);
      case Literal:
        return v.enterLiteral(n);
      case FuncCall:
        return v.enterFuncCall(n);
      case Collate:
        return v.enterCollation(n);
      case Param:
        return v.enterParamMarker(n);
      case Unary:
        return v.enterUnary(n);
      case GroupingOp:
        return v.enterGroupingOp(n);
      case Tuple:
        return v.enterTuple(n);
      case Match:
        return v.enterMatch(n);
      case Cast:
        return v.enterCast(n);
      case Symbol:
        return v.enterSymbol(n);
      case Default:
        return v.enterDefault(n);
      case Values:
        return v.enterValues(n);
      case Interval:
        return v.enterInterval(n);
      case Exists:
        return v.enterExists(n);
      case QueryExpr:
        return v.enterQueryExpr(n);
      case Wildcard:
        return v.enterWildcard(n);
      case Aggregate:
        return v.enterAggregate(n);
      case ConvertUsing:
        return v.enterConvertUsing(n);
      case Case:
        return v.enterCase(n);
      case When:
        return v.enterWhen(n);
      case Binary:
        return v.enterBinary(n);
      case Ternary:
        return v.enterTernary(n);
      case Indirection:
        return v.enterIndirection(n);
      case IndirectionComp:
        return v.enterIndirectionComp(n);
      case Array:
        return v.enterArray(n);
      case Unknown:
    }

    return false;
  }

  private static boolean enterTableSource(SqlNode n, SqlVisitor v) {
    assert TableSource.isInstance(n);

    return switch (n.$(TableSource_Kind)) {
      case SimpleSource -> v.enterSimpleTableSource(n);
      case JoinedSource -> v.enterJoinedTableSource(n);
      case DerivedSource -> v.enterDerivedTableSource(n);
    };

  }

  private static void visitExprChildren(SqlNode n, SqlVisitor v) {
    assert Expr.isInstance(n);
    switch (n.$(Expr_Kind)) {
      case Variable -> safeVisitChild(Variable_Assignment, n, v);
      case ColRef -> safeVisitChild(ColRef_ColName, n, v);
      case FuncCall -> safeVisitList(FuncCall_Args, n, v);
      case Collate -> safeVisitChild(Collate_Expr, n, v);
      case Unary -> safeVisitChild(Unary_Expr, n, v);
      case GroupingOp -> safeVisitList(GroupingOp_Exprs, n, v);
      case Tuple -> safeVisitList(Tuple_Exprs, n, v);
      case Match -> {
        safeVisitList(Match_Cols, n, v);
        safeVisitChild(Match_Expr, n, v);
      }
      case Cast -> safeVisitChild(Cast_Expr, n, v);
      case Default -> safeVisitChild(Default_Col, n, v);
      case Values -> safeVisitChild(Values_Expr, n, v);
      case Interval -> safeVisitChild(Interval_Expr, n, v);
      case Exists -> safeVisitChild(Exists_Subquery, n, v);
      case QueryExpr -> safeVisitChild(QueryExpr_Query, n, v);
      case Aggregate -> {
        safeVisitList(Aggregate_Args, n, v);
        safeVisitList(Aggregate_Order, n, v);
        safeVisitChild(Aggregate_WindowSpec, n, v);
      }
      case ConvertUsing -> safeVisitChild(ConvertUsing_Expr, n, v);
      case Case -> {
        safeVisitChild(Case_Cond, n, v);
        safeVisitList(Case_Whens, n, v);
        safeVisitChild(Case_Else, n, v);
      }
      case When -> {
        safeVisitChild(When_Cond, n, v);
        safeVisitChild(When_Expr, n, v);
      }
      case Binary -> {
        safeVisitChild(Binary_Left, n, v);
        safeVisitChild(Binary_Right, n, v);
      }
      case Ternary -> {
        safeVisitChild(Ternary_Left, n, v);
        safeVisitChild(Ternary_Middle, n, v);
        safeVisitChild(Ternary_Right, n, v);
      }
      case Wildcard -> safeVisitChild(Wildcard_Table, n, v);
      case Indirection -> {
        safeVisitChild(Interval_Expr, n, v);
        safeVisitList(Indirection_Comps, n, v);
      }
      case IndirectionComp -> {
        safeVisitChild(IndirectionComp_Start, n, v);
        safeVisitChild(IndirectionComp_End, n, v);
      }
      case Array -> safeVisitList(Array_Elements, n, v);
    }
  }

  private static void visitTableSourceChildren(SqlNode n, SqlVisitor v) {
    assert TableSource.isInstance(n);

    switch (n.$(TableSource_Kind)) {
      case SimpleSource -> {
        safeVisitChild(Simple_Table, n, v);
        safeVisitList(Simple_Hints, n, v);
      }
      case JoinedSource -> {
        safeVisitChild(Joined_Left, n, v);
        safeVisitChild(Joined_Right, n, v);
        safeVisitChild(Joined_On, n, v);
      }
      case DerivedSource -> safeVisitChild(Derived_Subquery, n, v);
    }
  }

  private static void leaveExpr(SqlNode n, SqlVisitor v) {
    assert Expr.isInstance(n);

    switch (n.$(Expr_Kind)) {
      case Variable:
        v.leaveColumnDef(n);
        return;

      case ColRef:
        v.leaveColumnRef(n);
        return;

      case FuncCall:
        v.leaveFuncCall(n);

      case Literal:
        v.leaveLiteral(n);
        return;

      case Collate:
        v.leaveCollation(n);
        return;

      case Param:
        v.leaveParamMarker(n);
        return;

      case Unary:
        v.leaveUnary(n);
        return;

      case GroupingOp:
        v.leaveGroupingOp(n);
        return;

      case Tuple:
        v.leaveTuple(n);
        return;

      case Match:
        v.leaveMatch(n);
        return;

      case Cast:
        v.leaveCast(n);
        return;

      case Symbol:
        v.leaveSymbol(n);
        return;

      case Default:
        v.leaveDefault(n);
        return;

      case Values:
        v.leaveValues(n);
        return;

      case Interval:
        v.leaveInterval(n);
        return;

      case Exists:
        v.leaveExists(n);
        return;

      case QueryExpr:
        v.leaveQueryExpr(n);
        return;

      case Wildcard:
        v.leaveWildcard(n);
        return;

      case Aggregate:
        v.leaveAggregate(n);
        return;

      case ConvertUsing:
        v.leaveConvertUsing(n);
        return;

      case Case:
        v.leaveCase(n);
        return;

      case When:
        v.leaveWhen(n);
        return;

      case Binary:
        v.leaveBinary(n);
        return;

      case Ternary:
        v.leaveTernary(n);
        return;

      case Indirection:
        v.leaveIndirection(n);
        return;

      case IndirectionComp:
        v.leaveIndirectionComp(n);
        return;

      case Array:
        v.leaveArray(n);
        return;

      case Unknown:
    }
  }

  private static void leaveTableSource(SqlNode n, SqlVisitor v) {
    assert TableSource.isInstance(n);

    switch (n.$(TableSource_Kind)) {
      case SimpleSource -> v.leaveSimpleTableSource(n);
      case JoinedSource -> v.leaveJoinedTableSource(n);
      case DerivedSource -> v.leaveDerivedTableSource(n);
    }
  }
}
