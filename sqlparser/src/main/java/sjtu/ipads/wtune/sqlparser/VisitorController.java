package sjtu.ipads.wtune.sqlparser;

import static sjtu.ipads.wtune.sqlparser.SQLNode.*;
import static sjtu.ipads.wtune.sqlparser.SQLNode.Type.INVALID;
import static java.util.Collections.emptyList;

abstract class VisitorController {
  private static final SQLNode INVALID_NODE = new SQLNode(INVALID);

  static boolean enter(SQLNode n, SQLVisitor v) {
    switch (n.type()) {
      case INVALID:
        return false;

      case TABLE_NAME:
        return v.enterTableName(n);

      case COLUMN_NAME:
        return v.enterColumnName(n);

      case CREATE_TABLE:
        return v.enterCreateTable(n);

      case COLUMN_DEF:
        return v.enterColumnDef(n);

      case INDEX_DEF:
        return v.enterIndexDef(n);

      case KEY_PART:
        return v.enterKeyPart(n);

      case REFERENCES:
        return v.enterReferences(n);
    }

    return false;
  }

  static void visitChildren(SQLNode n, SQLVisitor v) {
    switch (n.type()) {
      case CREATE_TABLE:
        n.get(CREATE_TABLE_NAME).accept(v);
        n.getOr(CREATE_TABLE_COLUMNS, emptyList()).forEach(it -> it.accept(v));
        n.getOr(CREATE_TABLE_CONSTRAINTS, emptyList()).forEach(it -> it.accept(v));
        break;

      case COLUMN_DEF:
        n.get(COLUMN_DEF_NAME).accept(v);
        n.getOr(COLUMN_DEF_REF, INVALID_NODE).accept(v);
        break;

      case REFERENCES:
        n.get(REFERENCES_TABLE).accept(v);
        n.getOr(REFERENCES_COLUMNS, emptyList()).forEach(it -> it.accept(v));
        break;

      case INDEX_DEF:
        final var keys = n.get(INDEX_DEF_KEYS);
        for (SQLNode key : keys) if (key != null) key.accept(v);

        final var refs = n.get(INDEX_DEF_REFS);
        if (refs != null) refs.accept(v);

      case KEY_PART:
      case COLUMN_NAME:
      case TABLE_NAME:
        break;
    }
  }

  static void leave(SQLNode n, SQLVisitor v) {
    switch (n.type()) {
      case INVALID:
        return;

      case TABLE_NAME:
        v.leaveTableName(n);
        break;

      case COLUMN_NAME:
        v.leaveColumnName(n);
        break;

      case CREATE_TABLE:
        v.leaveCreateTable(n);
        break;

      case COLUMN_DEF:
        v.leaveColumnDef(n);
        break;

      case REFERENCES:
        v.leaveReferences(n);
        break;

      case INDEX_DEF:
        v.leaveIndexDef(n);
        break;

      case KEY_PART:
        v.leaveKeyPart(n);
    }
  }
}
