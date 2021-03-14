package sjtu.ipads.wtune.superopt;

import org.junit.jupiter.api.Test;
import sjtu.ipads.wtune.sqlparser.ast.ASTNode;
import sjtu.ipads.wtune.sqlparser.schema.Schema;
import sjtu.ipads.wtune.stmt.Statement;
import sjtu.ipads.wtune.superopt.optimization.Optimizer;
import sjtu.ipads.wtune.superopt.optimization.SubstitutionBank;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static sjtu.ipads.wtune.stmt.support.Workflow.normalize;

public class TestOptimizerSlow {
  private static SubstitutionBank bank;

  private static SubstitutionBank bank() {
    if (bank != null) return bank;

    bank = SubstitutionBank.make();

    try {
      bank.importFrom(Files.readAllLines(Paths.get("wtune_data", "substitution_bank")));
    } catch (IOException ioe) {
      throw new UncheckedIOException(ioe);
    }

    return bank;
  }

  private static void doTest(String appName, int stmtId, String... expected) {
    final Statement stmt = Statement.findOne(appName, stmtId);
    final ASTNode ast = stmt.parsed();
    final Schema schema = stmt.app().schema("base", true);
    ast.context().setSchema(schema);
    normalize(stmt.parsed());

    final Optimizer optimizer = Optimizer.make(bank(), schema);
    final List<ASTNode> optimized = optimizer.optimize(ast);

    optimized.forEach(System.out::println);
    boolean passed = false;
    for (String s : expected)
      if (optimized.stream().anyMatch(it -> s.equals(it.toString()))) {
        passed = true;
        break;
      }
    assertTrue(passed);
  }

