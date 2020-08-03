package sjtu.ipads.wtune.stmt.dao.internal;

import sjtu.ipads.wtune.stmt.StmtException;
import sjtu.ipads.wtune.stmt.dao.SchemaPatchDao;
import sjtu.ipads.wtune.stmt.schema.SchemaPatch;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

public class DbSchemaPatchDao extends DbDao implements SchemaPatchDao {
  public DbSchemaPatchDao(Supplier<Connection> connectionSupplier) {
    super(connectionSupplier);
  }

  private static final String KEY_COLUMNS = "columnNames";
  private static final String KEY_APP = "app";
  private static final String KEY_TYPE = "type";
  private static final String KEY_TABLE_NAME = "tableName";
  private static final String KEY_SOURCE = "source";

  private static final String FIND_BY_APP =
      String.format(
          "SELECT patch_app AS %s, patch_type AS %s, patch_table_name AS %s, patch_columns_name AS %s, patch_source AS %s "
              + "FROM wtune_schema_patches "
              + "WHERE patch_app = ?",
          KEY_APP, KEY_TYPE, KEY_TABLE_NAME, KEY_COLUMNS, KEY_SOURCE);
  private static final String DELETE_GENERATED =
      "DELETE FROM wtune_schema_patches WHERE patch_source <> 'manual' AND patch_app = ?";
  private static final String INSERT =
      "INSERT OR REPLACE INTO wtune_schema_patches (patch_app, patch_type, patch_table_name, patch_columns_name, patch_source) "
          + "VALUES (?, ?, ?, ?, ?)";

  private SchemaPatch inflate(SchemaPatch patch, ResultSet set) throws SQLException {
    patch.setApp(set.getString(KEY_APP));
    patch.setType(SchemaPatch.Type.valueOf(set.getString(KEY_TYPE)));
    patch.setTableName(set.getString(KEY_TABLE_NAME));
    patch.setColumnNames(Arrays.asList(set.getString(KEY_COLUMNS).split(",")));
    patch.setSource(set.getString(KEY_SOURCE));
    return patch;
  }

  @Override
  public void beginBatch() {
    begin();
  }

  @Override
  public void endBatch() {
    commit();
  }

  @Override
  public void truncate(String appName) {
    try {
      final PreparedStatement ps = prepare(DELETE_GENERATED);
      ps.setString(1, appName);
      ps.executeUpdate();

    } catch (SQLException throwables) {
      throw new StmtException(throwables);
    }
  }

  @Override
  public List<SchemaPatch> findByApp(String appName) {
    try {
      final PreparedStatement ps = prepare(FIND_BY_APP);
      ps.setString(1, appName);

      final ResultSet rs = ps.executeQuery();

      final List<SchemaPatch> patches = new ArrayList<>(50);
      while (rs.next()) patches.add(inflate(new SchemaPatch(), rs));

      return patches;

    } catch (SQLException throwables) {
      throw new StmtException(throwables);
    }
  }

  @Override
  public void save(SchemaPatch patch) {
    try {
      final PreparedStatement ps = prepare(INSERT);
      ps.setString(1, patch.app());
      ps.setString(2, patch.type().name());
      ps.setString(3, patch.tableName());
      ps.setString(4, String.join(",", patch.columnNames()));
      ps.setString(5, patch.source());

      ps.executeUpdate();

    } catch (SQLException throwables) {
      throw new StmtException(throwables);
    }
  }
}
