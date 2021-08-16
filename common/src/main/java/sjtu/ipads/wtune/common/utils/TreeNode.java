package sjtu.ipads.wtune.common.utils;

public interface TreeNode<T extends TreeNode<T>> {
  T successor();

  T[] predecessors();

  void setSuccessor(T successor);

  void setPredecessor(int index, T predecessor);
}