  @Test // 6
  void testDiaspora224() {
    final String appName = "diaspora";
    final int stmtId = 224;
    final String[] expected =
        new String[] {
          "SELECT \"posts\".\"id\" AS \"id\", \"posts\".\"user_id\" AS \"user_id\", \"posts\".\"topic_id\" AS \"topic_id\", \"posts\".\"post_number\" AS \"post_number\", \"posts\".\"raw\" AS \"raw\", \"posts\".\"cooked\" AS \"cooked\", \"posts\".\"created_at\" AS \"created_at\", \"posts\".\"updated_at\" AS \"updated_at\", \"posts\".\"reply_to_post_number\" AS \"reply_to_post_number\", \"posts\".\"reply_count\" AS \"reply_count\", \"posts\".\"quote_count\" AS \"quote_count\", \"posts\".\"deleted_at\" AS \"deleted_at\", \"posts\".\"off_topic_count\" AS \"off_topic_count\", \"posts\".\"like_count\" AS \"like_count\", \"posts\".\"incoming_link_count\" AS \"incoming_link_count\", \"posts\".\"bookmark_count\" AS \"bookmark_count\", \"posts\".\"avg_time\" AS \"avg_time\", \"posts\".\"score\" AS \"score\", \"posts\".\"reads\" AS \"reads\", \"posts\".\"post_type\" AS \"post_type\", \"posts\".\"sort_order\" AS \"sort_order\", \"posts\".\"last_editor_id\" AS \"last_editor_id\", \"posts\".\"hidden\" AS \"hidden\", \"posts\".\"hidden_reason_id\" AS \"hidden_reason_id\", \"posts\".\"notify_moderators_count\" AS \"notify_moderators_count\", \"posts\".\"spam_count\" AS \"spam_count\", \"posts\".\"illegal_count\" AS \"illegal_count\", \"posts\".\"inappropriate_count\" AS \"inappropriate_count\", \"posts\".\"last_version_at\" AS \"last_version_at\", \"posts\".\"user_deleted\" AS \"user_deleted\", \"posts\".\"reply_to_user_id\" AS \"reply_to_user_id\", \"posts\".\"percent_rank\" AS \"percent_rank\", \"posts\".\"notify_user_count\" AS \"notify_user_count\", \"posts\".\"like_score\" AS \"like_score\", \"posts\".\"deleted_by_id\" AS \"deleted_by_id\", \"posts\".\"edit_reason\" AS \"edit_reason\", \"posts\".\"word_count\" AS \"word_count\", \"posts\".\"version\" AS \"version\", \"posts\".\"cook_method\" AS \"cook_method\", \"posts\".\"wiki\" AS \"wiki\", \"posts\".\"baked_at\" AS \"baked_at\", \"posts\".\"baked_version\" AS \"baked_version\", \"posts\".\"hidden_at\" AS \"hidden_at\", \"posts\".\"self_edits\" AS \"self_edits\", \"posts\".\"reply_quoted\" AS \"reply_quoted\", \"posts\".\"via_email\" AS \"via_email\", \"posts\".\"raw_email\" AS \"raw_email\", \"posts\".\"public_version\" AS \"public_version\", \"posts\".\"action_code\" AS \"action_code\", \"posts\".\"image_url\" AS \"image_url\", \"posts\".\"locked_by_id\" AS \"locked_by_id\" FROM \"posts\" AS \"posts\" INNER JOIN \"topics\" AS \"topics\" ON \"posts\".\"topic_id\" = \"topics\".\"id\" INNER JOIN \"group_users\" AS \"group_users\" ON \"posts\".\"user_id\" = \"group_users\".\"user_id\" WHERE NOT \"posts\".\"deleted_at\" IS NULL AND NOT \"topics\".\"deleted_at\" IS NULL AND \"group_users\".\"group_id\" = 2412 AND \"posts\".\"post_type\" = 1 AND \"topics\".\"archetype\" <> 'private_message' AND (\"topics\".\"category_id\" IS NULL OR \"topics\".\"category_id\" IN ($1)) AND \"topics\".\"visible\" IS TRUE ORDER BY \"created_at\" DESC LIMIT 50",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 26
  void testDiscourse877() {
    final String appName = "discourse";
    final int stmtId = 877;
    final String[] expected =
        new String[] {
          "SELECT \"posts\".\"id\" AS \"id\", \"posts\".\"user_id\" AS \"user_id\", \"posts\".\"topic_id\" AS \"topic_id\", \"posts\".\"post_number\" AS \"post_number\", \"posts\".\"raw\" AS \"raw\", \"posts\".\"cooked\" AS \"cooked\", \"posts\".\"created_at\" AS \"created_at\", \"posts\".\"updated_at\" AS \"updated_at\", \"posts\".\"reply_to_post_number\" AS \"reply_to_post_number\", \"posts\".\"reply_count\" AS \"reply_count\", \"posts\".\"quote_count\" AS \"quote_count\", \"posts\".\"deleted_at\" AS \"deleted_at\", \"posts\".\"off_topic_count\" AS \"off_topic_count\", \"posts\".\"like_count\" AS \"like_count\", \"posts\".\"incoming_link_count\" AS \"incoming_link_count\", \"posts\".\"bookmark_count\" AS \"bookmark_count\", \"posts\".\"avg_time\" AS \"avg_time\", \"posts\".\"score\" AS \"score\", \"posts\".\"reads\" AS \"reads\", \"posts\".\"post_type\" AS \"post_type\", \"posts\".\"sort_order\" AS \"sort_order\", \"posts\".\"last_editor_id\" AS \"last_editor_id\", \"posts\".\"hidden\" AS \"hidden\", \"posts\".\"hidden_reason_id\" AS \"hidden_reason_id\", \"posts\".\"notify_moderators_count\" AS \"notify_moderators_count\", \"posts\".\"spam_count\" AS \"spam_count\", \"posts\".\"illegal_count\" AS \"illegal_count\", \"posts\".\"inappropriate_count\" AS \"inappropriate_count\", \"posts\".\"last_version_at\" AS \"last_version_at\", \"posts\".\"user_deleted\" AS \"user_deleted\", \"posts\".\"reply_to_user_id\" AS \"reply_to_user_id\", \"posts\".\"percent_rank\" AS \"percent_rank\", \"posts\".\"notify_user_count\" AS \"notify_user_count\", \"posts\".\"like_score\" AS \"like_score\", \"posts\".\"deleted_by_id\" AS \"deleted_by_id\", \"posts\".\"edit_reason\" AS \"edit_reason\", \"posts\".\"word_count\" AS \"word_count\", \"posts\".\"version\" AS \"version\", \"posts\".\"cook_method\" AS \"cook_method\", \"posts\".\"wiki\" AS \"wiki\", \"posts\".\"baked_at\" AS \"baked_at\", \"posts\".\"baked_version\" AS \"baked_version\", \"posts\".\"hidden_at\" AS \"hidden_at\", \"posts\".\"self_edits\" AS \"self_edits\", \"posts\".\"reply_quoted\" AS \"reply_quoted\", \"posts\".\"via_email\" AS \"via_email\", \"posts\".\"raw_email\" AS \"raw_email\", \"posts\".\"public_version\" AS \"public_version\", \"posts\".\"action_code\" AS \"action_code\", \"posts\".\"image_url\" AS \"image_url\", \"posts\".\"locked_by_id\" AS \"locked_by_id\" FROM \"posts\" AS \"posts\" INNER JOIN \"topics\" AS \"topics\" ON \"posts\".\"topic_id\" = \"topics\".\"id\" INNER JOIN \"group_users\" AS \"group_users\" ON \"posts\".\"user_id\" = \"group_users\".\"user_id\" WHERE NOT \"posts\".\"deleted_at\" IS NULL AND NOT \"topics\".\"deleted_at\" IS NULL AND \"group_users\".\"group_id\" = 2412 AND \"posts\".\"post_type\" = 1 AND \"topics\".\"archetype\" <> 'private_message' AND (\"topics\".\"category_id\" IS NULL OR \"topics\".\"category_id\" IN ($1)) AND \"topics\".\"visible\" IS TRUE ORDER BY \"created_at\" DESC LIMIT 50",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 34
  void testDiscourse1000() {
    final String appName = "discourse";
    final int stmtId = 1000;
    final String[] expected =
        new String[] {
          "SELECT \"posts\".\"id\" AS \"id\", \"posts\".\"user_id\" AS \"user_id\", \"posts\".\"topic_id\" AS \"topic_id\", \"posts\".\"post_number\" AS \"post_number\", \"posts\".\"raw\" AS \"raw\", \"posts\".\"cooked\" AS \"cooked\", \"posts\".\"created_at\" AS \"created_at\", \"posts\".\"updated_at\" AS \"updated_at\", \"posts\".\"reply_to_post_number\" AS \"reply_to_post_number\", \"posts\".\"reply_count\" AS \"reply_count\", \"posts\".\"quote_count\" AS \"quote_count\", \"posts\".\"deleted_at\" AS \"deleted_at\", \"posts\".\"off_topic_count\" AS \"off_topic_count\", \"posts\".\"like_count\" AS \"like_count\", \"posts\".\"incoming_link_count\" AS \"incoming_link_count\", \"posts\".\"bookmark_count\" AS \"bookmark_count\", \"posts\".\"avg_time\" AS \"avg_time\", \"posts\".\"score\" AS \"score\", \"posts\".\"reads\" AS \"reads\", \"posts\".\"post_type\" AS \"post_type\", \"posts\".\"sort_order\" AS \"sort_order\", \"posts\".\"last_editor_id\" AS \"last_editor_id\", \"posts\".\"hidden\" AS \"hidden\", \"posts\".\"hidden_reason_id\" AS \"hidden_reason_id\", \"posts\".\"notify_moderators_count\" AS \"notify_moderators_count\", \"posts\".\"spam_count\" AS \"spam_count\", \"posts\".\"illegal_count\" AS \"illegal_count\", \"posts\".\"inappropriate_count\" AS \"inappropriate_count\", \"posts\".\"last_version_at\" AS \"last_version_at\", \"posts\".\"user_deleted\" AS \"user_deleted\", \"posts\".\"reply_to_user_id\" AS \"reply_to_user_id\", \"posts\".\"percent_rank\" AS \"percent_rank\", \"posts\".\"notify_user_count\" AS \"notify_user_count\", \"posts\".\"like_score\" AS \"like_score\", \"posts\".\"deleted_by_id\" AS \"deleted_by_id\", \"posts\".\"edit_reason\" AS \"edit_reason\", \"posts\".\"word_count\" AS \"word_count\", \"posts\".\"version\" AS \"version\", \"posts\".\"cook_method\" AS \"cook_method\", \"posts\".\"wiki\" AS \"wiki\", \"posts\".\"baked_at\" AS \"baked_at\", \"posts\".\"baked_version\" AS \"baked_version\", \"posts\".\"hidden_at\" AS \"hidden_at\", \"posts\".\"self_edits\" AS \"self_edits\", \"posts\".\"reply_quoted\" AS \"reply_quoted\", \"posts\".\"via_email\" AS \"via_email\", \"posts\".\"raw_email\" AS \"raw_email\", \"posts\".\"public_version\" AS \"public_version\", \"posts\".\"action_code\" AS \"action_code\", \"posts\".\"image_url\" AS \"image_url\", \"posts\".\"locked_by_id\" AS \"locked_by_id\" FROM \"posts\" AS \"posts\" INNER JOIN \"topics\" AS \"topics\" ON \"posts\".\"topic_id\" = \"topics\".\"id\" INNER JOIN \"group_users\" AS \"group_users\" ON \"posts\".\"user_id\" = \"group_users\".\"user_id\" WHERE NOT \"posts\".\"deleted_at\" IS NULL AND NOT \"topics\".\"deleted_at\" IS NULL AND \"group_users\".\"group_id\" = 2412 AND \"posts\".\"post_type\" = 1 AND \"topics\".\"archetype\" <> 'private_message' AND (\"topics\".\"category_id\" IS NULL OR \"topics\".\"category_id\" IN ($1)) AND \"topics\".\"visible\" IS TRUE ORDER BY \"created_at\" DESC LIMIT 50",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 38
  void testDiscourse1173() {
    final String appName = "discourse";
    final int stmtId = 1173;
    final String[] expected = new String[] {};
    doTest(appName, stmtId, expected);
  }

  @Test // 39
  void testDiscourse1174() {
    final String appName = "discourse";
    final int stmtId = 1174;
    final String[] expected = new String[] {};
    doTest(appName, stmtId, expected);
  }

  @Test // 46
  void testDiscourse1191() {
    final String appName = "discourse";
    final int stmtId = 1191;
    final String[] expected = new String[] {};
    doTest(appName, stmtId, expected);
  }

  @Test // 47
  void testDiscourse1196() {
    final String appName = "discourse";
    final int stmtId = 1196;
    final String[] expected = new String[] {};
    doTest(appName, stmtId, expected);
  }

  @Test // 48
  void testDiscourse1200() {
    final String appName = "discourse";
    final int stmtId = 1200;
    final String[] expected = new String[] {};
    doTest(appName, stmtId, expected);
  }

  @Test // 49
  void testDiscourse1213() {
    final String appName = "discourse";
    final int stmtId = 1213;
    final String[] expected = new String[] {};
    doTest(appName, stmtId, expected);
  }

  @Test // 50
  void testDiscourse1214() {
    final String appName = "discourse";
    final int stmtId = 1214;
    final String[] expected = new String[] {};
    doTest(appName, stmtId, expected);
  }

  @Test // 51
  void testDiscourse1216() {
    final String appName = "discourse";
    final int stmtId = 1216;
    final String[] expected = new String[] {};
    doTest(appName, stmtId, expected);
  }

  @Test // 70
  void testDiscourse3825() {
    final String appName = "discourse";
    final int stmtId = 3825;
    final String[] expected = new String[] {};
    doTest(appName, stmtId, expected);
  }

  @Test // 71
  void testDiscourse3829() {
    final String appName = "discourse";
    final int stmtId = 3829;
    final String[] expected = new String[] {};
    doTest(appName, stmtId, expected);
  }

  @Test // 72
  void testDiscourse3831() {
    final String appName = "discourse";
    final int stmtId = 3831;
    final String[] expected = new String[] {};
    doTest(appName, stmtId, expected);
  }

  @Test // 73
  void testDiscourse3842() {
    final String appName = "discourse";
    final int stmtId = 3842;
    final String[] expected = new String[] {};
    doTest(appName, stmtId, expected);
  }

  @Test // 123
  void testShopizer3() {
    final String appName = "shopizer";
    final int stmtId = 3;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM `watchers` AS `watchers` WHERE `watchers`.`watchable_id` = 1 AND `watchers`.`watchable_type` = 'Issue'",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 126
  void testShopizer24() {
    final String appName = "shopizer";
    final int stmtId = 24;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM `watchers` AS `watchers` WHERE `watchers`.`watchable_id` = 1 AND `watchers`.`watchable_type` = 'Issue'",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 128
  void testShopizer39() {
    final String appName = "shopizer";
    final int stmtId = 39;
    final String[] expected =
        new String[] {
          "SELECT COUNT(*) FROM `watchers` AS `watchers` WHERE `watchers`.`watchable_id` = 1 AND `watchers`.`watchable_type` = 'Issue'",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 131
  void testShopizer57() {
    final String appName = "shopizer";
    final int stmtId = 57;
    final String[] expected =
        new String[] {
          "SELECT `productrel0_`.`product_relationship_id` AS `product_1_56_0_`, `product1_`.`product_id` AS `product_1_42_1_`, `product2_`.`product_id` AS `product_1_42_2_`, `descriptio3_`.`description_id` AS `descript1_46_3_`, `productrel0_`.`active` AS `active2_56_0_`, `productrel0_`.`code` AS `code3_56_0_`, `productrel0_`.`product_id` AS `product_4_56_0_`, `productrel0_`.`related_product_id` AS `related_5_56_0_`, `productrel0_`.`merchant_id` AS `merchant6_56_0_`, `product1_`.`date_created` AS `date_cre2_42_1_`, `product1_`.`date_modified` AS `date_mod3_42_1_`, `product1_`.`updt_id` AS `updt_id4_42_1_`, `product1_`.`available` AS `availabl5_42_1_`, `product1_`.`cond` AS `cond6_42_1_`, `product1_`.`date_available` AS `date_ava7_42_1_`, `product1_`.`manufacturer_id` AS `manufac25_42_1_`, `product1_`.`merchant_id` AS `merchan26_42_1_`, `product1_`.`customer_id` AS `custome27_42_1_`, `product1_`.`preorder` AS `preorder8_42_1_`, `product1_`.`product_height` AS `product_9_42_1_`, `product1_`.`product_free` AS `product10_42_1_`, `product1_`.`product_length` AS `product11_42_1_`, `product1_`.`quantity_ordered` AS `quantit12_42_1_`, `product1_`.`review_avg` AS `review_13_42_1_`, `product1_`.`review_count` AS `review_14_42_1_`, `product1_`.`product_ship` AS `product15_42_1_`, `product1_`.`product_virtual` AS `product16_42_1_`, `product1_`.`product_weight` AS `product17_42_1_`, `product1_`.`product_width` AS `product18_42_1_`, `product1_`.`ref_sku` AS `ref_sku19_42_1_`, `product1_`.`rental_duration` AS `rental_20_42_1_`, `product1_`.`rental_period` AS `rental_21_42_1_`, `product1_`.`rental_status` AS `rental_22_42_1_`, `product1_`.`sku` AS `sku23_42_1_`, `product1_`.`sort_order` AS `sort_or24_42_1_`, `product1_`.`tax_class_id` AS `tax_cla28_42_1_`, `product1_`.`product_type_id` AS `product29_42_1_`, `product2_`.`date_created` AS `date_cre2_42_2_`, `product2_`.`date_modified` AS `date_mod3_42_2_`, `product2_`.`updt_id` AS `updt_id4_42_2_`, `product2_`.`available` AS `availabl5_42_2_`, `product2_`.`cond` AS `cond6_42_2_`, `product2_`.`date_available` AS `date_ava7_42_2_`, `product2_`.`manufacturer_id` AS `manufac25_42_2_`, `product2_`.`merchant_id` AS `merchan26_42_2_`, `product2_`.`customer_id` AS `custome27_42_2_`, `product2_`.`preorder` AS `preorder8_42_2_`, `product2_`.`product_height` AS `product_9_42_2_`, `product2_`.`product_free` AS `product10_42_2_`, `product2_`.`product_length` AS `product11_42_2_`, `product2_`.`quantity_ordered` AS `quantit12_42_2_`, `product2_`.`review_avg` AS `review_13_42_2_`, `product2_`.`review_count` AS `review_14_42_2_`, `product2_`.`product_ship` AS `product15_42_2_`, `product2_`.`product_virtual` AS `product16_42_2_`, `product2_`.`product_weight` AS `product17_42_2_`, `product2_`.`product_width` AS `product18_42_2_`, `product2_`.`ref_sku` AS `ref_sku19_42_2_`, `product2_`.`rental_duration` AS `rental_20_42_2_`, `product2_`.`rental_period` AS `rental_21_42_2_`, `product2_`.`rental_status` AS `rental_22_42_2_`, `product2_`.`sku` AS `sku23_42_2_`, `product2_`.`sort_order` AS `sort_or24_42_2_`, `product2_`.`tax_class_id` AS `tax_cla28_42_2_`, `product2_`.`product_type_id` AS `product29_42_2_`, `descriptio3_`.`date_created` AS `date_cre2_46_3_`, `descriptio3_`.`date_modified` AS `date_mod3_46_3_`, `descriptio3_`.`updt_id` AS `updt_id4_46_3_`, `descriptio3_`.`description` AS `descript5_46_3_`, `descriptio3_`.`language_id` AS `languag14_46_3_`, `descriptio3_`.`name` AS `name6_46_3_`, `descriptio3_`.`title` AS `title7_46_3_`, `descriptio3_`.`meta_description` AS `meta_des8_46_3_`, `descriptio3_`.`meta_keywords` AS `meta_key9_46_3_`, `descriptio3_`.`meta_title` AS `meta_ti10_46_3_`, `descriptio3_`.`product_id` AS `product15_46_3_`, `descriptio3_`.`download_lnk` AS `downloa11_46_3_`, `descriptio3_`.`product_highlight` AS `product12_46_3_`, `descriptio3_`.`sef_url` AS `sef_url13_46_3_`, `descriptio3_`.`product_id` AS `product15_46_0__`, `descriptio3_`.`description_id` AS `descript1_46_0__` FROM `product_relationship` AS `productrel0_` INNER JOIN `product` AS `product2_` ON `productrel0_`.`related_product_id` = `product2_`.`product_id` INNER JOIN `product` AS `product1_` ON `productrel0_`.`product_id` = `product1_`.`product_id` INNER JOIN `product_description` AS `descriptio3_` ON `product2_`.`product_id` = `descriptio3_`.`product_id` WHERE `productrel0_`.`code` = 'RELATED_ITEM' AND `productrel0_`.`merchant_id` = 1 AND `product1_`.`product_id` = 2 AND `descriptio3_`.`language_id` = 1",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 133
  void testShopizer68() {
    final String appName = "shopizer";
    final int stmtId = 68;
    final String[] expected =
        new String[] {
          "SELECT `productrel0_`.`product_relationship_id` AS `product_1_56_0_`, `product1_`.`product_id` AS `product_1_42_1_`, `product2_`.`product_id` AS `product_1_42_2_`, `descriptio3_`.`description_id` AS `descript1_46_3_`, `productrel0_`.`active` AS `active2_56_0_`, `productrel0_`.`code` AS `code3_56_0_`, `productrel0_`.`product_id` AS `product_4_56_0_`, `productrel0_`.`related_product_id` AS `related_5_56_0_`, `productrel0_`.`merchant_id` AS `merchant6_56_0_`, `product1_`.`date_created` AS `date_cre2_42_1_`, `product1_`.`date_modified` AS `date_mod3_42_1_`, `product1_`.`updt_id` AS `updt_id4_42_1_`, `product1_`.`available` AS `availabl5_42_1_`, `product1_`.`cond` AS `cond6_42_1_`, `product1_`.`date_available` AS `date_ava7_42_1_`, `product1_`.`manufacturer_id` AS `manufac25_42_1_`, `product1_`.`merchant_id` AS `merchan26_42_1_`, `product1_`.`customer_id` AS `custome27_42_1_`, `product1_`.`preorder` AS `preorder8_42_1_`, `product1_`.`product_height` AS `product_9_42_1_`, `product1_`.`product_free` AS `product10_42_1_`, `product1_`.`product_length` AS `product11_42_1_`, `product1_`.`quantity_ordered` AS `quantit12_42_1_`, `product1_`.`review_avg` AS `review_13_42_1_`, `product1_`.`review_count` AS `review_14_42_1_`, `product1_`.`product_ship` AS `product15_42_1_`, `product1_`.`product_virtual` AS `product16_42_1_`, `product1_`.`product_weight` AS `product17_42_1_`, `product1_`.`product_width` AS `product18_42_1_`, `product1_`.`ref_sku` AS `ref_sku19_42_1_`, `product1_`.`rental_duration` AS `rental_20_42_1_`, `product1_`.`rental_period` AS `rental_21_42_1_`, `product1_`.`rental_status` AS `rental_22_42_1_`, `product1_`.`sku` AS `sku23_42_1_`, `product1_`.`sort_order` AS `sort_or24_42_1_`, `product1_`.`tax_class_id` AS `tax_cla28_42_1_`, `product1_`.`product_type_id` AS `product29_42_1_`, `product2_`.`date_created` AS `date_cre2_42_2_`, `product2_`.`date_modified` AS `date_mod3_42_2_`, `product2_`.`updt_id` AS `updt_id4_42_2_`, `product2_`.`available` AS `availabl5_42_2_`, `product2_`.`cond` AS `cond6_42_2_`, `product2_`.`date_available` AS `date_ava7_42_2_`, `product2_`.`manufacturer_id` AS `manufac25_42_2_`, `product2_`.`merchant_id` AS `merchan26_42_2_`, `product2_`.`customer_id` AS `custome27_42_2_`, `product2_`.`preorder` AS `preorder8_42_2_`, `product2_`.`product_height` AS `product_9_42_2_`, `product2_`.`product_free` AS `product10_42_2_`, `product2_`.`product_length` AS `product11_42_2_`, `product2_`.`quantity_ordered` AS `quantit12_42_2_`, `product2_`.`review_avg` AS `review_13_42_2_`, `product2_`.`review_count` AS `review_14_42_2_`, `product2_`.`product_ship` AS `product15_42_2_`, `product2_`.`product_virtual` AS `product16_42_2_`, `product2_`.`product_weight` AS `product17_42_2_`, `product2_`.`product_width` AS `product18_42_2_`, `product2_`.`ref_sku` AS `ref_sku19_42_2_`, `product2_`.`rental_duration` AS `rental_20_42_2_`, `product2_`.`rental_period` AS `rental_21_42_2_`, `product2_`.`rental_status` AS `rental_22_42_2_`, `product2_`.`sku` AS `sku23_42_2_`, `product2_`.`sort_order` AS `sort_or24_42_2_`, `product2_`.`tax_class_id` AS `tax_cla28_42_2_`, `product2_`.`product_type_id` AS `product29_42_2_`, `descriptio3_`.`date_created` AS `date_cre2_46_3_`, `descriptio3_`.`date_modified` AS `date_mod3_46_3_`, `descriptio3_`.`updt_id` AS `updt_id4_46_3_`, `descriptio3_`.`description` AS `descript5_46_3_`, `descriptio3_`.`language_id` AS `languag14_46_3_`, `descriptio3_`.`name` AS `name6_46_3_`, `descriptio3_`.`title` AS `title7_46_3_`, `descriptio3_`.`meta_description` AS `meta_des8_46_3_`, `descriptio3_`.`meta_keywords` AS `meta_key9_46_3_`, `descriptio3_`.`meta_title` AS `meta_ti10_46_3_`, `descriptio3_`.`product_id` AS `product15_46_3_`, `descriptio3_`.`download_lnk` AS `downloa11_46_3_`, `descriptio3_`.`product_highlight` AS `product12_46_3_`, `descriptio3_`.`sef_url` AS `sef_url13_46_3_`, `descriptio3_`.`product_id` AS `product15_46_0__`, `descriptio3_`.`description_id` AS `descript1_46_0__` FROM `product_relationship` AS `productrel0_` INNER JOIN `product` AS `product2_` ON `productrel0_`.`related_product_id` = `product2_`.`product_id` INNER JOIN `product` AS `product1_` ON `productrel0_`.`product_id` = `product1_`.`product_id` INNER JOIN `product_description` AS `descriptio3_` ON `product2_`.`product_id` = `descriptio3_`.`product_id` WHERE `productrel0_`.`code` = 'RELATED_ITEM' AND `productrel0_`.`merchant_id` = 1 AND `product1_`.`product_id` = 2 AND `descriptio3_`.`language_id` = 1",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 134
  void testShopizer119() {
    final String appName = "shopizer";
    final int stmtId = 119;
    final String[] expected =
        new String[] {
          "SELECT `productrel0_`.`product_relationship_id` AS `product_1_56_0_`, `product1_`.`product_id` AS `product_1_42_1_`, `product2_`.`product_id` AS `product_1_42_2_`, `descriptio3_`.`description_id` AS `descript1_46_3_`, `productrel0_`.`active` AS `active2_56_0_`, `productrel0_`.`code` AS `code3_56_0_`, `productrel0_`.`product_id` AS `product_4_56_0_`, `productrel0_`.`related_product_id` AS `related_5_56_0_`, `productrel0_`.`merchant_id` AS `merchant6_56_0_`, `product1_`.`date_created` AS `date_cre2_42_1_`, `product1_`.`date_modified` AS `date_mod3_42_1_`, `product1_`.`updt_id` AS `updt_id4_42_1_`, `product1_`.`available` AS `availabl5_42_1_`, `product1_`.`cond` AS `cond6_42_1_`, `product1_`.`date_available` AS `date_ava7_42_1_`, `product1_`.`manufacturer_id` AS `manufac25_42_1_`, `product1_`.`merchant_id` AS `merchan26_42_1_`, `product1_`.`customer_id` AS `custome27_42_1_`, `product1_`.`preorder` AS `preorder8_42_1_`, `product1_`.`product_height` AS `product_9_42_1_`, `product1_`.`product_free` AS `product10_42_1_`, `product1_`.`product_length` AS `product11_42_1_`, `product1_`.`quantity_ordered` AS `quantit12_42_1_`, `product1_`.`review_avg` AS `review_13_42_1_`, `product1_`.`review_count` AS `review_14_42_1_`, `product1_`.`product_ship` AS `product15_42_1_`, `product1_`.`product_virtual` AS `product16_42_1_`, `product1_`.`product_weight` AS `product17_42_1_`, `product1_`.`product_width` AS `product18_42_1_`, `product1_`.`ref_sku` AS `ref_sku19_42_1_`, `product1_`.`rental_duration` AS `rental_20_42_1_`, `product1_`.`rental_period` AS `rental_21_42_1_`, `product1_`.`rental_status` AS `rental_22_42_1_`, `product1_`.`sku` AS `sku23_42_1_`, `product1_`.`sort_order` AS `sort_or24_42_1_`, `product1_`.`tax_class_id` AS `tax_cla28_42_1_`, `product1_`.`product_type_id` AS `product29_42_1_`, `product2_`.`date_created` AS `date_cre2_42_2_`, `product2_`.`date_modified` AS `date_mod3_42_2_`, `product2_`.`updt_id` AS `updt_id4_42_2_`, `product2_`.`available` AS `availabl5_42_2_`, `product2_`.`cond` AS `cond6_42_2_`, `product2_`.`date_available` AS `date_ava7_42_2_`, `product2_`.`manufacturer_id` AS `manufac25_42_2_`, `product2_`.`merchant_id` AS `merchan26_42_2_`, `product2_`.`customer_id` AS `custome27_42_2_`, `product2_`.`preorder` AS `preorder8_42_2_`, `product2_`.`product_height` AS `product_9_42_2_`, `product2_`.`product_free` AS `product10_42_2_`, `product2_`.`product_length` AS `product11_42_2_`, `product2_`.`quantity_ordered` AS `quantit12_42_2_`, `product2_`.`review_avg` AS `review_13_42_2_`, `product2_`.`review_count` AS `review_14_42_2_`, `product2_`.`product_ship` AS `product15_42_2_`, `product2_`.`product_virtual` AS `product16_42_2_`, `product2_`.`product_weight` AS `product17_42_2_`, `product2_`.`product_width` AS `product18_42_2_`, `product2_`.`ref_sku` AS `ref_sku19_42_2_`, `product2_`.`rental_duration` AS `rental_20_42_2_`, `product2_`.`rental_period` AS `rental_21_42_2_`, `product2_`.`rental_status` AS `rental_22_42_2_`, `product2_`.`sku` AS `sku23_42_2_`, `product2_`.`sort_order` AS `sort_or24_42_2_`, `product2_`.`tax_class_id` AS `tax_cla28_42_2_`, `product2_`.`product_type_id` AS `product29_42_2_`, `descriptio3_`.`date_created` AS `date_cre2_46_3_`, `descriptio3_`.`date_modified` AS `date_mod3_46_3_`, `descriptio3_`.`updt_id` AS `updt_id4_46_3_`, `descriptio3_`.`description` AS `descript5_46_3_`, `descriptio3_`.`language_id` AS `languag14_46_3_`, `descriptio3_`.`name` AS `name6_46_3_`, `descriptio3_`.`title` AS `title7_46_3_`, `descriptio3_`.`meta_description` AS `meta_des8_46_3_`, `descriptio3_`.`meta_keywords` AS `meta_key9_46_3_`, `descriptio3_`.`meta_title` AS `meta_ti10_46_3_`, `descriptio3_`.`product_id` AS `product15_46_3_`, `descriptio3_`.`download_lnk` AS `downloa11_46_3_`, `descriptio3_`.`product_highlight` AS `product12_46_3_`, `descriptio3_`.`sef_url` AS `sef_url13_46_3_`, `descriptio3_`.`product_id` AS `product15_46_0__`, `descriptio3_`.`description_id` AS `descript1_46_0__` FROM `product_relationship` AS `productrel0_` INNER JOIN `product` AS `product2_` ON `productrel0_`.`related_product_id` = `product2_`.`product_id` INNER JOIN `product` AS `product1_` ON `productrel0_`.`product_id` = `product1_`.`product_id` INNER JOIN `product_description` AS `descriptio3_` ON `product2_`.`product_id` = `descriptio3_`.`product_id` WHERE `productrel0_`.`code` = 'RELATED_ITEM' AND `productrel0_`.`merchant_id` = 1 AND `product1_`.`product_id` = 2 AND `descriptio3_`.`language_id` = 1",
        };
    doTest(appName, stmtId, expected);
  }

  @Test // 146
  void testSolidus551() {
    final String appName = "solidus";
    final int stmtId = 551;
    final String[] expected =
        new String[] {
          "SELECT `productrel0_`.`product_relationship_id` AS `product_1_56_0_`, `product1_`.`product_id` AS `product_1_42_1_`, `product2_`.`product_id` AS `product_1_42_2_`, `descriptio3_`.`description_id` AS `descript1_46_3_`, `productrel0_`.`active` AS `active2_56_0_`, `productrel0_`.`code` AS `code3_56_0_`, `productrel0_`.`product_id` AS `product_4_56_0_`, `productrel0_`.`related_product_id` AS `related_5_56_0_`, `productrel0_`.`merchant_id` AS `merchant6_56_0_`, `product1_`.`date_created` AS `date_cre2_42_1_`, `product1_`.`date_modified` AS `date_mod3_42_1_`, `product1_`.`updt_id` AS `updt_id4_42_1_`, `product1_`.`available` AS `availabl5_42_1_`, `product1_`.`cond` AS `cond6_42_1_`, `product1_`.`date_available` AS `date_ava7_42_1_`, `product1_`.`manufacturer_id` AS `manufac25_42_1_`, `product1_`.`merchant_id` AS `merchan26_42_1_`, `product1_`.`customer_id` AS `custome27_42_1_`, `product1_`.`preorder` AS `preorder8_42_1_`, `product1_`.`product_height` AS `product_9_42_1_`, `product1_`.`product_free` AS `product10_42_1_`, `product1_`.`product_length` AS `product11_42_1_`, `product1_`.`quantity_ordered` AS `quantit12_42_1_`, `product1_`.`review_avg` AS `review_13_42_1_`, `product1_`.`review_count` AS `review_14_42_1_`, `product1_`.`product_ship` AS `product15_42_1_`, `product1_`.`product_virtual` AS `product16_42_1_`, `product1_`.`product_weight` AS `product17_42_1_`, `product1_`.`product_width` AS `product18_42_1_`, `product1_`.`ref_sku` AS `ref_sku19_42_1_`, `product1_`.`rental_duration` AS `rental_20_42_1_`, `product1_`.`rental_period` AS `rental_21_42_1_`, `product1_`.`rental_status` AS `rental_22_42_1_`, `product1_`.`sku` AS `sku23_42_1_`, `product1_`.`sort_order` AS `sort_or24_42_1_`, `product1_`.`tax_class_id` AS `tax_cla28_42_1_`, `product1_`.`product_type_id` AS `product29_42_1_`, `product2_`.`date_created` AS `date_cre2_42_2_`, `product2_`.`date_modified` AS `date_mod3_42_2_`, `product2_`.`updt_id` AS `updt_id4_42_2_`, `product2_`.`available` AS `availabl5_42_2_`, `product2_`.`cond` AS `cond6_42_2_`, `product2_`.`date_available` AS `date_ava7_42_2_`, `product2_`.`manufacturer_id` AS `manufac25_42_2_`, `product2_`.`merchant_id` AS `merchan26_42_2_`, `product2_`.`customer_id` AS `custome27_42_2_`, `product2_`.`preorder` AS `preorder8_42_2_`, `product2_`.`product_height` AS `product_9_42_2_`, `product2_`.`product_free` AS `product10_42_2_`, `product2_`.`product_length` AS `product11_42_2_`, `product2_`.`quantity_ordered` AS `quantit12_42_2_`, `product2_`.`review_avg` AS `review_13_42_2_`, `product2_`.`review_count` AS `review_14_42_2_`, `product2_`.`product_ship` AS `product15_42_2_`, `product2_`.`product_virtual` AS `product16_42_2_`, `product2_`.`product_weight` AS `product17_42_2_`, `product2_`.`product_width` AS `product18_42_2_`, `product2_`.`ref_sku` AS `ref_sku19_42_2_`, `product2_`.`rental_duration` AS `rental_20_42_2_`, `product2_`.`rental_period` AS `rental_21_42_2_`, `product2_`.`rental_status` AS `rental_22_42_2_`, `product2_`.`sku` AS `sku23_42_2_`, `product2_`.`sort_order` AS `sort_or24_42_2_`, `product2_`.`tax_class_id` AS `tax_cla28_42_2_`, `product2_`.`product_type_id` AS `product29_42_2_`, `descriptio3_`.`date_created` AS `date_cre2_46_3_`, `descriptio3_`.`date_modified` AS `date_mod3_46_3_`, `descriptio3_`.`updt_id` AS `updt_id4_46_3_`, `descriptio3_`.`description` AS `descript5_46_3_`, `descriptio3_`.`language_id` AS `languag14_46_3_`, `descriptio3_`.`name` AS `name6_46_3_`, `descriptio3_`.`title` AS `title7_46_3_`, `descriptio3_`.`meta_description` AS `meta_des8_46_3_`, `descriptio3_`.`meta_keywords` AS `meta_key9_46_3_`, `descriptio3_`.`meta_title` AS `meta_ti10_46_3_`, `descriptio3_`.`product_id` AS `product15_46_3_`, `descriptio3_`.`download_lnk` AS `downloa11_46_3_`, `descriptio3_`.`product_highlight` AS `product12_46_3_`, `descriptio3_`.`sef_url` AS `sef_url13_46_3_`, `descriptio3_`.`product_id` AS `product15_46_0__`, `descriptio3_`.`description_id` AS `descript1_46_0__` FROM `product_relationship` AS `productrel0_` INNER JOIN `product` AS `product2_` ON `productrel0_`.`related_product_id` = `product2_`.`product_id` INNER JOIN `product` AS `product1_` ON `productrel0_`.`product_id` = `product1_`.`product_id` INNER JOIN `product_description` AS `descriptio3_` ON `product2_`.`product_id` = `descriptio3_`.`product_id` WHERE `productrel0_`.`code` = 'RELATED_ITEM' AND `productrel0_`.`merchant_id` = 1 AND `product1_`.`product_id` = 2 AND `descriptio3_`.`language_id` = 1",
        };
    doTest(appName, stmtId, expected);
  }
}
