package com.jpa.example.model;

import lombok.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name="CART")
@SuppressWarnings("all")
@Setter
@Getter
@ToString
@NoArgsConstructor
@EqualsAndHashCode
public class Cart {

    @Id
    @Column(name = "CART_ID")
    private String id;

    @Column(name = "CART_NAME")
    private String name;

    @OneToMany(mappedBy="cart", fetch=FetchType.EAGER, cascade = CascadeType.ALL)
    @Fetch(FetchMode.JOIN)
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
    private List<Item> items;

}

