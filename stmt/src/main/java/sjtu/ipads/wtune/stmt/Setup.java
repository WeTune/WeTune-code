package sjtu.ipads.wtune.stmt;

import java.nio.file.Path;

public abstract class Setup {
  private static Setup INSTANCE;

  public static Setup _default() {
    return new DefaultSetup();
  }

  public static Setup current() {
    return INSTANCE;
  }

  public Setup registerAsGlobal() {
    INSTANCE = this;
    return this;
  }

  public abstract void setup();

  public abstract Path dataDir();

  public abstract Path scriptOutputDir();
}
