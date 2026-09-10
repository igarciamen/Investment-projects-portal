package com.igarciamen.users.payloads.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;

public class LoginRequest {
    @NotBlank
    @JsonAlias({"username", "email", "login"})
    private String login;

    @NotBlank
    private String password;

    public LoginRequest() {}

    public LoginRequest(String login, String password) {
        this.login = login;
        this.password = password;
    }

    public String getLogin() { return login; }
    public String getPassword() { return password; }
}
