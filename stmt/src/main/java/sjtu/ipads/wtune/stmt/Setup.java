package sjtu.ipads.wtune.stmt;

import java.nio.file.Path;

public abstract class Setup {
  private static Setup INSTANCE;

  public static String CSV_SEP = ";";

  public static Setup _default() {
    return new DefaultSetup();
  }

  public static Setup current() {
    return INSTANCE;
  }

  public Setup registerAsGlobal() {
    INSTANCE = this;
    setup();
    return this;
  }

  public abstract void setup();

  public abstract Path dataDir();

  public abstract Path outputDir();
}
