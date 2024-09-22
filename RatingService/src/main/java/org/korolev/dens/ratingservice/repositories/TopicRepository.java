package org.korolev.dens.ratingservice.repositories;

import org.korolev.dens.ratingservice.entities.Topic;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TopicRepository extends JpaRepository<Topic, Integer> {

}