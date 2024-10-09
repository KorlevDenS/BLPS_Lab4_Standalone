package org.korolev.dens.blps_lab4_standalone.entites;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "chapter")
@EntityListeners(AuditingEntityListener.class)
public class Chapter {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ColumnDefault("nextval('chapter_id_seq'")
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "title", nullable = false, length = Integer.MAX_VALUE)
    private String title;

    @ColumnDefault("")
    @Column(name = "description", nullable = false, length = Integer.MAX_VALUE)
    private String description;

    @ManyToOne
    @JoinColumn(name = "creator")
    private Client creator;

    @CreatedDate
    @JsonIgnore //TODO
    @Column(name = "created")
    private LocalDate created;

    @JsonIgnore
    @OneToMany(mappedBy = "chapter", fetch = FetchType.EAGER)
    private Set<Topic> topics = new LinkedHashSet<>();

}