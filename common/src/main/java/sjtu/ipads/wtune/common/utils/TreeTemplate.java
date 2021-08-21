package sjtu.ipads.wtune.common.utils;

import java.util.Set;
import java.util.function.Function;

import static sjtu.ipads.wtune.common.utils.Commons.newIdentitySet;

public class TreeTemplate<C extends TreeContext<C>, T extends TreeNode<C, T>> {
  private final TreeScaffold<C, T> ctx;
  private final T root;
  private final Set<T> jointPoint;
  private boolean copy;
  private Function<T, T> copyFunction;

  private T instantiated;

  TreeTemplate(TreeScaffold<C, T> ctx, T root) {
    this.ctx = ctx;
    this.root = root;
    this.jointPoint = newIdentitySet();
  }

  TreeTemplate(TreeScaffold<C, T> ctx, T root, Set<T> jointPoint) {
    this.ctx = ctx;
    this.root = root;
    this.jointPoint = jointPoint;
  }

  public T root() {
    return root;
  }

  public TreeTemplate<C, T> bindJointPoint(T point, T subTree) {
    return bindJointPoint(point, new TreeTemplate<>(ctx, subTree));
  }

  public TreeTemplate<C, T> bindJointPoint(T point, TreeTemplate<C, T> subTree) {
    jointPoint.add(point);
    ctx.bindJointPoint(point, subTree);
    subTree.setCopyFunction(copyFunction);
    return subTree;
  }

  public void setNeedCopy(boolean needCopy) {
    this.copy = needCopy;
  }

  public void setCopyFunction(Function<T, T> copyFunction) {
    this.copyFunction = copyFunction;
    this.copy = copyFunction != null;
  }

  public T getInstantiated() {
    return instantiated;
  }

  public T instantiate() {
    if (!copy && jointPoint.isEmpty()) return instantiated = root;

    final Function<T, T> copyFunc = copy ? copyFunction : Function.identity();
    return instantiated = instantiate0(root, copyFunc);
  }

  private T instantiate0(T subtree, Function<T, T> copyFunc) {
    if (jointPoint.contains(subtree)) return ctx.getJointPoint(subtree).instantiate();

    final T copy = copyFunc.apply(subtree);
    final T[] predecessors = subtree.predecessors();
    for (int i = 0, bound = predecessors.length; i < bound; ++i) {
      final T newPredecessor = instantiate0(predecessors[i], copyFunc);
      if (predecessors[i] != newPredecessor) copy.setPredecessor(i, newPredecessor);
    }

    return copy;
  }
}
