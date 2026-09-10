package com.igarciamen.users.payloads.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class SignupRequest {
    @NotBlank
    private String username;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String password;

    // User type chosen at signup: PROMOTER or INVESTOR.
    // ROLE_ADMIN is never assigned from public signup (only via DataLoader).
    @NotBlank
    @Pattern(regexp = "PROMOTER|INVESTOR", message = "userType must be PROMOTER or INVESTOR")
    private String userType;

    public SignupRequest() {}

    public SignupRequest(String username, String email, String password, String userType) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.userType = userType;
    }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getUserType() { return userType; }

}
