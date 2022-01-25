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
    for (int i = 0, bound = lines.size(); i < bound; i += 3) {
      final String[] q0Profile = lines.get(i).split(";");
      final String[] q1Profile = lines.get(i + 1).split(";");
      final String[] optProfile = lines.get(i + 2).split(";");

      final String appName = optProfile[0];
      final int appId = Integer.parseInt(optProfile[1]);

      final int p50BaseQ0 = Integer.parseInt(q0Profile[3]);
      final int p90BaseQ0 = Integer.parseInt(q0Profile[4]);
      final int p99BaseQ0 = Integer.parseInt(q0Profile[5]);
      final int p50BaseQ1 = Integer.parseInt(q1Profile[3]);
      final int p90BaseQ1 = Integer.parseInt(q1Profile[4]);
      final int p99BaseQ1 = Integer.parseInt(q1Profile[5]);
      final int p50Opt = Integer.parseInt(optProfile[3]);
      final int p90Opt = Integer.parseInt(optProfile[4]);
      final int p99Opt = Integer.parseInt(optProfile[5]);

      ProfileUpdate.updateCalciteProfile(
          CalciteStmtProfile.mk(
              appName, appId,
              p50BaseQ0, p90BaseQ0, p99BaseQ0,
              p50BaseQ1, p90BaseQ1, p99BaseQ1,
              p50Opt, p90Opt, p99Opt));
    }
  }
}
