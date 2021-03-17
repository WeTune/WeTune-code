package sjtu.ipads.wtune.superopt.daemon;

public interface Server {
  void run();

  void stop();

  byte[] poll() throws InterruptedException;
}
