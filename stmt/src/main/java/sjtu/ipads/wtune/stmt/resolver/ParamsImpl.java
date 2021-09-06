package sjtu.ipads.wtune.stmt.resolver;

import sjtu.ipads.wtune.common.attrs.FieldKey;
import sjtu.ipads.wtune.common.attrs.Fields;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.ast.ASTVistor;
import sjtu.ipads.wtune.sqlparser.ast.AttributeManagerBase;
import sjtu.ipads.wtune.sqlparser.ast.constants.BinaryOp;
import sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind;

import java.util.*;

import static java.util.Collections.singletonList;
import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.BINARY_OP;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.*;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind.*;
import static sjtu.ipads.wtune.sqlparser.ast.constants.NodeType.EXPR;
import static sjtu.ipads.wtune.sqlparser.ast.constants.NodeType.QUERY;
import static sjtu.ipads.wtune.sqlparser.relational.Relation.RELATION;
import static sjtu.ipads.wtune.stmt.resolver.BoolExprManager.BOOL_EXPR;
import static sjtu.ipads.wtune.stmt.resolver.ParamModifier.Type.LIMIT_VAL;
import static sjtu.ipads.wtune.stmt.resolver.ParamModifier.Type.OFFSET_VAL;
import static sjtu.ipads.wtune.stmt.resolver.ParamModifier.modifier;

public class ParamsImpl extends AttributeManagerBase<ParamDesc> implements Params {
  private final ASTNode ast;
  private final Map<ASTNode, ParamDesc> params;
  private final JoinGraph joinGraph;

  public ParamsImpl(ASTNode ast, Map<ASTNode, ParamDesc> params, JoinGraph joinGraph) {
    this.ast = ast;
    this.params = params;
    this.joinGraph = joinGraph;
  }

  public static Params build(ASTNode ast) {
    ast.get(RELATION); // trigger a re-resolve, ensure all relations are stable

    final BoolExprManager manager = ast.context().manager(BoolExprManager.class);
    if (manager == null) Resolution.resolveBoolExpr(ast);

    final ParamsImpl params = new ParamsImpl(ast, extractParams(ast), JoinGraphBuilder.build(ast));
    ast.context().addManager(params);
    return params;
  }

  public static Params simpleBuild(ASTNode ast) {
    final Map<ASTNode, ParamDesc> params = extractParamsSimple(ast);
    return new ParamsImpl(ast, params, null);
  }

  @Override
  public ParamDesc paramDesc(ASTNode node) {
    return params.get(node);
  }

  @Override
  public ParamDesc setParamDesc(ASTNode node, ParamDesc desc) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Collection<ParamDesc> params() {
    return params.values();
  }

  @Override
  public ASTNode ast() {
    return ast;
  }

  @Override
  public JoinGraph joinGraph() {
    return joinGraph;
  }

  @Override
  public Class<?> key() {
    return Params.class;
  }

  @Override
  protected FieldKey<ParamDesc> fieldKey() {
    return ParamDescField.INSTANCE;
  }

  private static Map<ASTNode, ParamDesc> extractParams(ASTNode ast) {
    final ExtractParam extractParam = new ExtractParam();
    ast.accept(extractParam);
    return extractParam.params;
  }

  private static Map<ASTNode, ParamDesc> extractParamsSimple(ASTNode ast) {
    final ExtractParamSimple extractParam = new ExtractParamSimple();
    ast.accept(extractParam);
    return extractParam.params;
  }
}

class ParamDescField implements FieldKey<ParamDesc> {
  static ParamDescField INSTANCE = new ParamDescField();

  @Override
  public String name() {
    return "testbed.param";
  }

  @Override
  public ParamDesc get(Fields owner) {
    final ASTNode node = owner.unwrap(ASTNode.class);
    return node.manager(Params.class).paramDesc(node);
  }

  @Override
  public ParamDesc set(Fields owner, ParamDesc obj) {
    final ASTNode node = owner.unwrap(ASTNode.class);
    return node.manager(Params.class).setParamDesc(node, obj);
  }
}

class ExtractParam implements ASTVistor {
  final Map<ASTNode, ParamDesc> params = new IdentityHashMap<>();
  private int nextIndex = 0;

  ExtractParam() {}

  @Override
  public boolean enter(ASTNode node) {
    if (QUERY.isInstance(node)) {
      final ASTNode offset = node.get(QUERY_OFFSET);
      final ASTNode limit = node.get(QUERY_LIMIT);
      if (offset != null) {
        final ParamDesc desc = new ParamDescImpl(null, offset, singletonList(modifier(OFFSET_VAL)));
        params.put(desc.node(), desc);
      }
      if (limit != null) {
        final ParamDesc desc = new ParamDescImpl(null, limit, singletonList(modifier(LIMIT_VAL)));
        params.put(desc.node(), desc);
      }
    }

    if (!EXPR.isInstance(node)) return true;

    final BoolExpr boolExpr = node.get(BOOL_EXPR);
    if (boolExpr == null || !boolExpr.isPrimitive()) return true;

    final List<ParamDesc> paramDescs = ResolveParam.resolve(node);
    if (paramDescs.contains(null)) return false;

    for (ParamDesc desc : paramDescs) {
      if (!desc.isCheckNull()) desc.setIndex(nextIndex++);
      if (desc.isElement()) nextIndex++;
      params.put(desc.node(), desc);
    }

    return node.get(BINARY_OP) == BinaryOp.IN_SUBQUERY || EXISTS.isInstance(node);
  }
}

class ExtractParamSimple implements ASTVistor {
  private static final Set<ExprKind> INTERESTING_ENV =
      Set.of(UNARY, BINARY, TERNARY, TUPLE, ARRAY, MATCH);

  final Map<ASTNode, ParamDesc> params = new IdentityHashMap<>();

  private void add(ASTNode node) {
    final ExprKind exprKind = node.parent().get(EXPR_KIND);
    if (exprKind != null && INTERESTING_ENV.contains(exprKind))
      params.put(node, new ParamDescImpl(null, node, null));
  }

  @Override
  public boolean enterParamMarker(ASTNode paramMarker) {
    add(paramMarker);
    return false;
  }

  @Override
  public boolean enterLiteral(ASTNode literal) {
    add(literal);
    return false;
  }

  @Override
  public boolean enterChild(ASTNode parent, FieldKey<ASTNode> key, ASTNode child) {
    if (key == QUERY_OFFSET) {
      if (child != null) add(child);
      return false;
    }

    return true;
  }
}
