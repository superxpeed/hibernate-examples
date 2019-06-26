package com.jpa.example;

import com.impetus.client.cassandra.common.CassandraConstants;
import com.jpa.example.model.Cart;
import com.jpa.example.model.Item;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.FlushModeType;
import javax.persistence.Persistence;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("all")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class KunderaCassandra {

    @Test
    public void stage1_insertCassandra() {
        Cart p1 = new Cart();
        p1.setId(UUID.randomUUID().toString().replace("-",""));
        p1.setName("Test cart");
        List<Item> items=new ArrayList<>();
        for(int i = 0; i < 3; i++){
            Item item = new Item();
            item.setId(UUID.randomUUID().toString().replace("-",""));
            item.setName("Item " + i);
            item.setCart(p1);
            items.add(item);
        }
        p1.setItems(items);

        Map<String, String> props = new HashMap<>();
        props.put(CassandraConstants.CQL_VERSION, CassandraConstants.CQL_VERSION_3_0);
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("cassandra-persistence", props);
        EntityManager em = emf.createEntityManager();
        em.setFlushMode(FlushModeType.COMMIT);
        em.getTransaction().begin();
        em.persist(p1);
        p1.getItems().forEach(x -> em.persist(x));
        em.getTransaction().commit();
        em.close();
        emf.close();

        EntityManagerFactory emf1 = Persistence.createEntityManagerFactory("cassandra-persistence", props);
        EntityManager em1 = emf1.createEntityManager();
        em1.setFlushMode(FlushModeType.COMMIT);
        Cart p2 = em1.find(Cart.class, p1.getId());
        assertEquals(p1.getName(), p2.getName());
        assertTrue(CollectionUtils.isEqualCollection(p1.getItems(), p2.getItems()));

        Item item = new Item();
        item.setId(UUID.randomUUID().toString().replace("-",""));
        item.setName("Item updated cassandra");
        item.setCart(p2);
        p2.getItems().add(item);

        em1.getTransaction().begin();
        em1.merge(p2);
        em1.flush();
        em1.getTransaction().commit();

        System.out.println(p2);
    }

}
