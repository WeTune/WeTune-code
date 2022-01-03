package sjtu.ipads.wtune.sql.resolution;

import sjtu.ipads.wtune.sql.ast1.SqlContext;
import sjtu.ipads.wtune.sql.ast1.SqlNode;
import sjtu.ipads.wtune.sql.ast1.SqlNodes;
import sjtu.ipads.wtune.sql.ast1.SqlVisitor;
import sjtu.ipads.wtune.sql.schema.Table;

import java.util.List;

import static java.util.Objects.requireNonNull;
import static sjtu.ipads.wtune.common.utils.ListSupport.flatMap;
import static sjtu.ipads.wtune.common.utils.ListSupport.map;
import static sjtu.ipads.wtune.sql.SqlSupport.selectItemNameOf;
import static sjtu.ipads.wtune.sql.ast1.SqlKind.QuerySpec;
import static sjtu.ipads.wtune.sql.ast1.SqlKind.SetOp;
import static sjtu.ipads.wtune.sql.ast1.SqlNodeFields.*;
import static sjtu.ipads.wtune.sql.ast1.TableSourceFields.Derived_Subquery;
import static sjtu.ipads.wtune.sql.ast1.TableSourceFields.Simple_Table;
import static sjtu.ipads.wtune.sql.resolution.Relation.isRelationRoot;
import static sjtu.ipads.wtune.sql.resolution.ResolutionSupport.getOuterRelation;

class ResolveRelation implements SqlVisitor {
  private final SqlContext ctx;
  private RelationsImpl relations;

  ResolveRelation(SqlContext ctx) {
    this.ctx = requireNonNull(ctx);
  }

  void resolve(RelationsImpl relations) {
    this.relations = relations;

    final SqlNode rootNode = SqlNode.mk(ctx, ctx.root());
    rootNode.accept(new SetupInfo());
    rootNode.accept(new SetupInput());
    rootNode.accept(new SetupAttrs());
  }

  private class SetupInfo implements SqlVisitor {
    @Override
    public boolean enter(SqlNode node) {
      if (isRelationRoot(node)) relations.bindRelationRoot(node);
      return true;
    }
  }

  private class SetupInput implements SqlVisitor {
    @Override
    public boolean enterSimpleTableSource(SqlNode tableSource) {
      final RelationImpl relation = relations.enclosingRelationOf(tableSource);
      final RelationImpl outerRelation = ((RelationImpl) getOuterRelation(relation));
      outerRelation.addInput(relation);
      return false;
    }

    @Override
    public boolean enterDerivedTableSource(SqlNode tableSource) {
      final RelationImpl relation = relations.enclosingRelationOf(tableSource.$(Derived_Subquery));
      final RelationImpl outerRelation = relations.enclosingRelationOf(tableSource);
      outerRelation.addInput(relation);
      return true;
    }
  }

  private class SetupAttrs implements SqlVisitor {
    @Override
    public void leaveSimpleTableSource(SqlNode simpleTableSource) {
      final RelationImpl relation = relations.enclosingRelationOf(simpleTableSource);
      final Table table = ctx.schema().table(simpleTableSource.$(Simple_Table).$(TableName_Table));
      final List<Attribute> attrs = map(table.columns(), c -> new ColumnAttribute(relation, c));
      relation.setAttributes(attrs);
    }

    @Override
    public void leaveQuery(SqlNode query) {
      final RelationImpl rel = relations.enclosingRelationOf(query);
      final SqlNode body = query.$(Query_Body);
      if (QuerySpec.isInstance(body)) {
        final SqlNodes items = body.$(QuerySpec_SelectItems);
        rel.setAttributes(
            map(items, it -> new ExprAttribute(rel, selectItemNameOf(it), it.$(SelectItem_Expr))));

      } else if (SetOp.isInstance(body)) {
        final List<Relation> inputs = rel.inputs();
        rel.setAttributes(flatMap(inputs, Relation::attributes));

      } else assert false;
    }
  }
}
