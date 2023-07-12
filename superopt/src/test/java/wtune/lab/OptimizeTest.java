package wtune.lab;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import wtune.common.datasource.DbSupport;
import wtune.sql.SqlSupport;
import wtune.sql.ast.SqlNode;
import wtune.sql.plan.PlanContext;
import wtune.sql.plan.PlanSupport;
import wtune.sql.schema.Schema;
import wtune.sql.schema.SchemaSupport;
import wtune.superopt.fragment.Fragment;
import wtune.superopt.optimizer.Optimizer;
import wtune.superopt.substitution.Substitution;
import wtune.superopt.substitution.SubstitutionBank;
import wtune.superopt.substitution.SubstitutionSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static wtune.sql.plan.PlanSupport.translateAsAst;
import static wtune.superopt.constraint.ConstraintSupport.enumConstraints;

@Execution(ExecutionMode.CONCURRENT)
public class OptimizeTest {

    /**
     * enum all possible rules for fragment0 and fragment1
     *
     * @param fragment0 structure of source fragment
     * @param fragment1 structure of target fragment
     * @return list of rules
     */
    static List<Substitution> enumRule(String fragment0, String fragment1) {
        final Fragment f0 = Fragment.parse(fragment0, null);
        final Fragment f1 = Fragment.parse(fragment1, null);
        return enumConstraints(f0, f1, 240000);
    }

    static boolean rewrite(String strSchema, String rawSql, String target, List<Substitution> rules) throws IOException {
        final SubstitutionBank substitutionBank = SubstitutionSupport.loadBank(rules.stream().map(Object::toString).collect(Collectors.toList()));
        final Schema schema = SchemaSupport.parseSchema(DbSupport.MySQL, strSchema);
        final SqlNode rawAST = SqlSupport.parseSql(DbSupport.MySQL, rawSql);
        SqlNode targetAST = SqlSupport.parseSql(DbSupport.MySQL, target);
        final PlanContext rawPlan = PlanSupport.assemblePlan(rawAST, schema);
        final PlanContext targetPlan = PlanSupport.assemblePlan(targetAST, schema);
        if (targetPlan != null) {
            targetAST = translateAsAst(targetPlan, targetPlan.root(), false);
        }

        final Optimizer optimizer = Optimizer.mk(substitutionBank);
        optimizer.setTimeout(Integer.MAX_VALUE);
        final Set<PlanContext> optimized = optimizer.optimize(rawPlan);
        final List<String> optSqls = new ArrayList<>();
        boolean success = false;
        for (PlanContext optPlan : optimized) {
            final SqlNode optAst = translateAsAst(optPlan, optPlan.root(), false);
            assert targetPlan != null;
            assert optAst != null;
            assert targetAST != null;
            optSqls.add(optAst.toString(false));
            if (optAst.toString().equals(targetAST.toString())){
                success = true;
            }
        }
        return success;
    }

    @Test
    void Test0() throws IOException {
        String schema = """
                CREATE TABLE student
                (
                    id         INT AUTO_INCREMENT PRIMARY KEY,
                    first_name VARCHAR(50),
                    last_name  VARCHAR(50),
                    age        INT,
                    gender     ENUM ('Male', 'Female'),
                    email      VARCHAR(100),
                    city       VARCHAR(50)
                );""";
        String source = "SELECT DISTINCT id FROM student";
        String target = "SELECT id FROM student";
        String fragment0 = "Proj*(Input)";
        String fragment1 = "Proj(Input)";
        List<Substitution> rules = enumRule(fragment0, fragment1);
        Assertions.assertTrue(rewrite(schema, source, target, rules));
    }

    @Test
    void Test1() throws IOException {
        String schema = """
                CREATE TABLE uploads
                (
                    id                     integer  NOT NULL PRIMARY KEY,
                    user_id                integer  NOT NULL,
                    original_filename      text     NOT NULL,
                    filesize               integer  NOT NULL,
                    url                    text     NOT NULL
                );

                CREATE TABLE post_uploads
                (
                    id        integer NOT NULL PRIMARY KEY,
                    upload_id integer NOT NULL,
                    FOREIGN KEY (upload_id) REFERENCES uploads (id)
                );""";
        String source = """
                SELECT post_uploads.upload_id
                FROM uploads
                         INNER JOIN post_uploads
                                    ON post_uploads.upload_id = uploads.id""";
        String target = "SELECT upload_id FROM post_uploads";
        String fragment0 = "Proj(InnerJoin(Input,Input)";
        String fragment1 = "Proj(Input)";
        List<Substitution> rules = enumRule(fragment0, fragment1);
        Assertions.assertTrue(rewrite(schema, source, target, rules));
    }

