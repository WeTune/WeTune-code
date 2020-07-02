package sjtu.ipads.wtune.stmt.resovler;

import sjtu.ipads.wtune.sqlparser.SQLNode;
import sjtu.ipads.wtune.sqlparser.SQLVisitor;
import sjtu.ipads.wtune.stmt.statement.Statement;

import java.util.concurrent.atomic.AtomicLong;

import static sjtu.ipads.wtune.stmt.attrs.StmtAttrs.NODE_ID;

public class IdResolver implements Resolver, SQLVisitor {
  private static final AtomicLong NEXT_ID = new AtomicLong(0);

  @Override
  public boolean enter(SQLNode node) {
    node.supplyIfAbsent(NODE_ID, NEXT_ID::getAndIncrement);
    return true;
  }

  @Override
  public boolean resolve(Statement stmt, SQLNode node) {
    node.accept(this);
    return true;
  }

  private static final IdResolver INSTANCE = new IdResolver();

  public static IdResolver singleton() {
    return INSTANCE;
  }

  public static void resolve(SQLNode node) {
    INSTANCE.resolve(null, node);
  }
}
