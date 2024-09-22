package org.korolev.dens.ratingservice.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Getter
@Setter
@Entity
@Table(name = "topic")
public class Topic {
    @Id
    @Column(name = "id", nullable = false)
    private Integer id;

    @ColumnDefault("0")
    @Column(name = "views")
    private Integer views;

    @ColumnDefault("0")
    @Column(name = "temporal_views")
    private Integer temporal_views;

    @ColumnDefault("0")
    @Column(name = "temporal_comments")
    private Integer temporal_comments;

    @ColumnDefault("0.0")
    @Column(name = "temporal_fame")
    private Double temporal_fame;

    @ColumnDefault("0.0")
    @Column(name = "fame")
    private Double fame;

    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "owner")
    private Client owner;

    @Override
    public String toString() {
        return "Topic{" +
                "id=" + id +
                ", views=" + views +
                ", temporal_views=" + temporal_views +
                ", temporal_comments=" + temporal_comments +
                ", temporal_fame=" + temporal_fame +
                ", fame=" + fame +
                '}';
    }

}