package com.jpa.example;

import com.jpa.example.postgres.partitioning.CartPartitioned;
import com.jpa.example.postgres.partitioning.ItemPartitioned;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Query;

import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertTrue;

@SuppressWarnings("all")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class PostgreSQLPartitioning {

    private static EntityManager postgresEntityManager;

    private static EntityManagerFactory postgresEntityManagerFactory;

    private static final String postgresTableCart = " CREATE TABLE IF NOT EXISTS TEST.MASTER_CART ( " +
            " OBJECT_ID VARCHAR(32) PRIMARY KEY, " +
            " CART_NAME VARCHAR(32) NOT NULL DEFAULT ''," +
            " CREATE_DATE DATE NOT NULL )";

    private static final String postgresTableItem = " CREATE TABLE IF NOT EXISTS TEST.MASTER_ITEM ( " +
            " OBJECT_ID VARCHAR(32) PRIMARY KEY, " +
            " ITEM_NAME VARCHAR(32) NOT NULL DEFAULT '', " +
            " CART_ID VARCHAR(32) NOT NULL," +
            " CREATE_DATE DATE NOT NULL, " +
            " CONSTRAINT item_ibfk_1 FOREIGN KEY (CART_ID) " +
            " REFERENCES TEST.MASTER_CART (OBJECT_ID) MATCH FULL " +
            " ON UPDATE NO ACTION ON DELETE NO ACTION )";

    private static final String createSchema = "CREATE SCHEMA test";

    private static final String createPartitioningFunction = "CREATE OR REPLACE FUNCTION TEST.create_partition_and_insert()\n" +
            "  RETURNS TRIGGER AS\n" +
            "$BODY$\n" +
            "DECLARE\n" +
            "  partition_date TEXT;\n" +
            "  partition      TEXT;\n" +
            "BEGIN\n" +
            "  partition_date \\:= to_char(NEW.CREATE_DATE, 'YYYY_MM_DD');\n" +
            "  partition \\:= 'MASTER_' || TG_RELNAME || '_' || partition_date;\n" +
            "  IF NOT EXISTS(SELECT relname\n" +
            "                FROM pg_class\n" +
            "                WHERE relname = partition)\n" +
            "  THEN\n" +
            "    RAISE NOTICE 'A partition has been created %', partition;\n" +
            "    EXECUTE\n" +
            "        'CREATE TABLE IF NOT EXISTS test.' || partition || ' (check (CREATE_DATE = ''' || NEW.CREATE_DATE || ''')) INHERITS (' || 'test.MASTER_'\n" +
            "        || TG_RELNAME || ');';\n" +
            "    EXECUTE 'CREATE UNIQUE INDEX IF NOT EXISTS unique_idx_' || partition || ' on test.' || partition || '(OBJECT_ID desc );';\n" +
            "  END IF;\n" +
            "  EXECUTE 'INSERT INTO test.' || partition || ' SELECT(' || 'test.' || TG_RELNAME || ' ' || quote_literal(NEW) ||\n" +
            "          ').* RETURNING OBJECT_ID;';\n" +
            "  RETURN NEW;\n" +
            "END;\n" +
            "$BODY$\n" +
            "  LANGUAGE plpgsql VOLATILE\n" +
            "  COST 100;";

    private static String createTriggerSql(String viewName){
        return "DROP TRIGGER IF EXISTS " + viewName +"_insert_trigger " +
                "  ON test." + viewName +";" +
                "CREATE TRIGGER " + viewName +"_insert_trigger " +
                "  INSTEAD OF INSERT ON test." + viewName +" " +
                "  FOR EACH ROW EXECUTE PROCEDURE test.create_partition_and_insert()";
    }

    private static String createViewSql(String viewName){
        return "CREATE VIEW TEST." + viewName + " AS SELECT * FROM TEST.MASTER_" + viewName;
    }

    private static void dropAllPartitionsForTable(String table){
        Query query = postgresEntityManager.createNativeQuery("SELECT relname FROM pg_class WHERE relname LIKE('master\\_" + table +"\\_____\\___\\___')");
        List list = query.getResultList();
        if (!list.isEmpty()){
            postgresEntityManager.getTransaction().begin();
            for(Object result : list){
                System.out.println("Dropping " + result + " partition");
                postgresEntityManager.createNativeQuery("DROP TABLE IF EXISTS TEST." + (String)result).executeUpdate();
            }
            postgresEntityManager.getTransaction().commit();
        }
    }

    private static boolean checkIfTableExists(String tableName){
        return ((BigInteger)postgresEntityManager.createNativeQuery("SELECT count(*) FROM pg_class WHERE relname = '" + tableName + "'").getSingleResult()).equals(BigInteger.ONE);
    }

    private static boolean checkIfFunctionExists(String functionName){
        return ((BigInteger)postgresEntityManager.createNativeQuery("SELECT count(*) FROM pg_proc WHERE proname = '" + functionName + "'").getSingleResult()).equals(BigInteger.valueOf(2));
    }

    @BeforeClass
    public static void prepareEverything() {
        postgresEntityManagerFactory = Persistence.createEntityManagerFactory("postgres-persistence");
        postgresEntityManager = postgresEntityManagerFactory.createEntityManager();
        assertTrue(postgresEntityManager.isOpen());

        postgresEntityManager.getTransaction().begin();
        postgresEntityManager.createNativeQuery("CREATE SCHEMA IF NOT EXISTS test").executeUpdate();
        postgresEntityManager.getTransaction().commit();

        dropAllPartitionsForTable("item");
        dropAllPartitionsForTable("cart");

        postgresEntityManager.getTransaction().begin();
        postgresEntityManager.createNativeQuery("DROP VIEW  IF EXISTS TEST.ITEM").executeUpdate();
        postgresEntityManager.createNativeQuery("DROP VIEW  IF EXISTS TEST.CART").executeUpdate();
        postgresEntityManager.createNativeQuery("DROP TABLE IF EXISTS TEST.MASTER_ITEM").executeUpdate();
        postgresEntityManager.createNativeQuery("DROP TABLE IF EXISTS TEST.MASTER_CART").executeUpdate();
        postgresEntityManager.createNativeQuery("drop function IF EXISTS test.create_partition_and_insert()").executeUpdate();
        postgresEntityManager.getTransaction().commit();

        // create master tables
        postgresEntityManager.getTransaction().begin();
        postgresEntityManager.createNativeQuery(postgresTableCart).executeUpdate();
        postgresEntityManager.createNativeQuery(postgresTableItem).executeUpdate();
        postgresEntityManager.getTransaction().commit();

        // create partitioning function
        postgresEntityManager.getTransaction().begin();
        postgresEntityManager.createNativeQuery(createPartitioningFunction).executeUpdate();
        postgresEntityManager.getTransaction().commit();

        // create views for master tables
        postgresEntityManager.getTransaction().begin();
        postgresEntityManager.createNativeQuery(createViewSql("ITEM")).executeUpdate();
        postgresEntityManager.createNativeQuery(createViewSql("CART")).executeUpdate();
        postgresEntityManager.getTransaction().commit();

        // create insertion triggers for views
        postgresEntityManager.getTransaction().begin();
        postgresEntityManager.createNativeQuery(createTriggerSql("ITEM")).executeUpdate();
        postgresEntityManager.createNativeQuery(createTriggerSql("CART")).executeUpdate();
        postgresEntityManager.getTransaction().commit();
    }

    @Test
    public void stage1_insertBoth() throws ParseException {

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date targetDate1 = dateFormat.parse("2019-06-28");
        Date targetDate2 = dateFormat.parse("2019-06-29");
        Date targetDate3 = dateFormat.parse("2019-06-30");

        CartPartitioned cart = new CartPartitioned();
        cart.setId(UUID.randomUUID().toString().replace("-",""));
        cart.setName("Test cart");
        cart.setCreateDate(targetDate1);
        List<ItemPartitioned> items=new ArrayList<>();
        for(int i = 0; i < 3; i++){
            ItemPartitioned item = new ItemPartitioned();
            item.setId(UUID.randomUUID().toString().replace("-",""));
            item.setName("Item " + i);
            item.setCart(cart);
            item.setCreateDate(targetDate1);
            items.add(item);
        }
        cart.setItems(items);

        postgresEntityManager.getTransaction().begin();
        postgresEntityManager.persist(cart);
        postgresEntityManager.getTransaction().commit();

        CartPartitioned cart2 = new CartPartitioned();
        cart2.setId(UUID.randomUUID().toString().replace("-",""));
        cart2.setName("Test cart");
        cart2.setCreateDate(targetDate2);
        List<ItemPartitioned> items2=new ArrayList<>();
        for(int i = 0; i < 3; i++){
            ItemPartitioned item = new ItemPartitioned();
            item.setId(UUID.randomUUID().toString().replace("-",""));
            item.setName("Item " + i);
            item.setCart(cart2);
            item.setCreateDate(targetDate2);
            items2.add(item);
        }
        cart2.setItems(items2);

        postgresEntityManager.getTransaction().begin();
        postgresEntityManager.persist(cart2);
        postgresEntityManager.getTransaction().commit();

        CartPartitioned cart3 = new CartPartitioned();
        cart3.setId(UUID.randomUUID().toString().replace("-",""));
        cart3.setName("Test cart");
        cart3.setCreateDate(targetDate3);
        List<ItemPartitioned> items3=new ArrayList<>();
        for(int i = 0; i < 3; i++){
            ItemPartitioned item = new ItemPartitioned();
            item.setId(UUID.randomUUID().toString().replace("-",""));
            item.setName("Item " + i);
            item.setCart(cart3);
            item.setCreateDate(targetDate3);
            items3.add(item);
        }
        cart3.setItems(items3);

        postgresEntityManager.getTransaction().begin();
        postgresEntityManager.persist(cart3);
        postgresEntityManager.getTransaction().commit();

        postgresEntityManager.clear();

        CartPartitioned p1 = postgresEntityManager.find(CartPartitioned.class, cart.getId());
        assertTrue(cart.getId().equals(p1.getId()));
        assertTrue(cart.getItems().size() == p1.getItems().size());

        assertTrue(checkIfTableExists("master_cart"));
        assertTrue(checkIfTableExists("master_item"));

        assertTrue(checkIfTableExists("cart"));
        assertTrue(checkIfTableExists("item"));

        SimpleDateFormat currentDate = new SimpleDateFormat("_yyyy_MM_dd");

        assertTrue(checkIfTableExists("master_cart" + currentDate.format(targetDate1)));
        assertTrue(checkIfTableExists("master_item" + currentDate.format(targetDate1)));

        assertTrue(checkIfTableExists("master_cart" + currentDate.format(targetDate2)));
        assertTrue(checkIfTableExists("master_item" + currentDate.format(targetDate2)));

        assertTrue(checkIfTableExists("master_cart" + currentDate.format(targetDate3)));
        assertTrue(checkIfTableExists("master_item" + currentDate.format(targetDate3)));
        assertTrue(checkIfFunctionExists("create_partition_and_insert"));
    }

    @AfterClass
    public static void teardown(){
        System.out.println("Closing entity manager");
        postgresEntityManager.close();
    }
}
