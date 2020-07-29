package sjtu.ipads.wtune.stmt.dao.internal;

import sjtu.ipads.wtune.stmt.StmtException;
import sjtu.ipads.wtune.stmt.dao.GroupDao;
import sjtu.ipads.wtune.stmt.similarity.SimGroup;
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

public abstract class DbGroupDao extends DbDao implements GroupDao {

  public DbGroupDao(Supplier<Connection> connectionSupplier, String tableName) {
    super(connectionSupplier);
    final String selectItems =
        String.format(
            "group_id AS %s, group_app_name AS %s, group_stmt_id AS %s",
            KEY_GROUP_ID, KEY_APP_NAME, KEY_STMT_ID);
    this.selectOne = "SELECT " + selectItems + " FROM " + tableName + " WHERE group_id = ?";
    this.selectItem =
        "SELECT "
            + selectItems
            + " FROM "
            + tableName
            + " WHERE group_id IN ("
            + "  SELECT group_id "
            + "  FROM wtune_output_group "
            + "  WHERE group_app_name = ? AND group_stmt_id = ?"
            + " )"
            + " ORDER BY group_id";
    this.insert =
        "INSERT INTO " + tableName + " (group_id, group_app_name, group_stmt_id) VALUES (?, ?, ?)";
    this.deleteAll = "DELETE FROM " + tableName;
  }

  private static final String KEY_GROUP_ID = "groupId";
  private static final String KEY_APP_NAME = "appName";
  private static final String KEY_STMT_ID = "stmtId";

  private final String selectOne;
  private final String selectItem;
  private final String insert;
  private final String deleteAll;

  @Override
  public void truncate() {
    try {
      prepare(deleteAll).executeUpdate();
    } catch (SQLException throwables) {
      throw new StmtException(throwables);
    }
  }

  @Override
  public void save(SimGroup group) {
    try {
      final PreparedStatement ps = prepare(insert);
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
  public SimGroup findOne(int groupId) {
    try {
      final PreparedStatement ps = prepare(selectOne);
      ps.setInt(1, groupId);
      final ResultSet rs = ps.executeQuery();

      final SimGroup group = new SimGroup();
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
  public List<SimGroup> findByStmt(Statement stmt) {
    try {
      final PreparedStatement ps = prepare(selectItem);
      ps.setString(1, stmt.appName());
      ps.setInt(2, stmt.stmtId());
      final ResultSet rs = ps.executeQuery();

      final List<SimGroup> groups = new ArrayList<>();
      Set<Statement> stmts = null;
      SimGroup group;
      Statement s;
      int curGroupId = -1;

      while (rs.next()) {
        final int groupId = rs.getInt(KEY_GROUP_ID);
        if (groupId != curGroupId) {
          groups.add(group = new SimGroup());
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
