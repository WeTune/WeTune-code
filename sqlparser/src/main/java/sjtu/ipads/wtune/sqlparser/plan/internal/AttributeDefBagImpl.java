package sjtu.ipads.wtune.sqlparser.plan.internal;

import static sjtu.ipads.wtune.sqlparser.ast.ExprFields.COLUMN_REF_COLUMN;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.COLUMN_NAME_COLUMN;
import static sjtu.ipads.wtune.sqlparser.ast.NodeFields.COLUMN_NAME_TABLE;
import static sjtu.ipads.wtune.sqlparser.ast.constants.ExprKind.COLUMN_REF;
import static sjtu.ipads.wtune.sqlparser.util.ASTHelper.simpleName;

import java.util.AbstractList;
import java.util.List;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.plan.AttributeDef;
import sjtu.ipads.wtune.sqlparser.plan.AttributeDefBag;

public class AttributeDefBagImpl extends AbstractList<AttributeDef> implements AttributeDefBag {
  private final List<AttributeDef> attrs;

  public AttributeDefBagImpl(List<AttributeDef> attrs) {
    this.attrs = attrs;
  }

  @Override
  public AttributeDef get(int index) {
    return attrs.get(index);
  }

  @Override
  public int size() {
    return attrs.size();
  }

  @Override
  public int locate(int id) {
    for (int i = 0; i < attrs.size(); i++) if (attrs.get(i).referencesTo(id)) return i;
    return -1;
  }

  @Override
  public int locate(ASTNode columnRef) {
    if (!COLUMN_REF.isInstance(columnRef)) throw new IllegalArgumentException();
    final ASTNode colName = columnRef.get(COLUMN_REF_COLUMN);
    return locate(colName.get(COLUMN_NAME_TABLE), colName.get(COLUMN_NAME_COLUMN));
  }

  @Override
  public int locate(String qualification, String name) {
    qualification = simpleName(qualification);
    name = simpleName(name);

    for (int i = 0; i < attrs.size(); i++)
      if (attrs.get(i).referencesTo(qualification, name)) return i;

    return -1;
  }

  @Override
  public int locate(AttributeDef reference) {
    if (reference == null) return -1;
    for (int i = 0; i < attrs.size(); i++) if (attrs.get(i).equals(reference)) return i;
    return -1;
  }
}
