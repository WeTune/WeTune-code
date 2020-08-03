package sjtu.ipads.wtune.stmt.dao.internal;

import sjtu.ipads.wtune.stmt.StmtException;
import sjtu.ipads.wtune.stmt.dao.IssueDao;
import sjtu.ipads.wtune.stmt.statement.Issue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class DbIssueDao extends DbDao implements IssueDao {
  public DbIssueDao(Supplier<Connection> connectionSupplier) {
    super(connectionSupplier);
  }

  private static final String KEY_APP_NAME = "appName";
  private static final String KEY_STMT_ID = "stmtId";

  private static final String SELECT_ITEMS =
      String.format("issue_app_name AS %s, issue_stmt_id AS %s", KEY_APP_NAME, KEY_STMT_ID);
  private static final String SELECT_ITEMS_2 =
      String.format("stat_app_name AS %s, stat_stmt_id AS %s", KEY_APP_NAME, KEY_STMT_ID);
  private static final String SELECT_ALL =
      "SELECT " + SELECT_ITEMS + " FROM wtune_issues ORDER BY issue_app_name, issue_stmt_id";
  private static final String SELECT_BY_APP =
      "SELECT "
          + SELECT_ITEMS
          + " FROM wtune_issues WHERE issue_app_name = ? ORDER BY issue_stmt_id";
  private static final String SELECT_UNCHECKED_BY_APP =
      "SELECT "
          + SELECT_ITEMS_2
          + " FROM wtune_opt_stat"
          + " WHERE stat_app_name = ?"
          + "  AND (stat_app_name, stat_stmt_id) NOT IN ("
          + "   SELECT issue_app_name, issue_stmt_id"
          + "   FROM wtune_issues)";

  private static Issue populateOne(ResultSet rs, Issue issue) throws SQLException {
    issue.setAppName(rs.getString(KEY_APP_NAME));
    issue.setStmtId(rs.getInt(KEY_STMT_ID));
    return issue;
  }

  @Override
  public List<Issue> findAll() {
    try {
      final PreparedStatement ps = prepare(SELECT_ALL);
      final ResultSet rs = ps.executeQuery();

      final List<Issue> issues = new ArrayList<>(200);
      while (rs.next()) issues.add(populateOne(rs, new Issue()));

      return issues;

    } catch (SQLException throwables) {
      throw new StmtException(throwables);
    }
  }

  @Override
  public List<Issue> findByApp(String appName) {
    final PreparedStatement ps;
    try {
      ps = prepare(SELECT_BY_APP);
      ps.setString(1, appName);

      final List<Issue> issues = new ArrayList<>(20);
      final ResultSet rs = ps.executeQuery();
      while (rs.next()) issues.add(populateOne(rs, new Issue()));

      return issues;

    } catch (SQLException throwables) {
      throw new StmtException(throwables);
    }
  }

  @Override
  public List<Issue> findUnchecked(String appName) {
    try {
      final PreparedStatement ps = prepare(SELECT_UNCHECKED_BY_APP);
      ps.setString(1, appName);

      final List<Issue> issues = new ArrayList<>(100);
      final ResultSet rs = ps.executeQuery();
      while (rs.next()) issues.add(populateOne(rs, new Issue()));

      return issues;

    } catch (SQLException throwables) {
      throw new StmtException(throwables);
    }
  }
}
