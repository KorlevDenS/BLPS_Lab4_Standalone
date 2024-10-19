package org.korolev.dens.blps_lab4_standalone.camunda;

import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.camunda.bpm.engine.variable.VariableMap;
import org.camunda.bpm.engine.variable.Variables;
import org.korolev.dens.blps_lab4_standalone.aspects.Authorize;
import org.korolev.dens.blps_lab4_standalone.aspects.ConfusionReport;
import org.korolev.dens.blps_lab4_standalone.entites.Approval;
import org.korolev.dens.blps_lab4_standalone.entites.Rating;
import org.korolev.dens.blps_lab4_standalone.entites.Topic;
import org.korolev.dens.blps_lab4_standalone.exceptions.ForbiddenActionException;
import org.korolev.dens.blps_lab4_standalone.exceptions.ForumException;
import org.korolev.dens.blps_lab4_standalone.exceptions.ForumLogicException;
import org.korolev.dens.blps_lab4_standalone.exceptions.ForumObjectNotFoundException;
import org.korolev.dens.blps_lab4_standalone.requests.SimpleTopic;
import org.korolev.dens.blps_lab4_standalone.requests.StatsMessage;
import org.korolev.dens.blps_lab4_standalone.services.TopicService;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
public class TopicTaskList {

    private final TopicService topicService;

    public TopicTaskList(TopicService topicService) {
        this.topicService = topicService;
    }

    @Authorize
    @ConfusionReport
    public void performUnsubscribe(ExternalTask externalTask, ExternalTaskService externalTaskService)
            throws ForumException {
        Integer topicId = externalTask.getVariable("topicId");
        topicService.unsubscribe(topicId);
        externalTaskService.complete(externalTask);
    }

    @Authorize
    @ConfusionReport
    public void performSubscribe(ExternalTask externalTask, ExternalTaskService externalTaskService)
            throws ForbiddenActionException, ForumLogicException, ForumObjectNotFoundException {
        Integer topicId = externalTask.getVariable("topicId");
        topicService.subscribe(topicId);
        externalTaskService.complete(externalTask);
    }

    @Authorize
    @ConfusionReport
    public void performGetRating(ExternalTask externalTask, ExternalTaskService externalTaskService)
            throws ForumObjectNotFoundException {
        Integer topicId = externalTask.getVariable("topicId");
        List<Rating> ratings = topicService.findTopicRatings(topicId);
        VariableMap variableMap = Variables.createVariables();
        variableMap.put("ratings", ratings);
        externalTaskService.complete(externalTask, variableMap);
    }

    @Authorize
    @ConfusionReport
    public void performRateTopic(ExternalTask externalTask, ExternalTaskService externalTaskService)
            throws ForumException {
        Integer topicId = externalTask.getVariable("topicId");
        Integer rating = Integer.parseInt(externalTask.getVariable("rating"));
        Rating ratingObj = new Rating();
        ratingObj.setRating(rating);
        ratingObj.setCreated(LocalDate.now());
        Rating addedRating = topicService.rate(ratingObj, topicId);
        StatsMessage statsMessage = new StatsMessage(
                topicId, addedRating.getCreator().getLogin(),
                addedRating.getRating().toString(), addedRating.getTopic().getOwner().getLogin()
        );
        VariableMap variableMap = Variables.createVariables();
        variableMap.put("message", statsMessage);
        externalTaskService.complete(externalTask, variableMap);
    }

    @Authorize
    @ConfusionReport
    public void performUpdateTopic(ExternalTask externalTask, ExternalTaskService externalTaskService)
            throws ForumException {
        Integer topicId = externalTask.getVariable("topicId");
        String newTitle = externalTask.getVariable("newTitle");
        String newText = externalTask.getVariable("newText");
        Topic updateTopic = new Topic();
        updateTopic.setId(topicId);
        updateTopic.setTitle(newTitle);
        updateTopic.setText(newText);
        topicService.update(updateTopic);
        externalTaskService.complete(externalTask);
    }

    @ConfusionReport
    public void performGetAllByChapter(ExternalTask externalTask, ExternalTaskService externalTaskService)
            throws ForumObjectNotFoundException {
        Integer chapterId = externalTask.getVariable("selected");
        List<Topic> topics = topicService.findAllByChapter(chapterId);
        List<SimpleTopic> topicList = new ArrayList<>();
        topics.forEach(topic -> topicList.add(
                new SimpleTopic(topic.getId(), topic.getTitle(), topic.getText(), topic.getOwner().getLogin())));
        VariableMap variableMap = Variables.createVariables();
        variableMap.put("chapterTopics", topicList);
        externalTaskService.complete(externalTask, variableMap);
    }

    @Authorize
    @ConfusionReport
    public void performApproveTopic(ExternalTask externalTask, ExternalTaskService externalTaskService)
            throws ForumObjectNotFoundException {
        Integer approvalId = externalTask.getVariable("approvalId");
        topicService.approveByApprovalId(approvalId);
        externalTaskService.complete(externalTask);
    }

    @Authorize
    @ConfusionReport
    public void performDeleteTopic(ExternalTask externalTask, ExternalTaskService externalTaskService)
            throws ForumException {
        Integer topicId = externalTask.getVariable("topicId");
        Topic deletedTopic = topicService.delete(topicId);
        StatsMessage statsMessage = new StatsMessage(topicId, "", "delete",
                deletedTopic.getOwner().getLogin());
        VariableMap variableMap = Variables.createVariables();
        variableMap.put("message", statsMessage);
        externalTaskService.complete(externalTask, variableMap);
    }


    @Authorize
    @ConfusionReport
    public void performAddTopic(ExternalTask externalTask, ExternalTaskService externalTaskService)
            throws ForumException {
        ByteArrayInputStream object1 = externalTask.getVariable("img1");
        ByteArrayInputStream object2 = externalTask.getVariable("img2");
        ByteArrayInputStream object3 = externalTask.getVariable("img3");
        FormImage image1 = new FormImage(object1);
        FormImage image2 = new FormImage(object2);
        FormImage image3 = new FormImage(object3);
        Topic topic = new Topic();
        topic.setTitle(externalTask.getVariable("topicTitle"));
        topic.setText(externalTask.getVariable("topicText"));
        Approval addedApproval = topicService.add(topic, externalTask.getVariable("selected"), image1, image2, image3);
        StatsMessage statsMessage = new StatsMessage(
                addedApproval.getTopic().getId(), "",
                "add", addedApproval.getTopic().getOwner().getLogin()
        );
        VariableMap variableMap = Variables.createVariables();
        variableMap.put("approval", addedApproval);
        variableMap.put("approvalId", addedApproval.getId());
        variableMap.put("topicId", addedApproval.getTopic().getId());
        variableMap.put("message", statsMessage);
        externalTaskService.complete(externalTask, variableMap);
    }

}
