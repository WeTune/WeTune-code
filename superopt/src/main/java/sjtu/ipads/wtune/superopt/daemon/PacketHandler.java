package sjtu.ipads.wtune.superopt.daemon;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.List;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.stmt.App;
import sjtu.ipads.wtune.stmt.Statement;

public class PacketHandler {
  private final DaemonContext ctx;

  public PacketHandler(DaemonContext ctx) {
    this.ctx = ctx;
  }

  public void handle(byte[] packet) {
    try (final DataInputStream stream = new DataInputStream(new ByteArrayInputStream(packet))) {
      final String contextName = stream.readUTF();
      final String sql = stream.readUTF();

      final Optimizations registration = ctx.optimizationsOf(contextName);
      final App app = ctx.appOf(contextName);

      final Statement stmt = Statement.make(app.name(), sql, null);
      if (registration.contains(stmt)) return;

      final List<ASTNode> optimizations = ctx.optimizer().optimize(stmt);
      final ASTNode optimization = optimizations.isEmpty() ? null : optimizations.get(0);
      registration.register(stmt, optimization);

    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }
}
