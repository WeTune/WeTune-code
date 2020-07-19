package sjtu.ipads.wtune.bootstrap;

import sjtu.ipads.wtune.stmt.DefaultSetup;

import java.util.Arrays;

import static sjtu.ipads.wtune.common.utils.Commons.assertFalse;

public class Main {
  private static final String[] KNOWN_APPS =
      new String[] {
        "broadleaf",
        "diaspora",
        "discourse",
        "eladmin",
        "fatfreecrm",
        "febs",
        "forest_blog",
        "gitlab",
        "guns",
        "halo",
        "homeland",
        "lobsters",
        "publiccms",
        "pybbs",
        "redmine",
        "refinerycms",
        "sagan",
        "shopizer",
        "solidus",
        "spree"
      };
  //              ,
  //              "fanchaoo",
  //        "springblog",
  //        "wordpress"
  //      };

  public static void main(String[] args) {
    DefaultSetup._default().registerAsGlobal();
    assert args.length >= 2;
    final String cmd = args[0];
    final String apps = args[1];
    final String[] appNames;
    if ("all".equals(apps)) appNames = KNOWN_APPS;
    else appNames = apps.split(",");

    final Task task;
    switch (cmd) {
      case "genscript":
        task = new GenScript();
        break;
      case "genpatch":
        task = new PatchSchema();
        break;
      case "maintenance":
        task = new Maintenance();
        break;
      case "updatedb":
        task = new UpdateDb();
        break;
      default:
        assertFalse();
        return;
    }

    task.setArgs(Arrays.copyOfRange(args, 2, args.length));
    task.doTasks(appNames);
  }
}
