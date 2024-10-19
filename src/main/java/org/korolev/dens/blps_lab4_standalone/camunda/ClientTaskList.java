package org.korolev.dens.blps_lab4_standalone.camunda;

import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.camunda.bpm.engine.variable.VariableMap;
import org.camunda.bpm.engine.variable.Variables;
import org.korolev.dens.blps_lab4_standalone.aspects.Authorize;
import org.korolev.dens.blps_lab4_standalone.aspects.ConfusionReport;
import org.korolev.dens.blps_lab4_standalone.entites.Client;
import org.korolev.dens.blps_lab4_standalone.entites.Notification;
import org.korolev.dens.blps_lab4_standalone.entites.Role;
import org.korolev.dens.blps_lab4_standalone.exceptions.ForbiddenActionException;
import org.korolev.dens.blps_lab4_standalone.exceptions.ForumLogicException;
import org.korolev.dens.blps_lab4_standalone.exceptions.ForumObjectNotFoundException;
import org.korolev.dens.blps_lab4_standalone.services.ClientService;
import org.korolev.dens.blps_lab4_standalone.services.JwtTokenService;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class ClientTaskList {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;
    private final ClientService clientService;

    public ClientTaskList(AuthenticationManager authenticationManager, JwtTokenService jwtTokenService,
                          ClientService clientService) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenService = jwtTokenService;
        this.clientService = clientService;
    }

    @Authorize
    @ConfusionReport
    public void performGetNotifications(ExternalTask externalTask, ExternalTaskService externalTaskService)
            throws ForbiddenActionException {
        List<Notification> notifications = clientService.findNotifications();
        List<String> notificationsTexts = notifications.stream().map(Notification::getDescription).toList();
        VariableMap variableMap = Variables.createVariables();
        variableMap.put("notifications", notificationsTexts);
        externalTaskService.complete(externalTask, variableMap);
    }

    @Authorize
    @ConfusionReport
    public void performGiveRole(ExternalTask externalTask, ExternalTaskService externalTaskService)
            throws ForumLogicException, ForumObjectNotFoundException {
        String clientLogin = externalTask.getVariable("selectedClient");
        Integer roleId = externalTask.getVariable("selectedRole");
        clientService.addPermission(clientLogin, roleId);
        VariableMap variableMap = Variables.createVariables();
        variableMap.put("result", "Role " + roleId + " is given to " + clientLogin);
        externalTaskService.complete(externalTask, variableMap);
    }

    @Authorize
    @ConfusionReport
    public void performStripRole(ExternalTask externalTask, ExternalTaskService externalTaskService)
            throws ForumLogicException, ForumObjectNotFoundException {
        String clientLogin = externalTask.getVariable("selectedClient");
        Integer roleId = externalTask.getVariable("selectedRole");
        clientService.deletePermission(clientLogin, roleId);
        VariableMap variableMap = Variables.createVariables();
        variableMap.put("result", "Role " + roleId + " is striped from " + clientLogin);
        externalTaskService.complete(externalTask, variableMap);
    }

    @Authorize
    public void performGetUsers(ExternalTask externalTask, ExternalTaskService externalTaskService) {
        List<Client> clients = clientService.findAllClients();
        VariableMap variableMap = Variables.createVariables();
        variableMap.put("clients", clients);
        externalTaskService.complete(externalTask, variableMap);
    }

    @Authorize
    public void performGetRoles(ExternalTask externalTask, ExternalTaskService externalTaskService) {
        List<Role> roles = clientService.findAllRoles();
        VariableMap variableMap = Variables.createVariables();
        variableMap.put("roles", roles);
        externalTaskService.complete(externalTask, variableMap);
    }

    public void performLoginUser(ExternalTask externalTask, ExternalTaskService externalTaskService) {
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
    }

    public void performRegisterUser(ExternalTask externalTask, ExternalTaskService externalTaskService) {
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
    }

}
