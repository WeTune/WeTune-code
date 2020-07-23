package sjtu.ipads.wtune.stmt.scriptgen;

import com.google.common.collect.Lists;
import sjtu.ipads.wtune.stmt.attrs.Param;
import sjtu.ipads.wtune.stmt.attrs.ParamModifier;

public class ParamGen implements ScriptNode {
  private final Param param;

  public ParamGen(Param param) {
    this.param = param;
  }

  @Override
  public void output(Output out) {
    out.printf("[%d] = { ", param.index());

    assert !param.modifiers().isEmpty();
    for (ParamModifier modifier : Lists.reverse(param.modifiers())) {
      final ParamModifier.Type type = modifier.type();
      final Object[] args = modifier.args();

      out.print("M.").print(type.name().toLowerCase()).print("(");

      for (int i = 0, bound = args.length; i < bound; i++) {
        final Object arg = args[i];
        if (arg == null) out.print("nil");
        else if (arg instanceof String) out.printf("'%s'", arg);
        else out.print(arg.toString());
        if (i != bound - 1) out.print(", ");
      }

      out.print("), ");
    }

    out.print("}");
  }
}
