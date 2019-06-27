package com.jpa.example.postgres.partitioning;

import lombok.*;
import javax.persistence.*;
import java.util.Date;
import java.util.List;

@Entity
@Table(name="TEST.CART")
@SuppressWarnings("all")
@Setter
@Getter
@ToString
@NoArgsConstructor
@EqualsAndHashCode
public class CartPartitioned {

    @Id
    @Column(name = "OBJECT_ID")
    private String id;

    @Column(name = "CART_NAME")
    private String name;

    @OneToMany(mappedBy="cart", fetch=FetchType.LAZY, cascade = CascadeType.ALL)
    private List<ItemPartitioned> items;

    @Column(name = "CREATE_DATE")
    private Date createDate;

}