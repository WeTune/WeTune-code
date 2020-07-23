package sjtu.ipads.wtune.systhesis;

import sjtu.ipads.wtune.stmt.Setup;
import sjtu.ipads.wtune.stmt.resolver.ParamResolver;
import sjtu.ipads.wtune.stmt.scriptgen.ScriptUtils;
import sjtu.ipads.wtune.stmt.similarity.output.OutputSimGroup;
import sjtu.ipads.wtune.stmt.statement.Statement;
import sjtu.ipads.wtune.systhesis.exprlist.ExprListMutation;
import sjtu.ipads.wtune.systhesis.predicate.PredicateMutation;
import sjtu.ipads.wtune.systhesis.relation.RelationMutation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Comparator.comparingLong;

public class Synthesis {
  public static final System.Logger LOG = System.getLogger("Synthesis.Core");
  public static final int CANDIDATES_BATCH_SIZE = 500;
  public static final int EXPECTED_OPTIMIZED = 5;

  public static void synthesis(Statement stmt) {
    stmt.retrofitStandard();
    stmt.resolve(ParamResolver.class);

    final List<Statement> references = collectReferences(stmt);
    LOG.log(Level.INFO, "{0} #refs = {1}", stmt, references.size());

    final SynthesisContext ctx = new SynthesisContext(stmt);
    ctx.addStage(RelationMutation.build(stmt));
    ctx.addStage(PredicateMutation.build(ctx, 2, 1));
    ctx.addStage(ExprListMutation.build());
    ctx.addStage(ctx.collector());

    for (Statement reference : references) {
      ctx.setReferenceStmt(reference);
      if (!ctx.start()) break;
    }

    ctx.verifyCandidates();
    final List<Statement> produced = ctx.produced();
    final List<Statement> optimized = ctx.optimized();
    LOG.log(
        Level.INFO,
        "{0} #candidates = {1}, #optimized = {2}",
        stmt,
        produced.size(),
        optimized.size());
    for (int i = 0; i < Math.min(10, optimized.size()); i++)
      LOG.log(Level.INFO, "{0} opt-{1}\n{2}", stmt, i, optimized.get(i).parsed());
  }

  public static List<Statement> verify(Statement base, List<Statement> candidates) {
    if (candidates.size() <= 1) return Collections.emptyList();
    assert candidates.get(0) == base;
    final String appName = base.appName();

    candidates.forEach(Statement::reResolve);
    ScriptUtils.genWorkload(candidates, appName, "verify", false);

    try {
      final Process verifyTask =
          new ProcessBuilder("python3", "exec.py", "-p", "verify", "-c", "verify", appName)
              .directory(Setup.current().outputDir().toFile())
              .start();

      final String verified = readCommandOutput(verifyTask);
      LOG.log(Level.INFO, "{0} verification -> {1}", base, verified);
      if (verified == null) return Collections.emptyList();

      final Process compareTask =
          new ProcessBuilder(
                  "python3", "exec.py", "-p", "compare", "-c", "compare", appName, "-T", verified)
              .directory(Setup.current().outputDir().toFile())
              .start();

      final String optimized = readCommandOutput(compareTask);
      LOG.log(Level.INFO, "{0} comparison -> {1}", base, optimized);
      if (optimized == null) return Collections.emptyList();

      final String[] indexes = optimized.split(",");
      final List<Statement> optimizedStmts = new ArrayList<>(indexes.length);
      for (String index : indexes) optimizedStmts.add(candidates.get(Integer.parseInt(index)));

      return optimizedStmts;

    } catch (IOException e) {
      LOG.log(Level.WARNING, e);
      return Collections.emptyList();
    }
  }

  private static List<Statement> collectReferences(Statement stmt) {
    final long baseTiming = stmt.timing(Statement.TAG_INDEX).p50();
    return stmt.outputSimilarGroups().stream()
        .map(OutputSimGroup::stmts)
        .flatMap(Collection::stream)
        .distinct()
        .filter(it -> it.timing(Statement.TAG_INDEX).p50() < baseTiming)
        .sorted(comparingLong(it -> it.timing(Statement.TAG_INDEX).p50()))
        .collect(Collectors.toList());
  }

  private static String readCommandOutput(Process p) throws IOException {
    try (final var reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
      final String output = reader.readLine();
      if (output.charAt(0) != '>') return null;
      return output.substring(1);
    }
  }
}
