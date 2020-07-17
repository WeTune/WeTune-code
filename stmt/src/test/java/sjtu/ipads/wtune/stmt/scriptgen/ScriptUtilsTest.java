package sjtu.ipads.wtune.stmt.scriptgen;

import org.junit.jupiter.api.Test;
import sjtu.ipads.wtune.stmt.Setup;
import sjtu.ipads.wtune.stmt.statement.Statement;

import java.util.Collections;

class ScriptUtilsTest {
  @Test
  void test() {
    Setup._default().registerAsGlobal();
    final Statement stmt = Statement.findOne("discourse", 2242);
    ScriptUtils.genWorkload(Collections.singletonList(stmt), "discourse", "base", false);
  }
}
