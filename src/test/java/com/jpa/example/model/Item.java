package com.jpa.example.model;

import lombok.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.*;

@Entity
@Table(name="ITEM")
@SuppressWarnings("all")
@Setter
@Getter
@ToString(exclude = "cart")
@NoArgsConstructor
@EqualsAndHashCode(exclude = "cart")
public class Item {

    @Id
    @Column(name = "ID")
    private String id;

    @Column(name = "ITEM_NAME")
    private String name;

    @ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumn(name="CART_ID", nullable=false)
    private Cart cart;
}
