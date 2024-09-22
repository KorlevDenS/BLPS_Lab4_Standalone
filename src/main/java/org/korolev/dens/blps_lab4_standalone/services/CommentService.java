package org.korolev.dens.blps_lab4_standalone.services;

import org.korolev.dens.blps_lab4_standalone.entites.*;
import org.korolev.dens.blps_lab4_standalone.repositories.*;
import org.korolev.dens.blps_lab4_standalone.requests.StatsMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Optional;

@Service
public class CommentService {

    private final TransactionTemplate transactionTemplate;
    private final CommentRepository commentRepository;
    private final TopicRepository topicRepository;
    private final ClientRepository clientRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final NotificationRepository notificationRepository;
    private final MessageProducer messageProducer;

    public CommentService(PlatformTransactionManager platformTransactionManager, CommentRepository commentRepository,
                          TopicRepository topicRepository, ClientRepository clientRepository,
                          SubscriptionRepository subscriptionRepository, NotificationRepository notificationRepository, MessageProducer messageProducer) {
        this.transactionTemplate = new TransactionTemplate(platformTransactionManager);
        this.subscriptionRepository = subscriptionRepository;
        this.notificationRepository = notificationRepository;
        this.transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
        this.commentRepository = commentRepository;
        this.topicRepository = topicRepository;
        this.clientRepository = clientRepository;
        this.messageProducer = messageProducer;
    }


    public ResponseEntity<?> comment(String login, Integer topicId, Integer quoteId, Comment comment) {
        return transactionTemplate.execute(status -> {
            Comment addedComment;
            Client topicOwner;
            try {
                if (quoteId > 0) {
                    Optional<Comment> optionalComment = commentRepository.findById(quoteId);
                    if (optionalComment.isEmpty()) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Комментарий для цитирования не существует");
                    }
                    comment.setQuote(optionalComment.get());
                }
                Optional<Topic> optionalTopic = topicRepository.findById(topicId);
                Optional<Client> optionalClient = clientRepository.findByLogin(login);
                if (optionalClient.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Пользователь не авторизован");
                } else if (optionalTopic.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Тема не существует");
                }
                topicOwner = optionalTopic.get().getOwner();
                comment.setCommentator(optionalClient.get());
                comment.setTopic(optionalTopic.get());
                try {
                    addedComment = commentRepository.save(comment);
                } catch (Exception e) {
                    status.setRollbackOnly();
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("Comment was not saved to data base");
                }
                List<Subscription> subscriptions = subscriptionRepository.findAllByTopic(optionalTopic.get());
                for (Subscription subscription : subscriptions) {
                    Notification notification = new Notification();
                    notification.setTopic(optionalTopic.get());
                    notification.setInitiator(optionalClient.get());
                    notification.setRecipient(subscription.getClient());
                    notification.setDescription("Пользователь " + optionalClient.get().getLogin()
                            + " добавил комментарий к теме " + optionalTopic.get().getTitle());
                    try {
                        notificationRepository.save(notification);
                    } catch (Exception e) {
                        status.setRollbackOnly();
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body("Could not send notification to subscribers");
                    }
                }
            } catch (Exception e) {
                status.setRollbackOnly();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Comment was not added");
            }
            messageProducer.sendMessage(new StatsMessage(topicId, login, "comment", topicOwner.getLogin()));
            return ResponseEntity.ok(addedComment);
        });
    }

}
