package sjtu.ipads.wtune.sqlparser;

import sjtu.ipads.wtune.common.attrs.Attrs;

import java.util.List;

public interface SQLVisitor {
  default boolean isMutator() {
    return false;
  }

  default boolean enter(SQLNode node) {
    return true;
  }

  default void leave(SQLNode node) {}

  default boolean enterChild(SQLNode parent, Attrs.Key<SQLNode> key, SQLNode child) {
    return true;
  }

  default void leaveChild(SQLNode parent, Attrs.Key<SQLNode> key, SQLNode child) {}

  default boolean enterChildren(SQLNode parent, Attrs.Key<List<SQLNode>> key, List<SQLNode> child) {
    return true;
  }

  default void leaveChildren(SQLNode parent, Attrs.Key<List<SQLNode>> key, List<SQLNode> child) {}

  default boolean enterCreateTable(SQLNode createTable) {
    return true;
  }

  default void leaveCreateTable(SQLNode createTable) {}

  default boolean enterName2(SQLNode name2) {
    return true;
  }

  default void leaveName2(SQLNode name2) {}

  default boolean enterName3(SQLNode name3) {
    return true;
  }

  default void leaveName3(SQLNode name3) {}

  default boolean enterTableName(SQLNode tableName) {
    return true;
  }

  default void leaveTableName(SQLNode tableName) {}

  default boolean enterColumnDef(SQLNode colDef) {
    return true;
  }

  default void leaveColumnDef(SQLNode colDef) {}

  default boolean enterReferences(SQLNode ref) {
    return true;
  }

  default void leaveReferences(SQLNode ref) {}

  default boolean enterColumnName(SQLNode colName) {
    return true;
  }

  default void leaveColumnName(SQLNode colName) {}

  default boolean enterIndexDef(SQLNode indexDef) {
    return true;
  }

  default void leaveIndexDef(SQLNode indexDef) {}

  default boolean enterKeyPart(SQLNode keyPart) {
    return true;
  }

  default void leaveKeyPart(SQLNode keyPart) {}

  default boolean enterVariable(SQLNode variable) {
    return true;
  }

  default void leaveVariable(SQLNode variable) {}

  default boolean enterColumnRef(SQLNode columnRef) {
    return true;
  }

  default void leaveColumnRef(SQLNode columnRef) {}

  default boolean enterLiteral(SQLNode literal) {
    return true;
  }

  default void leaveLiteral(SQLNode literal) {}

  default boolean enterFuncCall(SQLNode funcCall) {
    return true;
  }

  default void leaveFuncCall(SQLNode funcCall) {}

  default boolean enterCollation(SQLNode collation) {
    return true;
  }

  default void leaveCollation(SQLNode collation) {}

  default boolean enterParamMarker(SQLNode paramMarker) {
    return true;
  }

  default void leaveParamMarker(SQLNode paramMarker) {}

  default boolean enterUnary(SQLNode unary) {
    return true;
  }

  default void leaveUnary(SQLNode unary) {}

  default boolean enterGroupingOp(SQLNode groupingOp) {
    return true;
  }

  default void leaveGroupingOp(SQLNode groupingOp) {}

  default boolean enterTuple(SQLNode tuple) {
    return true;
  }

  default void leaveTuple(SQLNode tuple) {}

  default boolean enterMatch(SQLNode match) {
    return true;
  }

  default void leaveMatch(SQLNode match) {}

  default boolean enterCast(SQLNode cast) {
    return true;
  }

  default void leaveCast(SQLNode cast) {}

  default boolean enterSymbol(SQLNode symbol) {
    return true;
  }

  default void leaveSymbol(SQLNode symbol) {}

  default boolean enterDefault(SQLNode _default) {
    return true;
  }

  default void leaveDefault(SQLNode _default) {}

  default boolean enterValues(SQLNode values) {
    return true;
  }

