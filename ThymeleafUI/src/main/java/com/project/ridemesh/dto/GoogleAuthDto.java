package com.project.ridemesh.dto;

public class GoogleAuthDto {
    private String email;
    private String name;
    private String profilePictureUrl;

    public GoogleAuthDto() {
    }

    public GoogleAuthDto(String email, String name) {
        this.email = email;
        this.name = name;
    }

    public GoogleAuthDto(String email, String name, String profilePictureUrl) {
        this.email = email;
        this.name = name;
        this.profilePictureUrl = profilePictureUrl;
    }

    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }

    public void setProfilePictureUrl(String profilePictureUrl) {
        this.profilePictureUrl = profilePictureUrl;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
