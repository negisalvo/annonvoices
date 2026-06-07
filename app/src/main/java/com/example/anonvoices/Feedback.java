package com.example.anonvoices;

public class Feedback {
    private String id;
    private String message;
    private String category;
    private long timestamp;
    private boolean read;
    private String sessionCode;
    private String sessionTitle;
    private String teacherUid;

    public Feedback() {
        // Default constructor required for calls to DataSnapshot.getValue(Feedback.class)
    }

    public Feedback(String id, String message, String category, long timestamp, boolean read, String sessionCode, String sessionTitle, String teacherUid) {
        this.id = id;
        this.message = message;
        this.category = category;
        this.timestamp = timestamp;
        this.read = read;
        this.sessionCode = sessionCode;
        this.sessionTitle = sessionTitle;
        this.teacherUid = teacherUid;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public String getSessionCode() { return sessionCode; }
    public void setSessionCode(String sessionCode) { this.sessionCode = sessionCode; }

    public String getSessionTitle() { return sessionTitle; }
    public void setSessionTitle(String sessionTitle) { this.sessionTitle = sessionTitle; }

    public String getTeacherUid() { return teacherUid; }
    public void setTeacherUid(String teacherUid) { this.teacherUid = teacherUid; }
}
