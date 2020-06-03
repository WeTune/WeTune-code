package sjtu.ipads.wtune.sqlparser.mysql;

public class Main {
  public static void main(String[] args) {
    final String a =
        "create table a (k int(100) primary key references b(x), "
            + "index (k(10)), unique (k DESC) using rtree, "
            + "constraint fk_cons foreign key fk (k) references b(x));";
    final var node = MySQLASTParser.parse(a, MySQLParser::createTable);
    System.out.println(node);
  }
}
