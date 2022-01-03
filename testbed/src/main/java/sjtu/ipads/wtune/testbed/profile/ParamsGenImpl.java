package sjtu.ipads.wtune.testbed.profile;

import com.google.common.collect.Iterables;
import sjtu.ipads.wtune.sql.relational.Relation;
import sjtu.ipads.wtune.sql.schema.Column;
import sjtu.ipads.wtune.stmt.resolver.JoinGraph;
import sjtu.ipads.wtune.stmt.resolver.JoinGraph.JoinKey;
import sjtu.ipads.wtune.stmt.resolver.ParamDesc;
import sjtu.ipads.wtune.stmt.resolver.Params;
import sjtu.ipads.wtune.testbed.common.Element;
import sjtu.ipads.wtune.testbed.population.Generator;
import sjtu.ipads.wtune.testbed.population.Generators;
import sjtu.ipads.wtune.testbed.population.PopulationConfig;

import java.lang.System.Logger.Level;
import java.util.*;

import static sjtu.ipads.wtune.testbed.profile.Profiler.LOG;

public class ParamsGenImpl implements ParamsGen {
  private final Params params;
  private final Generators generators;
  private final JoinGraph joinGraph;
  private final PopulationConfig populationConfig;

  private List<Relation> pivotRelations;
  private Map<Relation, Integer> seeds;
  private Map<ParamDesc, Object> values;

  ParamsGenImpl(Params params, Generators generators) {
    this.params = params;
    this.generators = generators;
    this.joinGraph = params.joinGraph();
    this.populationConfig = generators.config();
    this.populationConfig.setNeedPrePopulation(true);
  }

  @Override
  public Params params() {
    return params;
  }

  @Override
  public Generators generators() {
    return generators;
  }

  @Override
  public List<Relation> pivotRelations() {
    return pivotRelations;
  }

  @Override
  public Map<ParamDesc, Object> values() {
    return values;
  }

  @Override
  public void setPivotTables(List<Relation> pivotRelations) {
    this.pivotRelations = pivotRelations;
  }

  @Override
  public boolean setPivotSeed(int seed) {
    if (seeds == null) seeds = new IdentityHashMap<>();
    else seeds.clear();

    if (pivotRelations == null) pivotRelations = determinePivotRelations(joinGraph);

    for (Relation pivotTable : pivotRelations)
      if (!setPivotSeed0(pivotTable, seed)) {
        return false;
      }

    assert seeds.size() == joinGraph.tables().size();
    return true;
  }

  @Override
  public int seedOf(Relation relation) {
    return seeds.getOrDefault(relation, -1);
  }

  @Override
  public boolean generateAll() {
    if (values == null) values = new IdentityHashMap<>();
    else values.clear();

    for (ParamDesc param : params.params()) {
      final ParamGen gen = new ParamGen(this, param);
      if (!gen.generate()) {
        return false;
      }

      values.put(param, gen.value());
    }

    return true;
  }

  private boolean setPivotSeed0(Relation relation, int seed) {
    if (seeds.containsKey(relation)) return true;

    seeds.put(relation, seed);

    for (Relation joined : joinGraph.getJoined(relation)) {
      final JoinKey joinKey = joinGraph.getJoinKey(relation, joined);

      final Column leftCol = joinKey.leftCol(), rightCol = joinKey.rightCol();
      final Generator leftGen = generators.bind(Element.ofColumn(leftCol));
      final Generator rightGen = generators.bind(Element.ofColumn(rightCol));

      final int rightUnits = populationConfig.unitCountOf(joinKey.rightTable().table().name());
      final Object target = leftGen.generate(seed);
      final int rightSeed =
          rightGen.locate(target).filter(it -> it >= 0 && it < rightUnits).findFirst().orElse(-1);

      if (rightSeed == -1) {
        LOG.log(
            Level.DEBUG,
            "cannot find {0} in {1}. cannot set seed {2} for {3}",
            target,
            rightCol,
            seed,
            relation);
        return false;
      }

      if (!setPivotSeed0(joinKey.rightTable(), rightSeed)) return false;
    }

    return true;
  }

  private static List<Relation> determinePivotRelations(JoinGraph graph) {
    final List<Set<Relation>> scc = graph.getSCC();
    final List<Relation> pivotRelations = new ArrayList<>(scc.size());
    for (Set<Relation> component : scc) pivotRelations.add(Iterables.get(component, 0));
    return pivotRelations;
  }
}
