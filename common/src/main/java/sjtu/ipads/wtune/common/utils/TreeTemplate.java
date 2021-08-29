package sjtu.ipads.wtune.common.utils;

import com.google.common.base.Objects;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

public class TreeTemplate<C extends TreeContext<C>, T extends TreeNode<C, T>> {
  private final TreeScaffold<C, T> ctx;
  private final T root;
  private final Set<Object> jointPoints;
  private boolean copy;
  private Function<T, T> copyFunction;

  private T instantiated;

  TreeTemplate(TreeScaffold<C, T> ctx, T root) {
    this.ctx = ctx;
    this.root = root;
    this.jointPoints = new HashSet<>();
  }

  public T root() {
    return root;
  }

  public TreeTemplate<C, T> bindJointPoint(T point, T subTree) {
    return bindJointPoint(point, new TreeTemplate<>(ctx, subTree));
  }

  public TreeTemplate<C, T> bindJointPoint(T point, TreeTemplate<C, T> subTree) {
    jointPoints.add(point);
    ctx.bindJointPoint(point, subTree);
    subTree.setCopyFunction(copyFunction);
    return subTree;
  }

  public TreeTemplate<C, T> bindJointPoint(T parent, int childIndex, T subTree) {
    return bindJointPoint(parent, childIndex, new TreeTemplate<>(ctx, subTree));
  }

  public TreeTemplate<C, T> bindJointPoint(T parent, int childIndex, TreeTemplate<C, T> subTree) {
    final Placeholder<C, T> point = new Placeholder<>(parent, childIndex);
    jointPoints.add(point);
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
    if (!copy && jointPoints.isEmpty()) return instantiated = root;

    final Function<T, T> copyFunc = copy ? copyFunction : Function.identity();
    return instantiated = instantiate0(root, copyFunc);
  }

  private T instantiate0(T subtree, Function<T, T> copyFunc) {
    if (jointPoints.contains(subtree)) return ctx.getJointPoint(subtree).instantiate();

    final T copy = copyFunc.apply(subtree);
    final T[] predecessors = subtree.predecessors();

    for (int i = 0, bound = predecessors.length; i < bound; ++i) {
      final T predecessor = predecessors[i];

      T newPredecessor = null;
      Placeholder<C, T> p;

      if (predecessor != null && jointPoints.contains(predecessor))
        newPredecessor = ctx.getJointPoint(predecessor).instantiate();

      if (newPredecessor == null && jointPoints.contains(p = new Placeholder<>(subtree, i)))
        newPredecessor = ctx.getJointPoint(p).instantiate();

      if (newPredecessor == null && predecessor != null)
        newPredecessor = instantiate0(predecessor, copyFunc);

      if (predecessor != newPredecessor) copy.setPredecessor(i, newPredecessor);
    }

    return copy;
  }

  private record Placeholder<C extends TreeContext<C>, T extends TreeNode<C, T>>(T parent, int childIndex) {
    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Placeholder<?, ?> that = (Placeholder<?, ?>) o;
      return childIndex == that.childIndex && Objects.equal(parent, that.parent);
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(parent, childIndex);
    }
  }
}
