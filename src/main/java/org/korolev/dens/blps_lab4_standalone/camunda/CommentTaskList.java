package org.korolev.dens.blps_lab4_standalone.camunda;

import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.camunda.bpm.engine.variable.VariableMap;
import org.camunda.bpm.engine.variable.Variables;
import org.korolev.dens.blps_lab4_standalone.aspects.Authorize;
import org.korolev.dens.blps_lab4_standalone.aspects.ConfusionReport;
import org.korolev.dens.blps_lab4_standalone.entites.Comment;
import org.korolev.dens.blps_lab4_standalone.exceptions.ForumException;
import org.korolev.dens.blps_lab4_standalone.exceptions.ForumObjectNotFoundException;
import org.korolev.dens.blps_lab4_standalone.requests.StatsMessage;
import org.korolev.dens.blps_lab4_standalone.services.CommentService;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class CommentTaskList {

    private final CommentService commentService;

    public CommentTaskList(CommentService commentService) {
        this.commentService = commentService;
    }

    @Authorize
    @ConfusionReport
    public void performAddComment(ExternalTask externalTask, ExternalTaskService externalTaskService)
            throws ForumException {
        String commentText = externalTask.getVariable("text");
        Integer topicId = externalTask.getVariable("topicId");
        Comment comment = new Comment();
        comment.setText(commentText);
        comment.setCreated(LocalDate.now());
        Comment addedComment = commentService.comment(topicId, 0, comment);
        StatsMessage statsMessage = new StatsMessage(
                topicId, addedComment.getCommentator().getLogin(),
                "comment", addedComment.getTopic().getOwner().getLogin()
        );
        VariableMap variableMap = Variables.createVariables();
        variableMap.put("message", statsMessage);
        externalTaskService.complete(externalTask, variableMap);
    }

    @Authorize
    @ConfusionReport
    public void performDeleteComment(ExternalTask externalTask, ExternalTaskService externalTaskService)
            throws ForumObjectNotFoundException {
        Integer commentId = externalTask.getVariable("selectedComment");
        commentService.delete(commentId);
        externalTaskService.complete(externalTask);
    }

    @ConfusionReport
    public void performGetTopicComments(ExternalTask externalTask, ExternalTaskService externalTaskService)
            throws ForumObjectNotFoundException {
        Integer topicId = externalTask.getVariable("topicId");
        List<Comment> comments = commentService.findAllByTopic(topicId);
        VariableMap variableMap = Variables.createVariables();
        variableMap.put("comments", comments);
        externalTaskService.complete(externalTask, variableMap);
    }

}