    @Test
    void Test2() throws IOException {
        String schema = """
                CREATE TABLE custom_fields
                (
                    id      int(11)     NOT NULL AUTO_INCREMENT PRIMARY KEY,
                    type    varchar(30) NOT NULL DEFAULT '',
                    name    varchar(30) NOT NULL DEFAULT '',
                    visible tinyint(1)  NOT NULL DEFAULT '1'
                );

                CREATE TABLE roles
                (
                    id          int(11)      NOT NULL AUTO_INCREMENT PRIMARY KEY,
                    name        varchar(255) NOT NULL DEFAULT '',
                    position    int(11)               DEFAULT NULL,
                    assignable  tinyint(1)            DEFAULT '1',
                    builtin     int(11)      NOT NULL DEFAULT '0',
                    permissions text
                );

                CREATE TABLE custom_fields_roles
                (
                    custom_field_id int(11) NOT NULL,
                    role_id         int(11) NOT NULL,
                    FOREIGN KEY (custom_field_id) REFERENCES custom_fields (id),
                    FOREIGN KEY (role_id) REFERENCES roles (id)
                );""";
        String source = """
                SELECT custom_fields.id, custom_fields_roles.role_id
                FROM custom_fields
                         INNER JOIN custom_fields_roles
                                    ON custom_fields.id = custom_fields_roles.custom_field_id
                         INNER JOIN roles ON custom_fields_roles.role_id = roles.id
                WHERE custom_fields.visible = FALSE""";
        String target = """
                SELECT custom_fields.id, custom_fields_roles.role_id
                FROM custom_fields
                         INNER JOIN custom_fields_roles
                                    ON custom_fields.id = custom_fields_roles.custom_field_id
                WHERE custom_fields.visible = FALSE""";
        String fragment0 = "Proj(Filter(InnerJoin(Input,Input))";
        String fragment1 = "Proj(Filter(Input))";
        List<Substitution> rules = enumRule(fragment0, fragment1);
        Assertions.assertTrue(rewrite(schema, source, target, rules));
    }

    @Test
    void Test3() throws IOException {
        String schema = """
                CREATE TABLE student
                (
                    id         INT AUTO_INCREMENT PRIMARY KEY,
                    first_name VARCHAR(50),
                    last_name  VARCHAR(50),
                    age        INT,
                    gender     ENUM ('Male', 'Female'),
                    email      VARCHAR(100),
                    city       VARCHAR(50)
                );

                CREATE TABLE grade
                (
                    id         INT AUTO_INCREMENT PRIMARY KEY,
                    subject    varchar(255) NOT NULL,
                    student_id INT          NOT NULL,
                    score      INT          NOT NULL,
                    FOREIGN KEY (student_id) REFERENCES student (id)
                );""";
        String source = """
                SELECT *
                FROM grade
                WHERE grade.student_id IN (SELECT id FROM student)
                  AND student_id = 10""";

        String middle1 = """
                SELECT grade.*
                FROM student AS student
                         INNER JOIN grade AS grade
                                    ON student.id = grade.student_id
                WHERE grade.student_id = 10""";

        String fragment0_0 = "Proj(Filter(InSubFilter(Input,Proj(Input))))";
        String fragment0_1 = "Proj(Filter(InnerJoin(Input,Input)))";
        List<Substitution> rules = enumRule(fragment0_0, fragment0_1);
        Assertions.assertTrue(rewrite(schema, source, middle1, rules));

        String target = """
                SELECT grade.*
                FROM grade AS grade
                WHERE grade.student_id = 10""";

        String fragment1_0 = "Proj(Filter(InnerJoin(Input,Input)))";
        String fragment1_1 = "Proj(Filter(Input))";
        rules = enumRule(fragment1_0, fragment1_1);
        Assertions.assertTrue(rewrite(schema, middle1, target, rules));
    }

