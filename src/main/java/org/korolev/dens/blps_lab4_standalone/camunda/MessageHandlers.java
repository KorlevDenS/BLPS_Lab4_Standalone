package org.korolev.dens.blps_lab4_standalone.camunda;

import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTaskHandler;
import org.korolev.dens.blps_lab4_standalone.requests.StatsMessage;
import org.korolev.dens.blps_lab4_standalone.services.MessageProducer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MessageHandlers {

    @Bean
    @ExternalTaskSubscription(
            topicName = "sendMessage",
            processDefinitionKeyIn = {"interactWithTopic", "addTopic", "moderateForum"},
            includeExtensionProperties = true,
            variableNames = {"message"}
    )
    public ExternalTaskHandler handleMessage(MessageProducer messageProducer) {
        return (externalTask, externalTaskService) -> {
            StatsMessage statsMessage = externalTask.getVariable("message");
            messageProducer.sendMessage(statsMessage);
            externalTaskService.complete(externalTask);
        };
    }

}