  default void leaveValues(SQLNode values) {}

  default boolean enterInterval(SQLNode interval) {
    return true;
  }

  default void leaveInterval(SQLNode interval) {}

  default boolean enterExists(SQLNode exists) {
    return true;
  }

  default void leaveExists(SQLNode exists) {}

  default boolean enterQueryExpr(SQLNode queryExpr) {
    return true;
  }

  default void leaveQueryExpr(SQLNode queryExpr) {}

  default boolean enterWildcard(SQLNode wildcard) {
    return true;
  }

  default void leaveWildcard(SQLNode wildcard) {}

  default boolean enterAggregate(SQLNode aggregate) {
    return true;
  }

  default void leaveAggregate(SQLNode aggregate) {}

  default boolean enterConvertUsing(SQLNode convertUsing) {
    return true;
  }

  default void leaveConvertUsing(SQLNode convertUsing) {}

  default boolean enterCase(SQLNode _case) {
    return true;
  }

  default void leaveCase(SQLNode _case) {}

  default boolean enterWhen(SQLNode when) {
    return true;
  }

  default void leaveWhen(SQLNode when) {}

  default boolean enterBinary(SQLNode binary) {
    return true;
  }

  default void leaveBinary(SQLNode binary) {}

  default boolean enterFrameBound(SQLNode frameBound) {
    return true;
  }

  default void leaveFrameBound(SQLNode frameBound) {}

  default boolean enterWindowFrame(SQLNode windowFrame) {
    return true;
  }

  default void leaveWindowFrame(SQLNode windowFrame) {}

  default boolean enterWindowSpec(SQLNode windowSpec) {
    return true;
  }

  default void leaveWindowSpec(SQLNode windowSpec) {}

  default boolean enterOrderItem(SQLNode orderItem) {
    return true;
  }

  default void leaveOrderItem(SQLNode orderItem) {}

  default boolean enterTernary(SQLNode ternary) {
    return true;
  }

  default void leaveTernary(SQLNode ternary) {}

  default boolean enterSelectItem(SQLNode selectItem) {
    return true;
  }

  default void leaveSelectItem(SQLNode selectItem) {}

  default boolean enterIndexHint(SQLNode indexHint) {
    return true;
  }

  default void leaveIndexHint(SQLNode indexHint) {}

  default boolean enterSimpleTableSource(SQLNode simpleTableSource) {
    return true;
  }

  default void leaveSimpleTableSource(SQLNode simpleTableSource) {}

  default boolean enterDerivedTableSource(SQLNode derivedTableSource) {
    return true;
  }

  default void leaveDerivedTableSource(SQLNode derivedTableSource) {}

  default boolean enterJoinedTableSource(SQLNode joinedTableSource) {
    return true;
  }

  default void leaveJoinedTableSource(SQLNode joinedTableSource) {}

  default boolean enterQuery(SQLNode query) {
    return true;
  }

  default void leaveQuery(SQLNode query) {}

  default boolean enterQuerySpec(SQLNode querySpec) {
    return true;
  }

  default void leaveQuerySpec(SQLNode querySpec) {}

  default boolean enterSetOp(SQLNode union) {
    return true;
  }

  default void leaveUnion(SQLNode union) {}

  default boolean enterStatement(SQLNode statement) {
    return true;
  }

  default void leaveStatement(SQLNode statement) {}

  default boolean enterIndirection(SQLNode indirection) {
    return true;
  }

  default void leaveIndirection(SQLNode indirection) {}

  default boolean enterIndirectionComp(SQLNode indirectionComp) {
    return true;
  }

  default void leaveIndirectionComp(SQLNode indirectionComp) {}

  default boolean enterCommonName(SQLNode commonName) {
    return true;
  }

  default void leaveCommonName(SQLNode commonName) {}

  default boolean enterArray(SQLNode array) {
    return true;
  }

  default void leaveArray(SQLNode array) {}
}
