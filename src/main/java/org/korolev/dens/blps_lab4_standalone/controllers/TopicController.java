package org.korolev.dens.blps_lab4_standalone.controllers;

import jakarta.annotation.Nullable;
import org.korolev.dens.blps_lab4_standalone.entites.*;
import org.korolev.dens.blps_lab4_standalone.exceptions.*;
import org.korolev.dens.blps_lab4_standalone.requests.StatsMessage;
import org.korolev.dens.blps_lab4_standalone.services.MessageProducer;
import org.korolev.dens.blps_lab4_standalone.services.TopicService;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


@RestController
@RequestMapping("/topic")
public class TopicController {

    private final TopicService topicService;
    private final MessageProducer messageProducer;

    public TopicController(TopicService topicService, MessageProducer messageProducer) {
        this.topicService = topicService;
        this.messageProducer = messageProducer;
    }

    @GetMapping("/get/all/by/chapter/{chapterId}")
    public ResponseEntity<?> getAllTopicsByChapter(@PathVariable Integer chapterId) throws ForumObjectNotFoundException {
        return ResponseEntity.ok(topicService.findAllByChapter(chapterId));
    }

    @GetMapping("get/by/id/{topicId}")
    public ResponseEntity<?> getTopicById(@PathVariable Integer topicId) throws ForumObjectNotFoundException,
            ForbiddenActionException {
        Pair<Topic, String> gotTopicAndWatcher = topicService.findTopicById(topicId);
        messageProducer.sendMessage(new StatsMessage(
                gotTopicAndWatcher.getFirst().getId(), gotTopicAndWatcher.getSecond(),
                "watch", gotTopicAndWatcher.getFirst().getOwner().getLogin())
        );
        return ResponseEntity.ok(gotTopicAndWatcher.getFirst());
    }

    @GetMapping("get/image/{URL}")
    public ResponseEntity<?> getTopicImageByURL(@PathVariable String URL) throws ImageAccessException {
        return ResponseEntity.ok(topicService.getImageFromDisk(URL));
    }

    @PostMapping("/add/{chapterId}")
    public ResponseEntity<?> addTopic(@RequestParam("title") String title, @RequestParam("text") String text,
                                      @PathVariable Integer chapterId,
                                      @RequestParam("img1") @Nullable MultipartFile img1,
                                      @RequestParam("img2") @Nullable MultipartFile img2,
                                      @RequestParam("img3") @Nullable MultipartFile img3) throws ForumException {
        Topic topic = new Topic();
        topic.setTitle(title);
        topic.setText(text);
        Approval addedApproval = topicService.add(topic, chapterId, img1, img2, img3);
        messageProducer.sendMessage(new StatsMessage(
                addedApproval.getTopic().getId(), "",
                "add", addedApproval.getTopic().getOwner().getLogin())
        );
        return ResponseEntity.ok(addedApproval);
    }

    @DeleteMapping("/delete/{topicId}")
    public ResponseEntity<?> deleteTopic(@PathVariable Integer topicId) throws ForumException {
        Topic deletedTopic = topicService.delete(topicId);
        messageProducer.sendMessage(new StatsMessage(topicId, "", "delete",
                deletedTopic.getOwner().getLogin()));
        return ResponseEntity.status(HttpStatus.OK).body("Topic with id " + topicId + " deleted");
    }

    @GetMapping("/get/all/to/approve")
    public ResponseEntity<?> getAllToApprove() {
        return ResponseEntity.ok(topicService.findAllWaitingForApprove());
    }

    @DeleteMapping("/approve/{approvalId}/{isApproved}")
    public ResponseEntity<?> approveTopic(@PathVariable Integer approvalId, @PathVariable Boolean isApproved)
            throws ForumException {
        topicService.approveOrReject(approvalId, isApproved);
        if (isApproved) {
            return ResponseEntity.status(HttpStatus.OK).body("Topic is approved");
        } else {
            return ResponseEntity.status(HttpStatus.OK).body("Topic is rejected");
        }
    }

    @PostMapping("/update")
    public ResponseEntity<?> updateTopic(@Validated @RequestBody Topic topic) throws ForumException {
        return ResponseEntity.ok(topicService.update(topic));
    }

    @PostMapping("/add/rating/{topicId}")
    public ResponseEntity<?> addRating(@RequestBody @Validated Rating rating, @PathVariable Integer topicId)
            throws ForumException {
        Rating addedRating = topicService.rate(rating, topicId);
        messageProducer.sendMessage(new StatsMessage(
                topicId, addedRating.getCreator().getLogin(),
                addedRating.getRating().toString(), addedRating.getTopic().getOwner().getLogin())
        );
        return ResponseEntity.ok(addedRating);
    }

    @GetMapping("/get/rating/{topicId}")
    public ResponseEntity<?> getRatingOfClients(@PathVariable Integer topicId) throws ForumObjectNotFoundException {
        return ResponseEntity.ok(topicService.findTopicRatings(topicId));
    }

    @DeleteMapping("/delete/subscription/{topicId}")
    public ResponseEntity<?> deleteSubscription(@PathVariable Integer topicId) throws ForumException {
        topicService.unsubscribe(topicId);
        return ResponseEntity.ok("Subscription for topic  " + topicId + " deleted successfully");
    }

    @PostMapping("/add/subscription/{topicId}")
    public ResponseEntity<?> addSubscription(@PathVariable Integer topicId) throws ForbiddenActionException,
            ForumLogicException, ForumObjectNotFoundException {
        return ResponseEntity.ok(topicService.subscribe(topicId));
    }

}
