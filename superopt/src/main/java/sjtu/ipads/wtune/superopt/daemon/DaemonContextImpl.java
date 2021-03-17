package sjtu.ipads.wtune.superopt.daemon;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import sjtu.ipads.wtune.stmt.App;
import sjtu.ipads.wtune.superopt.internal.OptimizerRunner;
import sjtu.ipads.wtune.superopt.optimizer.SubstitutionBank;
import sjtu.ipads.wtune.superopt.profiler.DataSourceFactory;

public class DaemonContextImpl implements DaemonContext {
  private final OptimizerRunner optimizer;
  private final Map<String, App> appMap;
  private final Map<String, Optimizations> optimizationsMap;

  private final Server server;
  private final ExecutorService executor;

  private boolean stopped;

  private DaemonContextImpl(OptimizerRunner optimizer, Server server, ExecutorService executor) {
    this.optimizer = optimizer;
    this.executor = executor;
    this.appMap = new HashMap<>();
    this.optimizationsMap = new HashMap<>();
    this.server = server;
  }

  public static DaemonContext make(Properties config) throws IOException {
    // TODO: read custom context from property

    final String bankPath = config.getProperty("bank_path", "wtune_data/filtered_bank");
    final SubstitutionBank bank =
        SubstitutionBank.make().importFrom(Files.readAllLines(Paths.get(bankPath)), false);

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

    return new DaemonContextImpl(new OptimizerRunner(bank), server, executor);
  }

  @Override
  public App appOf(String contextName) {
    return appMap.computeIfAbsent(contextName, App::of);
  }

  @Override
  public OptimizerRunner optimizer() {
    return optimizer;
  }

  @Override
  public Optimizations optimizationsOf(String contextName) {
    final App app = appOf(contextName);
    return optimizationsMap.computeIfAbsent(contextName, ignored -> makeOptimizations(app));
  }

  private static Optimizations makeOptimizations(App app) {
    return new MySQLOptimizations(app.name(), DataSourceFactory.instance().make(app.dbProps()));
  }

  @Override
  public void run() {
    Runtime.getRuntime().addShutdownHook(new Thread(this::stop));

    new Thread(server::run).start();
    while (!stopped) {
      try {
        final byte[] packet = server.poll();
        executor.execute(() -> new PacketHandlerImpl(this).handle(packet));

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
