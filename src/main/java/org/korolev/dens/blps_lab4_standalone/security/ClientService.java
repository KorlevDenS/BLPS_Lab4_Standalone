package org.korolev.dens.blps_lab4_standalone.security;

import org.korolev.dens.blps_lab4_standalone.entites.Client;
import org.korolev.dens.blps_lab4_standalone.entites.Permission;
import org.korolev.dens.blps_lab4_standalone.entites.PermissionId;
import org.korolev.dens.blps_lab4_standalone.entites.Role;
import org.korolev.dens.blps_lab4_standalone.repositories.ClientRepository;
import org.korolev.dens.blps_lab4_standalone.repositories.PermissionRepository;
import org.korolev.dens.blps_lab4_standalone.repositories.RoleRepository;
import org.korolev.dens.blps_lab4_standalone.responces.JwtAuthenticationResponse;
import org.korolev.dens.blps_lab4_standalone.services.JwtTokenService;
import org.korolev.dens.blps_lab4_standalone.services.PasswordService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ClientService implements UserDetailsService {

    private final PasswordService passwordService;
    private final JwtTokenService jwtTokenService;
    private final ClientRepository clientRepository;
    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;

    public ClientService(PasswordService passwordService, ClientRepository clientRepository,
                         JwtTokenService jwtTokenService, PermissionRepository permissionRepository,
                         RoleRepository roleRepository) {
        this.jwtTokenService = jwtTokenService;
        this.passwordService = passwordService;
        this.clientRepository = clientRepository;
        this.permissionRepository = permissionRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<Client> clientDetail = clientRepository.findByLogin(username);
        return clientDetail.map(ClientDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("User with login " + username + " not found"));
    }


    public ResponseEntity<?> addClient(Client client) {
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
        String jwt = jwtTokenService.generateToken(savedClient.getLogin());
        return ResponseEntity.ok(new JwtAuthenticationResponse(jwt));
    }
}
