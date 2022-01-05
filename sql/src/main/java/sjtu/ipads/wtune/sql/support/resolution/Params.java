package sjtu.ipads.wtune.sql.support.resolution;

import sjtu.ipads.wtune.sql.ast.AdditionalInfo;
import sjtu.ipads.wtune.sql.ast.SqlNode;

import java.util.function.Consumer;
import java.util.function.Predicate;

public interface Params extends AdditionalInfo<Params> {
  AdditionalInfo.Key<Params> PARAMS = ParamsImpl::new;

  int numParams();

  ParamDesc paramOf(SqlNode node);

  void forEach(Consumer<ParamDesc> consumer);

  boolean forEach(Predicate<ParamDesc> consumer);

  JoinGraph joinGraph();
}
