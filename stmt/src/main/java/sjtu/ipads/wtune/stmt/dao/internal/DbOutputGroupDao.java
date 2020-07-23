package sjtu.ipads.wtune.stmt.dao.internal;

import sjtu.ipads.wtune.stmt.StmtException;
import sjtu.ipads.wtune.stmt.dao.OutputGroupDao;
import sjtu.ipads.wtune.stmt.similarity.output.OutputSimGroup;
import sjtu.ipads.wtune.stmt.statement.Statement;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public class DbOutputGroupDao extends DbDao implements OutputGroupDao {
  public DbOutputGroupDao(Supplier<Connection> connectionSupplier) {
    super(connectionSupplier);
  }

  private static final String KEY_GROUP_ID = "groupId";
  private static final String KEY_APP_NAME = "appName";
  private static final String KEY_STMT_ID = "stmtId";

  private static final String SELECT_ITEMS =
      String.format(
          "group_id AS %s, group_app_name AS %s, group_stmt_id AS %s",
          KEY_GROUP_ID, KEY_APP_NAME, KEY_STMT_ID);

  private static final String SELECT_ONE =
      "SELECT " + SELECT_ITEMS + " FROM wtune_output_group WHERE group_id = ?";

  private static final String SELECT_BY_STMT =
      "SELECT "
          + SELECT_ITEMS
          + " FROM wtune_output_group"
          + " WHERE group_id IN ("
          + "  SELECT group_id "
          + "  FROM wtune_output_group "
          + "  WHERE group_app_name = ? AND group_stmt_id = ?"
          + " )"
          + " ORDER BY group_id";

  private static final String INSERT =
      "INSERT INTO wtune_output_group (group_id, group_app_name, group_stmt_id) VALUES (?, ?, ?)";

  private static final String DELETE_ALL = "DELETE FROM wtune_output_group";

  @Override
  public void truncate() {
    try {
      prepare(DELETE_ALL).executeUpdate();
    } catch (SQLException throwables) {
      throw new StmtException(throwables);
    }
  }

  @Override
  public void save(OutputSimGroup group) {
    try {
      final PreparedStatement ps = prepare(INSERT);
      for (Statement statement : group.stmts()) {
        ps.setInt(1, group.groupId());
        ps.setString(2, statement.appName());
        ps.setInt(3, statement.stmtId());
        ps.addBatch();
      }
      ps.executeBatch();

    } catch (SQLException throwables) {
      throwables.printStackTrace();
    }
  }

  @Override
  public OutputSimGroup findOne(int groupId) {
    try {
      final PreparedStatement ps = prepare(SELECT_ONE);
      ps.setInt(1, groupId);
      final ResultSet rs = ps.executeQuery();

      final OutputSimGroup group = new OutputSimGroup();
      group.setGroupId(groupId);
      final Set<Statement> stmts = new HashSet<>(64);

      while (rs.next()) {
        final Statement stmt = new Statement();
        stmt.setAppName(rs.getString(KEY_APP_NAME));
        stmt.setStmtId(rs.getInt(KEY_STMT_ID));
        stmts.add(stmt);
      }
      group.setStmts(stmts);
      return group;

    } catch (SQLException throwables) {
      throw new StmtException(throwables);
    }
  }

  @Override
  public List<OutputSimGroup> findByStmt(Statement stmt) {
    try {
      final PreparedStatement ps = prepare(SELECT_BY_STMT);
      ps.setString(1, stmt.appName());
      ps.setInt(2, stmt.stmtId());
      final ResultSet rs = ps.executeQuery();

      final List<OutputSimGroup> groups = new ArrayList<>();
      Set<Statement> stmts = null;
      OutputSimGroup group;
      Statement s;
      int curGroupId = -1;

      while (rs.next()) {
        final int groupId = rs.getInt(KEY_GROUP_ID);
        if (groupId != curGroupId) {
          groups.add(group = new OutputSimGroup());
          group.setGroupId(curGroupId = groupId);
          group.setStmts(stmts = new HashSet<>(64));
        }

        assert stmts != null;

        stmts.add(s = new Statement());
        s.setAppName(rs.getString(KEY_APP_NAME));
        s.setStmtId(rs.getInt(KEY_STMT_ID));
      }

      return groups;

    } catch (SQLException throwables) {
      throw new StmtException(throwables);
    }
  }

  @Override
  public void beginBatch() {
    begin();
  }

  @Override
  public void endBatch() {
    commit();
  }
}
