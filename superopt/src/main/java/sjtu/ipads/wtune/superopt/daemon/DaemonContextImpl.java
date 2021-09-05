package sjtu.ipads.wtune.superopt.daemon;

import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.schema.Schema;
import sjtu.ipads.wtune.stmt.App;
import sjtu.ipads.wtune.stmt.Statement;
import sjtu.ipads.wtune.superopt.optimizer.OptimizerSupport;
import sjtu.ipads.wtune.superopt.profiler.ConnectionProvider;
import sjtu.ipads.wtune.superopt.profiler.DataSourceFactory;
import sjtu.ipads.wtune.superopt.substitution.SubstitutionBank;
import sjtu.ipads.wtune.superopt.substitution.SubstitutionSupport;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static sjtu.ipads.wtune.superopt.profiler.ProfilerSupport.pickMinCost;

public class DaemonContextImpl implements DaemonContext {
  private final SubstitutionBank bank;
  private final Map<String, App> appMap;
  private final Map<String, Registration> regs;

  private final Server server;
  private final ExecutorService executor;

  private boolean stopped;

  private DaemonContextImpl(SubstitutionBank bank, Server server, ExecutorService executor) {
    this.bank = bank;
    this.executor = executor;
    this.appMap = new HashMap<>();
    this.regs = new HashMap<>();
    this.server = server;
  }

  public static DaemonContext make(Properties config) throws IOException {
    // TODO: read custom context from property

    final String bankPath = config.getProperty("bank_path", "wtune_data/filtered_bank");
    final SubstitutionBank bank = SubstitutionSupport.loadBank(Paths.get(bankPath));

    final int port = Integer.parseInt(config.getProperty("port", "9876"));
    final String inetAddrStr = config.getProperty("bind_address", "localhost");
    final InetAddress inetAddr = Inet4Address.getByName(inetAddrStr);
    final UDPServer server = new UDPServer(inetAddr, port, new ArrayBlockingQueue<>(1024));

    final String maxWorkersStr = config.getProperty("max_workers");
    final int maxWorkers =
        maxWorkersStr == null
            ? Runtime.getRuntime().availableProcessors()
            : Integer.parseInt(maxWorkersStr);
    final ThreadPoolExecutor executor =
        new ThreadPoolExecutor(1, maxWorkers, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<>(1024));

    return new DaemonContextImpl(bank, server, executor);
  }

  @Override
  public App appOf(String contextName) {
    return appMap.computeIfAbsent(contextName, App::of);
  }

  @Override
  public Registration registrationOf(String contextName) {
    final App app = appOf(contextName);
    return regs.computeIfAbsent(contextName, ignored -> makeRegistration(app));
  }

  @Override
  public ASTNode optimize(Statement stmt) {
    final Schema schema = stmt.app().schema("base");
    final Set<ASTNode> candidates = OptimizerSupport.optimize(bank, schema, stmt.parsed());
    final var result = pickMinCost(schema, stmt.parsed(), candidates, stmt.app().dbProps());
    return result == null ? null : result.getLeft();
  }

  private static Registration makeRegistration(App app) {
    final String dbType = app.dbType();
    final ConnectionProvider connPool =
        DataSourceFactory.instance().make(app.dbProps())::getConnection;
    return Registration.make(dbType, connPool);
  }

  @Override
  public void run() {
    Runtime.getRuntime().addShutdownHook(new Thread(this::stop));

    new Thread(server::run).start();
    while (!stopped) {
      try {
        final byte[] packet = server.poll();
        executor.execute(() -> new PacketHandler(this).handle(packet));

      } catch (InterruptedException ex) {
        break;
      }
    }
  }

  @Override
  public void stop() {
    server.stop();
    stopped = true;
    executor.shutdown();
  }
}
