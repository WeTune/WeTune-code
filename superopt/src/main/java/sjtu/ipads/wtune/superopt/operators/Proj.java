package sjtu.ipads.wtune.superopt.operators;

import sjtu.ipads.wtune.superopt.Operator;
import sjtu.ipads.wtune.superopt.operators.impl.ProjImpl;

public interface Proj extends Operator {
    static Proj create() {
        return ProjImpl.create();
    }
}
