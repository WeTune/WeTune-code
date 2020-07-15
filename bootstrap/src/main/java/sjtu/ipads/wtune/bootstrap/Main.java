package sjtu.ipads.wtune.bootstrap;

import sjtu.ipads.wtune.stmt.DefaultSetup;

import java.util.Arrays;

public class Main {
  public static void main(String[] args) {
    DefaultSetup._default().registerAsGlobal();

    switch (args[0]) {
      case "genscript":
        new ScriptGen().doTask(Arrays.asList(args).subList(1, args.length));
        return;
    }
  }
}
