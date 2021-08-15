package sjtu.ipads.wtune.superopt.runner;

public interface Runner {
  void prepare(String[] args) throws Exception;

  void run(String[] args) throws Exception;
}
