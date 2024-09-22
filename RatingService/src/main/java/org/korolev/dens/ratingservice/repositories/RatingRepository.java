package org.korolev.dens.ratingservice.repositories;

import org.korolev.dens.ratingservice.entities.Client;
import org.korolev.dens.ratingservice.entities.Rating;
import org.korolev.dens.ratingservice.entities.Topic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RatingRepository extends JpaRepository<Rating, Integer> {

    List<Rating> findAllByTopic(Topic topic);

    Optional<Rating> findByCreatorAndTopic(Client creator, Topic topic);

    void removeAllByTopic(Topic topic);
}