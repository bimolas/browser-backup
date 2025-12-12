package com.example.nexus.model;

public class Profile {
    private int id;
    private String name;
    private String avatarPath;

    // Constructors, getters, and setters
    public Profile() {}

    public Profile(String name) {
        this.name = name;
    }

    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAvatarPath() { return avatarPath; }
    public void setAvatarPath(String avatarPath) { this.avatarPath = avatarPath; }
}