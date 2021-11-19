package sjtu.ipads.wtune.common.tree;

import sjtu.ipads.wtune.common.field.Fields;

public interface LabeledTreeFields<Kind> extends Fields {
  Kind kind();

  int parent();
}
