package com.igarciamen.projects.payloads.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class CreateProjectRequest {

    @NotNull
    private Long sectorId;

    @NotBlank
    @Size(max = 120)
    private String title;

    @Size(max = 2000)
    private String description;

    @NotBlank
    @Size(max = 100)
    private String country;

    @NotNull
    @DecimalMin(value = "0.01", message = "requestedAmount must be greater than 0")
    private BigDecimal requestedAmount;

    public CreateProjectRequest() {}

    public Long getSectorId() { return sectorId; }
    public void setSectorId(Long sectorId) { this.sectorId = sectorId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public BigDecimal getRequestedAmount() { return requestedAmount; }
    public void setRequestedAmount(BigDecimal requestedAmount) { this.requestedAmount = requestedAmount; }
}
