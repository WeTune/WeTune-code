package sjtu.ipads.wtune.testbed.runner;

import sjtu.ipads.wtune.common.utils.Args;
import sjtu.ipads.wtune.common.utils.IOSupport;
import sjtu.ipads.wtune.stmt.CalciteStmtProfile;
import sjtu.ipads.wtune.stmt.support.ProfileUpdate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class UpdateCalciteProfileData implements Runner {
  private Path profileFile;

  @Override
  public void prepare(String[] argStrings) throws Exception {
    final Args args = Args.parse(argStrings, 1);

    final Path dataDir = Runner.dataDir();
    final Path dir = dataDir.resolve(args.getOptional("D", "dir", String.class, "profile_calcite"));
    profileFile = dir.resolve(args.getRequired("in", String.class));
    IOSupport.checkFileExists(profileFile);
  }

  @Override
  public void run() throws Exception {
    final List<String> lines = Files.readAllLines(profileFile);
    ProfileUpdate.cleanCalcite();
    for (int i = 0, bound = lines.size(); i < bound; i += 2) {
      final String[] baseProfile = lines.get(i).split(";");
      final String[] optProfile = lines.get(i + 1).split(";");

      final String appName = baseProfile[0];
      final String appId = baseProfile[1];

      int p50Base = -1,
          p90Base = -1,
          p99Base = -1,
          p50OptCalcite = -1,
          p90OptCalcite = -1,
          p99OptCalcite = -1,
          p50OptWeTune = -1,
          p90OptWeTune = -1,
          p99OptWeTune = -1;

      p50Base = Integer.parseInt(baseProfile[3]);
      p90Base = Integer.parseInt(baseProfile[4]);
      p99Base = Integer.parseInt(baseProfile[5]);

      if (optProfile[2].split("_")[1].equals("cal")) {
        p50OptCalcite = Integer.parseInt(optProfile[3]);
        p90OptCalcite = Integer.parseInt(optProfile[4]);
        p99OptCalcite = Integer.parseInt(optProfile[5]);
      } else {
        p50OptWeTune = Integer.parseInt(optProfile[3]);
        p90OptWeTune = Integer.parseInt(optProfile[4]);
        p99OptWeTune = Integer.parseInt(optProfile[5]);
      }

      if (i + 2 < bound) {
        final String[] nextProfile = lines.get(i + 2).split(";");
        if (nextProfile[0].equals(appName) && nextProfile[1].equals(appId)) {
          final String[] optProfile1 = lines.get(i + 1).split(";");
          if (optProfile1[2].split("_")[1].equals("cal")) {
            p50OptCalcite = Integer.parseInt(optProfile1[3]);
            p90OptCalcite = Integer.parseInt(optProfile1[4]);
            p99OptCalcite = Integer.parseInt(optProfile1[5]);
          } else {
            p50OptWeTune = Integer.parseInt(optProfile1[3]);
            p90OptWeTune = Integer.parseInt(optProfile1[4]);
            p99OptWeTune = Integer.parseInt(optProfile1[5]);
          }
        }
      }

      ProfileUpdate.updateCalciteProfile(
          CalciteStmtProfile.mk(
              appName,
              Integer.parseInt(appId),
              p50Base,
              p90Base,
              p99Base,
              p50OptCalcite,
              p90OptCalcite,
              p99OptCalcite,
              p50OptWeTune,
              p90OptWeTune,
              p99OptWeTune));
    }
  }
}
