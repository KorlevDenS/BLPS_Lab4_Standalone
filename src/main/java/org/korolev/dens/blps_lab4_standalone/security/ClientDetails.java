package org.korolev.dens.blps_lab4_standalone.security;

import org.korolev.dens.blps_lab4_standalone.entites.Client;
import org.korolev.dens.blps_lab4_standalone.entites.Role;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class ClientDetails implements UserDetails {

    private final String clientName;
    private final String clientPassword;
    private final List<GrantedAuthority> authorities;

    public ClientDetails(Client client) {
        this.clientName = client.getLogin();
        this.clientPassword = client.getPassword();
        this.authorities = client.getRoles().stream().map(Role::getName)
                .map(SimpleGrantedAuthority::new).collect(Collectors.toList());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.authorities;
    }

    @Override
    public String getPassword() {
        return this.clientPassword;
    }

    @Override
    public String getUsername() {
        return this.clientName;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
