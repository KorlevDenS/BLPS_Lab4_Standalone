package org.korolev.dens.ratingservice.controllers;

import org.korolev.dens.ratingservice.exceptions.RateObjectNotFoundException;
import org.korolev.dens.ratingservice.exceptions.RatingLogicException;
import org.korolev.dens.ratingservice.exceptions.ServiceErrorException;
import org.korolev.dens.ratingservice.services.TopicStatsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/stats/topic")
public class TopicStatsController {

    private final TopicStatsService topicStatsService;

    public TopicStatsController(TopicStatsService topicStatsService) {
        this.topicStatsService = topicStatsService;
    }

    @GetMapping("/get/{topicId}")
    public ResponseEntity<?> getTopicStats(@PathVariable Integer topicId) throws RateObjectNotFoundException,
            ServiceErrorException {
        return ResponseEntity.ok(topicStatsService.findTopicStats(topicId));
    }

    @GetMapping("/get/places/{topicId}")
    public ResponseEntity<?> getTopicPlaces(@PathVariable Integer topicId) throws RateObjectNotFoundException,
            ServiceErrorException {
        return ResponseEntity.ok(topicStatsService.findTopicPlaces(topicId));
    }

    @GetMapping("/get/views/top/{n}")
    public ResponseEntity<?> getTopicsViewsTop(@PathVariable Integer n) throws RatingLogicException,
            ServiceErrorException {
        return ResponseEntity.ok(topicStatsService.findTopicsViewsTop(n));
    }

    @GetMapping("/get/fame/top/{n}")
    public ResponseEntity<?> getTopicsFameTop(@PathVariable Integer n) throws RatingLogicException,
            ServiceErrorException {
        return ResponseEntity.ok(topicStatsService.findTopicsFameTop(n));
    }

}
