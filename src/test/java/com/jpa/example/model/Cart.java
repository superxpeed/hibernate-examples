package com.jpa.example.model;

import lombok.*;
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

    @OneToMany(mappedBy="cart", fetch=FetchType.LAZY, cascade = CascadeType.ALL)
    private List<Item> items;

}

