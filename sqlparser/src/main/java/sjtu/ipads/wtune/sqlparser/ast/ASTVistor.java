package sjtu.ipads.wtune.sqlparser.ast;

import sjtu.ipads.wtune.common.attrs.FieldKey;

import java.util.List;
import java.util.function.Consumer;

public interface ASTVistor {

  default boolean enter(ASTNode node) {
    return true;
  }

  default void leave(ASTNode node) {}

  default boolean enterChild(ASTNode parent, FieldKey<ASTNode> key, ASTNode child) {
    return true;
  }

  default void leaveChild(ASTNode parent, FieldKey<ASTNode> key, ASTNode child) {}

  default boolean enterChildren(ASTNode parent, FieldKey<List<ASTNode>> key, List<ASTNode> child) {
    return true;
  }

  default void leaveChildren(ASTNode parent, FieldKey<List<ASTNode>> key, List<ASTNode> child) {}

  default boolean enterCreateTable(ASTNode createTable) {
    return true;
  }

  default void leaveCreateTable(ASTNode createTable) {}

  default boolean enterName2(ASTNode name2) {
    return true;
  }

  default void leaveName2(ASTNode name2) {}

  default boolean enterName3(ASTNode name3) {
    return true;
  }

  default void leaveName3(ASTNode name3) {}

  default boolean enterTableName(ASTNode tableName) {
    return true;
  }

  default void leaveTableName(ASTNode tableName) {}

  default boolean enterColumnDef(ASTNode colDef) {
    return true;
  }

  default void leaveColumnDef(ASTNode colDef) {}

  default boolean enterReferences(ASTNode ref) {
    return true;
  }

  default void leaveReferences(ASTNode ref) {}

  default boolean enterColumnName(ASTNode colName) {
    return true;
  }

  default void leaveColumnName(ASTNode colName) {}

  default boolean enterIndexDef(ASTNode indexDef) {
    return true;
  }

  default void leaveIndexDef(ASTNode indexDef) {}

  default boolean enterKeyPart(ASTNode keyPart) {
    return true;
  }

  default void leaveKeyPart(ASTNode keyPart) {}

  default boolean enterVariable(ASTNode variable) {
    return true;
  }

  default void leaveVariable(ASTNode variable) {}

  default boolean enterColumnRef(ASTNode columnRef) {
    return true;
  }

  default void leaveColumnRef(ASTNode columnRef) {}

  default boolean enterLiteral(ASTNode literal) {
    return true;
  }

  default void leaveLiteral(ASTNode literal) {}

  default boolean enterFuncCall(ASTNode funcCall) {
    return true;
  }

  default void leaveFuncCall(ASTNode funcCall) {}

  default boolean enterCollation(ASTNode collation) {
    return true;
  }

  default void leaveCollation(ASTNode collation) {}

  default boolean enterParamMarker(ASTNode paramMarker) {
    return true;
  }

  default void leaveParamMarker(ASTNode paramMarker) {}

  default boolean enterUnary(ASTNode unary) {
    return true;
  }

  default void leaveUnary(ASTNode unary) {}

  default boolean enterGroupingOp(ASTNode groupingOp) {
    return true;
  }

  default void leaveGroupingOp(ASTNode groupingOp) {}

  default boolean enterTuple(ASTNode tuple) {
    return true;
  }

  default void leaveTuple(ASTNode tuple) {}

  default boolean enterMatch(ASTNode match) {
    return true;
  }

  default void leaveMatch(ASTNode match) {}

  default boolean enterCast(ASTNode cast) {
    return true;
  }

  default void leaveCast(ASTNode cast) {}

  default boolean enterSymbol(ASTNode symbol) {
    return true;
  }

  default void leaveSymbol(ASTNode symbol) {}

  default boolean enterDefault(ASTNode _default) {
    return true;
  }

  default void leaveDefault(ASTNode _default) {}

  default boolean enterValues(ASTNode values) {
    return true;
  }

  default void leaveValues(ASTNode values) {}

  default boolean enterInterval(ASTNode interval) {
    return true;
  }

  default void leaveInterval(ASTNode interval) {}

  default boolean enterExists(ASTNode exists) {
    return true;
  }

  default void leaveExists(ASTNode exists) {}

