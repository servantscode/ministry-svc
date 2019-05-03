package org.servantscode.ministry;

public class MinistryRole {
    private int id;
    private int ministryId;
    private String name;
    private boolean contact;
    private boolean leader;

    // ----- Acccessors -----
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getMinistryId() { return ministryId; }
    public void setMinistryId(int ministryId) { this.ministryId = ministryId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public boolean isContact() { return contact; }
    public void setContact(boolean contact) { this.contact = contact; }

    public boolean isLeader() { return leader; }
    public void setLeader(boolean leader) { this.leader = leader; }
}
