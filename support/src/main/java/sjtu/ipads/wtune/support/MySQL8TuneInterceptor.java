package sjtu.ipads.wtune.support;

import static sjtu.ipads.wtune.support.MessageSender.CONFIG_CONTEXT_NAME;
import static sjtu.ipads.wtune.support.MessageSender.CONFIG_MSG_DEST;
import static sjtu.ipads.wtune.support.MessageSender.CONFIG_MSG_PORT;

import com.mysql.cj.MysqlConnection;
import com.mysql.cj.Query;
import com.mysql.cj.interceptors.QueryInterceptor;
import com.mysql.cj.log.Log;
import com.mysql.cj.protocol.Resultset;
import com.mysql.cj.protocol.ServerSession;
import java.util.Properties;
import java.util.function.Supplier;

public class MySQL8TuneInterceptor implements QueryInterceptor {
  private MessageSender sender;

  @Override
  public QueryInterceptor init(MysqlConnection conn, Properties props, Log log) {
    sender = MessageSender.instance();
    if (sender == null) {
      final String name = props.getProperty(CONFIG_CONTEXT_NAME);
      if (name == null) return null;

      final String address = props.getProperty(CONFIG_MSG_DEST, "localhost");
      final int port = Integer.parseInt(props.getProperty(CONFIG_MSG_PORT, "9876"));
      sender = MessageSender.init(address, port, name);
    }
    return this;
  }

  @Override
  public <T extends Resultset> T preProcess(Supplier<String> sql, Query interceptedQuery) {
    final String query = sql.get();
    if (query.startsWith("select") || query.startsWith("SELECT")) sender.enqueue(query);
    return null;
  }

  @Override
  public boolean executeTopLevelOnly() {
    return false;
  }

  @Override
  public void destroy() {}

  @Override
  public <T extends Resultset> T postProcess(
      Supplier<String> sql,
      Query interceptedQuery,
      T originalResultSet,
      ServerSession serverSession) {
    return null;
  }
}
