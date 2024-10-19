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
public class TopicHandlers {

    private final TopicTaskList topicTaskList;

    public TopicHandlers(TopicTaskList topicTaskList) {
        this.topicTaskList = topicTaskList;
    }

    @Bean
    @ExternalTaskSubscription(
            topicName = "unsubscribe",
            processDefinitionKeyIn = {"interactWithTopic"},
            includeExtensionProperties = true,
            variableNames = {"jwtToken", "topicId"}
    )
    public ExternalTaskHandler unsubscribe() {
        return new ExternalTaskHandler() {
            @Async
            @SneakyThrows
            @Override
            public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
                topicTaskList.performUnsubscribe(externalTask, externalTaskService);
            }
        };
    }

    @Bean
    @ExternalTaskSubscription(
            topicName = "subscribe",
            processDefinitionKeyIn = {"interactWithTopic"},
            includeExtensionProperties = true,
            variableNames = {"jwtToken", "topicId"}
    )
    public ExternalTaskHandler subscribe() {
        return new ExternalTaskHandler() {
            @Async
            @SneakyThrows
            @Override
            public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
                topicTaskList.performSubscribe(externalTask, externalTaskService);
            }
        };
    }

    @Bean
    @ExternalTaskSubscription(
            topicName = "showRating",
            processDefinitionKeyIn = {"interactWithTopic"},
            includeExtensionProperties = true,
            variableNames = {"jwtToken", "topicId"}
    )
    public ExternalTaskHandler showTopicRatings() {
        return new ExternalTaskHandler() {
            @Async
            @SneakyThrows
            @Override
            public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
                topicTaskList.performGetRating(externalTask, externalTaskService);
            }
        };
    }

    @Bean
    @ExternalTaskSubscription(
            topicName = "rateTopic",
            processDefinitionKeyIn = {"interactWithTopic"},
            includeExtensionProperties = true,
            variableNames = {"jwtToken", "topicId", "rating"}
    )
    public ExternalTaskHandler rateTopic() {
        return new ExternalTaskHandler() {
            @Async
            @SneakyThrows
            @Override
            public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
                topicTaskList.performRateTopic(externalTask, externalTaskService);
            }
        };
    }

    @Bean
    @ExternalTaskSubscription(
            topicName = "updateTopic",
            processDefinitionKeyIn = {"interactWithTopic"},
            includeExtensionProperties = true,
            variableNames = {"jwtToken", "topicId", "newTitle", "newText"}
    )
    public ExternalTaskHandler updateTopic() {
        return new ExternalTaskHandler() {
            @Async
            @SneakyThrows
            @Override
            public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
                topicTaskList.performUpdateTopic(externalTask, externalTaskService);
            }
        };
    }

    @Bean
    @ExternalTaskSubscription(
            topicName = "getTopics",
            processDefinitionKeyIn = {"moderateForum", "interactWithTopic"},
            includeExtensionProperties = true,
            variableNames = {"selected"}
    )
    public ExternalTaskHandler getTopics() {
        return new ExternalTaskHandler() {
            @Async
            @SneakyThrows
            @Override
            public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
                topicTaskList.performGetAllByChapter(externalTask, externalTaskService);
            }
        };
    }

    @Bean
    @ExternalTaskSubscription(
            topicName = "approveTopic",
            processDefinitionKey = "addTopic",
            includeExtensionProperties = true,
            variableNames = {"jwtToken", "approvalId"}
    )
    public ExternalTaskHandler approveTopic() {
        return new ExternalTaskHandler() {
            @Async
            @SneakyThrows
            @Override
            public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
                topicTaskList.performApproveTopic(externalTask, externalTaskService);
            }
        };
    }

    @Bean
    @ExternalTaskSubscription(
            topicName = "deleteTopic",
            processDefinitionKeyIn = {"moderateForum", "addTopic"},
            includeExtensionProperties = true,
            variableNames = {"jwtToken", "topicId"}
    )
    public ExternalTaskHandler deleteTopic() {
        return new ExternalTaskHandler() {
            @Async
            @SneakyThrows
            @Override
            public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
                topicTaskList.performDeleteTopic(externalTask, externalTaskService);
            }
        };
    }

    @Bean
    @ExternalTaskSubscription(
            topicName = "addTopic",
            processDefinitionKey = "addTopic",
            includeExtensionProperties = true,
            variableNames = {"jwtToken", "selected", "topicTitle", "topicText", "img1", "img2", "img3"}
    )
    public ExternalTaskHandler addTopic() {
        return new ExternalTaskHandler() {
            @Async
            @SneakyThrows
            @Override
            public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
                topicTaskList.performAddTopic(externalTask, externalTaskService);
            }
        };
    }

}
