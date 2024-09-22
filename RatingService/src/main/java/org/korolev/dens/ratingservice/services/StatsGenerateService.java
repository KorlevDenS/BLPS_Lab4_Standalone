package org.korolev.dens.ratingservice.services;

import org.korolev.dens.ratingservice.entities.Client;
import org.korolev.dens.ratingservice.entities.Rating;
import org.korolev.dens.ratingservice.entities.Topic;
import org.korolev.dens.ratingservice.repositories.ClientRepository;
import org.korolev.dens.ratingservice.repositories.RatingRepository;
import org.korolev.dens.ratingservice.repositories.TopicRepository;
import org.korolev.dens.ratingservice.responces.StatsMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class StatsGenerateService {

    private final ClientRepository clientRepository;
    private final TopicRepository topicRepository;
    private final RatingRepository ratingRepository;

    public StatsGenerateService(ClientRepository clientRepository, TopicRepository topicRepository, RatingRepository ratingRepository) {
        this.clientRepository = clientRepository;
        this.topicRepository = topicRepository;
        this.ratingRepository = ratingRepository;
    }

    @Transactional
    public void processMessage(String message) {
        ObjectMapper jsonMapper = new ObjectMapper();
        StatsMessage statsMessage;
        try {
            statsMessage = jsonMapper.readValue(message, StatsMessage.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        Client ownerClient = installClient(statsMessage.getTopicOwner());

        Optional<Topic> optionalTopic = topicRepository.findById(statsMessage.getTopicId());
        Topic topic;
        if (optionalTopic.isEmpty()) {
            Topic newTopic = new Topic();
            newTopic.setId(statsMessage.getTopicId());
            newTopic.setOwner(ownerClient);
            newTopic.setViews(0);
            newTopic.setTemporal_views(0);
            newTopic.setTemporal_fame(10.0);
            newTopic.setTemporal_comments(0);
            newTopic.setFame(10.0);
            topic = topicRepository.save(newTopic);
        } else {
            topic = optionalTopic.get();
        }

        switch (statsMessage.getProducerAction()) {
            case "watch" -> processWatch(topic, statsMessage.getProducer());
            case "comment" -> processComment(topic, statsMessage.getProducer());
            case "1", "2", "3", "4", "5", "6", "7", "8", "9", "10" -> processRate(topic, statsMessage.getProducer(),
                    statsMessage.getProducerAction());
            case "add" -> processAdd(ownerClient);
            case "delete" -> processDelete(topic);
            default -> System.out.println("Wrong message");
        }
    }

    private Client installClient(String login) {
        Optional<Client> optionalClient = clientRepository.findById(login);
        Client client;
        if (optionalClient.isEmpty()) {
            Client newClient = new Client();
            newClient.setLogin(login);
            newClient.setRating(1.0);
            newClient.setActivity(1);
            client = clientRepository.save(newClient);
        } else {
            client = optionalClient.get();
        }
        return client;
    }

    private void processWatch(Topic topic, String producerLogin) {
        if (!producerLogin.isEmpty()) {
            Client producer = installClient(producerLogin);
            producer.setActivity(producer.getActivity() + 1);
            clientRepository.save(producer);
        }
        topic.setViews(topic.getViews() + 1);
        topic.setTemporal_views(topic.getTemporal_views() + 1);
        topicRepository.save(topic);
    }

    private void processComment(Topic topic, String producerLogin) {
        Client producer = installClient(producerLogin);
        topic.setTemporal_comments(topic.getTemporal_comments() + 1);
        topicRepository.save(topic);
        producer.setActivity(producer.getActivity() + 4);
        clientRepository.save(producer);
    }

    private void processRate(Topic topic, String producerLogin, String strRating) {
        Client producer = installClient(producerLogin);
        Rating rating;
        Optional<Rating> oldRating = ratingRepository.findByCreatorAndTopic(producer, topic);
        rating = oldRating.orElseGet(Rating::new);
        rating.setCreator(producer);
        rating.setTopic(topic);
        rating.setRating(Integer.parseInt(strRating));
        ratingRepository.save(rating);
        producer.setActivity(producer.getActivity() + 2);
        clientRepository.save(producer);
    }

    private void processAdd(Client owner) {
        owner.setActivity(owner.getActivity() + 10);
        clientRepository.save(owner);
    }

    private void processDelete(Topic topic) {
        ratingRepository.removeAllByTopic(topic);
        topicRepository.delete(topic);
    }

}
