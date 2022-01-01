package sjtu.ipads.wtune.common.tree;

import sjtu.ipads.wtune.common.field.FieldKey;

import java.util.Map;
import java.util.NoSuchElementException;

import static sjtu.ipads.wtune.common.tree.TreeContext.NO_SUCH_NODE;
import static sjtu.ipads.wtune.common.utils.ArraySupport.linearFind;

public interface TreeSupport {
  static void checkNodePresent(TreeContext<?> context, int nodeId) {
    if (!context.isPresent(nodeId))
      throw new NoSuchElementException("no such node in this tree: " + nodeId);
  }

  static void checkIsValidChild(TreeContext<?> context, int parentId, int childId) {
    final int existingParent = context.parentOf(childId);
    if (existingParent != NO_SUCH_NODE)
      throw new IllegalStateException("cannot set parent: already has parent");
    if (isDescendant(context, parentId, childId))
      throw new IllegalStateException("cannot set parent: loop incurred");
  }

  static int countNodes(TreeContext<?> context) {
    int count = 0;
    for (int nodeId = 1, bound = context.maxNodeId(); nodeId <= bound; nodeId++) {
      if (!context.isPresent(nodeId)) ++count;
    }
    return count;
  }

  static int rootOf(TreeContext<?> context, int nodeId) {
    int parent = context.parentOf(nodeId);
    while (parent != NO_SUCH_NODE) {
      nodeId = parent;
      parent = context.parentOf(nodeId);
    }
    return nodeId;
  }

  static boolean isDetached(TreeContext<?> context, int rootId, int nodeId) {
    return rootOf(context, nodeId) != rootId;
  }

  static boolean isDescendant(TreeContext<?> context, int rootId, int toCheckNodeId) {
    int n = toCheckNodeId;
    while (n != 0) {
      if (n == rootId) return true;
      n = context.parentOf(n);
    }
    return false;
  }

  static void deleteDetached(TreeContext<?> context, int rootId) {
    for (int i = 1, bound = context.maxNodeId(); i <= bound; ++i) {
      if (context.isPresent(i) && isDetached(context, rootId, i)) {
        context.deleteNode(i);
      }
    }
  }

  static int indexOfChild(UniformTreeContext<?> context, int nodeId) {
    final int parent = context.parentOf(nodeId);
    final int[] children = context.childrenOf(parent);
    return linearFind(children, nodeId, 0);
  }

  static int locate(UniformTreeContext<?> context, int childId) {
    final int parentId = context.parentOf(childId);
    final int[] children = context.childrenOf(parentId);
    return linearFind(children, childId, 0);
  }

  static FieldKey<?> locate(LabeledTreeContext<?> context, int childId) {
    final int parentId = context.parentOf(childId);
    final LabeledTreeFields<?> fields = context.fieldsOf(parentId);

    for (Map.Entry<FieldKey<?>, Object> pair : fields.entrySet()) {
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

  static <Kind> int copyTree(UniformTreeContext<Kind> context, int rootId) {
    final Kind kind = context.kindOf(rootId);
    final int[] children = context.childrenOf(rootId);
    final int newNode = context.mkNode(kind);

    for (int i = 0; i < children.length; i++) {
      final int newChild = copyTree(context, children[i]);
      context.setChild(newNode, i, newChild);
    }

    return newNode;
  }

  static boolean matchAstNode(Object obj, int nodeId) {
    return obj instanceof LabeledTreeNode && ((LabeledTreeNode) obj).nodeId() == nodeId;
  }
}
