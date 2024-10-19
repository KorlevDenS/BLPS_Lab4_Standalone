package org.korolev.dens.ratingservice.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "client_stats")
public class Client {
    @Id
    @Column(name = "login", nullable = false, length = Integer.MAX_VALUE)
    private String login;

    @ColumnDefault("1")
    @Column(name = "rating")
    private Double rating;

    @ColumnDefault("1")
    @Column(name = "activity")
    private Integer activity;

    @JsonIgnore
    @OneToMany(mappedBy = "owner")
    private Set<Topic> topics = new LinkedHashSet<>();

}