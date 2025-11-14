package models.Entities;
import java.time.LocalDateTime;

public class Notification extends BaseEntity {
    private int userId;
    private String title;
    private String message;
    private LocalDateTime createdAt;
    private boolean isRead;
    
    public Notification(int userId, String title, String message) {
        super();
        this.userId = userId;
        this.title = title;
        this.message = message;
        this.createdAt = LocalDateTime.now();
        this.isRead = false;
    }
    
    public void markAsRead() {
        this.isRead = true;
    }
    
    // Getters and Setters
    public int getUserId() { return userId; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public boolean isRead() { return isRead; }
    
    @Override
    public String toString() {
        return "[" + (isRead ? "READ" : "UNREAD") + "] " + title + ": " + message;
    }
}