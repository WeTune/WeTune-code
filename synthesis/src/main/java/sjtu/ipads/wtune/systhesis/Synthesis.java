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
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Comparator.comparingLong;

public class Synthesis {
  public static final System.Logger LOG = System.getLogger("Synthesis.Core");
  public static final int CANDIDATES_BATCH_SIZE = 50;
  public static final int EXPECTED_OPTIMIZED = 1;

  public static SynthesisOutput synthesis(Statement stmt) {
    stmt.retrofitStandard();
    stmt.resolve(ParamResolver.class);

    final List<Statement> references = collectReferences(stmt);
    LOG.log(Level.INFO, "{0} #refs = {1}", stmt, references.size());

    final SynthesisContext ctx = new SynthesisContext(stmt);
    ctx.addStage(RelationMutation.build(ctx, stmt));
    ctx.addStage(PredicateMutation.build(ctx, 2, 1));
    ctx.addStage(ExprListMutation.build(ctx, stmt));
    ctx.addStage(ctx.collector());

    for (Statement reference : references) {
      ctx.setReferenceStmt(reference);
      if (!ctx.start()) break;
    }
    if (references.isEmpty()) ctx.start();

    final SynthesisOutput output = ctx.output();
    // verify the last batch
    long lastVerificationElapsed = output.verificationElapsed;
    ctx.verifyCandidates();
    lastVerificationElapsed = output.verificationElapsed - lastVerificationElapsed;

    final List<Statement> optimized = output.optimized;

    LOG.log(
        Level.INFO,
        "{0} #candidates = {1}, #optimized = {2}",
        stmt,
        output.producedCount,
        optimized.size());

    for (int i = 0; i < Math.min(10, optimized.size()); i++)
      LOG.log(Level.INFO, "{0} opt-{1}\n{2}", stmt, i, optimized.get(i).parsed());

    output.totalRefCount = references.size();

    output.relationElapsed -= output.predicateElapsed;
    output.predicateElapsed -= output.exprListElapsed;
    output.exprListElapsed -= (output.verificationElapsed - lastVerificationElapsed);

    LOG.log(
        Level.INFO,
        "{0} relation: {1}, predicate: {2}, exprlist: {3}, verification: {4}",
        stmt,
        output.relationElapsed,
        output.predicateElapsed,
        output.exprListElapsed,
        output.verificationElapsed);

    return output;
  }

  public static void verify(Statement base, List<Statement> candidates, SynthesisOutput output) {
    if (candidates.size() <= 1) return;

    assert candidates.get(0) == base;
    final String appName = base.appName();

    final String dbType = base.parsed().dbType();
    final List<Statement> others = candidates.subList(1, candidates.size());
    others.forEach(it -> it.parsed().setDbTypeRec(dbType));
    others.forEach(Statement::reResolve);
    ScriptUtils.genWorkload(candidates, appName, "verify");

    try {
      final Process verifyTask =
          new ProcessBuilder("python3", "exec.py", "-p", "verify", "-c", "verify", appName)
              .directory(Setup.current().outputDir().toFile())
              .start();

      final String verified = readCommandOutput(verifyTask);
      LOG.log(Level.INFO, "{0} verification -> {1}", base, verified);
      if (verified == null || verified.isBlank()) return;

      final Process compareTask =
          new ProcessBuilder(
                  "python3", "exec.py", "-p", "compare", "-c", "compare", appName, "-T", verified)
              .directory(Setup.current().outputDir().toFile())
              .start();

      final String optimized = readCommandOutput(compareTask);
      LOG.log(Level.INFO, "{0} comparison -> {1}", base, optimized);
      if (optimized == null) return;

      final String[] pairs = optimized.split(",");
      final String basePair = pairs[0];
      output.baseP50 = Long.parseLong(basePair.substring(basePair.indexOf(';') + 1));

      for (int i = 1; i < pairs.length; i++) {
        final String pair = pairs[i];
        final int sepIndex = pair.indexOf(';');
        output.optimized.add(candidates.get(Integer.parseInt(pair.substring(0, sepIndex))));
        output.optP50.add(Long.parseLong(pair.substring(sepIndex + 1)));
      }

    } catch (IOException | InterruptedException e) {
      LOG.log(Level.WARNING, e);
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

  private static String readCommandOutput(Process p) throws IOException, InterruptedException {
    try (final var reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
      String line;
      while ((line = reader.readLine()) != null)
        if (!line.isEmpty() && line.charAt(0) == '>') return line.substring(1);
      p.waitFor();
      return null;
    } finally {
      p.destroy();
    }
  }
}
