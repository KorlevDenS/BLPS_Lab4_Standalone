package org.korolev.dens.blps_lab4_standalone.camunda;

import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTaskHandler;
import org.camunda.bpm.engine.variable.VariableMap;
import org.camunda.bpm.engine.variable.Variables;
import org.korolev.dens.blps_lab4_standalone.entites.Client;
import org.korolev.dens.blps_lab4_standalone.services.ClientService;
import org.korolev.dens.blps_lab4_standalone.services.JwtTokenService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;

import java.time.LocalDate;


@Configuration
public class AuthHandlers {

    @Bean
    @ExternalTaskSubscription(
            topicName = "loginUser",
            processDefinitionKey = "getToken",
            includeExtensionProperties = true,
            variableNames = {"login", "password"}
    )
    public ExternalTaskHandler loginUser(AuthenticationManager authenticationManager, JwtTokenService jwtTokenService) {
        return (externalTask, externalTaskService) -> {
            String login = externalTask.getVariable("login");
            String password = externalTask.getVariable("password");
            try {
                authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(login, password));
            } catch (AuthenticationException e) {
                externalTaskService.handleBpmnError(externalTask, HttpStatus.FORBIDDEN.toString(),
                        "Wrong login or password");
                return;
            }
            VariableMap variableMap = Variables.createVariables();
            variableMap.put("jwt", jwtTokenService.generateToken(login));
            externalTaskService.complete(externalTask, variableMap);
        };
    }

    @Bean
    @ExternalTaskSubscription(
            topicName = "registerUser",
            processDefinitionKey = "getToken",
            includeExtensionProperties = true,
            variableNames = {"login", "password", "email", "sex", "birthday"}
    )
    public ExternalTaskHandler registerUser(ClientService clientService, JwtTokenService jwtTokenService) {
        return (externalTask, externalTaskService) -> {
            try {
                Client client = new Client();
                client.setEmail(externalTask.getVariable("email"));
                client.setPassword(externalTask.getVariable("password"));
                client.setLogin(externalTask.getVariable("login"));
                client.setBirthday(LocalDate.parse(externalTask.getVariable("birthday")));
                client.setRegistered(LocalDate.now());
                client.setSex(externalTask.getVariable("sex"));
                Client savedClient = clientService.addClient(client);
                VariableMap variableMap = Variables.createVariables();
                variableMap.put("jwt", jwtTokenService.generateToken(savedClient.getLogin()));
                externalTaskService.complete(externalTask, variableMap);
            } catch (Exception e) {
                externalTaskService.handleBpmnError(externalTask, "reg-failed", e.getMessage());
            }
        };
    }

}
