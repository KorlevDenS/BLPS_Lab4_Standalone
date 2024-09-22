package org.korolev.dens.blps_lab4_standalone.entites;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.Hibernate;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

@Getter
@Setter
@Embeddable
public class PermissionId implements Serializable {
    @Serial
    private static final long serialVersionUID = 259339961437135705L;
    @NotNull
    @Column(name = "client", nullable = false)
    private Integer client;

    @NotNull
    @Column(name = "role", nullable = false)
    private Integer role;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        PermissionId entity = (PermissionId) o;
        return Objects.equals(this.role, entity.role) &&
                Objects.equals(this.client, entity.client);
    }

    @Override
    public int hashCode() {
        return Objects.hash(role, client);
    }

}