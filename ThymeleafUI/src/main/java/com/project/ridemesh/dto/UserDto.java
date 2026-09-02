package com.project.ridemesh.dto;

import java.util.Set;

public class UserDto {
    private Long id;
    private String name;
    private String email;
    private Set<String> roles;
    private Boolean emailVerified;

    public UserDto() {
    }

    public UserDto(Long id, String name, String email, Set<String> roles, Boolean emailVerified) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.roles = roles;
        this.emailVerified = emailVerified;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    public Boolean getEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(Boolean emailVerified) {
        this.emailVerified = emailVerified;
    }
}
