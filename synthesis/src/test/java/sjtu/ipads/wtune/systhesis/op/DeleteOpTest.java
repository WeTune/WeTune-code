package sjtu.ipads.wtune.systhesis.op;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import sjtu.ipads.wtune.stmt.Setup;
import sjtu.ipads.wtune.stmt.statement.Statement;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static sjtu.ipads.wtune.systhesis.TestHelper.fastRecycleIter;

class DeleteOpTest {
  @BeforeAll
  static void setup() throws ClassNotFoundException {
    Class.forName("org.sqlite.JDBC");
    Setup._default().registerAsGlobal().setup();
  }

  @Test
  void testSimple() {
    final Statement stmt = new Statement();
    stmt.setAppName("test");
    stmt.setRawSql("SELECT a.* FROM a LEFT JOIN b ON a.i = b.x WHERE a.j = 1 ORDER BY b.x");
    stmt.retrofitStandard();

    final OpContext ctx = OpContext.build(stmt);
    final Set<DeleteOp> ops = DeleteOp.collectApplicable(ctx);
    System.out.println(ops);
    assertEquals(3, ops.size());
    for (DeleteOp op : ops) System.out.println(op.apply(ctx.derive()));
  }

  @Test
  void testAll() {
    final List<Statement> stmts = Statement.findAll();

    final List<Integer> opCnt = new ArrayList<>();
    for (Statement stmt : fastRecycleIter(stmts)) {
      stmt.retrofitStandard();
      final OpContext ctx = OpContext.build(stmt);
      final Set<DeleteOp> ops = DeleteOp.collectApplicable(ctx);
      opCnt.add(ops.size());
      //      System.out.println(stmt);
      for (DeleteOp op : ops) op.apply(ctx.derive());
    }
    opCnt.sort(Integer::compareTo);
    System.out.println(opCnt.get(0));
    System.out.println(opCnt.get(opCnt.size() - 1));
    System.out.println(opCnt.get((int) (opCnt.size() * 0.5)));
    System.out.println(opCnt.get((int) (opCnt.size() * 0.9)));
    System.out.println(opCnt.stream().mapToInt(it -> it).average().orElse(0.0));
  }
}
