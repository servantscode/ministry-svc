package org.servantscode.ministry;

public class Ministry {
    private int id;
    private String name;
    private String description;

    public Ministry() {}

    public Ministry(String name) {
        this.name = name;
    }

    public Ministry(int id, String name) {
        this.id = id;
        this.name = name;
    }
    // ----- Accessors -----
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
