package org.korolev.dens.blps_lab4_standalone.services;

public interface PasswordService {

    String makeBCryptHash(String password);

    boolean checkIdentity(String rawPassword, String encodedPassword);

}
