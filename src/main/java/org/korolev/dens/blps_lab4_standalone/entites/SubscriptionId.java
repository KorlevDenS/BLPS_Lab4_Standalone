package org.korolev.dens.blps_lab4_standalone.entites;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.Hibernate;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

@Getter
@Setter
@Embeddable
public class SubscriptionId implements Serializable {
    @Serial
    private static final long serialVersionUID = -7534941229579610967L;
    @Column(name = "client", nullable = false)
    private Integer client;

    @Column(name = "topic", nullable = false)
    private Integer topic;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        SubscriptionId entity = (SubscriptionId) o;
        return Objects.equals(this.client, entity.client) &&
                Objects.equals(this.topic, entity.topic);
    }

    @Override
    public int hashCode() {
        return Objects.hash(client, topic);
    }

}