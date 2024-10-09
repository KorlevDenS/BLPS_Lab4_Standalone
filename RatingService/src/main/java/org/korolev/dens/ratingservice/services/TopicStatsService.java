package org.korolev.dens.ratingservice.services;

import org.korolev.dens.ratingservice.entities.Topic;
import org.korolev.dens.ratingservice.exceptions.RateObjectNotFoundException;
import org.korolev.dens.ratingservice.exceptions.RatingLogicException;
import org.korolev.dens.ratingservice.exceptions.ServiceErrorException;
import org.korolev.dens.ratingservice.repositories.TopicRepository;
import org.korolev.dens.ratingservice.responces.TopicPlaces;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class TopicStatsService {

    private final TopicRepository topicRepository;

    public TopicStatsService(TopicRepository topicRepository) {
        this.topicRepository = topicRepository;
    }

    public Topic findTopicStats(Integer topicId) throws ServiceErrorException, RateObjectNotFoundException {
        Optional<Topic> topic;
        try {
            topic = topicRepository.findById(topicId);
        } catch (Exception e) {
            throw new ServiceErrorException("Database error");
        }
        if (topic.isEmpty()) {
            throw new RateObjectNotFoundException("No stats for topic " + topicId);
        }
        return topic.get();
    }

    public List<Topic> findTopicsViewsTop(Integer n) throws ServiceErrorException, RatingLogicException {
        List<Topic> allTopics;
        try {
            allTopics = topicRepository.findAll();
        } catch (Exception e) {
            throw new ServiceErrorException("Database error");
        }
        allTopics.sort(Comparator.comparingInt(Topic::getViews).reversed());
        if (n == 0) {
            return allTopics;
        } else if (n > 0) {
            return allTopics.stream().limit(n).toList();
        } else {
            throw new RatingLogicException("N parameter is negative", HttpStatus.BAD_REQUEST);
        }
    }

    public List<Topic> findTopicsFameTop(Integer n) throws ServiceErrorException, RatingLogicException {
        List<Topic> allTopics;
        try {
            allTopics = topicRepository.findAll();
        } catch (Exception e) {
            throw new ServiceErrorException("Database error");
        }
        allTopics.sort(Comparator.comparingDouble(Topic::getFame).reversed());
        if (n == 0) {
            return allTopics;
        } else if (n > 0) {
            return allTopics.stream().limit(n).toList();
        } else {
            throw new RatingLogicException("N parameter is negative", HttpStatus.BAD_REQUEST);
        }
    }

    public TopicPlaces findTopicPlaces(Integer topicId) throws ServiceErrorException, RateObjectNotFoundException {
        List<Topic> allTopics;
        try {
            allTopics = topicRepository.findAll();
        } catch (Exception e) {
            throw new ServiceErrorException("Database error");
        }
        if (!allTopics.stream().map(Topic::getId).toList().contains(topicId)) {
            throw new RateObjectNotFoundException("No stats for topic " + topicId);
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
        return topicPlaces;
    }

}
