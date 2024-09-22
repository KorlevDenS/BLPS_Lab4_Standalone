package org.korolev.dens.blps_lab4_standalone.repositories;

import org.korolev.dens.blps_lab4_standalone.entites.Role;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Integer> {

    Role findByName(String name);

}