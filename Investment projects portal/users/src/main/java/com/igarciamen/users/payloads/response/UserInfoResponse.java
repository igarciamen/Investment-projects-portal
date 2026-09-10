package com.igarciamen.users.payloads.response;

import java.util.Set;

public class UserInfoResponse {
    private Long id;
    private String username;
    private String email;
    private Set<String> roles;
    private String country;
    private String occupation;
    private String preferredContactLanguage;
    private String organisationName;
    private String organisationCountry;

    public UserInfoResponse(Long id, String username, String email, Set<String> roles,
                            String country, String occupation, String preferredContactLanguage,
                            String organisationName, String organisationCountry) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.roles = roles;
        this.country = country;
        this.occupation = occupation;
        this.preferredContactLanguage = preferredContactLanguage;
        this.organisationName = organisationName;
        this.organisationCountry = organisationCountry;
    }
    public Long getId() {
        return id;
    }
    public String getUsername() {
        return username;
    }
    public String getEmail() {
        return email;
    }
    public Set<String> getRoles() {
        return roles;
    }
    public String getCountry() { return country; }
    public String getOccupation() { return occupation; }
    public String getPreferredContactLanguage() { return preferredContactLanguage; }
    public String getOrganisationName() { return organisationName; }
    public String getOrganisationCountry() { return organisationCountry; }
}