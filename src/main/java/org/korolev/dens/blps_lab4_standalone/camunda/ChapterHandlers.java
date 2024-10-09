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
public class ChapterHandlers {

    private final ChapterTaskList chapterTaskList;

    public ChapterHandlers(ChapterTaskList chapterTaskList) {
        this.chapterTaskList = chapterTaskList;
    }

    @Bean
    @ExternalTaskSubscription(
            topicName = "getChapters",
            processDefinitionKey = "deleteChapter",
            includeExtensionProperties = true
    )
    public ExternalTaskHandler getAllChapters() {
        return new ExternalTaskHandler() {
            @Async
            @Override
            public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
                chapterTaskList.performGetAllChapters(externalTask, externalTaskService);
            }
        };
    }

    @Bean
    @ExternalTaskSubscription(
            topicName = "checkChapter",
            processDefinitionKey = "deleteChapter",
            includeExtensionProperties = true
    )
    public ExternalTaskHandler checkForTopics() {
        return new ExternalTaskHandler() {
            @Async
            @SneakyThrows
            @Override
            public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
                chapterTaskList.performCheckForTopics(externalTask, externalTaskService);
            }
        };
    }

    @Bean
    @ExternalTaskSubscription(
            topicName = "deleteChapter",
            processDefinitionKey = "deleteChapter",
            includeExtensionProperties = true
    )
    public ExternalTaskHandler deleteChapter() {
        return new ExternalTaskHandler() {
            @Async
            @SneakyThrows
            @Override
            public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
                chapterTaskList.performDelete(externalTask, externalTaskService);
            }
        };
    }

}
