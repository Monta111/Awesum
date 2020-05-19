package com.monta.awesum.model;

public class User {
    private String id;
    private String username;
    private String fullname;
    private String email;
    private String avatarUrl;
    private String bio;

    public User() {
    }

    public User(String id, String username, String fullname, String email, String avatarUrl, String bio) {
        this.id = id;
        this.username = username;
        this.fullname = fullname;
        this.email = email;
        this.avatarUrl = avatarUrl;
        this.bio = bio;
    }


    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getFullname() {
        return fullname;
    }

    public String getEmail() {
        return email;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public String getBio() {
        return bio;
    }
}
