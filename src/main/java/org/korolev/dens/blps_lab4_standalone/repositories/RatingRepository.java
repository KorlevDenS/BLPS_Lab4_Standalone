package org.korolev.dens.blps_lab4_standalone.repositories;

import org.korolev.dens.blps_lab4_standalone.entites.Client;
import org.korolev.dens.blps_lab4_standalone.entites.Rating;
import org.korolev.dens.blps_lab4_standalone.entites.Topic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RatingRepository extends JpaRepository<Rating, Integer> {

    Optional<Rating> findRatingByCreatorAndTopic(Client creator, Topic topic);

    List<Rating> findAllByTopic(Topic topic);

    @Modifying
    @Query(value = """
                update Rating rating set rating.rating = :rating where rating.topic.id = :topicId
                and rating.creator.login = :login""")
    void updateRatingByClientAndTopic(@Param("login") String login, @Param("rating") Integer rating,
                              @Param("topicId") Integer topicId);

}