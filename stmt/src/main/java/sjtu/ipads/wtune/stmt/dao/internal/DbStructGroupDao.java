package sjtu.ipads.wtune.stmt.dao.internal;

import sjtu.ipads.wtune.stmt.dao.StructGroupDao;

import java.sql.Connection;
import java.util.function.Supplier;

public class DbStructGroupDao extends DbGroupDao implements StructGroupDao {
  public DbStructGroupDao(Supplier<Connection> connectionSupplier) {
    super(connectionSupplier, "wtune_struct_group");
  }
}
