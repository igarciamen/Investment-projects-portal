package com.igarciamen.users.model;

import com.igarciamen.users.enums.ERole;
import jakarta.persistence.*;


@Entity
@Table(schema = "public", name = "roles")
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ERole name;

    public Role() {}
    public Role(ERole name) {this.name = name;}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public ERole getName() { return name; }
}
