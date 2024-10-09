package org.korolev.dens.blps_lab4_standalone.services;

import org.korolev.dens.blps_lab4_standalone.entites.*;
import org.korolev.dens.blps_lab4_standalone.exceptions.ForbiddenActionException;
import org.korolev.dens.blps_lab4_standalone.exceptions.ForumLogicException;
import org.korolev.dens.blps_lab4_standalone.exceptions.ForumObjectNotFoundException;
import org.korolev.dens.blps_lab4_standalone.repositories.ClientRepository;
import org.korolev.dens.blps_lab4_standalone.repositories.NotificationRepository;
import org.korolev.dens.blps_lab4_standalone.repositories.PermissionRepository;
import org.korolev.dens.blps_lab4_standalone.repositories.RoleRepository;
import org.korolev.dens.blps_lab4_standalone.security.ClientDetails;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ClientService implements UserDetailsService {

    private final PasswordService passwordService;
    private final ClientRepository clientRepository;
    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final NotificationRepository notificationRepository;

    public ClientService(PasswordService passwordService, ClientRepository clientRepository,
                         PermissionRepository permissionRepository,
                         RoleRepository roleRepository, NotificationRepository notificationRepository) {
        this.passwordService = passwordService;
        this.clientRepository = clientRepository;
        this.permissionRepository = permissionRepository;
        this.roleRepository = roleRepository;
        this.notificationRepository = notificationRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<Client> clientDetail = clientRepository.findByLogin(username);
        return clientDetail.map(ClientDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("User with login " + username + " not found"));
    }

    public Client addClient(Client client) {
        client.setPassword(passwordService.makeBCryptHash(client.getPassword()));
        Client savedClient = clientRepository.save(client);
        Role userRole = roleRepository.findByName("ROLE_USER");
        Permission permission = new Permission();
        PermissionId permissionId = new PermissionId();
        permissionId.setRole(userRole.getId());
        permissionId.setClient(savedClient.getId());
        permission.setId(permissionId);
        permission.setRole(userRole);
        permission.setClient(savedClient);
        permissionRepository.save(permission);
        return savedClient;
    }

    @PreAuthorize("hasRole('ADMIN')")
    public List<Role> findAllRoles() {
        return roleRepository.findAll();
    }

    @PreAuthorize("hasRole('MODER')")
    public List<Client> findAllClients() {
        return clientRepository.findAll();
    }

    @PreAuthorize("hasRole('USER')")
    public List<Notification> findNotifications() throws ForbiddenActionException {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Optional<Client> optionalClient = clientRepository.findByLogin(userDetails.getUsername());
        if (optionalClient.isEmpty()) {
            throw new ForbiddenActionException("Illegal access to resource: no such client in database");
        } else {
            return notificationRepository.findAllByRecipient(optionalClient.get());
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    public Permission addPermission(String clientLogin, Integer roleId) throws ForumObjectNotFoundException,
            ForumLogicException {
        Optional<Client> client = clientRepository.findByLogin(clientLogin);
        if (client.isEmpty()) {
            throw new ForumObjectNotFoundException("Impossible to give role: Client " + clientLogin + " not found");
        }
        Optional<Role> role = roleRepository.findById(roleId);
        if (role.isEmpty()) {
            throw new ForumObjectNotFoundException("Impossible to give role: Role " + roleId + " not found");
        }
        if (client.get().getRoles().stream().map(Role::getId).toList().contains(roleId)) {
            throw new ForumLogicException("Client " + clientLogin + " already has role " + role.get().getName(),
                    HttpStatus.CONFLICT);
        }
        PermissionId permissionId = new PermissionId();
        permissionId.setClient(client.get().getId());
        permissionId.setRole(roleId);
        Permission permission = new Permission();
        permission.setId(permissionId);
        permission.setClient(client.get());
        permission.setRole(role.get());
        return permissionRepository.save(permission);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public void deletePermission(String clientLogin, Integer roleId) throws ForumObjectNotFoundException,
            ForumLogicException {
        Optional<Role> role = roleRepository.findById(roleId);
        if (role.isEmpty()) {
            throw new ForumObjectNotFoundException("Impossible to strip role: Role " + roleId + " not found");
        }
        Optional<Client> client = clientRepository.findByLogin(clientLogin);
        if (client.isEmpty()) {
            throw new ForumObjectNotFoundException("Impossible to strip role: Client " + clientLogin + " not found");
        }
        if (!client.get().getRoles().stream().map(Role::getId).toList().contains(roleId)) {
            throw new ForumLogicException("Client " + clientLogin + " does not have role " + role.get().getName(),
                    HttpStatus.CONFLICT);
        }
        PermissionId permissionId = new PermissionId();
        permissionId.setClient(client.get().getId());
        permissionId.setRole(role.get().getId());
        permissionRepository.deleteById(permissionId);
    }

}
