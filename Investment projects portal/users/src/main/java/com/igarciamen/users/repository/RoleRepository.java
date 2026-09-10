package com.igarciamen.users.repository;

import com.igarciamen.users.enums.ERole;
import com.igarciamen.users.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Integer> {
    Optional<Role> findByName(ERole name);
}
