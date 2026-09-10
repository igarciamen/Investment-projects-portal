package com.igarciamen.users.model;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(
        name = "users",
        schema = "public",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "username"),
                @UniqueConstraint(columnNames = "email")
        }
)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String username;

    @Column(nullable = false, length = 100)
    private String email;


    @Column(nullable = false)
    private String password;

    // ---------------- Profile / Organisation fields (all optional) ----------------
    // These do NOT come from signup -- they are filled in later, on a
    // dedicated "My Profile" page. This is the lightweight stand-in for the
    // real InvestEU Portal's Profile/Organisation wizard steps: no separate
    // Organisation entity, just a handful of extra columns on the user's
    // own profile, since nothing in this platform's actual business logic
    // (evaluation, interest, messaging) needs to know about an organisation
    // as its own domain concept.
    @Column(length = 100)
    private String country;

    @Column(length = 150)
    private String occupation;

    @Column(length = 50)
    private String preferredContactLanguage;

    @Column(length = 150)
    private String organisationName;

    @Column(length = 100)
    private String organisationCountry;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_roles",
            schema = "public",
            joinColumns = @JoinColumn(name = "user_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "role_id", referencedColumnName = "id")
    )
    private Set<Role> roles = new HashSet<>();

    public User() {}

    public User(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
    }


    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }

    public String getEmail() { return email; }

    public String getPassword() { return password; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getOccupation() { return occupation; }
    public void setOccupation(String occupation) { this.occupation = occupation; }

    public String getPreferredContactLanguage() { return preferredContactLanguage; }
    public void setPreferredContactLanguage(String preferredContactLanguage) { this.preferredContactLanguage = preferredContactLanguage; }

    public String getOrganisationName() { return organisationName; }
    public void setOrganisationName(String organisationName) { this.organisationName = organisationName; }

    public String getOrganisationCountry() { return organisationCountry; }
    public void setOrganisationCountry(String organisationCountry) { this.organisationCountry = organisationCountry; }

    public Set<Role> getRoles() { return roles; }
    public void setRoles(Set<Role> roles) { this.roles = roles; }
}