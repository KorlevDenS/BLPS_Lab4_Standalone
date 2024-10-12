package org.korolev.dens.blps_lab4_standalone.entites;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "topic")
@EntityListeners(AuditingEntityListener.class)
public class Topic {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ColumnDefault("nextval('topic_id_seq'")
    @Column(name = "id", nullable = false)
    private Integer id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "chapter", nullable = false)
    private Chapter chapter;

    @Column(name = "title", nullable = false, length = Integer.MAX_VALUE)
    private String title;

    @Column(name = "text", nullable = false, length = Integer.MAX_VALUE)
    private String text;

    @ManyToOne
    @JoinColumn(name = "owner")
    private Client owner;

    @JsonIgnore
    @OneToOne(mappedBy = "topic", fetch = FetchType.EAGER)
    private Approval approval;

    @JsonIgnore //TODO
    @CreatedDate
    @Column(name = "created", nullable = false)
    private LocalDate created;

    @JsonIgnore
    @OneToMany(mappedBy = "topic")
    private Set<Comment> comments = new LinkedHashSet<>();

    @JsonIgnore
    @OneToMany(mappedBy = "topic")
    private Set<Notification> notifications = new LinkedHashSet<>();

    @JsonIgnore
    @OneToMany(mappedBy = "topic")
    private Set<Rating> ratings = new LinkedHashSet<>();

    @OneToMany(mappedBy = "topic")
    private Set<Image> images = new LinkedHashSet<>();

}