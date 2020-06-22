package sjtu.ipads.wtune.sqlparser;

public interface SQLVisitor {
  boolean enter(SQLNode node);

  void leave(SQLNode node);

  boolean enterTableName(SQLNode tableName);

  void leaveTableName(SQLNode tableName);

  boolean enterColumnName(SQLNode colName);

  void leaveColumnName(SQLNode colName);

  boolean enterCreateTable(SQLNode createTable);

  void leaveCreateTable(SQLNode createTable);

  boolean enterColumnDef(SQLNode colDef);

  void leaveColumnDef(SQLNode colDef);

  boolean enterReferences(SQLNode ref);

  void leaveReferences(SQLNode ref);

  boolean enterIndexDef(SQLNode indexDef);

  void leaveIndexDef(SQLNode indexDef);

  boolean enterKeyPart(SQLNode keyPart);

  void leaveKeyPart(SQLNode keyPart);

  boolean enterVariable(SQLNode variable);

  void leaveVariable(SQLNode variable);

  boolean enterColumnRef(SQLNode columnRef);

  void leaveColumnRef(SQLNode columnRef);

  boolean enterLiteral(SQLNode literal);

  void leaveLiteral(SQLNode literal);

  boolean enterFuncCall(SQLNode funcCall);

  void leaveFuncCall(SQLNode funcCall);

  boolean enterCollation(SQLNode collation);

  void leaveCollation(SQLNode collation);

  boolean enterParamMarker(SQLNode paramMarker);

  void leaveParamMarker(SQLNode paramMarker);

  boolean enterUnary(SQLNode unary);

  void leaveUnary(SQLNode unary);

  boolean enterGroupingOp(SQLNode groupingOp);

  void leaveGroupingOp(SQLNode groupingOp);

  boolean enterTuple(SQLNode tuple);

  void leaveTuple(SQLNode tuple);

  boolean enterMatch(SQLNode match);

  void leaveMatch(SQLNode match);

  boolean enterCast(SQLNode cast);

  void leaveCast(SQLNode cast);

  boolean enterSymbol(SQLNode symbol);

  void leaveSymbol(SQLNode symbol);

  boolean enterDefault(SQLNode _default);

  void leaveDefault(SQLNode _default);

  boolean enterValues(SQLNode values);

  void leaveValues(SQLNode values);

  boolean enterInterval(SQLNode interval);

  void leaveInterval(SQLNode interval);

  boolean enterExists(SQLNode exists);

  void leaveExists(SQLNode exists);

  boolean enterSubquery(SQLNode subquery);

  void leaveSubquery(SQLNode subquery);

  boolean enterWildcard(SQLNode wildcard);

  void leaveWildcard(SQLNode wildcard);

  boolean enterAggregate(SQLNode aggregate);

  void leaveAggregate(SQLNode aggregate);

  boolean enterConvertUsing(SQLNode convertUsing);

  void leaveConvertUsing(SQLNode convertUsing);

  boolean enterCase(SQLNode _case);

  void leaveCase(SQLNode _case);

  boolean enterWhen(SQLNode when);

  void leaveWhen(SQLNode when);

  boolean enterBinary(SQLNode binary);

  void leaveBinary(SQLNode binary);

  boolean enterWindowSpec(SQLNode windowSpec);

  void leaveWindowSpec(SQLNode windowSpec);

  boolean enterWindowFrame(SQLNode windowFrame);

  void leaveWindowFrame(SQLNode windowFrame);

  boolean enterFrameBound(SQLNode frameBound);

  void leaveFrameBound(SQLNode frameBound);

  boolean enterOrderItem(SQLNode orderItem);

  void leaveOrderItem(SQLNode orderItem);

  boolean enterTernary(SQLNode ternary);

  void leaveTernary(SQLNode ternary);
}
