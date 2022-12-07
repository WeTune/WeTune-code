package wtune.superopt.fragment;

import wtune.common.utils.Commons;
import wtune.superopt.util.Hole;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static wtune.superopt.fragment.OpKind.PROJ;
import static wtune.superopt.fragment.OpKind.SET_OP;

class FragmentUtils {
  static boolean isDedup(Op op) {
    return op.kind() == PROJ && ((Proj) op).isDeduplicated();
  }

  static int structuralSize(Op tree) {
    if (tree == null) return 0;

    int sub = 0;
    for (Op predecessor : tree.predecessors()) sub += structuralSize(predecessor);
    return sub + 1;
  }

  static int structuralHash(Op tree) {
    int h = tree.shadowHash();
    for (Op operator : tree.predecessors()) {
      // Input is out of consideration.
      if (operator == null || operator instanceof Input) h = h * 31;
      else h = h * 31 + structuralHash(operator);
    }
    return h;
  }

  static boolean structuralEq(Op tree0, Op tree1) {
    if (tree0 == tree1) return true;
    if (tree0 == null || tree1 == null) return false;
    if (tree0.kind() != tree1.kind()) return false;
    if (tree0 instanceof ProjOp && tree1 instanceof ProjOp && ((ProjOp) tree0).isDeduplicated != ((ProjOp) tree1).isDeduplicated)
      return false;

    final Op[] prevs0 = tree0.predecessors();
    final Op[] prevs1 = tree1.predecessors();
    for (int i = 0, bound = prevs0.length; i < bound; i++)
      if (!structuralEq(prevs0[i], prevs1[i])) return false;

    return true;
  }

  static int structuralCompare(Op tree0, Op tree1) {
    if (tree0 == tree1) return 0;

    final int sz0 = structuralSize(tree0), sz1 = structuralSize(tree1);
    if (sz0 < sz1) return -1;
    if (sz0 > sz1) return 1;

    final OpKind type0 = tree0.kind(), type1 = tree1.kind();
    if (type0.ordinal() < type1.ordinal()) return -1;
    if (type0.ordinal() > type1.ordinal()) return 1;
    if (!isDedup(tree0) && isDedup(tree1)) return -1;
    if (isDedup(tree0) && !isDedup(tree1)) return 1;

    for (int i = 0, bound = type0.numPredecessors(); i < bound; ++i) {
      final int cmp = structuralCompare(tree0.predecessors()[i], tree1.predecessors()[i]);
      if (cmp != 0) return cmp;
    }

    return 0;
  }

  static StringBuilder structuralToString(Op tree, SymbolNaming naming, StringBuilder builder) {
    if (tree == null) return builder;

    builder.append(tree.kind().text());
    if (tree.kind() == PROJ && ((Proj) tree).isDeduplicated()) builder.append('*');
    if (tree.kind() == SET_OP && ((Union) tree).isDeduplicated()) builder.append('*');

    if (naming != null)
      switch (tree.kind()) {
        case INPUT:
          builder.append('<').append(naming.nameOf(((Input) tree).table())).append('>');
          break;
        case PROJ:
          builder.append('<').append(naming.nameOf(((Proj) tree).attrs()));
          builder.append(' ').append(naming.nameOf(((Proj) tree).schema())).append('>');
          break;
        case SIMPLE_FILTER:
          builder.append('<').append(naming.nameOf(((SimpleFilter) tree).predicate()));
          builder.append(' ').append(naming.nameOf(((SimpleFilter) tree).attrs())).append('>');
          break;
        case IN_SUB_FILTER:
          builder.append('<').append(naming.nameOf(((InSubFilter) tree).attrs())).append('>');
          break;
        case LEFT_JOIN:
        case INNER_JOIN:
          builder.append('<').append(naming.nameOf(((Join) tree).lhsAttrs()));
          builder.append(' ').append(naming.nameOf(((Join) tree).rhsAttrs())).append('>');
          break;
        case SET_OP:
          break;
        case AGG:
          builder.append('<').append(naming.nameOf(((AggOp) tree).groupByAttrs()));
          builder.append(' ').append(naming.nameOf(((AggOp) tree).aggregateAttrs()));
          builder.append(' ').append(naming.nameOf(((AggOp) tree).aggFunc()));
          builder.append(' ').append(naming.nameOf(((AggOp) tree).schema()));
          builder.append(' ').append(naming.nameOf(((AggOp) tree).havingPred())).append('>');
          break;
        default:
          throw new UnsupportedOperationException();
      }

    if (tree.kind().numPredecessors() > 0) {
      builder.append('(');
      Commons.joining(
          ",", asList(tree.predecessors()), builder, (it, b) -> structuralToString(it, naming, b));
      builder.append(')');
    }

    return builder;
  }

  static List<Hole<Op>> gatherHoles(Fragment fragment) {
    if (fragment.root() == null)
      return singletonList(Hole.ofSetter(((FragmentImpl) fragment)::setRoot0));

    final List<Hole<Op>> holes = new ArrayList<>();
    fragment.acceptVisitor(OpVisitor.traverse(x -> gatherHoles(x, holes)));

    return holes;
  }

  static void bindNames(Op op, String[] names, SymbolNaming naming) {
    switch (op.kind()) {
      case INPUT:
        naming.setName(((Input) op).table(), names[1]);
        break;
      case INNER_JOIN:
      case LEFT_JOIN:
        naming.setName(((Join) op).lhsAttrs(), names[1]);
        naming.setName(((Join) op).rhsAttrs(), names[2]);
        break;
      case SIMPLE_FILTER:
        naming.setName(((SimpleFilter) op).predicate(), names[1]);
        naming.setName(((SimpleFilter) op).attrs(), names[2]);
        break;
      case IN_SUB_FILTER:
        naming.setName(((InSubFilter) op).attrs(), names[1]);
        break;
      case PROJ:
        naming.setName(((Proj) op).attrs(), names[1]);
        naming.setName(((Proj) op).schema(), names[2]);
        break;
      case SET_OP:
        break;
      case AGG:
        naming.setName(((AggOp) op).groupByAttrs(), names[1]);
        naming.setName(((AggOp) op).aggregateAttrs(), names[2]);
        naming.setName(((AggOp) op).aggFunc(), names[3]);
        naming.setName(((AggOp) op).schema(), names[4]);
        naming.setName(((AggOp) op).havingPred(), names[5]);
        break;
      default:
        throw new UnsupportedOperationException();
    }
  }

  private static void gatherHoles(Op op, List<Hole<Op>> buffer) {
    final Op[] prev = op.predecessors();

    for (int i = 0, bound = prev.length; i < bound; i++)
      if (prev[i] == null) {
        final int j = i;
        buffer.add(Hole.ofSetter(x -> op.setPredecessor(j, x)));
      }
  }
}
