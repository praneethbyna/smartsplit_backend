package com.smartsplit.backend.model;


import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Data
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private Boolean isVerified = false;

    @Column(nullable = true)
    private String verificationToken;

    @Column(nullable = true)
    private LocalDateTime accountVerifyTokenExpirationTime;

    @Column(nullable = true)
    private LocalDateTime passwordResetTokenExpirationTime;

    @ManyToMany(mappedBy = "members")
    @JsonBackReference
    private Set<Group> groups = new HashSet<>();

    @OneToMany(mappedBy = "admin", cascade = CascadeType.ALL)
    @JsonManagedReference
    private Set<Group> adminGroups = new HashSet<>();

    public void addGroup(Group group) {
        this.groups.add(group);
        group.getMembers().add(this); // Maintain the inverse side
    }

    public void removeGroup(Group group) {
        this.groups.remove(group);
        group.getMembers().remove(this); // Maintain the inverse side
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return id != null && id.equals(user.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }




}
