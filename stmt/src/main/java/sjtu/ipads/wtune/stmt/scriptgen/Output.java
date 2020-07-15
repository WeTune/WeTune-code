package sjtu.ipads.wtune.stmt.scriptgen;

import java.util.List;

public interface Output {
  Output increaseIndent();

  Output decreaseIndent();

  Output indent();

  Output println();

  Output println(String str);

  Output print(String str);

  Output printf(String str, Object... args);

  default Output prints(String delimiter, List<String> strs) {
    print(String.join(delimiter, strs));
    return this;
  }

  default Output accept(ScriptNode node) {
    node.output(this);
    return this;
  }
}
