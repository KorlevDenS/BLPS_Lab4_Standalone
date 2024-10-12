package org.korolev.dens.blps_lab4_standalone.camunda;

import lombok.SneakyThrows;
import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskHandler;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Async;

@Configuration
public class CommentHandlers {

    private final CommentTaskList commentTaskList;

    public CommentHandlers(CommentTaskList commentTaskList) {
        this.commentTaskList = commentTaskList;
    }

    @Bean
    @ExternalTaskSubscription(
            topicName = "deleteComment",
            processDefinitionKey = "moderateForum",
            includeExtensionProperties = true,
            variableNames = {"jwtToken", "selectedComment"}
    )
    public ExternalTaskHandler deleteComment() {
        return new ExternalTaskHandler() {
            @Async
            @SneakyThrows
            @Override
            public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
                commentTaskList.performDeleteComment(externalTask, externalTaskService);
            }
        };
    }

    @Bean
    @ExternalTaskSubscription(
            topicName = "getComments",
            processDefinitionKey = "moderateForum",
            includeExtensionProperties = true,
            variableNames = {"selectedTopic"}
    )
    public ExternalTaskHandler getTopicComments() {
        return new ExternalTaskHandler() {
            @Async
            @SneakyThrows
            @Override
            public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
                commentTaskList.performGetTopicComments(externalTask, externalTaskService);
            }
        };
    }

}
