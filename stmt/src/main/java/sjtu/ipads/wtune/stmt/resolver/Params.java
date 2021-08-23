package sjtu.ipads.wtune.stmt.resolver;

import java.util.Collection;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.AttributeManager;

public interface Params extends AttributeManager<ParamDesc> {
  ParamDesc paramDesc(ASTNode node);

  ParamDesc setParamDesc(ASTNode node, ParamDesc desc);

  Collection<ParamDesc> params();

  ASTNode ast();

  JoinGraph joinGraph();
}