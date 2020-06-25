package sjtu.ipads.wtune.sqlparser;

public abstract class SQLVisitorAdapter implements SQLVisitor {
  @Override
  public boolean enter(SQLNode node) {
    return true;
  }

  @Override
  public void leave(SQLNode node) {}

  @Override
  public boolean enterCreateTable(SQLNode createTable) {
    return true;
  }

  @Override
  public void leaveCreateTable(SQLNode createTable) {}

  @Override
  public boolean enterTableName(SQLNode tableName) {
    return true;
  }

  @Override
  public void leaveTableName(SQLNode tableName) {}

  @Override
  public boolean enterColumnDef(SQLNode colDef) {
    return true;
  }

  @Override
  public void leaveColumnDef(SQLNode colDef) {}

  @Override
  public boolean enterReferences(SQLNode ref) {
    return true;
  }

  @Override
  public void leaveReferences(SQLNode ref) {}

  @Override
  public boolean enterColumnName(SQLNode colName) {
    return true;
  }

  @Override
  public void leaveColumnName(SQLNode colName) {}

  @Override
  public boolean enterIndexDef(SQLNode indexDef) {
    return true;
  }

  @Override
  public void leaveIndexDef(SQLNode indexDef) {}

  @Override
  public boolean enterKeyPart(SQLNode keyPart) {
    return true;
  }

  @Override
  public void leaveKeyPart(SQLNode keyPart) {}

  @Override
  public boolean enterVariable(SQLNode variable) {
    return true;
  }

  @Override
  public void leaveVariable(SQLNode variable) {}

  @Override
  public boolean enterColumnRef(SQLNode columnRef) {
    return true;
  }

  @Override
  public void leaveColumnRef(SQLNode columnRef) {}

  @Override
  public boolean enterLiteral(SQLNode literal) {
    return true;
  }

  @Override
  public void leaveLiteral(SQLNode literal) {}

  @Override
  public boolean enterFuncCall(SQLNode funcCall) {
    return true;
  }

  @Override
  public void leaveFuncCall(SQLNode funcCall) {}

  @Override
  public boolean enterCollation(SQLNode collation) {
    return true;
  }

  @Override
  public void leaveCollation(SQLNode collation) {}

  @Override
  public boolean enterParamMarker(SQLNode paramMarker) {
    return true;
  }

  @Override
  public void leaveParamMarker(SQLNode paramMarker) {}

  @Override
  public boolean enterUnary(SQLNode unary) {
    return true;
  }

  @Override
  public void leaveUnary(SQLNode unary) {}

  @Override
  public boolean enterGroupingOp(SQLNode groupingOp) {
    return true;
  }

  @Override
  public void leaveGroupingOp(SQLNode groupingOp) {}

  @Override
  public boolean enterTuple(SQLNode tuple) {
    return true;
  }

  @Override
  public void leaveTuple(SQLNode tuple) {}

  @Override
  public boolean enterMatch(SQLNode match) {
    return true;
  }

  @Override
  public void leaveMatch(SQLNode match) {}

  @Override
  public boolean enterCast(SQLNode cast) {
    return true;
  }

  @Override
  public void leaveCast(SQLNode cast) {}

  @Override
  public boolean enterSymbol(SQLNode symbol) {
    return true;
  }

  @Override
  public void leaveSymbol(SQLNode symbol) {}

  @Override
  public boolean enterDefault(SQLNode _default) {
    return true;
  }

  @Override
  public void leaveDefault(SQLNode _default) {}

  @Override
  public boolean enterValues(SQLNode values) {
    return true;
  }

  @Override
  public void leaveValues(SQLNode values) {}

  @Override
  public boolean enterInterval(SQLNode interval) {
    return true;
  }

  @Override
  public void leaveInterval(SQLNode interval) {}

  @Override
  public boolean enterExists(SQLNode exists) {
    return true;
  }

  @Override
  public void leaveExists(SQLNode exists) {}

  @Override
  public boolean enterQueryExpr(SQLNode queryExpr) {
    return true;
  }

  @Override
  public void leaveQueryExpr(SQLNode queryExpr) {}

  @Override
  public boolean enterWildcard(SQLNode wildcard) {
    return true;
  }

  @Override
  public void leaveWildcard(SQLNode wildcard) {}

  @Override
  public boolean enterAggregate(SQLNode aggregate) {
    return true;
  }

  @Override
  public void leaveAggregate(SQLNode aggregate) {}

  @Override
  public boolean enterConvertUsing(SQLNode convertUsing) {
    return true;
  }

  @Override
  public void leaveConvertUsing(SQLNode convertUsing) {}

  @Override
  public boolean enterCase(SQLNode _case) {
    return true;
  }

  @Override
  public void leaveCase(SQLNode _case) {}

  @Override
  public boolean enterWhen(SQLNode when) {
    return true;
  }

  @Override
  public void leaveWhen(SQLNode when) {}

  @Override
  public boolean enterBinary(SQLNode binary) {
    return true;
  }

  @Override
  public void leaveBinary(SQLNode binary) {}

  @Override
  public boolean enterFrameBound(SQLNode frameBound) {
    return true;
  }

  @Override
  public void leaveFrameBound(SQLNode frameBound) {}

  @Override
  public boolean enterWindowFrame(SQLNode windowFrame) {
    return true;
  }

  @Override
  public void leaveWindowFrame(SQLNode windowFrame) {}

  @Override
  public boolean enterWindowSpec(SQLNode windowSpec) {
    return true;
  }

  @Override
  public void leaveWindowSpec(SQLNode windowSpec) {}

  @Override
  public boolean enterOrderItem(SQLNode orderItem) {
    return true;
  }

  @Override
  public void leaveOrderItem(SQLNode orderItem) {}

  @Override
  public boolean enterTernary(SQLNode ternary) {
    return true;
  }

  @Override
  public void leaveTernary(SQLNode ternary) {}

  @Override
  public boolean enterSelectItem(SQLNode selectItem) {
    return true;
  }

  @Override
  public void leaveSelectItem(SQLNode selectItem) {}

  @Override
  public boolean enterIndexHint(SQLNode indexHint) {
    return true;
  }

  @Override
  public void leaveIndexHint(SQLNode indexHint) {}

  @Override
  public boolean enterSimpleTableSource(SQLNode simpleTableSource) {
    return true;
  }

  @Override
  public void leaveSimpleTableSource(SQLNode simpleTableSource) {}

  @Override
  public boolean enterDerivedTableSource(SQLNode derivedTableSource) {
    return true;
  }

  @Override
  public void leaveDerivedTableSource(SQLNode derivedTableSource) {}

  @Override
  public boolean enterJoinedTableSource(SQLNode joinedTableSource) {
    return true;
  }

  @Override
  public void leaveJoinedTableSource(SQLNode joinedTableSource) {}

  @Override
  public boolean enterQuery(SQLNode query) {
    return true;
  }

  @Override
  public void leaveQuery(SQLNode query) {}

  @Override
  public boolean enterQuerySpec(SQLNode querySpec) {
    return true;
  }

  @Override
  public void leaveQuerySpec(SQLNode querySpec) {}

  @Override
  public boolean enterUnion(SQLNode union) {
    return true;
  }

  @Override
  public void leaveUnion(SQLNode union) {}

  @Override
  public boolean enterStatement(SQLNode statement) {
    return true;
  }

  @Override
  public void leaveStatement(SQLNode statement) {}
}
