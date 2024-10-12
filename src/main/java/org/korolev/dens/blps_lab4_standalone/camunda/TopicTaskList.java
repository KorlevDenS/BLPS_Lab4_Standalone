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
import org.korolev.dens.blps_lab4_standalone.exceptions.ForumException;
import org.korolev.dens.blps_lab4_standalone.exceptions.ForumObjectNotFoundException;
import org.korolev.dens.blps_lab4_standalone.requests.SimpleTopic;
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
    public void performRateTopic(ExternalTask externalTask, ExternalTaskService externalTaskService)
            throws ForumException {
        Integer topicId = externalTask.getVariable("selectedTopic");
        Integer rating = externalTask.getVariable("rating");
        Rating ratingObj = new Rating();
        ratingObj.setRating(rating);
        ratingObj.setCreated(LocalDate.now());
        topicService.rate(ratingObj, topicId);
        externalTaskService.complete(externalTask);
    }

    @Authorize
    @ConfusionReport
    public void performUpdateTopic(ExternalTask externalTask, ExternalTaskService externalTaskService)
            throws ForumException {
        Integer topicId = externalTask.getVariable("selectedTopic");
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
        Integer topicId = externalTask.getVariable("selectedTopic");
        topicService.delete(topicId);
        externalTaskService.complete(externalTask);
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
        Approval approval = topicService.add(topic, externalTask.getVariable("selected"), image1, image2, image3);
        VariableMap variableMap = Variables.createVariables();
        variableMap.put("approval", approval);
        variableMap.put("approvalId", approval.getId());
        variableMap.put("topicId", approval.getTopic().getId());
        externalTaskService.complete(externalTask, variableMap);
    }

}
