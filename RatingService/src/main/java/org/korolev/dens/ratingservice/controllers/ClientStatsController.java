package org.korolev.dens.ratingservice.controllers;

import org.korolev.dens.ratingservice.exceptions.RateObjectNotFoundException;
import org.korolev.dens.ratingservice.exceptions.RatingLogicException;
import org.korolev.dens.ratingservice.exceptions.ServiceErrorException;
import org.korolev.dens.ratingservice.services.ClientStatsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/stats/client")
public class ClientStatsController {

    private final ClientStatsService clientStatsService;

    public ClientStatsController(ClientStatsService clientStatsService) {
        this.clientStatsService = clientStatsService;
    }

    @GetMapping("/get/{login}")
    public ResponseEntity<?> getClientStats(@PathVariable String login) throws RateObjectNotFoundException,
            ServiceErrorException {
        return ResponseEntity.ok(clientStatsService.findClientStats(login));
    }

    @GetMapping("/get/places/{login}")
    public ResponseEntity<?> getClientPlaces(@PathVariable String login) throws RateObjectNotFoundException,
            ServiceErrorException {
        return ResponseEntity.ok(clientStatsService.findClientPlaces(login));
    }

    @GetMapping("/get/fame/top/{n}")
    public ResponseEntity<?> getClientsFameTop(@PathVariable Integer n) throws RatingLogicException,
            ServiceErrorException {
        return ResponseEntity.ok(clientStatsService.findClientsFameTop(n));
    }

    @GetMapping("/get/activity/top/{n}")
    public ResponseEntity<?> getClientActivityTop(@PathVariable Integer n) throws RatingLogicException,
            ServiceErrorException {
        return ResponseEntity.ok(clientStatsService.findClientsActivityTop(n));
    }

}
