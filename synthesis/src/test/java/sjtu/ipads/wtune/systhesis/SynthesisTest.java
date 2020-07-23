package sjtu.ipads.wtune.systhesis;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import sjtu.ipads.wtune.stmt.Setup;
import sjtu.ipads.wtune.stmt.statement.Statement;

class SynthesisTest {
  @BeforeAll
  static void setup() {
    Setup._default().registerAsGlobal();
  }

  @Test
  void testBroadleaf199() {
    Synthesis.synthesis(Statement.findOne("broadleaf", 241));
  }

  @Test
  void testPybbs16() {
    Synthesis.synthesis(Statement.findOne("pybbs", 16));
  }
}
