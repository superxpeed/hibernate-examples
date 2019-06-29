package com.jpa.example;

import com.jpa.example.model.Cart;
import com.jpa.example.model.Item;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.*;
import org.junit.runners.MethodSorters;
import javax.persistence.*;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("all")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class MultipleEM {

    private static EntityManager mysqlEntityManager;
    private static EntityManager postgresEntityManager;

    private static EntityManagerFactory mysqlEntityManagerFactory;
    private static EntityManagerFactory postgresEntityManagerFactory;

    // Hibernate DDL: create table CART (CART_ID varchar(255) not null, CART_NAME varchar(255), primary key (CART_ID)) engine=InnoDB
    private static final String mysqlTableCart =    " CREATE TABLE IF NOT EXISTS `CART` ( " +
                                                    " `CART_ID` VARCHAR(32), " +
                                                    " `CART_NAME` VARCHAR(32) NOT NULL DEFAULT '', " +
                                                    " PRIMARY KEY (`CART_ID`) ) ENGINE=InnoDB DEFAULT CHARSET=utf8";

    // Hibernate DDL: create table ITEM (ID varchar(255) not null, ITEM_NAME varchar(255), CART_ID varchar(255) not null, primary key (ID)) engine=InnoDB
    // Hibernate DDL: alter table ITEM add constraint FKiiemg9iq85055iqpi7q5vkgbu foreign key (CART_ID) references CART (CART_ID)
    private static final String mysqlTableItem =    " CREATE TABLE IF NOT EXISTS  `ITEM` ( " +
                                                    " `ID` VARCHAR(32), " +
                                                    " `ITEM_NAME` VARCHAR(32) NOT NULL DEFAULT '', " +
                                                    " `CART_ID` VARCHAR(32) NOT NULL, " +
                                                    " PRIMARY KEY (`ID`), " +
                                                    " KEY `CART_ID` (`CART_ID`), " +
                                                    " CONSTRAINT `item_ibfk_1` FOREIGN KEY (`CART_ID`) REFERENCES `CART` (`CART_ID`) " +
                                                    " ) ENGINE=InnoDB DEFAULT CHARSET=utf8 ";

    // Hibernate DDL: create table CART (CART_ID varchar(255) not null, CART_NAME varchar(255), primary key (CART_ID))
    private static final String postgresTableCart = " CREATE TABLE IF NOT EXISTS CART ( " +
                                                    " CART_ID VARCHAR(32) PRIMARY KEY, " +
                                                    " CART_NAME VARCHAR(32) NOT NULL DEFAULT '' )";

    // Hibernate DDL: create table ITEM (ID varchar(255) not null, ITEM_NAME varchar(255), CART_ID varchar(255) not null, primary key (ID))
    // Hibernate DDL: alter table if exists ITEM add constraint FKiiemg9iq85055iqpi7q5vkgbu foreign key (CART_ID) references CART
    private static final String postgresTableItem = " CREATE TABLE IF NOT EXISTS ITEM ( " +
                                                    " ID VARCHAR(32) PRIMARY KEY, " +
                                                    " ITEM_NAME VARCHAR(32) NOT NULL DEFAULT '', " +
                                                    " CART_ID VARCHAR(32) NOT NULL, " +
                                                    " CONSTRAINT item_ibfk_1 FOREIGN KEY (CART_ID) " +
                                                    " REFERENCES CART (CART_ID) MATCH FULL " +
                                                    " ON UPDATE NO ACTION ON DELETE NO ACTION )";

    private static final List<String> insertedMysql = new ArrayList<>();
    private static final List<String> insertedPostgres = new ArrayList<>();

    @BeforeClass
    public static void prepareEverything() {
        mysqlEntityManagerFactory = Persistence.createEntityManagerFactory("mysql-persistence");
        mysqlEntityManager = mysqlEntityManagerFactory.createEntityManager();
        postgresEntityManagerFactory = Persistence.createEntityManagerFactory("postgres-persistence");
        postgresEntityManager = postgresEntityManagerFactory.createEntityManager();

        assertTrue(mysqlEntityManager.isOpen());
        assertTrue(postgresEntityManager.isOpen());

        mysqlEntityManager.getTransaction().begin();
        mysqlEntityManager.createNativeQuery(mysqlTableCart).executeUpdate();
        mysqlEntityManager.createNativeQuery(mysqlTableItem).executeUpdate();
        mysqlEntityManager.createNativeQuery("DELETE FROM `Item`").executeUpdate();
        mysqlEntityManager.createNativeQuery("DELETE FROM `Cart`").executeUpdate();
        mysqlEntityManager.getTransaction().commit();

        postgresEntityManager.getTransaction().begin();
        postgresEntityManager.createNativeQuery(postgresTableCart).executeUpdate();
        postgresEntityManager.createNativeQuery(postgresTableItem).executeUpdate();
        postgresEntityManager.createNativeQuery("DELETE FROM Item").executeUpdate();
        postgresEntityManager.createNativeQuery("DELETE FROM Cart").executeUpdate();
        postgresEntityManager.getTransaction().commit();
    }

    @Test
    public void stage1_insertBoth() {
        Cart cart = new Cart();
        cart.setId(UUID.randomUUID().toString().replace("-",""));
        cart.setName("Test cart");
        List<Item> items=new ArrayList<>();
        for(int i = 0; i < 3; i++){
            Item item = new Item();
            item.setId(UUID.randomUUID().toString().replace("-",""));
            item.setName("Item " + i);
            item.setCart(cart);
            items.add(item);
        }
        cart.setItems(items);

        mysqlEntityManager.getTransaction().begin();
        mysqlEntityManager.persist(cart);
        mysqlEntityManager.getTransaction().commit();

        mysqlEntityManager.detach(cart);

        insertedMysql.add(cart.getId());
        System.out.println("Inserted into MySQL: " + cart);

        postgresEntityManager.getTransaction().begin();
        postgresEntityManager.persist(cart);
        postgresEntityManager.getTransaction().commit();

        insertedPostgres.add(cart.getId());
        System.out.println("Inserted into PostgreSQL: " + cart);
    }

    @Test
    public void stage2_readBoth() {
        assertTrue(insertedPostgres.get(0).equals(insertedMysql.get(0)));

        /*
            Hibernate: select cart0_.CART_ID as CART_ID1_0_0_,
                              cart0_.CART_NAME as CART_NAM2_0_0_,
                              items1_.CART_ID as CART_ID3_1_1_,
                              items1_.ID as ID1_1_1_,
                              items1_.ID as ID1_1_2_,
                              items1_.CART_ID as CART_ID3_1_2_,
                              items1_.ITEM_NAME as ITEM_NAM2_1_2_
                                    from CART cart0_
                                         left outer join
                                         ITEM items1_
                                    on cart0_.CART_ID=items1_.CART_ID where cart0_.CART_ID=?

         */
        postgresEntityManager.clear();
        Cart p1 = postgresEntityManager.find(Cart.class, insertedPostgres.get(0));
        System.out.println("Main entity id: " + p1.getId());
        System.out.println("Fetched items: " + p1.getItems());

        mysqlEntityManager.clear();
        Cart m1 = mysqlEntityManager.find(Cart.class, insertedMysql.get(0));
        System.out.println("Main entity id: " + m1.getId());
        System.out.println("Fetched items: " + m1.getItems());

        assertEquals(m1.getName(), p1.getName());
        assertTrue(CollectionUtils.isEqualCollection(m1.getItems(), p1.getItems()));
    }

    @Test
    public void stage3_updateBoth() {

        Cart p1 = postgresEntityManager.find(Cart.class, insertedPostgres.get(0));

        Item item = new Item();
        item.setId(UUID.randomUUID().toString().replace("-",""));
        item.setName("Item updated");
        item.setCart(p1);
        p1.getItems().add(item);
        p1.setName("Cart updated");

        postgresEntityManager.getTransaction().begin();
        postgresEntityManager.merge(p1);
        postgresEntityManager.flush();
        postgresEntityManager.getTransaction().commit();

        postgresEntityManager.detach(p1);

        mysqlEntityManager.getTransaction().begin();
        mysqlEntityManager.merge(p1);
        mysqlEntityManager.flush();
        mysqlEntityManager.getTransaction().commit();

        Cart p1updated = postgresEntityManager.find(Cart.class, insertedPostgres.get(0));
        Cart m1updated = mysqlEntityManager.find(Cart.class, insertedMysql.get(0));

        System.out.println("Updated from PostgreSQL: " + p1updated);
        System.out.println("Updated from MySQL: " + m1updated);

        assertEquals(p1updated.getName(), m1updated.getName());
        assertTrue(CollectionUtils.isEqualCollection(m1updated.getItems(), p1updated.getItems()));
    }

    @Test
    public void stage4_entityLock() throws Exception {

        Cart p1 = postgresEntityManager.find(Cart.class, insertedPostgres.get(0));
        postgresEntityManager.getTransaction().begin();
        postgresEntityManager.lock(p1, LockModeType.PESSIMISTIC_WRITE);
        System.out.println("Locked from main thread");
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                EntityManager postgresEntityManagerNew = postgresEntityManagerFactory.createEntityManager();
                Cart p1New = postgresEntityManagerNew.find(Cart.class, insertedPostgres.get(0));
                p1New.setName("Lock example");
                try{
                    postgresEntityManagerNew.getTransaction().begin();
                    postgresEntityManagerNew.lock(p1New, LockModeType.PESSIMISTIC_WRITE);
                    postgresEntityManagerNew.merge(p1New);
                    postgresEntityManagerNew.flush();
                    postgresEntityManagerNew.getTransaction().commit();
                    System.out.println("Commit from new thread");
                }catch (LockTimeoutException e){
                    assertTrue(e.getMessage().contains("could not obtain pessimistic lock"));
                }
            }
        });
        thread.start();
        Thread.sleep(1000);
        postgresEntityManager.getTransaction().rollback();
        System.out.println("Unlocked from main thread");
        thread.join();
    }

    @AfterClass
    public static void teardown(){
        System.out.println("Closing entity managers");
        mysqlEntityManager.close();
        postgresEntityManager.close();
    }
}
