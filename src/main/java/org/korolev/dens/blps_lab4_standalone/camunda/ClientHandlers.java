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
public class ClientHandlers {

    private final ClientTaskList clientTaskList;

    public ClientHandlers(ClientTaskList clientTaskList) {
        this.clientTaskList = clientTaskList;
    }

    @Bean
    @ExternalTaskSubscription(
            topicName = "giveRole",
            processDefinitionKey = "manageRoles",
            includeExtensionProperties = true,
            variableNames = {"jwtToken", "selectedRole", "selectedClient"}
    )
    public ExternalTaskHandler giveRole() {
        return new ExternalTaskHandler() {
            @Async
            @SneakyThrows
            @Override
            public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
                clientTaskList.performGiveRole(externalTask, externalTaskService);
            }
        };
    }

    @Bean
    @ExternalTaskSubscription(
            topicName = "stripRole",
            processDefinitionKey = "manageRoles",
            includeExtensionProperties = true,
            variableNames = {"jwtToken", "selectedRole", "selectedClient"}
    )
    public ExternalTaskHandler stripRole() {
        return new ExternalTaskHandler() {
            @Async
            @SneakyThrows
            @Override
            public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
                clientTaskList.performStripRole(externalTask, externalTaskService);
            }
        };
    }

    @Bean
    @ExternalTaskSubscription(
            topicName = "getClients",
            processDefinitionKey = "manageRoles",
            includeExtensionProperties = true,
            variableNames = {"jwtToken"}
    )
    public ExternalTaskHandler getClients() {
        return new ExternalTaskHandler() {
            @Async
            @Override
            public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
                clientTaskList.performGetUsers(externalTask, externalTaskService);
            }
        };
    }

    @Bean
    @ExternalTaskSubscription(
            topicName = "getRoles",
            processDefinitionKey = "manageRoles",
            includeExtensionProperties = true,
            variableNames = {"jwtToken"}
    )
    public ExternalTaskHandler getRoles() {
        return new ExternalTaskHandler() {
            @Async
            @Override
            public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
                clientTaskList.performGetRoles(externalTask, externalTaskService);
            }
        };
    }

    @Bean
    @ExternalTaskSubscription(
            topicName = "loginUser",
            processDefinitionKey = "getToken",
            includeExtensionProperties = true,
            variableNames = {"login", "password"}
    )
    public ExternalTaskHandler loginUser() {
        return clientTaskList::performLoginUser;
    }

    @Bean
    @ExternalTaskSubscription(
            topicName = "registerUser",
            processDefinitionKey = "getToken",
            includeExtensionProperties = true,
            variableNames = {"login", "password", "email", "sex", "birthday"}
    )
    public ExternalTaskHandler registerUser() {
        return clientTaskList::performRegisterUser;
    }

}
