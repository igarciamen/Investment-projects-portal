package com.igarciamen.users.payloads.request;

import jakarta.validation.constraints.Size;

public class UpdateProfileRequest {

    @Size(max = 100)
    private String country;

    @Size(max = 150)
    private String occupation;

    @Size(max = 50)
    private String preferredContactLanguage;

    @Size(max = 150)
    private String organisationName;

    @Size(max = 100)
    private String organisationCountry;

    public UpdateProfileRequest() {}

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
}