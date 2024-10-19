package org.korolev.dens.blps_lab4_standalone.controllers;

import org.korolev.dens.blps_lab4_standalone.entites.*;
import org.korolev.dens.blps_lab4_standalone.exceptions.ForumException;
import org.korolev.dens.blps_lab4_standalone.exceptions.ForumObjectNotFoundException;
import org.korolev.dens.blps_lab4_standalone.requests.StatsMessage;
import org.korolev.dens.blps_lab4_standalone.services.CommentService;
import org.korolev.dens.blps_lab4_standalone.services.MessageProducer;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/comment")
public class CommentController {

    private final CommentService commentService;
    private final MessageProducer messageProducer;

    public CommentController(CommentService commentService, MessageProducer messageProducer) {
        this.commentService = commentService;
        this.messageProducer = messageProducer;
    }

    @GetMapping("/get/all/by/topic/{topicId}")
    public ResponseEntity<?> getAllByTopic(@PathVariable Integer topicId) throws ForumObjectNotFoundException {
        return ResponseEntity.ok(commentService.findAllByTopic(topicId));
    }

    @DeleteMapping("/delete/{commentId}")
    public ResponseEntity<?> deleteComment(@PathVariable Integer commentId) throws ForumObjectNotFoundException {
        commentService.delete(commentId);
        return ResponseEntity.ok("Comment with id " + commentId + " deleted");
    }

    @PostMapping("/add/{topicId}/{quoteId}")
    public ResponseEntity<?> addComment(@PathVariable Integer topicId, @PathVariable Integer quoteId,
                                        @RequestBody Comment comment) throws ForumException {
        Comment addedComment = commentService.comment(topicId, quoteId, comment);
        messageProducer.sendMessage(new StatsMessage(
                topicId, addedComment.getCommentator().getLogin(),
                "comment", addedComment.getTopic().getOwner().getLogin())
        );
        return ResponseEntity.ok(addedComment);
    }

}
