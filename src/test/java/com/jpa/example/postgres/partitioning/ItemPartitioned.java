package com.jpa.example.postgres.partitioning;

import lombok.*;
import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name="TEST.ITEM")
@SuppressWarnings("all")
@Setter
@Getter
@ToString(exclude = "cart")
@NoArgsConstructor
@EqualsAndHashCode(exclude = "cart")
public class ItemPartitioned {

    @Id
    @Column(name = "OBJECT_ID")
    private String id;

    @Column(name = "ITEM_NAME")
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name="CART_ID", nullable=false)
    private CartPartitioned cart;

    @Column(name = "CREATE_DATE")
    private Date createDate;
}

