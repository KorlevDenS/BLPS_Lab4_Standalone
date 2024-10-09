package org.korolev.dens.blps_lab4_standalone.controllers;

import org.korolev.dens.blps_lab4_standalone.entites.*;
import org.korolev.dens.blps_lab4_standalone.exceptions.ForbiddenActionException;
import org.korolev.dens.blps_lab4_standalone.exceptions.ForumLogicException;
import org.korolev.dens.blps_lab4_standalone.exceptions.ForumObjectNotFoundException;
import org.korolev.dens.blps_lab4_standalone.requests.ClientLoginRequest;
import org.korolev.dens.blps_lab4_standalone.responces.JwtAuthenticationResponse;
import org.korolev.dens.blps_lab4_standalone.services.ClientService;
import org.korolev.dens.blps_lab4_standalone.services.JwtTokenService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/client")
public class ClientController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;
    private final ClientService clientService;

    public ClientController(AuthenticationManager authenticationManager, JwtTokenService jwtTokenService,
                            ClientService clientService) {
        this.clientService = clientService;
        this.jwtTokenService = jwtTokenService;
        this.authenticationManager = authenticationManager;

    }

    @PostMapping("/register")
    public ResponseEntity<?> registerClientN(@RequestBody @Validated(Client.New.class) Client client) {
        Client savedClient = clientService.addClient(client);
        String jwt = jwtTokenService.generateToken(savedClient.getLogin());
        return ResponseEntity.ok(new JwtAuthenticationResponse(jwt));
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginClient(@RequestBody ClientLoginRequest clientLoginRequest) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                clientLoginRequest.login(), clientLoginRequest.password()
        ));
        return ResponseEntity
                .ok(new JwtAuthenticationResponse(jwtTokenService.generateToken(clientLoginRequest.login())));
    }

    @GetMapping("/get/roles")
    public List<Role> getRoles() {
        return clientService.findAllRoles();
    }

    @PutMapping("/give/role/{clientLogin}/{roleId}")
    public ResponseEntity<?> giveRole(@PathVariable String clientLogin, @PathVariable Integer roleId)
            throws ForumLogicException, ForumObjectNotFoundException {
        Permission savedPermission = clientService.addPermission(clientLogin, roleId);
        return ResponseEntity.ok(savedPermission);
    }

    @DeleteMapping("/strip/role/{clientLogin}/{roleId}")
    public ResponseEntity<?> stripRole(@PathVariable String clientLogin, @PathVariable Integer roleId)
            throws ForumLogicException, ForumObjectNotFoundException {
        clientService.deletePermission(clientLogin, roleId);
        return ResponseEntity
                .ok("Role " + roleId + " deleted from client " + clientLogin + " successfully");
    }

    @GetMapping("/get/clients")
    public List<Client> getClients() {
        return clientService.findAllClients();
    }

    @GetMapping("/get/notifications")
    public ResponseEntity<?> getClientNotifications() throws ForbiddenActionException {
        return ResponseEntity.ok(clientService.findNotifications());
    }

}
