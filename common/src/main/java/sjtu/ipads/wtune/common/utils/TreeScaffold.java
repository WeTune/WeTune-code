package sjtu.ipads.wtune.common.utils;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Function;

public class TreeScaffold<T extends TreeNode<T>> {
  private final TreeTemplate<T> root;
  private final Map<T, TreeTemplate<T>> jointPoints;

  public TreeScaffold(T root) {
    this(root, null);
  }

  public TreeScaffold(T root, Function<T, T> copyFunction) {
    this.root = new TreeTemplate<>(this, root);
    this.jointPoints = new IdentityHashMap<>();
    this.root.setCopyFunction(copyFunction);
  }

  void bindJointPoint(T point, TreeTemplate<T> subTree) {
    jointPoints.put(point, subTree);
  }

  TreeTemplate<T> getJointPoint(T point) {
    return jointPoints.get(point);
  }

  public TreeTemplate<T> rootTemplate() {
    return root;
  }

  public T instantiate() {
    return root.instantiate();
  }

  public void setCopyFunction(Function<T, T> copyFunc) {
    this.root.setCopyFunction(copyFunc);
  }
}
