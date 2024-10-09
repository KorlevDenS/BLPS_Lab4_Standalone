package org.korolev.dens.ratingservice.services;

import org.korolev.dens.ratingservice.exceptions.RatingComputeException;
import org.korolev.dens.ratingservice.entities.Client;
import org.korolev.dens.ratingservice.entities.Rating;
import org.korolev.dens.ratingservice.entities.Topic;
import org.korolev.dens.ratingservice.repositories.ClientRepository;
import org.korolev.dens.ratingservice.repositories.RatingRepository;
import org.korolev.dens.ratingservice.repositories.TopicRepository;
import jakarta.transaction.Transactional;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.OptionalDouble;

@Service
public class RatingComputingService {

    private final TopicRepository topicRepository;
    private final RatingRepository ratingRepository;
    private final ClientRepository clientRepository;

    public RatingComputingService(TopicRepository topicRepository, RatingRepository ratingRepository, ClientRepository clientRepository) {
        this.topicRepository = topicRepository;
        this.ratingRepository = ratingRepository;
        this.clientRepository = clientRepository;
    }

    public double calcNorm(int x, List<Integer> xList, double offset) throws RatingComputeException {
        if (xList.isEmpty()) {
            throw new RatingComputeException("List is empty");
        }
        double min = Collections.min(xList);
        double max = Collections.max(xList);
        if (max - min == 0.0) {
            throw new RatingComputeException("Min value == max value");
        }
        return (x - min) / (max - min) + offset;
    }

    public double calcNorm(double x, double min, double max, double offset) {
        return (x - min) / (max - min) + offset;
    }

    @Scheduled(cron = "0 18 * * * ?")
    @Transactional
    public void updateClientRating() {
        List<Client> allClients = clientRepository.findAll();
        for (Client client : allClients) {
            double authorSum = client.getTopics().stream().map(Topic::getFame).mapToDouble(Double::doubleValue).sum();
            client.setRating(authorSum);
            clientRepository.save(client);
        }
    }

    /// 0.5 <= k <= 1;
    /// 1 <= a <= 2;
    /// 2 <= b <= 3;
    @Scheduled(cron = "0 0 * * * ?")
    public void updateTopicRatings() {
        List<Topic> allTopics = topicRepository.findAll();
        List<Double> ratingAVGs = allTopics.stream().map(topic -> ratingRepository.findAllByTopic(topic).stream()
                .map(Rating::getRating).mapToInt(Integer::intValue).average()).filter(OptionalDouble::isPresent)
                .map(OptionalDouble::getAsDouble).toList();
        double ratingAvgMin;
        double ratingAvgMax;
        try {
            ratingAvgMin = Collections.min(ratingAVGs);
            ratingAvgMax = Collections.max(ratingAVGs);
        } catch (NoSuchElementException e) {
            ratingAvgMin = 1;
            ratingAvgMax = 1;
            System.out.println("Still empty rating list. Impossible to calculate min and max!");
        }
        List<Integer> temporalViewsAll = allTopics.stream().map(Topic::getTemporal_views).toList();
        List<Integer> temporalCommentsAll = allTopics.stream().map(Topic::getTemporal_comments).toList();
        List<Integer> temporalViewsComments = allTopics.stream()
                .map(t -> t.getTemporal_comments() + t.getTemporal_views()).toList();
        for (Topic topic : allTopics) {
            double k;
            double a;
            double b;
            try {
                k = calcNorm(topic.getTemporal_views() + topic.getTemporal_comments(), temporalViewsComments, 0)
                        / 2 + 0.5;
            } catch (RatingComputeException e) {
                k = 1;
            }
            try {
                a = calcNorm(topic.getTemporal_views(), temporalViewsAll, 1);
            } catch (RatingComputeException e) {
                a = 1.5;
            }
            try {
                b = calcNorm(topic.getTemporal_comments(), temporalCommentsAll, 2);
            } catch (RatingComputeException e) {
                b = 2.5;
            }
            double temporalFame = k * topic.getTemporal_fame() + a * topic.getTemporal_views() + b * topic.getTemporal_comments();
            double k1;
            OptionalDouble optionalRatingAvg = ratingRepository.findAllByTopic(topic).stream().map(Rating::getRating)
                    .mapToInt(Integer::intValue).average();
            if (optionalRatingAvg.isEmpty()) {
                k1 = 1;
            } else {
                if (ratingAvgMax == ratingAvgMin) {
                    k1 = 1;
                } else {
                    double ratingAvg = optionalRatingAvg.getAsDouble();
                    k1 = ratingAvg * calcNorm(ratingAvg, ratingAvgMin, ratingAvgMax, 1);
                }
            }
            double fame = k1 * temporalFame;
            System.out.println("Was: " + topic + "\n");
            System.out.println("k=" + k + ", a=" + a + ", b=" + b + ", k1=" + k1 + "\n");
            topic.setFame(fame);
            topic.setTemporal_fame(temporalFame);
            topic.setTemporal_views(0);
            topic.setTemporal_comments(0);
            topicRepository.save(topic);
            System.out.println("Became: " + topic + "\n");
        }
    }

}