    @Test
    void Test4() throws IOException {
        String schema = """
                CREATE TABLE spree_orders
                (
                    id           int(11)        NOT NULL AUTO_INCREMENT PRIMARY KEY,
                    number       varchar(32)             DEFAULT NULL,
                    item_total   decimal(10, 2) NOT NULL DEFAULT '0.00',
                    total        decimal(10, 2) NOT NULL DEFAULT '0.00',
                    completed_at datetime                DEFAULT NULL,
                    currency     varchar(255)            DEFAULT NULL
                );


                CREATE TABLE spree_promotions
                (
                    id                    int(11) NOT NULL AUTO_INCREMENT PRIMARY KEY,
                    description           varchar(255) DEFAULT NULL,
                    expires_at            datetime     DEFAULT NULL,
                    starts_at             datetime     DEFAULT NULL,
                    name                  varchar(255) DEFAULT NULL,
                    type                  varchar(255) DEFAULT NULL,
                    promotion_category_id int(11)      DEFAULT NULL
                );

                CREATE TABLE spree_order_promotions
                (
                    id           int(11) NOT NULL AUTO_INCREMENT PRIMARY KEY,
                    order_id     int(11) DEFAULT NULL,
                    promotion_id int(11) DEFAULT NULL,
                    FOREIGN KEY (order_id) REFERENCES spree_orders (id),
                    FOREIGN KEY (promotion_id) REFERENCES spree_promotions (id)
                );""";
        String source = """
                SELECT DISTINCT spree_orders.id
                FROM spree_orders
                         INNER JOIN spree_order_promotions
                                    ON spree_order_promotions.order_id = spree_orders.id
                         INNER JOIN spree_promotions ON spree_promotions.id = spree_order_promotions.promotion_id
                WHERE spree_promotions.id = 100 AND spree_orders.total > 300""";
        String middle1 = """
                SELECT DISTINCT spree_orders.id
                FROM spree_orders
                         INNER JOIN spree_order_promotions
                                    ON spree_order_promotions.order_id = spree_orders.id
                         INNER JOIN spree_promotions
                                    ON spree_order_promotions.promotion_id = spree_promotions.id
                WHERE spree_order_promotions.promotion_id = 100
                  AND spree_orders.total > 300""";

        String fragment0_0 = "Filter(InnerJoin(Input,Input))";
        String fragment0_1 = "Filter(InnerJoin(Input,Input))";
        List<Substitution> rules = enumRule(fragment0_0, fragment0_1);
        Assertions.assertTrue(rewrite(schema, source, middle1, rules));

        String target = """
                SELECT DISTINCT spree_orders.id
                FROM spree_orders
                         INNER JOIN spree_order_promotions
                                    ON spree_order_promotions.order_id = spree_orders.id
                WHERE spree_order_promotions.promotion_id = 100
                  AND spree_orders.total > 300""";
        String fragment1_0 = "Proj*(Filter(InnerJoin(Input,Input)))";
        String fragment1_1 = "Proj*(Filter(Input))";
        rules = enumRule(fragment1_0, fragment1_1);
        Assertions.assertTrue(rewrite(schema, middle1, target, rules));
    }

    @Test
    void Test5() throws IOException {
        String schema = """
                CREATE TABLE topics
                (
                    id          integer           NOT NULL PRIMARY KEY,
                    title       text              NOT NULL,
                    posts_count integer DEFAULT 0 NOT NULL,
                    image_url   text,
                    like_count  integer DEFAULT 0 NOT NULL
                );

                CREATE TABLE posts
                (
                    id          integer               NOT NULL PRIMARY KEY,
                    topic_id    integer               NOT NULL,
                    post_number integer               NOT NULL,
                    avg_time    integer,
                    score       double precision,
                    hidden      boolean DEFAULT false NOT NULL,
                    FOREIGN KEY (topic_id) REFERENCES topics (id)
                );

                CREATE TABLE post_actions
                (
                    id                integer               NOT NULL PRIMARY KEY,
                    post_id           integer               NOT NULL,
                    staff_took_action boolean DEFAULT false NOT NULL,
                    FOREIGN KEY (post_id) REFERENCES posts (id)
                );


                CREATE TABLE bookmarks
                (
                    id       integer NOT NULL PRIMARY KEY,
                    topic_id integer NOT NULL,
                    post_id  integer NOT NULL,
                    name     text,
                    FOREIGN KEY (topic_id) REFERENCES topics (id)
                );""";
        String source = """
                SELECT post_actions.id, post_actions.post_id, posts.topic_id
                FROM post_actions
                         INNER JOIN posts
                                    ON posts.id = post_actions.post_id
                         INNER JOIN topics ON topics.id = posts.topic_id
                WHERE post_actions.staff_took_action = TRUE
                  AND post_actions.staff_took_action = TRUE""";
        String middle1 = """
                SELECT post_actions.id, post_actions.post_id, posts.topic_id
                FROM post_actions
                         INNER JOIN posts
                                    ON posts.id = post_actions.post_id
                         INNER JOIN topics
                                    ON topics.id = posts.topic_id
                WHERE post_actions.staff_took_action = TRUE""";

        String fragment0_0 = "Filter(Filter(Input))";
        String fragment0_1 = "Filter(Input)";
        List<Substitution> rules = enumRule(fragment0_0, fragment0_1);
        Assertions.assertTrue(rewrite(schema, source, middle1, rules));

        String target = """
                SELECT post_actions.id, post_actions.post_id, posts.topic_id
                FROM post_actions
                         INNER JOIN posts\s
                                    ON posts.id = post_actions.post_id
                WHERE post_actions.staff_took_action = TRUE""";
        String fragment1_0 = "Proj(Filter(InnerJoin(Input,Input)))";
        String fragment1_1 = "Proj(Filter(Input))";
        rules = enumRule(fragment1_0, fragment1_1);
        Assertions.assertTrue(rewrite(schema, middle1, target, rules));
    }

