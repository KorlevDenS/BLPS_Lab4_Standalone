package org.korolev.dens.blps_lab4_standalone.services;

import org.korolev.dens.blps_lab4_standalone.entites.*;
import org.korolev.dens.blps_lab4_standalone.exceptions.*;
import org.korolev.dens.blps_lab4_standalone.repositories.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
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

    public CommentService(PlatformTransactionManager platformTransactionManager, CommentRepository commentRepository,
                          TopicRepository topicRepository, ClientRepository clientRepository,
                          SubscriptionRepository subscriptionRepository, NotificationRepository notificationRepository) {
        this.transactionTemplate = new TransactionTemplate(platformTransactionManager);
        this.subscriptionRepository = subscriptionRepository;
        this.notificationRepository = notificationRepository;
        this.transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
        this.commentRepository = commentRepository;
        this.topicRepository = topicRepository;
        this.clientRepository = clientRepository;
    }

    public List<Comment> findAllByTopic(Integer topicId) throws ForumObjectNotFoundException {
        if (topicRepository.findById(topicId).isEmpty()) {
            throw new ForumObjectNotFoundException("Topic with id " + topicId + " not found");
        }
        return commentRepository.getAllByTopic(topicId);
    }

    @PreAuthorize("hasRole('MODER')")
    public void delete(Integer commentId) throws ForumObjectNotFoundException {
        Optional<Comment> optionalComment = commentRepository.findById(commentId);
        if (optionalComment.isEmpty()) {
            throw new ForumObjectNotFoundException("Comment with id " + commentId + " not found");
        }
        commentRepository.deleteById(commentId);
    }


    @PreAuthorize("hasRole('USER')")
    public Comment comment(Integer topicId, Integer quoteId, Comment comment) throws ForumException {
        String login = ((UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                .getUsername();
        TransactionExceptionKeeper keeper = new TransactionExceptionKeeper();
        Comment c = transactionTemplate.execute(status -> {
            Comment addedComment;
            try {
                if (quoteId > 0) {
                    Optional<Comment> optionalComment = commentRepository.findById(quoteId);
                    if (optionalComment.isEmpty()) {
                        keeper.setEx(new ForumObjectNotFoundException(
                                "Comment for quoting " + quoteId + " does not exist"));
                        return null;
                    }
                    comment.setQuote(optionalComment.get());
                }
                Optional<Topic> optionalTopic = topicRepository.findById(topicId); //TODO
                Optional<Client> optionalClient = clientRepository.findByLogin(login);
                if (optionalClient.isEmpty()) {
                    keeper.setEx(new ForbiddenActionException("Illegal access to resource: no such client in database"));
                    return null;
                } else if (optionalTopic.isEmpty()) {
                    keeper.setEx(new ForumObjectNotFoundException("Topic " + topicId + " does not exist"));
                    return null;
                }
                comment.setCommentator(optionalClient.get());
                comment.setTopic(optionalTopic.get());
                try {
                    addedComment = commentRepository.save(comment);
                } catch (Exception e) {
                    status.setRollbackOnly();
                    keeper.setEx(new ForumException("Comment was not saved to data base"));
                    return null;
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
                        keeper.setEx(new ForumException("Could not send notification to subscribers"));
                        return null;
                    }
                }
            } catch (Exception e) {
                status.setRollbackOnly();
                keeper.setEx(new ForumException("Comment was not added"));
                return null;
            }
            return addedComment;
        });
        keeper.throwIfSet();
        return c;
    }

}
