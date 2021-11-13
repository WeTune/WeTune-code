package sjtu.ipads.wtune.common.tree;

import org.apache.commons.lang3.tuple.Pair;
import sjtu.ipads.wtune.common.field.FieldKey;
import sjtu.ipads.wtune.common.utils.ArraySupport;
import sjtu.ipads.wtune.common.utils.ListSupport;

import java.util.List;
import java.util.ListIterator;

public interface TreeSupport {
  static boolean isDescendant(TypedTreeContext<?> context, int rootId, int toCheckNodeId) {
    int n = toCheckNodeId;
    while (n != 0) {
      if (n == rootId) return true;
      n = context.parentOf(n);
    }
    return false;
  }

  static int locate(TypedTreeContext<?> context, int childId) {
    final int parentId = context.parentOf(childId);
    final int[] children = context.childrenOf(parentId);
    return ArraySupport.sequentialFind(children, childId, 0);
  }

  static FieldKey<?> locate(AstContext<?> context, int childId) {
    final int parentId = context.parentOf(childId);
    final AstFields<?> fields = context.fieldsOf(parentId);

    for (Pair<FieldKey<?>, Object> pair : fields) {
      final Object value = pair.getValue();

      if (matchAstNode(value, childId)) return pair.getKey();

      if (value instanceof Iterable)
        for (Object o : (Iterable<?>) value)
          if (matchAstNode(o, childId)) {
            return pair.getKey();
          }
    }

    return null;
  }

  static <Kind> int copyTree(TypedTreeContext<Kind> context, int rootId) {
    final Kind kind = context.kindOf(rootId);
    final int[] children = context.childrenOf(rootId);
    final int newNode = context.mkNode(kind);

    for (int i = 0; i < children.length; i++) {
      final int newChild = copyTree(context, children[i]);
      context.setChild(newNode, i, newChild);
    }

    return newNode;
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  static <Kind> int copyTree(AstContext<Kind> context, int rootId) {
    final Kind kind = context.kindOf(rootId);
    final int newNode = context.mkNode(kind);

    for (Pair<FieldKey<?>, Object> pair : context.fieldsOf(rootId)) {
      final FieldKey key = pair.getKey();
      final Object value = pair.getValue();
      final Object newValue;

      if (value instanceof AstNode) {
        newValue = ((AstNode<?>) value).copyTree();

      } else if (value instanceof Iterable) {
        final List newList = ListSupport.fromIterable(((Iterable<?>) value));
        final ListIterator iter = newList.listIterator();
        while (iter.hasNext()) {
          final Object o = iter.next();
          if (o instanceof AstNode) iter.set(((AstNode<?>) o).copyTree());
        }

        newValue = newList;

      } else {
        newValue = value;
      }

      context.setFieldOf(newNode, key, newValue);
    }

    return newNode;
  }

  static void removeTree(TypedTreeContext<?> context, int rootId) {
    final int parentId = context.parentOf(rootId);
    final int childIndex = locate(context, rootId);
    context.unsetChild(parentId, childIndex);
  }

  static void removeTree(AstContext<?> context, int rootId) {
    final int parentId = context.parentOf(rootId);
    final FieldKey<?> childKey = locate(context, rootId);
    context.unsetFieldOf(parentId, childKey);
  }

  static void moveTree(TypedTreeContext<?> context, int rootId, int newParentId, int childIndex) {
    removeTree(context, rootId);
    context.setChild(newParentId, childIndex, rootId);
  }

  static void moveTree(
      AstContext<?> context, int rootId, int newParentId, FieldKey<AstNode<?>> fieldKey) {
    removeTree(context, rootId);
    context.setFieldOf(newParentId, fieldKey, AstNode.mk(context, rootId));
  }

  static void pasteTree(TypedTreeContext<?> context, int rootId, int newParentId, int childIndex) {
    context.setChild(newParentId, childIndex, copyTree(context, rootId));
  }

  static void pasteTree(
      AstContext<?> context, int rootId, int newParentId, FieldKey<AstNode<?>> fieldKey) {
    context.setFieldOf(newParentId, fieldKey, AstNode.mk(context, copyTree(context, rootId)));
  }

  static boolean matchAstNode(Object obj, int nodeId) {
    return obj instanceof AstNode && ((AstNode<?>) obj).nodeId() == nodeId;
  }
}
