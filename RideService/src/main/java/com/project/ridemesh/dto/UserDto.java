package com.project.ridemesh.dto;

import com.project.ridemesh.entities.enums.Roles;
import java.util.Set;

public class UserDto {
    private Long id;
    private String name;
    private String email;
    private Set<Roles> roles;

    public UserDto() {
    }

    public UserDto(Long id, String name, String email, Set<Roles> roles) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.roles = roles;
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

    public Set<Roles> getRoles() {
        return roles;
    }

    public void setRoles(Set<Roles> roles) {
        this.roles = roles;
    }
}
