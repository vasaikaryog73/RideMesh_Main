package com.project.ridemesh.dto;

public class SignupDto {
    private String email;
    private String name;
    private String password;
    private String username;
    private String profilePictureUrl;
    private String phoneNumber;

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public SignupDto() {
    }

    public SignupDto(String email, String name, String password) {
        this.email = email;
        this.name = name;
        this.password = password;
    }

    public SignupDto(String email, String name, String password, String username, String profilePictureUrl) {
        this.email = email;
        this.name = name;
        this.password = password;
        this.username = username;
        this.profilePictureUrl = profilePictureUrl;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
