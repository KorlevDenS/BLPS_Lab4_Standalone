package org.korolev.dens.ratingservice.repositories;

import org.korolev.dens.ratingservice.entities.Client;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientRepository extends JpaRepository<Client, String> {

}