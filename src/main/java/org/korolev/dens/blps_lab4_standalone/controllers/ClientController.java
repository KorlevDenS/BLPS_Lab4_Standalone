package org.korolev.dens.blps_lab4_standalone.controllers;

import org.korolev.dens.blps_lab4_standalone.entites.*;
import org.korolev.dens.blps_lab4_standalone.repositories.ClientRepository;
import org.korolev.dens.blps_lab4_standalone.repositories.NotificationRepository;
import org.korolev.dens.blps_lab4_standalone.repositories.PermissionRepository;
import org.korolev.dens.blps_lab4_standalone.repositories.RoleRepository;
import org.korolev.dens.blps_lab4_standalone.requests.ClientLoginRequest;
import org.korolev.dens.blps_lab4_standalone.responces.JwtAuthenticationResponse;
import org.korolev.dens.blps_lab4_standalone.security.ClientService;
import org.korolev.dens.blps_lab4_standalone.services.JwtTokenService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/client")
public class ClientController {

    private final AuthenticationManager authenticationManager;
    private final ClientRepository clientRepository;
    private final JwtTokenService jwtTokenService;
    private final NotificationRepository notificationRepository;

    private final ClientService clientService;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    public ClientController(AuthenticationManager authenticationManager,
                            ClientRepository clientRepository, JwtTokenService jwtTokenService,
                            NotificationRepository notificationRepository, ClientService clientService,
                            RoleRepository roleRepository, PermissionRepository permissionRepository) {
        this.clientService = clientService;
        this.clientRepository = clientRepository;
        this.jwtTokenService = jwtTokenService;
        this.notificationRepository = notificationRepository;
        this.authenticationManager = authenticationManager;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerClientN(@RequestBody @Validated(Client.New.class) Client client) {
        return clientService.addClient(client);
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginClient(@RequestBody ClientLoginRequest clientLoginRequest) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                clientLoginRequest.login(), clientLoginRequest.password()
        ));
        return ResponseEntity
                .ok(new JwtAuthenticationResponse(jwtTokenService.generateToken(clientLoginRequest.login())));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/get/roles")
    public List<Role> getRoles() {
        return roleRepository.findAll();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/give/role/{clientLogin}/{roleId}")
    public ResponseEntity<?> giveRole(@PathVariable String clientLogin, @PathVariable Integer roleId) {
        Optional<Client> client = clientRepository.findByLogin(clientLogin);
        if (client.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Client not found");
        }
        Optional<Role> role = roleRepository.findById(roleId);
        if (role.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Role not found");
        }
        if (client.get().getRoles().stream().map(Role::getId).toList().contains(roleId)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Client already has this role");
        }
        PermissionId permissionId = new PermissionId();
        permissionId.setClient(client.get().getId());
        permissionId.setRole(roleId);
        Permission permission = new Permission();
        permission.setId(permissionId);
        permission.setClient(client.get());
        permission.setRole(role.get());
        Permission savedPermission = permissionRepository.save(permission);
        return ResponseEntity.ok(savedPermission);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/strip/role/{clientLogin}/{roleId}")
    public ResponseEntity<?> stripRole(@PathVariable String clientLogin, @PathVariable Integer roleId) {
        Optional<Role> role = roleRepository.findById(roleId);
        if (role.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Role not found");
        }
        Optional<Client> client = clientRepository.findByLogin(clientLogin);
        if (client.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Client not found");
        }
        if (!client.get().getRoles().stream().map(Role::getId).toList().contains(roleId)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("User does not have this role ");
        }
        PermissionId permissionId = new PermissionId();
        permissionId.setClient(client.get().getId());
        permissionId.setRole(role.get().getId());
        permissionRepository.deleteById(permissionId);
        return ResponseEntity
                .ok("Role " + role.get().getName() + " deleted from client " + clientLogin + " successfully");
    }

    @PreAuthorize("hasRole('MODER')")
    @GetMapping("/get/clients")
    public List<Client> getClients() {
        return clientRepository.findAll();
    }

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/get/notifications")
    public ResponseEntity<?> getClientNotifications(@AuthenticationPrincipal UserDetails userDetails) {
        Optional<Client> optionalClient = clientRepository.findByLogin(userDetails.getUsername());
        if (optionalClient.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Пользователь не авторизован");
        } else {
            List<Notification> notifications = notificationRepository.findAllByRecipient(optionalClient.get());
            return ResponseEntity.ok(notifications);
        }
    }

}
