package sjtu.ipads.wtune.stmt.scriptgen;

import java.util.List;

public interface ScriptNode {
  default List<? extends ScriptNode> children() {
    return null;
  }

  default void outputHead(Output out) {}

  default void outputTail(Output out) {}

  default void outputBeforeChild(Output out) {}

  default void outputAfterChild(Output out) {}

  default void outputBeforeChild(Output out, int index, int total) {
    outputBeforeChild(out);
  }

  default void outputAfterChild(Output out, int index, int total) {
    outputAfterChild(out);
  }

  default void output(Output out) {
    outputHead(out);
    final List<? extends ScriptNode> children = children();
    if (children != null)
      for (int i = 0, bound = children.size(); i < bound; i++) {
        outputBeforeChild(out, i, bound);
        children.get(i).output(out);
        outputAfterChild(out, i, bound);
      }
    outputTail(out);
  }
}
