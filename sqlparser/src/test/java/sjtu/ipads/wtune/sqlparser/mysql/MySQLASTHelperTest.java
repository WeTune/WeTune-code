package sjtu.ipads.wtune.sqlparser.mysql;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import sjtu.ipads.wtune.sqlparser.mysql.internal.MySQLParser;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static sjtu.ipads.wtune.sqlparser.mysql.MySQLASTHelper.stringifyIdentifier;

public class MySQLASTHelperTest {
  @Test
  @DisplayName("stringify id")
  void testStringifyIdentifier0() {
    final MySQLASTParser parser = new MySQLASTParser();
    final String id0 = "a";
    final String out0 = stringifyIdentifier(parser.parse0(id0, MySQLParser::identifier));
    assertEquals("a", out0);

    final String id1 = "`a`";
    final String out1 = stringifyIdentifier(parser.parse0(id1, MySQLParser::identifier));
    assertEquals("a", out1);

    final String id2 = "execute";
    final String out2 = stringifyIdentifier(parser.parse0(id2, MySQLParser::identifier));
    assertEquals("execute", out2);
  }

  @Test
  @DisplayName("stringify dotId")
  void testStringifyIdentifier1() {
    final MySQLASTParser parser = new MySQLASTParser();
    final String id0 = ".a";
    final String out0 = stringifyIdentifier(parser.parse0(id0, MySQLParser::dotIdentifier));
    assertEquals("a", out0);

    final String id1 = ".`a`";
    final String out1 = stringifyIdentifier(parser.parse0(id1, MySQLParser::dotIdentifier));
    assertEquals("a", out1);
  }

  @Test
  @DisplayName("stringify qualified id")
  void testStringifyIdentifier2() {
    final MySQLASTParser parser = new MySQLASTParser();
    final String id0 = "a";
    final String[] out0 = stringifyIdentifier(parser.parse0(id0, MySQLParser::qualifiedIdentifier));
    assertArrayEquals(new String[] {null, "a"}, out0);

    final String id1 = "a.b";
    final String[] out1 = stringifyIdentifier(parser.parse0(id1, MySQLParser::qualifiedIdentifier));
    assertArrayEquals(new String[] {"a", "b"}, out1);

    final String id2 = "`a`.b";
    final String[] out2 = stringifyIdentifier(parser.parse0(id2, MySQLParser::qualifiedIdentifier));
    assertArrayEquals(new String[] {"a", "b"}, out2);

    final String id3 = "`a`.b";
    final String[] out3 = stringifyIdentifier(parser.parse0(id3, MySQLParser::qualifiedIdentifier));
    assertArrayEquals(new String[] {"a", "b"}, out3);
  }

  @Test
  @DisplayName("stringify field id")
  void testStringifyIdentifier3() {
    final MySQLASTParser parser = new MySQLASTParser();
    final String id0 = "a";
    final String[] out0 = stringifyIdentifier(parser.parse0(id0, MySQLParser::fieldIdentifier));
    assertArrayEquals(new String[] {null, null, "a"}, out0);

    final String id1 = "a.b";
    final String[] out1 = stringifyIdentifier(parser.parse0(id1, MySQLParser::fieldIdentifier));
    assertArrayEquals(new String[] {null, "a", "b"}, out1);

    final String id2 = "a.b.c";

    final String[] out2 = stringifyIdentifier(parser.parse0(id2, MySQLParser::fieldIdentifier));
    assertArrayEquals(new String[] {"a", "b", "c"}, out2);
  }
}