    @Test
    void Test6() throws IOException {
        String schema = """
                CREATE TABLE people
                (
                    id     int          NOT NULL AUTO_INCREMENT PRIMARY KEY,
                    guid   varchar(255) NOT NULL,
                    pod_id int DEFAULT NULL
                );

                CREATE TABLE contacts
                (
                    id        int NOT NULL AUTO_INCREMENT PRIMARY KEY,
                    person_id int NOT NULL,
                    user_id   int NOT NULL,
                    FOREIGN KEY (person_id) REFERENCES people (id)
                );   \s""";
        String source = """
                SELECT DISTINCT contacts.id
                FROM contacts
                         LEFT OUTER JOIN people
                                         ON people.id = contacts.person_id
                WHERE contacts.user_id = 1945""";
        String middle1 = """
                SELECT DISTINCT contacts.id
                FROM contacts
                         INNER JOIN people
                                    ON contacts.person_id = people.id
                WHERE contacts.user_id = 1945""";

        String fragment0_0 = "LeftJoin(Input,Input)";
        String fragment0_1 = "InnerJoin(Input,Input)";
        List<Substitution> rules = enumRule(fragment0_0, fragment0_1);
        Assertions.assertTrue(rewrite(schema, source, middle1, rules));

        String middle2 = """
                SELECT DISTINCT contacts.id
                FROM contacts
                WHERE contacts.user_id = 1945""";

        String fragment1_0 = "Proj*(Filter(InnerJoin(Input,Input)))";
        String fragment1_1 = "Proj*(Filter(Input))";
        rules = enumRule(fragment1_0, fragment1_1);
        Assertions.assertTrue(rewrite(schema, middle1, middle2, rules));

        String target = """
                SELECT contacts.id
                FROM contacts
                WHERE contacts.user_id = 1945""";

        String fragment2_0 = "Proj*(Input)";
        String fragment2_1 = "Proj(Input)";
        rules = enumRule(fragment2_0, fragment2_1);
        Assertions.assertTrue(rewrite(schema, middle2, target, rules));
    }

    @Test
    void Test7() throws IOException {
        String schema = """
                CREATE TABLE notes
                (
                    id        integer NOT NULL PRIMARY KEY,
                    note      text,
                    author_id integer,
                    commit_id text,
                    type      text
                );""";
        String source = """
                SELECT n.*
                FROM notes AS n
                WHERE n.id IN
                      (SELECT m.id FROM notes AS m WHERE m.commit_id = '10232')""";
        String middle1 = """
                SELECT n.*
                FROM notes AS m
                         INNER JOIN notes AS n
                                    ON m.id = n.id
                WHERE m.commit_id = '10232'""";

        String fragment0_0 = "Proj(InSubFilter(Input,Proj(Filter(Input))))";
        String fragment0_1 = "Proj(Filter(InnerJoin(Input,Input)))";
        List<Substitution> rules = enumRule(fragment0_0, fragment0_1);
        Assertions.assertTrue(rewrite(schema, source, middle1, rules));

        String middle2 = """
                SELECT n.*
                FROM notes AS n
                         INNER JOIN notes AS m
                                    ON n.id = m.id
                WHERE n.commit_id = '10232'""";

        String fragment1_0 = "Filter(InnerJoin(Input,Input))";
        String fragment1_1 = "Filter(InnerJoin(Input,Input))";
        rules = enumRule(fragment1_0, fragment1_1);
        Assertions.assertTrue(rewrite(schema, middle1, middle2, rules));

        String target = """
                SELECT n.*
                FROM notes AS n
                WHERE n.commit_id = '10232'""";

        String fragment2_0 = "Proj(Filter(InnerJoin(Input,Input)))";
        String fragment2_1 = "Proj(Filter(Input))";
        rules = enumRule(fragment2_0, fragment2_1);
        Assertions.assertTrue(rewrite(schema, middle2, target, rules));
    }
}