package org.korolev.dens.blps_lab4_standalone.entites;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.korolev.dens.blps_lab4_standalone.security.ClientPassword;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "client")
@EntityListeners(AuditingEntityListener.class)
public class Client {
    @JsonIgnore
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ColumnDefault("nextval('client_id_seq'")
    @Column(name = "id", nullable = false)
    private Integer id;

    @NotBlank
    @Column(name = "login", nullable = false, length = Integer.MAX_VALUE)
    private String login;

    @ClientPassword(groups = {New.class})
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(name = "password", nullable = false, length = Integer.MAX_VALUE)
    private String password;

    @Email(message = "Невалидный email")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(name = "email", nullable = false, length = Integer.MAX_VALUE)
    private String email;

    @ColumnDefault("нет")
    @Column(name = "sex", length = 3)
    private String sex;

    @JsonIgnore //TODO
    @CreatedDate
    @Column(name = "registered", nullable = false)
    private LocalDate registered;

    @JsonIgnore //TODO
    @Past(message = "День рождения должен быть в прошлом")
    @Column(name = "birthday", nullable = false)
    private LocalDate birthday;

    @JsonIgnore
    @OneToMany(mappedBy = "creator")
    private Set<Chapter> chapters = new LinkedHashSet<>();

    @JsonIgnore
    @OneToMany(mappedBy = "commentator")
    private Set<Comment> comments = new LinkedHashSet<>();

    @JsonIgnore
    @OneToMany(mappedBy = "recipient")
    private Set<Notification> gotNotifications = new LinkedHashSet<>();

    @JsonIgnore
    @OneToMany(mappedBy = "initiator")
    private Set<Notification> sentNotifications = new LinkedHashSet<>();

    @JsonIgnore
    @OneToMany(mappedBy = "creator")
    private Set<Rating> ratings = new LinkedHashSet<>();

    @JsonIgnore
    @OneToMany(mappedBy = "owner")
    private Set<Topic> topics = new LinkedHashSet<>();

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "permission",
            joinColumns = @JoinColumn(name = "client"),
            inverseJoinColumns = @JoinColumn(name = "role"))
    private Set<Role> roles = new LinkedHashSet<>();

    public interface New {
    }

}