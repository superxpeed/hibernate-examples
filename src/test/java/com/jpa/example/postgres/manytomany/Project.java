package com.jpa.example.postgres.manytomany;

import lombok.*;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Setter
@Getter
@ToString(exclude = "employees")
@NoArgsConstructor
@EqualsAndHashCode(exclude = "employees")
@Table(name = "TEST.PROJECT")
@SuppressWarnings("all")
public class Project {

    @Id
    @Column(name = "PROJECT_ID")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long projectId;

    @Column(name = "TITLE")
    private String title;

    @ManyToMany(mappedBy = "projects")
    private Set<Employee> employees = new HashSet<>();

}
