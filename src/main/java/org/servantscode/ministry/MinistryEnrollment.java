package org.servantscode.ministry;

public class MinistryEnrollment {
    private int personId;
    private String personName;
    private int ministryId;
    private String MinistryName;
    private int roleId;
    private String role;

    public MinistryEnrollment() {}

    public MinistryEnrollment(int personId, int ministryId, String role) {
        this.personId = personId;
        this.ministryId = ministryId;
        this.role = role;
    }

    // ----- Accessors -----
    public int getPersonId() { return personId; }
    public void setPersonId(int personId) { this.personId = personId; }

    public String getPersonName() { return personName; }
    public void setPersonName(String personName) { this.personName = personName; }

    public int getMinistryId() { return ministryId; }
    public void setMinistryId(int ministryId) { this.ministryId = ministryId; }

    public String getMinistryName() { return MinistryName; }
    public void setMinistryName(String ministryName) { MinistryName = ministryName; }

    public int getRoleId() { return roleId; }
    public void setRoleId(int roleId) { this.roleId = roleId; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
