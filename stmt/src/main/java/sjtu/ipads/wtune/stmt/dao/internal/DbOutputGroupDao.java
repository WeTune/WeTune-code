package sjtu.ipads.wtune.stmt.dao.internal;

import sjtu.ipads.wtune.stmt.dao.OutputGroupDao;

import java.sql.Connection;
import java.util.function.Supplier;

public class DbOutputGroupDao extends DbGroupDao implements OutputGroupDao {
  public DbOutputGroupDao(Supplier<Connection> connectionSupplier) {
    super(connectionSupplier, "wtune_output_group");
  }
}
