package com.example.fitsathi.models;

import java.util.HashMap;
import java.util.Map;

public class Squad {
    private String id;
    private String name;
    private String inviteCode;
    private String leaderId;
    private long createdAt;
    private Map<String, Boolean> members = new HashMap<>();

    public Squad() {
        // Default constructor for Firebase
    }

    public Squad(String id, String name, String inviteCode, String leaderId) {
        this.id = id;
        this.name = name;
        this.inviteCode = inviteCode;
        this.leaderId = leaderId;
        this.createdAt = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getInviteCode() { return inviteCode; }
    public void setInviteCode(String inviteCode) { this.inviteCode = inviteCode; }

    public String getLeaderId() { return leaderId; }
    public void setLeaderId(String leaderId) { this.leaderId = leaderId; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public Map<String, Boolean> getMembers() { return members; }
    public void setMembers(Map<String, Boolean> members) { this.members = members; }

    public int getMemberCount() {
        return members != null ? members.size() : 0;
    }
}
