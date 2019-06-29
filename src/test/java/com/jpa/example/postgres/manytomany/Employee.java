package com.jpa.example.postgres.manytomany;

import lombok.*;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Setter
@Getter
@ToString(exclude = "projects")
@NoArgsConstructor
@EqualsAndHashCode(exclude = "projects")
@Table(name = "TEST.EMPLOYEE")
@SuppressWarnings("all")
public class Employee {

    @Id
    @Column(name = "EMPLOYEE_ID")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long employeeId;

    @Column(name = "FIRST_NAME")
    private String firstName;

    @Column(name = "LAST_NAME")
    private String lastName;

    @ManyToMany(cascade = { CascadeType.ALL })
    @JoinTable(
            name = "TEST.EMPLOYEE_PROJECT",
            joinColumns = { @JoinColumn(name = "EMPLOYEE_ID") },
            inverseJoinColumns = { @JoinColumn(name = "PROJECT_ID") }
    )
    Set<Project> projects = new HashSet<>();


}
