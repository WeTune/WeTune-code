package sjtu.ipads.wtune.testbed.runner;

import sjtu.ipads.wtune.common.utils.Args;
import sjtu.ipads.wtune.common.utils.IOSupport;
import sjtu.ipads.wtune.stmt.StmtProfile;
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
    final Path dir = dataDir.resolve(args.getOptional("D", "dir", String.class, "profile"));
    profileFile = dir.resolve(args.getOptional("in", String.class, "base_ss_cal.0122204027.csv"));
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
      final int appId = Integer.parseInt(baseProfile[1]);

      final int p50Base = Integer.parseInt(baseProfile[3]);
      final int p50Opt = Integer.parseInt(optProfile[3]);
      final int p90Base = Integer.parseInt(baseProfile[4]);
      final int p90Opt = Integer.parseInt(optProfile[4]);
      final int p99Base = Integer.parseInt(baseProfile[5]);
      final int p99Opt = Integer.parseInt(optProfile[5]);
      ProfileUpdate.updateCalciteProfile(
          StmtProfile.mk(appName, appId, p50Base, p90Base, p99Base, p50Opt, p90Opt, p99Opt));
    }
  }
}