  default boolean enterQueryExpr(ASTNode queryExpr) {
    return true;
  }

  default void leaveQueryExpr(ASTNode queryExpr) {}

  default boolean enterWildcard(ASTNode wildcard) {
    return true;
  }

  default void leaveWildcard(ASTNode wildcard) {}

  default boolean enterAggregate(ASTNode aggregate) {
    return true;
  }

  default void leaveAggregate(ASTNode aggregate) {}

  default boolean enterConvertUsing(ASTNode convertUsing) {
    return true;
  }

  default void leaveConvertUsing(ASTNode convertUsing) {}

  default boolean enterCase(ASTNode _case) {
    return true;
  }

  default void leaveCase(ASTNode _case) {}

  default boolean enterWhen(ASTNode when) {
    return true;
  }

  default void leaveWhen(ASTNode when) {}

  default boolean enterBinary(ASTNode binary) {
    return true;
  }

  default void leaveBinary(ASTNode binary) {}

  default boolean enterFrameBound(ASTNode frameBound) {
    return true;
  }

  default void leaveFrameBound(ASTNode frameBound) {}

  default boolean enterWindowFrame(ASTNode windowFrame) {
    return true;
  }

  default void leaveWindowFrame(ASTNode windowFrame) {}

  default boolean enterWindowSpec(ASTNode windowSpec) {
    return true;
  }

  default void leaveWindowSpec(ASTNode windowSpec) {}

  default boolean enterOrderItem(ASTNode orderItem) {
    return true;
  }

  default void leaveOrderItem(ASTNode orderItem) {}

  default boolean enterTernary(ASTNode ternary) {
    return true;
  }

  default void leaveTernary(ASTNode ternary) {}

  default boolean enterSelectItem(ASTNode selectItem) {
    return true;
  }

  default void leaveSelectItem(ASTNode selectItem) {}

  default boolean enterIndexHint(ASTNode indexHint) {
    return true;
  }

  default void leaveIndexHint(ASTNode indexHint) {}

  default boolean enterSimpleTableSource(ASTNode simpleTableSource) {
    return true;
  }

  default void leaveSimpleTableSource(ASTNode simpleTableSource) {}

  default boolean enterDerivedTableSource(ASTNode derivedTableSource) {
    return true;
  }

  default void leaveDerivedTableSource(ASTNode derivedTableSource) {}

  default boolean enterJoinedTableSource(ASTNode joinedTableSource) {
    return true;
  }

  default void leaveJoinedTableSource(ASTNode joinedTableSource) {}

  default boolean enterQuery(ASTNode query) {
    return true;
  }

  default void leaveQuery(ASTNode query) {}

  default boolean enterQuerySpec(ASTNode querySpec) {
    return true;
  }

  default void leaveQuerySpec(ASTNode querySpec) {}

  default boolean enterSetOp(ASTNode union) {
    return true;
  }

  default void leaveUnion(ASTNode union) {}

  default boolean enterStatement(ASTNode statement) {
    return true;
  }

  default void leaveStatement(ASTNode statement) {}

  default boolean enterIndirection(ASTNode indirection) {
    return true;
  }

  default void leaveIndirection(ASTNode indirection) {}

  default boolean enterIndirectionComp(ASTNode indirectionComp) {
    return true;
  }

  default void leaveIndirectionComp(ASTNode indirectionComp) {}

  default boolean enterCommonName(ASTNode commonName) {
    return true;
  }

  default void leaveCommonName(ASTNode commonName) {}

  default boolean enterArray(ASTNode array) {
    return true;
  }

  default void leaveArray(ASTNode array) {}

  default boolean enterGroupItem(ASTNode groupItem) {
    return true;
  }

  default void leaveGroupItem(ASTNode groupItem) {}

  static ASTVistor topDownVisit(Consumer<ASTNode> func) {
    return new ASTVistor() {
      @Override
      public boolean enter(ASTNode node) {
        func.accept(node);
        return true;
      }
    };
  }

  static ASTVistor topDownVisit(Consumer<ASTNode> func, FieldDomain... types) {
    return new ASTVistor() {
      @Override
      public boolean enter(ASTNode node) {
        for (FieldDomain type : types)
          if (type.isInstance(node)) {
            func.accept(node);
            break;
          }
        return true;
      }
    };
  }
}
