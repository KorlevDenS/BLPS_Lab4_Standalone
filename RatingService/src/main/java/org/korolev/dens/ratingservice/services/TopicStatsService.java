package org.korolev.dens.ratingservice.services;

import org.korolev.dens.ratingservice.entities.Topic;
import org.korolev.dens.ratingservice.repositories.TopicRepository;
import org.korolev.dens.ratingservice.responces.TopicPlaces;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class TopicStatsService {

    private final TopicRepository topicRepository;

    public TopicStatsService(TopicRepository topicRepository) {
        this.topicRepository = topicRepository;
    }

    public ResponseEntity<?> findTopicStats(Integer topicId) {
        Optional<Topic> topic;
        try {
            topic = topicRepository.findById(topicId);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Database error");
        }
        if (topic.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No stats for topic " + topicId);
        }
        return ResponseEntity.ok().body(topic.get());
    }

    public ResponseEntity<?> findTopicsViewsTop(Integer n) {
        List<Topic> allTopics;
        try {
            allTopics = topicRepository.findAll();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Database error");
        }
        allTopics.sort(Comparator.comparingInt(Topic::getViews).reversed());
        if (n == 0) {
            return ResponseEntity.ok().body(allTopics);
        } else if (n > 0) {
            return ResponseEntity.ok().body(allTopics.stream().limit(n).toList());
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("N parameter is negative");
        }
    }

    public ResponseEntity<?> findTopicsFameTop(Integer n) {
        List<Topic> allTopics;
        try {
            allTopics = topicRepository.findAll();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Database error");
        }
        allTopics.sort(Comparator.comparingDouble(Topic::getFame).reversed());
        if (n == 0) {
            return ResponseEntity.ok().body(allTopics);
        } else if (n > 0) {
            return ResponseEntity.ok().body(allTopics.stream().limit(n).toList());
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("N parameter is negative");
        }
    }

    public ResponseEntity<?> findTopicPlaces(Integer topicId) {
        List<Topic> allTopics;
        try {
            allTopics = topicRepository.findAll();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Database error");
        }
        if (!allTopics.stream().map(Topic::getId).toList().contains(topicId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No stats for topic " + topicId);
        }
        TopicPlaces topicPlaces = new TopicPlaces();
        List<Topic> sortedByViews = new ArrayList<>(allTopics);
        sortedByViews.sort(Comparator.comparingInt(Topic::getViews).reversed());
        for (int i = 0; i < sortedByViews.size(); i++) {
            if (Objects.equals(sortedByViews.get(i).getId(), topicId)) {
                topicPlaces.setPlaceByViews(i + 1);
            }
        }
        List<Topic> sortedByFame = new ArrayList<>(allTopics);
        sortedByFame.sort(Comparator.comparingDouble(Topic::getFame).reversed());
        for (int i = 0; i < sortedByFame.size(); i++) {
            if (Objects.equals(sortedByFame.get(i).getId(), topicId)) {
                topicPlaces.setPlaceByFame(i + 1);
            }
        }
        return ResponseEntity.ok().body(topicPlaces);
    }

}
