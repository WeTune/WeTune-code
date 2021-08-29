package sjtu.ipads.wtune.common.utils;

import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.util.Arrays.asList;
import static sjtu.ipads.wtune.common.utils.TreeNode.treeRootOf;

public class TreeScaffold<C extends TreeContext<C>, T extends TreeNode<C, T>> {
  private final TreeTemplate<C, T> root;
  private final Map<Object, TreeTemplate<C, T>> jointPoints;

  public TreeScaffold(T root) {
    this(root, (Function<T, T>) null);
  }

  public TreeScaffold(T root, C newContext) {
    this(root, newContext != null ? it -> it.copy(newContext) : null);
  }

  public TreeScaffold(T root, Function<T, T> copyFunction) {
    this.root = new TreeTemplate<>(this, root);
    this.jointPoints = new HashMap<>();
    if (copyFunction != null) {
      this.root.setCopyFunction(copyFunction);
    } else {
      final C dup = root.context() == null ? null : root.context().dup();
      this.root.setCopyFunction(it -> it.copy(dup));
    }
  }

  void bindJointPoint(Object point, TreeTemplate<C, T> subTree) {
    jointPoints.put(point, subTree);
  }

  TreeTemplate<C, T> getJointPoint(Object point) {
    return jointPoints.get(point);
  }

  public TreeTemplate<C, T> rootTemplate() {
    return root;
  }

  public T instantiate() {
    return root.instantiate();
  }

  public void setCopyFunction(Function<T, T> copyFunc) {
    this.root.setCopyFunction(copyFunc);
  }

  @SafeVarargs
  public static <C extends TreeContext<C>, T extends TreeNode<C, T>> T replaceLocal(
      T parent, T... children) {
    return replaceLocal(parent.context().dup(), parent, children);
  }

  @SafeVarargs
  public static <C extends TreeContext<C>, T extends TreeNode<C, T>> T replaceLocal(
      C context, T parent, T... children) {
    final TreeScaffold<C, T> scaffold = new TreeScaffold<>(parent, context);
    final TreeTemplate<C, T> template = scaffold.rootTemplate();

    for (int i = 0; i < children.length; i++)
      if (children[i] != null && children[i] != parent.predecessors()[i]) {
        template.bindJointPoint(parent.predecessors()[i], children[i]);
      }

    return scaffold.instantiate();
  }

  @SafeVarargs
  public static <C extends TreeContext<C>, T extends TreeNode<C, T>> T replaceLocal(
      T parent, Pair<T, T>... replacements) {
    return replaceLocal(parent.context().dup(), parent, replacements);
  }

  @SafeVarargs
  public static <C extends TreeContext<C>, T extends TreeNode<C, T>> T replaceLocal(
      C context, T parent, Pair<T, T>... replacements) {
    final TreeScaffold<C, T> scaffold = new TreeScaffold<>(parent, context);
    final TreeTemplate<C, T> template = scaffold.rootTemplate();

    for (Pair<T, T> replacement : replacements)
      template.bindJointPoint(replacement.getKey(), replacement.getValue());

    return scaffold.instantiate();
  }

  @SafeVarargs
  public static <C extends TreeContext<C>, T extends TreeNode<C, T>> T replaceGlobal(
      T parent, T... children) {
    return replaceGlobal(parent.context().dup(), parent, children);
  }

  public static <C extends TreeContext<C>, T extends TreeNode<C, T>> T replaceGlobal(
      T parent, List<T> children) {
    return replaceGlobal(parent.context().dup(), parent, children);
  }

  @SafeVarargs
  public static <C extends TreeContext<C>, T extends TreeNode<C, T>> T replaceGlobal(
      C context, T parent, T... children) {
    return replaceGlobal(context, parent, asList(children));
  }

  public static <C extends TreeContext<C>, T extends TreeNode<C, T>> T replaceGlobal(
      C context, T parent, List<T> children) {
    final T root = treeRootOf(parent);
    final TreeScaffold<C, T> scaffold = new TreeScaffold<>(root, context);
    final TreeTemplate<C, T> rootTemplate = scaffold.rootTemplate();
    final TreeTemplate<C, T> localTemplate;

    if (parent != root) localTemplate = rootTemplate.bindJointPoint(parent, parent);
    else localTemplate = rootTemplate;

    boolean changed = false;

    for (int i = 0, bound = children.size(); i < bound; i++) {
      final T newChild = children.get(i);
      if (newChild != null && newChild != parent.predecessors()[i]) {
        changed = true;
        localTemplate.bindJointPoint(parent.predecessors()[i], newChild);
      }
    }

    if (!changed) return parent;

    scaffold.instantiate();
    return localTemplate.getInstantiated();
  }

  @SafeVarargs
  public static <C extends TreeContext<C>, T extends TreeNode<C, T>> T replaceGlobal(
      T parent, Pair<T, T>... replacements) {
    return replaceGlobal(parent.context().dup(), parent, replacements);
  }

  public static <C extends TreeContext<C>, T extends TreeNode<C, T>> T replaceGlobal(
      T parent, Iterable<Pair<T, T>> replacements) {
    return replaceGlobal(parent.context().dup(), parent, replacements);
  }

  @SafeVarargs
  public static <C extends TreeContext<C>, T extends TreeNode<C, T>> T replaceGlobal(
      C context, T parent, Pair<T, T>... replacements) {
    return replaceGlobal(context, parent, asList(replacements));
  }

  public static <C extends TreeContext<C>, T extends TreeNode<C, T>> T replaceGlobal(
      C context, T parent, Iterable<Pair<T, T>> replacements) {
    final T root = treeRootOf(parent);
    final TreeScaffold<C, T> scaffold = new TreeScaffold<>(root, context);
    final TreeTemplate<C, T> rootTemplate = scaffold.rootTemplate();
    final TreeTemplate<C, T> localTemplate;
    if (root != parent) localTemplate = rootTemplate.bindJointPoint(parent, parent);
    else localTemplate = rootTemplate;

    for (Pair<T, T> replacement : replacements)
      localTemplate.bindJointPoint(replacement.getKey(), replacement.getValue());

    scaffold.instantiate();
    return localTemplate.getInstantiated();
  }

  public static <C extends TreeContext<C>, T extends TreeNode<C, T>> T displaceGlobal(
      T target, T rep, boolean copyRep) {
    return displaceGlobal(copyRep ? null : rep.context(), target, rep, copyRep);
  }

  public static <C extends TreeContext<C>, T extends TreeNode<C, T>> T displaceGlobal(
      C ctx, T target, T rep, boolean copyRep) {
    if (target == rep) return target;

    final TreeScaffold<C, T> scaffold = new TreeScaffold<>(treeRootOf(target), ctx);
    final TreeTemplate<C, T> rootTemplate = scaffold.rootTemplate();
    final TreeTemplate<C, T> localTemplate = rootTemplate.bindJointPoint(target, rep);
    localTemplate.setNeedCopy(copyRep);

    scaffold.instantiate();
    return localTemplate.getInstantiated();
  }
}
