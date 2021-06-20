package sjtu.ipads.wtune.sqlparser.plan1;

public interface ValueLookup {
  Value lookup(String qualification, String name);
}
