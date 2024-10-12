package org.korolev.dens.blps_lab4_standalone.repositories;

import org.korolev.dens.blps_lab4_standalone.entites.Approval;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovalRepository extends JpaRepository<Approval, Integer> {
}