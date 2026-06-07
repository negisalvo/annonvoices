package com.example.anonvoices;

public class Session {
    private String sessionId;
    private String teacherId;
    private String sessionCode;
    private long startTime;
    private boolean active;

    public Session() {
        // Default constructor for Firebase
    }

    public Session(String sessionId, String teacherId, String sessionCode, long startTime, boolean active) {
        this.sessionId = sessionId;
        this.teacherId = teacherId;
        this.sessionCode = sessionCode;
        this.startTime = startTime;
        this.active = active;
    }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getTeacherId() { return teacherId; }
    public void setTeacherId(String teacherId) { this.teacherId = teacherId; }

    public String getSessionCode() { return sessionCode; }
    public void setSessionCode(String sessionCode) { this.sessionCode = sessionCode; }

    public long getStartTime() { return startTime; }
    public void setStartTime(long startTime) { this.startTime = startTime; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
