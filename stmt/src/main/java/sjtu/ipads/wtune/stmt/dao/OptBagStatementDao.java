package sjtu.ipads.wtune.stmt.dao;

import sjtu.ipads.wtune.stmt.Statement;
import sjtu.ipads.wtune.stmt.dao.internal.DbOptBagStatementDao;

import java.util.List;

public interface OptBagStatementDao {
    Statement findOne(String appName, int stmtId);

    List<Statement> findByApp(String appName);

    List<Statement> findAll();

    static OptBagStatementDao instance() {
        return DbOptBagStatementDao.instance();
    }
}
