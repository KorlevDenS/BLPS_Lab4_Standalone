package org.korolev.dens.ratingservice.services;

import org.korolev.dens.ratingservice.entities.Client;
import org.korolev.dens.ratingservice.exceptions.RateObjectNotFoundException;
import org.korolev.dens.ratingservice.exceptions.RatingLogicException;
import org.korolev.dens.ratingservice.exceptions.ServiceErrorException;
import org.korolev.dens.ratingservice.repositories.ClientRepository;
import org.korolev.dens.ratingservice.responces.ClientPlaces;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ClientStatsService {

    private final ClientRepository clientRepository;

    public ClientStatsService(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    public Client findClientStats(String login) throws ServiceErrorException, RateObjectNotFoundException {
        Optional<Client> client;
        try {
            client = clientRepository.findById(login);
        } catch (Exception e) {
            throw new ServiceErrorException("Database error");
        }
        if (client.isEmpty()) {
            throw new RateObjectNotFoundException("No stats for client " + login);
        }
        return client.get();
    }

    public ClientPlaces findClientPlaces(String login) throws ServiceErrorException, RateObjectNotFoundException {
        List<Client> allClients;
        try {
            allClients = clientRepository.findAll();
        } catch (Exception e) {
            throw new ServiceErrorException("Database error");
        }
        if (!allClients.stream().map(Client::getLogin).toList().contains(login)) {
            throw new RateObjectNotFoundException("No stats for client " + login);
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
        return clientPlaces;
    }

    public List<Client> findClientsFameTop(Integer n) throws ServiceErrorException, RatingLogicException {
        List<Client> allClients;
        try {
            allClients = clientRepository.findAll();
        } catch (Exception e) {
            throw new ServiceErrorException("Database error");
        }
        allClients.sort(Comparator.comparingDouble(Client::getRating).reversed());
        return formTopN(n, allClients);
    }

    public List<Client> findClientsActivityTop(Integer n) throws RatingLogicException, ServiceErrorException {
        List<Client> allClients;
        try {
            allClients = clientRepository.findAll();
        } catch (Exception e) {
            throw new ServiceErrorException("Database error");
        }
        allClients.sort(Comparator.comparingInt(Client::getActivity).reversed());
        return formTopN(n, allClients);
    }

    private List<Client> formTopN(Integer n, List<Client> clients) throws RatingLogicException {
        if (n == 0) {
            return clients;
        } if (n > 0) {
            return clients.stream().limit(n).toList();
        } else {
            throw new RatingLogicException("N parameter is negative", HttpStatus.BAD_REQUEST);
        }
    }

}
