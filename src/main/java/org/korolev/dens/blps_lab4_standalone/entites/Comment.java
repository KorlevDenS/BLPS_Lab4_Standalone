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
@Table(name = "comment")
@EntityListeners(AuditingEntityListener.class)
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ColumnDefault("nextval('comment_id_seq'")
    @Column(name = "id", nullable = false)
    private Integer id;

    @ColumnDefault("")
    @Column(name = "text", nullable = false, length = Integer.MAX_VALUE)
    private String text;

    @ManyToOne
    @JoinColumn(name = "commentator")
    private Client commentator;

    @CreatedDate
    @Column(name = "created", nullable = false)
    private LocalDate created;

    @ManyToOne
    @JoinColumn(name = "quote")
    private Comment quote;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic")
    private Topic topic;

    @JsonIgnore
    @OneToMany(mappedBy = "quote")
    private Set<Comment> comments = new LinkedHashSet<>();

}