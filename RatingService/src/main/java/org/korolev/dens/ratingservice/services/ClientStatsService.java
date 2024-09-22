package org.korolev.dens.ratingservice.services;

import org.korolev.dens.ratingservice.entities.Client;
import org.korolev.dens.ratingservice.repositories.ClientRepository;
import org.korolev.dens.ratingservice.responces.ClientPlaces;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ClientStatsService {

    private final ClientRepository clientRepository;

    public ClientStatsService(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    public ResponseEntity<?> findClientStats(String login) {
        Optional<Client> client;
        try {
            client = clientRepository.findById(login);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Database error");
        }
        if (client.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No stats for client " + login);
        }
        return ResponseEntity.ok().body(client.get());
    }

    public ResponseEntity<?> findClientPlaces(String login) {
        List<Client> allClients;
        try {
            allClients = clientRepository.findAll();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Database error");
        }
        if (!allClients.stream().map(Client::getLogin).toList().contains(login)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No stats for client " + login);
        }
        ClientPlaces clientPlaces = new ClientPlaces();
        List<Client> sortedByFame = new ArrayList<>(allClients);
        sortedByFame.sort(Comparator.comparingDouble(Client::getRating).reversed());
        for (int i = 0; i < sortedByFame.size(); i++) {
            if (Objects.equals(sortedByFame.get(i).getLogin(), login)) {
                clientPlaces.setPlaceByFame(i + 1);
            }
        }
        List<Client> sortedByActivity = new ArrayList<>(allClients);
        sortedByActivity.sort(Comparator.comparingInt(Client::getActivity).reversed());
        for (int i = 0; i < sortedByActivity.size(); i++) {
            if (Objects.equals(sortedByActivity.get(i).getLogin(), login)) {
                clientPlaces.setPlaceByActivity(i + 1);
            }
        }
        return ResponseEntity.ok().body(clientPlaces);
    }

    public ResponseEntity<?> findClientsFameTop(Integer n) {
        List<Client> allClients;
        try {
            allClients = clientRepository.findAll();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Database error");
        }
        allClients.sort(Comparator.comparingDouble(Client::getRating).reversed());
        return formTopN(n, allClients);
    }

    public ResponseEntity<?> findClientsActivityTop(Integer n) {
        List<Client> allClients;
        try {
            allClients = clientRepository.findAll();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Database error");
        }
        allClients.sort(Comparator.comparingInt(Client::getActivity).reversed());
        return formTopN(n, allClients);
    }

    private ResponseEntity<?> formTopN(Integer n, List<Client> clients) {
        if (n == 0) {
            return ResponseEntity.ok().body(clients);
        } if (n > 0) {
            return ResponseEntity.ok().body(clients.stream().limit(n).toList());
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("N parameter is negative");
        }
    }

}
