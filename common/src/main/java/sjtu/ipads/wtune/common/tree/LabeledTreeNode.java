package sjtu.ipads.wtune.common.tree;

import sjtu.ipads.wtune.common.field.Fields;

public interface LabeledTreeNode<
        Kind, C extends LabeledTreeContext<Kind>, N extends LabeledTreeNode<Kind, C, N>>
    extends Fields {
  C context();

  int nodeId();

  Kind kind();

  N parent();
}
