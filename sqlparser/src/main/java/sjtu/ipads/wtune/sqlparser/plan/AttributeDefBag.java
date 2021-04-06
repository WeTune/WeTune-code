package sjtu.ipads.wtune.sqlparser.plan;

import static sjtu.ipads.wtune.common.utils.Commons.safeGet;
import static sjtu.ipads.wtune.common.utils.FuncUtils.listMap;

import java.util.List;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.plan.internal.AttributeDefBagImpl;

public interface AttributeDefBag extends List<AttributeDef> {
  int locate(int id);

  int locate(ASTNode columnRef);

  int locate(String qualification, String name);

  int locate(AttributeDef reference);

  default AttributeDefBag copy() {
    return makeBag(listMap(AttributeDef::copy, this));
  }

  default AttributeDefBag copyWithQualification(String qualification) {
    return makeBag(listMap(it -> it.copyWithQualification(qualification), this));
  }

  default AttributeDef lookup(int id) {
    return safeGet(this, locate(id));
  }

  default AttributeDef lookup(ASTNode columnRef) {
    return safeGet(this, locate(columnRef));
  }

  default AttributeDef lookup(String qualification, String name) {
    return safeGet(this, locate(qualification, name));
  }

  default AttributeDef lookup(AttributeDef reference) {
    return safeGet(this, locate(reference));
  }

  default boolean covers(AttributeDefBag covered) {
    if (this.size() != covered.size()) return false;

    for (AttributeDef coverageAttr : this) if (covered.locate(coverageAttr) == -1) return false;

    return true;
  }

  static AttributeDefBag makeBag(List<AttributeDef> attrs) {
    return new AttributeDefBagImpl(attrs);
  }
}
